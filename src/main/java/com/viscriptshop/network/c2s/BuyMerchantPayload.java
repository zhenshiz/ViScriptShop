package com.viscriptshop.network.c2s;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.viscript_lib.util.item.ItemOutputTargets;
import com.viscript_lib.util.item.ItemUtil;
import com.viscriptshop.Config;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.event.neoforge.ShopServerEvent;
import com.viscriptshop.gui.components.Message;
import com.viscriptshop.gui.data.AggregatedResources;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.network.s2c.S2CPayload;
import com.viscriptshop.promotion.PromotionEngine;
import com.viscriptshop.promotion.ConditionItemPayment;
import com.viscriptshop.promotion.TradeQuote;
import com.viscriptshop.util.MoneyUtil;
import com.viscriptshop.util.ShopHelper;
import com.viscriptshop.util.ViScriptShopServerUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BuyMerchantPayload {
    public static final String BUY_MERCHANT = C2SPayload.MOD_ID + "buy_merchant";

    /**
     * 根据商品清单结算交易，成本、收益和指令只取自服务端商店。
     *
     * <p>清单中每个商品只能出现一次，购买数量必须为正数；任意无效条目都会拒绝整笔交易。
     *
     * @param sender RPC 发送者，交易玩家由连接身份确定
     * @param shopLocation 商店相对路径
     * @param purchaseRequest 仅包含分类 ID、商品 ID 和购买数量的请求
     * @param outputTargetId 物品输出位置标识
     */
    @RPCPacket(BUY_MERCHANT)
    public static void buyMerchant(RPCSender sender, String shopLocation, PurchaseRequest purchaseRequest,
                                   String outputTargetId) {
        if (sender.isServer()) return;
        ServerPlayer player = sender.asPlayer();
        if (player == null) return;
        var purchases = purchaseRequest == null ? null : purchaseRequest.getPurchases();
        try {
            shopLocation = ShopHelper.normalizeShopLocation(shopLocation);
        } catch (IllegalArgumentException e) {
            rejectInvalidRequest(player);
            return;
        }
        ShopInfo shopInfo = ViScriptShopServerUtil.getShopInfo(shopLocation);
        if (shopInfo == null || !isValidPurchaseList(shopInfo, purchases)) {
            rejectInvalidRequest(player);
            return;
        }
        AggregatedResources request = new AggregatedResources();
        request.setPurchaseEntries(purchases);
        TradeQuote quote = PromotionEngine.quote(player, shopLocation, shopInfo, request);
        AggregatedResources cost = quote.cost();
        AggregatedResources gain = quote.gain();
        if (gain.isEmpty()) {
            RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.ERROR,
                    Component.translatable("viscript_shop.message.shoppingCar.empty"));
            return;
        }

        if (cost.hasMissingItems() || gain.hasMissingItems()) {
            RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.ERROR,
                    Component.translatable("viscript_shop.message.buy.missing_item"));
            NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyFail(player, shopInfo, cost, gain));
            return;
        }

        var outputTarget = ItemOutputTargets.resolve(outputTargetId);
        if (!gain.getResourceItems().isEmpty() && !outputTarget.isItemOutputAvailable(player)) {
            RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.ERROR,
                    Component.translatable(
                            "viscript_shop.message.output_target.unavailable",
                            outputTarget.getItemOutputUnavailableReason(player)
                    ));
            NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyFail(player, shopInfo, cost, gain));
            return;
        }

        if (NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyPre(player, shopInfo, cost, gain)).isCanceled()) return;

        // 检查库存是否充足
        var playerStageFlags = ViScriptShopServerUtil.getStageFlags(player);
        for (var purchaseEntry : gain.getPurchaseEntries()) {
            var categoryInfo = shopInfo.getCategoryInfos().stream()
                    .filter(c -> c.getId().equals(purchaseEntry.getCategoryId()))
                    .findFirst()
                    .orElse(null);
            if (categoryInfo == null) {
                rejectInvalidRequest(player);
                return;
            }

            if (!categoryInfo.canAccess(playerStageFlags)) {
                RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.ERROR,
                        Component.translatable("viscript_shop.message.stage_flags.missing"));
                NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyFail(player, shopInfo, cost, gain));
                return;
            }

            var merchantInfo = categoryInfo.getMerchants().stream()
                    .filter(m -> m.getId().equals(purchaseEntry.getMerchantId()))
                    .findFirst()
                    .orElse(null);
            if (merchantInfo == null) {
                rejectInvalidRequest(player);
                return;
            }

            int stock = ViScriptShopServerUtil.getEffectiveMerchantStock(player, shopLocation, purchaseEntry.getCategoryId(), merchantInfo);
            int buyCount = purchaseEntry.getBuyCount();

            if (!merchantInfo.canAccess(playerStageFlags)) {
                RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.ERROR,
                        Component.translatable("viscript_shop.message.stage_flags.missing"));
                NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyFail(player, shopInfo, cost, gain));
                return;
            }

            if (stock >= 0 && buyCount > stock) {
                // 发送错误消息
                RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.ERROR,
                        Component.translatable("viscript_shop.message.shoppingCart.out_of_stock"));
                RPCPacketDistributor.rpcToPlayer(player, S2CPayload.UPDATE_OUT_OF_STOCK,
                        purchaseEntry.getCategoryId(), purchaseEntry.getMerchantId(), stock);
                // 库存不足
                NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyFail(player, shopInfo, cost, gain));
                return;
            }
        }

        int maxShopUiGiveItemsPerPurchase = Config.maxShopUiGiveItemsPerPurchase.get();
        long totalGainItemCount = gain.getTotalItemCount();
        if (maxShopUiGiveItemsPerPurchase >= 0 && totalGainItemCount > maxShopUiGiveItemsPerPurchase) {
            RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.ERROR,
                    Component.translatable("viscript_shop.message.buy.too_many_items", maxShopUiGiveItemsPerPurchase));
            NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyFail(player, shopInfo, cost, gain));
            return;
        }

        // 先规划整车优惠券，报价和任何失败分支都不会扣除物品。
        ConditionItemPayment conditionPayment = ConditionItemPayment.plan(player, quote.conditionCosts());
        if (!conditionPayment.isAffordable()) {
            RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.ERROR,
                    Component.translatable("viscript_shop.message.promotion.items_unavailable"));
            NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyFail(player, shopInfo, cost, gain));
            return;
        }
        var regularItemCosts = quote.regularItemCosts();
        // 普通商品成本不能占用已经预留的优惠券。
        for (AggregatedResources.ItemEntry itemEntry : regularItemCosts) {
            var itemStack = itemEntry.getItemStack();
            if (!itemStack.isEmpty() && conditionPayment.availableFor(player, itemEntry) < itemEntry.getCount()) {
                // 物品数量不够
                RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.ERROR, Component.translatable("viscript_shop.message.notEnoughItem", itemStack.getItem().getDescription().getString()));
                NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyFail(player, shopInfo, cost, gain));
                return;
            }
        }

        double netMoneyCost = MoneyUtil.subtract(cost.getTotalMoney(), gain.getTotalMoney());
        double netMoneyGain = MoneyUtil.subtract(gain.getTotalMoney(), cost.getTotalMoney());
        double playerMoney = ViScriptShopServerUtil.getMoney(player);
        if (!MoneyUtil.hasEnough(playerMoney, netMoneyCost)) {
            // 钱不够
            RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.ERROR,
                    Component.translatable("viscript_shop.message.noEnoughMoney",
                            MoneyUtil.format(MoneyUtil.add(netMoneyCost, -playerMoney))));
            NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyFail(player, shopInfo, cost, gain));
            return;
        }

        // 所有可失败的交易检查通过后，才按规划的条目从玩家全部容器中扣券。
        if (!conditionPayment.consume(player)) {
            RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.ERROR,
                    Component.translatable("viscript_shop.message.promotion.items_unavailable"));
            NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyFail(player, shopInfo, cost, gain));
            return;
        }

        // 组件匹配规则可能重叠；实际扣除失败时不能继续发放物品或执行指令。
        for (AggregatedResources.ItemEntry itemEntry : regularItemCosts) {
            var rule = itemEntry.getMatchRule();
            long remaining = ItemUtil.removeItemForPlayer(player, itemEntry.getItemStack(), itemEntry.getCount(),
                    rule.resolvedCompareMode(), rule.resolvedComponents());
            if (remaining > 0) {
                RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.ERROR,
                        Component.translatable("viscript_shop.message.notEnoughItem",
                                itemEntry.getItemStack().getHoverName().getString()));
                NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyFail(player, shopInfo, cost, gain));
                return;
            }
        }

        // 扣减库存
        for (var purchaseEntry : gain.getPurchaseEntries()) {
            var categoryInfo = shopInfo.getCategoryInfos().stream()
                    .filter(c -> c.getId().equals(purchaseEntry.getCategoryId()))
                    .findFirst()
                    .orElse(null);
            if (categoryInfo == null) continue;

            var merchantInfo = categoryInfo.getMerchants().stream()
                    .filter(m -> m.getId().equals(purchaseEntry.getMerchantId()))
                    .findFirst()
                    .orElse(null);
            if (merchantInfo == null) continue;

            ViScriptShopServerUtil.reduceMerchantStock(player, shopLocation, purchaseEntry.getCategoryId(),
                    merchantInfo, purchaseEntry.getBuyCount());
        }

        // 保存数据到文件
        if (!shopLocation.isEmpty()) {
            var shopSavedData = ViscriptShop.getShopSavedData();
            if (shopSavedData != null) {
                shopSavedData.setShopInfo(shopLocation, shopInfo);
            }
        }

        // 同一购物车的货币收入与支出按净额一次性结算，物品仍分别验证和处理。
        double settledMoney = MoneyUtil.add(MoneyUtil.subtract(playerMoney, netMoneyCost, true), netMoneyGain);
        if (Double.compare(settledMoney, playerMoney) != 0) {
            ViScriptShopServerUtil.setMoney(player, settledMoney);
        }

        // 给予玩家物品
        gain.getResourceItems().forEach(itemEntry -> ItemOutputTargets.giveItem(
                player,
                outputTargetId,
                itemEntry.getItemStack(),
                itemEntry.getCount()
        ));

        // 给予玩家经验
        if (gain.getTotalXp() > 0) player.giveExperiencePoints(gain.getTotalXp());

        // 执行指令
        if (!gain.getCommands().isEmpty()) {
            for (String command : gain.getCommands()) {
                executeCommand(player, command);
            }
        }

        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.SUCCESS,
                Component.translatable("viscript_shop.message.buySuccess"));
        NeoForge.EVENT_BUS.post(new ShopServerEvent.BuySuccess(player, shopInfo, cost, gain));

        // 交易全部完成后以服务端真实背包为准刷新物品计数，再重新加载 UI。
        GetItemCountC2SPayload.sendItemCountSnapshot(player, shopInfo);
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.RELOAD_SHOP_UI,
                ViScriptShopServerUtil.getPlayerVisibleShopInfo(player, shopLocation, shopInfo));
    }

    /**
     * 以当前玩家为上下文执行一条商品指令。
     *
     * <p>该方法不会拆分指令文本；多条指令由商品的指令列表分别提供。
     *
     * @param player 完成交易的服务端玩家
     * @param value 一条完整指令
     */
    private static void executeCommand(ServerPlayer player, String value) {
        String command = value == null ? "" : value.trim();
        if (!command.isBlank()) {
            MinecraftServer server = Platform.getMinecraftServer();
            CommandSourceStack commandSource = player.createCommandSourceStack().withPermission(Commands.LEVEL_GAMEMASTERS).withSuppressedOutput();
            var dispatcher = server.getCommands().getDispatcher();
            try {
                dispatcher.execute(dispatcher.parse(command, commandSource));
            } catch (UnsupportedOperationException e) {
                server.getCommands().performPrefixedCommand(commandSource, command);
            } catch (CommandSyntaxException e) {
                ViscriptShop.LOGGER.error("Error executing command on server: {}", command, e);
            }
        }
    }

    private static boolean isValidPurchaseList(ShopInfo shopInfo, List<AggregatedResources.PurchaseEntry> purchases) {
        if (purchases == null || purchases.isEmpty()) return false;
        Set<List<String>> requested = new HashSet<>();
        for (var entry : purchases) {
            if (entry == null || entry.getCategoryId() == null || entry.getCategoryId().isBlank()
                    || entry.getMerchantId() == null || entry.getMerchantId().isBlank()
                    || entry.getBuyCount() <= 0
                    || !requested.add(List.of(entry.getCategoryId(), entry.getMerchantId()))) {
                return false;
            }
            var categories = shopInfo.getCategoryInfos().stream()
                    .filter(category -> entry.getCategoryId().equals(category.getId())).toList();
            if (categories.size() != 1 || categories.getFirst().getMerchants().stream()
                    .filter(merchant -> entry.getMerchantId().equals(merchant.getId())).count() != 1) {
                return false;
            }
        }
        return true;
    }

    private static void rejectInvalidRequest(ServerPlayer player) {
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.ERROR,
                Component.translatable("viscript_shop.message.buy.invalid_request"));
    }

}

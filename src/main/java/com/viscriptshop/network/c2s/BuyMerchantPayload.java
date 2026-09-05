package com.viscriptshop.network.c2s;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.viscript_lib.util.item.ItemOutputTargets;
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
import com.viscriptshop.util.ViScriptShopServerUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

public class BuyMerchantPayload {
    public static final String BUY_MERCHANT = C2SPayload.MOD_ID + "buy_merchant";

    @RPCPacket(BUY_MERCHANT)
    public static void buyMerchant(RPCSender sender, String shopLocation, AggregatedResources request,
                                   String outputTargetId) {
        ServerPlayer player = sender.asPlayer();
        if (player == null) return;
        ShopInfo shopInfo = ViScriptShopServerUtil.getShopInfo(shopLocation);
        if (shopInfo == null) return;
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
            if (categoryInfo == null) continue;

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
            if (merchantInfo == null) continue;

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
        ConditionItemPayment conditionPayment = ConditionItemPayment.plan(player.getInventory(), quote.conditionCosts());
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
                            MoneyUtil.format(MoneyUtil.subtract(netMoneyCost, playerMoney))));
            NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyFail(player, shopInfo, cost, gain));
            return;
        }

        // 所有可失败的交易检查通过后，才从之前核对过的实际栏位扣券。
        if (!conditionPayment.consume()) {
            RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.ERROR,
                    Component.translatable("viscript_shop.message.promotion.items_unavailable"));
            NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyFail(player, shopInfo, cost, gain));
            return;
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

        // 删除物品
        for (AggregatedResources.ItemEntry itemEntry : regularItemCosts) {
            itemEntry.removeItemForPlayer(player);
        }

        // 同一购物车的货币收入与支出按净额一次性结算，物品仍分别验证和处理。
        double settledMoney = MoneyUtil.add(MoneyUtil.subtract(playerMoney, netMoneyCost), netMoneyGain);
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
    public static void executeCommand(ServerPlayer player, String value) {
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

}

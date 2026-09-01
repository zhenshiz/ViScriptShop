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
        AggregatedResources cost = buildAuthoritativeCost(shopInfo, request);
        AggregatedResources gain = buildAuthoritativeGain(shopInfo, request);
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
        if (!gain.getItems().isEmpty() && !outputTarget.isItemOutputAvailable(player)) {
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

        // 判断数量是否足够
        for (AggregatedResources.ItemEntry itemEntry : cost.getItemEntries()) {
            var itemStack = itemEntry.getItemStack();
            if (!itemStack.isEmpty() && itemEntry.getItemForPlayerCount(player) < itemEntry.getCount()) {
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

        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.SUCCESS, Component.translatable("viscript_shop.message.buySuccess"));
        NeoForge.EVENT_BUS.post(new ShopServerEvent.BuySuccess(player, shopInfo, cost, gain));

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
        for (AggregatedResources.ItemEntry itemEntry : cost.getItemEntries()) {
            itemEntry.removeItemForPlayer(player);
        }

        // 同一购物车的货币收入与支出按净额一次性结算，物品仍分别验证和处理。
        double settledMoney = MoneyUtil.add(MoneyUtil.subtract(playerMoney, netMoneyCost), netMoneyGain);
        if (Double.compare(settledMoney, playerMoney) != 0) {
            ViScriptShopServerUtil.setMoney(player, settledMoney);
        }

        // 给予玩家物品
        gain.getItems().forEach(itemStack -> ItemOutputTargets.giveItem(player, outputTargetId, itemStack));

        // 给予玩家经验
        if (gain.getTotalXp() > 0) player.giveExperiencePoints(gain.getTotalXp());

        // 执行指令
        if (!gain.getCommands().isEmpty()) {
            for (String command : gain.getCommands()) {
                executeCommands(player, command);
            }
        }

        // 交易全部完成后以服务端真实背包为准刷新物品计数，再重新加载 UI。
        GetItemCountC2SPayload.sendItemCountSnapshot(player, shopInfo);
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.RELOAD_SHOP_UI,
                ViScriptShopServerUtil.getPlayerVisibleShopInfo(player, shopLocation, shopInfo));
    }

    public static void executeCommands(ServerPlayer player, String value) {
        var commands = value.split(";");
        for (var command : commands) {
            command = command.trim();
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

    private static AggregatedResources buildAuthoritativeCost(ShopInfo shopInfo, AggregatedResources request) {
        AggregatedResources cost = new AggregatedResources();
        for (AggregatedResources.PurchaseEntry purchaseEntry : request.getPurchaseEntries()) {
            if (purchaseEntry.getBuyCount() <= 0) continue;

            CategoryInfo categoryInfo = findCategory(shopInfo, purchaseEntry.getCategoryId());
            if (categoryInfo == null) continue;
            MerchantInfo merchantInfo = findMerchant(categoryInfo, purchaseEntry.getMerchantId());
            if (merchantInfo == null) continue;

            cost.getPurchaseEntries().add(new AggregatedResources.PurchaseEntry(
                    purchaseEntry.getCategoryId(),
                    purchaseEntry.getMerchantId(),
                    purchaseEntry.getBuyCount()
            ));
            switch (categoryInfo.getShopType()) {
                case ITEM_FOR_ITEM -> {
                    cost.addItemEntry(merchantInfo.getSerializedItemA(), purchaseEntry.getBuyCount(), merchantInfo.getItemAMatchRule());
                    cost.addItemEntry(merchantInfo.getSerializedItemB(), purchaseEntry.getBuyCount(), merchantInfo.getItemBMatchRule());
                }
                case CURRENCY -> {
                    switch (merchantInfo.getTradeType()) {
                        case BUY -> cost.addMoney(merchantInfo.getMoney(), purchaseEntry.getBuyCount());
                        case SELL -> cost.addItemEntry(merchantInfo.getSerializedItemResult(), purchaseEntry.getBuyCount(), null);
                    }
                }
            }
        }
        return cost;
    }

    private static AggregatedResources buildAuthoritativeGain(ShopInfo shopInfo, AggregatedResources request) {
        AggregatedResources gain = new AggregatedResources();
        for (AggregatedResources.PurchaseEntry purchaseEntry : request.getPurchaseEntries()) {
            if (purchaseEntry.getBuyCount() <= 0) continue;

            CategoryInfo categoryInfo = findCategory(shopInfo, purchaseEntry.getCategoryId());
            if (categoryInfo == null) continue;
            MerchantInfo merchantInfo = findMerchant(categoryInfo, purchaseEntry.getMerchantId());
            if (merchantInfo == null) continue;

            gain.getPurchaseEntries().add(new AggregatedResources.PurchaseEntry(
                    purchaseEntry.getCategoryId(),
                    purchaseEntry.getMerchantId(),
                    purchaseEntry.getBuyCount()
            ));
            gain.addXp(merchantInfo.getXp(), purchaseEntry.getBuyCount());
            gain.addCommand(merchantInfo.getCommand());
            switch (categoryInfo.getShopType()) {
                case ITEM_FOR_ITEM -> gain.addItem(merchantInfo.getSerializedItemResult(), purchaseEntry.getBuyCount());
                case CURRENCY -> {
                    switch (merchantInfo.getTradeType()) {
                        case BUY -> gain.addItem(merchantInfo.getSerializedItemResult(), purchaseEntry.getBuyCount());
                        case SELL -> gain.addMoney(merchantInfo.getMoney(), purchaseEntry.getBuyCount());
                    }
                }
            }
        }
        return gain;
    }

    private static CategoryInfo findCategory(ShopInfo shopInfo, String categoryId) {
        return shopInfo.getCategoryInfos().stream()
                .filter(category -> category.getId().equals(categoryId))
                .findFirst()
                .orElse(null);
    }

    private static MerchantInfo findMerchant(CategoryInfo categoryInfo, String merchantId) {
        return categoryInfo.getMerchants().stream()
                .filter(merchant -> merchant.getId().equals(merchantId))
                .findFirst()
                .orElse(null);
    }
}

package com.viscriptshop.util;

import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.command.ShopCommand;
import com.viscriptshop.gui.data.ShopDisplayBatch;
import com.viscriptshop.gui.data.ShopDisplayEntry;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.network.s2c.ShopJeiS2CPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** 按需读取服务端商店，并为请求玩家生成价格、阶段和赠品展示快照。 */
public final class ShopJeiCatalog {
    private static long revision;

    private ShopJeiCatalog() {
    }

    /**
     * 为打开 JEI 的玩家发送当前展示，不重置库存或触发其他配方重载。
     *
     * @param player 请求玩家；为 {@code null} 时忽略
     */
    public static void refresh(ServerPlayer player) {
        if (player == null) return;
        var entries = new ArrayList<ShopDisplayEntry>();
        var flags = ViScriptShopServerUtil.getStageFlags(player);
        for (String location : ShopCommand.getServerShopFiles().stream().sorted().toList()) {
            try {
                var shop = currentShop(location);
                if (shop == null) continue;
                boolean hideLocked = shop.getLockedMerchantVisibility() == ShopInfo.LockedMerchantVisibility.HIDDEN;
                for (var category : shop.getCategoryInfos()) {
                    if (hideLocked && !category.canAccess(flags)) continue;
                    for (var merchant : category.getMerchants()) {
                        if (hideLocked && !merchant.canAccess(flags)) continue;
                        entries.add(ShopDisplayEntry.of(player, location, shop, category, merchant));
                    }
                }
            } catch (RuntimeException exception) {
                ViscriptShop.LOGGER.warn("Cannot read JEI shop display {}", location, exception);
            }
        }
        long version = ++revision;
        // 收齐末批后一次性替换展示，避免出现半个目录。
        int batches = Math.max(1, (entries.size() + 31) / 32);
        for (int index = 0; index < batches; index++) {
            var batch = new ShopDisplayBatch(version, index, index == batches - 1,
                    new ArrayList<>(entries.subList(index * 32, Math.min(entries.size(), (index + 1) * 32))));
            RPCPacketDistributor.rpcToPlayer(player, ShopJeiS2CPayload.SNAPSHOT, batch);
        }
    }

    private static ShopInfo currentShop(String location) {
        var saved = ViScriptShopServerUtil.getSavedShopInfo(location);
        return saved != null ? saved : ShopHelper.getShop(location, false);
    }

    /**
     * 校验服务端文件目录和当前阶段后打开目标，不执行购买。
     *
     * @param player 发起跳转的玩家；为 {@code null} 时忽略
     * @param location 服务端商店相对路径
     * @param categoryId 目标分类 ID
     * @param merchantId 用于填充商品编号筛选的商品 ID
     */
    public static void open(ServerPlayer player, String location, String categoryId, String merchantId) {
        if (player == null) return;
        if (ShopCommand.getServerShopFiles().contains(location)) {
            var shop = currentShop(location);
            var flags = ViScriptShopServerUtil.getStageFlags(player);
            if (containsAccessibleTrade(shop, flags, categoryId, merchantId)) {
                ViScriptShopServerUtil.serverOpenShop(player, location, categoryId, merchantId);
                return;
            }
        }
        player.sendSystemMessage(Component.translatable("viscript_shop.jei.unavailable"));
    }

    private static boolean containsAccessibleTrade(ShopInfo shop, List<String> flags, String categoryId, String merchantId) {
        return shop != null && shop.getCategoryInfos().stream()
                .filter(category -> category.getId().equals(categoryId) && category.canAccess(flags))
                .flatMap(category -> category.getMerchants().stream())
                .anyMatch(merchant -> merchant.getId().equals(merchantId) && merchant.canAccess(flags));
    }
}

package com.viscriptshop.util;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.viscript_lib.util.CodecUtil;
import com.viscriptshop.Config;
import com.viscriptshop.ShopRegistries;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.ShopEditor;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.gui.data.ShopSavedData;
import com.viscriptshop.network.s2c.S2CPayload;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.sirgrantd.sg_economy.api.SGEconomyApi;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ViScriptShopServerUtil {

    @Info("服务端打开商店编辑器")
    public static void serverOpenShopEditor(ServerPlayer player, String shop) {
        ShopInfo shopInfo = ShopHelper.getShop(shop);
        PlayerUIMenuType.openUI(player, ShopEditor.SHOP_ID);
        if (shopInfo != null) RPCPacketDistributor.rpcToPlayer(player, S2CPayload.OPEN_SHOP_EDITOR, shopInfo);
    }

    @Info("服务端为玩家打开快捷商店选择界面")
    public static void serverOpenShopSelector(ServerPlayer player) {
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.OPEN_SHOP_SELECTOR);
    }

    @Info("服务端打开商店")
    public static void serverOpenShop(ServerPlayer player, String shopLocation) {
        serverOpenShop(player, shopLocation, null, null);
    }

    @Info("服务端打开商店（带分类和商品参数）")
    public static void serverOpenShop(ServerPlayer player, String shopLocation, String categoryId, String merchantId) {
        ShopInfo shopInfo = getOrInitSavedShopInfo(shopLocation);
        if (shopInfo == null) {
            ViscriptShop.LOGGER.error("shop location {} not found", shopLocation);
            return;
        }
        ShopInfo visibleShopInfo = getPlayerVisibleShopInfo(player, shopLocation, shopInfo);
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.OPEN_SHOP_UI, shopLocation, visibleShopInfo,
                categoryId != null ? categoryId : "",
                merchantId != null ? merchantId : "");
    }

    @Info("重置商店信息")
    public static void reloadOpenShop(String shop) {
        ShopSavedData shopSavedData = ViscriptShop.getShopSavedData();
        shopSavedData.resetShopInfo(shop);
        ShopHelper.clearCache();
    }

    @Nullable
    @Info("仅从savedData获取商店信息，不会读取服务端文件")
    public static ShopInfo getSavedShopInfo(String shop) {
        return ViscriptShop.getShopSavedData().getShopInfo(shop);
    }

    @Nullable
    @Info("获取商店信息，优先从savedData读取，不存在时回退到服务端文件")
    public static ShopInfo getShopInfo(String shop) {
        ShopInfo shopInfo = getSavedShopInfo(shop);
        if (shopInfo == null) shopInfo = ShopHelper.getShop(shop);
        return shopInfo;
    }

    @Nullable
    @Info("获取可写的存档级商店信息，如果savedData中不存在则从服务端文件加载并写入savedData")
    public static ShopInfo getOrInitSavedShopInfo(String shop) {
        ShopSavedData shopSavedData = ViscriptShop.getShopSavedData();
        ShopInfo shopInfo = shopSavedData.getShopInfo(shop);
        if (shopInfo == null) {
            shopInfo = ShopHelper.getShop(shop);
            if (shopInfo != null) {
                shopSavedData.setShopInfo(shop, shopInfo);
            }
        }
        return shopInfo;
    }

    @Info("设置商品信息")
    public static void setShopInfo(String shop, ShopInfo shopInfo) {
        ShopSavedData shopSavedData = ViscriptShop.getShopSavedData();
        shopSavedData.setShopInfo(shop, shopInfo);
    }

    @Info("添加商店商品")
    public static void addShopMerchant(String shop, int categoryIndex, MerchantInfo merchantInfo) {
        ShopInfo shopInfo = getOrInitSavedShopInfo(shop);
        if (shopInfo == null) return;

        shopInfo.getCategoryInfos().get(categoryIndex).getMerchants().add(merchantInfo);
        setShopInfo(shop, shopInfo);
    }

    @Info("设置商店是否显示在快捷商店选择界面中，可用于按进度锁定或解锁商店")
    public static void setQuickOpening(String shop, boolean quickOpening) {
        ShopInfo shopInfo = getOrInitSavedShopInfo(shop);
        if (shopInfo == null) return;

        shopInfo.setQuickOpening(quickOpening);
        setShopInfo(shop, shopInfo);
    }

    @Info("设置商店商品库存")
    public static boolean setMerchantStock(String shopLocation, String categoryId, String merchantId, int stock) {
        ShopInfo shopInfo = getOrInitSavedShopInfo(shopLocation);
        if (shopInfo == null) {
            return false;
        }

        // 查找并设置库存
        for (var category : shopInfo.getCategoryInfos()) {
            if (category.getId().equals(categoryId)) {
                for (var merchant : category.getMerchants()) {
                    if (merchant.getId().equals(merchantId)) {
                        merchant.setStock(stock);
                        ViscriptShop.getShopSavedData().clearMerchantStock(shopLocation, categoryId, merchantId);
                        // 保存到存档数据并清理文件缓存
                        setShopInfo(shopLocation, shopInfo);
                        ShopHelper.clearCache();
                        return true;
                    }
                }
                break;
            }
        }
        return false;
    }

    @Info("删除商店商品")
    public static boolean removeMerchant(String shopLocation, String categoryId, String merchantId) {
        ShopInfo shopInfo = getOrInitSavedShopInfo(shopLocation);
        if (shopInfo == null) {
            return false;
        }

        // 查找并删除商品
        for (var category : shopInfo.getCategoryInfos()) {
            if (category.getId().equals(categoryId)) {
                boolean removed = category.getMerchants().removeIf(merchant -> merchant.getId().equals(merchantId));
                if (removed) {
                    ViscriptShop.getShopSavedData().clearMerchantStock(shopLocation, categoryId, merchantId);
                    // 保存到存档数据并清理文件缓存
                    setShopInfo(shopLocation, shopInfo);
                    ShopHelper.clearCache();
                    return true;
                }
                break;
            }
        }
        return false;
    }

    @Info("是否启用玩家独立库存")
    public static boolean isPersonalStockEnabled() {
        return Config.isPersonalStock != null && Config.isPersonalStock.get();
    }

    @Info("获取玩家可见的商店信息")
    public static ShopInfo getPlayerVisibleShopInfo(ServerPlayer player, String shopLocation, ShopInfo shopInfo) {
        ShopInfo visibleShopInfo = copyShopInfo(shopInfo);
        for (CategoryInfo categoryInfo : visibleShopInfo.getCategoryInfos()) {
            for (MerchantInfo merchantInfo : categoryInfo.getMerchants()) {
                merchantInfo.setStock(getEffectiveMerchantStock(player, shopLocation, categoryInfo.getId(), merchantInfo));
            }
        }
        return visibleShopInfo;
    }

    @Info("获取玩家当前实际可购买库存")
    public static int getEffectiveMerchantStock(ServerPlayer player, String shopLocation, String categoryId, MerchantInfo merchantInfo) {
        int stock = merchantInfo.getStock();
        if (stock < 0) {
            return stock;
        }

        ShopSavedData shopSavedData = ViscriptShop.getShopSavedData();
        if (shopSavedData == null) {
            return stock;
        }
        return shopSavedData.getMerchantStock(shopLocation, getStockOwner(player), categoryId, merchantInfo.getId(), stock);
    }

    @Info("扣减玩家购买后的库存")
    public static boolean reduceMerchantStock(ServerPlayer player, String shopLocation, String categoryId, MerchantInfo merchantInfo, int count) {
        int stock = merchantInfo.getStock();
        if (stock < 0 || count <= 0) {
            return false;
        }

        ShopSavedData shopSavedData = ViscriptShop.getShopSavedData();
        if (shopSavedData != null) {
            int currentStock = getEffectiveMerchantStock(player, shopLocation, categoryId, merchantInfo);
            shopSavedData.setMerchantStock(shopLocation, getStockOwner(player), categoryId, merchantInfo.getId(),
                    Math.max(0, currentStock - count));
        }
        return false;
    }

    private static ShopInfo copyShopInfo(ShopInfo shopInfo) {
        Tag tag = CodecUtil.serializeNBT(ShopInfo.CODEC, shopInfo, Platform.getFrozenRegistry());
        return CodecUtil.deserializeNBT(ShopInfo.CODEC, tag, Platform.getFrozenRegistry());
    }

    private static String getStockOwner(ServerPlayer player) {
        return isPersonalStockEnabled() ? player.getUUID().toString() : ShopSavedData.GLOBAL_STOCK_OWNER;
    }

    @Info("获取玩家钱")
    public static double getMoney(ServerPlayer player) {
        if (ViscriptShop.isMagicCoinsLoaded() && Config.isReplaceMoneyToMagicCoin.get()) {
            return MoneyUtil.normalize(SGEconomyApi.getBalance(player));
        }
        return MoneyUtil.normalize(player.getData(ShopRegistries.MONEY).getMoney());
    }

    @Info("获取玩家阶段标记")
    public static List<String> getStageFlags(ServerPlayer player) {
        return player.getData(ShopRegistries.MONEY).getFlags();
    }

    @Info("玩家是否拥有指定阶段标记")
    public static boolean hasStageFlag(ServerPlayer player, String flag) {
        return getStageFlags(player).contains(flag);
    }

    @Info("玩家是否拥有所有指定阶段标记")
    public static boolean hasStageFlags(ServerPlayer player, List<String> flags) {
        return getMissingStageFlags(player, flags).isEmpty();
    }

    @Info("获取玩家缺少的阶段标记")
    public static List<String> getMissingStageFlags(ServerPlayer player, List<String> flags) {
        if (flags == null || flags.isEmpty()) {
            return List.of();
        }
        List<String> playerFlags = getStageFlags(player);
        List<String> missing = new ArrayList<>();
        for (String flag : flags) {
            if (!flag.isEmpty() && !playerFlags.contains(flag)) {
                missing.add(flag);
            }
        }
        return missing;
    }

    @Info("给玩家添加阶段标记")
    public static boolean addStageFlag(ServerPlayer player, String flag) {
        if (flag.isEmpty()) {
            return false;
        }
        ShopRegistries.Money data = player.getData(ShopRegistries.MONEY);
        if (data.getFlags().contains(flag)) {
            return false;
        }
        data.getFlags().add(flag);
        player.setData(ShopRegistries.MONEY, data);
        return true;
    }

    @Info("移除玩家阶段标记")
    public static boolean removeStageFlag(ServerPlayer player, String flag) {
        ShopRegistries.Money data = player.getData(ShopRegistries.MONEY);
        boolean removed = data.getFlags().remove(flag);
        if (removed) {
            player.setData(ShopRegistries.MONEY, data);
        }
        return removed;
    }

    @Info("设置玩家钱")
    public static void setMoney(ServerPlayer player, double money) {
        double normalized = MoneyUtil.normalize(money);
        if (ViscriptShop.isMagicCoinsLoaded() && Config.isReplaceMoneyToMagicCoin.get()) {
            SGEconomyApi.setBalance(player, normalized);
        }
        ShopRegistries.Money data = player.getData(ShopRegistries.MONEY);
        data.setMoney(normalized);
        player.setData(ShopRegistries.MONEY, data);
    }

    @Info("给玩家钱")
    public static void addMoney(ServerPlayer player, double money) {
        if (MoneyUtil.isPositive(money)) {
            setMoney(player, MoneyUtil.add(getMoney(player), money));
        }
    }

    @Info("扣除玩家钱")
    public static double removeMoney(ServerPlayer player, double money) {
        double requested = MoneyUtil.normalize(money);
        double playerMoney = getMoney(player);
        double removed = Math.min(requested, playerMoney);
        setMoney(player, MoneyUtil.subtract(playerMoney, removed));
        return removed;
    }
}

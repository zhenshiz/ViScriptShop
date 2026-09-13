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
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
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

    /**
     * 为玩家打开 FTB 侧边栏配置的默认商店。
     *
     * <p>路径为空时打开快捷商店选择界面；路径无效时发送错误消息并回退到选择界面。
     * 客户端不提供路径，实际目标始终由服务端公共配置决定。
     *
     * @param player 接收商店界面的服务端玩家
     */
    public static void serverOpenFtbShop(ServerPlayer player) {
        String location = Config.ftbDefaultShop.get().trim();
        if (location.isEmpty()) {
            serverOpenShopSelector(player);
            return;
        }
        ShopInfo shop = getOrInitSavedShopInfo(location);
        if (shop == null) {
            player.sendSystemMessage(Component.translatable("viscript_shop.message.ftb_default_shop_unavailable", location)
                    .withStyle(ChatFormatting.RED));
            serverOpenShopSelector(player);
            return;
        }
        serverOpenShop(player, location);
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

    /**
     * 增加商品的基准库存及当前世界中所有对应的运行时剩余库存。
     *
     * <p>该操作保留每个库存所有者已经消耗的数量。负数库存表示无限库存，因此保持不变；
     * 有限库存超过 {@code Integer.MAX_VALUE} 时饱和到该上限。小于或等于零的增加量不生效。
     *
     * @param shopLocation 商店路径
     * @param categoryId 分类标识
     * @param merchantId 商品标识
     * @param amount 要增加的正整数库存量
     * @return 找到商品且增加量有效时返回 {@code true}
     */
    @Info("增加商品库存，保留当前世界中已经消耗的数量")
    public static boolean addMerchantStock(String shopLocation, String categoryId, String merchantId, int amount) {
        if (amount <= 0) {
            return false;
        }

        ShopInfo shopInfo = getOrInitSavedShopInfo(shopLocation);
        if (shopInfo == null) {
            return false;
        }

        for (var category : shopInfo.getCategoryInfos()) {
            if (category.getId().equals(categoryId)) {
                for (var merchant : category.getMerchants()) {
                    if (merchant.getId().equals(merchantId)) {
                        int stock = merchant.getStock();
                        if (stock >= 0) {
                            merchant.setStock((int) Math.min(Integer.MAX_VALUE, (long) stock + amount));
                            ViscriptShop.getShopSavedData().addMerchantStock(
                                    shopLocation, categoryId, merchantId, amount);
                            setShopInfo(shopLocation, shopInfo);
                            ShopHelper.clearCache();
                        }
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
    public static boolean reduceMerchantStock(ServerPlayer player, String shopLocation, String categoryId, MerchantInfo merchantInfo, long count) {
        int stock = merchantInfo.getStock();
        if (stock < 0 || count <= 0) {
            return false;
        }

        ShopSavedData shopSavedData = ViscriptShop.getShopSavedData();
        if (shopSavedData != null) {
            int currentStock = getEffectiveMerchantStock(player, shopLocation, categoryId, merchantInfo);
            shopSavedData.setMerchantStock(shopLocation, getStockOwner(player), categoryId, merchantInfo.getId(),
                    (int) Math.max(0L, currentStock - count));
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
            return MoneyUtil.normalizeBalance(SGEconomyApi.getBalance(player));
        }
        return MoneyUtil.normalizeBalance(player.getData(ShopRegistries.MONEY).getMoney());
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
        double normalized = MoneyUtil.normalizeBalance(money);
        if (ViscriptShop.isMagicCoinsLoaded() && Config.isReplaceMoneyToMagicCoin.get()) {
            SGEconomyApi.setBalance(player, MoneyUtil.normalize(normalized));
            normalized = MoneyUtil.normalizeBalance(SGEconomyApi.getBalance(player));
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

    /**
     * 扣除玩家货币，默认最多扣除当前的正余额。
     *
     * <p>已有负余额保持不变且返回零；允许透支时使用带策略参数的重载。
     *
     * @param player 被扣款的服务端玩家
     * @param money 待扣除的浮点金额；非正数、非数字或无穷值不扣款
     * @return 实际扣除的非负金额
     * @see #removeMoney(ServerPlayer, double, boolean)
     */
    @Info("扣除玩家钱，默认不允许透支")
    public static double removeMoney(ServerPlayer player, double money) {
        return removeMoney(player, money, false);
    }

    /**
     * 按指定透支策略扣除玩家货币。
     *
     * <p>禁止透支时最多扣除当前的正余额；已有负余额保持不变且返回零。
     * 允许透支时余额最低饱和到 {@code -Double.MAX_VALUE}。
     * 启用 Magic Coins 替换时遵循其余额限制，最多扣至零。
     *
     * @param player 被扣款的服务端玩家
     * @param money 待扣除的浮点金额；非正数、非数字或无穷值不扣款
     * @param allowNegative 是否允许扣款后余额为负数
     * @return 实际扣除的非负金额
     */
    @Info("扣除玩家钱，可指定是否允许余额为负数")
    public static double removeMoney(ServerPlayer player, double money, boolean allowNegative) {
        double requested = MoneyUtil.normalize(money);
        double playerMoney = getMoney(player);
        double removed = allowNegative ? requested : Math.min(requested, MoneyUtil.normalize(playerMoney));
        if (removed <= 0) {
            return 0;
        }
        double remaining = MoneyUtil.subtract(playerMoney, removed, true);
        setMoney(player, remaining);
        return MoneyUtil.add(playerMoney, -getMoney(player));
    }
}

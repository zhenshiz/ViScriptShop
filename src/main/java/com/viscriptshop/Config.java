package com.viscriptshop;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    public static final ModConfigSpec CONFIG_SPEC;
    public static final ModConfigSpec CLIENT_CONFIG_SPEC;

    //是否打开FTB Library的按钮来允许打开商店
    public static ModConfigSpec.BooleanValue showFtbLibraryButton = null;

    /**
     * FTB 侧边栏按钮默认打开的商店路径。
     *
     * <p>该值由服务端读取，格式与 {@code /viscript_shop open} 后的商店参数一致；
     * 留空时显示快捷商店选择界面。
     */
    public static ModConfigSpec.ConfigValue<String> ftbDefaultShop;

    //是否使用MagicCoins的货币来替换本模组的货币
    public static ModConfigSpec.BooleanValue isReplaceMoneyToMagicCoin = null;

    //是否启用旧版本数据迁移（.shop文件）
    //当确认所有.shop文件都是最新版本后，可以关闭此选项以提升性能
    public static ModConfigSpec.BooleanValue enableLegacyDataMigration;

    // 商店UI单次购买最多给予玩家多少个物品
    // -1 表示不限制
    public static ModConfigSpec.IntValue maxShopUiGiveItemsPerPurchase;

    // 是否使用玩家独立库存
    // false 表示所有玩家共享库存，true 表示每个玩家单独消耗库存
    public static ModConfigSpec.BooleanValue isPersonalStock;

    // 商店 UI 的客户端主题
    public static ModConfigSpec.EnumValue<ShopUiTheme> shopUiTheme;

    static {
        ModConfigSpec.Builder CONFIG_BUILDER = new ModConfigSpec.Builder();
        CONFIG_BUILDER.push("config");
        if (ViscriptShop.isFtbLibraryLoaded()) {
            showFtbLibraryButton = CONFIG_BUILDER.define("showFtbLibraryButton", false);
        }
        ftbDefaultShop = CONFIG_BUILDER
                .translation("viscript_shop.configuration.ftbDefaultShop")
                .define("ftbDefaultShop", "");
        if (ViscriptShop.isMagicCoinsLoaded()) {
            isReplaceMoneyToMagicCoin = CONFIG_BUILDER.define("isReplaceMoneyToMagicCoin", false);
        }
        enableLegacyDataMigration = CONFIG_BUILDER.define("enableLegacyDataMigration", true);
        maxShopUiGiveItemsPerPurchase = CONFIG_BUILDER.defineInRange("maxShopUiGiveItemsPerPurchase", -1, -1, Integer.MAX_VALUE);
        isPersonalStock = CONFIG_BUILDER.define("isPersonalStock", false);
        CONFIG_BUILDER.pop();
        CONFIG_SPEC = CONFIG_BUILDER.build();

        ModConfigSpec.Builder CLIENT_CONFIG_BUILDER = new ModConfigSpec.Builder();
        CLIENT_CONFIG_BUILDER.push("client");
        shopUiTheme = CLIENT_CONFIG_BUILDER
                .translation("viscript_shop.configuration.shopUiTheme")
                .defineEnum("shopUiTheme", ShopUiTheme.GLASS_DARK);
        CLIENT_CONFIG_BUILDER.pop();
        CLIENT_CONFIG_SPEC = CLIENT_CONFIG_BUILDER.build();
    }

    public enum ShopUiTheme {
        GRAY_CAT_WORKSHOP,
        GLASS_DARK
    }
}

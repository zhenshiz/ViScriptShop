package com.viscriptshop.uitest;

import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.viscriptshop.Config;
import com.viscriptshop.ShopRegistries;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.ShopUI;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.promotion.PromotionRule;
import com.viscriptshop.promotion.condition.PlayerItemCondition;
import com.viscriptshop.promotion.condition.PromotionConditionEntry;
import com.viscriptshop.util.ShopHelper;
import com.viscriptshop.util.ViScriptShopServerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * 验证商店最大数量快捷操作同时受玩家支付能力和商品库存限制。
 *
 * <p>通过正式服务端入口打开商店，并使用服务端背包计数快照验证无限库存、双物品成本、
 * 同种成本合并和货币交易；测试结束后恢复玩家物品、余额和客户端配置。
 */
@LDLRegisterClient(name = "shop_maximum_quantity", group = ViscriptShop.MOD_ID,
        registry = UIScenario.REGISTRY, environment = RegistrationEnvironment.DEV_ONLY)
public final class ShopMaximumQuantityScenario implements UIScenario {
    private static final String SHOP = "__uitest_maximum_quantity";
    private static final String ADD_BUTTON = "#shop_merchant_add_0";
    private static final int MAXIMUM_MODIFIER = Minecraft.ON_OSX
            ? GLFW.GLFW_KEY_LEFT_SUPER : GLFW.GLFW_KEY_LEFT_CONTROL;

    @Override
    public void configure(ScenarioOptions options) {
        options.requiresWorld(true).guiScale(3).defaultTimeoutMs(15_000)
                .scenarioTimeoutMs(180_000).tags("shop", "quantity");
    }

    @Override
    public void define(ScenarioBuilder scenario) {
        scenario.server("准备支付物品与独立商店", context -> {
                    var player = context.player();
                    context.put("originalInventory", copyContainer(player.getInventory()));
                    context.put("originalEnderChest", copyContainer(player.getEnderChestInventory()));
                    context.put("originalMoney", player.getData(ShopRegistries.MONEY));
                    player.getInventory().clearContent();
                    player.getEnderChestInventory().clearContent();
                    player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 50));
                    player.getInventory().setItem(1, new ItemStack(Items.EMERALD, 12));
                    player.inventoryMenu.broadcastChanges();
                    player.setData(ShopRegistries.MONEY, new ShopRegistries.Money());
                    player.getData(ShopRegistries.MONEY).setCurrencyGridLayout(false);
                    ViScriptShopServerUtil.setMoney(player, 50);
                    ViScriptShopServerUtil.setShopInfo(SHOP, createShop());
                })
                .waitForSync("真实背包钻石数量同步", context ->
                                context.player().getInventory().countItem(Items.DIAMOND),
                        context -> context.requirePlayer().getInventory().countItem(Items.DIAMOND))
                .waitForSync("货币余额同步", context -> ViScriptShopServerUtil.getMoney(context.player()),
                        context -> context.requirePlayer().getData(ShopRegistries.MONEY).getMoney())
                .step("固定列表主题并保留客户端配置", context -> {
                    context.put("originalTheme", Config.shopUiTheme.get());
                    context.put("originalContentScale", Config.shopContentScale.get());
                    if (ShopHelper.cacheShopInfo != null) {
                        context.put("originalCachedCart", ShopHelper.cacheShopInfo);
                    }
                    Config.shopUiTheme.set(Config.ShopUiTheme.GLASS_DARK);
                    Config.shopContentScale.set(1.0);
                });

        openCase(scenario, "unlimited", 50);
        scenario.check("无限库存初始数量为零", context -> selectedMerchant(context).getBuyCount().intValue() == 0)
                .click(ADD_BUTTON)
                .check("普通点击只增加一份", context -> selectedMerchant(context).getBuyCount().intValue() == 1);
        selectMaximum(scenario, "无限库存按50钻石每份5计算", 10);
        scenario.click("#shop_merchant_remove_0")
                .check("最大数量仍可普通减一", context -> selectedMerchant(context).getBuyCount().intValue() == 9)
                .click(ADD_BUTTON)
                .check("普通加一恢复十份", context -> selectedMerchant(context).getBuyCount().intValue() == 10);

        openCase(scenario, "stock_15", 50);
        selectMaximum(scenario, "库存15时支付能力限制为10", 10);
        for (int count = 0; count < 5; count++) {
            scenario.click(ADD_BUTTON);
        }
        scenario.check("普通点击允许数量达到库存15", context -> selectedMerchant(context).getBuyCount().intValue() == 15)
                .click(ADD_BUTTON)
                .check("库存上限后普通点击不再增加", context -> selectedMerchant(context).getBuyCount().intValue() == 15);
        selectMaximum(scenario, "达到库存上限后快捷操作仍可下调至支付上限10", 10);
        openCase(scenario, "stock_5", 50);
        selectMaximum(scenario, "库存5时库存限制为5", 5);
        openCase(scenario, "dual_cost", 50);
        selectMaximum(scenario, "双成本取50除5与12除3的较小值", 4);
        openCase(scenario, "merged_cost", 50);
        selectMaximum(scenario, "同种钻石成本2加3合并后只能买10份", 10);
        openCase(scenario, "currency_sell", 50);
        selectMaximum(scenario, "出售交易使用结果物品每份5钻石作为成本", 10);
        openCase(scenario, "currency_buy", 50);
        selectMaximum(scenario, "货币购买按50余额每份5计算", 10);

        scenario.closeScreen()
                .server("准备数量改变时失效的消耗优惠券", context -> {
                    context.player().getInventory().setItem(0, new ItemStack(Items.DIAMOND, 64));
                    context.player().getInventory().setItem(2, new ItemStack(Items.DIAMOND, 36));
                    context.player().inventoryMenu.broadcastChanges();
                })
                .waitForSync("一百钻石背包同步", context ->
                                context.player().getInventory().countItem(Items.DIAMOND),
                        context -> context.requirePlayer().getInventory().countItem(Items.DIAMOND));
        openCase(scenario, "coupon_threshold", 100);
        selectMaximum(scenario, "每份消耗100钻石的优惠券失效后可按原价买20份", 20);

        scenario.closeScreen()
                .server("移除钻石并保留快照标记物品", context -> {
                    context.player().getInventory().setItem(0, ItemStack.EMPTY);
                    context.player().getInventory().setItem(2, ItemStack.EMPTY);
                    context.player().inventoryMenu.broadcastChanges();
                })
                .waitForSync("空钻石背包同步", context ->
                                context.player().getInventory().countItem(Items.DIAMOND),
                        context -> context.requirePlayer().getInventory().countItem(Items.DIAMOND));
        openCase(scenario, "unlimited", 0);
        scenario.click(ADD_BUTTON)
                .check("无支付物品时普通点击行为保留", context -> selectedMerchant(context).getBuyCount().intValue() == 1);
        selectMaximum(scenario, "没有钻石时最大数量清零", 0);
        scenario.checkServer("数量快捷操作没有消耗玩家物品", context ->
                        context.player().getInventory().countItem(Items.EMERALD) == 12)
                .checkServer("数量快捷操作没有扣款", context -> ViScriptShopServerUtil.getMoney(context.player()) == 50)
                .teardown("恢复客户端配置与按键", context -> {
                    context.input().keyUp(MAXIMUM_MODIFIER, 0);
                    Config.shopUiTheme.set(context.get("originalTheme", Config.ShopUiTheme.GLASS_DARK));
                    Config.shopContentScale.set(context.get("originalContentScale", 1.0));
                    context.mc().setScreen(null);
                    ShopHelper.cacheShopInfo = context.get("originalCachedCart");
                })
                .teardownServer("恢复背包余额并删除测试商店", context -> {
                    List<ItemStack> inventory = context.get("originalInventory");
                    List<ItemStack> enderChest = context.get("originalEnderChest");
                    if (inventory != null) restoreContainer(context.player().getInventory(), inventory);
                    if (enderChest != null) restoreContainer(context.player().getEnderChestInventory(), enderChest);
                    if (context.get("originalMoney") instanceof ShopRegistries.Money money) {
                        context.player().setData(ShopRegistries.MONEY, money);
                    }
                    context.player().inventoryMenu.broadcastChanges();
                    ViscriptShop.getShopSavedData().resetShopInfo(SHOP);
                });
    }

    private static void openCase(ScenarioBuilder scenario, String categoryId, long diamondCount) {
        scenario.closeScreen()
                .step("清除上一案例购物车", context -> ShopHelper.cacheShopInfo = null)
                .server("正式入口打开" + categoryId, context ->
                        ViScriptShopServerUtil.serverOpenShop(context.player(), SHOP, categoryId, null))
                .awaitModularUI().awaitElement(ADD_BUTTON)
                .waitUntil("完整支付物品快照同步" + categoryId, context ->
                        snapshotCount(context, Items.DIAMOND) == diamondCount
                                && snapshotCount(context, Items.EMERALD) == 12)
                .check("打开指定分类" + categoryId, context ->
                        categoryId.equals(currentUi(context).getSelectedCategory().getId()))
                .check("列表商品初始数量为零", context -> selectedMerchant(context).getBuyCount().intValue() == 0);
    }

    private static void selectMaximum(ScenarioBuilder scenario, String description, int expected) {
        scenario.keyDown(MAXIMUM_MODIFIER).click(ADD_BUTTON).keyUp(MAXIMUM_MODIFIER)
                .waitUntil(description, context -> selectedMerchant(context).getBuyCount().intValue() == expected)
                .check(description, context -> selectedMerchant(context).getBuyCount().intValue() == expected);
    }

    private static ShopUI currentUi(TestContext context) {
        return (ShopUI) context.requireUI().ui.rootElement;
    }

    private static MerchantInfo selectedMerchant(TestContext context) {
        return currentUi(context).getSelectedCategory().getMerchants().getFirst();
    }

    private static long snapshotCount(TestContext context, Item item) {
        return currentUi(context).playerItems.stream()
                .filter(entry -> entry.getItemStack().is(item))
                .mapToLong(entry -> entry.getCount()).sum();
    }

    private static List<ItemStack> copyContainer(Container container) {
        List<ItemStack> items = new ArrayList<>();
        for (int index = 0; index < container.getContainerSize(); index++) {
            items.add(container.getItem(index).copy());
        }
        return items;
    }

    private static void restoreContainer(Container container, List<ItemStack> items) {
        container.clearContent();
        for (int index = 0; index < items.size(); index++) {
            container.setItem(index, items.get(index).copy());
        }
        container.setChanged();
    }

    private static ShopInfo createShop() {
        ShopInfo shop = new ShopInfo();
        shop.setName("最大购买数量测试商店");
        addItemCategory(shop, "unlimited", -1, 5, ItemStack.EMPTY);
        addItemCategory(shop, "stock_15", 15, 5, ItemStack.EMPTY);
        addItemCategory(shop, "stock_5", 5, 5, ItemStack.EMPTY);
        addItemCategory(shop, "dual_cost", -1, 5, new ItemStack(Items.EMERALD, 3));
        addItemCategory(shop, "merged_cost", -1, 2, new ItemStack(Items.DIAMOND, 3));
        addItemCategory(shop, "coupon_threshold", -1, 5, ItemStack.EMPTY);
        MerchantInfo couponMerchant = shop.getCategoryInfos().getLast().getMerchants().getFirst();
        couponMerchant.setPromotionEnabled(true);
        PromotionRule discount = new PromotionRule();
        discount.setPercentage(50);
        PromotionConditionEntry conditionEntry = new PromotionConditionEntry(PlayerItemCondition.ID);
        PlayerItemCondition coupon = (PlayerItemCondition) conditionEntry.getCondition();
        coupon.setItem(new ItemStack(Items.DIAMOND, 100));
        coupon.setConsumeOnPurchase(true);
        discount.getConditions().add(conditionEntry);
        couponMerchant.getPromotionRules().add(discount);
        for (MerchantInfo.TradeType tradeType : MerchantInfo.TradeType.values()) {
            CategoryInfo category = new CategoryInfo();
            category.setId(tradeType == MerchantInfo.TradeType.BUY ? "currency_buy" : "currency_sell");
            category.setName(category.getId());
            category.setShopType(CategoryInfo.ShopType.CURRENCY);
            MerchantInfo merchant = new MerchantInfo();
            merchant.setId("currency");
            merchant.setTradeType(tradeType);
            merchant.setMoney(5);
            merchant.setItemResult(new ItemStack(Items.DIAMOND, 5));
            category.getMerchants().add(merchant);
            shop.getCategoryInfos().add(category);
        }
        return shop;
    }

    private static void addItemCategory(ShopInfo shop, String id, int stock, int diamondCost, ItemStack secondCost) {
        CategoryInfo category = new CategoryInfo();
        category.setId(id);
        category.setName(id);
        category.setShopType(CategoryInfo.ShopType.ITEM_FOR_ITEM);
        MerchantInfo merchant = new MerchantInfo();
        merchant.setId("item");
        merchant.setStock(stock);
        merchant.setItemA(new ItemStack(Items.DIAMOND, diamondCost));
        merchant.setItemB(secondCost);
        merchant.setItemResult(new ItemStack(Items.BREAD));
        category.getMerchants().add(merchant);
        shop.getCategoryInfos().add(category);
    }
}

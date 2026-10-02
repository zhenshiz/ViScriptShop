package com.viscriptshop.uitest;

import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ElementBounds;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.viscriptshop.Config;
import com.viscript_lib.util.item.ViScriptItemStack;
import com.viscriptshop.promotion.PromotionRule;
import com.viscriptshop.ShopRegistries;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.ShopUI;
import com.viscriptshop.gui.layout.ShopUiScale;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.util.ShopHelper;
import com.viscriptshop.util.ViScriptShopClientUtil;
import com.viscriptshop.util.ViScriptShopServerUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/** 验证商店独立缩放、数量显示及窗口调整后的点击命中。 */
@LDLRegisterClient(name = "shop_independent_scale", group = ViscriptShop.MOD_ID,
        registry = UIScenario.REGISTRY, environment = RegistrationEnvironment.DEV_ONLY)
public final class ShopUiScaleScenario implements UIScenario {
    private static final String SHOP = "uitest/scale_capture";

    @Override
    public void configure(ScenarioOptions options) {
        options.requiresWorld(true).defaultTimeoutMs(15000).scenarioTimeoutMs(180000);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.server("准备独立测试商店", ctx -> {
            ViScriptShopServerUtil.setShopInfo(SHOP, shop());
            ctx.player().getData(ShopRegistries.MONEY).setCurrencyGridLayout(false);
        });
        s.runCommand("time set noon").runCommand("gamerule doDaylightCycle false")
                .runCommand("weather clear")
                .step("固定主题与视角", ctx -> {
                    ctx.put("originalTheme", Config.shopUiTheme.get());
                    ctx.put("originalContentScale", Config.shopContentScale.get());
                    Config.shopUiTheme.set(Config.ShopUiTheme.GLASS_DARK);
                    ctx.requirePlayer().setXRot(20);
                    ctx.requirePlayer().setYRot(180);
                });
        capture(s, 1280, 720, 1, 1.0);
        capture(s, 1280, 720, 3, 1.0);
        capture(s, 1920, 1080, 1, 1.0);
        capture(s, 1920, 1080, 4, 1.0);
        capture(s, 1920, 1080, 0, 1.0);
        capture(s, 960, 540, 2, 1.0);
        capture(s, 1024, 768, 3, 1.0);
        capture(s, 1920, 810, 3, 1.0);
        capture(s, 1280, 720, 3, 0.75);
        verifyHeaderInteractions(s);
        capture(s, 1280, 720, 3, 1.25);
        verifyCurrency(s);
        capture(s, 1280, 720, 3, 1.5);
        capture(s, 1920, 810, 3, 1.5);
        capture(s, 1280, 720, 3, 0.5);
        verifyHeaderInteractions(s);
        verifyCurrency(s);
        s.step("切换灰猫主题", ctx -> Config.shopUiTheme.set(Config.ShopUiTheme.GRAY_CAT_WORKSHOP));
        capture(s, 1280, 720, 1, 1.0);
        capture(s, 1280, 720, 3, 1.0);
        capture(s, 1280, 720, 1, 0.75);
        capture(s, 1280, 720, 3, 0.75);
        verifyHeaderInteractions(s);
        capture(s, 1280, 720, 3, 1.25);
        verifyCurrency(s);
        s.click("#shop_item_search")
                .waitUntil("灰猫物品搜索下拉框打开", ctx -> ((ShopUI) ctx.requireUI().ui.rootElement).searchComponent.isOpen())
                .step("检查灰猫搜索下拉框边界", ctx -> {
                    var search = ((ShopUI) ctx.requireUI().ui.rootElement).searchComponent;
                    var anchor = ElementBounds.of(search);
                    var popup = ElementBounds.of(search.dialog);
                    ctx.check("搜索下拉框跟随缩放后的输入框", Math.abs(popup.x() - anchor.x()) < 2);
                    ctx.check("搜索下拉框处于屏幕内", popup.x() >= 0 && popup.y() >= 0
                            && popup.right() <= ctx.mc().getWindow().getGuiScaledWidth()
                            && popup.bottom() <= ctx.mc().getWindow().getGuiScaledHeight());
                }).screenshot("gray-search-popup")
                .click("#shop_search_mode_toggle").checkVisible("#shop_id_search")
                .click("#shop_search_mode_toggle").checkVisible("#shop_item_search");
        capture(s, 1280, 720, 3, 0.5);
        verifyHeaderInteractions(s);
        verifyCurrency(s);
        capture(s, 960, 540, 2, 1.25);
        capture(s, 1280, 720, 3, 1.5);
        capture(s, 1920, 810, 3, 1.5);
        capture(s, 1024, 768, 3, 1.0);
        capture(s, 1920, 810, 3, 1.0);
        s.step("恢复玻璃主题", ctx -> Config.shopUiTheme.set(Config.ShopUiTheme.GLASS_DARK));
        capture(s, 1280, 720, 3, 1.0);
        s.step("窗口改变时保留已打开商店", ctx -> resize(ctx, 960, 540, 2))
                .waitUntil("窗口调整完成", ctx -> ctx.mc().getWindow().getWidth() == 960)
                .ticks(10).step("检查窗口调整后的布局", ctx -> verifyLayout(ctx, "live-resize"))
                .click("#shop_merchant_add_0")
                .check("窗口调整后仍能加入购物车", ctx -> currentShop(ctx).getCategoryInfos().getFirst()
                        .getMerchants().getFirst().getBuyCount().intValue() == 1);
        s.teardown("恢复主题", ctx -> {
            Config.shopUiTheme.set(ctx.get("originalTheme", Config.ShopUiTheme.GLASS_DARK));
            Config.shopContentScale.set(ctx.get("originalContentScale", 1.0));
            ShopHelper.cacheShopInfo = null;
            ctx.mc().setScreen(null);
        });
    }

    private void capture(ScenarioBuilder s, int width, int height, int guiScale, double contentScale) {
        String label = width + "x" + height + "-gui-" + guiScale + "-content-" + contentScale;
        s.group(label, group -> group.closeScreen()
                .step("设置窗口与内容缩放", ctx -> {
                    Config.shopContentScale.set(contentScale);
                    resize(ctx, width, height, guiScale);
                })
                .waitUntil("实际分辨率达到" + width + "x" + height,
                        ctx -> ctx.mc().getWindow().getWidth() == width && ctx.mc().getWindow().getHeight() == height)
                .ticks(10)
                .step("从正式入口打开商店", ctx -> {
                    ShopHelper.cacheShopInfo = null;
                    ViScriptShopClientUtil.clientOpenShop(SHOP, shop(), null, null);
                }).awaitModularUI().awaitElement("#shop_buy_button").ticks(10)
                .hoverAt(0, 0)
                .step("验证独立布局", ctx -> verifyLayout(ctx, label))
                .screenshot(label)
                .step("滚动查看结算区末尾物品", ctx -> {
                    ShopUI ui = (ShopUI) ctx.requireUI().ui.rootElement;
                    ui.shoppingCarView.scrollToChild(ui.shoppingCarView.viewContainer.getChildren().getLast());
                    ui.inventoryView.scrollToChild(ui.inventoryView.viewContainer.getChildren().getLast());
                }).ticks(2)
                .step("结算区末尾数量可完整查看", ctx -> {
                    ShopUI ui = (ShopUI) ctx.requireUI().ui.rootElement;
                    for (var view : new com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView[]{ui.shoppingCarView, ui.inventoryView}) {
                        var last = ElementBounds.of(view.viewContainer.getChildren().getLast());
                        var viewport = ElementBounds.of(view.viewPort);
                        ctx.check("滚动后最后一项完整可见", last.y() >= viewport.y() - 0.1
                                && last.bottom() <= viewport.bottom() + 0.1);
                    }
                })
                .click("#shop_merchant_add_0")
                .check("缩放后加号命中并增加数量", ctx -> currentShop(ctx).getCategoryInfos().getFirst()
                        .getMerchants().getFirst().getBuyCount().intValue() == 2)
                .click("#shop_clear_button")
                .check("缩放后清空按钮命中", ctx -> currentShop(ctx).getCategoryInfos().stream()
                        .flatMap(c -> c.getMerchants().stream()).allMatch(m -> m.getBuyCount().intValue() == 0)));
    }

    private static void resize(TestContext ctx, int width, int height, int guiScale) {
        var window = ctx.mc().getWindow();
        int[] windowWidth = new int[1];
        int[] windowHeight = new int[1];
        GLFW.glfwGetWindowSize(window.getWindow(), windowWidth, windowHeight);
        float pixelRatio = (float) window.getWidth() / windowWidth[0];
        GLFW.glfwRestoreWindow(window.getWindow());
        GLFW.glfwSetWindowSize(window.getWindow(), Math.round(width / pixelRatio), Math.round(height / pixelRatio));
        ctx.mc().options.guiScale().set(guiScale);
        ctx.mc().resizeDisplay();
    }

    private static void verifyLayout(TestContext ctx, String label) {
        var window = ctx.mc().getWindow();
        float guiScale = (float) window.getGuiScale();
        float effectiveScale = ShopUiScale.contentScale();
        var shell = ctx.el("#shop_ui_shell").bounds();
        boolean glass = Config.shopUiTheme.get() == Config.ShopUiTheme.GLASS_DARK;
        if (glass) {
            ctx.check("外框高度保持91%", Math.abs(shell.height() * guiScale / window.getHeight() - 0.91) < 0.005);
        }
        ctx.check("商店整体居中且不超出屏幕", shell.x() >= 0 && shell.right() <= window.getGuiScaledWidth()
                && Math.abs((shell.x() + shell.right()) * guiScale / 2 - window.getWidth() / 2f) <= 2.1);
        String shellKey = "shell:" + window.getWidth() + "x" + window.getHeight() + ":" + Config.shopUiTheme.get();
        float[] originalShell = ctx.get(shellKey);
        if (originalShell == null) {
            ctx.put(shellKey, new float[]{shell.width() * guiScale, shell.height() * guiScale});
        } else {
            ctx.check("改变内容倍率不缩放外框高度", Math.abs(shell.height() * guiScale - originalShell[1]) < 3);
        }
        var add = ctx.el("#shop_merchant_add_0").bounds();
        String sizeKey = "content:" + window.getWidth() + "x" + window.getHeight() + ":" + Config.shopUiTheme.get();
        Float originalSize = ctx.get(sizeKey);
        if (Config.shopContentScale.get() == 1.0) {
            ctx.put(sizeKey, add.height() * guiScale);
        } else if (originalSize != null) {
            double expectedHeight = originalSize * effectiveScale;
            ctx.check("内容倍率实际改变按钮大小", Math.abs(add.height() * guiScale - expectedHeight) < 1,
                    expectedHeight, add.height() * guiScale);
        }
        var card = ctx.el("#shop_merchant_list_0").bounds();
        String cardKey = "card:" + window.getWidth() + "x" + window.getHeight() + ":" + Config.shopUiTheme.get();
        float[] originalCard = ctx.get(cardKey);
        if (Config.shopContentScale.get() == 1.0) {
            ctx.put(cardKey, new float[]{card.width() * guiScale, card.height() * guiScale});
        } else if (originalCard != null) {
            ctx.check("卡片宽度随实际内容倍率缩放", Math.abs(card.width() * guiScale - originalCard[0] * effectiveScale) < 3);
            ctx.check("卡片高度随实际内容倍率缩放", Math.abs(card.height() * guiScale - originalCard[1] * effectiveScale) < 3);
        }
        verifyCards(ctx);
        verifyHeader(ctx);
        var merchantViewport = ElementBounds.of(((ShopUI) ctx.requireUI().ui.rootElement).merchantsView.viewPort);
        var column = ctx.el("#shop_ui_merchant_column").bounds();
        var header = ctx.el("#shop_ui_merchant_header").bounds();
        var summary = ctx.el("#shop_ui_summary").bounds();
        var categories = ctx.el("#shop_ui_categories").bounds();
        float scale = add.height() / 14;
        ctx.check("中间栏仅保留卡片与滚动条所需宽度", column.width() >= card.width()
                && column.width() - card.width() <= 18 * scale);
        ctx.check("搜索栏与商品栏等宽", Math.abs(header.width() - column.width()) < 0.1);
        ctx.check("结算区紧邻中间栏且位于外框内", summary.x() >= column.right()
                && summary.x() - column.right() <= 4 * scale / effectiveScale
                && summary.right() <= shell.right() + 0.1);
        int visibleRows = 0;
        for (int i = 0; ctx.exists("#shop_merchant_list_" + i); i++) {
            var row = ctx.el("#shop_merchant_list_" + i).bounds();
            if (row.y() >= merchantViewport.y() - 0.1 && row.bottom() <= merchantViewport.bottom() + 0.1) {
                visibleRows++;
            }
        }
        String columnKey = "column:" + window.getWidth() + "x" + window.getHeight() + ":" + Config.shopUiTheme.get();
        float[] originalColumn = ctx.get(columnKey);
        if (Config.shopContentScale.get() == 1.0) {
            ctx.put(columnKey, new float[]{column.width() * guiScale, column.height() * guiScale,
                    summary.width() * guiScale, visibleRows, categories.width() * guiScale, shell.width() * guiScale});
        } else if (originalColumn != null) {
            ctx.check("中间栏宽度随卡片缩放", Math.abs(column.width() * guiScale
                    - originalColumn[0] * effectiveScale) < 3);
            ctx.check("中间栏高度不随内容倍率缩放", Math.abs(column.height() * guiScale - originalColumn[1]) < 3);
            ctx.check("右侧结算区宽度保持不变", Math.abs(summary.width() * guiScale - originalColumn[2]) < 3);
            ctx.check("左侧分类区宽度保持不变", Math.abs(categories.width() * guiScale - originalColumn[4]) < 3);
            ctx.check("外框宽度仅随中间栏增减", Math.abs((shell.width() - column.width()) * guiScale
                    - (originalColumn[5] - originalColumn[0])) < 3);
            if (Config.shopContentScale.get() < 1.0) {
                ctx.check("缩小内容后商店整体收窄", shell.width() * guiScale < originalColumn[5]);
                ctx.check("保持列表高度可显示更多完整商品行", visibleRows > originalColumn[3]);
            }
        }
        ctx.log("外框=" + shell + "，中间栏=" + column + "，右侧=" + summary + "，完整商品行=" + visibleRows);
        ctx.check("商品数量按钮没有横向溢出", add.x() >= merchantViewport.x() - 0.1f
                && add.right() <= merchantViewport.right() + 0.1f);
        var buy = ctx.el("#shop_buy_button").bounds();
        ctx.check("购买按钮完整位于屏幕内", buy.x() >= 0 && buy.y() >= 0
                && buy.right() <= window.getGuiScaledWidth() && buy.bottom() <= window.getGuiScaledHeight());
        ShopUI root = (ShopUI) ctx.requireUI().ui.rootElement;
        checkCounts(ctx, "#shop_cart_item_count_", ElementBounds.of(root.shoppingCarView.viewPort));
        checkCounts(ctx, "#shop_cost_item_count_", ElementBounds.of(root.inventoryView.viewPort));
        String key = window.getWidth() + "x" + window.getHeight() + ":" + Config.shopUiTheme.get() + ":" + Config.shopContentScale.get();
        for (String selector : new String[]{"#shop_ui_shell", "#shop_ui_summary", "#shop_merchant_add_0"}) {
            var bounds = ctx.el(selector).bounds();
            float[] pixels = {bounds.x() * guiScale, bounds.y() * guiScale,
                    bounds.width() * guiScale, bounds.height() * guiScale};
            float[] baseline = ctx.get(key + selector);
            if (baseline == null) {
                ctx.put(key + selector, pixels);
            } else {
                for (int i = 0; i < pixels.length; i++) {
                    ctx.check("原版GUI改变不影响" + selector + "的像素范围[" + i + "]",
                            Math.abs(pixels[i] - baseline[i]) < 3, baseline[i], pixels[i]);
                }
            }
        }
        ctx.log(label + " theme=" + Config.shopUiTheme.get() + " gui=" + guiScale
                + " root=" + root.getSizeWidth() + "x" + root.getSizeHeight()
                + " summary=" + ctx.el("#shop_ui_summary").bounds());
    }

    private static void verifyCards(TestContext ctx) {
        for (int i = 0; i < 3; i++) {
            var card = ctx.el("#shop_merchant_list_" + i).bounds();
            var index = ctx.el("#shop_merchant_index_" + i).bounds();
            var first = ctx.el("#shop_merchant_list_first_" + i).bounds();
            var arrow = ctx.el("#shop_merchant_arrow_" + i).bounds();
            var result = ctx.el("#shop_merchant_list_third_" + i).bounds();
            var gift = ctx.el("#shop_merchant_list_gift_slot_" + i).bounds();
            var action = ctx.el("#shop_merchant_action_" + i).bounds();
            float scale = ctx.el("#shop_merchant_add_" + i).bounds().height() / 14;
            ctx.check("序号紧邻第一件商品 " + i, first.x() >= index.right()
                    && first.x() - index.right() <= 10 * scale);
            ctx.check("成本、产出、赠品、数量区不重叠 " + i,
                    first.right() <= arrow.x() + 0.1 && arrow.right() <= result.x() + 0.1
                            && result.right() <= gift.x() + 0.1 && gift.right() <= action.x() + 0.1);
            ctx.check("操作区完整位于卡片内 " + i, action.right() <= card.right() - 2 * scale);
            if (i < 2) {
                var preview = ctx.el("#shop_merchant_list_" + i + "_gift_0").bounds();
                ctx.check("真实赠品图标和数量完整位于赠品栏 " + i,
                        preview.x() >= gift.x() - 0.1 && preview.right() <= gift.right() + 0.1
                                && preview.y() >= card.y() && preview.bottom() <= card.bottom());
            } else {
                ctx.check("无赠品商品仍保留对齐空间", !ctx.exists("#shop_merchant_list_2_gift_0"));
            }
        }
    }

    private static void verifyHeader(TestContext ctx) {
        var window = ctx.mc().getWindow();
        float guiScale = (float) window.getGuiScale();
        String key = "header:" + window.getWidth() + "x" + window.getHeight() + ":" + Config.shopUiTheme.get();
        for (String selector : new String[]{"#shop_ui_category_header", "#shop_ui_merchant_header",
                "#shop_ui_summary_header", "#shop_category_title", "#shop_title", "#shop_item_search",
                "#shop_search_icon", "#shop_search_mode_toggle", "#shop_player_head_icon"}) {
            var bounds = ctx.el(selector).bounds();
            float height = bounds.height() * guiScale;
            Float originalHeight = ctx.get(key + selector);
            if (originalHeight == null) {
                ctx.put(key + selector, height);
            } else {
                ctx.check("顶栏高度与控件大小不随内容倍率变化：" + selector,
                        Math.abs(height - originalHeight) < 1, originalHeight, height);
            }
        }
        var header = ctx.el("#shop_ui_merchant_header").bounds();
        var search = ctx.el("#shop_item_search").bounds();
        var mode = ctx.el("#shop_search_mode_toggle").bounds();
        var avatar = ctx.el("#shop_player_head_icon").bounds();
        ctx.check("搜索框、切换按钮与头像不重叠", search.right() <= mode.x() + 0.1 && mode.right() <= avatar.x());
        ctx.check("头像完整位于顶栏内", avatar.right() <= header.right() + 0.1
                && avatar.y() >= header.y() && avatar.bottom() <= header.bottom());
        if (((ShopUI) ctx.requireUI().ui.rootElement).getSelectedCategory().getShopType() == CategoryInfo.ShopType.CURRENCY) {
            var toggle = ctx.el("#shop_currency_layout_toggle").bounds();
            ctx.check("窄顶栏仍容纳货币布局按钮", toggle.x() >= mode.right() && toggle.right() <= avatar.x());
        }
    }

    private static void verifyHeaderInteractions(ScenarioBuilder s) {
        s.click("#shop_item_search")
                .waitUntil("固定大小的搜索框可点击", ctx -> ((ShopUI) ctx.requireUI().ui.rootElement).searchComponent.isOpen())
                .ticks(4).step("校验搜索下拉框", ctx -> {
                    var search = ((ShopUI) ctx.requireUI().ui.rootElement).searchComponent;
                    var anchor = ElementBounds.of(search);
                    var popup = ElementBounds.of(search.dialog);
                    ctx.check("下拉框紧贴搜索框", Math.abs(popup.x() - anchor.x()) < 2
                            && Math.abs(popup.y() - anchor.bottom()) < 2);
                    ctx.check("下拉框完整位于屏幕内", popup.x() >= 0 && popup.y() >= 0
                            && popup.right() <= ctx.mc().getWindow().getGuiScaledWidth()
                            && popup.bottom() <= ctx.mc().getWindow().getGuiScaledHeight());
                }).screenshot("fixed-header-search")
                .click("#shop_search_mode_toggle").checkVisible("#shop_id_search")
                .click("#shop_search_mode_toggle").checkVisible("#shop_item_search");
    }

    private static void verifyCurrency(ScenarioBuilder s) {
        s.step("切换带赠品的货币买入卖出列表", ctx -> {
                    ShopUI ui = (ShopUI) ctx.requireUI().ui.rootElement;
                    ui.setSelectedCategory(ui.currentShopInfo.getCategoryInfos().get(1));
                    ui.reloadMerchants();
                }).ticks(4)
                .step("校验货币列表与赠品布局", ShopUiScaleScenario::verifyCards)
                .hover("#shop_merchant_list_0_gift_0_icon").screenshot("currency-gift-hover")
                .hoverAt(0, 0).screenshot("currency-gifts")
                .click("#shop_merchant_add_1")
                .check("货币卖出数量按钮可点击", ctx -> currentShop(ctx).getCategoryInfos().get(1)
                        .getMerchants().get(1).getBuyCount().intValue() == 1)
                .click("#shop_clear_button")
                .click("#shop_currency_layout_toggle").awaitElement("#shop_merchant_grid_0").ticks(4)
                .step("网格切换后保留可用宽度", ctx -> {
                    var grid = ctx.el("#shop_merchant_grid_0").bounds();
                    var viewport = ElementBounds.of(((ShopUI) ctx.requireUI().ui.rootElement).merchantsView.viewPort);
                    ctx.check("网格卡片完整位于商品区域", grid.x() >= viewport.x()
                            && grid.right() <= viewport.right() + 0.1);
                })
                .click("#shop_currency_layout_toggle").awaitElement("#shop_merchant_list_0").ticks(4)
                .step("切回列表恢复紧凑宽度", ctx -> verifyLayout(ctx, "currency-list-restored"));
    }

    private static void checkCounts(TestContext ctx, String prefix, ElementBounds viewport) {
        for (int i = 0; ctx.exists(prefix + i); i++) {
            var label = ctx.el(prefix + i).bounds();
            ctx.check("数量标签不被视口裁切：" + prefix + i,
                    label.x() >= viewport.x() - 0.1f && label.right() <= viewport.right() + 0.1f);
        }
    }

    private static ShopInfo currentShop(TestContext ctx) {
        return ((ShopUI) ctx.requireUI().ui.rootElement).currentShopInfo;
    }

    private static ShopInfo shop() {
        ShopInfo shop = new ShopInfo();
        shop.setName("布丁商店");
        String[] names = {"矿石兑换", "建筑材料", "食物补给", "工具装备"};
        Item[] icons = {Items.IRON_INGOT, Items.BRICKS, Items.BREAD, Items.IRON_PICKAXE};
        Item[] outputs = {Items.COPPER_INGOT, Items.IRON_INGOT, Items.DIAMOND, Items.EMERALD,
                Items.REDSTONE, Items.LAPIS_LAZULI, Items.QUARTZ, Items.AMETHYST_SHARD};
        Item[] costs = {Items.IRON_INGOT, Items.GOLD_INGOT, Items.PRISMARINE_SHARD, Items.COAL};
        for (int c = 0; c < names.length; c++) {
            CategoryInfo category = new CategoryInfo();
            category.setId("scale-category-" + c);
            category.setName(names[c]);
            category.setIconItem(new ItemStack(icons[c]));
            if (c == 1) category.setShopType(CategoryInfo.ShopType.CURRENCY);
            for (int i = 0; i < 16; i++) {
                MerchantInfo merchant = new MerchantInfo();
                merchant.setId("scale-merchant-" + c + "-" + i);
                merchant.setItemA(new ItemStack(costs[i % costs.length], i == 0 ? 222300000 : 1 + i % 3));
                merchant.setItemB(i == 1 ? ItemStack.EMPTY : new ItemStack(costs[(i + 1) % costs.length], 1));
                merchant.setItemResult(new ItemStack(outputs[i % outputs.length], i == 0 ? 88900 : 2));
                merchant.setBuyCount(c == 0 && i < 3 ? 1 : 0);
                merchant.setMoney(123456.78);
                merchant.setTradeType(i % 2 == 0 ? MerchantInfo.TradeType.BUY : MerchantInfo.TradeType.SELL);
                if (i < 2) {
                    merchant.setPromotionEnabled(true);
                    PromotionRule gift = new PromotionRule();
                    gift.setType(PromotionRule.PromotionType.BUY_GET);
                    gift.setBuyThreshold(3);
                    gift.setGiftCount(5);
                    gift.setSerializedGiftItem(new ViScriptItemStack(new ItemStack(Items.GOLDEN_APPLE)));
                    merchant.getPromotionRules().add(gift);
                    PromotionRule discount = new PromotionRule();
                    discount.setPercentage(20);
                    merchant.getPromotionRules().add(discount);
                }
                category.getMerchants().add(merchant);
            }
            shop.getCategoryInfos().add(category);
        }
        return shop;
    }
}

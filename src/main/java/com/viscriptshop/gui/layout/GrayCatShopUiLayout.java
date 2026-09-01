package com.viscriptshop.gui.layout;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SearchComponent;
import com.lowdragmc.lowdraglib2.math.Size;
import com.viscriptshop.gui.components.theme.ShopTheme;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.world.item.ItemStack;

public final class GrayCatShopUiLayout implements ShopUiLayout {
    private static final float SHELL_WIDTH = 523;
    private static final float SHELL_HEIGHT = 276;
    private static final float SCROLLER_TOP_INSET = 4;
    private final SearchComponent<ItemStack> itemSearch;

    public GrayCatShopUiLayout(SearchComponent<ItemStack> itemSearch) {
        this.itemSearch = itemSearch;
    }

    @Override
    public UIElement build(ShopTheme theme, ShopUiElements elements) {
        UIElement root = new UIElement()
                .setId("shop_ui_shell")
                .addClass(theme.styleClass())
                .layout(layout -> {
                    layout.width(SHELL_WIDTH);
                    layout.height(SHELL_HEIGHT);
                    layout.gapAll(0);
                    layout.positionType(TaffyPosition.RELATIVE);
                    layout.justifyContent(AlignContent.FLEX_START);
                    layout.alignItems(AlignItems.FLEX_START);
                }).style(style -> style.backgroundTexture(theme.shellBackground()));

        UIElement categories = createCategoryColumn(theme, elements);
        UIElement merchants = createMerchantColumn(theme, elements);
        UIElement summary = createSummaryColumn(theme, elements);
        absolute(categories, 6, 6, 114, 265);
        absolute(merchants, 128, 6, 273, 265);
        absolute(summary, 398, 6, 125, 265);
        root.addChildren(categories, merchants, summary);
        return root;
    }

    @Override
    public void initScreen(UIElement shell, Size layoutSize) {
        float shellScale = Math.min(
                layoutSize.getWidth() * 0.9f / SHELL_WIDTH,
                layoutSize.getHeight() * 0.91f / SHELL_HEIGHT
        );
        shell.transform(transform -> transform.pivot(0.5f, 0.5f).scale(shellScale));
        itemSearch.dialog.transform(transform -> transform.pivot(0, 0).scale(shellScale));
    }

    private UIElement createCategoryColumn(ShopTheme theme, ShopUiElements elements) {
        UIElement column = new UIElement().setId("shop_ui_categories");
        UIElement header = new UIElement();
        UIElement body = new UIElement();
        UIElement categoryList = new UIElement().addChild(elements.categoryView());
        UIElement balance = new UIElement().layout(layout -> {
            layout.alignItems(AlignItems.CENTER);
            layout.flexDirection(FlexDirection.ROW);
        }).addChildren(
                new UIElement().layout(layout -> {
                    layout.width(23);
                    layout.heightPercent(100);
                    layout.justifyContent(AlignContent.CENTER);
                    layout.alignItems(AlignItems.CENTER);
                }).addChild(elements.balanceIcon()),
                elements.balanceValue()
        );

        elements.categoryTitle().textStyle(textStyle -> textStyle
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER));
        absolute(elements.categoryTitle(), 0, -1, 114, 25);
        header.addChild(elements.categoryTitle());

        elements.categoryView().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        // 灰猫主题的滚动条宽七像素，需要放在猫形选中项旁边，避免碰到其右侧的“猫眼”装饰。
        // 保持水平总边距不变，避免滚动条出现时改变分类视口的宽度。
        elements.categoryView().verticalScroller.layout(layout -> {
            layout.marginLeft(2);
            layout.marginRight(1);
            layout.marginTop(SCROLLER_TOP_INSET);
        });
        elements.categoryView().viewPort.getLayout().paddingAll(2);
        elements.balanceIcon().layout(layout -> {
            layout.width(theme.balanceIconSize());
            layout.height(theme.balanceIconSize());
        });
        elements.balanceValue().textStyle(textStyle -> textStyle
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER));
        elements.balanceValue().layout(layout -> {
            layout.flex(1);
            layout.heightPercent(100);
            layout.marginRight(theme.balanceFieldMarginRight());
        });
        elements.balanceValue().transform(transform -> transform.translate(0, 2));

        absolute(header, 0, 0, 114, 25);
        absolute(body, 0, 25, 114, 240);
        absolute(categoryList, 3, 2, 108, 208);
        absolute(balance, 6, 216, 108, 19);
        body.addChildren(categoryList, balance);
        column.addChildren(header, body);
        return column;
    }

    private UIElement createMerchantColumn(ShopTheme theme, ShopUiElements elements) {
        UIElement column = new UIElement();
        UIElement head = new UIElement();
        UIElement searchControls = new UIElement().addChildren(
                elements.searchIcon(),
                elements.itemSearch(),
                elements.idSearch(),
                elements.searchModeToggle(),
                elements.currencyLayoutToggle()
        );
        UIElement body = new UIElement().setId("shop_ui_merchants")
                .addChild(elements.merchantsView());

        absolute(head, 0, 0, 273, 28);
        absolute(searchControls, 0, 0, 273, 28);
        absolute(elements.searchIcon(), 7, 1, 20, 25);
        elements.searchIcon().getLayout().marginAll(0);
        absolute(elements.itemSearch(), 38, 5, 70, 19);
        absolute(elements.idSearch(), 38, 5, 70, 19);
        elements.itemSearch().dialog.layout(layout -> {
            layout.minWidth(70);
            layout.maxWidth(70);
        });
        elements.itemSearch().dialog.setOverflowVisible(false);
        elements.itemSearch().listView.layout(layout -> layout.widthPercent(100));
        elements.itemSearch().listView.setOverflowVisible(false);
        absolute(elements.searchModeToggle(), 117, 6, 16, 16);
        elements.searchModeToggle().getLayout().marginAll(0);
        elements.searchModeToggle().getLayout().paddingAll(0);
        absolute(elements.currencyLayoutToggle(), 136, 6, 16, 16);
        elements.currencyLayoutToggle().getLayout().marginAll(0);
        elements.currencyLayoutToggle().getLayout().paddingAll(0);
        absolute(elements.playerHead(), 252, 6, 21, 16);
        head.addChildren(searchControls, elements.playerHead());

        absolute(body, 0, 28, 273, 237);
        body.layout(layout -> {
            layout.paddingAll(0);
            layout.paddingHorizontal(8);
        });
        elements.merchantsView().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        // 商品主体包含主题图片的一像素边框；将滚动条向内缩进，确保拆分后的箭头贴图位于边框内。
        elements.merchantsView().verticalScroller.layout(layout -> {
            layout.marginTop(SCROLLER_TOP_INSET);
            layout.marginBottom(2);
        });
        elements.merchantsView().viewPort.getLayout().paddingTop(8);

        column.addChildren(head, body);
        return column;
    }

    private UIElement createSummaryColumn(ShopTheme theme, ShopUiElements elements) {
        UIElement column = new UIElement().setId("shop_ui_summary");
        UIElement header = new UIElement();
        UIElement body = new UIElement();
        UIElement shoppingCart = new UIElement().addChild(elements.shoppingCartView());
        UIElement summaryButtons = new UIElement().layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
        }).addChildren(elements.stashButton(), elements.clearButton());

        elements.shopTitle().textStyle(textStyle -> textStyle
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER));
        absolute(elements.shopTitle(), 0, -1, 125, 25);
        header.addChild(elements.shopTitle());

        elements.shoppingCartTitle().textStyle(textStyle -> textStyle
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER));
        elements.consumptionTitle().textStyle(textStyle -> textStyle
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER));
        absolute(elements.shoppingCartTitle(), 9, -2, 112, 20);
        absolute(shoppingCart, 7, 20, 114, 79);
        elements.shoppingCartView().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        absolute(elements.consumptionTitle(), 9, 99, 94, 14);
        absolute(elements.outputTargetButton(), 104, 99, 14, 14);
        absolute(elements.consumptionView(), 7, 113, 114, 82);
        elements.consumptionView().viewPort.getLayout().paddingAll(3);
        elements.consumptionView().viewPort.getLayout().paddingTop(6);

        absolute(summaryButtons, 7, 199, 114, 19);
        elements.stashButton().layout(layout -> {
            layout.width(54);
            layout.height(19);
        });
        elements.clearButton().layout(layout -> {
            layout.width(54);
            layout.height(19);
        });
        absolute(elements.buyButton(), 7, 221, 114, 19);

        absolute(header, 0, 0, 125, 25);
        absolute(body, 0, 25, 125, 240);
        body.addChildren(
                elements.shoppingCartTitle(),
                shoppingCart,
                elements.consumptionTitle(),
                elements.outputTargetButton(),
                elements.consumptionView(),
                summaryButtons,
                elements.buyButton()
        );
        column.addChildren(header, body);
        return column;
    }

    private static void absolute(UIElement element, float left, float top, float width, float height) {
        element.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(left);
            layout.top(top);
            layout.width(width);
            layout.height(height);
        });
    }
}

package com.viscriptshop.gui.layout;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.viscriptshop.gui.components.theme.ShopTheme;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;

public final class GlassDarkShopUiLayout implements ShopUiLayout {
    public static final GlassDarkShopUiLayout INSTANCE = new GlassDarkShopUiLayout();

    private GlassDarkShopUiLayout() {
    }

    @Override
    public UIElement build(ShopTheme theme, ShopUiElements elements) {
        UIElement root = new UIElement()
                .setId("shop_ui_shell")
                .addClass(theme.styleClass())
                .layout(layout -> {
                    layout.widthPercent(theme.shellWidthPercent());
                    layout.heightPercent(theme.shellHeightPercent());
                    layout.gapAll(3);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.justifyContent(AlignContent.CENTER);
                    layout.alignItems(AlignItems.CENTER);
                }).style(style -> style.backgroundTexture(theme.shellBackground()));

        root.addChildren(
                createCategoryColumn(theme, elements),
                createMerchantColumn(theme, elements),
                createSummaryColumn(theme, elements)
        );
        return root;
    }

    private UIElement createCategoryColumn(ShopTheme theme, ShopUiElements elements) {
        UIElement header = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(10);
        }).style(style -> style.backgroundTexture(theme.categoryHeader()));
        elements.categoryTitle().textStyle(textStyle -> textStyle
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER));
        elements.categoryTitle().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        header.addChild(elements.categoryTitle());

        UIElement categoryList = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(91);
        }).addChild(elements.categoryView());
        elements.categoryView().layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
        });

        UIElement balance = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
            layout.alignItems(AlignItems.CENTER);
            layout.flexDirection(FlexDirection.ROW);
        }).addChildren(
                new UIElement().layout(layout -> {
                    layout.widthPercent(21);
                    layout.justifyContent(AlignContent.CENTER);
                    layout.alignItems(AlignItems.CENTER);
                }).addChild(elements.balanceIcon()),
                elements.balanceValue()
        );
        elements.balanceIcon().layout(layout -> {
            layout.width(theme.balanceIconSize());
            layout.height(theme.balanceIconSize());
        });
        elements.balanceValue().textStyle(textStyle -> textStyle
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER));
        elements.balanceValue().layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
            layout.heightPercent(100);
            layout.marginRight(theme.balanceFieldMarginRight());
        }).style(style -> style.backgroundTexture(theme.balanceField()));

        UIElement body = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
        }).style(style -> style.backgroundTexture(theme.categoryPanel()))
                .addChildren(categoryList, balance);

        return new UIElement().setId("shop_ui_categories").layout(layout -> {
            layout.heightPercent(100);
            layout.widthPercent(22);
            layout.gapAll(3);
            layout.flexDirection(FlexDirection.COLUMN);
        }).style(style -> style.backgroundTexture(theme.categoryColumnBackground()))
                .addChildren(header, body);
    }

    private UIElement createMerchantColumn(ShopTheme theme, ShopUiElements elements) {
        UIElement searchControls = new UIElement().layout(layout -> {
            layout.heightPercent(100);
            layout.alignItems(AlignItems.CENTER);
            layout.flexDirection(FlexDirection.ROW);
        }).addChildren(
                elements.searchIcon(),
                elements.itemSearch(),
                elements.idSearch(),
                elements.searchModeToggle(),
                elements.currencyLayoutToggle()
        );
        elements.searchIcon().layout(layout -> {
            layout.marginLeft(5);
            layout.width(theme.searchIconWidth());
            layout.height(theme.searchIconHeight());
            layout.flexShrink(0);
        });
        elements.itemSearch().layout(layout -> {
            layout.width(70);
            layout.heightPercent(85);
            layout.paddingLeft(4);
        });
        elements.idSearch().layout(layout -> {
            layout.width(70);
            layout.heightPercent(85);
            layout.justifyContent(AlignContent.CENTER);
            layout.paddingLeft(4);
        });
        elements.searchModeToggle().layout(layout -> {
            layout.width(16);
            layout.height(16);
            layout.marginHorizontal(2);
        });
        elements.currencyLayoutToggle().layout(layout -> {
            layout.width(16);
            layout.height(16);
        });

        UIElement head = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(10);
            layout.paddingTop(2);
            layout.flexDirection(FlexDirection.ROW);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.alignItems(AlignItems.CENTER);
        }).style(style -> style.backgroundTexture(theme.topBar()))
                .addChildren(searchControls, elements.playerHead());
        elements.playerHead().layout(layout -> {
            layout.heightPercent(100);
            layout.alignItems(AlignItems.CENTER);
            layout.flexDirection(FlexDirection.ROW);
        });

        UIElement body = new UIElement().setId("shop_ui_merchants").layout(layout -> {
            layout.widthPercent(100);
            layout.justifyContent(AlignContent.CENTER);
            layout.alignItems(AlignItems.CENTER);
            layout.paddingAll(3);
            layout.paddingBottom(5);
            layout.flex(1);
        }).style(style -> style.backgroundTexture(theme.merchantPanel()))
                .addChild(elements.merchantsView());
        elements.merchantsView().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });

        return new UIElement().layout(layout -> {
            layout.widthPercent(55);
            layout.heightPercent(100);
            layout.gapAll(theme.centerPanelGap());
            layout.flexDirection(FlexDirection.COLUMN);
        }).style(style -> style.backgroundTexture(theme.merchantColumnBackground()))
                .addChildren(head, body);
    }

    private UIElement createSummaryColumn(ShopTheme theme, ShopUiElements elements) {
        UIElement header = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(10);
        }).style(style -> style.backgroundTexture(theme.titleHeader()));
        elements.shopTitle().textStyle(textStyle -> textStyle
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER));
        elements.shopTitle().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        header.addChild(elements.shopTitle());

        UIElement shoppingCart = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flex(11);
            layout.minHeight(0);
        }).style(style -> style.backgroundTexture(theme.shoppingCartPanel()))
                .addChild(elements.shoppingCartView());
        elements.shoppingCartView().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(85);
        });

        elements.consumptionView().layout(layout -> {
            layout.widthPercent(100);
            layout.flex(10);
            layout.minHeight(0);
        });
        elements.shoppingCartTitle().textStyle(textStyle -> textStyle.adaptiveHeight(true));
        elements.shoppingCartTitle().layout(layout -> {
            layout.marginLeft(3);
            layout.flexShrink(0);
        });
        elements.consumptionTitle().textStyle(textStyle -> textStyle
                .textAlignVertical(Vertical.CENTER));
        elements.consumptionTitle().layout(layout -> {
            layout.flex(1);
            layout.heightPercent(100);
            layout.marginLeft(3);
        });
        elements.outputTargetButton().layout(layout -> {
            layout.width(14);
            layout.height(14);
            layout.marginRight(3);
            layout.flexShrink(0);
        });
        UIElement consumptionHeader = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.height(14);
            layout.flexShrink(0);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
        }).addChildren(elements.consumptionTitle(), elements.outputTargetButton());

        UIElement summaryButtons = new UIElement().layout(layout -> {
            layout.marginTop(5);
            layout.marginBottom(2);
            layout.flexShrink(0);
            layout.flexDirection(FlexDirection.ROW);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
        }).addChildren(elements.stashButton(), elements.clearButton());
        elements.stashButton().layout(layout -> layout.widthPercent(45));
        elements.clearButton().layout(layout -> layout.widthPercent(45));
        elements.buyButton().layout(layout -> {
            layout.widthPercent(100);
            layout.flexShrink(0);
        });

        UIElement body = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.paddingAll(5);
        }).style(style -> style.backgroundTexture(theme.summaryPanel()))
                .addChildren(
                        elements.shoppingCartTitle(),
                        shoppingCart,
                        consumptionHeader,
                        elements.consumptionView(),
                        summaryButtons,
                        elements.buyButton()
                );

        return new UIElement().setId("shop_ui_summary").layout(layout -> {
            layout.widthPercent(25);
            layout.heightPercent(100);
            layout.gapAll(3);
            layout.flexDirection(FlexDirection.COLUMN);
        }).style(style -> style.backgroundTexture(theme.summaryColumnBackground()))
                .addChildren(header, body);
    }
}

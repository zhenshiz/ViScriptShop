package com.viscriptshop.gui.layout;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SearchComponent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.math.Size;
import com.viscriptshop.gui.components.theme.ShopTheme;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import org.joml.Vector2f;

public final class ShopColumnsLayout implements ShopUiLayout {
    public static final ShopColumnsLayout INSTANCE = new ShopColumnsLayout();
    public static final float LIST_CARD_WIDTH = 248;
    static final float LIST_COLUMN_WIDTH = LIST_CARD_WIDTH + 16;

    private ShopColumnsLayout() {
    }

    @Override
    public void initScreen(UIElement shell, Size layoutSize, boolean grid) {
        resizeColumns(shell, layoutSize.getWidth() * 0.9f, grid, 0);
    }

    static void resizeColumns(UIElement shell, float referenceWidth, boolean grid, float padding) {
        float gap = 3 / ShopUiScale.contentScale();
        float available = referenceWidth - padding * 2 - gap * 2;
        // 两侧按窗口基准分配宽度；中间栏节省的宽度直接从外框扣除。
        float categoryWidth = available * 22 / 102;
        float summaryWidth = available * 25 / 102;
        float merchantWidth = grid ? Math.max(LIST_COLUMN_WIDTH, available * 55 / 102) : LIST_COLUMN_WIDTH;
        shell.layout(layout -> {
            layout.width(categoryWidth + merchantWidth + summaryWidth + gap * 2 + padding * 2);
            layout.gapAll(gap);
        });
        shell.select("#shop_ui_categories").forEach(column -> column.getLayout().width(categoryWidth));
        shell.select("#shop_ui_merchant_column").forEach(column -> column.getLayout().width(merchantWidth));
        shell.select("#shop_ui_summary").forEach(column -> column.getLayout().width(summaryWidth));
        shell.select(".shop-header-content").forEach(ShopColumnsLayout::resizeHeaderContent);
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
        UIElement header = new UIElement().setId("shop_ui_category_header").layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(10);
            layout.flexShrink(0);
        }).style(style -> style.backgroundTexture(theme.categoryHeader()));
        elements.categoryTitle().setId("shop_category_title");
        elements.categoryTitle().textStyle(textStyle -> textStyle
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER));
        elements.categoryTitle().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        header.addChild(createHeaderContent().addChild(elements.categoryTitle()));

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
            layout.minWidth(0);
            layout.flexShrink(0);
            layout.gapAll(3);
            layout.flexDirection(FlexDirection.COLUMN);
        }).style(style -> style.backgroundTexture(theme.categoryColumnBackground()))
                .addChildren(header, body);
    }

    private UIElement createMerchantColumn(ShopTheme theme, ShopUiElements elements) {
        UIElement searchControls = new UIElement().layout(layout -> {
            layout.heightPercent(100);
            layout.flex(1);
            layout.minWidth(0);
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
            layout.flex(1);
            layout.minWidth(30);
            layout.maxWidth(70);
            layout.heightPercent(85);
            layout.paddingLeft(4);
        });
        elements.idSearch().layout(layout -> {
            layout.flex(1);
            layout.minWidth(30);
            layout.maxWidth(70);
            layout.heightPercent(85);
            layout.justifyContent(AlignContent.CENTER);
            layout.paddingLeft(4);
        });
        elements.searchModeToggle().layout(layout -> {
            layout.width(16);
            layout.height(16);
            layout.marginHorizontal(2);
            layout.flexShrink(0);
        });
        elements.currencyLayoutToggle().layout(layout -> {
            layout.width(16);
            layout.height(16);
            layout.flexShrink(0);
        });
        elements.itemSearch().dialog.transform(transform -> transform.pivot(0, 0)
                .scale(1 / ShopUiScale.contentScale()));
        elements.itemSearch().dialog.addEventListener(UIEvents.LAYOUT_CHANGED,
                event -> {
                    positionSearchPopup(elements.itemSearch());
                    event.stopImmediatePropagation();
                });

        UIElement headerContent = createHeaderContent().layout(layout -> {
            layout.paddingTop(2);
            layout.flexDirection(FlexDirection.ROW);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.alignItems(AlignItems.CENTER);
        }).addChildren(searchControls, elements.playerHead());
        UIElement head = new UIElement().setId("shop_ui_merchant_header").layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(10);
            layout.flexShrink(0);
        }).style(style -> style.backgroundTexture(theme.topBar())).addChild(headerContent);
        elements.playerHead().layout(layout -> {
            layout.width(21);
            layout.flexShrink(0);
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
        // 商品行已有横向内边距，减少滚动视口的重复留白，为按钮保留实际可用宽度。
        elements.merchantsView().viewPort.getLayout().paddingHorizontal(1);

        return new UIElement().setId("shop_ui_merchant_column").layout(layout -> {
            // 列表宽度只容纳卡片、边距和滚动条，随内容倍率缩放，纵向仍铺满外框。
            layout.width(LIST_COLUMN_WIDTH);
            layout.minWidth(LIST_COLUMN_WIDTH);
            layout.flexShrink(0);
            layout.heightPercent(100);
            layout.gapAll(theme.centerPanelGap());
            layout.flexDirection(FlexDirection.COLUMN);
        }).style(style -> style.backgroundTexture(theme.merchantColumnBackground()))
                .addChildren(head, body);
    }

    private UIElement createSummaryColumn(ShopTheme theme, ShopUiElements elements) {
        UIElement header = new UIElement().setId("shop_ui_summary_header").layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(10);
            layout.flexShrink(0);
        }).style(style -> style.backgroundTexture(theme.titleHeader()));
        elements.shopTitle().setId("shop_title");
        elements.shopTitle().textStyle(textStyle -> textStyle
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER));
        elements.shopTitle().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        header.addChild(createHeaderContent().addChild(elements.shopTitle()));

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
            layout.flexShrink(0);
            layout.minWidth(0);
            layout.heightPercent(100);
            layout.gapAll(3);
            layout.flexDirection(FlexDirection.COLUMN);
        }).style(style -> style.backgroundTexture(theme.summaryColumnBackground()))
                .addChildren(header, body);
    }

    private UIElement createHeaderContent() {
        UIElement content = new UIElement().addClass("shop-header-content");
        resizeHeaderContent(content);
        return content;
    }

    private static void resizeHeaderContent(UIElement content) {
        float contentScale = ShopUiScale.contentScale();
        // 为顶栏单独抵消内容倍率，布局空间同步补偿，保持文字、图标及点击区域一致。
        content.layout(layout -> {
            layout.widthPercent(100 * contentScale);
            layout.heightPercent(100 * contentScale);
            layout.flexShrink(0);
        }).transform(transform -> transform.pivot(0, 0).scale(1 / contentScale));
    }

    private static void positionSearchPopup(SearchComponent<?> search) {
        var ui = search.getModularUI();
        if (ui == null) return;
        search.dialog.transform(transform -> transform.pivot(0, 0).scale(1 / ShopUiScale.contentScale()));
        // 下拉框挂在根节点，按实际屏幕坐标约束，避免默认布局坐标裁边把它推离固定顶栏。
        var anchor = search.getWorldMouse(search.getPositionX(), search.getPositionY() + search.getSizeHeight());
        float scale = search.getWorldMouseNormal(1, 0).length();
        float x = Math.max(0, Math.min(anchor.x, ui.getScreenWidth() - search.dialog.getSizeWidth() * scale));
        float y = Math.max(0, Math.min(anchor.y, ui.getScreenHeight() - search.dialog.getSizeHeight() * scale));
        var offset = ui.ui.rootElement.worldToLocalLayoutOffset(new Vector2f(x, y));
        search.dialog.layout(layout -> {
            layout.left(offset.x);
            layout.top(offset.y);
        });
    }
}

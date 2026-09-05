package com.viscriptshop.gui.components.theme;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.viscriptshop.Config;
import com.viscriptshop.ViscriptShop;

/**
 * 商店界面的主题纹理集合。
 */
public record ShopTheme(
        String styleClass,
        float shellWidthPercent,
        float shellHeightPercent,
        float centerPanelGap,
        float searchIconWidth,
        float searchIconHeight,
        float searchIconScale,
        float balanceIconSize,
        float balanceIconScale,
        float balanceFieldMarginRight,
        float categoryEntryHeight,
        float merchantRowHeight,
        float merchantGridWidth,
        float merchantGridHeight,
        float scrollWidth,
        IGuiTexture shellBackground,
        IGuiTexture categoryColumnBackground,
        IGuiTexture merchantColumnBackground,
        IGuiTexture summaryColumnBackground,
        IGuiTexture categoryHeader,
        IGuiTexture categoryPanel,
        IGuiTexture topBar,
        IGuiTexture merchantPanel,
        IGuiTexture titleHeader,
        IGuiTexture summaryPanel,
        IGuiTexture shoppingCartPanel,
        IGuiTexture consumptionPanel,
        IGuiTexture messageBackground,
        IGuiTexture searchIcon,
        IGuiTexture searchIconBackground,
        IGuiTexture searchField,
        IGuiTexture toggleBase,
        IGuiTexture toggleHover,
        IGuiTexture balanceIconBackground,
        IGuiTexture balanceField,
        IGuiTexture merchantList,
        IGuiTexture merchantGrid,
        IGuiTexture actionButtonBase,
        IGuiTexture actionButtonHover,
        IGuiTexture actionButtonPressed,
        IGuiTexture secondaryButtonBase,
        IGuiTexture secondaryButtonHover,
        IGuiTexture secondaryButtonPressed,
        IGuiTexture categoryDefault,
        IGuiTexture categorySelected,
        IGuiTexture scrollHead,
        IGuiTexture scrollHeadPressed,
        IGuiTexture scrollTail,
        IGuiTexture scrollTailPressed,
        IGuiTexture scrollTrack,
        IGuiTexture scrollBarBase,
        IGuiTexture scrollBarHover,
        IGuiTexture scrollBarPressed
) {
    public static ShopTheme current() {
        return switch (Config.shopUiTheme.get()) {
            case GRAY_CAT_WORKSHOP -> grayCatWorkshop();
            case GLASS_DARK -> glassDark();
        };
    }

    public boolean isGrayCatWorkshop() {
        return styleClass.equals("shop-theme-gray-cat-workshop");
    }

    /**
     * 返回快捷商店选择弹窗的面板背景。
     *
     * @return 与此商店主题匹配的面板背景纹理
     */
    public IGuiTexture dialogPanel() {
        return isGrayCatWorkshop() ? messageBackground : rounded(0xC0202934, 0x70D6E5F2, 1, 5);
    }

    /**
     * 返回快捷商店选择器下拉列表的背景。
     *
     * @return 与此商店主题匹配的不透明下拉背景纹理
     */
    public IGuiTexture selectorPopup() {
        return isGrayCatWorkshop() ? messageBackground : rounded(0xF0202934, 0x70D6E5F2, 1, 4);
    }

    private static ShopTheme grayCatWorkshop() {
        return new ShopTheme(
                "shop-theme-gray-cat-workshop",
                90,
                84.5f,
                0,
                20,
                25,
                1,
                19,
                0.72f,
                4,
                25,
                22,
                47,
                76,
                7,
                sprite("gray_cat_workshop", "shell.png"),
                IGuiTexture.EMPTY,
                IGuiTexture.EMPTY,
                IGuiTexture.EMPTY,
                IGuiTexture.EMPTY,
                IGuiTexture.EMPTY,
                IGuiTexture.EMPTY,
                IGuiTexture.EMPTY,
                IGuiTexture.EMPTY,
                IGuiTexture.EMPTY,
                IGuiTexture.EMPTY,
                IGuiTexture.EMPTY,
                sprite("gray_cat_workshop", "controls/secondary_button.png").setBorder(2),
                IGuiTexture.EMPTY,
                IGuiTexture.EMPTY,
                sprite("gray_cat_workshop", "controls/input.png"),
                sprite("gray_cat_workshop", "controls/toggle.png"),
                sprite("gray_cat_workshop", "controls/toggle.png"),
                IGuiTexture.EMPTY,
                IGuiTexture.EMPTY,
                sprite("gray_cat_workshop", "panels/merchant_row.png"),
                sprite("gray_cat_workshop", "panels/merchant_grid.png"),
                sprite("gray_cat_workshop", "controls/primary_button.png"),
                sprite("gray_cat_workshop", "controls/primary_button_hover.png"),
                sprite("gray_cat_workshop", "controls/primary_button_pressed.png"),
                sprite("gray_cat_workshop", "controls/secondary_button.png"),
                sprite("gray_cat_workshop", "controls/secondary_button_hover.png"),
                sprite("gray_cat_workshop", "controls/secondary_button_pressed.png"),
                IGuiTexture.EMPTY,
                sprite("gray_cat_workshop", "controls/category_selected.png").setColor(0xFF6D6D6D),
                sprite("gray_cat_workshop", "scroll/up.png"),
                sprite("gray_cat_workshop", "scroll/up_pressed.png"),
                sprite("gray_cat_workshop", "scroll/down.png"),
                sprite("gray_cat_workshop", "scroll/down_pressed.png"),
                sprite("gray_cat_workshop", "scroll/track.png"),
                sprite("gray_cat_workshop", "scroll/thumb.png"),
                sprite("gray_cat_workshop", "scroll/thumb.png"),
                sprite("gray_cat_workshop", "scroll/thumb_pressed.png")
        );
    }

    private static ShopTheme glassDark() {
        return glass(
                "shop-theme-glass-dark",
                0xA0202934,
                0x78111922,
                0x6E17212B,
                0x8A26313E,
                0xB02F3E4D,
                0x70D6E5F2
        );
    }

    private static ShopTheme glass(String styleClass, int header, int panel, int inset,
                                   int card, int selected, int border) {
        return new ShopTheme(
                styleClass,
                90,
                91,
                3,
                18,
                18,
                0.8f,
                14,
                1,
                0,
                18,
                20,
                50,
                0,
                5,
                IGuiTexture.EMPTY,
                IGuiTexture.EMPTY,
                IGuiTexture.EMPTY,
                IGuiTexture.EMPTY,
                rounded(header, border, 1, 4),
                rounded(panel, border, 1, 4),
                rounded(header, border, 1, 5),
                rounded(panel, border, 1, 5),
                rounded(header, border, 1, 4),
                rounded(panel, border, 1, 4),
                rounded(inset, border, 1, 4),
                rounded(inset, border, 1, 4),
                rounded(header, border, 1, 4),
                commonIcon("search.png"),
                IGuiTexture.EMPTY,
                rounded(inset, border, 1, 5),
                Sprites.BORDER1_RT1_DARK,
                Sprites.BORDER1_RT1,
                IGuiTexture.EMPTY,
                IGuiTexture.EMPTY,
                rounded(card, border, 1, 4),
                rounded(card, border, 1, 4),
                rounded(0xA83B4652, 0x70D6E5F2, 1, 3),
                rounded(0xC0506270, 0x90EAF7FF, 1, 3),
                rounded(0xB02A333E, 0x70D6E5F2, 1, 3),
                rounded(0xA83B4652, 0x70D6E5F2, 1, 3),
                rounded(0xC0506270, 0x90EAF7FF, 1, 3),
                rounded(0xB02A333E, 0x70D6E5F2, 1, 3),
                rounded(0x50333F4B, border, 1, 3),
                rounded(selected, 0xC0F2FAFF, 1, 3),
                sprite("glass_dark", "scroll/scroll_top.png"),
                sprite("glass_dark", "scroll/scroll_top.png"),
                sprite("glass_dark", "scroll/scroll_bottom.png"),
                sprite("glass_dark", "scroll/scroll_bottom.png"),
                sprite("glass_dark", "scroll/scroll_bar_background.png"),
                sprite("glass_dark", "scroll/scroll_bar.png"),
                sprite("glass_dark", "scroll/scroll_bar_hover.png"),
                sprite("glass_dark", "scroll/scroll_bar_hold.png")
        );
    }

    private static SpriteTexture sprite(String themeName, String fileName) {
        return SpriteTexture.of(ViscriptShop.formattedMod("textures/themes/" + themeName + "/" + fileName));
    }

    private static SpriteTexture commonIcon(String fileName) {
        return SpriteTexture.of(ViscriptShop.formattedMod("textures/icons/" + fileName));
    }

    private static SDFRectTexture rounded(int fill, int border, float stroke, float radius) {
        return SDFRectTexture.of(fill)
                .setBorderColor(border)
                .setStroke(stroke)
                .setRadius(radius);
    }
}

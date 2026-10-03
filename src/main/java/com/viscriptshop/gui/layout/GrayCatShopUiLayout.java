package com.viscriptshop.gui.layout;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.math.Size;
import com.viscriptshop.gui.components.theme.ShopTheme;

public final class GrayCatShopUiLayout implements ShopUiLayout {
    static final float SHELL_ASPECT = 523f / 276f;

    @Override
    public UIElement build(ShopTheme theme, ShopUiElements elements) {
        UIElement shell = ShopColumnsLayout.INSTANCE.build(theme, elements);
        shell.layout(layout -> layout.paddingAll(5));
        elements.categoryView().verticalScroller.layout(layout -> {
            layout.marginLeft(2);
            layout.marginRight(1);
            layout.marginTop(4);
        });
        elements.categoryView().viewPort.getLayout().paddingAll(2);
        elements.merchantsView().verticalScroller.layout(layout -> {
            layout.marginTop(4);
            layout.marginBottom(2);
        });
        elements.shoppingCartView().layout(layout -> layout.heightPercent(100));
        elements.shoppingCartView().viewPort.getLayout().paddingAll(6);
        elements.consumptionView().viewPort.getLayout().paddingAll(6);
        elements.itemSearch().dialog.layout(layout -> {
            layout.minWidth(70);
            layout.maxWidth(70);
        });
        elements.itemSearch().dialog.setOverflowVisible(false);
        elements.itemSearch().listView.layout(layout -> layout.widthPercent(100));
        elements.itemSearch().listView.setOverflowVisible(false);
        return shell;
    }

    @Override
    public void initScreen(UIElement shell, Size layoutSize, boolean grid) {
        // 用原始美术比例确定高度，实际宽度由三栏内容决定。
        float width = Math.min(layoutSize.getWidth() * 0.9f, layoutSize.getHeight() * 0.91f * SHELL_ASPECT);
        float padding = 5 / ShopUiScale.contentScale();
        shell.layout(layout -> {
            layout.height(width / SHELL_ASPECT);
            layout.paddingAll(padding);
        });
        ShopColumnsLayout.resizeColumns(shell, width, grid, padding);
    }
}

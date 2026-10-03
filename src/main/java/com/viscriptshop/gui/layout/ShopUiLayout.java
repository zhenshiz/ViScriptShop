package com.viscriptshop.gui.layout;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.math.Size;
import com.viscriptshop.gui.components.theme.ShopTheme;

public interface ShopUiLayout {

    UIElement build(ShopTheme theme, ShopUiElements elements);

    default void initScreen(UIElement shell, Size layoutSize, boolean grid) {
    }
}

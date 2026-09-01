package com.viscriptshop.gui.layout;

import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SearchComponent;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.viscriptshop.gui.components.theme.ShopButton;
import com.viscriptshop.gui.components.ShopOutputTargetButton;
import net.minecraft.world.item.ItemStack;

public record ShopUiElements(
        ScrollerView categoryView,
        ScrollerView merchantsView,
        ScrollerView shoppingCartView,
        ScrollerView consumptionView,
        Label categoryTitle,
        Label shopTitle,
        UIElement balanceIcon,
        Label balanceValue,
        UIElement searchIcon,
        SearchComponent<ItemStack> itemSearch,
        StringConfigurator idSearch,
        Toggle searchModeToggle,
        Toggle currencyLayoutToggle,
        UIElement playerHead,
        Label shoppingCartTitle,
        Label consumptionTitle,
        ShopOutputTargetButton outputTargetButton,
        ShopButton stashButton,
        ShopButton clearButton,
        ShopButton buyButton
) {
}

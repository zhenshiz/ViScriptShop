package com.viscriptshop.util;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Menu;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.gui.util.TreeNode;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaEdge;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class MenuUtil {
    public static UIElement createMenuTab(TreeBuilder.Menu menu, @NotNull UIElement parent, String text) {
        return (new TextElement()).textStyle((textStyle) -> textStyle.adaptiveWidth(true).textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER)).setText(text).layout((layout) -> {
            layout.setHeightPercent(100.0F);
            layout.setPadding(YogaEdge.HORIZONTAL, 2.0F);
        }).style((style) -> style.backgroundTexture(IGuiTexture.EMPTY)).addEventListener("mouseEnter", (e) -> e.currentElement.style((style) -> style.backgroundTexture(ColorPattern.T_WHITE.rectTexture())), true).addEventListener("mouseLeave", (e) -> e.currentElement.style((style) -> style.backgroundTexture(IGuiTexture.EMPTY)), true).addEventListener("mouseDown", (e) -> openMenu(e.currentElement.getPositionX(), e.currentElement.getPositionY() + e.currentElement.getSizeHeight(), menu, parent));
    }

    private static void openMenu(float posX, float posY, @Nullable TreeBuilder.Menu menuBuilder, @NotNull UIElement parent) {
        if (menuBuilder != null && !menuBuilder.isEmpty()) {
            openMenu(posX, posY, menuBuilder.build(), TreeBuilder.Menu::uiProvider, parent).setHoverTextureProvider(TreeBuilder.Menu::hoverTextureProvider).setOnNodeClicked(TreeBuilder.Menu::handle);
        }
    }

    private static <T, C> Menu<T, C> openMenu(float posX, float posY, TreeNode<T, C> menuNode, UIElementProvider<T> uiProvider, @NotNull UIElement parent) {
        Menu<T, C> menu = new Menu<>(menuNode, uiProvider);
        menu.layout((layout) -> {
            layout.setPosition(YogaEdge.LEFT, posX - parent.getContentX());
            layout.setPosition(YogaEdge.TOP, posY - parent.getContentY());
        });
        parent.addChildren(menu);
        return menu;
    }
}

package com.viscriptshop.util;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Menu;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.gui.util.TreeNode;
import org.appliedenergistics.yoga.YogaEdge;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class UIElementUtil {
    public static Dialog numberEditorDialog(String title, Number initial,
                                            Number min, Number max, Consumer<Number> result) {
        AtomicReference<Number> value = new AtomicReference<>(initial);
        var dialog = new Dialog();
        var numberConfigurator = new NumberConfigurator(
                "",
                value::get,
                value::set,
                initial,
                false
        );

        // 设置范围
        numberConfigurator.setRange(min, max);

        // 根据初始值类型设置数字类型
        if (initial instanceof Integer) {
            numberConfigurator.setType(ConfigNumber.Type.INTEGER);
        } else if (initial instanceof Float) {
            numberConfigurator.setType(ConfigNumber.Type.FLOAT);
        } else if (initial instanceof Double) {
            numberConfigurator.setType(ConfigNumber.Type.DOUBLE);
        } else if (initial instanceof Long) {
            numberConfigurator.setType(ConfigNumber.Type.LONG);
        }

        dialog.setTitle(title);
        dialog.addContent(numberConfigurator.layout(layout -> layout.setWidth(120)));

        dialog.addButton(new Button()
                .setOnClick(e -> {
                    dialog.close();
                    result.accept(value.get());
                })
                .setText("ldlib.gui.tips.confirm"));

        return dialog;
    }

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

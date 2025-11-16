package com.viscriptshop.util;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.gui.util.TreeNode;
import com.lowdragmc.lowdraglib2.math.Size;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.appliedenergistics.yoga.YogaEdge;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UIElementUtil {

    public static SearchComponentConfigurator<Item> createItemSearchComponentConfigurator(String name, Supplier<String> getter, Consumer<String> setter, TagKey<Item> tag) {
        SearchComponentConfigurator<Item> itemSearchComponentConfigurator = new SearchComponentConfigurator<>(name,
                () -> {
                    String id = getter.get();
                    return id != null ? BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)) : Items.AIR;
                },
                item -> {
                    ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
                    setter.accept(key.toString());
                },
                BuiltInRegistries.ITEM.get(ResourceLocation.parse(
                        getter.get() != null ? getter.get() : Items.AIR.toString()
                )),
                false,
                (word, searchHandler) -> {
                    String lowerWord = word.toLowerCase();
                    for (var key : BuiltInRegistries.ITEM.keySet()) {
                        if (Thread.currentThread().isInterrupted()) return;
                        Item item = BuiltInRegistries.ITEM.get(key);
                        if (tag != null && !item.getDefaultInstance().is(tag)) continue;
                        if (key.toString().toLowerCase().contains(lowerWord) || Component.translatable(item.getDescriptionId()).getString().toLowerCase().contains(lowerWord)) {
                            ((IResultHandler<Item>) searchHandler).acceptResult(BuiltInRegistries.ITEM.get(key));
                        }
                    }
                },
                value -> BuiltInRegistries.ITEM.getKey(value).toString(),
                value -> ""
        );
        itemSearchComponentConfigurator.searchComponent.setCandidateUIProvider(UIElementProvider.iconText(
                ItemStackTexture::new,
                item -> Component.translatable(item.getDescriptionId())
        ));
        return itemSearchComponentConfigurator;
    }

    public static SearchComponentConfigurator<Item> createItemSearchComponentConfigurator(String name, Supplier<String> getter, Consumer<String> setter) {
        return createItemSearchComponentConfigurator(name, getter, setter, null);
    }

    public static ItemSlot createItemSlot(ItemStack item, boolean isRenderBackgroundTexture) {
        return (ItemSlot) new ItemSlot().setItem(item)
                .slotStyle(slotStyle -> {
                    if (!isRenderBackgroundTexture) slotStyle.hoverOverlay(new ColorRectTexture(0));
                })
                .layout(layout -> {
                    layout.setWidth(18);
                    layout.setHeight(18);
                })
                .style(style -> {
                    if (!isRenderBackgroundTexture) style.backgroundTexture(null);
                });
    }

    public static ModularUI createUI(UIElement root) {
        return new ModularUI(UI.of(root, size -> {
            int width = size.width;
            int height = size.height;

            float fontSize = Math.max(12, height * 0.04f);
            for (UIElement child : root.getChildren()) {
                if (child instanceof Label label) label.getTextStyle().fontSize(fontSize);
            }
            return Size.of(width, height);
        }));
    }

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

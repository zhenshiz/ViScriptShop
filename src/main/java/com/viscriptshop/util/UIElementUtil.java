package com.viscriptshop.util;

import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Menu;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.gui.util.TreeNode;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import com.viscript_lib.util.item.SimpleItemStackFilter;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.MerchantItemInfo;
import com.viscriptshop.gui.data.MerchantItemDisplay;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UIElementUtil {
    public static SearchComponentConfigurator<ItemStack> createItemStackSearchComponentConfigurator(String name, Supplier<ItemStack> itemGetter, Consumer<ItemStack> itemSetter, Collection<ItemStack> items) {
        return new SearchComponentConfigurator<>(
                name,
                itemGetter,
                itemSetter,
                ItemStack.EMPTY,
                false,
                (word, searchHandler) -> {
                    Collection<ItemStack> candidatesItems = items;

                    if (candidatesItems == null) {
                        candidatesItems = BuiltInRegistries.ITEM.stream()
                                .map(ItemStack::new)
                                .toList();
                    }

                    IResultHandler<ItemStack> handler = (IResultHandler<ItemStack>) searchHandler;

                    for (ItemStack stack : candidatesItems) {
                        if (Thread.currentThread().isInterrupted()) return;

                        if (stack.isEmpty()) {
                            handler.acceptResult(stack);
                            continue;
                        }

                        if (SimpleItemStackFilter.matchItemSearch(stack, word)) {
                            handler.acceptResult(stack);
                        }
                    }
                },
                value -> value.isEmpty() ? "" : value.getHoverName().getString(),
                value -> {
                    UIElement icon = new UIElement().layout(layout -> {
                        layout.width(10);
                        layout.height(10);
                        layout.flexShrink(0);
                    }).style(style -> style.backgroundTexture(new ItemStackTexture(value)));
                    TextElement label = (TextElement) new TextElement()
                            .setText(value.getHoverName())
                            .textStyle(style -> style
                                    .textWrap(TextWrap.HOVER_ROLL)
                                    .textAlignVertical(Vertical.CENTER))
                            .layout(layout -> {
                                layout.minWidth(0);
                                layout.height(10);
                                layout.flex(1);
                            })
                            .setOverflowVisible(false);
                    return new UIElement().addChildren(icon, label)
                            .addClass("shop-item-search-candidate")
                            .layout(layout -> {
                                layout.widthPercent(100);
                                layout.height(10);
                                layout.gapAll(2);
                                layout.flexDirection(FlexDirection.ROW);
                            })
                            .setOverflowVisible(false)
                            .addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                        if (!value.isEmpty()) {
                            Minecraft mc = Minecraft.getInstance();
                            TooltipFlag flag = mc.options.advancedItemTooltips
                                    ? net.minecraft.world.item.TooltipFlag.ADVANCED
                                    : net.minecraft.world.item.TooltipFlag.NORMAL;

                            List<Component> tooltips = value.getTooltipLines(
                                    Item.TooltipContext.of(mc.level),
                                    mc.player,
                                    flag
                            );

                            event.hoverTooltips = new HoverTooltips(tooltips, null, null, value);
                        }
                    });
                }
        );
    }

    public static ItemSlot createItemSlot(ItemStack item, int size, boolean isRenderBackgroundTexture, boolean showItemTooltips) {
        return (ItemSlot) new ItemSlot().setItem(item)
                .slotStyle(slotStyle -> {
                    if (!isRenderBackgroundTexture) slotStyle.hoverOverlay(new ColorRectTexture(0));
                    slotStyle.showItemTooltips(showItemTooltips);
                })
                .layout(layout -> {
                    layout.width(size);
                    layout.height(size);
                })
                .style(style -> {
                    if (!isRenderBackgroundTexture) style.backgroundTexture(IGuiTexture.EMPTY);
                });
    }

    public static ItemSlot createItemSlot(ItemStack item, boolean isRenderBackgroundTexture, boolean showItemTooltips) {
        return createItemSlot(item, 16, isRenderBackgroundTexture, showItemTooltips);
    }

    /**
     * 根据商品信息创建客户端图标。
     *
     * <p>资源包图片和替代物品模式只影响返回的界面元素。交易、匹配和库存处理仍使用
     * {@link MerchantItemInfo#getItem()} 返回的实际物品。
     *
     * @param itemInfo 商品的实际物品与图标配置
     * @param showItemTooltips 是否显示物品模式的原版物品提示
     * @return 尺寸为 16×16 的物品槽或资源图片元素
     */
    public static UIElement createMerchantItemDisplay(MerchantItemInfo itemInfo,
                                                       boolean showItemTooltips) {
        ItemStack actualItem = itemInfo == null ? ItemStack.EMPTY : itemInfo.getItem();
        MerchantItemDisplay display = itemInfo == null ? null : itemInfo.getDisplay();
        MerchantItemDisplay.RenderMode mode = display == null
                ? MerchantItemDisplay.RenderMode.ITEM
                : display.resolvedRenderMode();
        UIElement element = switch (mode) {
            case ITEM -> createItemSlot(actualItem, false, showItemTooltips)
                    .addClass("merchant-item-display-actual");
            case ITEM_RENDER -> createItemSlot(
                    display == null ? ItemStack.EMPTY : display.resolvedRenderItem(),
                    false,
                    showItemTooltips
            ).addClass("merchant-item-display-item-render");
            case RESOURCE -> createResourceItemDisplay(display)
                    .addClass("merchant-item-display-resource");
        };
        return element.addClass("merchant-item-display");
    }

    private static UIElement createResourceItemDisplay(MerchantItemDisplay display) {
        UIElement element = new UIElement().layout(layout -> {
            layout.width(16);
            layout.height(16);
        });
        ResourceLocation resourceLocation = parseResourceLocation(display == null ? "" : display.getResourcePath());
        if (resourceLocation != null) {
            element.style(style -> style.backgroundTexture(SpriteTexture.of(resourceLocation)));
        } else {
            element.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        }

        String tooltip = display == null ? "" : display.getResourceName();
        if (tooltip == null || tooltip.isBlank()) {
            tooltip = display == null ? "" : display.getResourcePath();
        }
        if (!tooltip.isBlank()) {
            String tooltipText = tooltip;
            element.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                event.hoverTooltips = new HoverTooltips(
                        List.of(Component.literal(tooltipText)),
                        null,
                        null,
                        null
                );
            });
        }
        return element;
    }

    @Nullable
    private static ResourceLocation parseResourceLocation(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        try {
            return ResourceLocation.parse(path.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static UIElement createCategoryUI(CategoryInfo categoryInfo, boolean isSelected,
                                             Consumer<CategoryInfo> onSelectCallback,
                                             IGuiTexture defaultBg, IGuiTexture selectedBg) {
        return createCategoryUI(categoryInfo, isSelected, onSelectCallback, defaultBg, selectedBg, 18);
    }

    public static UIElement createCategoryUI(CategoryInfo categoryInfo, boolean isSelected,
                                             Consumer<CategoryInfo> onSelectCallback,
                                             IGuiTexture defaultBg, IGuiTexture selectedBg,
                                             float entryHeight) {
        UIElement category = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.height(entryHeight);
            layout.gapAll(2);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.marginBottom(5);
        }).addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 0) {
                onSelectCallback.accept(categoryInfo);
            }
        });
        category.addClass("shop-category");
        category.addClass(isSelected ? "shop-category-selected" : "shop-category-default");
        UIElement icon = new UIElement().layout(layout -> {
            layout.minWidth(16);
            layout.minHeight(16);
            layout.width(16);
            layout.height(16);
            layout.maxWidth(16);
            layout.maxHeight(16);
        });
        Label label = (Label) new Label().setText(categoryInfo.getName())
                .textStyle(textStyle -> {
                    textStyle.textAlignHorizontal(Horizontal.LEFT).textAlignVertical(Vertical.CENTER).adaptiveWidth(true);
                    textStyle.fontSize(8);
                    textStyle.textColor(ColorPattern.WHITE.color);
                }).layout(layout -> {
                    layout.heightPercent(100);
                });
        label.addClass("shop-category-label");
        UIElement name = new UIElement().layout(layout -> {
                    layout.flex(8);
                    layout.heightPercent(100);
                    layout.paddingAll(3);
                }).style(style -> {
                    style.backgroundTexture(isSelected ? selectedBg : defaultBg);
                })
                .addChild(label);
        switch (categoryInfo.getIconType()) {
            case ITEM -> icon = createItemSlot(categoryInfo.getIconItem(), false, false);
            case TEXTURE -> {
                String iconTexture = categoryInfo.getIconTexture();
                if (!iconTexture.isEmpty() && ViscriptShop.isPresentResource(ResourceLocation.parse(iconTexture))) {
                    icon.style(style -> style.backgroundTexture(SpriteTexture.of(iconTexture)));
                }
            }
        }
        icon.addClass("shop-category-icon");
        category.addChildren(icon, name);
        return category;
    }

    public static void openMenu(float posX, float posY, @Nullable TreeBuilder.Menu menuBuilder, @NotNull UIElement parent) {
        if (menuBuilder != null && !menuBuilder.isEmpty()) {
            openMenu(posX, posY, menuBuilder.build(), TreeBuilder.Menu::uiProvider, parent).setHoverTextureProvider(TreeBuilder.Menu::hoverTextureProvider).setOnNodeClicked(TreeBuilder.Menu::handle);
        }
    }

    private static <T, C> Menu<T, C> openMenu(float posX, float posY, TreeNode<T, C> menuNode, UIElementProvider<T> uiProvider, @NotNull UIElement parent) {
        Menu<T, C> menu = new Menu<>(menuNode, uiProvider);
        menu.layout((layout) -> {
            layout.left(posX - parent.getContentX());
            layout.top(posY - parent.getContentY());
        });
        parent.addChildren(menu);
        return menu;
    }
}

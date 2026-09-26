package com.viscriptshop.gui.view;

import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.viscript_lib.gui.components.DraggableUI;
import com.viscript_lib.util.item.ItemUtil;
import com.viscriptshop.gui.ShopEditor;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.Shop;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.util.UIElementUtil;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
public class CategoryView extends View {
    public final ShopEditor editor;
    public final ScrollerView scrollerView = new ScrollerView();
    private CategoryInfo selectedCategory = null;
    private ShopInfo shopInfo;
    private DraggableUI<CategoryInfo> draggableCategories = null;
    private int lastRenderedSignature;
    private boolean categoryListDirty = true;

    public CategoryView(ShopEditor editor) {
        super("viscript_shop.editor.view_category");
        this.editor = editor;
        this.scrollerView.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        this.scrollerView.viewPort.getStyle().backgroundTexture(null);
        scrollerView.viewContainer.layout(layout -> {
            layout.paddingAll(5);
            layout.flexDirection(FlexDirection.COLUMN);
        }).addEventListener(UIEvents.TICK, event -> reloadCategoryList());
        this.addChildren(this.scrollerView);
    }

    public void loadView() {
        if (editor.getCurrentProject() instanceof Shop shop) {
            this.shopInfo = shop.getShopInfo();
            selectedCategory = null;
            draggableCategories = null;
            categoryListDirty = true;
        }
    }

    public void reloadCategoryList() {
        if (shopInfo == null) return;
        if (draggableCategories != null && draggableCategories.isDragging()) return;

        int signature = System.identityHashCode(selectedCategory);
        for (CategoryInfo category : shopInfo.getCategoryInfos()) {
            // 分类卡片只依赖名称和图标，不对分类中的全部商品求哈希。
            ItemStack icon = category.getIconItem();
            signature = 31 * signature + Objects.hash(System.identityHashCode(category), category.getName(),
                    category.getIconType(), category.getIconTexture(), ItemUtil.hashItemStack(icon), icon.getCount());
        }
        if (!categoryListDirty && signature == lastRenderedSignature) return;
        categoryListDirty = false;
        lastRenderedSignature = signature;

        scrollerView.clearAllScrollViewChildren();

        draggableCategories = new DraggableUI<>(shopInfo.getCategoryInfos(), this::applyCategoryOrder);
        draggableCategories.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.wrap(FlexWrap.NO_WRAP);
            layout.paddingAll(0);
            layout.gapAll(0);
        });

        for (CategoryInfo categoryInfo : shopInfo.getCategoryInfos()) {
            UIElement categoryUI = UIElementUtil.createCategoryUI(
                    categoryInfo,
                    categoryInfo == this.selectedCategory,
                    this::setSelectedCategory,
                    SDFRectTexture.of(ColorPattern.T_BLACK.color).setRadius(3),
                    SDFRectTexture.of(ColorPattern.T_WHITE.color).setRadius(3)
            ).layout(layout -> {
                layout.flex(11);
                layout.marginRight(10);
                layout.marginBottom(0);
            });
            UIElement dragHandle = createDragHandle();
            UIElement deleteButton = new UIElement().style(style -> {
                style.backgroundTexture(Icons.DELETE.copy().setColor(ColorPattern.RED.color));
                style.tooltips("viscript_shop.button.delete");
            }).layout(layout -> {
                layout.flex(1);
                layout.height(15);
            }).addEventListener(UIEvents.MOUSE_DOWN, event -> {
                if (event.button == 0) {
                    Dialog.showCheckBox("viscript_shop.button.delete", "viscript_shop.dialog.delete_category.info", (result) -> {
                        if (result) removeCategory(categoryInfo);
                    }).show(editor);
                    event.stopPropagation();
                }
            });
            UIElement uiElement = new UIElement().layout(layout -> {
                layout.widthPercent(100);
                layout.flexDirection(FlexDirection.ROW);
                layout.alignItems(AlignItems.CENTER);
                layout.marginBottom(5);
            }).addChildren(dragHandle, categoryUI, deleteButton);
            draggableCategories.addSortableCard(categoryInfo, uiElement, dragHandle);
        }

        scrollerView.addScrollViewChild(draggableCategories);
        scrollerView.viewContainer.addChildren(new Button().setText("+").setOnClick(event -> {
            CategoryInfo categoryInfo = new CategoryInfo();
            shopInfo.getCategoryInfos().add(categoryInfo);
            setSelectedCategory(categoryInfo);
        }).layout(layout -> {
            layout.maxWidth(15);
            layout.maxHeight(15);
        }).addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
            event.hoverTooltips = new HoverTooltips(List.of(Component.translatable("viscript_shop.editor.add.category")), null, null, null);
        })
        );
    }

    private UIElement createDragHandle() {
        return new UIElement().layout(layout -> {
            layout.width(10);
            layout.height(10);
        }).style(style -> style.backgroundTexture(Icons.ARROW_UP_DOWN));
    }

    private void applyCategoryOrder(List<CategoryInfo> newOrder) {
        shopInfo.getCategoryInfos().clear();
        shopInfo.getCategoryInfos().addAll(new ArrayList<>(newOrder));
    }

    private void removeCategory(CategoryInfo categoryInfo) {
        int index = findCategoryIndexByIdentity(categoryInfo);
        if (index < 0) return;

        shopInfo.getCategoryInfos().remove(index);
        if (selectedCategory == categoryInfo) {
            selectedCategory = null;
            editor.inspectShop();
        }
    }

    private int findCategoryIndexByIdentity(CategoryInfo target) {
        for (int i = 0; i < shopInfo.getCategoryInfos().size(); i++) {
            if (shopInfo.getCategoryInfos().get(i) == target) {
                return i;
            }
        }
        return -1;
    }

    private void setSelectedCategory(CategoryInfo newCategory) {
        if (newCategory != this.selectedCategory) {
            this.selectedCategory = newCategory;
        }
        editor.inspectCategory(newCategory);
    }
}

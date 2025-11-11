package com.viscriptshop.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.math.Size;
import com.viscriptshop.gui.components.Message;
import com.viscriptshop.gui.components.ShopEditorDialog;
import com.viscriptshop.gui.data.MerchantInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.appliedenergistics.yoga.*;

import java.util.ArrayList;
import java.util.List;

public class ShopEditor extends UIElement {
    private final List<MerchantInfo> merchants = new ArrayList<>();
    public ShopEditorDialog dialog;
    private ScrollerView merchantsView;

    {
        ItemStack item = Items.STICK.getDefaultInstance();
        item.set(DataComponents.ITEM_NAME, Component.nullToEmpty("测试"));
        merchants.add(new MerchantInfo()
                .itemA(item)
                .itemResult(Items.STONE.getDefaultInstance())
                .xp(100)
                .build());
        merchants.add(new MerchantInfo()
                .itemA(Items.STICK.getDefaultInstance())
                .itemResult(Items.STONE.getDefaultInstance())
                .xp(100)
                .build());
        merchants.add(new MerchantInfo()
                .itemA(Items.STICK.getDefaultInstance())
                .itemB(Items.DIAMOND_AXE.getDefaultInstance())
                .itemResult(Items.STONE.getDefaultInstance())
                .xp(100)
                .build());
    }

    public ShopEditor() {
        this.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
            layout.setJustifyContent(YogaJustify.CENTER);
            layout.setAlignItems(YogaAlign.CENTER);
        });
        UIElement root = new UIElement();
        root.layout((layout) -> {
            layout.setWidthPercent(75);
            layout.setHeightPercent(70);
            layout.setPadding(YogaEdge.ALL, 10);
            layout.setFlexDirection(YogaFlexDirection.COLUMN);
        });
        root.getStyle().backgroundTexture(Sprites.BORDER);
        this.addChild(root);
        UIElement head = new UIElement().layout(layout -> {
            layout.setWidthPercent(100);
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setJustifyContent(YogaJustify.SPACE_BETWEEN);
        });
        Label title = (Label) new Label().setText("viscript_shop.editor.title").textStyle(style -> {
            style.textAlignHorizontal(Horizontal.CENTER)
                    .textAlignVertical(Vertical.CENTER);
        }).layout(layout -> {
            layout.setHeightPercent(5);
            layout.setPadding(YogaEdge.TOP, 10);
            layout.setFlex(14);
        });
        head.addChildren(new Button().setText("viscript_shop.editor.save").layout(layout -> layout.setFlex(2)),
                title,
                new Button().setOnClick(event -> Minecraft.getInstance().setScreen(null)).setText("X").layout(layout -> layout.setFlex(1)));

        this.dialog = (ShopEditorDialog) new ShopEditorDialog(this)
                .layout(layout -> {
                    layout.setWidthPercent(100);
                    layout.setHeightPercent(100);
                });
        createMerchants();
        root.addChildren(head, this.merchantsView);
    }

    public ModularUI createUI() {
        return new ModularUI(UI.of(this, size -> {
            int width = size.width;
            int height = size.height;

            float fontSize = Math.max(12, height * 0.04f);
            for (UIElement child : this.getChildren()) {
                if (child instanceof Label label) label.getTextStyle().fontSize(fontSize);
            }
            return Size.of(width, height);
        })).shouldCloseOnEsc(false);
    }

    public void addMerchant(MerchantInfo merchant) {
        merchants.add(merchant);
        reloadMerchants();
    }

    public void updateMerchant(int index, MerchantInfo merchant) {
        merchants.set(index, merchant);
        reloadMerchants();
    }

    public void removeMerchant(int index) {
        merchants.remove(index);
        reloadMerchants();
    }

    public List<MerchantInfo> getMerchants() {
        return new ArrayList<>(merchants);
    }

    public void reloadMerchants() {
        merchantsView.clearAllScrollViewChildren();

        // 重新添加所有商品
        for (int i = 0; i < merchants.size(); i++) {
            MerchantInfo merchantInfo = merchants.get(i);
            int finalI = i;
            merchantsView.addScrollViewChild(createMerchant(merchantInfo, i)
                    .addEventListener(UIEvents.CLICK, event -> {
                        this.dialog.updateShow(this, merchantInfo, finalI);
                    })
            );
        }

        // 添加 "+" 按钮
        merchantsView.addScrollViewChild(new UIElement().layout(layout -> {
            layout.setWidthPercent(29);
            layout.setGap(YogaGutter.ALL, 5);
            layout.setMargin(YogaEdge.LEFT, 5);
            layout.setJustifyContent(YogaJustify.CENTER);
            layout.setFlexDirection(YogaFlexDirection.ROW);
        }).addChildren(new Button().setText("+").layout(layout -> {
            layout.setWidth(15);
            layout.setHeight(15);
        }).addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
            event.hoverTooltips = new HoverTooltips(List.of(Component.translatable("viscript_shop.editor.add.merchant")), null, null, null);
        }).addEventListener(UIEvents.CLICK, event -> {
            this.dialog.addShow(this);
        })));
    }

    private void createMerchants() {
        this.merchantsView = new ScrollerView();
        merchantsView.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
            layout.setPadding(YogaEdge.ALL, 10);
        });

        merchantsView.viewContainer.layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setWrap(YogaWrap.WRAP);
            layout.setMargin(YogaEdge.TOP, 5);
            layout.setGap(YogaGutter.ALL, 5);
        });

        reloadMerchants();
    }

    public UIElement createMerchant(MerchantInfo merchantInfo, int index) {
        UIElement merchant = new UIElement().layout(layout -> {
            layout.setWidthPercent(29);
            layout.setGap(YogaGutter.ALL, 5);
            layout.setMargin(YogaEdge.LEFT, 5);
            layout.setJustifyContent(YogaJustify.CENTER);
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setAlignItems(YogaAlign.CENTER);
        });
        ItemSlot itemASlot = createItemSlot(merchantInfo.getItemA(), false);
        ItemSlot itemBSlot = createItemSlot(merchantInfo.getItemB(), false);
        ItemSlot resultItemSlot = createItemSlot(merchantInfo.getItemResult(), true);
        merchant.addChildren(itemASlot, itemBSlot,
                new UIElement().style(style -> style.backgroundTexture(Icons.RIGHT_ARROW_NO_BAR_S_LIGHT)).layout(layout -> {
                    layout.setWidth(6);
                    layout.setHeight(6);
                    layout.setMargin(YogaEdge.ALL, 5);
                }),
                resultItemSlot,
                new UIElement().style(style -> {
                    style.backgroundTexture(Icons.DELETE.copy().setColor(0xFFFF0000));
                    style.setTooltips("viscript_shop.editor.merchant.button.delete");
                }).layout(layout -> {
                    layout.setWidth(10);
                    layout.setHeight(10);
                }).addEventListener(UIEvents.CLICK, event -> {
                    removeMerchant(index);
                    event.stopPropagation();
                })
        );
        return merchant;
    }

    private ItemSlot createItemSlot(ItemStack item, boolean isRenderBackgroundTexture) {
        return (ItemSlot) new ItemSlot().setItem(item)
                .slotStyle(slotStyle -> {
                    if (!isRenderBackgroundTexture) slotStyle.hoverOverlay(new ColorRectTexture(0));
                })
                .layout(layout -> {
                    layout.setWidth(15);
                    layout.setHeight(15);
                })
                .style(style -> {
                    if (!isRenderBackgroundTexture) style.backgroundTexture(null);
                });
    }
}

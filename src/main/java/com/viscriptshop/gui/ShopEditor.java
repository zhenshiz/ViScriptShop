package com.viscriptshop.gui;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.components.Message;
import com.viscriptshop.gui.components.ShopEditorDialog;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.Shop;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.util.ShopHelper;
import com.viscriptshop.util.UIElementUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class ShopEditor extends UIElement {
    public ShopInfo shopInfo = new ShopInfo();
    public ShopEditorDialog dialog;
    private ScrollerView merchantsView;

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
        TreeBuilder.Menu fileMenu = TreeBuilder.Menu.start().leaf("viscript_shop.editor.load", () -> {
            //打开本地文件
            Dialog.showFileDialog("viscript_shop.editor.load", new File(LDLib2.getAssetsDir(), ShopHelper.SHOP_PATH), true, Dialog.suffixFilter(Shop.SUFFIX), file -> {
                if (file != null && file.exists()) {
                    try {
                        CompoundTag data = NbtIo.read(file.toPath());
                        ShopInfo shopInfo = new ShopInfo();
                        shopInfo.deserializeNBT(Platform.getFrozenRegistry(), Objects.requireNonNull(data));
                        this.shopInfo = shopInfo;
                        ShopHelper.cacheShopFile = file;
                        reloadMerchants();
                    } catch (IOException e) {
                        Message.error("editor.loading_failed", this);
                    }
                }
            }).show(this);
        }).leaf("viscript_shop.editor.save", () -> {
            //判断缓存文件是否存在
            if (ShopHelper.cacheShopFile != null && ShopHelper.cacheShopFile.exists()) {
                //直接更新文件
                this.saveFile();
            } else {
                //另存为
                this.saveAsFile();
            }
        }).leaf("viscript_shop.editor.saveAs", this::saveAsFile);

        TreeBuilder.Menu toolMenu = TreeBuilder.Menu.start().leaf("viscript_shop.editor.setStage", () -> {
            UIElementUtil.numberEditorDialog("viscript_shop.editor.setStage", 0, 0, Integer.MAX_VALUE, (value) -> {
                this.shopInfo.setStage(value.intValue());
                reloadMerchants();
            }).show(this);
        });
        head.addChildren(
                UIElementUtil.createMenuTab(fileMenu, this, "viscript_shop.editor.file"),
                UIElementUtil.createMenuTab(toolMenu, this, "viscript_shop.editor.tool"),
                title,
                new Button().setOnClick(event -> Minecraft.getInstance().setScreen(null)).setText("X").layout(layout -> layout.setFlex(1)))
        ;

        this.dialog = (ShopEditorDialog) new ShopEditorDialog(this)
                .layout(layout -> {
                    layout.setWidthPercent(100);
                    layout.setHeightPercent(100);
                });

        File file = ShopHelper.cacheShopFile;
        if (file != null && file.exists()) {
            try {
                CompoundTag data = NbtIo.read(file.toPath());
                ShopInfo shopInfo = new ShopInfo();
                shopInfo.deserializeNBT(Platform.getFrozenRegistry(), Objects.requireNonNull(data));
                this.shopInfo = shopInfo;
            } catch (IOException e) {
                Message.error("editor.loading_failed", this);
            }
        }
        createMerchants();
        root.addChildren(head, this.merchantsView);
    }

    public void addMerchant(MerchantInfo merchant) {
        shopInfo.getMerchants().add(merchant);
        reloadMerchants();
    }

    public void updateMerchant(int index, MerchantInfo merchant) {
        shopInfo.getMerchants().set(index, merchant);
        reloadMerchants();
    }

    public void removeMerchant(int index) {
        shopInfo.getMerchants().remove(index);
        reloadMerchants();
    }

    public void reloadMerchants() {
        merchantsView.clearAllScrollViewChildren();

        // 重新添加所有商品
        for (int i = 0; i < shopInfo.getMerchants().size(); i++) {
            MerchantInfo merchantInfo = shopInfo.getMerchants().get(i);
            if (merchantInfo.getStage() != shopInfo.getStage()) continue;
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
        ItemSlot itemASlot = UIElementUtil.createItemSlot(merchantInfo.getItemA(), false);
        ItemSlot itemBSlot = UIElementUtil.createItemSlot(merchantInfo.getItemB(), false);
        ItemSlot resultItemSlot = UIElementUtil.createItemSlot(merchantInfo.getItemResult(), true);
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

    public void saveFile() {
        try {
            CompoundTag fileData = this.shopInfo.serializeNBT(Platform.getFrozenRegistry());
            NbtIo.write(fileData, ShopHelper.cacheShopFile.toPath());
            Message.success("viscript_shop.message.saveSuccess", this);
        } catch (Exception exception) {
            ViscriptShop.LOGGER.error("Failed to save shop file : {} \n {}", ShopHelper.cacheShopFile, exception.toString());
        }
    }

    public void saveAsFile() {
        if (!shopInfo.getMerchants().isEmpty()) {
            String suffix = Shop.SUFFIX;
            Dialog.showFileDialog("viscript_shop.editor.saveAs", new File(LDLib2.getAssetsDir(), ShopHelper.SHOP_PATH), false, Dialog.suffixFilter(suffix), (file) -> {
                if (file != null && !file.isDirectory()) {
                    if (!file.getName().endsWith(suffix)) {
                        file = new File(file.getParentFile(), file.getName() + suffix);
                    }

                    try {
                        CompoundTag fileData = this.shopInfo.serializeNBT(Platform.getFrozenRegistry());
                        NbtIo.write(fileData, file.toPath());
                        ShopHelper.cacheShopFile = file;
                    } catch (Exception exception) {
                        ViscriptShop.LOGGER.error("Failed to saveAs shop file : {} \n {}", file, exception.toString());
                    }
                }
            }).show(this);
        }
    }
}

package com.viscriptshop.gui.components;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.viscriptshop.gui.ShopEditor;
import com.viscriptshop.gui.data.MerchantInfo;
import net.minecraft.world.item.ItemStack;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaJustify;

public class ShopEditorDialog extends Dialog {
    public final ShopEditor shopEditor;
    private final MerchantInfo editingMerchantInfo = new MerchantInfo();
    public boolean isAdd = false;
    public int index;

    public ShopEditorDialog(ShopEditor shopEditor) {
        super();
        this.shopEditor = shopEditor;
        ConfiguratorGroup group = (ConfiguratorGroup) new ConfiguratorGroup()
                .layout(layout -> {
                    layout.setWidthPercent(100);
                    layout.setHeightPercent(50);
                });
        this.editingMerchantInfo.buildConfigurator(group);
        for (Configurator configurator : group.getConfigurators()) {
            configurator.layout(layout -> layout.setWidthPercent(100));
            this.addContent(configurator);
        }
        this.addButton(
                new UIElement().layout(layout -> {
                            layout.setHeightPercent(100);
                            layout.setWidthPercent(100);
                            layout.setFlexDirection(YogaFlexDirection.ROW);
                            layout.setJustifyContent(YogaJustify.FLEX_END);
                        })
                        .addChildren(
                                new Button().setText("viscript_shop.editor.merchant.button.confirm")
                                        .setOnClick(event -> {
                                            if (editingMerchantInfo.getItemA().isEmpty() && editingMerchantInfo.getItemB().isEmpty()) {
                                                Message.warn("viscript_shop.message.item.empty", this.shopEditor);
                                                return;
                                            }
                                            if (editingMerchantInfo.getItemResult().isEmpty()) {
                                                Message.warn("viscript_shop.message.itemResult.empty", this.shopEditor);
                                                return;
                                            }
                                            this.close();
                                            applyChanges();
                                        }).layout(layout -> {
                                            layout.setMargin(YogaEdge.RIGHT, 5);
                                        }),
                                new Button().setText("viscript_shop.editor.merchant.button.cancel")
                                        .setOnClick(event -> {
                                            this.close();
                                        })
                        )
        );
        this.setAutoClose(false);
    }

    public void updateShow(UIElement parent, MerchantInfo merchantInfo, int index) {
        super.show(parent);
        setIsAdd(false);
        this.editingMerchantInfo.setItemA(merchantInfo.getItemA() != null ? merchantInfo.getItemA().copy() : null);
        this.editingMerchantInfo.setItemB(merchantInfo.getItemB() != null ? merchantInfo.getItemB().copy() : null);
        this.editingMerchantInfo.setItemResult(merchantInfo.getItemResult() != null ? merchantInfo.getItemResult().copy() : null);
        this.editingMerchantInfo.setXp(merchantInfo.getXp());
        this.editingMerchantInfo.setCommand(merchantInfo.getCommand());
        this.editingMerchantInfo.setStage(merchantInfo.getStage());

        this.index = index;
    }

    public void addShow(UIElement parent) {
        super.show(parent);
        this.editingMerchantInfo.setItemA(ItemStack.EMPTY);
        this.editingMerchantInfo.setItemB(ItemStack.EMPTY);
        this.editingMerchantInfo.setItemResult(ItemStack.EMPTY);
        this.editingMerchantInfo.setXp(0);
        this.editingMerchantInfo.setCommand("");
        this.editingMerchantInfo.setStage(0);

        setIsAdd(true);
    }

    private void applyChanges() {
        // 创建最终的深拷贝用于保存
        MerchantInfo finalMerchant = new MerchantInfo();
        finalMerchant.setItemA(editingMerchantInfo.getItemA() != null ? editingMerchantInfo.getItemA().copy() : null);
        finalMerchant.setItemB(editingMerchantInfo.getItemB() != null ? editingMerchantInfo.getItemB().copy() : null);
        finalMerchant.setItemResult(editingMerchantInfo.getItemResult() != null ? editingMerchantInfo.getItemResult().copy() : null);
        finalMerchant.setXp(editingMerchantInfo.getXp());
        finalMerchant.setCommand(editingMerchantInfo.getCommand());
        finalMerchant.setStage(editingMerchantInfo.getStage());

        if (isAdd) {
            this.shopEditor.addMerchant(finalMerchant);
        } else {
            this.shopEditor.updateMerchant(this.index, finalMerchant);
        }
    }

    private void setIsAdd(boolean isAdd) {
        this.isAdd = isAdd;
        if (isAdd) {
            this.setTitle("viscript_shop.editor.add.merchant");
        } else {
            this.setTitle("viscript_shop.editor.update.merchant");
        }
    }
}

package com.viscriptshop.gui.data;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.world.item.ItemStack;

@Data
@AllArgsConstructor
public class MerchantInfo implements IConfigurable, IPersistedSerializable {
    @Configurable(name = "viscript_shop.editor.merchant.itemA")
    private ItemStack itemA;
    @Configurable(name = "viscript_shop.editor.merchant.itemB")
    private ItemStack itemB;
    @Configurable(name = "viscript_shop.editor.merchant.itemResult")
    private ItemStack itemResult;
    @Configurable(name = "viscript_shop.editor.merchant.xp")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    private int xp;
    @Configurable(name = "viscript_shop.editor.merchant.command")
    private String command;
    @Configurable(name = "viscript_shop.editor.merchant.stage", tips = "viscript_shop.editor.merchant.stage.tip")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    private int stage;

    public MerchantInfo() {
        this.itemA = ItemStack.EMPTY;
        this.itemB = ItemStack.EMPTY;
        this.itemResult = ItemStack.EMPTY;
        this.xp = 0;
        this.command = "";
        this.stage = 0;
    }

    public void defaultValue() {
        this.setItemA(ItemStack.EMPTY);
        this.setItemB(ItemStack.EMPTY);
        this.setItemResult(ItemStack.EMPTY);
        this.setXp(0);
        this.setCommand("");
        this.setStage(0);
    }

    public MerchantInfo itemA(ItemStack itemA) {
        if (itemA != null) this.itemA = itemA;
        return this;
    }

    public MerchantInfo itemB(ItemStack itemB) {
        if (itemB != null) this.itemB = itemB;
        return this;
    }

    public MerchantInfo itemResult(ItemStack itemResult) {
        if (itemResult != null) this.itemResult = itemResult;
        return this;
    }

    public MerchantInfo xp(int xp) {
        this.xp = Math.max(xp, 0);
        return this;
    }

    public MerchantInfo command(String command) {
        if (command != null) this.command = command;
        return this;
    }

    public MerchantInfo stage(int stage) {
        this.stage = Math.max(stage, 0);
        return this;
    }

    public MerchantInfo build() {
        return this;
    }
}

package com.viscriptshop.gui.data;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.ReadOnlyManaged;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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

    public MerchantInfo() {
        this.itemA = ItemStack.EMPTY;
        this.itemB = ItemStack.EMPTY;
        this.itemResult = ItemStack.EMPTY;
        this.xp = 0;
        this.command = "";
    }

    public MerchantInfo itemA(ItemStack itemA) {
        this.itemA = itemA;
        return this;
    }

    public MerchantInfo itemB(ItemStack itemB) {
        this.itemB = itemB;
        return this;
    }

    public MerchantInfo itemResult(ItemStack itemResult) {
        this.itemResult = itemResult;
        return this;
    }

    public MerchantInfo xp(int xp) {
        this.xp = xp;
        return this;
    }

    public MerchantInfo command(String command) {
        this.command = command;
        return this;
    }

    public MerchantInfo build() {
        return this;
    }
}

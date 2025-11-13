package com.viscriptshop.gui.data;

import com.mojang.blaze3d.MethodsReturnNonnullByDefault;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class Shop implements INBTSerializable<CompoundTag> {
    public static final String SUFFIX = ".shop";
    @Nullable
    @Setter
    private String path;
    public MerchantInfo merchantInfo;

    public Shop() {
        merchantInfo = new MerchantInfo();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return merchantInfo.serializeNBT(provider);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        merchantInfo.deserializeNBT(provider, tag);
    }
}

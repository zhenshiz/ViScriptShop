package com.viscriptshop.gui.configurator;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.syncdata.AccessorRegistries;
import com.lowdragmc.lowdraglib2.syncdata.accessor.direct.CustomDirectAccessor;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public class SyncAccessor {
    private static final Codec<ItemStack> ITEM_STACK_CODEC = Codec.PASSTHROUGH.xmap(
            dynamic -> {
                CompoundTag tag = (CompoundTag) dynamic.getValue();
                return ItemStack.parseOptional(Platform.getFrozenRegistry(), tag);
            },
            itemStack -> {
                if (itemStack == null || itemStack.isEmpty()) {
                    return new Dynamic<>(NbtOps.INSTANCE, new CompoundTag());
                }
                return new Dynamic<>(NbtOps.INSTANCE, itemStack.saveOptional(Platform.getFrozenRegistry()));
            }
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> ITEM_STACK_STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(ITEM_STACK_CODEC);

    public static void init() {
        AccessorRegistries.registerAccessor(CustomDirectAccessor.builder(ItemStack.class)
                        .codec(ITEM_STACK_CODEC).streamCodec(ITEM_STACK_STREAM_CODEC)
                        .customMark(ItemStack::copy, ItemStack::matches)
                        .build(),
                0
        );
    }
}

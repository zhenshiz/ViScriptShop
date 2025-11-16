package com.viscriptshop.gui.data;

import com.lowdragmc.lowdraglib2.Platform;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ShopSavedData extends SavedData {
    private final Map<String, ShopInfo> shopInfoMap = new HashMap<>();
    private final ServerLevel world;

    public static SavedData.Factory<ShopSavedData> factory(ServerLevel world) {
        return new SavedData.Factory<>(() -> new ShopSavedData(world), (nbt, r) -> fromNbt(world, nbt), null);
    }

    public ShopSavedData(ServerLevel world) {
        this.world = world;
    }

    public ShopInfo getShopInfo(String shop) {
        setDirty();
        return shopInfoMap.get(shop);
    }

    public void setShopInfo(String shop, ShopInfo shopInfo) {
        shopInfoMap.put(shop, shopInfo);
        setDirty();
    }

    public void resetShopInfo(String shop) {
        shopInfoMap.remove(shop);
        setDirty();
    }

    public static ShopSavedData fromNbt(ServerLevel world, CompoundTag nbt) {
        ShopSavedData shopSavedData = new ShopSavedData(world);
        for (String shop : nbt.getAllKeys()) {
            ShopInfo shopInfo = new ShopInfo();
            shopInfo.deserializeNBT(Platform.getFrozenRegistry(), nbt.getCompound(shop));
            shopSavedData.shopInfoMap.put(shop, shopInfo);
        }
        return shopSavedData;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag, HolderLookup.@NotNull Provider provider) {
        for (Map.Entry<String, ShopInfo> entry : shopInfoMap.entrySet()) {
            compoundTag.put(entry.getKey(), entry.getValue().serializeNBT(provider));
        }
        return compoundTag;
    }
}

package com.viscriptshop.util;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.command.ShopCommand;
import com.viscriptshop.gui.data.Shop;
import lombok.SneakyThrows;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
public class ShopHelper {
    private final static Map<String, Shop> CACHE = new HashMap<>();
    public static final String SHOP_PATH = "%s/shop/".formatted(ViscriptShop.MOD_ID);
    public static File cacheShopFile;

    public static int clearCache() {
        var count = CACHE.size();
        CACHE.clear();
        return count;
    }

    public static void removeCache(String path){
        CACHE.remove(path);
    }

    @Nullable
    public static Shop getShop(String path) {
        return getShop(path, true);
    }


    @Nullable
    public static Shop getShop(String path, boolean useCache) {
        return useCache ? CACHE.computeIfAbsent(path, location -> loadShop(path)) : loadShop(path);
    }

    public static Set<String> scanShopFiles() {
        ShopCommand.shopFilesPath.clear();
        return FileScanner.scanFilesWithSuffix(new File(LDLib2.getAssetsDir(), SHOP_PATH), Shop.SUFFIX);
    }

    @Nullable
    @SneakyThrows
    private static Shop loadShop(String path) {
        File file = new File(LDLib2.getAssetsDir(), SHOP_PATH + path + Shop.SUFFIX);
        if (!file.exists()) {
            ViscriptShop.LOGGER.error("shop file {} not found", path);
            return null;
        }
        try {
            CompoundTag tag = NbtIo.read(file.toPath());
            if (tag == null) return null;
            var shop = new Shop();
            shop.setPath(path);
            shop.deserializeNBT(Platform.getFrozenRegistry(), tag);
            return shop;
        } catch (Exception ignored) {
            return null;
        }
    }
}

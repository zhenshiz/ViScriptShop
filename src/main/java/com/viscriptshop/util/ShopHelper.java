package com.viscriptshop.util;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.viscript_lib.gui.editor.EditorAssetFiles;
import com.viscriptshop.Config;
import com.viscriptshop.gui.data.Shop;
import com.viscriptshop.gui.data.ShopInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
public class ShopHelper {
    private final static Map<String, ShopInfo> CACHE = new HashMap<>();
    public static final String SHOP_PATH = "viscript_shop/shop";
    //缓存的商店信息
    public static ShopInfo cacheShopInfo;

    public static int clearCache() {
        var count = CACHE.size();
        CACHE.clear();
        return count;
    }

    @Nullable
    public static ShopInfo getShop(String shopLocation) {
        return getShop(shopLocation, true);
    }


    public static ShopInfo getShop(String shopLocation, boolean useCache) {
        try {
            shopLocation = normalizeShopLocation(shopLocation);
        } catch (IllegalArgumentException e) {
            return null;
        }
        ShopInfo cached = useCache ? CACHE.get(shopLocation) : null;
        return cached != null ? cached : loadShop(shopLocation);
    }

    /**
     * 规范化商店相对路径，使同一商店共享存档和库存标识。
     *
     * <p>保留目录层级，移除外围引号和冗余分隔符，拒绝绝对路径及任何父目录片段。
     *
     * @param shopLocation 商店相对路径，不含运行时文件后缀
     * @return 规范化的商店路径
     * @throws IllegalArgumentException 路径为空、非法或包含父目录片段时抛出
     */
    public static String normalizeShopLocation(String shopLocation) {
        String location = shopLocation == null ? "" : shopLocation.trim().replace('\\', '/');
        if (location.length() > 1 && location.startsWith("\"") && location.endsWith("\"")) {
            location = location.substring(1, location.length() - 1);
        }
        for (String segment : location.split("/")) {
            if (segment.equals("..")) throw new IllegalArgumentException("Shop path contains '..'");
        }
        Path path = Path.of(location).normalize();
        if (path.isAbsolute() || path.toString().isBlank()) {
            throw new IllegalArgumentException("Invalid shop path");
        }
        return path.toString().replace('\\', '/');
    }

    private static ShopInfo loadShop(String shopLocation) {
        // 只接受目录中实际存在的逻辑 ID，避免大小写不敏感的文件系统产生独立库存别名。
        if (!EditorAssetFiles.listRuntimeFiles(Shop.FORMAT, true).contains(shopLocation)) return null;
        File file = new File(LDLib2.getAssetsDir(), SHOP_PATH + "/" + shopLocation + Shop.SUFFIX);
        CompoundTag compoundTag;
        if (!file.exists()) return null;
        try {
            if (!file.toPath().toRealPath().startsWith(Shop.FORMAT.functionDirectory().toPath().toRealPath())) {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
        try (var inputStream = Files.newInputStream(file.toPath())) {
            compoundTag = NbtIo.readCompressed(inputStream);
        } catch (IOException e) {
            compoundTag = new CompoundTag();
        }

        boolean migrateLegacy = Config.enableLegacyDataMigration != null && Config.enableLegacyDataMigration.get();
        ShopInfo shopInfo = Shop.deserializeRuntimeInfo(Platform.getFrozenRegistry(), compoundTag, migrateLegacy);
        CACHE.put(shopLocation, shopInfo);
        return shopInfo;
    }
}

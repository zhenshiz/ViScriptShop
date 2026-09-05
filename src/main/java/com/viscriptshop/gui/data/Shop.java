package com.viscriptshop.gui.data;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.editor.project.ProjectType;
import com.lowdragmc.lowdraglib2.editor.resource.Resources;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.mojang.blaze3d.MethodsReturnNonnullByDefault;
import com.viscript_lib.gui.editor.EditorFileFormat;
import com.viscript_lib.gui.editor.FunctionFileProjectType;
import com.viscript_lib.gui.editor.IRuntimeFileProject;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.components.Message;
import com.viscriptshop.util.ShopHelper;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.nio.file.Files;
import java.util.Objects;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class Shop implements IRuntimeFileProject {
    public static final String SUFFIX = ".shop";
    public static final EditorFileFormat FORMAT = EditorFileFormat.compressed(ViscriptShop.MOD_ID, "shop", SUFFIX);
    public static final ProjectType PROVIDER = new ShopFunctionFileProjectType();
    public static final int VERSION = 7;
    public static final String VERSION_TAG = "version_num";
    public ShopInfo shopInfo;

    public Shop() {
        shopInfo = new ShopInfo();
    }

    @Override
    public String getVersion() {
        return "%d.0".formatted(VERSION);
    }

    @Override
    public Resources getResources() {
        return Resources.EMPTY;
    }

    @Override
    public ProjectType getProjectType() {
        return PROVIDER;
    }

    @Override
    public CompoundTag serializeProject(@NotNull HolderLookup.Provider provider) {
        return serializeRuntimeFile(provider);
    }

    @Override
    public CompoundTag serializeRuntimeFile(HolderLookup.Provider provider) {
        return serializeNBT(provider);
    }

    @Override
    public void deserializeProject(@NotNull HolderLookup.Provider provider, @NotNull CompoundTag nbt) {
        deserializeNBT(provider, nbt);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return serializeRuntimeNBT(provider, shopInfo);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        deserializeRuntimeNBT(provider, tag, true);
    }

    public void deserializeRuntimeNBT(HolderLookup.Provider provider, CompoundTag tag, boolean migrateLegacy) {
        shopInfo = deserializeRuntimeInfo(provider, tag, migrateLegacy);
    }

    public boolean isTrueFormat(Editor editor) {
        for (CategoryInfo categoryInfo : this.shopInfo.getCategoryInfos()) {
            for (MerchantInfo merchant : categoryInfo.getMerchants()) {
                switch (categoryInfo.getShopType()) {
                    case ITEM_FOR_ITEM -> {
                        if (merchant.getItemA().isEmpty() && merchant.getItemB().isEmpty()) {
                            Message.warn("viscript_shop.message.item.empty", editor);
                            return false;
                        } else if (merchant.getItemResult().isEmpty()) {
                            Message.warn("viscript_shop.message.itemResult.empty", editor);
                            return false;
                        }
                    }
                    case CURRENCY -> {
                        if (merchant.getItemResult().isEmpty()) {
                            Message.warn("viscript_shop.message.itemResult.empty", editor);
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public static CompoundTag serializeRuntimeNBT(HolderLookup.Provider provider, ShopInfo shopInfo) {
        var data = shopInfo.serializeNBT(provider);
        data.putInt(VERSION_TAG, VERSION);
        return data;
    }

    public static ShopInfo deserializeRuntimeInfo(HolderLookup.Provider provider, CompoundTag tag, boolean migrateLegacy) {
        CompoundTag shopTag = unwrapRuntimeTag(tag);
        if (migrateLegacy) {
            shopTag = migrateShopData(shopTag, getRuntimeDataVersion(tag, shopTag));
        }

        ShopInfo shopInfo = new ShopInfo();
        shopInfo.deserializeNBT(provider, shopTag);
        return shopInfo;
    }

    public static CompoundTag unwrapRuntimeTag(CompoundTag tag) {
        if (tag.contains("shop", Tag.TAG_COMPOUND)) {
            return tag.getCompound("shop");
        }
        if (tag.contains("data", Tag.TAG_COMPOUND)) {
            CompoundTag dataTag = tag.getCompound("data");
            if (dataTag.contains("shop", Tag.TAG_COMPOUND)) {
                return dataTag.getCompound("shop");
            }
            return dataTag;
        }
        return tag;
    }

    public static int getRuntimeDataVersion(CompoundTag rootTag, CompoundTag shopTag) {
        if (shopTag.contains(VERSION_TAG, Tag.TAG_INT)) {
            return shopTag.getInt(VERSION_TAG);
        }
        if (rootTag.contains(VERSION_TAG, Tag.TAG_INT)) {
            return rootTag.getInt(VERSION_TAG);
        }
        if (rootTag.contains("meta", Tag.TAG_COMPOUND)) {
            CompoundTag meta = rootTag.getCompound("meta");
            if (meta.contains(VERSION_TAG, Tag.TAG_INT)) {
                return meta.getInt(VERSION_TAG);
            }
            if (meta.contains("version", Tag.TAG_STRING)) {
                return parseMajorVersion(meta.getString("version"));
            }
        }
        return 1;
    }

    private static int parseMajorVersion(String version) {
        int dotIndex = version.indexOf('.');
        String major = dotIndex >= 0 ? version.substring(0, dotIndex) : version;
        try {
            return Integer.parseInt(major);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    /**
     * 将旧版本商店运行时数据迁移到当前格式。
     *
     * @param shopTag 商店运行时数据
     * @param version 数据版本号，如果无法确定版本传入 1
     * @return 迁移后的商店运行时数据
     */
    @NotNull
    public static CompoundTag migrateShopData(@NotNull CompoundTag shopTag, int version) {
        if (version >= VERSION) {
            if (shopTag.contains(VERSION_TAG, Tag.TAG_INT)) {
                return shopTag;
            }
            CompoundTag tagged = shopTag.copy();
            tagged.putInt(VERSION_TAG, VERSION);
            return tagged;
        }

        CompoundTag currentTag = shopTag;
        int currentVersion = version;
        while (currentVersion < VERSION) {
            currentTag = migrateToNextVersion(currentTag, currentVersion);
            currentVersion++;
        }
        currentTag.putInt(VERSION_TAG, VERSION);
        return currentTag;
    }

    @NotNull
    private static CompoundTag migrateToNextVersion(@NotNull CompoundTag shopTag, int fromVersion) {
        return switch (fromVersion) {
            case 1 -> migrateV1ToV2(shopTag);
            case 2 -> migrateV2ToV3(shopTag);
            case 3 -> migrateV3ToV4(shopTag);
            case 4 -> migrateV4ToV5(shopTag);
            case 5 -> migrateV5ToV6(shopTag);
            case 6 -> migrateV6ToV7(shopTag);
            default -> shopTag;
        };
    }

    private static CompoundTag migrateV6ToV7(CompoundTag shopTag) {
        CompoundTag migratedTag = shopTag.copy();
        if (!(migratedTag.get("categoryInfos") instanceof ListTag categories)) {
            return migratedTag;
        }
        for (Tag categoryTag : categories) {
            if (!(categoryTag instanceof CompoundTag category)
                    || !(category.get("merchants") instanceof ListTag merchants)) {
                continue;
            }
            for (Tag merchantTag : merchants) {
                if (!(merchantTag instanceof CompoundTag merchant)) {
                    continue;
                }
                ListTag commands = new ListTag();
                if (merchant.get("command") instanceof StringTag legacyCommand) {
                    for (String command : legacyCommand.getAsString().split(";")) {
                        String normalized = command.trim();
                        if (!normalized.isEmpty()) {
                            commands.add(StringTag.valueOf(normalized));
                        }
                    }
                }
                if (!merchant.contains("commands", Tag.TAG_LIST)) {
                    merchant.put("commands", commands);
                }
                merchant.remove("command");
            }
        }
        return migratedTag;
    }

    private static CompoundTag migrateV5ToV6(CompoundTag shopTag) {
        CompoundTag migratedTag = shopTag.copy();
        migratedTag.putBoolean("promotionEnabled", hasListEntries(migratedTag, "promotionRules"));

        if (!(migratedTag.get("categoryInfos") instanceof ListTag categories)) {
            return migratedTag;
        }
        for (Tag categoryTag : categories) {
            if (!(categoryTag instanceof CompoundTag category)) {
                continue;
            }
            category.putBoolean("promotionEnabled", hasMeaningfulPromotionSettings(category));
            category.putBoolean("stageRestrictionEnabled", hasStageSettings(category));
            if (!(category.get("merchants") instanceof ListTag merchants)) {
                continue;
            }
            for (Tag merchantTag : merchants) {
                if (merchantTag instanceof CompoundTag merchant) {
                    merchant.putBoolean("promotionEnabled", hasMeaningfulPromotionSettings(merchant));
                    merchant.putBoolean("stageRestrictionEnabled", hasStageSettings(merchant));
                }
            }
        }
        return migratedTag;
    }

    private static boolean hasMeaningfulPromotionSettings(CompoundTag owner) {
        if (hasListEntries(owner, "promotionRules")) {
            return true;
        }
        if (owner.contains("inheritParentPromotions", Tag.TAG_BYTE)
                && !owner.getBoolean("inheritParentPromotions")) {
            return true;
        }
        String aggregation = owner.getString("promotionAggregation");
        return !aggregation.isEmpty()
                && !aggregation.endsWith(".inherit")
                && !aggregation.equalsIgnoreCase("inherit");
    }

    private static boolean hasStageSettings(CompoundTag owner) {
        return hasListEntries(owner, "flagGroups") || hasListEntries(owner, "lockMessages");
    }

    private static boolean hasListEntries(CompoundTag owner, String key) {
        Tag value = owner.get(key);
        if (value instanceof ListTag list) {
            return !list.isEmpty();
        }
        if (value instanceof CompoundTag wrapper && wrapper.get("payload") instanceof ListTag list) {
            return !list.isEmpty();
        }
        return false;
    }

    private static CompoundTag migrateV4ToV5(CompoundTag shopTag) {
        // 第五版新增商店、分类和商品三级促销字段；旧商店由数据模型补齐安全默认值。
        return shopTag.copy();
    }

    private static CompoundTag migrateV1ToV2(CompoundTag shopTag) {
        CompoundTag migratedTag = shopTag.copy();

        if (migratedTag.contains("categoryInfos")) {
            var categoryInfosTag = migratedTag.get("categoryInfos");
            if (categoryInfosTag instanceof CompoundTag oldCategoryFormat) {
                if (oldCategoryFormat.contains("payload")) {
                    var payload = oldCategoryFormat.get("payload");
                    if (payload != null) {
                        migratedTag.put("categoryInfos", payload);
                        if (payload instanceof ListTag categoryList) {
                            for (var category : categoryList) {
                                if (category instanceof CompoundTag categoryCompound) {
                                    migrateCategoryMerchants(categoryCompound);
                                }
                            }
                        }
                    }
                }
            } else if (categoryInfosTag instanceof ListTag categoryList) {
                for (var category : categoryList) {
                    if (category instanceof CompoundTag categoryCompound) {
                        migrateCategoryMerchants(categoryCompound);
                    }
                }
            }
        }

        return migratedTag;
    }

    private static CompoundTag migrateV2ToV3(CompoundTag shopTag) {
        CompoundTag migratedTag = shopTag.copy();
        migratedTag.remove("stage");

        var categoryInfosTag = migratedTag.get("categoryInfos");
        if (categoryInfosTag instanceof ListTag categoryList) {
            for (var category : categoryList) {
                if (category instanceof CompoundTag categoryCompound) {
                    migrateMerchantStageGroups(categoryCompound);
                }
            }
        }

        return migratedTag;
    }

    private static CompoundTag migrateV3ToV4(CompoundTag shopTag) {
        CompoundTag migratedTag = shopTag.copy();
        var categoryInfosTag = migratedTag.get("categoryInfos");
        if (!(categoryInfosTag instanceof ListTag categoryList)) {
            return migratedTag;
        }

        for (var category : categoryList) {
            if (!(category instanceof CompoundTag categoryCompound)
                    || !(categoryCompound.get("merchants") instanceof ListTag merchants)) {
                continue;
            }
            for (var merchant : merchants) {
                if (merchant instanceof CompoundTag merchantCompound) {
                    migrateCostItemInfo(merchantCompound, "itemA", "itemAMatchRule", "itemADisplay");
                    migrateCostItemInfo(merchantCompound, "itemB", "itemBMatchRule", "itemBDisplay");
                    migrateResultItemInfo(merchantCompound, "itemResult", "itemResultDisplay");
                }
            }
        }
        return migratedTag;
    }

    private static void migrateCostItemInfo(CompoundTag merchant,
                                            String itemKey,
                                            String matchRuleKey,
                                            String displayKey) {
        CompoundTag itemInfo = nestedItemInfo(merchant, itemKey);
        copyIfPresent(merchant, matchRuleKey, itemInfo, "matchRule");
        copyIfPresent(merchant, displayKey, itemInfo, "display");
        merchant.remove(matchRuleKey);
        merchant.remove(displayKey);
        if (!itemInfo.isEmpty()) {
            merchant.put(itemKey, itemInfo);
        }
    }

    private static void migrateResultItemInfo(CompoundTag merchant,
                                              String itemKey,
                                              String displayKey) {
        CompoundTag itemInfo = nestedItemInfo(merchant, itemKey);
        copyIfPresent(merchant, displayKey, itemInfo, "display");
        merchant.remove(displayKey);
        if (!itemInfo.isEmpty()) {
            merchant.put(itemKey, itemInfo);
        }
    }

    private static CompoundTag nestedItemInfo(CompoundTag merchant, String itemKey) {
        Tag existing = merchant.get(itemKey);
        if (existing instanceof CompoundTag existingCompound
                && (existingCompound.contains("item")
                || existingCompound.contains("display")
                || existingCompound.contains("matchRule"))) {
            return existingCompound.copy();
        }

        CompoundTag itemInfo = new CompoundTag();
        if (existing != null) {
            itemInfo.put("item", existing.copy());
        }
        return itemInfo;
    }

    private static void copyIfPresent(CompoundTag source,
                                      String sourceKey,
                                      CompoundTag target,
                                      String targetKey) {
        Tag value = source.get(sourceKey);
        if (value != null && !target.contains(targetKey)) {
            target.put(targetKey, value.copy());
        }
    }

    private static void migrateCategoryMerchants(CompoundTag categoryCompound) {
        if (categoryCompound.contains("merchants")) {
            var merchantsTag = categoryCompound.get("merchants");
            if (merchantsTag instanceof CompoundTag oldMerchantsFormat) {
                if (oldMerchantsFormat.contains("payload")) {
                    var payload = oldMerchantsFormat.get("payload");
                    if (payload != null) {
                        categoryCompound.put("merchants", payload);
                    }
                }
            }
        }
    }

    private static void migrateMerchantStageGroups(CompoundTag categoryCompound) {
        if (!(categoryCompound.get("merchants") instanceof ListTag merchants)) {
            return;
        }

        for (var merchant : merchants) {
            if (!(merchant instanceof CompoundTag merchantCompound)) {
                continue;
            }

            ListTag flags = merchantCompound.contains("flags", Tag.TAG_LIST)
                    ? merchantCompound.getList("flags", Tag.TAG_STRING).copy()
                    : new ListTag();
            if (merchantCompound.contains("stage", Tag.TAG_INT)) {
                int stage = merchantCompound.getInt("stage");
                merchantCompound.remove("stage");
                if (stage > 0) {
                    addFlagIfAbsent(flags, String.valueOf(stage));
                }
            }

            merchantCompound.remove("flags");
            if (merchantCompound.contains("flagGroups", Tag.TAG_LIST) || flags.isEmpty()) {
                continue;
            }

            merchantCompound.put("flagGroups", createAndFlagGroups(flags));
        }
    }

    private static void addFlagIfAbsent(ListTag flags, String flag) {
        for (int i = 0; i < flags.size(); i++) {
            if (flag.equals(flags.getString(i))) {
                return;
            }
        }
        flags.add(StringTag.valueOf(flag));
    }

    private static ListTag createAndFlagGroups(ListTag flags) {
        CompoundTag group = new CompoundTag();
        group.putString("mode", "viscript_shop.data.flag_group.mode.and");
        group.put("flags", flags);
        ListTag flagGroups = new ListTag();
        flagGroups.add(group);
        return flagGroups;
    }

    private static class ShopFunctionFileProjectType extends FunctionFileProjectType {
        private ShopFunctionFileProjectType() {
            super(IGuiTexture.EMPTY, "viscript_shop.editor.shop.add", FORMAT, Shop::new);
        }

        @Override
        public IProject loadProjectFromFile(File file) throws Exception {
            CompoundTag data;
            if (FORMAT.compressed()) {
                try (var inputStream = Files.newInputStream(file.toPath())) {
                    data = NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());
                }
            } else {
                data = Objects.requireNonNull(NbtIo.read(file.toPath()));
            }
            var project = getProjectCreator().get();
            project.deserializeProject(Platform.getFrozenRegistry(), data);
            return project;
        }

        @Override
        public void saveProjectToFile(IProject project, File file) throws Exception {
            if (file.getParentFile() != null) {
                Files.createDirectories(file.getParentFile().toPath());
            }
            var fileData = serializeRuntimeFile(project);
            if (FORMAT.compressed()) {
                NbtIo.writeCompressed(fileData, file.toPath());
            } else {
                NbtIo.write(fileData, file.toPath());
            }
            ShopHelper.clearCache();
        }

        @Override
        public boolean isProjectDirty(IProject project, File file) throws Exception {
            CompoundTag fileData;
            if (FORMAT.compressed()) {
                try (var inputStream = Files.newInputStream(file.toPath())) {
                    fileData = NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());
                }
            } else {
                fileData = Objects.requireNonNull(NbtIo.read(file.toPath()));
            }
            return !serializeRuntimeFile(project).equals(fileData);
        }

        private CompoundTag serializeRuntimeFile(IProject project) {
            if (project instanceof IRuntimeFileProject runtimeFileProject) {
                return runtimeFileProject.serializeRuntimeFile(Platform.getFrozenRegistry());
            }
            return project.serializeProject(Platform.getFrozenRegistry());
        }
    }
}

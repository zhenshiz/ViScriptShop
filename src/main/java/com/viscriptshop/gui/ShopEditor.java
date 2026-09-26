package com.viscriptshop.gui;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.mojang.blaze3d.MethodsReturnNonnullByDefault;
import com.viscript_lib.gui.editor.EditorUploadAction;
import com.viscript_lib.gui.editor.FunctionFileEditor;
import com.viscript_lib.util.ClipBoardHelper;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.Shop;
import com.viscriptshop.gui.settings.ShopEditorSettings;
import com.viscriptshop.gui.util.ShopEditorUploads;
import com.viscriptshop.gui.view.CategoryView;
import com.viscriptshop.gui.view.ShopInspectorView;
import com.viscriptshop.gui.view.ShopPreviewView;
import com.viscriptshop.util.ShopHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.nio.file.Path;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ShopEditor extends FunctionFileEditor {
    public final static ResourceLocation SHOP_ID = ViscriptShop.id("editor");

    public ShopInspectorView shopInspectorView;
    public final CategoryView categoryView = new CategoryView(this);
    public final ShopPreviewView shopPreviewView = new ShopPreviewView(this);

    public ShopEditor() {
        fileMenu.registerMenuCreator((tab, menu) -> addFxxkSdmLeaf(menu));
        registerProjectType(Shop.PROVIDER);
        this.leftWindow.getLeftTop().addView(categoryView);
        this.centerWindow.getLeftTop().addView(shopPreviewView);
        removeBottomWindow();
        selectInspectorView();
    }

    @Override
    protected Editor createNewEditorInstance() {
        return new ShopEditor();
    }

    @Override
    protected void initEditorSettings() {
        super.initEditorSettings();
        editorSettings.registerSettings(new ShopEditorSettings(), ShopEditorSettings.CODEC);
    }

    @Override
    protected void onPrepareInspectorView() {
        shopInspectorView = new ShopInspectorView(this);
        placeView(shopInspectorView, () -> rightWindow.getRightTop());
    }

    @Override
    protected EditorUploadAction createServerUploadAction() {
        if (getCurrentProject() instanceof Shop shop) {
            return new ShopServerUploadAction(shop, this);
        }
        return null;
    }

    @Override
    protected void loadNewProject(IProject project, @Nullable File projectFile) {
        if (project instanceof Shop shop) {
            super.loadNewProject(project, projectFile);
            shopInspectorView.loadShop(shop.getShopInfo());
            selectInspectorView();
            categoryView.loadView();
            shopPreviewView.loadView();
        }
    }

    public void inspectShop() {
        if (getCurrentProject() instanceof Shop shop) {
            shopInspectorView.inspectShop(shop.getShopInfo());
        }
    }

    public void inspectCategory(CategoryInfo categoryInfo) {
        shopInspectorView.inspectCategory(categoryInfo);
    }

    public void inspectMerchant(MerchantInfo merchantInfo, CategoryInfo.ShopType shopType) {
        shopInspectorView.inspectMerchant(merchantInfo, shopType);
    }

    /**
     * 打开商品检查器，并保留其所属分类以解析上级促销规则。
     *
     * @param merchantInfo 当前商品
     * @param categoryInfo 商品所属分类
     */
    public void inspectMerchant(MerchantInfo merchantInfo, CategoryInfo categoryInfo) {
        shopInspectorView.inspectMerchant(merchantInfo, categoryInfo);
    }

    private void selectInspectorView() {
        var container = rightWindow.getRightTop();
        if (shopInspectorView.getViewContainer() != container ||
                container.getAllViews().indexOf(shopInspectorView) > 0) {
            container.addViewAt(shopInspectorView, 0);
        }
        container.selectView(shopInspectorView);
    }

    private boolean shouldReloadShopAfterUpload() {
        return editorSettings.getSettings(ShopEditorSettings.ID)
                .filter(ShopEditorSettings.class::isInstance)
                .map(ShopEditorSettings.class::cast)
                .map(ShopEditorSettings::isReloadShopAfterUpload)
                .orElse(true);
    }

    private record ShopServerUploadAction(Shop shop, ShopEditor editor) implements EditorUploadAction {
        @Override
        public Component getDisplayName() {
            return Component.translatable("viscript_shop.editor.project.upload_shop");
        }

        @Override
        public String getDialogTitleKey() {
            return "viscript_shop.editor.project.upload_shop";
        }

        @Override
        public String getDefaultFileName() {
            File currentFile = editor.getCurrentProjectFile();
            if (currentFile == null) {
                return "test";
            }
            String fileName = currentFile.getName();
            String suffix = getSuffix();
            return fileName.endsWith(suffix) ? fileName.substring(0, fileName.length() - suffix.length()) : fileName;
        }

        @Override
        public String getSuffix() {
            return Shop.FORMAT.runtimeSuffix();
        }

        @Override
        public void uploadToServer(String fileName) {
            if (!shop.isTrueFormat(editor)) {
                return;
            }
            ShopEditorUploads.uploadShopToServer(
                    fileName,
                    shop.serializeRuntimeFile(Platform.getFrozenRegistry()),
                    editor.shouldReloadShopAfterUpload()
            );
            ShopHelper.clearCache();
        }
    }

    private void addFxxkSdmLeaf(TreeBuilder.Menu menu) {
        menu.leaf("强兼SDM商店数据", () -> { // 从剪贴板导入数据
            var dialog = new Dialog()
                    .setTitle("注意事项")
                    .addContent(new Label().setText("请先保存当前已有的项目。复制一份sdm商店数据文件的完整路径到剪贴板，点击确定即可导入。并不能兼容所有数据，请以实际效果为准。")
                            .textStyle(style -> style.textWrap(TextWrap.WRAP).adaptiveHeight(true))
                            .layout(layout -> layout.width(150)));
            dialog.addButton(new Button()
                    .setOnClick(e -> {
                        Dialog.showNotification(fxxkSdmShopData(), 3).show(getModularUI());
                        dialog.close();
                    })
                    .setText("ldlib.gui.tips.confirm")
                    .addClass("__confirm-button__"));
            dialog.addButton(new Button()
                    .setOnClick(e -> dialog.close())
                    .setText("ldlib.gui.tips.cancel")
                    .addClass("__cancel-button__"));
            dialog.show(getModularUI());
        });
    }

    private String fxxkSdmShopData() {
        Shop project = (Shop) getProjectTypes().get(0).newEmptyProject();
        try {
            var file = ClipBoardHelper.getFirstCopiedFile();
            if (file == null) return "请复制一份商店数据文件路径到剪贴板";
            CompoundTag tag = null;
            try { tag = NbtIo.readCompressed(file); } catch (Exception ignored) {}
            if (tag == null) try { tag = NbtIo.read(file); } catch (Exception ignored) {}
            if (tag == null) {
                try {
                    Class<?> forName = Class.forName("dev.ftb.mods.ftblibrary.snbt.SNBT");
                    var read = forName.getDeclaredMethod("read", Path.class);
                    read.setAccessible(true);
                    tag = (CompoundTag) read.invoke(null, file.toPath());
                } catch (Exception e) {
                    ViscriptShop.LOGGER.error("读取文件失败，该功能需要ftblibrary库才能正常工作", e);
                }
                if (tag != null && tag.contains("shopTabs")) return fxxkLegacySdmData(project, tag);
            }
            if (tag == null || !tag.contains("shop_tabs")) return "读取文件失败，请复制一份正确的商店数据文件到剪贴板";

            var shopInfoTag = new CompoundTag();
            shopInfoTag.putString("name", tag.getString("id"));
            var currencyCategory = new CompoundTag();
            currencyCategory.putString("name", "货币商店");
            currencyCategory.putString("shopType", "viscript_shop.data.category.shopType.currency");
            var currencyMerchants = new ListTag();
            var itemCategory = new CompoundTag();
            itemCategory.putString("name", "物品商店");
            var itemMerchants = new ListTag();
            var shopEntries = tag.getList("shop_entries", Tag.TAG_COMPOUND);
            for (int i = 0; i < shopEntries.size(); i++) {
                var entry = shopEntries.getCompound(i);
                boolean isCurrency = entry.getCompound("seller_type").getString("register_id").equals("money_seller");
                var entryType = entry.getCompound("entry_type");
                var merchantInfo = new CompoundTag();
                if (entryType.getString("type_id").equals("shopItemEntryType")) {
                    var itemResult = new CompoundTag();
                    var item = entryType.getCompound("itemStack");
                    item.putInt("Count", (int) entry.getLong("count"));
                    itemResult.put("item", item);
                    var renderItem = entry.getCompound("render_component").getCompound("icon");
                    if (!renderItem.isEmpty() && !renderItem.getString("id").equals("minecraft:air")) {
                        var display = new CompoundTag();
                        display.putString("renderMode", "viscript_shop.data.merchant.itemDisplay.renderMode.item_render");
                        display.put("renderItem", renderItem);
                        itemResult.put("display", display);
                    }
                    merchantInfo.put("itemResult", itemResult);
                } else continue; // 出售非物品的商店跳过，暂时没有做兼容
                int price = (int) entry.getDouble("price");
                if (isCurrency) merchantInfo.putInt("money", price);
                else {
                    var item = entry.getCompound("seller_type").getCompound("data").getCompound("money_item");
                    item.putInt("Count", price);
                    var itemA = new CompoundTag();
                    itemA.put("item", item);
                    merchantInfo.put("itemA", itemA);
                }
                int limit = entry.getInt("limiter_value");
                merchantInfo.putInt("stock", limit == 0 ? -1 : limit);
                int buyOrSell = entry.getInt("type");
                if (buyOrSell == 0) {
                    merchantInfo.putString("tradeType", "viscript_shop.data.merchant.tradeType.sell");
                    if (!isCurrency) { // 非货币商店，需要交换 itemA 和 itemResult
                        var itemA = merchantInfo.getCompound("itemA");
                        merchantInfo.put("itemA", merchantInfo.getCompound("itemResult"));
                        merchantInfo.put("itemResult", itemA);
                    }
                }
                if (isCurrency) currencyMerchants.add(merchantInfo); else itemMerchants.add(merchantInfo);
            }
            // 添加分类
            var categoryInfos = new ListTag();
            if (!currencyMerchants.isEmpty()) {
                currencyCategory.put("merchants", currencyMerchants);
                categoryInfos.add(currencyCategory);
            }
            if (!itemMerchants.isEmpty()) {
                itemCategory.put("merchants", itemMerchants);
                categoryInfos.add(itemCategory);
            }
            shopInfoTag.put("categoryInfos", categoryInfos);
            project.getShopInfo().deserializeNBT(shopInfoTag);
            if (getCurrentProject() != null) closeCurrentProject(false, null);
            loadNewProject(project, null);
            return "sdm shop 数据导入成功";
        } catch (Exception e) {
            ViscriptShop.LOGGER.error("导入数据失败", e);
        }
        return "导入数据出错，错误信息请看日志文件";
    }

    private String fxxkLegacySdmData(Shop project, CompoundTag tag) {
        try {
            var shopInfoTag = new CompoundTag();
            var categoryInfos = new ListTag();
            var shopTabs = tag.getList("shopTabs", Tag.TAG_COMPOUND);
            for (int i = 0; i < shopTabs.size(); i++) {
                var tab = shopTabs.getCompound(i);
                var iconItem = tab.getCompound("icon");

                var currencyCategory = new CompoundTag();
                currencyCategory.putString("name", tab.getString("title"));
                currencyCategory.putString("shopType", "viscript_shop.data.category.shopType.currency");
                currencyCategory.put("iconItem", iconItem);
                var currencyMerchants = new ListTag();

                var itemCategory = new CompoundTag();
                itemCategory.putString("name", tab.getString("title") + " 以物易物");
                itemCategory.put("iconItem", iconItem);
                var itemMerchants = new ListTag();

                var shopEntries = tab.getList("tabEntry", Tag.TAG_COMPOUND);
                for (int j = 0; j < shopEntries.size(); j++) {
                    var entry = shopEntries.getCompound(j);
                    boolean isCurrency = entry.getCompound("shopSeller").getString("shopSellerTypeID").equals("money");
                    var entryType = entry.getCompound("entryType");
                    var merchantInfo = new CompoundTag();
                    if (entryType.getString("shopEntryTypeID").equals("shopItemEntryType")) {
                        var itemResult = new CompoundTag();
                        var item = entryType.getCompound("itemStack");
                        item.putInt("Count", entry.getInt("entryCount"));
                        itemResult.put("item", item);
                        merchantInfo.put("itemResult", itemResult);
                    } else continue; // 出售非物品的商店跳过，暂时没有做兼容
                    int price = (int) entry.getLong("entryPrice");
                    if (isCurrency) merchantInfo.putInt("money", price);
                    else {
                        var item = entry.getCompound("shopSeller").getCompound("item");
                        item.putInt("Count", price);
                        var itemA = new CompoundTag();
                        itemA.put("item", item);
                        merchantInfo.put("itemA", itemA);
                    }
                    int limit = entry.getInt("limit");
                    merchantInfo.putInt("stock", limit == 0 ? -1 : limit);
                    byte buyOrSell = entry.getByte("isSell");
                    if (buyOrSell == 1) {
                        merchantInfo.putString("tradeType", "viscript_shop.data.merchant.tradeType.sell");
                        if (!isCurrency) { // 非货币商店，需要交换 itemA 和 itemResult
                            var itemA = merchantInfo.getCompound("itemA");
                            merchantInfo.put("itemA", merchantInfo.getCompound("itemResult"));
                            merchantInfo.put("itemResult", itemA);
                        }
                    }
                    if (isCurrency) currencyMerchants.add(merchantInfo); else itemMerchants.add(merchantInfo);
                }
                if (!currencyMerchants.isEmpty()) {
                    currencyCategory.put("merchants", currencyMerchants);
                    categoryInfos.add(currencyCategory);
                }
                if (!itemMerchants.isEmpty()) {
                    itemCategory.put("merchants", itemMerchants);
                    categoryInfos.add(itemCategory);
                }
            }
            shopInfoTag.put("categoryInfos", categoryInfos);
            project.getShopInfo().deserializeNBT(shopInfoTag);
            if (getCurrentProject() != null) closeCurrentProject(false, null);
            loadNewProject(project, null);
            return "sdm shop 数据导入成功";
        } catch (Exception e) {
            ViscriptShop.LOGGER.error("导入数据失败", e);
        }
        return "导入数据出错，错误信息请看日志文件";
    }
}

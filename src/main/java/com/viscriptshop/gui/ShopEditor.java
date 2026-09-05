package com.viscriptshop.gui;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.mojang.blaze3d.MethodsReturnNonnullByDefault;
import com.viscript_lib.gui.editor.EditorUploadAction;
import com.viscript_lib.gui.editor.FunctionFileEditor;
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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ShopEditor extends FunctionFileEditor {
    public final static ResourceLocation SHOP_ID = ViscriptShop.id("editor");

    public ShopInspectorView shopInspectorView;
    public final CategoryView categoryView = new CategoryView(this);
    public final ShopPreviewView shopPreviewView = new ShopPreviewView(this);

    public ShopEditor() {
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
}

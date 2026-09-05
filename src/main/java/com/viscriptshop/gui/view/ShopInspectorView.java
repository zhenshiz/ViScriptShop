package com.viscriptshop.gui.view;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Inspector;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.viscriptshop.gui.ShopEditor;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.promotion.PromotionResolver;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class ShopInspectorView extends View {
    private final Inspector inspector;
    private final Button shopButton;
    private final Button categoryButton;
    private final Button merchantButton;

    @Nullable
    private ShopInfo shopInfo;
    @Nullable
    private CategoryInfo categoryInfo;
    @Nullable
    private MerchantInfo merchantInfo;
    private Scope scope = Scope.SHOP;

    public ShopInspectorView(ShopEditor editor) {
        super("viscript_shop.editor.view.inspector", Icons.SETTINGS);
        this.inspector = new Inspector();
        this.inspector.setHistoryStack(editor.getHistoryView());
        this.inspector.layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
        });

        this.shopButton = createScopeButton("viscript_shop.editor.inspector.shop", () -> {
            if (shopInfo != null) inspectShop(shopInfo);
        });
        this.categoryButton = createScopeButton("viscript_shop.editor.inspector.category", () -> {
            if (categoryInfo != null) inspectCategory(categoryInfo);
        });
        this.merchantButton = createScopeButton("viscript_shop.editor.inspector.merchant", () -> {
            if (merchantInfo != null && categoryInfo != null) inspectMerchant(merchantInfo, categoryInfo);
        });

        UIElement header = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.height(18);
            layout.flexDirection(FlexDirection.ROW);
            layout.gapAll(2);
            layout.paddingAll(2);
        }).style(style -> style.backgroundTexture(Sprites.RECT_SOLID));
        header.addChildren(shopButton, categoryButton, merchantButton);

        layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        addChildren(header, inspector);
        updateButtons();
    }

    public void loadShop(ShopInfo shopInfo) {
        this.shopInfo = shopInfo;
        this.categoryInfo = null;
        this.merchantInfo = null;
        inspectShop(shopInfo);
    }

    public void inspectShop(ShopInfo shopInfo) {
        this.shopInfo = shopInfo;
        this.scope = Scope.SHOP;
        clearInspector();
        inspector.inspect(shopInfo);
        updateButtons();
    }

    public void inspectCategory(CategoryInfo categoryInfo) {
        this.categoryInfo = categoryInfo;
        this.merchantInfo = null;
        this.scope = Scope.CATEGORY;
        clearInspector();
        inspector.inspect(categoryInfo);
        updateButtons();
    }

    public void inspectMerchant(MerchantInfo merchantInfo, CategoryInfo.ShopType shopType) {
        CategoryInfo owner = categoryInfo != null
                && categoryInfo.getMerchants().stream().anyMatch(candidate -> candidate == merchantInfo)
                ? categoryInfo
                : null;
        inspectMerchant(merchantInfo, owner, shopType);
    }

    /**
     * 检查指定分类中的商品，并展示它可继承的商店级与分类级规则。
     *
     * @param merchantInfo 当前商品
     * @param categoryInfo 商品所属分类
     */
    public void inspectMerchant(MerchantInfo merchantInfo, CategoryInfo categoryInfo) {
        inspectMerchant(merchantInfo, categoryInfo, categoryInfo.resolvedShopType());
    }

    private void inspectMerchant(MerchantInfo merchantInfo,
                                 @Nullable CategoryInfo categoryInfo,
                                 CategoryInfo.ShopType shopType) {
        this.merchantInfo = merchantInfo;
        this.categoryInfo = categoryInfo;
        this.scope = Scope.MERCHANT;
        clearInspector();

        ConfiguratorGroup configurator = (ConfiguratorGroup) merchantInfo.createConfigurator(
                shopType,
                PromotionResolver.collectParentRules(shopInfo, categoryInfo)
        ).layout(layout -> layout.widthPercent(100));
        configurator.setCollapse(false);
        recordMerchantChanges(merchantInfo, configurator);
        inspector.scrollerView.addScrollViewChild(configurator);
        updateButtons();
    }

    private void clearInspector() {
        inspector.clear();
        inspector.scrollerView.clearAllScrollViewChildren();
    }

    private void recordMerchantChanges(MerchantInfo merchantInfo, ConfiguratorGroup configurator) {
        var recorder = merchantInfo.createHistoryRecorder();
        var historyStack = inspector.getHistoryStack();
        if (recorder == null || historyStack == null) return;

        configurator.addEventListener(Configurator.CHANGE_EVENT, event -> {
            if (event.target instanceof Configurator changedConfigurator) {
                var notifyName = changedConfigurator.getNotifyName();
                recorder.record(
                        historyStack,
                        notifyName.getString().isEmpty() ? Component.literal(merchantInfo.getConfigurableName()) : notifyName,
                        changedConfigurator
                );
            }
        });
    }

    private Button createScopeButton(String text, Runnable onClick) {
        return (Button) new Button().setText(text).setOnClick(event -> {
            if (event.button == 0) {
                onClick.run();
            }
        }).layout(layout -> {
            layout.flex(1);
            layout.heightPercent(100);
        });
    }

    private void updateButtons() {
        shopButton.setActive(shopInfo != null && scope != Scope.SHOP);
        categoryButton.setActive(categoryInfo != null && scope != Scope.CATEGORY);
        merchantButton.setActive(merchantInfo != null && scope != Scope.MERCHANT);
    }

    private enum Scope {
        SHOP,
        CATEGORY,
        MERCHANT
    }
}

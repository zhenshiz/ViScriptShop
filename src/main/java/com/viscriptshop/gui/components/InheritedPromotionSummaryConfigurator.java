package com.viscriptshop.gui.components;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.viscriptshop.promotion.PromotionResolver;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 在商品编辑器中只读展示商店级和分类级促销规则来源。
 */
public class InheritedPromotionSummaryConfigurator extends ConfiguratorGroup {
    /**
     * 创建上级促销摘要。
     *
     * @param parentRules 商店和分类提供的候选规则
     */
    public InheritedPromotionSummaryConfigurator(List<PromotionResolver.ScopedRule> parentRules) {
        super("viscript_shop.editor.promotion.inherited.title", true);
        setId("merchant_inherited_promotions");
        setTips("viscript_shop.editor.promotion.inherited.tip");

        List<PromotionResolver.ScopedRule> safeRules = parentRules == null ? List.of() : parentRules;
        if (safeRules.isEmpty()) {
            addConfigurator(createRow(Component.translatable(
                    "viscript_shop.editor.promotion.inherited.empty"
            )));
            return;
        }

        for (PromotionResolver.ScopedRule scopedRule : safeRules) {
            String id = scopedRule.rule().getId();
            Component source = id == null || id.isBlank()
                    ? Component.translatable("viscript_shop.promotion.source.rule")
                    : Component.literal(id);
            addConfigurator(createRow(Component.translatable(
                    "viscript_shop.editor.promotion.inherited.entry",
                    source,
                    Component.translatable(scopedRule.scope().getTranslationKey())
            )));
        }
    }

    private Configurator createRow(Component text) {
        Configurator row = new Configurator("");
        row.addInlineChild(new Label()
                .setText(text)
                .textStyle(style -> style
                        .textWrap(TextWrap.WRAP)
                        .adaptiveHeight(true)
                        .textAlignVertical(Vertical.CENTER))
                .layout(layout -> layout.widthPercent(100)));
        return row;
    }
}

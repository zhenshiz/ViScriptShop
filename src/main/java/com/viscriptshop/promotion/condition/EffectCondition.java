package com.viscriptshop.promotion.condition;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscript_lib.gui.components.search.MobEffectSearchBox;
import com.viscriptshop.gui.components.ConfiguratorFieldHelper;
import com.viscriptshop.promotion.PromotionContext;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * 判断玩家是否拥有指定 ID 和最低等级的状态效果。
 */
@Getter
@Setter
@LDLRegister(name = EffectCondition.ID, registry = PromotionCondition.REGISTRY, priority = 700)
public class EffectCondition implements PromotionCondition {
    public static final String ID = "viscript_shop:effect";

    @Configurable(name = "viscript_shop.data.promotion.condition.effect_id")
    private String effectId = "minecraft:hero_of_the_village";

    @Configurable(name = "viscript_shop.data.promotion.condition.effect_level")
    @ConfigNumber(range = {1, 256})
    private int minimumLevel = 1;

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        ResourceLocation id = ResourceLocation.tryParse(effectId == null ? "" : effectId.trim());
        var selectedEffect = id == null
                ? null
                : BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null);
        MobEffectSearchBox searchBox = new MobEffectSearchBox(selectedEffect);
        searchBox.setId("promotion_effect_search");
        searchBox.textField.setId("promotion_effect_search_input");
        searchBox.layout(layout -> {
            layout.widthPercent(100);
            layout.height(18);
        });
        searchBox.searchStyle(style -> {
            style.maxItemCount(8);
            style.scrollerViewHeight(120);
        });

        // 缺少扩展模组时仍显示原 ID，重新选择有效效果后即可恢复自动补全值。
        if (selectedEffect == null && effectId != null && !effectId.isBlank()) {
            searchBox.textField.setText(effectId);
        }

        Configurator effectConfigurator = new Configurator(
                "viscript_shop.data.promotion.condition.effect_id"
        );
        effectConfigurator.setId("promotion_effect_id");
        effectConfigurator.addInlineChild(searchBox);
        searchBox.setOnValueChanged(effect -> {
            effectId = MobEffectSearchBox.getMobEffectIdString(effect);
            effectConfigurator.notifyChanges();
        });
        father.addConfigurator(effectConfigurator);
        ConfiguratorFieldHelper.addField(father, this, "minimumLevel")
                .setId("promotion_effect_level");
    }

    @Override
    public boolean test(PromotionContext context) {
        if (context.player() == null) {
            return false;
        }
        ResourceLocation id = ResourceLocation.tryParse(effectId == null ? "" : effectId.trim());
        if (id == null) {
            return false;
        }
        var effect = BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null);
        if (effect == null) {
            return false;
        }
        var instance = context.player().getEffect(effect);
        return instance != null && instance.getAmplifier() + 1 >= Math.max(1, minimumLevel);
    }
}

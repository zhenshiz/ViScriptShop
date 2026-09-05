package com.viscriptshop.promotion.condition;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptshop.promotion.PromotionContext;
import lombok.Getter;
import lombok.Setter;

/**
 * 判断玩家经验等级是否达到指定值。
 */
@Getter
@Setter
@LDLRegister(name = ExperienceLevelCondition.ID, registry = PromotionCondition.REGISTRY, priority = 600)
public class ExperienceLevelCondition implements PromotionCondition {
    public static final String ID = "viscript_shop:experience_level";

    @Configurable(name = "viscript_shop.data.promotion.condition.experience_level")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    private int minimumLevel;

    @Override
    public boolean test(PromotionContext context) {
        return context.player() != null && context.player().experienceLevel >= Math.max(0, minimumLevel);
    }
}

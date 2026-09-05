package com.viscriptshop.promotion.condition;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptshop.ShopRegistries;
import com.viscriptshop.promotion.PromotionContext;
import lombok.Getter;
import lombok.Setter;

/**
 * 判断玩家是否拥有指定商店阶段标记。
 */
@Getter
@Setter
@LDLRegister(name = StageFlagCondition.ID, registry = PromotionCondition.REGISTRY, priority = 900)
public class StageFlagCondition implements PromotionCondition {
    public static final String ID = "viscript_shop:stage_flag";

    @Configurable(name = "viscript_shop.data.promotion.condition.stage_flag")
    private String flag = "";

    @Override
    public boolean test(PromotionContext context) {
        return context.player() != null
                && flag != null
                && !flag.isBlank()
                && context.player().getData(ShopRegistries.MONEY).getFlags().contains(flag.trim());
    }
}

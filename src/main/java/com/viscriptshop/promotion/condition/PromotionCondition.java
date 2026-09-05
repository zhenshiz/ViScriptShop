package com.viscriptshop.promotion.condition;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.registry.ILDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.viscriptshop.promotion.PromotionContext;

import java.util.function.Supplier;

/**
 * 促销条件扩展接口。
 *
 * <p>新增条件时只需实现本接口、提供无参构造器并添加 {@code @LDLRegister}。
 * 面向编辑器的条件字段使用 LDLib2 的 {@code @Configurable} 描述后，保存、加载与
 * 可视化表单均由 LDLib2 处理，不需要为每个实现修改集中枚举或手写列表界面。
 */
public interface PromotionCondition extends
        ILDLRegister<PromotionCondition, Supplier<PromotionCondition>>,
        IConfigurable,
        IPersistedSerializable {
    String REGISTRY = "viscript_shop:promotion_condition";

    /**
     * 判断当前促销上下文是否满足条件。
     *
     * @param context 只读促销上下文
     * @return 满足时返回 {@code true}
     */
    boolean test(PromotionContext context);

    /**
     * 获取可视化编辑器中的条件名称翻译键。
     *
     * <p>第三方条件可以覆写此方法使用自己的翻译键。
     *
     * @return 条件名称翻译键
     */
    default String getDisplayNameKey() {
        String registeredName = name();
        int separator = registeredName.indexOf(':');
        String path = separator >= 0 ? registeredName.substring(separator + 1) : registeredName;
        return "viscript_shop.promotion.condition." + path;
    }
}

package com.viscriptshop.promotion;

import com.lowdragmc.lowdraglib2.registry.AutoRegistry;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.promotion.condition.PromotionCondition;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 保存促销系统提供给其他模组扩展的 LDLib2 注册表。
 */
public final class PromotionRegistries {
    public static final AutoRegistry.LDLibRegister<PromotionCondition, Supplier<PromotionCondition>> CONDITIONS =
            AutoRegistry.LDLibRegister.create(
                    ViscriptShop.id("promotion_condition"),
                    PromotionCondition.class,
                    AutoRegistry::noArgsCreator
            );

    private PromotionRegistries() {
    }

    /**
     * 在模组构造阶段触发类初始化和条件扫描。
     */
    public static void init() {
        CONDITIONS.keys();
    }

    /**
     * 按注册优先级返回所有可用条件 ID，供可视化编辑器构建选项。
     *
     * @return 条件注册 ID 列表
     */
    public static List<String> conditionIds() {
        List<String> ids = new ArrayList<>();
        for (var holder : CONDITIONS) {
            ids.add(holder.annotation().name());
        }
        return ids;
    }

    /**
     * 根据注册 ID 创建一个全新的条件实例。
     *
     * @param typeId 条件注册 ID
     * @return 条件实例；未注册时返回 {@code null}
     */
    @Nullable
    public static PromotionCondition createCondition(String typeId) {
        var holder = CONDITIONS.get(typeId);
        return holder == null ? null : holder.value().get();
    }

    /**
     * 获取条件在编辑器中使用的翻译键。
     *
     * @param typeId 条件注册 ID
     * @return 翻译键；未知条件返回原始 ID
     */
    public static String conditionName(String typeId) {
        var holder = CONDITIONS.get(typeId);
        if (holder == null) {
            return typeId == null || typeId.isBlank()
                    ? "viscript_shop.promotion.condition.unknown"
                    : typeId;
        }
        return holder.value().get().getDisplayNameKey();
    }
}

package com.viscriptshop.promotion;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 促销计算产生的展示与审计信息。
 */
public final class PromotionResult {
    private PromotionResult() {
    }

    /**
     * 单个价格槽的折扣结果。
     *
     * @param target 价格槽
     * @param baseAmount 原始单价或原始物品数量
     * @param finalAmount 最终单价或最终物品数量
     * @param totalRate 聚合后的带符号变化率，负数表示减少
     * @param details 命中的规则明细
     */
    public record PriceAdjustment(
            PromotionRule.Target target,
            double baseAmount,
            double finalAmount,
            double totalRate,
            List<DiscountDetail> details
    ) {
        public PriceAdjustment {
            details = details == null ? List.of() : List.copyOf(details);
        }

        /**
         * 判断最终价格是否发生变化。
         *
         * @return 发生变化时返回 {@code true}
         */
        public boolean hasChange() {
            return Math.abs(baseAmount - finalAmount) > 1.0E-9;
        }

        /**
         * 将物品槽结果转换为安全整数。
         *
         * @return 物品数量
         */
        public int finalItemCount() {
            if (finalAmount >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            return Math.max(0, (int) finalAmount);
        }
    }

    /**
     * 一条命中折扣的来源明细。
     *
     * @param source 规则 ID 或默认来源翻译键
     * @param signedRate 带符号变化率
     * @param scope 规则所在的数据层级
     */
    public record DiscountDetail(
            String source,
            double signedRate,
            PromotionResolver.Scope scope
    ) {
        public DiscountDetail {
            source = source == null ? "" : source;
            scope = scope == null ? PromotionResolver.Scope.MERCHANT : scope;
        }
    }

    /**
     * 一条买赠规则产生的赠品明细。
     *
     * @param item 赠品物品
     * @param count 最终数量
     * @param source 规则 ID 或默认来源翻译键
     * @param scope 规则所在的数据层级
     */
    public record BonusDetail(
            ItemStack item,
            long count,
            String source,
            PromotionResolver.Scope scope
    ) {
        public BonusDetail {
            item = item == null ? ItemStack.EMPTY : item.copy();
            count = Math.max(0L, count);
            source = source == null ? "" : source;
            scope = scope == null ? PromotionResolver.Scope.MERCHANT : scope;
        }
    }
}

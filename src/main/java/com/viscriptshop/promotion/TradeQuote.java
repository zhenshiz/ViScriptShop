package com.viscriptshop.promotion;

import com.viscriptshop.gui.data.AggregatedResources;

import java.util.ArrayList;
import java.util.List;

/** 购物车的完整成本（含优惠券）、收益、赠品，以及需从随身栏位扣除的优惠凭证。 */
public record TradeQuote(
        AggregatedResources cost,
        AggregatedResources gain,
        List<PromotionResult.BonusDetail> bonuses,
        List<AggregatedResources.ItemEntry> conditionCosts
) {
    public TradeQuote(AggregatedResources cost, AggregatedResources gain, List<PromotionResult.BonusDetail> bonuses) {
        this(cost, gain, bonuses, List.of());
    }

    public TradeQuote {
        cost = cost == null ? new AggregatedResources() : cost;
        gain = gain == null ? new AggregatedResources() : gain;
        bonuses = bonuses == null ? List.of() : List.copyOf(bonuses);
        conditionCosts = conditionCosts == null ? List.of() : conditionCosts.stream()
                .map(entry -> entry.copyWithCount(entry.getCount())).toList();
    }

    /** 优惠券独立预留和扣除；从总成本中减去它们，防止同一券扣两次。 */
    public List<AggregatedResources.ItemEntry> regularItemCosts() {
        List<AggregatedResources.ItemEntry> entries = new ArrayList<>();
        cost.getItemEntries().forEach(entry -> entries.add(entry.copyWithCount(entry.getCount())));
        for (var condition : conditionCosts) {
            long remaining = condition.getCount();
            for (var entry : entries) {
                if (remaining > 0 && entry.canMerge(condition.getItemStack(), condition.getMatchRule())) {
                    long amount = Math.min(remaining, entry.getCount());
                    entry.setCount(entry.getCount() - amount);
                    remaining -= amount;
                }
            }
        }
        entries.removeIf(entry -> entry.getCount() <= 0);
        return entries;
    }
}

package com.viscriptshop.promotion;

import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.ShopInfo;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 按商店、分类、商品三个数据层级解析一次交易实际使用的促销配置。
 */
public final class PromotionResolver {
    private PromotionResolver() {
    }

    /**
     * 解析商品最终继承的规则及合并方式。
     *
     * <p>未启用某一级促销设置时，该级不提供本地规则或合并方式，但分类和商品仍沿用上级规则。
     * 启用分类设置并关闭继承时只切断商店规则；启用商品设置并关闭继承时切断全部上级规则。
     * 合并方式采用距离商品最近的已启用且非“继承”设置。
     *
     * @param shop 当前商店
     * @param category 当前分类
     * @param merchant 当前商品
     * @return 完整且不可变的解析结果
     */
    public static ResolvedPromotions resolve(ShopInfo shop,
                                             CategoryInfo category,
                                             MerchantInfo merchant) {
        List<ScopedRule> rules = new ArrayList<>();
        PromotionRule.AggregationMode aggregation = PromotionRule.AggregationMode.ADD;
        if (merchant == null) {
            return new ResolvedPromotions(rules, aggregation);
        }

        boolean merchantInherits = !merchant.isPromotionEnabled()
                || merchant.isInheritParentPromotions();
        if (merchantInherits) {
            if (category != null) {
                boolean categoryInherits = !category.isPromotionEnabled()
                        || category.isInheritParentPromotions();
                if (categoryInherits && shop != null && shop.isPromotionEnabled()) {
                    addRules(rules, shop.getPromotionRules(), Scope.SHOP);
                    aggregation = shop.resolvedPromotionAggregation();
                }
                if (category.isPromotionEnabled()) {
                    addRules(rules, category.getPromotionRules(), Scope.CATEGORY);
                    aggregation = category.resolvePromotionAggregation(aggregation);
                }
            } else if (shop != null && shop.isPromotionEnabled()) {
                addRules(rules, shop.getPromotionRules(), Scope.SHOP);
                aggregation = shop.resolvedPromotionAggregation();
            }
        }

        if (merchant.isPromotionEnabled()) {
            addRules(rules, merchant.getPromotionRules(), Scope.MERCHANT);
            aggregation = merchant.resolvePromotionAggregation(aggregation);
        }
        return new ResolvedPromotions(rules, aggregation);
    }

    /**
     * 选出商品唯一的赠品规则，供预览和结算共用。
     *
     * <p>商品优先于分类，分类优先于全店；同层取列表中的第一条有效规则。
     * 先选择规则，再判断其购买门槛和附加条件，避免实际发放与预览切换到不同赠品。
     * 旧配置中的其他赠品规则仍保留，但不叠加。
     */
    public static Optional<ScopedRule> resolveGift(ShopInfo shop,
                                                  CategoryInfo category,
                                                  MerchantInfo merchant) {
        ScopedRule selected = null;
        for (ScopedRule scopedRule : resolve(shop, category, merchant).rules()) {
            PromotionRule rule = scopedRule.rule();
            if (!rule.isEnabled() || rule.resolvedType() != PromotionRule.PromotionType.BUY_GET
                    || rule.getBuyThreshold() <= 0 || rule.getGiftCount() <= 0
                    || (rule.getGiftItem().isEmpty() && merchant.getItemResult().isEmpty())) {
                continue;
            }
            if (selected == null || scopedRule.scope().ordinal() > selected.scope().ordinal()) {
                selected = scopedRule;
            }
        }
        return Optional.ofNullable(selected);
    }

    /**
     * 收集商品编辑器中需要展示的上级候选规则。
     *
     * <p>本方法不读取商品的继承开关，因此用户切换开关时摘要内容保持稳定。未启用分类
     * 促销设置时默认继续继承；启用后，分类的继承开关决定商店级规则是否可达。
     *
     * @param shop 当前商店
     * @param category 当前分类
     * @return 商店和分类提供的不可变规则列表
     */
    public static List<ScopedRule> collectParentRules(ShopInfo shop, CategoryInfo category) {
        List<ScopedRule> rules = new ArrayList<>();
        if (category != null) {
            boolean categoryInherits = !category.isPromotionEnabled()
                    || category.isInheritParentPromotions();
            if (categoryInherits && shop != null && shop.isPromotionEnabled()) {
                addRules(rules, shop.getPromotionRules(), Scope.SHOP);
            }
            if (category.isPromotionEnabled()) {
                addRules(rules, category.getPromotionRules(), Scope.CATEGORY);
            }
        } else if (shop != null && shop.isPromotionEnabled()) {
            addRules(rules, shop.getPromotionRules(), Scope.SHOP);
        }
        return List.copyOf(rules);
    }

    private static void addRules(List<ScopedRule> target,
                                 List<PromotionRule> source,
                                 Scope scope) {
        if (source == null) {
            return;
        }
        for (PromotionRule rule : source) {
            if (rule != null) {
                target.add(new ScopedRule(rule, scope));
            }
        }
    }

    /**
     * 规则在商店数据树中的来源层级。
     */
    @Getter
    public enum Scope {
        SHOP("viscript_shop.promotion.scope.shop"),
        CATEGORY("viscript_shop.promotion.scope.category"),
        MERCHANT("viscript_shop.promotion.scope.merchant");

        private final String translationKey;

        Scope(String translationKey) {
            this.translationKey = translationKey;
        }
    }

    /**
     * 一条带来源层级的规则。
     *
     * @param rule 促销规则
     * @param scope 规则来源层级
     */
    public record ScopedRule(PromotionRule rule, Scope scope) {
    }

    /**
     * 一次层级解析的最终结果。
     *
     * @param rules 按商店、分类、商品顺序排列的规则
     * @param aggregation 最终使用的具体合并方式
     */
    public record ResolvedPromotions(
            List<ScopedRule> rules,
            PromotionRule.AggregationMode aggregation
    ) {
        public ResolvedPromotions {
            rules = rules == null ? List.of() : List.copyOf(rules);
            aggregation = aggregation == null ? PromotionRule.AggregationMode.ADD : aggregation;
        }
    }
}

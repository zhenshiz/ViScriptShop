package com.viscriptshop.promotion;

import com.viscript_lib.util.item.ViScriptItemStack;
import com.viscriptshop.gui.data.AggregatedResources;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.promotion.PromotionResult.BonusDetail;
import com.viscriptshop.promotion.PromotionResult.DiscountDetail;
import com.viscriptshop.promotion.PromotionResult.PriceAdjustment;
import com.viscriptshop.promotion.PromotionResolver.ResolvedPromotions;
import com.viscriptshop.promotion.PromotionResolver.ScopedRule;
import com.viscriptshop.promotion.condition.PromotionConditionEntry;
import com.viscriptshop.promotion.condition.PlayerItemCondition;
import com.viscriptshop.util.MoneyUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;

/**
 * 商店促销与最终报价的唯一计算入口。
 *
 * <p>客户端展示和服务端结算必须调用本类。折扣先按单次交易价格计算，再乘购物车购买倍数；
 * 物品成本最低为一个，虚拟货币允许被减免为零。虚拟货币购买成本和出售收益使用不同目标，
 * 因而既能配置货币折扣，也能配置活动期间的出售加成。
 */
public final class PromotionEngine {
    private PromotionEngine() {
    }

    /**
     * 根据客户端商店数据中的购买倍数生成当前购物车报价。
     *
     * @param player 当前玩家
     * @param shopLocation 商店文件位置
     * @param shopInfo 商店数据
     * @return 完整报价
     */
    public static TradeQuote quoteCurrentCart(@Nullable Player player, String shopLocation, ShopInfo shopInfo) {
        AggregatedResources request = new AggregatedResources();
        if (shopInfo != null) {
            for (CategoryInfo category : shopInfo.getCategoryInfos()) {
                for (MerchantInfo merchant : category.getMerchants()) {
                    int quantity = merchant.getBuyCount() == null ? 0 : merchant.getBuyCount().intValue();
                    if (quantity > 0) {
                        request.getPurchaseEntries().add(new AggregatedResources.PurchaseEntry(
                                category.getId(), merchant.getId(), quantity
                        ));
                    }
                }
            }
        }
        return quote(player, shopLocation, shopInfo, request);
    }

    /**
     * 根据最小购买请求生成服务端权威报价。
     *
     * <p>客户端提交的资源汇总不会被信任；这里只读取分类 ID、商品 ID 和购买倍数，
     * 所有价格、物品、赠品与条件都从服务端商店数据重新计算。
     *
     * @param player 当前玩家
     * @param shopLocation 商店文件位置
     * @param shopInfo 服务端商店数据
     * @param request 最小购买请求
     * @return 完整报价
     */
    public static TradeQuote quote(@Nullable Player player,
                                   String shopLocation,
                                   ShopInfo shopInfo,
                                   AggregatedResources request) {
        AggregatedResources cost = new AggregatedResources();
        AggregatedResources gain = new AggregatedResources();
        List<BonusDetail> bonuses = new ArrayList<>();
        AggregatedResources conditionCosts = new AggregatedResources();
        if (shopInfo == null || request == null) {
            return new TradeQuote(cost, gain, bonuses);
        }

        List<CartLine> cart = createCartSnapshot(shopInfo, request);
        for (CartLine line : cart) {
            CategoryInfo category = line.categoryInfo();
            MerchantInfo merchant = line.merchantInfo();
            int quantity = line.quantity();
            cost.getPurchaseEntries().add(new AggregatedResources.PurchaseEntry(
                    category.getId(), merchant.getId(), quantity
            ));
            gain.getPurchaseEntries().add(new AggregatedResources.PurchaseEntry(
                    category.getId(), merchant.getId(), quantity
            ));

            PromotionContext context = new PromotionContext(
                    player, shopLocation, shopInfo, category, merchant, quantity
            );
            Set<PromotionRule> appliedRules = new LinkedHashSet<>();
            switch (category.getShopType()) {
                case ITEM_FOR_ITEM -> {
                    addDiscountedItemCost(cost, merchant.getSerializedItemA(), merchant.getItemAMatchRule(),
                            quantity, PromotionRule.Target.ITEM_A, context, appliedRules);
                    addDiscountedItemCost(cost, merchant.getSerializedItemB(), merchant.getItemBMatchRule(),
                            quantity, PromotionRule.Target.ITEM_B, context, appliedRules);
                    gain.addItem(merchant.getSerializedItemResult(), quantity);
                }
                case CURRENCY -> {
                    if (merchant.getTradeType() == MerchantInfo.TradeType.BUY) {
                        PriceAdjustment adjustment = calculateMoneyPrice(
                                context, PromotionRule.Target.MONEY_COST, merchant.getMoney(), appliedRules
                        );
                        cost.addMoney(adjustment.finalAmount(), quantity);
                        gain.addItem(merchant.getSerializedItemResult(), quantity);
                    } else {
                        addDiscountedItemCost(cost, merchant.getSerializedItemResult(), null,
                                quantity, PromotionRule.Target.SELL_ITEM_COST, context, appliedRules);
                        PriceAdjustment adjustment = calculateMoneyPrice(
                                context, PromotionRule.Target.MONEY_REWARD, merchant.getMoney(), appliedRules
                        );
                        gain.addMoney(adjustment.finalAmount(), quantity);
                    }
                }
            }

            gain.addXp(merchant.getXp(), quantity);
            merchant.getCommands().forEach(gain::addCommand);
            addBonuses(gain, bonuses, context, quantity, appliedRules);
            for (PromotionRule rule : appliedRules) {
                for (PromotionConditionEntry entry : rule.getConditions()) {
                    if (entry.getCondition() instanceof PlayerItemCondition item && item.isConsumeOnPurchase()) {
                        conditionCosts.addItemEntry(item.getItem(), quantity, item.getMatchRule());
                    }
                }
            }
        }
        for (AggregatedResources.ItemEntry entry : conditionCosts.getItemEntries()) {
            cost.addItemEntry(entry.getItemStack().copyWithCount(1), entry.getCount(), entry.getMatchRule());
        }
        return new TradeQuote(cost, gain, bonuses, conditionCosts.getItemEntries());
    }

    /**
     * 为商品列表计算一个物品成本槽的当前单次价格。
     *
     * @param player 当前玩家
     * @param shopLocation 商店文件位置
     * @param shopInfo 商店数据
     * @param category 当前分类
     * @param merchant 当前商品
     * @param target 物品成本槽
     * @param baseCount 原始数量
     * @return 折扣结果
     */
    public static PriceAdjustment calculateItemPrice(@Nullable Player player,
                                                      String shopLocation,
                                                      ShopInfo shopInfo,
                                                      CategoryInfo category,
                                                      MerchantInfo merchant,
                                                      PromotionRule.Target target,
                                                      int baseCount) {
        PromotionContext context = new PromotionContext(
                player,
                shopLocation,
                shopInfo,
                category,
                merchant,
                merchant == null || merchant.getBuyCount() == null
                        ? 0
                        : merchant.getBuyCount().intValue()
        );
        return calculateItemPrice(context, target, baseCount);
    }

    /**
     * 为商品列表计算虚拟货币的当前单次价格或收益。
     *
     * @param player 当前玩家
     * @param shopLocation 商店文件位置
     * @param shopInfo 商店数据
     * @param category 当前分类
     * @param merchant 当前商品
     * @param target 货币成本或货币收益槽
     * @param baseAmount 原始金额
     * @return 折扣结果
     */
    public static PriceAdjustment calculateMoneyPrice(@Nullable Player player,
                                                       String shopLocation,
                                                       ShopInfo shopInfo,
                                                       CategoryInfo category,
                                                       MerchantInfo merchant,
                                                       PromotionRule.Target target,
                                                       double baseAmount) {
        PromotionContext context = new PromotionContext(
                player,
                shopLocation,
                shopInfo,
                category,
                merchant,
                merchant == null || merchant.getBuyCount() == null
                        ? 0
                        : merchant.getBuyCount().intValue()
        );
        return calculateMoneyPrice(context, target, baseAmount);
    }

    private static void addDiscountedItemCost(AggregatedResources cost,
                                              ViScriptItemStack item,
                                              com.viscriptshop.gui.data.ItemMatchRule matchRule,
                                              int quantity,
                                              PromotionRule.Target target,
                                              PromotionContext context,
                                              Set<PromotionRule> appliedRules) {
        if (item == null || item.toItemStack().isEmpty()) {
            return;
        }
        PriceAdjustment adjustment = calculateItemPrice(context, target, item.toItemStack().getCount(), appliedRules);
        cost.addItemEntry(item.copyWithCount(adjustment.finalItemCount()), quantity, matchRule);
    }

    private static PriceAdjustment calculateItemPrice(PromotionContext context,
                                                       PromotionRule.Target target,
                                                       int baseCount) {
        return calculateItemPrice(context, target, baseCount, null);
    }

    private static PriceAdjustment calculateItemPrice(PromotionContext context, PromotionRule.Target target,
                                                       int baseCount, @Nullable Set<PromotionRule> appliedRules) {
        RateAggregation aggregation = calculateRate(context, target);
        int normalizedBase = Math.max(0, baseCount);
        BigDecimal calculated = BigDecimal.valueOf(normalizedBase)
                .multiply(BigDecimal.valueOf(aggregation.factor()))
                .setScale(0, RoundingMode.FLOOR);
        double finalAmount = normalizedBase == 0
                ? 0
                : Math.max(1, calculated.min(BigDecimal.valueOf(Integer.MAX_VALUE)).intValue());
        if (appliedRules != null && finalAmount != normalizedBase) appliedRules.addAll(aggregation.rules());
        return new PriceAdjustment(
                target,
                normalizedBase,
                finalAmount,
                aggregation.factor() - 1,
                aggregation.details()
        );
    }

    private static PriceAdjustment calculateMoneyPrice(PromotionContext context,
                                                        PromotionRule.Target target,
                                                        double baseAmount) {
        return calculateMoneyPrice(context, target, baseAmount, null);
    }

    private static PriceAdjustment calculateMoneyPrice(PromotionContext context, PromotionRule.Target target,
                                                        double baseAmount, @Nullable Set<PromotionRule> appliedRules) {
        RateAggregation aggregation = calculateRate(context, target);
        double normalizedBase = MoneyUtil.normalize(baseAmount);
        double finalAmount;
        if (normalizedBase == 0 || aggregation.factor() == 0) {
            finalAmount = 0;
        } else {
            finalAmount = MoneyUtil.multiply(normalizedBase, aggregation.factor());
        }
        if (appliedRules != null && finalAmount != normalizedBase) appliedRules.addAll(aggregation.rules());
        return new PriceAdjustment(
                target,
                normalizedBase,
                finalAmount,
                aggregation.factor() - 1,
                aggregation.details()
        );
    }

    private static RateAggregation calculateRate(PromotionContext context, PromotionRule.Target target) {
        List<BigDecimal> rates = new ArrayList<>();
        List<PromotionRule> rules = new ArrayList<>();
        List<DiscountDetail> details = new ArrayList<>();
        ResolvedPromotions promotions = PromotionResolver.resolve(
                context.shopInfo(),
                context.categoryInfo(),
                context.merchantInfo()
        );
        for (ScopedRule scopedRule : promotions.rules()) {
            PromotionRule rule = scopedRule.rule();
            if (!rule.isEnabled()
                    || rule.resolvedType() != PromotionRule.PromotionType.DISCOUNT
                    || !rule.matchesTarget(target)
                    || !conditionsMet(rule, context)) {
                continue;
            }
            double percentage = Double.isFinite(rule.getPercentage())
                    ? Math.max(0, rule.getPercentage())
                    : 0;
            BigDecimal rate = BigDecimal.valueOf(percentage).movePointLeft(2);
            // 方向描述玩家是否受益：优惠会降低支出，但会提高出售收入。
            boolean reduceNumericValue = rule.resolvedDirection() == PromotionRule.Direction.REDUCE;
            if (target == PromotionRule.Target.MONEY_REWARD) {
                reduceNumericValue = !reduceNumericValue;
            }
            if (reduceNumericValue) {
                rate = rate.negate();
            }
            rates.add(rate);
            rules.add(rule);
            String source = rule.getId() == null || rule.getId().isBlank()
                    ? "viscript_shop.promotion.source.rule"
                    : rule.getId();
            details.add(new DiscountDetail(source, rate.doubleValue(), scopedRule.scope()));
        }

        if (rates.isEmpty()) {
            return new RateAggregation(1, details, List.of());
        }
        PromotionRule.AggregationMode mode = promotions.aggregation();
        BigDecimal totalRate = switch (mode) {
            case MAX -> rates.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            case MIN -> rates.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            // 单条减价超过 100% 时按免费处理，不能让两个负因子相乘后反而恢复原价。
            case MULTIPLY -> rates.stream()
                    .map(rate -> BigDecimal.ONE.add(rate).max(BigDecimal.ZERO))
                    .reduce(BigDecimal.ONE, BigDecimal::multiply)
                    .subtract(BigDecimal.ONE);
            case ADD -> rates.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        };
        BigDecimal factor = BigDecimal.ONE.add(totalRate).max(BigDecimal.ZERO);
        double safeFactor = factor.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) >= 0
                ? Double.MAX_VALUE
                : factor.doubleValue();
        // MAX/MIN 只使用选中的一条规则；其余规则的优惠券不能被扣除。
        List<PromotionRule> contributing = new ArrayList<>();
        if (mode == PromotionRule.AggregationMode.MAX || mode == PromotionRule.AggregationMode.MIN) {
            int selected = rates.indexOf(totalRate);
            if (selected >= 0 && rates.get(selected).signum() != 0) contributing.add(rules.get(selected));
        } else {
            for (int i = 0; i < rates.size(); i++) {
                if (rates.get(i).signum() != 0) contributing.add(rules.get(i));
            }
        }
        return new RateAggregation(safeFactor, details, contributing);
    }

    private static boolean conditionsMet(PromotionRule rule, PromotionContext context) {
        List<PromotionConditionEntry> conditions = rule.getConditions();
        if (conditions.isEmpty()) {
            return true;
        }
        for (PromotionConditionEntry condition : conditions) {
            if (condition == null || !condition.test(context)) {
                return false;
            }
        }
        return true;
    }

    private static void addBonuses(AggregatedResources gain,
                                   List<BonusDetail> bonuses,
                                   PromotionContext context,
                                   int purchaseQuantity,
                                   Set<PromotionRule> appliedRules) {
        var selected = PromotionResolver.resolveGift(
                context.shopInfo(), context.categoryInfo(), context.merchantInfo());
        if (selected.isEmpty()) {
            return;
        }
        ScopedRule scopedRule = selected.get();
        PromotionRule rule = scopedRule.rule();
        if (!conditionsMet(rule, context)) {
            return;
        }
        int groups = purchaseQuantity / rule.getBuyThreshold();
        long totalGiftCount = saturatedMultiply(groups, rule.getGiftCount());
        if (totalGiftCount <= 0) {
            return;
        }
        ItemStack gift = rule.getGiftItem();
        if (gift.isEmpty()) {
            gift = context.merchantInfo().getItemResult();
        }
        if (gift.isEmpty()) return;
        appliedRules.add(rule);
        ItemStack unitGift = gift.copyWithCount(1);
        gain.addItem(unitGift, totalGiftCount);
        String source = rule.getId() == null || rule.getId().isBlank()
                ? "viscript_shop.promotion.source.rule"
                : rule.getId();
        bonuses.add(new BonusDetail(unitGift, totalGiftCount, source, scopedRule.scope()));
    }

    private static List<CartLine> createCartSnapshot(ShopInfo shopInfo, AggregatedResources request) {
        Map<CartKey, Integer> quantities = new LinkedHashMap<>();
        for (AggregatedResources.PurchaseEntry entry : request.getPurchaseEntries()) {
            if (entry == null || entry.getBuyCount() <= 0) {
                continue;
            }
            CartKey key = new CartKey(entry.getCategoryId(), entry.getMerchantId());
            quantities.merge(key, entry.getBuyCount(), PromotionEngine::saturatedAdd);
        }

        List<CartLine> cart = new ArrayList<>();
        for (Map.Entry<CartKey, Integer> requested : quantities.entrySet()) {
            CategoryInfo category = findCategory(shopInfo, requested.getKey().categoryId());
            if (category == null) {
                continue;
            }
            MerchantInfo merchant = findMerchant(category, requested.getKey().merchantId());
            if (merchant != null) {
                cart.add(new CartLine(category, merchant, requested.getValue()));
            }
        }
        return List.copyOf(cart);
    }

    @Nullable
    private static CategoryInfo findCategory(ShopInfo shopInfo, String categoryId) {
        for (CategoryInfo category : shopInfo.getCategoryInfos()) {
            if (category.getId().equals(categoryId)) {
                return category;
            }
        }
        return null;
    }

    @Nullable
    private static MerchantInfo findMerchant(CategoryInfo category, String merchantId) {
        for (MerchantInfo merchant : category.getMerchants()) {
            if (merchant.getId().equals(merchantId)) {
                return merchant;
            }
        }
        return null;
    }

    private static long saturatedMultiply(long left, long right) {
        if (left <= 0 || right <= 0) return 0L;
        if (left > Long.MAX_VALUE / right) return Long.MAX_VALUE;
        return left * right;
    }

    private static int saturatedAdd(int left, int right) {
        long result = (long) left + right;
        return result >= Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(0, (int) result);
    }

    private record CartKey(String categoryId, String merchantId) {
    }

    private record CartLine(CategoryInfo categoryInfo, MerchantInfo merchantInfo, int quantity) {
    }

    private record RateAggregation(double factor, List<DiscountDetail> details, List<PromotionRule> rules) {
    }
}

package com.viscriptshop.gui.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.Getter;
import lombok.Setter;
import com.viscriptshop.promotion.PromotionEngine;
import com.viscriptshop.promotion.PromotionContext;
import com.viscriptshop.promotion.PromotionResolver;
import com.viscriptshop.promotion.PromotionRule;
import com.viscriptshop.util.ViScriptShopServerUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端提供的单个商店展示条目，不包含交易指令、库存或促销执行规则。
 *
 * <p>保留对象身份作为 JEI 配方身份；内容比较使用序列化数据，避免物品渲染副本影响相等判断。
 */
@Getter
@Setter
public class ShopDisplayEntry implements IPersistedSerializable {
    @Persisted private String shopLocation = "";
    @Persisted private String shopName = "";
    @Persisted private String categoryId = "";
    @Persisted private String categoryName = "";
    @Persisted private String merchantId = "";
    @Persisted private CategoryInfo.ShopType shopType = CategoryInfo.ShopType.ITEM_FOR_ITEM;
    @Persisted private MerchantInfo.TradeType tradeType = MerchantInfo.TradeType.BUY;
    @Persisted private MerchantItemInfo itemA = new MerchantItemInfo();
    @Persisted private MerchantItemInfo itemB = new MerchantItemInfo();
    @Persisted private MerchantItemInfo result = new MerchantItemInfo();
    @Persisted private double money;
    @Persisted private double baseMoney;
    @Persisted private int itemACount;
    @Persisted private int itemBCount;
    @Persisted private int resultCount;
    @Persisted private boolean locked;
    @Persisted private List<Component> stageTooltips = new ArrayList<>();
    @Persisted private MerchantItemInfo gift = new MerchantItemInfo();
    @Persisted private int giftThreshold;
    @Persisted private boolean giftConditionsMet;

    /**
     * 提取单个商品的当前玩家价格、阶段、赠品和外观信息。
     *
     * @param player 报价所属玩家
     * @param location 服务端商店相对路径，不含扩展名
     * @param shop 商品所属商店
     * @param category 商品所属分类
     * @param merchant 待展示商品
     * @return 保留完整物品数量的展示条目
     */
    public static ShopDisplayEntry of(ServerPlayer player, String location, ShopInfo shop, CategoryInfo category, MerchantInfo merchant) {
        var entry = new ShopDisplayEntry();
        entry.shopLocation = location;
        entry.shopName = shop.getName();
        entry.categoryId = category.getId();
        entry.categoryName = category.getName();
        entry.merchantId = merchant.getId();
        entry.shopType = category.getShopType();
        entry.tradeType = merchant.getTradeType();
        entry.itemA = new MerchantItemInfo(merchant.getItemAInfo().getSerializedItem(), merchant.getItemADisplay());
        entry.itemB = new MerchantItemInfo(merchant.getItemBInfo().getSerializedItem(), merchant.getItemBDisplay());
        entry.result = new MerchantItemInfo(merchant.getItemResultInfo().getSerializedItem(), merchant.getItemResultDisplay());
        entry.baseMoney = merchant.getMoney();
        entry.money = entry.baseMoney;
        entry.itemACount = merchant.getItemA().getCount();
        entry.itemBCount = merchant.getItemB().getCount();
        entry.resultCount = merchant.getItemResult().getCount();
        if (entry.isCurrency()) {
            entry.money = PromotionEngine.calculateMoneyPrice(player, location, shop, category, merchant,
                    entry.isSelling() ? PromotionRule.Target.MONEY_REWARD : PromotionRule.Target.MONEY_COST,
                    entry.baseMoney).finalAmount();
            if (entry.isSelling()) {
                entry.resultCount = PromotionEngine.calculateItemPrice(player, location, shop, category, merchant,
                        PromotionRule.Target.SELL_ITEM_COST, entry.resultCount).finalItemCount();
            }
        } else {
            entry.itemACount = PromotionEngine.calculateItemPrice(player, location, shop, category, merchant,
                    PromotionRule.Target.ITEM_A, entry.itemACount).finalItemCount();
            entry.itemBCount = PromotionEngine.calculateItemPrice(player, location, shop, category, merchant,
                    PromotionRule.Target.ITEM_B, entry.itemBCount).finalItemCount();
        }
        var flags = ViScriptShopServerUtil.getStageFlags(player);
        entry.locked = !category.canAccess(flags) || !merchant.canAccess(flags);
        appendStage(entry.stageTooltips, category, "viscript_shop.promotion.scope.category", flags);
        appendStage(entry.stageTooltips, merchant, "viscript_shop.promotion.scope.merchant", flags);
        PromotionResolver.resolveGift(shop, category, merchant).ifPresent(scoped -> {
            var rule = scoped.rule();
            entry.gift = rule.getGiftItem().isEmpty()
                    ? new MerchantItemInfo(merchant.getSerializedItemResult().copyWithCount(rule.getGiftCount()), merchant.getItemResultDisplay())
                    : new MerchantItemInfo(rule.getSerializedGiftItem().copyWithCount(rule.getGiftCount()), null);
            entry.giftThreshold = rule.getBuyThreshold();
            var context = new PromotionContext(player, location, shop, category, merchant, entry.giftThreshold);
            entry.giftConditionsMet = rule.getConditions().stream().allMatch(condition -> condition.test(context));
        });
        return entry;
    }

    private static void appendStage(List<Component> lines, StageRestricted restricted, String scope, List<String> flags) {
        var tooltips = restricted.getLockTooltips(flags);
        if (tooltips.isEmpty()) return;
        lines.add(Component.translatable(scope));
        lines.add(Component.translatable("viscript_shop.jei.stage.locked"));
        lines.addAll(tooltips);
    }

    /** @return 此条目属于虚拟货币交易时返回 {@code true} */
    public boolean isCurrency() {
        return shopType == CategoryInfo.ShopType.CURRENCY;
    }

    /** @return 玩家交付物品并获得虚拟货币时返回 {@code true} */
    public boolean isSelling() {
        return isCurrency() && tradeType == MerchantInfo.TradeType.SELL;
    }
}

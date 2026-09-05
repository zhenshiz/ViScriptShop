package com.viscriptshop.gui.components;

import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.viscript_lib.util.CountTextUtil;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.MerchantItemInfo;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.promotion.PromotionResolver;
import com.viscriptshop.promotion.PromotionRule;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 商店卡片和编辑器共用的单个赠品预览。展示每次达到门槛的赠送数量，实际获赠数量由购物栏报价决定。
 */
public final class MerchantGiftPreview {
    private MerchantGiftPreview() {
    }

    public static Optional<MerchantItemAmountDisplay> create(ShopInfo shop,
                                                        CategoryInfo category,
                                                        MerchantInfo merchant,
                                                        String id) {
        return PromotionResolver.resolveGift(shop, category, merchant).map(scopedRule -> {
            PromotionRule rule = scopedRule.rule();
            ItemStack gift = rule.getGiftItem();
            MerchantItemInfo giftInfo = gift.isEmpty()
                    ? merchant.getItemResultInfo()
                    : new MerchantItemInfo(gift, null);
            MerchantItemAmountDisplay display = MerchantItemAmountDisplay.count(
                    giftInfo, id + "_gift_0",
                    () -> Component.literal(CountTextUtil.formatCount((long) rule.getGiftCount()))
            ).withGiftBadge();
            display.appendDisplayHoverTooltips(() -> {
                List<Component> lines = new ArrayList<>();
                lines.add(Component.translatable("viscript_shop.ui.promotion.gift_preview",
                        rule.getBuyThreshold(), rule.getGiftCount()));
                Component source = rule.getId() == null || rule.getId().isBlank()
                        ? Component.translatable("viscript_shop.promotion.source.rule")
                        : Component.literal(rule.getId());
                lines.add(Component.translatable("viscript_shop.editor.promotion.inherited.entry",
                        source, Component.translatable(scopedRule.scope().getTranslationKey())));
                if (!rule.getConditions().isEmpty()) {
                    lines.add(Component.translatable("viscript_shop.ui.promotion.gift_conditional"));
                }
                return new HoverTooltips(lines, null, null, null);
            });
            return display;
        });
    }
}

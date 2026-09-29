package com.viscriptshop.event;

import com.viscript_lib.annotation.ViScriptRegisterAccessors;
import com.viscript_lib.event.RegisterAccessorEvent;
import com.viscriptshop.gui.data.*;
import com.viscriptshop.promotion.PromotionRule;
import com.viscriptshop.promotion.condition.PromotionConditionEntry;

public final class ShopRegisterAccessorEvent {
    private ShopRegisterAccessorEvent() {
    }

    @ViScriptRegisterAccessors
    public static void onRegisterAccessor(RegisterAccessorEvent event) {
        event.register(ItemMatchRule.class, ItemMatchRule::new);
        event.register(MerchantFlagGroup.class, MerchantFlagGroup::new);
        event.register(MerchantItemDisplay.class, MerchantItemDisplay::new);
        event.register(MerchantItemInfo.class, MerchantItemInfo::new);
        event.register(MerchantCostItemInfo.class, MerchantCostItemInfo::new);
        event.register(MerchantInfo.class, MerchantInfo::new);
        event.register(CategoryInfo.class, CategoryInfo::new);
        event.register(AggregatedResources.PurchaseEntry.class, AggregatedResources.PurchaseEntry::new);
        event.register(AggregatedResources.ItemEntry.class, AggregatedResources.ItemEntry::new);
        event.register(AggregatedResources.class, AggregatedResources::new);
        event.register(PromotionConditionEntry.class, PromotionConditionEntry::new);
        event.register(PromotionRule.class, PromotionRule::new);
        event.register(ShopInfo.class, ShopInfo::new);
        event.register(ShopDisplayEntry.class, ShopDisplayEntry::new);
        event.register(ShopDisplayBatch.class, ShopDisplayBatch::new);
    }
}

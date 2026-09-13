package com.viscriptshop.promotion.condition;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscript_lib.util.item.ViScriptItemStack;
import com.viscriptshop.gui.components.ConfiguratorFieldHelper;
import com.viscriptshop.gui.data.AggregatedResources;
import com.viscriptshop.gui.data.ItemMatchRule;
import com.viscriptshop.promotion.PromotionContext;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** 检查玩家容器（服务端含末影箱与已联动外部存储；客户端预览优先使用服务端计数快照）；可重复使用的凭证和按购买份数消耗的优惠券共用组件筛选。 */
@Getter
@Setter
@LDLRegister(name = PlayerItemCondition.ID, registry = PromotionCondition.REGISTRY, priority = 500)
public class PlayerItemCondition implements PromotionCondition {
    public static final String ID = "viscript_shop:player_item";

    /** 最近一次服务端推送的全容器物品计数快照，供客户端价格预览使用；由库存快照包整体替换。 */
    private static volatile List<AggregatedResources.ItemEntry> clientSnapshot = List.of();

    /**
     * 用服务端推送的快照替换客户端条件预览数据。
     *
     * @param entries 服务端按全容器统计的物品条目；零计数条目已由服务端剔除
     */
    public static void updateClientSnapshot(@Nullable List<AggregatedResources.ItemEntry> entries) {
        clientSnapshot = entries == null ? List.of() : List.copyOf(entries);
    }

    @Configurable(name = "viscript_shop.data.promotion.condition.required_item", key = "item", collapse = false,
            tips = "viscript_shop.data.promotion.condition.required_item.tip")
    private ViScriptItemStack serializedItem = new ViScriptItemStack();

    @Configurable(name = "viscript_shop.data.promotion.condition.item_match_rule", subConfigurable = true, collapse = false)
    private ItemMatchRule matchRule = new ItemMatchRule();

    @Configurable(name = "viscript_shop.data.promotion.condition.consume_item",
            tips = "viscript_shop.data.promotion.condition.consume_item.tip")
    private boolean consumeOnPurchase;

    public ItemStack getItem() {
        return serializedItem == null || serializedItem.isMissingItem() ? ItemStack.EMPTY : serializedItem.toItemStack();
    }

    public void setItem(ItemStack item) {
        serializedItem = new ViScriptItemStack(item == null ? ItemStack.EMPTY : item.copy());
    }

    public ItemMatchRule getMatchRule() {
        return matchRule == null ? new ItemMatchRule() : matchRule;
    }

    public long requiredCount(int purchaseQuantity) {
        return (long) getItem().getCount() * (consumeOnPurchase ? Math.max(1, purchaseQuantity) : 1);
    }

    public long countItems(Player player) {
        if (player == null || getItem().isEmpty()) return 0;
        if (player instanceof ServerPlayer serverPlayer) {
            // 服务端权威：与商品成本一致，遍历全部已注册容器助手统计
            return getMatchRule().getItemForPlayerCount(serverPlayer, getItem());
        }
        // 客户端价格预览：优先使用服务端推送的全容器计数快照，券在外部容器时也能正确显示折扣
        for (AggregatedResources.ItemEntry entry : clientSnapshot) {
            if (entry.canMerge(getItem(), getMatchRule())) return entry.getCount();
        }
        // 无可用快照时退回随身背包近似统计（服务端结算为准）
        long count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack candidate = player.getInventory().getItem(slot);
            if (getMatchRule().matches(candidate, getItem())) count += candidate.getCount();
        }
        return count;
    }

    @Override
    public boolean test(PromotionContext context) {
        return !getItem().isEmpty() && countItems(context.player()) >= requiredCount(context.purchaseQuantity());
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        ConfiguratorFieldHelper.addField(father, this, "serializedItem").setId("promotion_required_item");
        ConfiguratorFieldHelper.addField(father, this, "matchRule").setId("promotion_item_match_rule");
        ConfiguratorFieldHelper.addField(father, this, "consumeOnPurchase").setId("promotion_consume_item");
    }
}

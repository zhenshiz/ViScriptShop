package com.viscriptshop.promotion;

import com.viscript_lib.util.item.ItemUtil;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.data.AggregatedResources;
import lombok.Getter;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 只读规划优惠券的全容器计数，所有交易检查通过后才一次性扣除。
 * 与商品成本使用同一套 ViScriptLib 容器助手统计（随身背包、末影箱、合成槽与手持及已联动外部存储）。
 * 规则互相匹配对方模板的条件条目归入同一池共享计数，防止同一张券分给多条促销。
 */
public final class ConditionItemPayment {
    private final List<ItemPool> pools;
    @Getter
    private final boolean affordable;
    private boolean consumed;

    private ConditionItemPayment(List<ItemPool> pools, boolean affordable) {
        this.pools = pools;
        this.affordable = affordable;
    }

    public static ConditionItemPayment plan(ServerPlayer player, List<AggregatedResources.ItemEntry> costs) {
        if (costs.isEmpty()) return new ConditionItemPayment(List.of(), true);
        for (var cost : costs) {
            if (cost.isMissingItem() && cost.getCount() > 0) {
                return new ConditionItemPayment(List.of(), false);
            }
        }
        // 传递闭包分池：互相兼容的条目通过并查集并入同一池
        int size = costs.size();
        int[] parent = new int[size];
        for (int i = 0; i < size; i++) parent[i] = i;
        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                if (poolCompatible(costs.get(i), costs.get(j))) {
                    parent[find(parent, i)] = find(parent, j);
                }
            }
        }
        List<ItemPool> pools = new ArrayList<>();
        Map<Integer, ItemPool> byRoot = new HashMap<>();
        for (int i = 0; i < size; i++) {
            ItemPool pool = byRoot.computeIfAbsent(find(parent, i), root -> {
                ItemPool created = new ItemPool();
                pools.add(created);
                return created;
            });
            pool.entries.add(costs.get(i));
        }
        boolean affordable = true;
        for (ItemPool pool : pools) {
            long required = 0;
            long supply = 0;
            for (var entry : pool.entries) {
                required += entry.getCount();
                supply = Math.max(supply, entry.getItemForPlayerCount(player));
            }
            if (supply < required) {
                affordable = false;
                break;
            }
        }
        return new ConditionItemPayment(List.copyOf(pools), affordable);
    }

    /** 只有两条规则的物品集合互相覆盖对方模板时才视为共享同一批物品。 */
    private static boolean poolCompatible(AggregatedResources.ItemEntry a, AggregatedResources.ItemEntry b) {
        return a.getMatchRule().matches(b.getItemStack(), a.getItemStack())
                && b.getMatchRule().matches(a.getItemStack(), b.getItemStack());
    }

    private static int find(int[] parent, int node) {
        while (parent[node] != node) {
            parent[node] = parent[parent[node]];
            node = parent[node];
        }
        return node;
    }

    /** 普通商品成本不能再使用已经预留给促销的那些物品。 */
    public long availableFor(ServerPlayer player, AggregatedResources.ItemEntry cost) {
        if (!affordable) return 0;
        return Math.max(0, cost.getItemForPlayerCount(player) - reservedFor(cost));
    }

    private long reservedFor(AggregatedResources.ItemEntry cost) {
        long count = 0;
        for (ItemPool pool : pools) {
            for (var entry : pool.entries) {
                if (entry.getCount() > 0 && cost.getMatchRule().matches(entry.getItemStack(), cost.getItemStack())) {
                    count = ItemUtil.saturatedAdd(count, entry.getCount());
                }
            }
        }
        return count;
    }

    /** 失败不修改任何容器；成功也只能调用一次。 */
    public boolean consume(ServerPlayer player) {
        if (!affordable || consumed) return false;
        for (ItemPool pool : pools) {
            for (var entry : pool.entries) {
                if (entry.getCount() <= 0) continue;
                var rule = entry.getMatchRule();
                long remainder = ItemUtil.removeItemForPlayer(player, entry.getItemStack(), entry.getCount(),
                        rule.resolvedCompareMode(), rule.resolvedComponents());
                if (remainder > 0) {
                    ViscriptShop.LOGGER.error("Failed to consume condition items for player {}: {} x{} remain {}",
                            player.getGameProfile().getName(), entry.getItemStack(), entry.getCount(), remainder);
                    consumed = true;
                    return false;
                }
            }
        }
        consumed = true;
        return true;
    }

    /** 同一池中的条目规则互相匹配对方模板，因此池内供应量可以共享。 */
    private static final class ItemPool {
        private final List<AggregatedResources.ItemEntry> entries = new ArrayList<>();
    }
}

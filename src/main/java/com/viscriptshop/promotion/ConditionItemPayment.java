package com.viscriptshop.promotion;

import com.viscriptshop.gui.data.AggregatedResources;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 只读规划优惠券的实际栏位，所有交易检查通过后才一次性扣除。
 * 用容量匹配解决宽松/严格组件条件重叠，不能把同一张券分给多条促销。
 */
public final class ConditionItemPayment {
    private final Container inventory;
    private final List<ItemStack> snapshot;
    private final int[] reserved;
    private final boolean affordable;
    private boolean consumed;

    private ConditionItemPayment(Container inventory, List<ItemStack> snapshot, int[] reserved, boolean affordable) {
        this.inventory = inventory;
        this.snapshot = snapshot;
        this.reserved = reserved;
        this.affordable = affordable;
    }

    public static ConditionItemPayment plan(Container inventory, List<AggregatedResources.ItemEntry> costs) {
        int slots = inventory.getContainerSize();
        List<ItemStack> snapshot = new ArrayList<>();
        long available = 0;
        for (int i = 0; i < slots; i++) {
            ItemStack stack = inventory.getItem(i).copy();
            snapshot.add(stack);
            available += stack.getCount();
        }
        long required = 0;
        for (var cost : costs) {
            if (cost.isMissingItem() || cost.getCount() > available - required) {
                return new ConditionItemPayment(inventory, snapshot, new int[slots], false);
            }
            required += cost.getCount();
        }
        int sink = slots + costs.size() + 1;
        List<List<Edge>> graph = new ArrayList<>();
        for (int i = 0; i <= sink; i++) graph.add(new ArrayList<>());
        List<Edge> supplies = new ArrayList<>();
        for (int slot = 0; slot < slots; slot++) {
            supplies.add(connect(graph, 0, slot + 1, snapshot.get(slot).getCount()));
            for (int c = 0; c < costs.size(); c++) {
                var cost = costs.get(c);
                if (cost.getMatchRule().matches(snapshot.get(slot), cost.getItemStack())) {
                    connect(graph, slot + 1, slots + c + 1, snapshot.get(slot).getCount());
                }
            }
        }
        for (int c = 0; c < costs.size(); c++) {
            connect(graph, slots + c + 1, sink, costs.get(c).getCount());
        }
        long flow = 0;
        while (flow < required) {
            int[] previous = new int[sink + 1];
            Arrays.fill(previous, -1);
            Edge[] path = new Edge[sink + 1];
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            previous[0] = 0;
            queue.add(0);
            while (!queue.isEmpty() && previous[sink] == -1) {
                int node = queue.remove();
                for (Edge edge : graph.get(node)) {
                    if (edge.remaining > 0 && previous[edge.to] == -1) {
                        previous[edge.to] = node;
                        path[edge.to] = edge;
                        queue.add(edge.to);
                    }
                }
            }
            if (previous[sink] == -1) break;
            long amount = required - flow;
            for (int n = sink; n != 0; n = previous[n]) amount = Math.min(amount, path[n].remaining);
            for (int n = sink; n != 0; n = previous[n]) {
                Edge edge = path[n];
                edge.remaining -= amount;
                graph.get(edge.to).get(edge.reverse).remaining += amount;
            }
            flow += amount;
        }
        int[] reserved = new int[slots];
        for (int i = 0; i < slots; i++) reserved[i] = snapshot.get(i).getCount() - (int) supplies.get(i).remaining;
        return new ConditionItemPayment(inventory, snapshot, reserved, flow == required);
    }

    public boolean isAffordable() {
        return affordable;
    }

    /** 普通商品成本不能再使用已经预留给促销的那些物品。 */
    public long availableFor(ServerPlayer player, AggregatedResources.ItemEntry cost) {
        return Math.max(0, cost.getItemForPlayerCount(player) - reservedFor(cost));
    }

    public long reservedFor(AggregatedResources.ItemEntry cost) {
        long count = 0;
        for (int i = 0; i < reserved.length; i++) {
            if (reserved[i] > 0 && cost.getMatchRule().matches(snapshot.get(i), cost.getItemStack())) count += reserved[i];
        }
        return count;
    }

    /** 失败不修改任何栏位；成功也只能调用一次。 */
    public boolean consume() {
        if (!affordable || consumed) return false;
        for (int slot = 0; slot < reserved.length; slot++) {
            if (reserved[slot] > 0) {
                ItemStack current = inventory.getItem(slot);
                if (current.getCount() < reserved[slot] || !ItemStack.isSameItemSameComponents(current, snapshot.get(slot))) return false;
            }
        }
        for (int slot = 0; slot < reserved.length; slot++) {
            if (reserved[slot] > 0) inventory.removeItem(slot, reserved[slot]);
        }
        if (Arrays.stream(reserved).anyMatch(count -> count > 0)) inventory.setChanged();
        consumed = true;
        return true;
    }

    private static Edge connect(List<List<Edge>> graph, int from, int to, long capacity) {
        Edge forward = new Edge(to, graph.get(to).size(), capacity);
        graph.get(to).add(new Edge(from, graph.get(from).size(), 0));
        graph.get(from).add(forward);
        return forward;
    }

    private static final class Edge {
        final int to;
        final int reverse;
        long remaining;

        Edge(int to, int reverse, long remaining) {
            this.to = to;
            this.reverse = reverse;
            this.remaining = remaining;
        }
    }
}

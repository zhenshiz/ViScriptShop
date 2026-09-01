package com.viscriptshop.gui.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.viscript_lib.util.item.ViScriptItemStack;
import com.viscriptshop.util.MoneyUtil;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 汇总购物车中所需支付或获得的物品、货币和经验值。
 *
 * <p>物品及其数量直接保存在 {@link ViScriptItemStack} 中，避免原版物品堆 Codec 的
 * 数量上限和缺失注册表项导致网络包解析失败。背包匹配、事件和物品发放等运行时边界
 * 仍通过 {@link ItemStack} 完成。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AggregatedResources implements IPersistedSerializable {
    public static final StreamCodec<ByteBuf, AggregatedResources> STREAM_CODEC;
    public static final Codec<AggregatedResources> CODEC;

    @Persisted(key = "items")
    private List<ViScriptItemStack> serializedItems = new ArrayList<>();
    @Persisted
    private List<ItemEntry> itemEntries = new ArrayList<>();
    @Persisted
    private List<String> commands = new ArrayList<>();
    @Persisted
    private double totalMoney = 0;
    @Persisted
    private int totalXp = 0;
    @Persisted
    private List<PurchaseEntry> purchaseEntries = new ArrayList<>();

    static {
        CODEC = PersistedParser.createCodec(AggregatedResources::new);
        STREAM_CODEC = PersistedParser.createStreamCodec(AggregatedResources::new);
    }

    /**
     * 购买条目，记录具体购买了哪个商品多少数量
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PurchaseEntry implements IPersistedSerializable {
        public static final StreamCodec<ByteBuf, PurchaseEntry> STREAM_CODEC;
        public static final Codec<PurchaseEntry> CODEC;

        @Persisted
        private String categoryId;
        @Persisted
        private String merchantId;
        @Persisted
        private int buyCount;

        static {
            CODEC = PersistedParser.createCodec(PurchaseEntry::new);
            STREAM_CODEC = PersistedParser.createStreamCodec(PurchaseEntry::new);
        }
    }

    /**
     * 物品消耗条目，除了物品和数量外还保存组件比较规则。
     */
    @Data
    @NoArgsConstructor
    public static class ItemEntry implements IPersistedSerializable {
        public static final StreamCodec<ByteBuf, ItemEntry> STREAM_CODEC;
        public static final Codec<ItemEntry> CODEC;

        @Persisted(key = "itemStack")
        private ViScriptItemStack serializedItemStack = new ViScriptItemStack();
        @Persisted
        private ItemMatchRule matchRule = new ItemMatchRule();

        static {
            CODEC = PersistedParser.createCodec(ItemEntry::new);
            STREAM_CODEC = PersistedParser.createStreamCodec(ItemEntry::new);
        }

        /**
         * 创建带组件匹配规则的容错物品条目。
         *
         * @param itemStack 容错物品数据
         * @param matchRule 组件匹配规则
         */
        public ItemEntry(ViScriptItemStack itemStack, ItemMatchRule matchRule) {
            this.serializedItemStack = itemStack == null ? new ViScriptItemStack() : itemStack;
            this.matchRule = matchRule == null ? new ItemMatchRule() : matchRule;
        }

        /**
         * 创建带组件匹配规则的原版物品条目。
         *
         * @param itemStack 原版物品堆
         * @param matchRule 组件匹配规则
         */
        public ItemEntry(ItemStack itemStack, ItemMatchRule matchRule) {
            this(new ViScriptItemStack(itemStack == null ? ItemStack.EMPTY : itemStack), matchRule);
        }

        public boolean canMerge(ItemStack stack, ItemMatchRule rule) {
            return !isMissingItem() && stack != null && hasSameRule(rule)
                    && safeRule().matches(getItemStack(), stack);
        }

        public boolean canMerge(ViScriptItemStack stack, ItemMatchRule rule) {
            return stack != null && !stack.isMissingItem() && canMerge(stack.toItemStack(), rule);
        }

        public boolean hasSameRule(ItemMatchRule rule) {
            ItemMatchRule otherRule = rule == null ? new ItemMatchRule() : rule;
            return safeRule().resolvedCompareMode() == otherRule.resolvedCompareMode()
                    && componentSet(safeRule()).equals(componentSet(otherRule));
        }

        public ItemEntry copyWithCount(int count) {
            return new ItemEntry(getSerializedItemStack().copyWithCount(count), safeRule().copy());
        }

        public int getItemForPlayerCount(ServerPlayer player) {
            if (isMissingItem()) return 0;
            return safeRule().getItemForPlayerCount(player, getItemStack());
        }

        public void removeItemForPlayer(ServerPlayer player) {
            if (isMissingItem()) return;
            safeRule().removeItemForPlayer(player, getItemStack(), getCount());
        }

        /**
         * 获取供背包操作和事件使用的原版物品堆副本。
         *
         * @return 已解析的物品堆或缺失物品占位符
         */
        public ItemStack getItemStack() {
            return getSerializedItemStack().toItemStack();
        }

        /**
         * 使用原版物品堆替换条目数据。
         *
         * @param itemStack 原版物品堆；传入 {@code null} 时使用空物品
         */
        public void setItemStack(ItemStack itemStack) {
            setSerializedItemStack(new ViScriptItemStack(itemStack == null ? ItemStack.EMPTY : itemStack));
        }

        /**
         * 获取参与持久化和网络传输的容错物品数据。
         *
         * @return 非 {@code null} 的容错物品数据
         */
        public ViScriptItemStack getSerializedItemStack() {
            if (serializedItemStack == null) {
                serializedItemStack = new ViScriptItemStack();
            }
            return serializedItemStack;
        }

        /**
         * 替换参与持久化和网络传输的容错物品数据。
         *
         * @param itemStack 容错物品数据；传入 {@code null} 时使用空物品
         */
        public void setSerializedItemStack(ViScriptItemStack itemStack) {
            serializedItemStack = itemStack == null ? new ViScriptItemStack() : itemStack;
        }

        /**
         * 获取直接保存在容错物品堆中的数量。
         *
         * @return 物品数量
         */
        public int getCount() {
            return getItemStack().getCount();
        }

        /**
         * 更新容错物品堆中的数量。
         *
         * @param count 新数量；非正数会清空物品堆
         */
        public void setCount(int count) {
            serializedItemStack = getSerializedItemStack().copyWithCount(count);
        }

        /**
         * 判断条目是否引用了当前未注册的物品。
         *
         * @return 使用缺失物品占位符时返回 {@code true}
         */
        public boolean isMissingItem() {
            return getSerializedItemStack().isMissingItem();
        }

        /**
         * 获取非空的组件匹配规则。
         *
         * @return 组件匹配规则
         */
        public ItemMatchRule getMatchRule() {
            return safeRule();
        }

        private ItemMatchRule safeRule() {
            return matchRule == null ? new ItemMatchRule() : matchRule;
        }

        private static Set<DataComponentType<?>> componentSet(ItemMatchRule rule) {
            return new HashSet<>(rule.resolvedComponents());
        }
    }

    public boolean isEmpty() {
        return getPurchaseEntries().isEmpty();
    }

    /**
     * 创建只包含商品 ID 和购买倍数的最小购买请求。
     *
     * <p>物品成本和收益由服务端根据商店文件重新计算，客户端不再重复发送可被篡改的
     * 物品汇总数据。
     *
     * @return 独立的购买请求
     */
    public AggregatedResources toPurchaseRequest() {
        AggregatedResources request = new AggregatedResources();
        for (PurchaseEntry entry : getPurchaseEntries()) {
            request.getPurchaseEntries().add(new PurchaseEntry(
                    entry.getCategoryId(),
                    entry.getMerchantId(),
                    entry.getBuyCount()
            ));
        }
        return request;
    }

    public long getTotalItemCount() {
        long total = 0L;
        for (ViScriptItemStack item : getSerializedItems()) {
            total = saturatedAdd(total, item.toItemStack().getCount());
        }
        return total;
    }

    /**
     * 获取供渲染、事件或游戏逻辑使用的原版物品堆副本。
     *
     * <p>每个返回物品堆已经包含汇总后的数量，不再通过独立整数传递数量。
     *
     * @return 原版物品堆副本列表
     */
    public List<ItemStack> getItems() {
        return getSerializedItems().stream().map(ViScriptItemStack::toItemStack).toList();
    }

    /**
     * 获取参与持久化和网络传输的容错物品列表。
     *
     * @return 非 {@code null} 的容错物品列表
     */
    public List<ViScriptItemStack> getSerializedItems() {
        if (serializedItems == null) {
            serializedItems = new ArrayList<>();
        }
        return serializedItems;
    }

    /**
     * 获取非空的物品成本条目列表。
     *
     * @return 物品成本条目列表
     */
    public List<ItemEntry> getItemEntries() {
        if (itemEntries == null) {
            itemEntries = new ArrayList<>();
        }
        return itemEntries;
    }

    /**
     * 获取非空的购买条目列表。
     *
     * @return 购买条目列表
     */
    public List<PurchaseEntry> getPurchaseEntries() {
        if (purchaseEntries == null) {
            purchaseEntries = new ArrayList<>();
        }
        return purchaseEntries;
    }

    /**
     * 判断汇总中是否包含当前未注册的物品。
     *
     * @return 任意输出物品或成本条目缺失时返回 {@code true}
     */
    public boolean hasMissingItems() {
        return getSerializedItems().stream().anyMatch(ViScriptItemStack::isMissingItem)
                || getItemEntries().stream().anyMatch(ItemEntry::isMissingItem);
    }

    /**
     * 将一个 ItemStack 合并到汇总中。
     *
     * @param stack 要合并的物品（通常数量为1，但也可以是任意数量）
     * @param count 购买数量 (buyCount)
     */
    public void addItem(ItemStack stack, int count) {
        addItem(new ViScriptItemStack(stack == null ? ItemStack.EMPTY : stack), count);
    }

    /**
     * 将容错物品数据合并到汇总中，并把购买倍数写入物品堆数量。
     *
     * @param stack 容错物品数据
     * @param count 购买倍数
     */
    public void addItem(ViScriptItemStack stack, int count) {
        if (stack == null || stack.toItemStack().isEmpty() || count <= 0) return;

        int totalQuantity = saturatedMultiply(stack.toItemStack().getCount(), count);
        if (!stack.isMissingItem()) {
            ItemStack runtimeStack = stack.toItemStack();
            for (int index = 0; index < getSerializedItems().size(); index++) {
                ViScriptItemStack existing = getSerializedItems().get(index);
                if (!existing.isMissingItem()
                        && ItemStack.isSameItemSameComponents(runtimeStack, existing.toItemStack())) {
                    int mergedCount = saturatedAdd(existing.toItemStack().getCount(), totalQuantity);
                    getSerializedItems().set(index, existing.copyWithCount(mergedCount));
                    return;
                }
            }
        }
        getSerializedItems().add(stack.copyWithCount(totalQuantity));
    }

    public void addItemEntry(ItemStack stack, int count, ItemMatchRule matchRule) {
        addItemEntry(new ViScriptItemStack(stack == null ? ItemStack.EMPTY : stack), count, matchRule);
    }

    public void addItemEntry(ViScriptItemStack stack, int count, ItemMatchRule matchRule) {
        if (stack == null || stack.toItemStack().isEmpty() || count <= 0) return;

        int totalQuantity = saturatedMultiply(stack.toItemStack().getCount(), count);
        ItemMatchRule rule = matchRule == null ? new ItemMatchRule() : matchRule;

        for (ItemEntry entry : getItemEntries()) {
            if (entry.canMerge(stack, rule)) {
                entry.setCount(saturatedAdd(entry.getCount(), totalQuantity));
                addItem(stack, count);
                return;
            }
        }

        getItemEntries().add(new ItemEntry(stack.copyWithCount(totalQuantity), rule.copy()));
        addItem(stack, count);
    }

    /**
     * 合并货币花费。
     *
     * @param money 花费的货币值
     * @param count 购买数量
     */
    public void addMoney(double money, int count) {
        if (MoneyUtil.isPositive(money) && count > 0) {
            this.totalMoney = MoneyUtil.add(this.totalMoney, MoneyUtil.multiply(money, count));
        }
    }

    /**
     * 合并经验值。
     *
     * @param xp    获得的经验值
     * @param count 购买数量
     */
    public void addXp(int xp, int count) {
        if (xp > 0 && count > 0) {
            this.totalXp = saturatedAdd(this.totalXp, saturatedMultiply(xp, count));
        }
    }

    /**
     * 合并指令
     *
     * @param command 指令
     */
    public void addCommand(String command) {
        if (!command.isEmpty()) {
            commands.add(command);
        }
    }

    /**
     * 计算购物车中所有商品的成本（玩家需要支付的）。
     *
     * @param shopInfo 商店信息，包括各个分类里所有的购物车列表
     * @return 购物车中所有商品的成本
     */
    public static AggregatedResources getCostSummary(ShopInfo shopInfo) {
        AggregatedResources cost = new AggregatedResources();
        for (CategoryInfo categoryInfo : shopInfo.getCategoryInfos()) {
            for (MerchantInfo merchant : categoryInfo.getMerchants()) {
                int count = (int) merchant.getBuyCount();
                if (count <= 0) continue;

                // 记录购买条目
                cost.getPurchaseEntries().add(new PurchaseEntry(categoryInfo.getId(), merchant.getId(), count));

                switch (categoryInfo.getShopType()) {
                    case ITEM_FOR_ITEM -> {
                        // 以物换物商店：成本是 itemA 和 itemB
                        cost.addItemEntry(merchant.getSerializedItemA(), count, merchant.getItemAMatchRule());
                        cost.addItemEntry(merchant.getSerializedItemB(), count, merchant.getItemBMatchRule());
                    }
                    case CURRENCY -> {
                        switch (merchant.getTradeType()) {
                            case BUY -> // 购买物品：成本是货币
                                    cost.addMoney(merchant.getMoney(), count);
                            case SELL -> // 出售物品：成本是玩家出售的物品 (itemResult)
                                    cost.addItemEntry(merchant.getSerializedItemResult(), count, null);
                        }
                    }
                }
            }
        }
        return cost;
    }

    /**
     * 计算购物车中所有商品的收益（玩家可以获得的）。
     *
     * @param shopInfo 商店信息，包括各个分类里所有的购物车列表
     * @return 购物车中所有商品的收益
     */
    public static AggregatedResources getGainSummary(ShopInfo shopInfo) {
        AggregatedResources gain = new AggregatedResources();
        for (CategoryInfo categoryInfo : shopInfo.getCategoryInfos()) {
            for (MerchantInfo merchant : categoryInfo.getMerchants()) {
                int count = (int) merchant.getBuyCount();
                if (count <= 0) continue;

                // 记录购买条目（只需要记录一次即可）
                gain.getPurchaseEntries().add(new PurchaseEntry(categoryInfo.getId(), merchant.getId(), count));

                //通用收益
                gain.addXp(merchant.getXp(), count);
                gain.addCommand(merchant.getCommand());
                switch (categoryInfo.getShopType()) {
                    case ITEM_FOR_ITEM -> {
                        // 以物换物商店：收益是 itemResult
                        gain.addItem(merchant.getSerializedItemResult(), count);
                    }
                    case CURRENCY -> {
                        // 通用货币商店：根据 TradeType 决定收益
                        switch (merchant.getTradeType()) {
                            case BUY -> {
                                // 购买物品：收益是 itemResult
                                gain.addItem(merchant.getSerializedItemResult(), count);
                            }
                            case SELL -> {
                                // 出售物品：收益是货币
                                gain.addMoney(merchant.getMoney(), count);
                            }
                        }
                    }
                }
            }
        }
        return gain;
    }

    private static int saturatedMultiply(int left, int right) {
        long result = (long) left * right;
        return result >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }

    private static int saturatedAdd(int left, int right) {
        long result = (long) left + right;
        return result >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }

    private static long saturatedAdd(long left, int right) {
        if (Long.MAX_VALUE - left < right) return Long.MAX_VALUE;
        return left + right;
    }
}

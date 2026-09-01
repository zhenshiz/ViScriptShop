package com.viscriptshop.gui.data;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.ConfiguratorParser;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.viscriptshop.gui.components.StageRestrictionConfigurator;
import com.viscriptshop.util.MoneyUtil;
import com.viscript_lib.util.CodecUtil;
import com.viscript_lib.util.item.ViScriptItemStack;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.nbt.Tag;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MerchantInfo implements IConfigurable, IPersistedSerializable, StageRestricted {
    public static final StreamCodec<ByteBuf, MerchantInfo> STREAM_CODEC;
    public static final Codec<MerchantInfo> CODEC;

    //以物换物商店
    @Configurable(name = "viscript_shop.data.merchant.itemA", key = "itemA", subConfigurable = true)
    private MerchantCostItemInfo itemAInfo = new MerchantCostItemInfo();
    @Configurable(name = "viscript_shop.data.merchant.itemB", key = "itemB", subConfigurable = true)
    private MerchantCostItemInfo itemBInfo = new MerchantCostItemInfo();
    //通用货币商店
    @Configurable(name = "viscript_shop.data.merchant.money")
    @ConfigNumber(range = {0, Double.MAX_VALUE}, wheel = 0.1, type = ConfigNumber.Type.DOUBLE)
    private double money = 0;
    @Configurable(name = "viscript_shop.data.merchant.tradeType")
    private TradeType tradeType = TradeType.BUY;
    //通用参数
    @Configurable(name = "viscript_shop.data.merchant.id")
    private String id = UUID.randomUUID().toString();
    @Configurable(name = "viscript_shop.data.merchant.itemResult", key = "itemResult", subConfigurable = true)
    private MerchantItemInfo itemResultInfo = new MerchantItemInfo();
    @Configurable(name = "viscript_shop.data.merchant.stock", tips = "viscript_shop.data.merchant.stock.tips")
    @ConfigNumber(range = {-1, Integer.MAX_VALUE}, wheel = 1)
    private int stock = -1;
    @Configurable(name = "viscript_shop.data.merchant.xp")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    private int xp = 0;
    @Configurable(name = "viscript_shop.data.merchant.command", tips = "viscript_shop.data.merchant.command.tip")
    private String command = "";
    @Persisted
    private List<String> lockMessages = new ArrayList<>();
    @Persisted
    private MerchantFlagGroup.GroupMatchMode flagGroupMode = MerchantFlagGroup.GroupMatchMode.OR;
    @Persisted
    private List<MerchantFlagGroup> flagGroups = new ArrayList<>();
    // 界面使用的参数
    private Number buyCount = 0;

    static {
        CODEC = PersistedParser.createCodec(MerchantInfo::new);
        STREAM_CODEC = PersistedParser.createStreamCodec(MerchantInfo::new);
    }

    public Configurator createConfigurator(CategoryInfo.ShopType shopType) {
        ConfiguratorGroup group = new ConfiguratorGroup();
        group.setCanCollapse(false);
        group.setCollapse(false);
        group.lineContainer.setDisplay(TaffyDisplay.NONE);
        getItemAInfo();
        getItemBInfo();
        getItemResultInfo();
        // 显式指定字段分组，避免字段顺序变化后把无关配置显示到另一种商店类型中。
        if (shopType == CategoryInfo.ShopType.ITEM_FOR_ITEM) {
            addFieldConfigurator(group, "itemAInfo")
                    .addClass("merchant-cost-item-info");
            addFieldConfigurator(group, "itemBInfo")
                    .addClass("merchant-cost-item-info");
        } else if (shopType == CategoryInfo.ShopType.CURRENCY) {
            addFieldConfigurator(group, "money");
            addFieldConfigurator(group, "tradeType");
        }
        addFieldConfigurator(group, "id");
        addFieldConfigurator(group, "itemResultInfo")
                .addClass("merchant-result-item-info");
        addFieldConfigurator(group, "stock");
        addFieldConfigurator(group, "xp");
        addFieldConfigurator(group, "command");
        group.addConfigurator(new StageRestrictionConfigurator(this));
        return group;
    }

    private Configurator addFieldConfigurator(ConfiguratorGroup group, String fieldName) {
        try {
            int previousSize = group.getConfigurators().size();
            ConfiguratorParser.createFieldConfigurator(
                    getClass().getDeclaredField(fieldName),
                    group,
                    getClass(),
                    new HashMap<>(),
                    this
            );
            if (group.getConfigurators().size() <= previousSize) {
                throw new IllegalStateException("No configurator created for merchant field: " + fieldName);
            }
            return group.getConfigurators().getLast();
        } catch (NoSuchFieldException exception) {
            throw new IllegalStateException("Missing merchant field: " + fieldName, exception);
        }
    }

    @Deprecated
    public List<String> getFlags() {
        if (flagGroups.size() == 1 && flagGroups.getFirst().getMode() == MerchantFlagGroup.MatchMode.AND) {
            return flagGroups.getFirst().getFlags();
        }
        List<String> flags = new ArrayList<>();
        for (MerchantFlagGroup group : flagGroups) {
            flags.addAll(group.normalizedFlags());
        }
        return flags;
    }

    @Deprecated
    public void setFlags(List<String> flags) {
        flagGroups.clear();
        if (flags != null && !flags.isEmpty()) {
            flagGroups.add(new MerchantFlagGroup(MerchantFlagGroup.MatchMode.AND, new ArrayList<>(flags)));
        }
    }

    public MerchantInfo copy() {
        Tag tag = CodecUtil.serializeNBT(MerchantInfo.CODEC, this, Platform.getFrozenRegistry());
        MerchantInfo copy = CodecUtil.deserializeNBT(MerchantInfo.CODEC, tag, Platform.getFrozenRegistry());
        // 生成新的UUID，确保ID唯一性
        copy.setId(UUID.randomUUID().toString());
        return copy;
    }

    /**
     * 设置单次交易使用的货币金额。
     *
     * <p>负数、非数字和无穷值均会被规范化为零。
     *
     * @param money 新的交易金额
     */
    public void setMoney(double money) {
        this.money = MoneyUtil.normalize(money);
    }

    /**
     * 获取交易物品 A 的完整信息。
     *
     * @return 非 {@code null} 的交易物品 A 信息
     */
    public MerchantCostItemInfo getItemAInfo() {
        if (itemAInfo == null) {
            itemAInfo = new MerchantCostItemInfo();
        }
        return itemAInfo;
    }

    /**
     * 获取交易物品 B 的完整信息。
     *
     * @return 非 {@code null} 的交易物品 B 信息
     */
    public MerchantCostItemInfo getItemBInfo() {
        if (itemBInfo == null) {
            itemBInfo = new MerchantCostItemInfo();
        }
        return itemBInfo;
    }

    /**
     * 获取返回物品的完整信息。
     *
     * @return 不包含组件匹配规则的返回物品信息
     */
    public MerchantItemInfo getItemResultInfo() {
        if (itemResultInfo == null) {
            itemResultInfo = new MerchantItemInfo();
        }
        return itemResultInfo;
    }

    /**
     * 获取参与交易的物品 A。
     *
     * @return 物品 A 的实际物品堆
     */
    public ItemStack getItemA() {
        return getItemAInfo().getItem();
    }

    /**
     * 获取物品 A 的容错持久化数据。
     *
     * @return 物品 A 的容错物品数据
     */
    public ViScriptItemStack getSerializedItemA() {
        return getItemAInfo().getSerializedItem();
    }

    /**
     * 设置参与交易的物品 A，但不修改其匹配规则或图标配置。
     *
     * @param itemA 物品 A 的实际物品堆；传入 {@code null} 时使用空物品堆
     */
    public void setItemA(ItemStack itemA) {
        getItemAInfo().setItem(itemA == null ? ItemStack.EMPTY : itemA);
    }

    /**
     * 获取参与交易的物品 B。
     *
     * @return 物品 B 的实际物品堆
     */
    public ItemStack getItemB() {
        return getItemBInfo().getItem();
    }

    /**
     * 获取物品 B 的容错持久化数据。
     *
     * @return 物品 B 的容错物品数据
     */
    public ViScriptItemStack getSerializedItemB() {
        return getItemBInfo().getSerializedItem();
    }

    /**
     * 设置参与交易的物品 B，但不修改其匹配规则或图标配置。
     *
     * @param itemB 物品 B 的实际物品堆；传入 {@code null} 时使用空物品堆
     */
    public void setItemB(ItemStack itemB) {
        getItemBInfo().setItem(itemB == null ? ItemStack.EMPTY : itemB);
    }

    /**
     * 获取返回物品的实际物品堆。
     *
     * @return 返回物品的实际物品堆
     */
    public ItemStack getItemResult() {
        return getItemResultInfo().getItem();
    }

    /**
     * 获取返回物品的容错持久化数据。
     *
     * @return 返回物品的容错物品数据
     */
    public ViScriptItemStack getSerializedItemResult() {
        return getItemResultInfo().getSerializedItem();
    }

    /**
     * 设置返回物品，但不修改其图标配置。
     *
     * @param itemResult 返回物品堆；传入 {@code null} 时使用空物品堆
     */
    public void setItemResult(ItemStack itemResult) {
        getItemResultInfo().setItem(itemResult == null ? ItemStack.EMPTY : itemResult);
    }

    /**
     * 获取物品 A 的组件匹配规则。
     *
     * @return 物品 A 的组件匹配规则
     */
    public ItemMatchRule getItemAMatchRule() {
        return getItemAInfo().getMatchRule();
    }

    /**
     * 设置物品 A 的组件匹配规则。
     *
     * @param matchRule 组件匹配规则；传入 {@code null} 时使用默认规则
     */
    public void setItemAMatchRule(ItemMatchRule matchRule) {
        getItemAInfo().setMatchRule(matchRule == null ? new ItemMatchRule() : matchRule);
    }

    /**
     * 获取物品 B 的组件匹配规则。
     *
     * @return 物品 B 的组件匹配规则
     */
    public ItemMatchRule getItemBMatchRule() {
        return getItemBInfo().getMatchRule();
    }

    /**
     * 设置物品 B 的组件匹配规则。
     *
     * @param matchRule 组件匹配规则；传入 {@code null} 时使用默认规则
     */
    public void setItemBMatchRule(ItemMatchRule matchRule) {
        getItemBInfo().setMatchRule(matchRule == null ? new ItemMatchRule() : matchRule);
    }

    /**
     * 获取物品 A 的图标配置。
     *
     * @return 物品 A 的图标配置
     */
    public MerchantItemDisplay getItemADisplay() {
        return getItemAInfo().getDisplay();
    }

    /**
     * 获取物品 B 的图标配置。
     *
     * @return 物品 B 的图标配置
     */
    public MerchantItemDisplay getItemBDisplay() {
        return getItemBInfo().getDisplay();
    }

    /**
     * 获取返回物品的图标配置。
     *
     * @return 返回物品的图标配置
     */
    public MerchantItemDisplay getItemResultDisplay() {
        return getItemResultInfo().getDisplay();
    }

    @Getter
    @AllArgsConstructor
    public enum TradeType implements StringRepresentable {
        BUY("viscript_shop.data.merchant.tradeType.buy"),
        SELL("viscript_shop.data.merchant.tradeType.sell");

        private final String name;

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}

package com.viscriptshop.gui.data;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigList;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.viscriptshop.gui.components.ConfiguratorFieldHelper;
import com.viscriptshop.gui.components.StageRestrictionConfigurator;
import com.viscriptshop.gui.components.InheritedPromotionSummaryConfigurator;
import com.viscriptshop.gui.components.OptionalSectionConfigurator;
import com.viscriptshop.util.MoneyUtil;
import com.viscript_lib.util.CodecUtil;
import com.viscript_lib.util.item.ViScriptItemStack;
import com.viscriptshop.promotion.PromotionRule;
import com.viscriptshop.promotion.PromotionResolver;
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
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
    @Configurable(name = "viscript_shop.data.merchant.command",
            tips = "viscript_shop.data.merchant.command.tip", collapse = false)
    @ConfigList
    private List<String> commands = new ArrayList<>();
    @Persisted
    private boolean promotionEnabled;
    @Configurable(name = "viscript_shop.data.merchant.inherit_promotions",
            tips = "viscript_shop.data.merchant.inherit_promotions.tip")
    private boolean inheritParentPromotions = true;
    @Configurable(name = "viscript_shop.data.merchant.promotion_aggregation",
            tips = "viscript_shop.data.merchant.promotion_aggregation.tip")
    private PromotionRule.AggregationSetting promotionAggregation = PromotionRule.AggregationSetting.INHERIT;
    @Configurable(name = "viscript_shop.data.merchant.promotion_rules",
            tips = "viscript_shop.data.merchant.promotion_rules.tip", collapse = false)
    @ConfigList(
            configuratorMethod = "createPromotionRuleConfigurator",
            addDefaultMethod = "createDefaultPromotionRule"
    )
    private List<PromotionRule> promotionRules = new ArrayList<>();
    @Persisted
    private boolean stageRestrictionEnabled;
    @Persisted
    private List<String> lockMessages = new ArrayList<>();
    @Persisted
    private MerchantFlagGroup.GroupMatchMode flagGroupMode = MerchantFlagGroup.GroupMatchMode.OR;
    @Persisted
    private List<MerchantFlagGroup> flagGroups = new ArrayList<>();
    // 界面使用的参数
    private Number buyCount = 0;
    private transient CategoryInfo.ShopType configuratorShopType = CategoryInfo.ShopType.ITEM_FOR_ITEM;

    static {
        CODEC = PersistedParser.createCodec(MerchantInfo::new);
        STREAM_CODEC = PersistedParser.createStreamCodec(MerchantInfo::new);
    }

    public Configurator createConfigurator(CategoryInfo.ShopType shopType) {
        return createConfigurator(shopType, List.of());
    }

    /**
     * 创建商品编辑表单，并附带只读的上级规则摘要。
     *
     * @param shopType 商品所属分类的交易类型
     * @param parentRules 商店和分类提供的可继承规则
     * @return 商品编辑表单
     */
    public Configurator createConfigurator(CategoryInfo.ShopType shopType,
                                           List<PromotionResolver.ScopedRule> parentRules) {
        configuratorShopType = shopType == null ? CategoryInfo.ShopType.ITEM_FOR_ITEM : shopType;
        ConfiguratorGroup group = new ConfiguratorGroup();
        group.setCanCollapse(false);
        group.setCollapse(false);
        group.lineContainer.setDisplay(TaffyDisplay.NONE);
        getItemAInfo();
        getItemBInfo();
        getItemResultInfo();
        getPromotionRules();
        if (promotionAggregation == null) {
            promotionAggregation = PromotionRule.AggregationSetting.INHERIT;
        }

        ConfiguratorFieldHelper.addField(group, this, "id").setId("merchant_id");
        ConfiguratorFieldHelper.addField(group, this, "stock").setId("merchant_stock");

        // 交易成本字段按分类类型生成，避免编辑到当前类型不会使用的数据。
        if (configuratorShopType == CategoryInfo.ShopType.ITEM_FOR_ITEM) {
            ConfiguratorFieldHelper.addField(group, this, "itemAInfo")
                    .addClass("merchant-cost-item-info");
            ConfiguratorFieldHelper.addField(group, this, "itemBInfo")
                    .addClass("merchant-cost-item-info");
        } else {
            ConfiguratorFieldHelper.addField(group, this, "money").setId("merchant_money");
            ConfiguratorFieldHelper.addField(group, this, "tradeType").setId("merchant_trade_type");
        }
        ConfiguratorFieldHelper.addField(group, this, "itemResultInfo")
                .addClass("merchant-result-item-info");
        ConfiguratorFieldHelper.addField(group, this, "xp").setId("merchant_xp");
        ConfiguratorFieldHelper.addField(group, this, "commands").setId("merchant_commands");

        OptionalSectionConfigurator promotionSection = new OptionalSectionConfigurator(
                "viscript_shop.editor.section.merchant.promotion",
                this::isPromotionEnabled,
                this::setPromotionEnabled
        ).setToggleId("merchant_promotion_enabled");
        promotionSection.setId("merchant_promotion_section");
        promotionSection.folderIcon.setId("merchant_promotion_expand");
        promotionSection.setTips("viscript_shop.editor.section.merchant.promotion.tip");
        ConfiguratorFieldHelper.addField(promotionSection, this, "inheritParentPromotions")
                .setId("merchant_inherit_promotions");
        ConfiguratorFieldHelper.addField(promotionSection, this, "promotionAggregation")
                .setId("merchant_promotion_aggregation");
        promotionSection.addConfigurator(new InheritedPromotionSummaryConfigurator(parentRules));
        ConfiguratorFieldHelper.addField(promotionSection, this, "promotionRules")
                .setId("merchant_promotion_rules");

        OptionalSectionConfigurator stageSection = new OptionalSectionConfigurator(
                "viscript_shop.editor.section.stage",
                this::isStageRestrictionEnabled,
                this::setStageRestrictionEnabled
        ).setToggleId("merchant_stage_enabled");
        stageSection.setId("merchant_stage_section");
        stageSection.folderIcon.setId("merchant_stage_expand");
        stageSection.setTips("viscript_shop.editor.section.stage.tip");
        ConfiguratorGroup stageContent = new StageRestrictionConfigurator(this).hideTitle();
        stageContent.setId("merchant_stage_content");
        stageSection.addConfigurator(stageContent);

        group.addConfigurators(promotionSection, stageSection);
        return group;
    }

    private Configurator createPromotionRuleConfigurator(Supplier<PromotionRule> getter,
                                                          Consumer<PromotionRule> setter) {
        return PromotionRule.createListEntryConfigurator(getter, setter, configuratorShopType);
    }

    private PromotionRule createDefaultPromotionRule() {
        return new PromotionRule();
    }

    /**
     * 获取该商品拥有的促销规则。
     *
     * @return 非 {@code null} 的商品级促销规则列表
     */
    public List<PromotionRule> getPromotionRules() {
        if (promotionRules == null) {
            promotionRules = new ArrayList<>();
        }
        return promotionRules;
    }

    /**
     * 获取交易成功后依次执行的指令。
     *
     * @return 非 {@code null} 的指令列表，每个元素表示一条完整指令
     */
    public List<String> getCommands() {
        if (commands == null) {
            commands = new ArrayList<>();
        }
        return commands;
    }

    /**
     * 使用上级合并方式解析商品当前采用的具体方式。
     *
     * @param fallback 上级提供的具体合并方式
     * @return 商品最终采用的合并方式
     */
    public PromotionRule.AggregationMode resolvePromotionAggregation(PromotionRule.AggregationMode fallback) {
        if (promotionAggregation == null) {
            promotionAggregation = PromotionRule.AggregationSetting.INHERIT;
        }
        return promotionAggregation.resolve(fallback);
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

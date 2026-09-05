package com.viscriptshop.promotion;

import com.lowdragmc.lowdraglib2.configurator.ConfiguratorParser;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigList;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorSelectorConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.SelectorConfigurator;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.viscript_lib.util.item.ViScriptItemStack;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.promotion.condition.PromotionConditionEntry;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.Getter;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 描述一条可以由商店、分类或具体商品拥有的促销规则。
 *
 * <p>规则不保存商店、分类或商品筛选条件。规则所在的数据层级决定
 * 作用域，玩家和世界条件只决定规则何时生效。
 */
@Data
public class PromotionRule implements IConfigurable, IPersistedSerializable {
    public static final Codec<PromotionRule> CODEC = PersistedParser.createCodec(PromotionRule::new);
    public static final StreamCodec<ByteBuf, PromotionRule> STREAM_CODEC =
            PersistedParser.createStreamCodec(PromotionRule::new);

    @Configurable(name = "viscript_shop.data.promotion.id")
    private String id = "";

    @Configurable(name = "viscript_shop.data.promotion.enabled")
    private boolean enabled = true;

    @Configurable(name = "viscript_shop.data.promotion.type")
    private PromotionType type = PromotionType.DISCOUNT;

    @Configurable(name = "viscript_shop.data.promotion.target")
    private Target target = Target.ALL;

    @Configurable(name = "viscript_shop.data.promotion.direction")
    private Direction direction = Direction.REDUCE;

    @Configurable(name = "viscript_shop.data.promotion.percentage",
            tips = "viscript_shop.data.promotion.percentage.tip")
    @ConfigNumber(range = {0, 10000}, wheel = 1, type = ConfigNumber.Type.DOUBLE)
    private double percentage = 10;

    @Configurable(name = "viscript_shop.data.promotion.buy_threshold")
    @ConfigNumber(range = {1, Integer.MAX_VALUE})
    private int buyThreshold = 3;

    @Configurable(name = "viscript_shop.data.promotion.gift_count")
    @ConfigNumber(range = {1, Integer.MAX_VALUE})
    private int giftCount = 1;

    @Configurable(name = "viscript_shop.data.promotion.gift_item", key = "giftItem",
            tips = "viscript_shop.data.promotion.gift_item.tip")
    private ViScriptItemStack serializedGiftItem = new ViScriptItemStack();

    @Configurable(name = "viscript_shop.data.promotion.conditions",
            tips = "viscript_shop.data.promotion.conditions.tip", collapse = false)
    @ConfigList(
            configuratorMethod = "createConditionConfigurator",
            addDefaultMethod = "createDefaultCondition"
    )
    private List<PromotionConditionEntry> conditions = new ArrayList<>();

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        buildConfigurator(father, null);
    }

    /**
     * 为所属商品类型生成规则表单。
     *
     * <p>货币分类显示虚拟货币支出、收入和出售物品消耗，以物换物分类只显示物品成本槽；
     * 商店级规则允许在两组目标之间选择。规则列表本身由 LDLib2 的集合配置器负责。
     *
     * @param father 承载规则字段的父配置组
     * @param shopType 所属分类的交易类型；传入 {@code null} 时显示全部成本槽选项
     */
    public void buildConfigurator(ConfiguratorGroup father, CategoryInfo.ShopType shopType) {
        getConditions();
        addFieldConfigurator(father, "id");
        addFieldConfigurator(father, "enabled");
        father.addConfigurator(new ConfiguratorSelectorConfigurator<>(
                "viscript_shop.data.promotion.type",
                this::resolvedType,
                this::setType,
                PromotionType.DISCOUNT,
                true,
                Arrays.asList(PromotionType.values()),
                PromotionType::getSerializedName,
                (selected, group) -> buildTypeConfigurator(selected, shopType, group)
        ));
        addFieldConfigurator(father, "conditions").setId("promotion_conditions");
    }

    private void buildTypeConfigurator(PromotionType selectedType,
                                       CategoryInfo.ShopType shopType,
                                       ConfiguratorGroup group) {
        if (selectedType == PromotionType.BUY_GET) {
            addFieldConfigurator(group, "buyThreshold");
            addFieldConfigurator(group, "giftCount");
            addFieldConfigurator(group, "serializedGiftItem");
            return;
        }
        List<Target> targets = shopType == null
                ? List.of(
                        Target.ALL,
                        Target.ITEM_A,
                        Target.ITEM_B,
                        Target.MONEY,
                        Target.MONEY_COST,
                        Target.MONEY_REWARD,
                        Target.SELL_ITEM_COST
                )
                : shopType == CategoryInfo.ShopType.CURRENCY
                ? List.of(Target.ALL, Target.MONEY_COST, Target.MONEY_REWARD, Target.SELL_ITEM_COST)
                : List.of(Target.ALL, Target.ITEM_A, Target.ITEM_B);
        group.addConfigurator(new SelectorConfigurator<>(
                "viscript_shop.data.promotion.target",
                this::resolvedTarget,
                this::setTarget,
                Target.ALL,
                true,
                targets,
                Target::getSerializedName
        ));
        Configurator directionConfigurator = new SelectorConfigurator<>(
                "viscript_shop.data.promotion.direction",
                this::resolvedDirection,
                this::setDirection,
                Direction.REDUCE,
                true,
                Arrays.asList(Direction.values()),
                Direction::getSerializedName
        );
        directionConfigurator.setTips("viscript_shop.data.promotion.direction.tip");
        group.addConfigurator(directionConfigurator);
        addFieldConfigurator(group, "percentage");
    }

    private Configurator createConditionConfigurator(Supplier<PromotionConditionEntry> getter,
                                                      Consumer<PromotionConditionEntry> setter) {
        PromotionConditionEntry entry = getter.get();
        if (entry == null) {
            entry = new PromotionConditionEntry();
            setter.accept(entry);
        }
        PromotionConditionEntry currentEntry = entry;
        ConfiguratorGroup group = new ConfiguratorGroup("", true);
        group.label.bindDataSource(SupplierDataSource.of(() -> Component.translatable(
                PromotionRegistries.conditionName(currentEntry.getTypeId())
        )));
        group.label.setDisplay(true);
        currentEntry.buildConfigurator(group);
        return group;
    }

    private PromotionConditionEntry createDefaultCondition() {
        return new PromotionConditionEntry();
    }

    /**
     * 为促销规则创建可折叠的 LDLib2 列表元素。
     *
     * <p>元素标题跟随规则名称变化。返回的组件只描述单个元素，列表的添加、
     * 删除、选中和排序仍由 LDLib2 集合配置器负责。
     *
     * @param getter 当前规则的获取器；返回 {@code null} 时使用默认规则替换
     * @param setter 替换当前列表元素的设置器
     * @param shopType 所属分类的交易类型；传入 {@code null} 时允许选择物品和虚拟货币目标
     * @return 单条规则的可折叠配置器
     */
    public static Configurator createListEntryConfigurator(Supplier<PromotionRule> getter,
                                                            Consumer<PromotionRule> setter,
                                                            CategoryInfo.ShopType shopType) {
        PromotionRule rule = getter.get();
        if (rule == null) {
            rule = new PromotionRule();
            setter.accept(rule);
        }
        PromotionRule currentRule = rule;
        ConfiguratorGroup group = new ConfiguratorGroup("", true);
        group.label.bindDataSource(SupplierDataSource.of(() -> {
            String name = currentRule.getId();
            return name == null || name.isBlank()
                    ? Component.translatable("viscript_shop.promotion.source.rule")
                    : Component.literal(name);
        }));
        group.label.setDisplay(true);
        currentRule.buildConfigurator(group, shopType);
        return group;
    }

    private Configurator addFieldConfigurator(ConfiguratorGroup father, String fieldName) {
        try {
            int previousSize = father.getConfigurators().size();
            ConfiguratorParser.createFieldConfigurator(
                    getClass().getDeclaredField(fieldName),
                    father,
                    getClass(),
                    new HashMap<>(),
                    this
            );
            if (father.getConfigurators().size() <= previousSize) {
                throw new IllegalStateException("未能为促销字段创建配置组件：" + fieldName);
            }
            return father.getConfigurators().getLast();
        } catch (NoSuchFieldException exception) {
            throw new IllegalStateException("促销字段不存在：" + fieldName, exception);
        }
    }

    /**
     * 判断规则是否作用于给定价格槽。
     *
     * <p>货币商品只有一个金额字段，买入时它是支出、卖出时它是收益，因此货币规则
     * 不要求作者再选择一次成本或收益目标。
     *
     * @param currentTarget 当前计算的价格槽
     * @return 规则适用于当前价格槽时返回 {@code true}
     */
    public boolean matchesTarget(Target currentTarget) {
        Target configuredTarget = resolvedTarget();
        if (currentTarget == Target.MONEY_COST || currentTarget == Target.MONEY_REWARD) {
            return configuredTarget == Target.ALL
                    || configuredTarget == Target.MONEY
                    || configuredTarget == currentTarget;
        }
        return configuredTarget == Target.ALL || configuredTarget == currentTarget;
    }

    /**
     * 获取非空条件列表。
     *
     * @return 当前规则的条件列表
     */
    public List<PromotionConditionEntry> getConditions() {
        if (conditions == null) {
            conditions = new ArrayList<>();
        }
        return conditions;
    }

    /**
     * 获取供运行时发放的赠品。
     *
     * @return 已解析的赠品物品堆
     */
    public ItemStack getGiftItem() {
        if (serializedGiftItem == null) {
            serializedGiftItem = new ViScriptItemStack();
        }
        return serializedGiftItem.toItemStack();
    }

    /**
     * 获取非空的促销类型。
     *
     * @return 促销类型
     */
    public PromotionType resolvedType() {
        return type == null ? PromotionType.DISCOUNT : type;
    }

    /**
     * 获取非空的作用目标。
     *
     * @return 作用目标
     */
    public Target resolvedTarget() {
        return target == null ? Target.ALL : target;
    }

    /**
     * 获取非空的玩家价格方向。
     *
     * <p>{@link Direction#REDUCE} 表示玩家获得优惠：购买支出降低，出售收入提高。
     * {@link Direction#INCREASE} 表示玩家被加价，两种价格槽的数值变化相反。
     *
     * @return 玩家价格方向
     */
    public Direction resolvedDirection() {
        return direction == null ? Direction.REDUCE : direction;
    }

    @Getter
    public enum PromotionType implements StringRepresentable {
        DISCOUNT("viscript_shop.data.promotion.type.discount"),
        BUY_GET("viscript_shop.data.promotion.type.buy_get");

        private final String serializedName;

        PromotionType(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public @NotNull String getSerializedName() {
            return serializedName;
        }
    }

    @Getter
    public enum Target implements StringRepresentable {
        ALL("viscript_shop.data.promotion.target.all"),
        ITEM_A("viscript_shop.data.promotion.target.item_a"),
        ITEM_B("viscript_shop.data.promotion.target.item_b"),
        MONEY("viscript_shop.data.promotion.target.money"),
        MONEY_COST("viscript_shop.data.promotion.target.money_cost"),
        MONEY_REWARD("viscript_shop.data.promotion.target.money_reward"),
        SELL_ITEM_COST("viscript_shop.data.promotion.target.sell_item_cost");

        private final String serializedName;

        Target(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public @NotNull String getSerializedName() {
            return serializedName;
        }
    }

    @Getter
    public enum Direction implements StringRepresentable {
        REDUCE("viscript_shop.data.promotion.direction.reduce"),
        INCREASE("viscript_shop.data.promotion.direction.increase");

        private final String serializedName;

        Direction(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public @NotNull String getSerializedName() {
            return serializedName;
        }
    }

    @Getter
    public enum AggregationMode implements StringRepresentable {
        ADD("viscript_shop.data.promotion.aggregation.add"),
        MAX("viscript_shop.data.promotion.aggregation.max"),
        MIN("viscript_shop.data.promotion.aggregation.min"),
        MULTIPLY("viscript_shop.data.promotion.aggregation.multiply");

        private final String serializedName;

        AggregationMode(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public @NotNull String getSerializedName() {
            return serializedName;
        }
    }

    /**
     * 描述分类或商品如何选择折扣合并方式。
     */
    @Getter
    public enum AggregationSetting implements StringRepresentable {
        INHERIT("viscript_shop.data.promotion.aggregation.inherit", null),
        ADD("viscript_shop.data.promotion.aggregation.add", AggregationMode.ADD),
        MAX("viscript_shop.data.promotion.aggregation.max", AggregationMode.MAX),
        MIN("viscript_shop.data.promotion.aggregation.min", AggregationMode.MIN),
        MULTIPLY("viscript_shop.data.promotion.aggregation.multiply", AggregationMode.MULTIPLY);

        private final String serializedName;
        private final AggregationMode mode;

        AggregationSetting(String serializedName, AggregationMode mode) {
            this.serializedName = serializedName;
            this.mode = mode;
        }

        /**
         * 根据上级的具体合并方式解析当前设置。
         *
         * @param fallback 上级的具体合并方式
         * @return 当前设置为 {@link #INHERIT} 时返回上级方式，否则返回当前方式
         */
        public AggregationMode resolve(AggregationMode fallback) {
            return mode == null
                    ? fallback == null ? AggregationMode.ADD : fallback
                    : mode;
        }

        @Override
        public @NotNull String getSerializedName() {
            return serializedName;
        }
    }
}

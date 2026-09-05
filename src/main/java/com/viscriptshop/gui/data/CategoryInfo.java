package com.viscriptshop.gui.data;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigList;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSelector;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.viscript_lib.gui.configurator.ViScriptItemStackAccessor;
import com.viscript_lib.util.item.ViScriptItemStack;
import com.viscriptshop.gui.components.ConfiguratorFieldHelper;
import com.viscriptshop.gui.components.OptionalSectionConfigurator;
import com.viscriptshop.gui.components.StageRestrictionConfigurator;
import com.viscriptshop.promotion.PromotionRule;
import lombok.*;
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

//分类信息
@Data
@NoArgsConstructor
public class CategoryInfo implements IConfigurable, IPersistedSerializable, StageRestricted {
    public static final StreamCodec<ByteBuf, CategoryInfo> STREAM_CODEC;
    public static final Codec<CategoryInfo> CODEC;

    @Configurable(name = "viscript_shop.data.category.id")
    private String id = UUID.randomUUID().toString();
    @Configurable(name = "viscript_shop.data.category.shopType")
    private ShopType shopType = ShopType.ITEM_FOR_ITEM;
    @Configurable(name = "viscript_shop.data.category.iconType")
    @ConfigSelector(subConfiguratorBuilder = "iconTypeSubConfiguratorBuilder")
    private IconType iconType = IconType.ITEM;
    @Persisted
    private ViScriptItemStack iconItem = new ViScriptItemStack();
    @Persisted
    private String iconTexture = "";
    @Configurable(name = "viscript_shop.data.category.name")
    private String name = "";
    @Persisted
    private boolean promotionEnabled;
    @Configurable(name = "viscript_shop.data.category.inherit_promotions",
            tips = "viscript_shop.data.category.inherit_promotions.tip")
    private boolean inheritParentPromotions = true;
    @Configurable(name = "viscript_shop.data.category.promotion_aggregation",
            tips = "viscript_shop.data.category.promotion_aggregation.tip")
    private PromotionRule.AggregationSetting promotionAggregation = PromotionRule.AggregationSetting.INHERIT;
    @Configurable(name = "viscript_shop.data.category.promotion_rules",
            tips = "viscript_shop.data.category.promotion_rules.tip", collapse = false)
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
    @Persisted
    private List<MerchantInfo> merchants = new ArrayList<>();

    static {
        CODEC = PersistedParser.createCodec(CategoryInfo::new);
        STREAM_CODEC = PersistedParser.createStreamCodec(CategoryInfo::new);
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        getPromotionRules();
        if (promotionAggregation == null) {
            promotionAggregation = PromotionRule.AggregationSetting.INHERIT;
        }

        ConfiguratorFieldHelper.addField(father, this, "id").setId("category_id");
        ConfiguratorFieldHelper.addField(father, this, "name").setId("category_name");
        ConfiguratorFieldHelper.addField(father, this, "shopType").setId("category_shop_type");
        ConfiguratorFieldHelper.addField(father, this, "iconType").setId("category_icon_type");

        OptionalSectionConfigurator promotionSection = new OptionalSectionConfigurator(
                "viscript_shop.editor.section.category.promotion",
                this::isPromotionEnabled,
                this::setPromotionEnabled
        ).setToggleId("category_promotion_enabled");
        promotionSection.setId("category_promotion_section");
        promotionSection.folderIcon.setId("category_promotion_expand");
        promotionSection.setTips("viscript_shop.editor.section.category.promotion.tip");
        ConfiguratorFieldHelper.addField(promotionSection, this, "inheritParentPromotions")
                .setId("category_inherit_promotions");
        ConfiguratorFieldHelper.addField(promotionSection, this, "promotionAggregation")
                .setId("category_promotion_aggregation");
        ConfiguratorFieldHelper.addField(promotionSection, this, "promotionRules")
                .setId("category_promotion_rules");

        OptionalSectionConfigurator stageSection = new OptionalSectionConfigurator(
                "viscript_shop.editor.section.stage",
                this::isStageRestrictionEnabled,
                this::setStageRestrictionEnabled
        ).setToggleId("category_stage_enabled");
        stageSection.setId("category_stage_section");
        stageSection.folderIcon.setId("category_stage_expand");
        stageSection.setTips("viscript_shop.editor.section.stage.tip");
        ConfiguratorGroup stageContent = new StageRestrictionConfigurator(this).hideTitle();
        stageContent.setId("category_stage_content");
        stageSection.addConfigurator(stageContent);

        father.addConfigurators(promotionSection, stageSection);
    }

    private Configurator createPromotionRuleConfigurator(Supplier<PromotionRule> getter,
                                                          Consumer<PromotionRule> setter) {
        return PromotionRule.createListEntryConfigurator(getter, setter, resolvedShopType());
    }

    private PromotionRule createDefaultPromotionRule() {
        return new PromotionRule();
    }

    /**
     * 获取分类级促销规则。
     *
     * @return 非 {@code null} 的分类级规则列表
     */
    public List<PromotionRule> getPromotionRules() {
        if (promotionRules == null) {
            promotionRules = new ArrayList<>();
        }
        return promotionRules;
    }

    /**
     * 使用上级合并方式解析分类当前采用的具体方式。
     *
     * @param fallback 上级提供的具体合并方式
     * @return 分类最终采用的合并方式
     */
    public PromotionRule.AggregationMode resolvePromotionAggregation(PromotionRule.AggregationMode fallback) {
        if (promotionAggregation == null) {
            promotionAggregation = PromotionRule.AggregationSetting.INHERIT;
        }
        return promotionAggregation.resolve(fallback);
    }

    /**
     * 获取非空交易类型。
     *
     * @return 当前交易类型
     */
    public ShopType resolvedShopType() {
        return shopType == null ? ShopType.ITEM_FOR_ITEM : shopType;
    }

    /**
     * 使用原版物品图标创建分类信息。
     *
     * @param id 分类 ID
     * @param shopType 商店类型
     * @param iconType 图标类型
     * @param iconItem 原版物品图标
     * @param iconTexture 资源图片路径
     * @param name 分类名称
     * @param merchants 商品列表
     */
    public CategoryInfo(String id, ShopType shopType, IconType iconType, ItemStack iconItem,
                        String iconTexture, String name, List<MerchantInfo> merchants) {
        this.id = id;
        this.shopType = shopType;
        this.iconType = iconType;
        setIconItem(iconItem);
        this.iconTexture = iconTexture;
        this.name = name;
        this.merchants = merchants;
    }

    @SneakyThrows
    private void iconTypeSubConfiguratorBuilder(IconType value, ConfiguratorGroup group) {
        switch (value) {
            case ITEM -> {
                group.addConfigurator(new ViScriptItemStackAccessor().create(
                        "viscript_shop.data.category.iconItem",
                        this::getSerializedIconItem,
                        this::setSerializedIconItem,
                        true,
                        this.getClass().getDeclaredField("iconItem"),
                        this
                ));
            }
            case TEXTURE -> {
                group.addConfigurator(new StringConfigurator("viscript_shop.data.category.iconTexture", this::getIconTexture, this::setIconTexture, iconTexture, true).setResourceLocation(true));
            }
        }
    }

    /**
     * 获取供界面渲染使用的分类图标副本。
     *
     * @return 已解析的原版物品堆或缺失物品占位符
     */
    public ItemStack getIconItem() {
        return getSerializedIconItem().toItemStack();
    }

    /**
     * 使用原版物品堆替换分类图标。
     *
     * @param iconItem 原版物品堆；传入 {@code null} 时使用空物品
     */
    public void setIconItem(ItemStack iconItem) {
        setSerializedIconItem(new ViScriptItemStack(iconItem == null ? ItemStack.EMPTY : iconItem));
    }

    /**
     * 获取分类图标的容错持久化数据。
     *
     * @return 非 {@code null} 的容错物品数据
     */
    public ViScriptItemStack getSerializedIconItem() {
        if (iconItem == null) {
            iconItem = new ViScriptItemStack();
        }
        return iconItem;
    }

    /**
     * 替换分类图标的容错持久化数据。
     *
     * @param iconItem 容错物品数据；传入 {@code null} 时使用空物品
     */
    public void setSerializedIconItem(ViScriptItemStack iconItem) {
        this.iconItem = iconItem == null ? new ViScriptItemStack() : iconItem;
    }

    @Getter
    @AllArgsConstructor
    public enum IconType implements StringRepresentable {
        ITEM("viscript_shop.data.category.iconType.item"),
        TEXTURE("viscript_shop.data.category.iconType.texture");

        private final String name;

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }

    @Getter
    @AllArgsConstructor
    public enum ShopType implements StringRepresentable {
        ITEM_FOR_ITEM("viscript_shop.data.category.shopType.item_for_item"),
        CURRENCY("viscript_shop.data.category.shopType.currency");

        private final String name;

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}

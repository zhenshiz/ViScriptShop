package com.viscriptshop.gui.data;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigList;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.BooleanConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.viscriptshop.gui.components.ConfiguratorFieldHelper;
import com.viscriptshop.gui.components.OptionalSectionConfigurator;
import com.viscriptshop.promotion.PromotionRule;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

//商店信息
@Data
public class ShopInfo implements IConfigurable, IPersistedSerializable {
    public static final StreamCodec<ByteBuf, ShopInfo> STREAM_CODEC;
    public static final Codec<ShopInfo> CODEC;

    @Configurable(name = "viscript_shop.data.shop.name", tips = "viscript_shop.data.shop.name.tip")
    private String name = "";
    @Persisted
    private boolean isQuickOpening = false;
    @Configurable(name = "viscript_shop.data.shop.lockedMerchantVisibility")
    private LockedMerchantVisibility lockedMerchantVisibility = LockedMerchantVisibility.SHOW_WITH_LOCK;
    @Persisted
    private boolean promotionEnabled;
    @Configurable(name = "viscript_shop.data.shop.promotion_aggregation",
            tips = "viscript_shop.data.shop.promotion_aggregation.tip")
    private PromotionRule.AggregationMode promotionAggregation = PromotionRule.AggregationMode.ADD;
    @Configurable(name = "viscript_shop.data.shop.promotion_rules",
            tips = "viscript_shop.data.shop.promotion_rules.tip", collapse = false)
    @ConfigList(
            configuratorMethod = "createPromotionRuleConfigurator",
            addDefaultMethod = "createDefaultPromotionRule"
    )
    private List<PromotionRule> promotionRules = new ArrayList<>();
    @Persisted
    private List<CategoryInfo> categoryInfos = new ArrayList<>();

    static {
        CODEC = PersistedParser.createCodec(ShopInfo::new);
        STREAM_CODEC = PersistedParser.createStreamCodec(ShopInfo::new);
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        getPromotionRules();
        resolvedPromotionAggregation();

        ConfiguratorFieldHelper.addField(father, this, "name").setId("shop_name");
        BooleanConfigurator isQuickOpeningConfigurator = new BooleanConfigurator("viscript_shop.data.shop.isQuickOpening", this::isQuickOpening, this::setQuickOpening, isQuickOpening, true);
        isQuickOpeningConfigurator.setTips("viscript_shop.data.shop.isQuickOpening.tip");
        isQuickOpeningConfigurator.setId("shop_quick_opening");
        father.addConfigurator(isQuickOpeningConfigurator);
        ConfiguratorFieldHelper.addField(father, this, "lockedMerchantVisibility")
                .setId("shop_locked_merchant_visibility");

        OptionalSectionConfigurator promotionSection = new OptionalSectionConfigurator(
                "viscript_shop.editor.section.shop.promotion",
                this::isPromotionEnabled,
                this::setPromotionEnabled
        ).setToggleId("shop_promotion_enabled");
        promotionSection.setId("shop_promotion_section");
        promotionSection.folderIcon.setId("shop_promotion_expand");
        promotionSection.setTips("viscript_shop.editor.section.shop.promotion.tip");
        ConfiguratorFieldHelper.addField(promotionSection, this, "promotionAggregation")
                .setId("shop_promotion_aggregation");
        ConfiguratorFieldHelper.addField(promotionSection, this, "promotionRules")
                .setId("shop_promotion_rules");

        father.addConfigurator(promotionSection);
    }

    private Configurator createPromotionRuleConfigurator(Supplier<PromotionRule> getter,
                                                          Consumer<PromotionRule> setter) {
        return PromotionRule.createListEntryConfigurator(getter, setter, null);
    }

    private PromotionRule createDefaultPromotionRule() {
        return new PromotionRule();
    }

    /**
     * 获取商店级促销规则。
     *
     * @return 非 {@code null} 的商店级规则列表
     */
    public List<PromotionRule> getPromotionRules() {
        if (promotionRules == null) {
            promotionRules = new ArrayList<>();
        }
        return promotionRules;
    }

    /**
     * 获取商店提供的具体折扣合并方式。
     *
     * @return 非 {@code null} 的折扣合并方式
     */
    public PromotionRule.AggregationMode resolvedPromotionAggregation() {
        if (promotionAggregation == null) {
            promotionAggregation = PromotionRule.AggregationMode.ADD;
        }
        return promotionAggregation;
    }

    @Getter
    @AllArgsConstructor
    public enum LockedMerchantVisibility implements StringRepresentable {
        SHOW_WITH_LOCK("viscript_shop.data.shop.lockedItemVisibility.show_with_lock"),
        HIDDEN("viscript_shop.data.shop.lockedItemVisibility.hidden");

        private final String name;

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}

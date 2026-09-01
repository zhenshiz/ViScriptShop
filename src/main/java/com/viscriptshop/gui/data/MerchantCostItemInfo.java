package com.viscriptshop.gui.data;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.viscript_lib.util.item.ViScriptItemStack;
import io.netty.buffer.ByteBuf;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * 保存玩家为一笔交易提供的实际物品、组件匹配规则和独立图标配置。
 *
 * <p>该类型用于 {@code itemA} 和 {@code itemB}。继承的图标配置不会改变实际物品，
 * 组件匹配规则也只对实际物品的库存查找与扣除生效。
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class MerchantCostItemInfo extends MerchantItemInfo {
    public static final StreamCodec<ByteBuf, MerchantCostItemInfo> STREAM_CODEC;
    public static final Codec<MerchantCostItemInfo> CODEC;

    @Configurable(name = "viscript_shop.data.merchant.item.matchRule", subConfigurable = true)
    private ItemMatchRule matchRule = new ItemMatchRule();

    static {
        CODEC = PersistedParser.createCodec(MerchantCostItemInfo::new);
        STREAM_CODEC = PersistedParser.createStreamCodec(MerchantCostItemInfo::new);
    }

    /**
     * 创建包含完整交易物品规则的商品信息。
     *
     * @param item 参与交易的实际物品堆
     * @param display 只影响客户端图标的展示配置
     * @param matchRule 实际物品的组件匹配规则
     */
    public MerchantCostItemInfo(ItemStack item,
                                MerchantItemDisplay display,
                                ItemMatchRule matchRule) {
        super(new ViScriptItemStack(item == null ? ItemStack.EMPTY : item), display);
        this.matchRule = matchRule;
    }

    /**
     * 使用不经过注册表解析的容错物品数据创建交易成本。
     *
     * @param item 容错物品数据
     * @param display 只影响客户端图标的展示配置
     * @param matchRule 库存操作使用的组件匹配规则
     */
    public MerchantCostItemInfo(ViScriptItemStack item,
                                MerchantItemDisplay display,
                                ItemMatchRule matchRule) {
        super(item == null ? new ViScriptItemStack() : item, display);
        this.matchRule = matchRule;
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        super.buildConfigurator(father);
        getMatchRule();
        addFieldConfigurator(father, MerchantCostItemInfo.class, "matchRule")
                .addClass("merchant-item-match-rule");
    }

    /**
     * 获取实际物品的组件匹配规则。
     *
     * @return 非 {@code null} 的组件匹配规则
     */
    public ItemMatchRule getMatchRule() {
        if (matchRule == null) {
            matchRule = new ItemMatchRule();
        }
        return matchRule;
    }
}

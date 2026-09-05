package com.viscriptshop.promotion.condition;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorSelectorConfigurator;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.viscriptshop.promotion.PromotionContext;
import com.viscriptshop.promotion.PromotionRegistries;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * 保存一个可替换实现的促销条件。
 *
 * <p>{@code typeId} 是 LDLib2 注册 ID，具体实现的数据保存在附加 NBT 中。
 * 这里必须使用附加数据，因为字段的运行时类型由注册表决定，普通静态字段访问器无法
 * 在不知道实现类的情况下创建对象。未知类型的原始数据会被保留，防止缺少扩展模组时丢配置。
 */
@Getter
public class PromotionConditionEntry implements IConfigurable, IPersistedSerializable {
    public static final Codec<PromotionConditionEntry> CODEC =
            PersistedParser.createCodec(PromotionConditionEntry::new);
    public static final StreamCodec<ByteBuf, PromotionConditionEntry> STREAM_CODEC =
            PersistedParser.createStreamCodec(PromotionConditionEntry::new);

    @Persisted
    private String typeId = StageFlagCondition.ID;

    private PromotionCondition condition = new StageFlagCondition();
    private CompoundTag unresolvedData = new CompoundTag();

    public PromotionConditionEntry() {
    }

    /**
     * 使用指定注册 ID 创建条件条目。
     *
     * @param typeId 条件注册 ID
     */
    public PromotionConditionEntry(String typeId) {
        setTypeId(typeId);
    }

    /**
     * 切换条件类型，并创建对应实现的默认实例。
     *
     * @param typeId 新条件注册 ID
     */
    public void setTypeId(String typeId) {
        String normalized = typeId == null || typeId.isBlank() ? StageFlagCondition.ID : typeId;
        if (normalized.equals(this.typeId) && condition != null) {
            return;
        }
        this.typeId = normalized;
        this.condition = PromotionRegistries.createCondition(normalized);
        this.unresolvedData = new CompoundTag();
    }

    /**
     * 使用当前实现判断条件。未知实现或执行异常均视为不满足。
     *
     * @param context 促销上下文
     * @return 条件最终结果
     */
    public boolean test(PromotionContext context) {
        if (condition == null) {
            return false;
        }
        try {
            return condition.test(context);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        List<String> candidates = new ArrayList<>(PromotionRegistries.conditionIds());
        if (!candidates.contains(typeId) && !"viscript_shop:permission_level".equals(typeId)) {
            candidates.add(typeId);
        }

        father.addConfigurator(new ConfiguratorSelectorConfigurator<>(
                "viscript_shop.data.promotion.condition.type",
                this::getTypeId,
                this::setTypeId,
                StageFlagCondition.ID,
                true,
                candidates,
                PromotionRegistries::conditionName,
                (ignored, group) -> {
                    if (condition != null) {
                        condition.buildConfigurator(group);
                    }
                }
        ));
    }

    @Override
    public Tag serializeAdditionalNBT(HolderLookup.Provider provider) {
        if (condition != null) {
            return condition.serializeNBT(provider);
        }
        return unresolvedData.copy();
    }

    @Override
    public void deserializeAdditionalNBT(Tag tag, HolderLookup.Provider provider) {
        unresolvedData = tag instanceof CompoundTag compound ? compound.copy() : new CompoundTag();
        condition = PromotionRegistries.createCondition(typeId);
        if (condition != null) {
            condition.deserializeNBT(provider, unresolvedData);
        }
    }
}

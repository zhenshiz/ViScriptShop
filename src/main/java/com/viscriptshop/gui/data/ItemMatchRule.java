package com.viscriptshop.gui.data;

import com.lowdragmc.lowdraglib2.configurator.ConfiguratorAccessors;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.accessors.IConfiguratorAccessor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSelector;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.viscript_lib.util.item.ItemStackCompareMode;
import com.viscript_lib.util.item.ItemUtil;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemMatchRule implements IConfigurable, IPersistedSerializable {
    public static final StreamCodec<ByteBuf, ItemMatchRule> STREAM_CODEC;
    public static final Codec<ItemMatchRule> CODEC;

    @Configurable(name = "viscript_shop.data.item_match_rule.compareMode")
    @ConfigSelector(subConfiguratorBuilder = "compareModeSubConfiguratorBuilder")
    private ItemStackCompareMode compareMode = ItemStackCompareMode.ALL_COMPONENTS;
    @Persisted
    private List<DataComponentType<?>> components = new ArrayList<>();

    static {
        CODEC = PersistedParser.createCodec(ItemMatchRule::new);
        STREAM_CODEC = PersistedParser.createStreamCodec(ItemMatchRule::new);
    }

    public boolean matches(ItemStack candidate, ItemStack target) {
        return ItemUtil.isSameItem(candidate, target, resolvedCompareMode(), resolvedComponents());
    }

    public long getItemForPlayerCount(ServerPlayer player, ItemStack itemStack) {
        return ItemUtil.getItemForPlayerCount(player, itemStack, resolvedCompareMode(), resolvedComponents());
    }

    public void removeItemForPlayer(ServerPlayer player, ItemStack itemStack, long count) {
        ItemUtil.removeItemForPlayer(player, itemStack, count, resolvedCompareMode(), resolvedComponents());
    }

    public ItemMatchRule copy() {
        return new ItemMatchRule(resolvedCompareMode(), new ArrayList<>(resolvedComponents()));
    }

    public ItemStackCompareMode resolvedCompareMode() {
        return compareMode == null ? ItemStackCompareMode.ALL_COMPONENTS : compareMode;
    }

    public List<DataComponentType<?>> resolvedComponents() {
        return components == null ? List.of() : components;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void compareModeSubConfiguratorBuilder(ItemStackCompareMode value, ConfiguratorGroup group) {
        if (value == ItemStackCompareMode.ALL_COMPONENTS) {
            return;
        }
        try {
            Field field = getClass().getDeclaredField("components");
            Configurator configurator = ((IConfiguratorAccessor) ConfiguratorAccessors.findByType(field.getGenericType()))
                    .create("viscript_shop.data.item_match_rule.components", this::getComponents, valueList -> setComponents((List<DataComponentType<?>>) valueList), true, field, this)
                    .setTips("viscript_shop.data.item_match_rule.components.tips");
            if (configurator instanceof ConfiguratorGroup componentGroup) {
                componentGroup.setCollapse(false);
            }
            group.addConfigurator(configurator);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }
}

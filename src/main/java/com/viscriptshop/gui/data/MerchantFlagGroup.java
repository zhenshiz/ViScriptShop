package com.viscriptshop.gui.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.viscript_lib.stage.IStageFlagGroup;
import com.viscript_lib.stage.StageMatchMode;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MerchantFlagGroup implements IPersistedSerializable, IStageFlagGroup {
    public static final StreamCodec<ByteBuf, MerchantFlagGroup> STREAM_CODEC;
    public static final Codec<MerchantFlagGroup> CODEC;

    @Persisted
    private MatchMode mode = MatchMode.AND;
    @Persisted
    private List<String> flags = new ArrayList<>();

    static {
        CODEC = PersistedParser.createCodec(MerchantFlagGroup::new);
        STREAM_CODEC = PersistedParser.createStreamCodec(MerchantFlagGroup::new);
    }

    @Override
    public StageMatchMode getStageMatchMode() {
        return mode == null ? StageMatchMode.AND : StageMatchMode.valueOf(mode.name());
    }

    @Override
    public void setStageMatchMode(StageMatchMode mode) {
        this.mode = MatchMode.valueOf(mode.name());
    }

    public static boolean canAccess(GroupMatchMode mode, List<MerchantFlagGroup> groups, Collection<String> flags) {
        return IStageFlagGroup.canAccess(mode == null ? StageMatchMode.OR : StageMatchMode.valueOf(mode.name()), groups, flags);
    }

    @Deprecated
    public static boolean canAccess(List<MerchantFlagGroup> groups, Collection<String> flags) {
        return canAccess(GroupMatchMode.OR, groups, flags);
    }

    public static List<Component> getLockTooltips(GroupMatchMode mode, List<MerchantFlagGroup> groups, Collection<String> flags) {
        return IStageFlagGroup.getLockTooltips(mode == null ? StageMatchMode.OR : StageMatchMode.valueOf(mode.name()), groups, flags);
    }

    @Deprecated
    public static List<Component> getLockTooltips(List<MerchantFlagGroup> groups, Collection<String> flags) {
        return getLockTooltips(GroupMatchMode.OR, groups, flags);
    }

    @Getter
    @AllArgsConstructor
    public enum MatchMode implements StringRepresentable {
        AND("viscript_shop.data.flag_group.mode.and"),
        OR("viscript_shop.data.flag_group.mode.or"),
        NOT("viscript_shop.data.flag_group.mode.not"),
        NOR("viscript_shop.data.flag_group.mode.nor");

        private final String name;

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }

    @Getter
    @AllArgsConstructor
    public enum GroupMatchMode implements StringRepresentable {
        AND("viscript_shop.data.flag_group.group_mode.and", "viscript_shop.ui.flag_groups.lock.group_and"),
        OR("viscript_shop.data.flag_group.group_mode.or", "viscript_shop.ui.flag_groups.lock.group_or"),
        NOT("viscript_shop.data.flag_group.group_mode.not", "viscript_shop.ui.flag_groups.lock.group_not"),
        NOR("viscript_shop.data.flag_group.group_mode.nor", "viscript_shop.ui.flag_groups.lock.group_nor");

        private final String name;
        private final String lockKey;

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}

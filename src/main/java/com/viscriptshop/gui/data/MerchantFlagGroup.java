package com.viscriptshop.gui.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.nikdo53.neobackports.io.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MerchantFlagGroup implements IPersistedSerializable {
    public static final StreamCodec<MerchantFlagGroup> STREAM_CODEC;
    public static final Codec<MerchantFlagGroup> CODEC;

    @Persisted
    private MatchMode mode = MatchMode.AND;
    @Persisted
    private List<String> flags = new ArrayList<>();

    static {
        CODEC = PersistedParser.createCodec(MerchantFlagGroup::new);
        STREAM_CODEC = PersistedParser.createStreamCodec(MerchantFlagGroup::new);
    }

    public List<String> normalizedFlags() {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (flags != null) {
            for (String flag : flags) {
                String value = flag == null ? "" : flag.trim();
                if (!value.isEmpty()) {
                    normalized.add(value);
                }
            }
        }
        return new ArrayList<>(normalized);
    }

    public boolean isEmpty() {
        return normalizedFlags().isEmpty();
    }

    public boolean matches(Collection<String> playerFlags) {
        List<String> required = normalizedFlags();
        if (required.isEmpty()) {
            return true;
        }

        MatchMode activeMode = mode == null ? MatchMode.AND : mode;
        return switch (activeMode) {
            case AND -> playerFlags.containsAll(required);
            case OR -> required.stream().anyMatch(playerFlags::contains);
            case NOT -> !playerFlags.containsAll(required);
            case NOR -> required.stream().noneMatch(playerFlags::contains);
        };
    }

    public Component getLockHint(int index, Collection<String> playerFlags) {
        List<String> required = normalizedFlags();
        MatchMode activeMode = mode == null ? MatchMode.AND : mode;
        return switch (activeMode) {
            case AND -> Component.translatable("viscript_shop.ui.flag_groups.lock.and", index, join(missingFlags(required, playerFlags)));
            case OR -> Component.translatable("viscript_shop.ui.flag_groups.lock.or", index, join(required));
            case NOT -> Component.translatable("viscript_shop.ui.flag_groups.lock.not", index, join(required));
            case NOR -> Component.translatable("viscript_shop.ui.flag_groups.lock.nor", index, join(conflictingFlags(required, playerFlags)));
        };
    }

    public static boolean canAccess(GroupMatchMode groupMode, List<MerchantFlagGroup> groups, Collection<String> playerFlags) {
        List<MerchantFlagGroup> activeGroups = activeGroups(groups);
        if (activeGroups.isEmpty()) {
            return true;
        }
        GroupMatchMode activeMode = groupMode == null ? GroupMatchMode.OR : groupMode;
        return switch (activeMode) {
            case AND -> activeGroups.stream().allMatch(group -> group.matches(playerFlags));
            case OR -> activeGroups.stream().anyMatch(group -> group.matches(playerFlags));
            case NOT -> !activeGroups.stream().allMatch(group -> group.matches(playerFlags));
            case NOR -> activeGroups.stream().noneMatch(group -> group.matches(playerFlags));
        };
    }

    @Deprecated
    public static boolean canAccess(List<MerchantFlagGroup> groups, Collection<String> playerFlags) {
        return canAccess(GroupMatchMode.OR, groups, playerFlags);
    }

    public static List<Component> getLockTooltips(GroupMatchMode groupMode, List<MerchantFlagGroup> groups, Collection<String> playerFlags) {
        List<MerchantFlagGroup> activeGroups = activeGroups(groups);
        if (activeGroups.isEmpty() || canAccess(groupMode, activeGroups, playerFlags)) {
            return List.of();
        }

        List<Component> tooltips = new ArrayList<>();
        GroupMatchMode activeMode = groupMode == null ? GroupMatchMode.OR : groupMode;
        tooltips.add(Component.translatable(activeMode.getLockKey()));
        List<Component> details = new ArrayList<>();
        for (int i = 0; i < activeGroups.size(); i++) {
            MerchantFlagGroup group = activeGroups.get(i);
            boolean matched = group.matches(playerFlags);
            if (activeMode == GroupMatchMode.NOT || activeMode == GroupMatchMode.NOR) {
                if (matched) {
                    details.add(Component.translatable("viscript_shop.ui.flag_groups.lock.satisfied", i + 1));
                }
            } else if (!matched) {
                details.add(group.getLockHint(i + 1, playerFlags));
            }
        }

        int maxShown = Math.min(4, details.size());
        for (int i = 0; i < maxShown; i++) {
            tooltips.add(details.get(i));
        }
        if (details.size() > maxShown) {
            tooltips.add(Component.translatable("viscript_shop.ui.flag_groups.lock.more", details.size() - maxShown));
        }
        return tooltips;
    }

    @Deprecated
    public static List<Component> getLockTooltips(List<MerchantFlagGroup> groups, Collection<String> playerFlags) {
        return getLockTooltips(GroupMatchMode.OR, groups, playerFlags);
    }

    private static List<MerchantFlagGroup> activeGroups(List<MerchantFlagGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            return List.of();
        }
        return groups.stream()
                .filter(group -> group != null && !group.isEmpty())
                .toList();
    }

    private static List<String> missingFlags(List<String> required, Collection<String> playerFlags) {
        return required.stream()
                .filter(flag -> !playerFlags.contains(flag))
                .toList();
    }

    private static List<String> conflictingFlags(List<String> required, Collection<String> playerFlags) {
        return required.stream()
                .filter(playerFlags::contains)
                .toList();
    }

    private static String join(List<String> flags) {
        return String.join(", ", flags);
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

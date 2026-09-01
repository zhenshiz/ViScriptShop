package com.viscriptshop.gui.components;

import com.lowdragmc.lowdraglib2.configurator.ui.ArrayConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.SelectorConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.viscriptshop.gui.data.MerchantFlagGroup;
import com.viscriptshop.gui.data.StageRestricted;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * 编辑分类和商品共用的阶段条件及自定义上锁信息。
 */
public class StageRestrictionConfigurator extends ConfiguratorGroup {
    private final StageRestricted restriction;
    private final Map<MerchantFlagGroup, Boolean> collapseStates = new IdentityHashMap<>();
    private final Map<MerchantFlagGroup, ConfiguratorGroup> groupConfigurators = new IdentityHashMap<>();

    /**
     * 创建阶段限制编辑器。
     *
     * @param restriction 需要编辑的分类或商品阶段限制
     */
    public StageRestrictionConfigurator(StageRestricted restriction) {
        super("viscript_shop.data.stage_restriction", false);
        this.restriction = restriction;
        setCanCollapse(true);
        setTips("viscript_shop.data.stage_restriction.tip");
        configuratorContainer.layout(layout -> layout.gapAll(3));
        rebuild();
    }

    private void rebuild() {
        captureGroupCollapseStates();
        groupConfigurators.clear();
        removeAllConfigurators();

        getLockMessages();
        List<MerchantFlagGroup> groups = getGroups();
        if (restriction.getFlagGroupMode() == null) {
            restriction.setFlagGroupMode(MerchantFlagGroup.GroupMatchMode.OR);
        }

        addConfigurator(createLockMessagesConfigurator());
        addConfigurator(new SelectorConfigurator<>(
                "viscript_shop.data.flag_group.group_mode",
                restriction::getFlagGroupMode,
                mode -> {
                    restriction.setFlagGroupMode(mode);
                    notifyChanges();
                },
                MerchantFlagGroup.GroupMatchMode.OR,
                true,
                Arrays.asList(MerchantFlagGroup.GroupMatchMode.values()),
                MerchantFlagGroup.GroupMatchMode::getName
        ).setTips("viscript_shop.data.flag_group.group_mode.tip"));

        if (groups.isEmpty()) {
            Configurator empty = new Configurator("");
            empty.addInlineChild(new Label()
                    .setText("viscript_shop.editor.stage_restriction.empty")
                    .textStyle(style -> style
                            .textWrap(TextWrap.WRAP)
                            .adaptiveHeight(true)
                            .textAlignVertical(Vertical.CENTER))
                    .layout(layout -> layout.widthPercent(100)));
            addConfigurator(empty);
        }

        for (int i = 0; i < groups.size(); i++) {
            addConfigurator(createGroupConfigurator(groups.get(i), i));
        }

        Configurator actions = new Configurator("");
        actions.addInlineChild(new Button()
                .setText("viscript_shop.editor.flag_group.add")
                .setOnClick(event -> {
                    getGroups().add(new MerchantFlagGroup());
                    notifyChanges();
                    rebuild();
                })
                .layout(layout -> layout.widthPercent(100)));
        addConfigurator(actions);
    }

    private Configurator createLockMessagesConfigurator() {
        ArrayConfiguratorGroup<String> messages = new ArrayConfiguratorGroup<>(
                "viscript_shop.data.stage_restriction.lockMessages",
                true,
                () -> new ArrayList<>(getLockMessages()),
                (getter, setter) -> new StringConfigurator("", getter, setter, "", true),
                true
        );
        messages.setTips("viscript_shop.data.stage_restriction.lockMessages.tip");
        messages.setAddDefault(() -> "");
        messages.setOnUpdate(updated -> {
            restriction.setLockMessages(new ArrayList<>(updated));
            notifyChanges();
        });
        return messages;
    }

    private Configurator createGroupConfigurator(MerchantFlagGroup group, int index) {
        if (group.getMode() == null) {
            group.setMode(MerchantFlagGroup.MatchMode.AND);
        }
        if (group.getFlags() == null) {
            group.setFlags(new ArrayList<>());
        }

        ConfiguratorGroup groupConfigurator = new ConfiguratorGroup("", collapseStates.getOrDefault(group, true));
        groupConfigurator.setLabel(Component.translatable("viscript_shop.editor.flag_group.title", index + 1));
        groupConfigurator.setCanCollapse(true);
        groupConfigurator.configuratorContainer(layout -> layout.layout(l -> l.gapAll(3)));
        groupConfigurators.put(group, groupConfigurator);

        groupConfigurator.addConfigurator(new SelectorConfigurator<>(
                "viscript_shop.data.flag_group.mode",
                group::getMode,
                mode -> {
                    group.setMode(mode);
                    notifyChanges();
                },
                MerchantFlagGroup.MatchMode.AND,
                true,
                Arrays.asList(MerchantFlagGroup.MatchMode.values()),
                MerchantFlagGroup.MatchMode::getName
        ).setTips("viscript_shop.data.flag_group.mode.tip"));

        groupConfigurator.addConfigurator(createFlagsConfigurator(group));

        Configurator actions = new Configurator("");
        actions.addInlineChild(new Button()
                .setText("viscript_shop.button.delete")
                .setOnClick(event -> {
                    getGroups().remove(group);
                    collapseStates.remove(group);
                    notifyChanges();
                    rebuild();
                })
                .layout(layout -> layout.widthPercent(100)));
        groupConfigurator.addConfigurator(actions);

        return groupConfigurator;
    }

    private Configurator createFlagsConfigurator(MerchantFlagGroup group) {
        Configurator flagsConfigurator = new Configurator("viscript_shop.data.flag_group.flags");
        flagsConfigurator.setTips("viscript_shop.data.flag_group.flags.tip");

        UIElement content = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(3);
        });

        UIElement flags = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.wrap(FlexWrap.WRAP);
            layout.gapAll(2);
        });

        List<String> normalizedFlags = group.normalizedFlags();
        if (normalizedFlags.isEmpty()) {
            flags.addChild(new Label()
                    .setText("viscript_shop.editor.flag_group.no_flags")
                    .textStyle(style -> style
                            .textWrap(TextWrap.WRAP)
                            .adaptiveHeight(true)
                            .textAlignVertical(Vertical.CENTER))
                    .layout(layout -> layout.widthPercent(100)));
        } else {
            for (String flag : normalizedFlags) {
                flags.addChild(createFlagChip(group, flag));
            }
        }

        Button addFlag = new Button()
                .setText("viscript_shop.editor.flag_group.add_flag")
                .setOnClick(event -> openFlagDialog(group, null));
        addFlag.layout(layout -> layout.widthPercent(100));

        content.addChildren(flags, addFlag);
        flagsConfigurator.addInlineChild(content);
        return flagsConfigurator;
    }

    private UIElement createFlagChip(MerchantFlagGroup group, String flag) {
        UIElement chip = new UIElement().layout(layout -> {
            layout.height(14);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
            layout.gapAll(1);
        });

        Button edit = new Button()
                .setText(Component.literal(flag))
                .setOnClick(event -> openFlagDialog(group, flag))
                .textStyle(style -> style
                        .textAlignHorizontal(Horizontal.CENTER)
                        .textAlignVertical(Vertical.CENTER)
                        .adaptiveWidth(true));
        edit.layout(layout -> layout.height(14));

        Button remove = new Button()
                .noText()
                .setOnClick(event -> {
                    group.getFlags().removeIf(existing -> flag.equals(existing == null ? "" : existing.trim()));
                    notifyChanges();
                    rebuild();
                });
        remove.style(style -> style.backgroundTexture(Icons.CLOSE));
        remove.layout(layout -> {
            layout.width(12);
            layout.height(12);
        });
        remove.buttonStyle(style -> style
                .baseTexture(IGuiTexture.EMPTY)
                .hoverTexture(IGuiTexture.EMPTY)
                .pressedTexture(IGuiTexture.EMPTY));

        chip.addChildren(edit, remove);
        return chip;
    }

    private void openFlagDialog(MerchantFlagGroup group, @Nullable String oldFlag) {
        String initial = oldFlag == null ? "" : oldFlag;
        Dialog.stringEditorDialog(
                oldFlag == null ? "viscript_shop.editor.flag_group.add_flag" : "viscript_shop.editor.flag_group.edit_flag",
                initial,
                value -> !value.trim().isEmpty(),
                value -> {
                    String flag = value.trim();
                    if (oldFlag != null) {
                        group.getFlags().removeIf(existing -> oldFlag.equals(existing == null ? "" : existing.trim()));
                    }
                    if (group.getFlags().stream().noneMatch(existing -> flag.equals(existing == null ? "" : existing.trim()))) {
                        group.getFlags().add(flag);
                    }
                    collapseStates.put(group, false);
                    notifyChanges();
                    rebuild();
                }
        ).show(getModularUI());
    }

    private void captureGroupCollapseStates() {
        groupConfigurators.forEach((group, configurator) -> collapseStates.put(group, configurator.isCollapse()));
    }

    private List<String> getLockMessages() {
        List<String> messages = restriction.getLockMessages();
        if (messages == null) {
            messages = new ArrayList<>();
            restriction.setLockMessages(messages);
        }
        return messages;
    }

    private List<MerchantFlagGroup> getGroups() {
        List<MerchantFlagGroup> groups = restriction.getFlagGroups();
        if (groups == null) {
            groups = new ArrayList<>();
            restriction.setFlagGroups(groups);
        }
        return groups;
    }
}

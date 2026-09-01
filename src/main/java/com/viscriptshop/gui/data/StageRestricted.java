package com.viscriptshop.gui.data;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 定义分类和商品共用的阶段限制数据与访问规则。
 *
 * <p>实现类分别持久化自定义上锁信息翻译键、条件组关系和条件组列表。自定义信息为空时，
 * 锁定提示回退到根据阶段条件自动生成的说明。未注册的翻译键按原始键文本显示，以兼容旧版
 * 保存的自由文本。
 */
public interface StageRestricted {
    /**
     * 获取多个阶段条件组之间的组合关系。
     *
     * @return 条件组关系；未设置时可返回 {@code null}
     */
    MerchantFlagGroup.GroupMatchMode getFlagGroupMode();

    /**
     * 更新多个阶段条件组之间的组合关系。
     *
     * @param flagGroupMode 新的条件组关系
     */
    void setFlagGroupMode(MerchantFlagGroup.GroupMatchMode flagGroupMode);

    /**
     * 获取阶段条件组列表。
     *
     * @return 阶段条件组列表；尚未初始化时可返回 {@code null}
     */
    List<MerchantFlagGroup> getFlagGroups();

    /**
     * 替换阶段条件组列表。
     *
     * @param flagGroups 新的阶段条件组列表
     */
    void setFlagGroups(List<MerchantFlagGroup> flagGroups);

    /**
     * 获取锁定时优先显示的自定义提示翻译键。
     *
     * @return 自定义提示翻译键列表；尚未初始化时可返回 {@code null}
     */
    List<String> getLockMessages();

    /**
     * 替换锁定时优先显示的自定义提示翻译键。
     *
     * @param lockMessages 新的自定义提示翻译键列表
     */
    void setLockMessages(List<String> lockMessages);

    /**
     * 判断给定阶段标记是否满足此对象的访问条件。
     *
     * <p>没有有效条件组时允许访问。传入 {@code null} 与没有阶段标记等效。
     *
     * @param  playerFlags 玩家当前持有的阶段标记
     * @return 满足阶段条件或没有有效条件时返回 {@code true}
     */
    default boolean canAccess(Collection<String> playerFlags) {
        Collection<String> activeFlags = playerFlags == null ? List.of() : playerFlags;
        return MerchantFlagGroup.canAccess(getFlagGroupMode(), getFlagGroups(), activeFlags);
    }

    /**
     * 获取此对象锁定时显示的多行悬浮提示。
     *
     * <p>对象已经解锁时返回空列表。至少配置了一行非空自定义信息时，按列表顺序返回
     * 自定义翻译文本；否则返回根据阶段条件自动生成的默认说明。每个非空字符串均作为
     * Minecraft 翻译键解析，未注册的键按原始键文本显示。
     *
     * @param  playerFlags 玩家当前持有的阶段标记
     * @return 锁定时的提示行，或已经解锁时的空列表
     */
    default List<Component> getLockTooltips(Collection<String> playerFlags) {
        Collection<String> activeFlags = playerFlags == null ? List.of() : playerFlags;
        if (canAccess(activeFlags)) {
            return List.of();
        }

        List<Component> customTooltips = new ArrayList<>();
        List<String> messages = getLockMessages();
        if (messages != null) {
            for (String message : messages) {
                if (message != null && !message.isBlank()) {
                    customTooltips.add(Language.getInstance().has(message)
                            ? Component.translatable(message)
                            : Component.literal(message));
                }
            }
        }
        if (!customTooltips.isEmpty()) {
            return customTooltips;
        }
        return MerchantFlagGroup.getLockTooltips(getFlagGroupMode(), getFlagGroups(), activeFlags);
    }
}

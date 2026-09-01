package com.viscriptshop.compat.ftbquests;

import com.viscriptshop.ViscriptShop;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import dev.ftb.mods.ftbquests.quest.reward.RewardTypes;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import dev.ftb.mods.ftbquests.quest.task.TaskTypes;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;

/**
 * 注册 VSS 提供给 FTB Quests 的任务目标和奖励类型。
 */
public final class FtbQuestsComPat {
    static final Icon VIRTUAL_CURRENCY_ICON = new ImageIcon(
            ViscriptShop.id("textures/icons/coin.png")
    );

    /** FTB Quests 中用于提交 VSS 虚拟货币的任务目标类型。 */
    public static final TaskType VIRTUAL_CURRENCY_TASK = TaskTypes.register(
            ViscriptShop.id("virtual_currency"),
            VirtualCurrencyTask::new,
            () -> VIRTUAL_CURRENCY_ICON
    ).setDisplayName(Component.translatable("viscript_shop.ftbquests.virtual_currency"));

    /** FTB Quests 中用于发放 VSS 虚拟货币的奖励类型。 */
    public static final RewardType VIRTUAL_CURRENCY_REWARD = RewardTypes.register(
            ViscriptShop.id("virtual_currency"),
            VirtualCurrencyReward::new,
            () -> VIRTUAL_CURRENCY_ICON
    ).setDisplayName(Component.translatable("viscript_shop.ftbquests.virtual_currency"));

    private FtbQuestsComPat() {
    }

    /**
     * 完成当前运行端所需的 FTB Quests 联动初始化。
     *
     * @param dist 当前物理运行端
     */
    public static void init(Dist dist) {
        if (dist == Dist.CLIENT) {
            FtbQuestsClientComPat.init();
        }
    }
}

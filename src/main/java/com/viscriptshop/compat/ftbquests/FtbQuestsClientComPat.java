package com.viscriptshop.compat.ftbquests;

import dev.ftb.mods.ftbquests.client.GuiProviders;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 为自定义 FTB Quests 类型安装客户端创建界面。
 */
@OnlyIn(Dist.CLIENT)
final class FtbQuestsClientComPat {
    private FtbQuestsClientComPat() {
    }

    static void init() {
        FtbQuestsComPat.VIRTUAL_CURRENCY_TASK.setGuiProvider(
                GuiProviders.defaultTaskGuiProvider(VirtualCurrencyTask::new)
        );
        FtbQuestsComPat.VIRTUAL_CURRENCY_REWARD.setGuiProvider(
                GuiProviders.defaultRewardGuiProvider(VirtualCurrencyReward::new)
        );
    }
}

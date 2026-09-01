package com.viscriptshop.compat.ftbquests;

import com.viscriptshop.util.MoneyUtil;
import com.viscriptshop.util.ViScriptShopServerUtil;
import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftbquests.net.NotifyRewardMessage;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * 向领取奖励的玩家发放 VSS 当前使用的虚拟货币。
 */
final class VirtualCurrencyReward extends Reward {
    private static final String AMOUNT_TAG = "amount";
    private double amount = 100;

    VirtualCurrencyReward(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public RewardType getType() {
        return FtbQuestsComPat.VIRTUAL_CURRENCY_REWARD;
    }

    @Override
    public void claim(ServerPlayer player, boolean notify) {
        double granted = MoneyUtil.normalize(amount);
        if (granted <= 0) {
            return;
        }

        ViScriptShopServerUtil.addMoney(player, granted);
        if (notify) {
            NetworkManager.sendToPlayer(player, new NotifyRewardMessage(
                    id,
                    Component.translatable("viscript_shop.ftbquests.reward.received", MoneyUtil.format(granted)),
                    FtbQuestsComPat.VIRTUAL_CURRENCY_ICON,
                    disableRewardScreenBlur
            ));
        }
    }

    @Override
    public void writeData(CompoundTag tag, HolderLookup.Provider provider) {
        super.writeData(tag, provider);
        tag.putDouble(AMOUNT_TAG, amount);
    }

    @Override
    public void readData(CompoundTag tag, HolderLookup.Provider provider) {
        super.readData(tag, provider);
        amount = tag.contains(AMOUNT_TAG) ? clampAmount(tag.getDouble(AMOUNT_TAG)) : 100;
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeDouble(amount);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        amount = clampAmount(buffer.readDouble());
    }

    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);
        config.addDouble(AMOUNT_TAG, amount, value -> amount = clampAmount(value), 100, 0, Double.MAX_VALUE)
                .setNameKey("viscript_shop.ftbquests.amount");
    }

    @Override
    public MutableComponent getAltTitle() {
        return Component.translatable("viscript_shop.ftbquests.reward.title", MoneyUtil.format(amount));
    }

    @Override
    public String getButtonText() {
        return MoneyUtil.format(amount) + "◎";
    }

    private static double clampAmount(double value) {
        return MoneyUtil.normalize(value);
    }
}

package com.viscriptshop.compat.ftbquests;

import com.viscriptshop.util.ViScriptShopServerUtil;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.ISingleLongValueTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 允许玩家向任务提交 VSS 虚拟货币，并将实际扣除数量记入团队任务进度。
 */
final class VirtualCurrencyTask extends Task implements ISingleLongValueTask {
    private static final String AMOUNT_TAG = "amount";
    private long amount = 100L;

    VirtualCurrencyTask(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return FtbQuestsComPat.VIRTUAL_CURRENCY_TASK;
    }

    @Override
    public long getMaxProgress() {
        return amount;
    }

    @Override
    public String formatMaxProgress() {
        return Long.toString(amount);
    }

    @Override
    public String formatProgress(TeamData teamData, long progress) {
        return Long.toString(progress);
    }

    @Override
    public void setValue(long value) {
        amount = clampAmount(value);
    }

    @Override
    public long getDefaultConfigValue() {
        return 100L;
    }

    @Override
    public long getMinConfigValue() {
        return 1L;
    }

    @Override
    public long getMaxConfigValue() {
        return Long.MAX_VALUE;
    }

    @Override
    public boolean consumesResources() {
        return true;
    }

    @Override
    public void submitTask(TeamData teamData, ServerPlayer player, ItemStack craftedItem) {
        if (!checkTaskSequence(teamData)) {
            return;
        }

        long remaining = amount - teamData.getProgress(this);
        if (remaining <= 0L) {
            return;
        }

        double balance = ViScriptShopServerUtil.getMoney(player);
        long requested = Math.min(remaining, (long) Math.floor(balance));
        if (requested <= 0L) {
            return;
        }

        long submitted = (long) Math.floor(ViScriptShopServerUtil.removeMoney(player, requested));
        if (submitted > 0L) {
            teamData.addProgress(this, submitted);
        }
    }

    @Override
    public void writeData(CompoundTag tag, HolderLookup.Provider provider) {
        super.writeData(tag, provider);
        tag.putLong(AMOUNT_TAG, amount);
    }

    @Override
    public void readData(CompoundTag tag, HolderLookup.Provider provider) {
        super.readData(tag, provider);
        amount = tag.contains(AMOUNT_TAG) ? clampAmount(tag.getLong(AMOUNT_TAG)) : 100L;
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeVarLong(amount);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        amount = clampAmount(buffer.readVarLong());
    }

    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);
        config.addLong(AMOUNT_TAG, amount, this::setValue, 100L, 1L, Long.MAX_VALUE)
                .setNameKey("viscript_shop.ftbquests.amount");
    }

    @Override
    public MutableComponent getAltTitle() {
        return Component.translatable("viscript_shop.ftbquests.task.title", formatMaxProgress());
    }

    private static long clampAmount(long value) {
        return Math.max(1L, value);
    }
}

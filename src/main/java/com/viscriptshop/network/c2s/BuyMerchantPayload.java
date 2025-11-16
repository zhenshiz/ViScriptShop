package com.viscriptshop.network.c2s;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.network.s2c.ReloadShopUIPayload;
import com.viscriptshop.util.ViScriptShopUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public record BuyMerchantPayload(MerchantInfo merchantInfo) implements CustomPacketPayload {
    public static final Type<BuyMerchantPayload> TYPE = new Type<>(ViscriptShop.id("buy_merchant"));
    public static final StreamCodec<FriendlyByteBuf, BuyMerchantPayload> CODEC = StreamCodec.composite(
            MerchantInfo.STREAM_CODEC,
            BuyMerchantPayload::merchantInfo,
            BuyMerchantPayload::new
    );


    public static void execute(BuyMerchantPayload payload, IPayloadContext context) {
        MerchantInfo merchantInfo = payload.merchantInfo();
        ServerPlayer player = (ServerPlayer) context.player();
        if (merchantInfo != null) {
            ItemStack itemA = merchantInfo.getItemA();
            ItemStack itemB = merchantInfo.getItemB();
            ViScriptShopUtil.removeItem(player, itemA, itemA.getCount());
            ViScriptShopUtil.removeItem(player, itemB, itemB.getCount());
            ItemHandlerHelper.giveItemToPlayer(player, merchantInfo.getItemResult());
            if (merchantInfo.getXp() != 0) player.giveExperiencePoints(merchantInfo.getXp());
            if (!merchantInfo.getCommand().isEmpty())
                Arrays.stream(merchantInfo.getCommand().split(";")).forEach(command -> runCommand(player, command));
            player.connection.send(new ReloadShopUIPayload());
        }
    }

    private static void runCommand(ServerPlayer player, String command) {
        CommandSourceStack commandSource = player.createCommandSourceStack();
        commandSource = commandSource.withPermission(Commands.LEVEL_GAMEMASTERS).withSuppressedOutput();
        var dispatcher = player.server.getCommands().getDispatcher();
        try {
            dispatcher.execute(dispatcher.parse(command, commandSource));
        } catch (CommandSyntaxException e) {
            ViscriptShop.LOGGER.error("Error executing command on server: {}", command, e);
        }
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

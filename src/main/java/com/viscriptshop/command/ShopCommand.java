package com.viscriptshop.command;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.util.ViScriptShopUtil;
import lombok.SneakyThrows;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

@LDLRegisterClient(name = "npc", registry = "viscript_shop:command")
public class ShopCommand implements ICommand {
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection commandSelection) {
        dispatcher.register(Commands.literal(ViscriptShop.MOD_ID).requires(commandSourceStack -> commandSourceStack.hasPermission(2))
                .then(Commands.literal("editor")
                        .executes(this::openEditor)
                )
        );
    }

    @SneakyThrows
    private int openEditor(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            ViScriptShopUtil.serverOpenShopEditor(player);
            return 1;
        } else {
            throw playerOnlyException();
        }
    }
}

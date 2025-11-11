package com.viscriptshop.command;

import com.lowdragmc.lowdraglib2.registry.ILDLRegisterClient;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public interface ICommand extends ILDLRegisterClient<ICommand, Supplier<ICommand>> {

    void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection commandSelection);

    default CommandSyntaxException playerOnlyException() {
        return new CommandSyntaxException(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument(), Component.translatable("command.target.entity.only"));
    }

    default CommandSyntaxException entityOnlyException() {
        return new CommandSyntaxException(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument(), Component.translatable("command.target.player.only"));
    }
}

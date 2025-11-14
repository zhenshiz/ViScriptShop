package com.viscriptshop.command;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.util.ViScriptShopUtil;
import lombok.SneakyThrows;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@LDLRegisterClient(name = "npc", registry = "viscript_shop:command")
public class ShopCommand implements ICommand {
    public static Set<String> shopFilesPath = new HashSet<>();

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection commandSelection) {
        dispatcher.register(Commands.literal(ViscriptShop.MOD_ID).requires(commandSourceStack -> commandSourceStack.hasPermission(Commands.LEVEL_OWNERS))
                .then(Commands.literal("editor")
                        .executes(this::openEditor)
                )
                .then(Commands.literal("open")
                        .then(Commands.argument("shop", StringArgumentType.string())
                                .suggests(this::shopFileSuggestions)
                                .executes(this::openShop)
                        )
                )
                .then(Commands.literal("reload")
                        .then(Commands.argument("shop", StringArgumentType.string())
                                .suggests(this::shopFileSuggestions)
                                .executes(this::reloadShop)
                        )
                )
                .then(Commands.literal("setStage")
                        .then(Commands.argument("shop", StringArgumentType.string())
                                .suggests(this::shopFileSuggestions)
                                .then(Commands.argument("stage", IntegerArgumentType.integer())
                                        .executes(this::setStageShop)
                                )
                        )
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

    @SneakyThrows
    private int openShop(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            String shop = StringArgumentType.getString(context, "shop");
            ViScriptShopUtil.serverOpenShop(player, shop);
            return 1;
        } else {
            throw playerOnlyException();
        }
    }

    private int reloadShop(CommandContext<CommandSourceStack> context) {
        String shop = StringArgumentType.getString(context, "shop");
        ViScriptShopUtil.reloadOpenShop(shop);
        return 1;
    }

    @SneakyThrows
    private int setStageShop(CommandContext<CommandSourceStack> context) {
        String shop = StringArgumentType.getString(context, "shop");
        int stage = IntegerArgumentType.getInteger(context, "stage");
        ViScriptShopUtil.setStageShop(shop, stage);
        return 1;
    }

    private CompletableFuture<Suggestions> shopFileSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        shopFilesPath.forEach(builder::suggest);
        return builder.buildFuture();
    }
}

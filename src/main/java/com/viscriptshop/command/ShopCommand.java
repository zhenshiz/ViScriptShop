package com.viscriptshop.command;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.util.ShopHelper;
import com.viscriptshop.util.ViScriptShopUtil;
import lombok.SneakyThrows;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@LDLRegisterClient(name = "npc", registry = "viscript_shop:command")
public class ShopCommand implements ICommand {
    public static final Set<ResourceLocation> shopFilesPath = new HashSet<>();

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection commandSelection) {
        dispatcher.register(Commands.literal(ViscriptShop.MOD_ID).requires(commandSourceStack -> commandSourceStack.hasPermission(Commands.LEVEL_OWNERS))
                .then(Commands.literal("editor")
                        .executes(this::openEditor)
                )
                .then(Commands.literal("open")
                        .then(Commands.argument("shop", ResourceLocationArgument.id())
                                .suggests(this::shopFileSuggestions)
                                .executes(context -> openShop(context, Component.translatable("viscript_shop.ui.title")))
                                .then(Commands.argument("title", ComponentArgument.textComponent(buildContext))
                                        .executes(context -> openShop(context, ComponentArgument.getComponent(context, "title")))
                                )
                        )
                )
                .then(Commands.literal("reload")
                        .executes(this::reload)
                        .then(Commands.argument("shop", ResourceLocationArgument.id())
                                .suggests(this::shopFileSuggestions)
                                .executes(this::reloadShop)
                        )
                )
                .then(Commands.literal("setStage")
                        .then(Commands.argument("shop", ResourceLocationArgument.id())
                                .suggests(this::shopFileSuggestions)
                                .then(Commands.argument("stage", IntegerArgumentType.integer())
                                        .executes(this::setStageShop)
                                )
                        )
                )
        );
    }

    private int reload(CommandContext<CommandSourceStack> context) {
        shopFilesPath.clear();
        for (String path : ShopHelper.scanShopFiles()) {
            shopFilesPath.add(ViscriptShop.id(path));
        }
        context.getSource().sendSuccess(() -> Component.translatable("command.viscript_shop.reload"), true);
        return 1;
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
    private int openShop(CommandContext<CommandSourceStack> context, Component title) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            ResourceLocation shop = ResourceLocationArgument.getId(context, "shop");
            ViScriptShopUtil.serverOpenShop(player, shop, title);
            return 1;
        } else {
            throw playerOnlyException();
        }
    }

    @SneakyThrows
    private int reloadShop(CommandContext<CommandSourceStack> context) {
        ResourceLocation shop = ResourceLocationArgument.getId(context, "shop");
        ViScriptShopUtil.reloadOpenShop(shop);
        context.getSource().sendSuccess(() -> Component.translatable("command.viscript_shop.reload.shop"), true);
        return 1;
    }

    @SneakyThrows
    private int setStageShop(CommandContext<CommandSourceStack> context) {
        ResourceLocation shop = ResourceLocationArgument.getId(context, "shop");
        int stage = IntegerArgumentType.getInteger(context, "stage");
        ViScriptShopUtil.setStageShop(shop, stage);
        return 1;
    }

    private CompletableFuture<Suggestions> shopFileSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        SharedSuggestionProvider.suggestResource(shopFilesPath, builder);
        return builder.buildFuture();
    }
}

package com.viscriptshop.command;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.viscript_lib.register.ICommand;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.data.*;
import com.viscriptshop.util.ShopHelper;
import com.viscriptshop.util.MoneyUtil;
import com.viscriptshop.util.ViScriptShopServerUtil;
import lombok.SneakyThrows;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@LDLRegister(name = "shop", registry = ICommand.COMMAND_ID)
public class ShopCommand implements ICommand {
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection commandSelection) {
        var root = Commands.literal(ViscriptShop.MOD_ID).requires(commandSourceStack -> commandSourceStack.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("editor")
                        .requires(commandSourceStack -> commandSourceStack.hasPermission(Commands.LEVEL_OWNERS))
                        .executes(context -> openEditor(context, ""))
                        .then(Commands.argument("shop", StringArgumentType.greedyString())
                                .suggests(ShopCommand::suggestServerShopFiles)
                                .executes(this::openEditorTarget)
                        )
                )
                .then(Commands.literal("open")
                        .executes(this::openShopSelector)
                        .then(Commands.argument("target", StringArgumentType.greedyString())
                                .suggests(ShopCommand::suggestOpenTarget)
                                .executes(this::openShopTarget)
                        )
                )
                .then(Commands.literal("setQuickOpening")
                        .then(Commands.argument("target", StringArgumentType.greedyString())
                                .suggests(ShopCommand::suggestQuickOpeningTarget)
                                .executes(this::setQuickOpeningTarget)
                        )
                )
                .then(Commands.literal("reload")
                        .executes(this::reload)
                        .then(Commands.argument("shop", StringArgumentType.greedyString())
                                .suggests(ShopCommand::suggestSavedShops)
                                .executes(this::reloadShop)
                        )
                )
                .then(Commands.literal("stage")
                        .then(Commands.literal("add")
                                .then(Commands.argument("flag", StringArgumentType.string())
                                        .executes(this::addStageFlag)
                                )
                        )
                        .then(Commands.literal("remove")
                                .then(Commands.argument("flag", StringArgumentType.string())
                                        .suggests(ShopCommand::suggestStageFlags)
                                        .executes(this::removeStageFlag)
                                )
                        )
                )
                .then(Commands.literal("setStock")
                        .then(Commands.argument("target", StringArgumentType.greedyString())
                                .suggests(ShopCommand::suggestSetStockTarget)
                                .executes(this::setMerchantStockTarget)
                        )
                )
                .then(Commands.literal("remove")
                        .then(Commands.argument("target", StringArgumentType.greedyString())
                                .suggests(ShopCommand::suggestRemoveTarget)
                                .executes(this::removeMerchantTarget)
                        )
                )
                .then(Commands.literal("money")
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("money", DoubleArgumentType.doubleArg(0, Double.MAX_VALUE))
                                                .executes(ctx -> {
                                                    ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                                    double money = DoubleArgumentType.getDouble(ctx, "money");
                                                    ViScriptShopServerUtil.addMoney(player, money);
                                                    ctx.getSource().sendSuccess(() -> Component.translatable("command.viscript_shop.money.add", player.getDisplayName(), MoneyUtil.format(money), MoneyUtil.format(ViScriptShopServerUtil.getMoney(player))), true);
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                        )
                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("money", DoubleArgumentType.doubleArg(0, Double.MAX_VALUE))
                                                .executes(ctx -> {
                                                    ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                                    double money = DoubleArgumentType.getDouble(ctx, "money");
                                                    double removeMoney = ViScriptShopServerUtil.removeMoney(player, money);
                                                    ctx.getSource().sendSuccess(() -> Component.translatable("command.viscript_shop.money.remove", player.getDisplayName(), MoneyUtil.format(removeMoney), MoneyUtil.format(ViScriptShopServerUtil.getMoney(player))), true);
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                        )
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                            double money = ViScriptShopServerUtil.getMoney(player);
                                            ctx.getSource().sendSuccess(() -> Component.translatable("command.viscript_shop.money.get", player.getDisplayName(), MoneyUtil.format(money)), true);
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
                        .then(Commands.literal("pay")
                                .then(Commands.argument("player1", EntityArgument.player())
                                        .then(Commands.argument("player2", EntityArgument.player())
                                                .then(Commands.argument("money", DoubleArgumentType.doubleArg(0, Double.MAX_VALUE))
                                                        .executes(ctx -> {
                                                            ServerPlayer player1 = EntityArgument.getPlayer(ctx, "player1");
                                                            ServerPlayer player2 = EntityArgument.getPlayer(ctx, "player2");
                                                            double money = DoubleArgumentType.getDouble(ctx, "money");
                                                            double removeMoney = ViScriptShopServerUtil.removeMoney(player1, money);
                                                            ViScriptShopServerUtil.addMoney(player2, removeMoney);
                                                            ctx.getSource().sendSuccess(() -> Component.translatable("command.viscript_shop.money.pay", player1.getDisplayName(), MoneyUtil.format(removeMoney), player2.getDisplayName()), true);
                                                            return Command.SINGLE_SUCCESS;
                                                        })
                                                )
                                        )
                                )
                        )
                );

        dispatcher.register(root);
    }

    private int reload(CommandContext<CommandSourceStack> context) {
        ShopSavedData shopSavedData = ViscriptShop.getShopSavedData();
        shopSavedData.reset();
        context.getSource().sendSuccess(() -> Component.translatable("command.viscript_shop.reload"), true);
        return 1;
    }

    @SneakyThrows
    private int openEditorTarget(CommandContext<CommandSourceStack> context) {
        List<String> args = parseGreedyArguments(context, "shop");
        if (args.size() != 1) {
            return sendInvalidUsage(context, "viscript_shop editor [shop]");
        }
        return openEditor(context, args.getFirst());
    }

    @SneakyThrows
    private int openEditor(CommandContext<CommandSourceStack> context, String shop) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            ViScriptShopServerUtil.serverOpenShopEditor(player, shop);
            return 1;
        } else {
            throw playerOnlyException();
        }
    }

    @SneakyThrows
    private int openShopSelector(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            throw playerOnlyException();
        }

        ViScriptShopServerUtil.serverOpenShopSelector(player);
        return 1;
    }

    @SneakyThrows
    private int openShopTarget(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            List<String> args = parseGreedyArguments(context, "target");
            if (args.isEmpty() || args.size() > 3) {
                return sendInvalidUsage(context, "viscript_shop open <shop> [categoryId] [merchantId]");
            }

            String shop = args.getFirst();
            String categoryId = args.size() > 1 ? args.get(1) : null;
            String merchantId = args.size() > 2 ? args.get(2) : null;
            ViScriptShopServerUtil.serverOpenShop(player, shop, categoryId, merchantId);
            return 1;
        } else {
            throw playerOnlyException();
        }
    }

    @SneakyThrows
    private int reloadShop(CommandContext<CommandSourceStack> context) {
        List<String> args = parseGreedyArguments(context, "shop");
        if (args.size() != 1) {
            return sendInvalidUsage(context, "viscript_shop reload [shop]");
        }

        String shop = args.getFirst();
        ViScriptShopServerUtil.reloadOpenShop(shop);
        context.getSource().sendSuccess(() -> Component.translatable("command.viscript_shop.reload.shop"), true);
        return 1;
    }

    @SneakyThrows
    private int setQuickOpeningTarget(CommandContext<CommandSourceStack> context) {
        List<String> args = parseGreedyArguments(context, "target");
        if (args.size() != 2) {
            return sendInvalidUsage(context, "viscript_shop setQuickOpening <shop> <true|false>");
        }

        String shop = args.getFirst();
        Boolean quickOpening = parseBoolean(args.get(1));
        if (quickOpening == null) {
            context.getSource().sendFailure(Component.translatable("command.viscript_shop.error.invalid_boolean", args.get(1)));
            return 0;
        }

        if (ViScriptShopServerUtil.getShopInfo(shop) == null) {
            context.getSource().sendFailure(Component.translatable("command.viscript_shop.error.shop_not_found", shop));
            return 0;
        }

        ViScriptShopServerUtil.setQuickOpening(shop, quickOpening);
        context.getSource().sendSuccess(() -> Component.translatable("command.viscript_shop.setQuickOpening.shop", shop, quickOpening), true);
        return 1;
    }

    @SneakyThrows
    private int setMerchantStockTarget(CommandContext<CommandSourceStack> context) {
        List<String> args = parseGreedyArguments(context, "target");
        if (args.size() != 4) {
            return sendInvalidUsage(context, "viscript_shop setStock <shop> <categoryId> <merchantId> <stock>");
        }

        String shop = args.getFirst();
        String categoryId = args.get(1);
        String merchantId = args.get(2);
        Integer stock = parseInteger(args.get(3));
        if (stock == null) {
            context.getSource().sendFailure(Component.translatable("command.viscript_shop.error.invalid_stock", args.get(3)));
            return 0;
        }

        boolean success = ViScriptShopServerUtil.setMerchantStock(shop, categoryId, merchantId, stock);

        if (!success) {
            context.getSource().sendFailure(Component.translatable("command.viscript_shop.error.shop_not_found", shop));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.translatable("command.viscript_shop.setStock.success", merchantId, stock), true);
        return 1;
    }

    @SneakyThrows
    private int removeMerchantTarget(CommandContext<CommandSourceStack> context) {
        List<String> args = parseGreedyArguments(context, "target");
        if (args.size() != 3) {
            return sendInvalidUsage(context, "viscript_shop remove <shop> <categoryId> <merchantId>");
        }

        String shop = args.getFirst();
        String categoryId = args.get(1);
        String merchantId = args.get(2);

        boolean success = ViScriptShopServerUtil.removeMerchant(shop, categoryId, merchantId);

        if (!success) {
            context.getSource().sendFailure(Component.translatable("command.viscript_shop.error.shop_not_found", shop));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.translatable("command.viscript_shop.remove.success", merchantId), true);
        return 1;
    }

    @SneakyThrows
    private int addStageFlag(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            throw playerOnlyException();
        }

        String flag = StringArgumentType.getString(context, "flag").trim();
        if (flag.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("command.viscript_shop.stage.empty"));
            return 0;
        }

        boolean added = ViScriptShopServerUtil.addStageFlag(player, flag);
        if (added) {
            context.getSource().sendSuccess(() -> Component.translatable("command.viscript_shop.stage.add", flag), true);
            return 1;
        }

        context.getSource().sendFailure(Component.translatable("command.viscript_shop.stage.add.exists", flag));
        return 0;
    }

    @SneakyThrows
    private int removeStageFlag(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            throw playerOnlyException();
        }

        String flag = StringArgumentType.getString(context, "flag").trim();
        if (flag.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("command.viscript_shop.stage.empty"));
            return 0;
        }

        boolean removed = ViScriptShopServerUtil.removeStageFlag(player, flag);
        if (removed) {
            context.getSource().sendSuccess(() -> Component.translatable("command.viscript_shop.stage.remove", flag), true);
            return 1;
        }

        context.getSource().sendFailure(Component.translatable("command.viscript_shop.stage.remove.missing", flag));
        return 0;
    }

    public static List<String> getServerShopFiles() {
        List<String> shopFiles = new ArrayList<>();
        var assets = new File(LDLib2.getAssetsDir(), ShopHelper.SHOP_PATH);
        if (assets.exists() && assets.isDirectory()) {
            try (var stream = Files.walk(assets.toPath())) {
                stream.filter(Files::isRegularFile).forEach(file -> {
                    String string = file.toString();
                    if (string.endsWith(Shop.SUFFIX)) {
                        shopFiles.add(string.replace(assets.getPath(), "").substring(1).replace("\\", "/").replace(Shop.SUFFIX, ""));
                    }
                });
            } catch (IOException ignored) {
            }
        }
        return shopFiles;
    }

    private static CompletableFuture<Suggestions> suggestServerShopFiles(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        suggestMatching(getServerShopFiles(), builder);
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestSavedShops(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        suggestMatching(ViscriptShop.getShopSavedData().shopInfoMap.keySet(), builder);
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestOpenTarget(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return suggestShopCategoryMerchantTarget(builder, getServerShopFiles());
    }

    private static CompletableFuture<Suggestions> suggestSetStockTarget(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return suggestShopCategoryMerchantTarget(builder, getKnownShopIds());
    }

    private static CompletableFuture<Suggestions> suggestRemoveTarget(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return suggestShopCategoryMerchantTarget(builder, getKnownShopIds());
    }

    private static CompletableFuture<Suggestions> suggestQuickOpeningTarget(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        List<String> tokens = parseSuggestionArguments(builder.getRemaining());
        boolean trailingSpace = hasTrailingWhitespace(builder.getRemaining());
        int argIndex = getCurrentArgumentIndex(tokens, trailingSpace);
        SuggestionsBuilder currentBuilder = currentArgumentBuilder(builder);

        if (argIndex == 0) {
            suggestMatching(getKnownShopIds(), currentBuilder);
        } else if (argIndex == 1) {
            suggestMatching(List.of("true", "false"), currentBuilder);
        }

        return currentBuilder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestShopCategoryMerchantTarget(SuggestionsBuilder builder, Collection<String> shopIds) {
        List<String> tokens = parseSuggestionArguments(builder.getRemaining());
        boolean trailingSpace = hasTrailingWhitespace(builder.getRemaining());
        int argIndex = getCurrentArgumentIndex(tokens, trailingSpace);
        SuggestionsBuilder currentBuilder = currentArgumentBuilder(builder);

        if (argIndex == 0) {
            suggestMatching(shopIds, currentBuilder);
        } else if (argIndex == 1 && !tokens.isEmpty()) {
            suggestCategories(tokens.getFirst(), currentBuilder);
        } else if (argIndex == 2 && tokens.size() >= 2) {
            suggestMerchants(tokens.getFirst(), tokens.get(1), currentBuilder);
        }

        return currentBuilder.buildFuture();
    }

    // 补全分类ID
    private static void suggestCategories(String shopId, SuggestionsBuilder builder) {
        ShopInfo shopInfo = ViScriptShopServerUtil.getShopInfo(shopId);
        if (shopInfo != null) {
            for (CategoryInfo category : shopInfo.getCategoryInfos()) {
                suggestMatching(List.of(category.getId()), builder);
            }
        }
    }

    // 补全商品ID
    private static void suggestMerchants(String shopId, String categoryId, SuggestionsBuilder builder) {
        ShopInfo shopInfo = ViScriptShopServerUtil.getShopInfo(shopId);
        if (shopInfo != null) {
            for (CategoryInfo category : shopInfo.getCategoryInfos()) {
                if (category.getId().equals(categoryId)) {
                    for (MerchantInfo merchant : category.getMerchants()) {
                        suggestMatching(List.of(merchant.getId()), builder);
                    }
                    break;
                }
            }
        }
    }

    // 补全当前执行玩家已拥有的阶段标记
    private static CompletableFuture<Suggestions> suggestStageFlags(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player != null) {
            ViScriptShopServerUtil.getStageFlags(player).forEach(builder::suggest);
        }
        return builder.buildFuture();
    }

    private static Collection<String> getKnownShopIds() {
        LinkedHashSet<String> shopIds = new LinkedHashSet<>(getServerShopFiles());
        shopIds.addAll(ViscriptShop.getShopSavedData().shopInfoMap.keySet());
        return shopIds;
    }

    private static void suggestMatching(Collection<String> suggestions, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String suggestion : suggestions) {
            if (suggestion.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(suggestion);
            }
        }
    }

    private static List<String> parseGreedyArguments(CommandContext<CommandSourceStack> context, String argumentName) throws CommandSyntaxException {
        return parseArguments(StringArgumentType.getString(context, argumentName));
    }

    private static List<String> parseSuggestionArguments(String input) {
        try {
            return parseArguments(input);
        } catch (CommandSyntaxException ignored) {
            String trimmed = input.trim();
            if (trimmed.isEmpty()) {
                return List.of();
            }
            return List.of(trimmed.split("\\s+"));
        }
    }

    private static List<String> parseArguments(String input) throws CommandSyntaxException {
        StringReader reader = new StringReader(input);
        List<String> args = new ArrayList<>();
        while (reader.canRead()) {
            reader.skipWhitespace();
            if (!reader.canRead()) {
                break;
            }
            if (StringReader.isQuotedStringStart(reader.peek())) {
                args.add(reader.readQuotedString());
            } else {
                args.add(readUnquotedGreedyToken(reader));
            }
        }
        return args;
    }

    private static String readUnquotedGreedyToken(StringReader reader) {
        int start = reader.getCursor();
        while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
            reader.skip();
        }
        return reader.getString().substring(start, reader.getCursor());
    }

    private static SuggestionsBuilder currentArgumentBuilder(SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        if (remaining.isEmpty()) {
            return builder;
        }
        int lastSpace = Math.max(remaining.lastIndexOf(' '), remaining.lastIndexOf('\t'));
        return lastSpace < 0 ? builder : builder.createOffset(builder.getStart() + lastSpace + 1);
    }

    private static int getCurrentArgumentIndex(List<String> tokens, boolean trailingSpace) {
        if (tokens.isEmpty()) {
            return 0;
        }
        return trailingSpace ? tokens.size() : tokens.size() - 1;
    }

    private static boolean hasTrailingWhitespace(String input) {
        return !input.isEmpty() && Character.isWhitespace(input.charAt(input.length() - 1));
    }

    private static Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Boolean parseBoolean(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        return null;
    }

    private static int sendInvalidUsage(CommandContext<CommandSourceStack> context, String usage) {
        context.getSource().sendFailure(Component.translatable("command.viscript_shop.error.invalid_usage", usage));
        return 0;
    }
}

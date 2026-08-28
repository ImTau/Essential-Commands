package com.fibermc.essentialcommands.commands;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.fibermc.essentialcommands.playerdata.PlayerData;
import com.fibermc.essentialcommands.playerdata.PlayerDataManager;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Resolves online players by real username first, then by a unique
 * Essential Commands nickname.
 */
public final class NicknameTargetResolver {
    private NicknameTargetResolver() {}

    /**
     * Greedy player argument for commands where the target is the final
     * argument. This allows unquoted nicknames containing spaces.
     */
    public static RequiredArgumentBuilder<CommandSourceStack, String> targetPlayerArgument() {
        return Commands.argument("target_player", StringArgumentType.greedyString())
            .suggests((context, builder) -> suggestPlayers(context, builder, false));
    }

    /**
     * Non-greedy player argument for commands that have arguments after the
     * target. Nicknames containing spaces can be quoted.
     */
    public static RequiredArgumentBuilder<CommandSourceStack, String> targetPlayerArgumentNonGreedy() {
        return Commands.argument("target_player", StringArgumentType.string())
            .suggests((context, builder) -> suggestPlayers(context, builder, true));
    }

    private static CompletableFuture<Suggestions> suggestPlayers(
        CommandContext<CommandSourceStack> context,
        SuggestionsBuilder builder,
        boolean quoteSuggestions
    ) throws CommandSyntaxException {
        if (builder.getRemaining().startsWith("@")) {
            return EntityArgument.player().listSuggestions(context, builder);
        }

        var names = new LinkedHashSet<String>();
        for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
            names.add(player.getGameProfile().name());
            PlayerData.access(player)
                .getNickname()
                .map(Component::getString)
                .filter(nickname -> !nickname.isBlank())
                .ifPresent(names::add);
        }

        String remaining = builder.getRemaining();
        String prefix = remaining.startsWith("\"") ? remaining.substring(1) : remaining;
        prefix = prefix.toLowerCase(Locale.ROOT);

        for (String name : names) {
            if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                builder.suggest(quoteSuggestions ? quoteIfNeeded(name) : name);
            }
        }
        return builder.buildFuture();
    }

    private static String quoteIfNeeded(String value) {
        if (value.indexOf(' ') < 0 && value.indexOf('"') < 0 && value.indexOf('\\') < 0) {
            return value;
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    public static ServerPlayer getPlayer(
        CommandContext<CommandSourceStack> context,
        String argumentName
    ) throws CommandSyntaxException {
        String target = StringArgumentType.getString(context, argumentName).trim();

        // Preserve normal vanilla selector behavior.
        if (target.startsWith("@")) {
            EntitySelector selector = EntityArgument.player().parse(
                new StringReader(target),
                context.getSource()
            );
            return selector.findSinglePlayer(context.getSource());
        }

        // Real Minecraft usernames always win over nickname collisions.
        ServerPlayer usernameMatch = context
            .getSource()
            .getServer()
            .getPlayerList()
            .getPlayerByName(target);
        if (usernameMatch != null) {
            return usernameMatch;
        }

        var nicknameMatches = PlayerDataManager
            .getInstance()
            .getPlayerDataMatchingNickname(target)
            .stream()
            .map(PlayerData::getPlayer)
            .filter(Objects::nonNull)
            .toList();

        if (nicknameMatches.size() == 1) {
            return nicknameMatches.get(0);
        }
        if (nicknameMatches.size() > 1) {
            throw CommandUtil.createSimpleException(Component.literal(
                "Nickname \"" + target + "\" matches more than one online player. Use the real username instead."
            ));
        }

        throw CommandUtil.createSimpleException(Component.literal(
            "No online player found with username or nickname \"" + target + "\"."
        ));
    }
}

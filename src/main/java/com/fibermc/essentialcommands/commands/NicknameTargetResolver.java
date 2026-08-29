package com.fibermc.essentialcommands.commands;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import com.fibermc.essentialcommands.access.EntitySelectorNicknameAccess;
import com.fibermc.essentialcommands.playerdata.PlayerData;
import com.fibermc.essentialcommands.playerdata.PlayerDataManager;
import com.fibermc.essentialcommands.types.NicknameCommandArgMode;
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

import static com.fibermc.essentialcommands.EssentialCommands.CONFIG;

/**
 * Adds optional nickname fallback on top of Minecraft's normal player
 * argument handling. Vanilla resolution is always attempted first.
 */
public final class NicknameTargetResolver {
    private NicknameTargetResolver() {}

    public static RequiredArgumentBuilder<CommandSourceStack, EntitySelector> targetPlayerArgument() {
        return Commands.argument("target_player", EntityArgument.player())
            .suggests(NicknameTargetResolver::suggestPlayers);
    }

    public static RequiredArgumentBuilder<CommandSourceStack, EntitySelector> targetPlayerArgumentNonGreedy() {
        return targetPlayerArgument();
    }

    private static CompletableFuture<Suggestions> suggestPlayers(
        CommandContext<CommandSourceStack> context,
        SuggestionsBuilder builder
    ) throws CommandSyntaxException {
        if (
            CONFIG.NICKNAMES_AS_COMMAND_ARG != NicknameCommandArgMode.Never
            && PlayerDataManager.exists()
        ) {
            String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
            var nicknameSuggestions = new LinkedHashSet<String>();

            for (PlayerData playerData : PlayerDataManager.getInstance().getAllPlayerData()) {
                playerData.getNickname()
                    .map(Component::getString)
                    .map(NicknameTargetResolver::normalizeNickname)
                    .filter(nickname -> !nickname.isBlank())
                    .ifPresent(nicknameSuggestions::add);
            }

            for (String nickname : nicknameSuggestions) {
                if (nickname.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                    builder.suggest(nickname);
                }
            }
        }

        return EntityArgument.player().listSuggestions(context, builder);
    }

    /**
     * Resolves an Essential Commands player argument.
     *
     * Everywhere:
     *   EntityArgument resolves nicknames through EntitySelectorNicknameMixin.
     * EssentialCommandsOnly:
     *   Vanilla is attempted first, then the shared literal nickname fallback.
     * Never:
     *   Vanilla behavior is used unchanged.
     */
    public static ServerPlayer getPlayer(
        CommandContext<CommandSourceStack> context,
        String argumentName
    ) throws CommandSyntaxException {
        try {
            return EntityArgument.getPlayer(context, argumentName);
        } catch (CommandSyntaxException vanillaFailure) {
            if (CONFIG.NICKNAMES_AS_COMMAND_ARG != NicknameCommandArgMode.EssentialCommandsOnly) {
                throw vanillaFailure;
            }

            EntitySelector selector = context.getArgument(argumentName, EntitySelector.class);
            String playerName = ((EntitySelectorNicknameAccess) (Object) selector).ec$getPlayerName();
            ServerPlayer nicknameMatch = resolveLiteralPlayer(playerName);
            if (nicknameMatch != null) {
                return nicknameMatch;
            }

            throw vanillaFailure;
        }
    }

    /**
     * Finds one online player whose nickname matches the literal command
     * player name after whitespace normalization.
     *
     * Returns null for no match or an ambiguous normalized nickname.
     */
    public static ServerPlayer resolveLiteralPlayer(String playerName) {
        if (
            playerName == null
            || playerName.isBlank()
            || !PlayerDataManager.exists()
        ) {
            return null;
        }

        String target = normalizeNickname(playerName);
        if (target.isBlank()) {
            return null;
        }

        ServerPlayer nicknameMatch = null;
        for (PlayerData playerData : PlayerDataManager.getInstance().getAllPlayerData()) {
            boolean matches = playerData
                .getNickname()
                .map(Component::getString)
                .map(NicknameTargetResolver::normalizeNickname)
                .map(nickname -> nickname.equalsIgnoreCase(target))
                .orElse(false);

            if (!matches) {
                continue;
            }

            ServerPlayer player = playerData.getPlayer();
            if (player == null) {
                continue;
            }

            if (nicknameMatch != null && nicknameMatch != player) {
                // Normalization can make different nicknames collide, e.g.
                // "Foo Bar" and "FooBar". Never choose one arbitrarily.
                return null;
            }

            nicknameMatch = player;
        }

        return nicknameMatch;
    }

    private static String normalizeNickname(String nickname) {
        return nickname.replaceAll("\\s+", "");
    }
}

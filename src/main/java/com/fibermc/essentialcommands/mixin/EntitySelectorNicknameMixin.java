package com.fibermc.essentialcommands.mixin;

import java.util.Collections;
import java.util.List;

import com.fibermc.essentialcommands.playerdata.PlayerData;
import com.fibermc.essentialcommands.playerdata.PlayerDataManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Adds Essential Commands nickname fallback to vanilla literal player
 * selectors. Vanilla usernames retain priority, and @ selectors are
 * unaffected because they do not populate EntitySelector.playerName.
 */
@Mixin(EntitySelector.class)
public abstract class EntitySelectorNicknameMixin {
    @Shadow
    @Final
    private String playerName;

    @Inject(method = "findPlayers", at = @At("HEAD"), cancellable = true)
    private void ec$resolveNicknameForPlayers(
        CommandSourceStack source,
        CallbackInfoReturnable<List<ServerPlayer>> cir
    ) {
        ServerPlayer player = ec$resolveLiteralPlayer(source);
        if (player != null) {
            cir.setReturnValue(Collections.singletonList(player));
        }
    }

    @Inject(method = "findEntities", at = @At("HEAD"), cancellable = true)
    private void ec$resolveNicknameForEntities(
        CommandSourceStack source,
        CallbackInfoReturnable<List<? extends Entity>> cir
    ) {
        ServerPlayer player = ec$resolveLiteralPlayer(source);
        if (player != null) {
            cir.setReturnValue(Collections.singletonList(player));
        }
    }

    private ServerPlayer ec$resolveLiteralPlayer(CommandSourceStack source) {
        if (this.playerName == null || this.playerName.isBlank()) {
            return null;
        }

        // Preserve vanilla username precedence.
        ServerPlayer usernameMatch = source
            .getServer()
            .getPlayerList()
            .getPlayerByName(this.playerName);
        if (usernameMatch != null) {
            return usernameMatch;
        }

        if (!PlayerDataManager.exists()) {
            return null;
        }

        List<PlayerData> matches = PlayerDataManager
            .getInstance()
            .getPlayerDataMatchingNickname(this.playerName);

        ServerPlayer nicknameMatch = null;
        for (PlayerData playerData : matches) {
            ServerPlayer player = playerData.getPlayer();
            if (player == null) {
                continue;
            }
            if (nicknameMatch != null && nicknameMatch != player) {
                // Ambiguous nickname: let vanilla fail rather than selecting
                // an arbitrary player.
                return null;
            }
            nicknameMatch = player;
        }
        return nicknameMatch;
    }
}

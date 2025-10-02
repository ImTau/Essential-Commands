package com.fibermc.essentialcommands.mixin;

import java.util.Optional;

import com.fibermc.essentialcommands.playerdata.PlayerDataManager;
import com.fibermc.essentialcommands.types.MinecraftLocation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.network.ClientConnection;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.util.math.ChunkPos;

@Mixin(targets = "net.minecraft.server.network.PrepareSpawnTask$PlayerSpawn")
public abstract class PrepareSpawnTaskMixin {
    @Inject(
        method = "onReady",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;refreshPositionAndAngles(Lnet/minecraft/util/math/Vec3d;FF)V"
        )
    )
    public void onReady_firstConnect_spawnPositionOverride(
        ClientConnection connection, ConnectedClientData clientData, CallbackInfoReturnable<ServerPlayerEntity> cir,
        @Local(ordinal = 0) ChunkPos chunkPos,
        @Local(ordinal = 0) ServerPlayerEntity serverPlayerEntity,
        @Local(ordinal = 0) Optional<ReadView> playerNbt
    ) {
        if (playerNbt.isPresent()) {
            // player data existed, definitely isn't first join
            return;
        }

        final MinecraftLocation[] location = new MinecraftLocation[1];
        PlayerDataManager.handleRespawnAtEcSpawn(null, (spawnPos) -> {
            location[0] = spawnPos;
        });

        if (location[0] == null) {
            // EC respawner doesn't want the player on EC spawn
            return;
        }

        var pos = location[0];
        serverPlayerEntity.setServerWorld(serverPlayerEntity.getEntityWorld().getServer().getWorld(location[0].dim()));
        serverPlayerEntity.refreshPositionAndAngles(pos.pos(), pos.headYaw(), pos.pitch());
    }
}

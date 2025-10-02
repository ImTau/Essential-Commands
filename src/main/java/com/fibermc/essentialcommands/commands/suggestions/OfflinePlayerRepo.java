package com.fibermc.essentialcommands.commands.suggestions;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.response.NameAndId;

import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.network.ServerPlayerEntity;

public class OfflinePlayerRepo {

    private final HashMap<String, NameAndId> gameProfileCache = new HashMap<>();
    private final MinecraftServer server;

    public OfflinePlayerRepo(MinecraftServer server) {
        this.server = server;
    }

    public CompletableFuture<ServerPlayerEntity> getOfflinePlayerByNameAsync(String playerName) {
        return getGameProfile(playerName)
            .handle(((gameProfile, throwable) -> gameProfile.map(this::getOfflinePlayer).orElse(null)));
    }

    public ServerPlayerEntity getOfflinePlayer(NameAndId playerProfile) {
        var player = new ServerPlayerEntity(
            server,
            server.getOverworld(),
            new GameProfile(playerProfile.id(), playerProfile.name()),
            SyncedClientOptions.createDefault());

        server.getPlayerManager().loadPlayerData(new PlayerConfigEntry(playerProfile.id(), playerProfile.name()));

        return player;
    }

    public CompletableFuture<Optional<NameAndId>> getGameProfile(String playerName) {
        var profile = gameProfileCache.get(playerName);
        if (profile != null) {
            CompletableFuture.completedFuture(profile);
        }
        return requestGameProfile(playerName)
            .whenComplete((gameProfile, err) -> {
                gameProfile.ifPresent(nameAndId -> gameProfileCache.put(nameAndId.name(), nameAndId));
            });
    }

    private final ExecutorService gameProfileExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private CompletableFuture<Optional<NameAndId>> requestGameProfile(String playerName) {
        CompletableFuture<Optional<NameAndId>> completable = new CompletableFuture<>();

        gameProfileExecutor.execute(() -> {
            try {
                completable.complete(server.getApiServices().profileRepository().findProfileByName(playerName));
            } catch (Exception ex) {
                completable.completeExceptionally(ex);
            }
        });

        return completable;
    }
}

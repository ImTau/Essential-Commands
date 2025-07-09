package com.fibermc.essentialcommands;

import java.util.Optional;

import com.fibermc.essentialcommands.codec.Codecs;
import com.fibermc.essentialcommands.types.MinecraftLocation;
import com.fibermc.essentialcommands.types.WarpStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;

public class WorldData {
    private @Nullable MinecraftLocation spawnLocation;
    private final @NotNull WarpStorage warps;

    WorldData() {
        this.spawnLocation = null;
        this.warps = new WarpStorage();
    }

    WorldData(Optional<MinecraftLocation> spawnLocation, @NotNull WarpStorage warps) {
        this.spawnLocation = spawnLocation.orElse(null);
        this.warps = warps;
    }

    public WarpStorage warps() {
        return this.warps;
    }

    public Optional<MinecraftLocation> getSpawn() {
        return Optional.ofNullable(this.spawnLocation);
    }

    public void setSpawn(@Nullable MinecraftLocation spawn) {
        this.spawnLocation = spawn;
    }

    public static WorldData fromNbt(NbtCompound nbt) {
        return CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow();
    }

    public NbtCompound toNbt() {
        return CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow().asCompound().orElseThrow();
    }

    public static final Codec<WorldData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codecs.MINECRAFT_LOCATION.optionalFieldOf("spawn").forGetter(WorldData::getSpawn),
            Codecs.WARP_STORAGE.fieldOf("warps").forGetter(WorldData::warps)
        ).apply(instance, WorldData::new)
    );
}

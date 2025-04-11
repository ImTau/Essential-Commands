package com.fibermc.essentialcommands.types;

import java.util.Optional;

import com.fibermc.essentialcommands.ECPerms;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

public class WarpLocation extends NamedMinecraftLocation {

    private String permissionString;

    private WarpLocation() {}

    /**
     * @param permissionString The string permission node for the warp. Null for no required permisison.
     */
    public WarpLocation(NamedMinecraftLocation location, String permissionString) {
        super(location, location.getName());
        this.permissionString = permissionString;
    }

    public WarpLocation(MinecraftLocation location, String permissionString, String name) {
        super(location, name);
        this.permissionString = permissionString;
    }

    public static WarpLocation fromNbt(NbtCompound tag, String name) {
        // `data` was the main obj key in old mc PersistentState schema
        tag = tag.getCompound("data").orElse(tag);

        var loc = new WarpLocation();
        loc.loadNbt(tag, name);
        loc.permissionString = tag.getString("permissionString")
            .flatMap(str -> str.isBlank() ? Optional.empty() : Optional.of(str))
            .orElse(null);
        return loc;
    }

    @Override
    public NbtCompound asNbt() {
        return this.writeNbt(new NbtCompound());
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        super.writeNbt(tag);
        if (permissionString != null) {
            tag.putString("permissionString", permissionString);
        }
        return tag;
    }

    public String getPermissionString() {
        return permissionString;
    }

    public boolean hasPermission(ServerPlayerEntity player) {
        return permissionString == null || ECPerms.check(
            player.getCommandSource(),
            String.format("%s.%s", ECPerms.Registry.warp_tp_named, permissionString));
    }
}

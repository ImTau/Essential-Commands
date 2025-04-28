
# Permissions Quickstart

Essential Commands ships with many features granted to all players by default, but some restricted. For more control over which features players can use, you'll want a permissions mod. I personally recommend one of [Luck Perms][luck-perms] or [PlayerRoles][player-roles], but anything that works with the Fabric Permissions API will do the job.

To start using a permissions mod with Essential Commands, set `use_permissions_api` to `true` in the file `config/EssentialCommands.properties`, install your chosen permission mod, and restart the server. For the remainder of this section, I'll focus on Luck Perms.

To give yourself permission to manage permissions, go to your server console and run

```
lp user <your username> permission set luckperms.* true
```

Now, all that is left to do is grant users or groups access to EC commands. Some examples:

Grant all players access to every command in EC:

```
/lp group default permission set essentialcommands.* true
```

Grant all players full regular access to the `tpa` and `home` features, and to teleport to `warp`s:

```
/lp group default permission set essentialcommands.tpa true
/lp group default permission set essentialcommands.tpahere true
/lp group default permission set essentialcommands.tpaccept true
/lp group default permission set essentialcommands.tpdeny true

/lp group default permission set essentialcommands.home.{set,tp,delete} true
/lp group default permission set essentialcommands.warp.tp true
```

Create a group called `admin` and grant it access to create and delete `warp`s:

```
/lp creategroup admin
/lp group admin permission set essentialcommands.warp.{set,delete} true
```

See the [List of Commands & Permissions](List-of-Commands-&-Permissions) to see what other Essential Commands permissions are available.

[luck-perms]: https://luckperms.net/wiki/Usage
[player-roles]: https://github.com/NucleoidMC/player-roles

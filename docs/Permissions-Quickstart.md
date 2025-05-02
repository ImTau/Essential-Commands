# Permissions Quickstart

Essential Commands ships with many features granted to all players by default,
but some restricted. For more control over which features players can use,
you'll want a permissions mod. I personally recommend one of [Luck
Perms][luck-perms] or [PlayerRoles][player-roles], but anything that works with
the Fabric Permissions API will do the job.

To start using a permissions mod with Essential Commands, set
`use_permissions_api` to `true` in the file
`config/EssentialCommands.properties`, install your chosen permission mod, and
restart the server.

Choose one of the Tabs below depending on which permissions mod you chose to
use:

<!-- tabs:start -->

### **Luck Perms**

To give yourself permission to manage permissions, go to your server console and
run

```
lp user <your username> permission set luckperms.* true
```

Now, all that is left to do is grant users or groups access to EC commands. Some
examples:

Grant all players full regular access to the `tpa` and `home` features, and to
teleport to `warp`s:

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

### **Player Roles**

Player roles is primarily managed with a single config file,
`config/roles.json`.

A simple starter configuration for Essential commands might look something like
this:

```json
{
    "admin": {
        "level": 100,
        "overrides": {
            "permission_keys": {
                "essentialcommands.warp.set": true,
                "essentialcommands.warp.delete": true
            }
        }
    },
    "everyone": {
        "overrides": {
            "permission_keys": {
                "essentialcommands.tpa": true,
                "essentialcommands.tpahere": true,
                "essentialcommands.tpaccept": true,
                "essentialcommands.tpdeny": true,
                "essentialcommands.home.set": true,
                "essentialcommands.home.tp": true,
                "essentialcommands.home.delete": true,
                "essentialcommands.warp.tp": true
            }
        }
    }
}
```

This creates a role `admin`, with the ability to create and delete `warp`s, and
grants all players the ability to use the `tpa` and `home` features, and the
ability to teleport to `warp`s.

<!-- tabs:end -->

See the [List of Commands & Permissions](List-of-Commands-&-Permissions) to see
what other Essential Commands permissions are available.

[luck-perms]: https://luckperms.net/wiki/Usage
[player-roles]: https://github.com/NucleoidMC/player-roles

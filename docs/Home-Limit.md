
## Setting home limits

The maximum number of homes players can have is determined by the `home_limit` config
option in `config/EssentialCommands.properties`.

This the `home_limit` field accepts a comma-separated list of integer numbers. The value
supplied has different effects depending on the value of the `use_permissions_api` config
option:

### If `use_permissions_api` is `false` in the config

The lowest value in the `home_limit` list is used for all non-op players. (the rest are ignored)
(you only really have to supply 1)

Example:

```properties
use_permissions_api=false
#...
home_limit=5
```

### If `use_permissions_api` is `true` in the config

Each number corresponds to a permission node that is then generated.

Example:

```properties
use_permissions_api=true
#...
home_limit=1,3,5
```

would generate the permissions:

```txt
essentialcommands.home.limit.1
essentialcommands.home.limit.3
essentialcommands.home.limit.5
```

You can then assign these permissions to users using your permission manager of choice.

OPs have unlimited homes (and other numeric properties), regardless of whether the permissions
api is being used.

---

### Other Options

#### `grant_lowest_numeric_by_default`

If this option is enabled, EssentialCommands will behave as if all players have the lowest numeric permission node in a group (instead of 0), unless the player has explicitly been granted a higher numeric permission.

### Common Errors

#### LuckPerms Wildcards

LuckPerms interprets permissions like `essentialcommands.home` as wildcards by default. If you had that node granted, you would implicitly have all of:
  - `essentialcommands.home.tp`
  - `essentialcommands.home.set`
  - `essentialcommands.home.delete`
  - `essentialcommands.home.limit.1`
  - `essentialcommands.home.limit.3`
  - `essentialcommands.home.limit.5`

Because of this, a player with `essentialcommands.home` granted, would always have access to the maximum number of homes (in this case, 5), even if you explicitly assigned a permission node with a lower limit.
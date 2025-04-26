
# File Locations

All locations are relative to the root dir of the server. (the same folder as `server.properties`).

These locations may change in the future. Such changes will be documented here
and in the changelog of the update in which they are modified.

The `.dat` files are stored using Minecraft's compressed NBT format. You can read and modify them manually
with a tool like [NBT Explorer](https://github.com/jaquadro/NBTExplorer).

File Contents | Location
---|---
Config  | `config/EssentialCommands.properties`
World Data (warps, spawn)   | `world/essentialcommands/world_data.dat`
Player Data (homes, nickname, etc.) | `world/modplayerdata/PLAYER_UUID.dat`

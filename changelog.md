## Essential Commands `v0.40.0-beta1` (mc 26.2.0 snapshot 2)

- upgrade to Minecraft 26.2.0 (snapshot 2) (#391) by @eclipseisoffline
- fix server placeholders (#388) by @arnokeesman 
- resolve unexpected CR box characters in /rules output (#388) by @tiehu
- fix permissions system not handling "Permissions Only, not OP" correctly (#380)
- migrate to Server Translations for langfiles (#384)
- fix server placeholders in MOTD by @arnokeesman
- use modrinth for update checking, enabling per-mc-version accuracy (#12)

--- --- ---

## Essential Commands `v0.39.0` (mc 26.1.1)

Note well that Minecraft has a new versioning scheme. This version of Essential
Commands supports Minecraft 26.1.1, the version following 1.21.11.

This upgrade includes:

- QOL improvements to /home and /warp, allowing `/home <name>` instead of requiring `/home tp <name>`, by @HongMeiIing (#358)
- fix RtpCenter configuration not accepting negative coords by @PalorderSoftWorksOfficial (#373)
- supporting the new non-obfuscated mojang source, by @arnokeesman (#361)
- force-disable the sleep command due to an exploitable bug (until I can find a solution)

--- --- ---

## Essential Commands `v0.38.6` (mc 1.21.9)

- upgrade to Minecraft 1.21.9

--- --- ---

## Essential Commands `v0.38.5` (mc 1.21.8)

- assorted internal dependency and tooling upgrades

--- --- ---

## Essential Commands `v0.38.4` (mc 1.21.8)

- fix playerdata saving mistake that could cause homes to not save under certian conditions
- assorted dependency upgrades to improve mod compatability

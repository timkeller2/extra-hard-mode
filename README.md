# Extra Hard Mode

Vanilla Hard only punishes mistakes. Extra Hard Mode changes the rules so experts still have to think.

A Fabric mod for **Minecraft Java 26.2** (Java 25). Runs in singleplayer and on dedicated Fabric servers. **Not for Realms** — Fabric mods do not run there.

This is a reimplementation of Extra Hard Mode, not a copy of `com.extrahardmode.*`. Licensed **AGPL-3.0-or-later**. See `LICENSE` and `NOTICE`.

## Build

Requires **JDK 25**.

```bash
./gradlew build
```

On Windows: `.\gradlew.bat build`. Output jars land in `build/libs/`.

`./gradlew compileJava compileClientJava` is enough to typecheck without packaging.

## Enable

The gamerule `extrahardmode:enabled` is a **server-global master switch** (26.2 game rules are not per-dimension). First-apply copies `enabledByDefault` into that gamerule once (overworld load). `/gamerule extrahardmode:enabled false` turns Extra Hard Mode off everywhere; `/gamerule extrahardmode:enabled true` turns it on in every dimension that has not opted out.

Each dimension has a live `enabled` flag that **defaults true** (opt-out). Custom dimensions inherit the overworld's current live flag so a world that opted Overworld out does not spring EHM on a new datapack dim. `/ehm set-world <bool>` (or `/ehm enable` / `/ehm disable`) toggles the current dimension. `WorldGate.isActive` is gamerule AND the dimension flag.

If `enabledByDefault` is missing, it defaults to `true` in singleplayer and `false` on dedicated servers.

Per-dimension TOML lives under the overworld save: `data/extrahardmode/<namespace>/<path>.toml` (for example `world/data/extrahardmode/minecraft/overworld.toml` on a dedicated server). Commands: `/ehm`, `/ehm version`, `/ehm enabled [world]`, `/ehm reload`, `/ehm debug`, `/ehm bypass`, `/ehm set <module> <bool>`, `/ehm set-world <bool>`, `/ehm enable`, `/ehm disable`.

## Tutorial

Instant denies (torch, pillaring, ore-next-to-stone, hardened-stone help) use the **action bar**. First-time mechanics use a **SystemToast**, at most `tutorial.maxShows` times per player (default 3; extras such as weight / zombie slow / melon seeds / dragon are once). Shown counts persist on the player as `ehm:tutorial`. Silent permission nodes from the original plugin.yml still work: `extrahardmode.silent.stone_mining_help`, `no_placing_ore_against_stone`, `realistic_building`, `limited_torch_placement`, `no_torches_here`. `/ehm set tutorial false` turns toasts off; action-bar denies still fire.

Dedicated servers do not load client extras. Optional **Cloth Config** + **Mod Menu** add a config screen when those mods are installed (26.2 artifacts `cloth-config-fabric` 26.2.155 and Mod Menu 20.0.1). The published jar does not bundle them.

## Credits

- Ryan "BigScary" Hamshire (original)
- Diemex
- RoboMWM
- Mitsugaru
- Grok Design (26.2 Fabric port)

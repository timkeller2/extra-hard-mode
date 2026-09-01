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

The gamerule `extrahardmode:enabled` is a **server-global master switch** (26.2 game rules are not per-dimension). `/gamerule extrahardmode:enabled false` turns Extra Hard Mode off everywhere.

Each dimension also has its own `enabled` flag, first-applied independently from `config/extrahardmode.toml` `enabledByDefault` (vanilla overworld/nether/end) or inherited from the overworld flag (custom dimensions). That flag is never overwritten once stamped. `WorldGate.isActive` is gamerule AND the dimension flag, so `/ehm enabled [world]` can disagree across dimensions while the gamerule is on.

If `enabledByDefault` is missing, it defaults to `true` in singleplayer and `false` on dedicated servers.

Per-dimension TOML lives under the overworld save: `data/extrahardmode/<namespace>/<path>.toml` (for example `world/data/extrahardmode/minecraft/overworld.toml` on a dedicated server). Commands: `/ehm`, `/ehm version`, `/ehm enabled [world]`, `/ehm reload`, `/ehm debug`, `/ehm bypass`, `/ehm set <module> <bool>`.

## Credits

- Ryan "BigScary" Hamshire (original)
- Diemex
- RoboMWM
- Mitsugaru
- Grok Design (26.2 Fabric port)

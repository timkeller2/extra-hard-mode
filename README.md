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

World enable via `/gamerule` ships in a later change. Until then the mod loads and registers no gameplay modules.

Global defaults live in `config/extrahardmode.toml` (`enabledByDefault`, `debug`). If `enabledByDefault` is missing, it defaults to `true` in singleplayer (client environment) and `false` on dedicated servers.

## Credits

- Ryan "BigScary" Hamshire (original)
- Diemex
- RoboMWM
- Mitsugaru
- Grok Design (26.2 Fabric port)

# Tougher

Vanilla Hard only punishes mistakes. Tougher **changes the rules** so experts still have to think. Stone does not yield to cheap picks, caves collapse when you mine ore, torches fail in the deep, water sources cannot be bucket-cloned, and every mob has a trick.

A Fabric mod for **Minecraft Java 26.3** (Java 25). Runs in singleplayer (integrated server) and on dedicated Fabric servers. **Not for Realms** — Fabric mods do not run there.

This is a greenfield reimplementation of Extra Hard Mode, renamed **Tougher**, not a copy of `com.extrahardmode.*`. Licensed **AGPL-3.0-or-later**. See [`LICENSE`](LICENSE), [`NOTICE`](NOTICE), and [`DESIGN.md`](DESIGN.md) for mechanics, Y remaps, and key decisions.

## Install

Requires:

- **Minecraft Java Edition 26.3** (exact)
- **Java 25**
- **Fabric Loader** `>=0.19.5`
- **Fabric API** `>=0.161.0+26.3`

Put the Tougher jar and Fabric API in `mods/`. The published jar does not bundle Fabric API.

Each push to `main` publishes a rolling GitHub Release: [latest Tougher jar](https://github.com/timkeller2/extra-hard-mode/releases/latest). `tougher.jar` is a stable filename for start scripts.

Optional client extras: **Cloth Config** and **Mod Menu** are suggested, not required. Dedicated servers load `main` + mixins and skip client extras. If no 26.3 Cloth Config artifact resolves, there is no in-game config screen — edit TOML instead. Do not invent Cloth or Sodium version pins. The mod does not depend on Sodium, Lithium, Iris, GraviTree, WorldGuard, or any other gameplay/render mod.

Build from source (JDK 25):

```bash
./gradlew build
```

On Windows: `.\gradlew.bat build`. Output jars land in `build/libs/`.

`./gradlew compileJava compileClientJava` typechecks without packaging.

## Enable

The gamerule `tougher:enabled` is a **server-global master switch** (26.3 game rules are not per-dimension). First-apply copies `enabledByDefault` from global TOML into that gamerule once (overworld load).

```
/gamerule tougher:enabled true
/gamerule tougher:enabled false
```

`/gamerule tougher:enabled false` turns Tougher off everywhere. `/gamerule tougher:enabled true` turns it on in every dimension that has not opted out.

Each dimension has a live `enabled` flag that **defaults true** (opt-out). Custom dimensions inherit the overworld's current live flag so a world that opted Overworld out does not spring Tougher on a new datapack dim. Toggle the current dimension with:

```
/tougher set-world true
/tougher set-world false
```

(`/tougher enable` / `/tougher disable` are aliases. `/ehm` still works as a command alias.) `WorldGate.isActive` is the gamerule **and** the dimension flag.

If `enabledByDefault` is missing, it defaults to `true` in singleplayer and `false` on dedicated servers. Operators who want new dedicated worlds on by default set `enabledByDefault = true` in `config/tougher.toml` **before** first load.

Per-dimension TOML lives under the overworld save: `data/tougher/<namespace>/<path>.toml` (for example `world/data/tougher/minecraft/overworld.toml` on a dedicated server). Global defaults: `config/tougher.toml`.

Commands: `/tougher` (alias `/ehm`), `/tougher version`, `/tougher enabled [world]`, `/tougher reload`, `/tougher debug`, `/tougher bypass`, `/tougher set <module> <bool>`, `/tougher set-world <bool>`, `/tougher enable`, `/tougher disable`.

`/tougher reload` (`tougher.admin`) reloads TOML on the server thread. Datapack tags still reload with vanilla `/reload`.

## Limitations (v1)

- **Not for Realms.** Fabric mods do not run on Realms. Do not file Realms tickets.
- **SMP claim plugins.** There is no WorldGuard / claim-mod explosion or cave-in hook in v1. Cave-ins, falling blocks, and Tougher explosions can grief claimed land. That is a stated v1 gap, not a promise to respect claims.
- **Teleport plugins.** Tougher does **not** add `/home`, `/back`, or `/tpa`, and authors must not add them. Those commands fight death-forfeit, weight, and environmental injury. Incompatible with typical teleport plugins; do not implement a compatibility layer in v1.
- **Villager trade nerf** is optional (default on, not an original Extra Hard Mode feature).
- **Create drills** that skip the player break event may not drain hardened-stone budgets. Known gap.

## License (AGPL)

Licensed under **GNU Affero GPL v3.0 or later**. Original Extra Hard Mode authors are listed below; this port is a derivative work.

**§6 (Corresponding Source for the jar).** Shipping or otherwise conveying the compiled jar is conveying object code. You **must** offer Corresponding Source — this public git repo satisfies that when you distribute an unmodified build from it. If you distribute a modified jar, you must offer that modified source the same way.

**§13 (network servers).** If you run a **modified** version that players interact with over a network (a dedicated SMP server with your patches), you must offer those players Corresponding Source for the version they are playing. Unmodified public builds do not trigger a separate §13 duty beyond §6.

The public `EhmApi` is AGPL-viral: any mod that compiles against it is AGPL.

## GameTests

Fabric GameTests are the `fabric-gametest` entrypoint (`dev.extrahardmode.test.EhmGameTests`).

```bash
./gradlew runGameTest
```

Windows: `.\gradlew.bat runGameTest`

That starts Loom's dedicated GameTest server (`gameTest` run config). `./gradlew test` is **not** Minecraft-free JUnit: with `enableGameTests = true`, Loom wires `test.dependsOn(runGameTest)`, so `test` runs JUnit 5 (`src/test/java`) **and** the GameTest server.

`runGameTest` executes the `@GameTest` methods compiled into this jar. The table below is the v1 acceptance **contract** from [`DESIGN.md`](DESIGN.md) PR 13, not a list of methods that command always runs. Feature rows are implemented with their features.

### Acceptance list

| Test | Expectation |
|---|---|
| Gamerule off | No-op. `WorldGate.isActive` / modules false when `tougher:enabled` is false. |
| First-apply | Overworld gamerule matches TOML `enabledByDefault`. Nether first-apply does not rewrite the gamerule. Dimension stamps are independent. |
| Hardened stone | Wooden pick cannot harvest (speed 0, no drop). Iron pick consumed after **128** stone. Tuff is hardened. |
| Ore / piston | Placing ore next to stone denied. Pistons cannot push hardened blocks or cave-in ores. |
| Cave-ins | `coal_ore` softens stone → cobble. Deep Dark biome skip. Ancient City skip. Trial Chamber skip. 16-block copper blob: no tick timeout, `physics_dropped == 0` on a quiet world. |
| Torches | Deny below Y=0. Redstone torches allowed. Covered torch survives rain. |
| Water | Placed fluid is **not** a source (`LEVEL≠0`, first write `LEVEL=1`). Waterlogged slab is not deleted. |
| Zombies | Reanimate 3–8s. On-fire no. **Villager no.** **Reinforcement no.** Reanimate count copied. Skull break cancels. |
| Limited building | Jump-place under feet cancelled. |
| Spawn replace | Unload/reload chunk does not re-roll (`ehm:spawn_processed` stays). |
| Trees | Oak falls. 2×2 jungle falls. Player-built log pillar + nearby leaf **does not**. |
| Anti-grinder | Mob on tuff still drops (tuff is natural). Mob on glass does not. |
| Deep Dark | No near-bedrock blaze replace. |
| TNT | Recipe result count 3. |
| Physics overflow | Queue drop metric increments, no watchdog. |

Villager-trade nerf is **not** in this suite.

## Tutorial

Instant denies (torch, pillaring, ore-next-to-stone, hardened-stone help, End building) use the **action bar**. First-time mechanics use a **SystemToast**, at most `tutorial.maxShows` times per player (default 3). `tutorial.maxShows = 0` disables **all** tutorial toasts, including once-only extras (weight, zombie slow, melon seeds). Dragon challenge/defeat still **broadcast chat** and also show a once-toast when toasts are enabled. Shown counts persist on the player as `tougher:tutorial`. Silent permission nodes from the original plugin.yml still work when `bypassing.checkPermission` is true: `tougher.silent.stone_mining_help`, `no_placing_ore_against_stone`, `realistic_building`, `limited_torch_placement`, `no_torches_here`. `/tougher set tutorial false` turns mechanic toasts off; action-bar denies and the overworld first-apply toast still fire.

Dedicated servers do not load client extras. Optional **Cloth Config** + **Mod Menu** add a config screen when those mods are installed (26.3 artifacts `cloth-config-fabric` 26.3.158 and Mod Menu 21.0.0-beta.1). The published jar does not bundle them.

## Optional villager trade nerf (default on)

Not original Extra Hard Mode. `/tougher set villager_nerf true` (gamerule must also be on) removes villager trades whose **result** is diamond or netherite sword/tools/armor. Librarian Mending books are stripped from novice and apprentice; a Master librarian may still sell Mending at **2×** emeralds (20 + a book). Farmer crop trades, cartographer maps, and copper-age trades are left alone. Missing `modules.tougher.villager_nerf` is **true**.

## Credits

- Ryan "BigScary" Hamshire (original)
- Diemex
- RoboMWM
- Mitsugaru
- Grok Design (26.3 Fabric port)

---
name: tougher
description: >
  Build, test, deploy, and document the Tougher Minecraft mod (package
  dev.extrahardmode, mod id tougher). Use when working in this repo, changing
  gameplay, running Gradle, copying the jar, restarting the Hostinger server,
  updating docs/Tougher.pdf, or pushing to origin main. Use when the user runs
  /tougher.
---

# Tougher

Local jar copy is in `Agents.md`. This skill is the rest of the standing setup.

## Shell and build

- Set `JAVA_HOME` to `C:\Users\tim\.jdks\jdk-25.0.4.1+1` on every command. It does not persist between calls.
- PowerShell has no `&&`. Chain with `;`.
- Minecraft 26.3, Fabric Loom 1.17.21, Java 25. Loom does not remap. Use `assemble`. There is no `remapJar`.
- Tests: `.\gradlew.bat test --exclude-task runGameTest`, or add `--tests` for one class. Do not run `runGameTest`.
- A Gradle command often moves to the background when the wrapper timeout hits, even if a longer timeout was set. Read that task's output when it finishes. Do not poll it.

## Where the jar goes

- `assemble` runs `deployJar`, which copies only `build/libs/tougher-*.jar` (never the sources jar) into the two local mods folders named in `Agents.md`, replacing older `tougher-*.jar` and `extrahardmode-*.jar` files.
- Leave the local dedicated server stopped. Do not start it.
- The live server is Hostinger, systemd unit `minecraft.service`, files in `/opt/minecraft/server`, mods in `/opt/minecraft/server/mods`, game port 57674. Copy and restart it only when nobody has joined since the current boot.
- SSH: `ssh -o BatchMode=yes -o ConnectTimeout=20 -i $env:USERPROFILE\.ssh\id_rsa root@srv1977903.hstgr.cloud`
- Install the jar as `minecraft:minecraft` mode `644`. PowerShell strips quotes inside the remote command, so remote `grep` patterns must not need spaces (`joined`, `Done`, `Tougher`, `For.help`).
- `joined` count `0` in `logs/latest.log` means the boot is empty. `Tougher loaded` is printed before the server is ready. A new boot is confirmed by `systemctl show minecraft.service -p ActiveEnterTimestamp` plus a later `Done` line.
- Never print the RCON password, the management secret, or anything from `server.properties`. A Lithium warning that the NaturalSpawner mixin did not apply is already known.

## Git and the guide

- Branch `extra-hard-mode` tracks `origin/main`. Push only when asked, with `git push origin HEAD:main` to `https://github.com/timkeller2/extra-hard-mode.git`. Do not commit unless asked.
- Commit subjects already in the log look like `feat:`, `fix:`, and `docs:`.
- Player guide source is `docs/build_tougher_guide.py`. It writes `docs/Tougher.pdf`. Regenerate it when a player-facing rule changes.
- Latest guide, always the file on `main`: `https://github.com/timkeller2/extra-hard-mode/raw/main/docs/Tougher.pdf`
- Browser copy: `https://github.com/timkeller2/extra-hard-mode/blob/main/docs/Tougher.pdf`

## Help text

- In-game help lines are at most 180 characters. An ability index line and its lang key must stay paired, and an `en_us.json` value must match the fallback string exactly when that key exists.
- Append a new ability id. Do not insert one in the middle of the ability list. Wise teachers already in the world keep the ability stored on them.

---
name: commit
description: >
  Commit the Tougher working tree, push extra-hard-mode to origin/main, and
  update the Hostinger and Witherfen servers when they have no players.
  Use when the user says "Commit", "commit and push", or "commit and update
  the servers", or runs /commit.
---

# Commit

"Commit" means the whole sequence: commit, push `origin/main`, then update both live servers. Stop after the push only when the user says to leave the servers alone.

Hosts, `JAVA_HOME`, the no-password rule, eased world settings, and the restart policy live in the tougher skill. Read that skill before the SSH steps. Do not print an RCON password, a management secret, or `server.properties`.

## 1. Build

If gameplay changed after the last successful `test assemble`, run this before the commit:

```powershell
$env:JAVA_HOME = 'C:\Users\tim\.jdks\jdk-25.0.4.1+1'; .\gradlew.bat test assemble --exclude-task runGameTest
```

Do not run `runGameTest`. The jar to upload is `build\libs\tougher-0.1.0+26.3.jar`. `assemble` already copied it locally. Leave the local dedicated server stopped.

## 2. Commit and push

Review `git status --short`, `git diff --no-ext-diff`, and `git log -5 --format=%B`. Commit the intended files. Skip secrets and build output.

Write the message to a temp file with UTF-8 and no BOM, then `git commit -F` that file and delete it. Subjects use `feat:`, `fix:`, or `docs:`. A here-string on PowerShell is fine. Convert the message to LF before `commit -F`.

Push with `git push origin HEAD:main`. Do not poll the GitHub release unless the user asks.

If the tree is clean and the branch is already on `origin/main`, skip to the servers. If it is clean but ahead, push, then update the servers.

## 3. Count players

Write the two scripts below to `%TEMP%`, convert them to LF UTF-8 with no BOM, and `scp` them. Run each one, then delete it on the host and locally. A count above 0 skips that host. Do not kick.

Hostinger:

```powershell
scp -o BatchMode=yes -o ConnectTimeout=20 -i $env:USERPROFILE\.ssh\id_rsa "$env:TEMP\tougher-rcon-hostinger.py" root@srv1977903.hstgr.cloud:/tmp/tougher-rcon-hostinger.py
ssh -o BatchMode=yes -o ConnectTimeout=20 -i $env:USERPROFILE\.ssh\id_rsa root@srv1977903.hstgr.cloud "python3 /tmp/tougher-rcon-hostinger.py; rm -f /tmp/tougher-rcon-hostinger.py"
```

The Hostinger script reads `rcon.password` from `/opt/minecraft/server/server.properties` and prints only `PLAYERS n/max`.

Witherfen uses port 28920 and reads `MCRCON_PASS` from `/opt/minecraft/minecraft.env`. Same packet code. Print only `PLAYERS n/max`.

```powershell
scp -o BatchMode=yes -o ConnectTimeout=20 -P 28920 -i $env:USERPROFILE\.ssh\id_rsa "$env:TEMP\tougher-rcon-witherfen.py" root@partycentral.tplinkdns.com:/tmp/tougher-rcon-witherfen.py
ssh -o BatchMode=yes -o ConnectTimeout=20 -p 28920 -i $env:USERPROFILE\.ssh\id_rsa root@partycentral.tplinkdns.com "python3 /tmp/tougher-rcon-witherfen.py; rm -f /tmp/tougher-rcon-witherfen.py"
```

If Hostinger SSH fails, update Witherfen and say Hostinger was skipped.

Packet script, shared by both files. Change only the password load.

```python
#!/usr/bin/env python3
import re
import socket
import struct
from pathlib import Path

password = ""
# Hostinger: line.startswith("rcon.password=") in /opt/minecraft/server/server.properties
# Witherfen: stripped.startswith("MCRCON_PASS=") in /opt/minecraft/minecraft.env, then strip quotes
for line in Path("REPLACE").read_text(errors="replace").splitlines():
    if line.startswith("REPLACE="):
        password = line.split("=", 1)[1].strip().strip('"').strip("'")
        break

def packet(request_id, packet_type, payload):
    body = struct.pack("<ii", request_id, packet_type) + payload.encode("utf-8") + b"\x00\x00"
    return struct.pack("<i", len(body)) + body

def recv_packet(sock):
    header = b""
    while len(header) < 4:
        chunk = sock.recv(4 - len(header))
        if not chunk:
            raise SystemExit(1)
        header += chunk
    length = struct.unpack("<i", header)[0]
    data = b""
    while len(data) < length:
        chunk = sock.recv(length - len(data))
        if not chunk:
            raise SystemExit(1)
        data += chunk
    request_id, packet_type = struct.unpack("<ii", data[:8])
    payload = data[8:-2].decode("utf-8", "replace")
    return request_id, packet_type, payload

sock = socket.create_connection(("127.0.0.1", 25575), timeout=10)
try:
    sock.sendall(packet(1, 3, password))
    request_id, _packet_type, _payload = recv_packet(sock)
    if request_id == -1:
        raise SystemExit(1)
    sock.sendall(packet(2, 2, "list"))
    sock.settimeout(5)
    parts = []
    while True:
        try:
            _request_id, _packet_type, payload = recv_packet(sock)
        except (socket.timeout, TimeoutError):
            break
        parts.append(payload)
finally:
    sock.close()

match = re.search(r"There are (\d+) of a max of (\d+) players online", "".join(parts))
if not match:
    raise SystemExit(1)
print(f"PLAYERS {match.group(1)}/{match.group(2)}")
```

## 4. Install and boot

For each empty host, allow about 150 seconds for stop. Witherfen's unit fails RCON auth and then waits out its stop timeout.

```powershell
ssh ... "systemctl stop minecraft.service; systemctl is-active minecraft.service; pgrep -x java || echo NOJAVA"
```

Hostinger `is-active` reports `failed` after a stop. Witherfen may report `inactive`. Continue only when the Java line is `NOJAVA`. `pgrep -af` can match the ssh command, so use `pgrep -x java`.

Upload the jar and this install script to `/tmp/` on each empty host. Convert the script to LF first. The `scp` of two local files onto `/tmp/` keeps their base names.

```bash
#!/bin/bash
set -eu
if pgrep -x java >/dev/null; then
  echo JAVA_STILL_RUNNING
  exit 1
fi
src=/tmp/tougher-0.1.0+26.3.jar
dest=/opt/minecraft/server/mods/tougher-0.1.0+26.3.jar
install -o minecraft -g minecraft -m 644 "$src" "$dest"
find /opt/minecraft/server/mods -maxdepth 1 -type f \( -name 'extrahardmode-*.jar' -o -name 'tougher-*.jar' \) ! -name 'tougher-0.1.0+26.3.jar' -delete
sha256sum "$dest"
stat -c '%s %a %U:%G' "$dest"
rm -f "$src"
echo INSTALLED
ls /opt/minecraft/server/mods
```

Leave Chunky, appleskin, and voicechat on Witherfen. Do not edit world toml. Do not change eased live settings.

Start the service, wait about 45 seconds, and read `systemctl show minecraft.service -p ActiveState -p ActiveEnterTimestamp` plus the new boot's `Tougher loaded` line and a later `Done` line. Delete `/tmp/tougher-install.sh` after it runs.

## 5. Record it

Re-read before editing. Update the recent-state sections of `topics/tougher-server.md`, `topics/extra-hard-mode-repo.md`, and `topics/minecraft-client.md` with the commit, jar size, sha256, and both boot timestamps. Do not edit `MEMORY.md`.

Tell the user the commit, both boot times, and that the client must be restarted. The local dedicated server stays stopped.

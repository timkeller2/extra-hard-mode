# Tougher — agent notes

After adding or changing gameplay, always rebuild and deploy. Do not wait for the user to ask. Run `.\gradlew.bat assemble` (or `build` / `deployJar`) before finishing the turn.

That copies the remapped Tougher jar to both local installs:

- `C:\Client\minecraftserver\mods`
- `C:\Users\tim\AppData\Roaming\.minecraft\mods`

Copy only the Tougher jar, not the sources jar. Replace any previous `tougher-*.jar` or leftover `extrahardmode-*.jar` in those folders.

The server path is the `mods` directory (Fabric does not load jars from the server root). Restart the client and server to load the new jar.

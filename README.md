# Visuals Menu (Fabric 1.21.11)

Open the menu: Shift+9 (the "(" key on US layouts), or bind "Open Menu" in Controls. Zoom: hold C.

## Install (players)
1. Install Minecraft 1.21.11 + Fabric Loader 0.18.1+ (https://fabricmc.net/use/installer/).
2. Put Fabric API (1.21.11) and `visualsmod-1.0.0.jar` in `.minecraft/mods`.
3. Launch the Fabric 1.21.11 profile.

## Build the jar
**Option A, no install:** push this folder to a GitHub repo -> Actions tab -> "Build mod jar" -> download the `visualsmod-jar` artifact.

**Option B, local:** install JDK 21 and Gradle 9.2+, then run `gradle build`. Jar is in `build/libs/visualsmod-1.0.0.jar` (not the `-sources` one).

If Gradle can't resolve a version in gradle.properties, check https://fabricmc.net/develop.

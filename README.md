# Decked Out Card Callout mod - source

`src/` contains the Java source of every class added or replaced in this mod (hud/*, the replaced
CardQueueManager, AudioEvent, BasicHttpServer and ServerPortCommand). All other classes in the jar are
unmodified from the original Decked Out OBS mod by Meza: https://github.com/meza/decked-out-obs (LGPL-3.0).

To build, drop `src/` over the original project's `src/main/java` and build with Gradle/Loom
for Minecraft 1.21.11. Resources: card textures in `assets/decked-out-obs/textures/cards/` are the original
card PNGs lowercased and scaled to 256x367; fabric.mod.json adds the `modmenu` and extra client entrypoints.

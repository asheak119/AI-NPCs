# Project Standards & Instructions

- **Target Minecraft version:** 1.20.1
- **Mod Loader:** MinecraftForge
- **Java version:** 17

## Threading Rule
All external network/LLM calls must run asynchronously off the game thread (e.g., using `CompletableFuture`).
Any game-state mutations (updating quest data, spawning items, mutating mob goals or attributes, or sending messages to players) must be synced back onto the main server thread using `MinecraftServer.execute()` or `Level.getServer().submit()`.

## Dependencies
- Ensure the project uses Java 17 toolchain settings.
- Use native `java.net.http.HttpClient` or `com.google.gson.Gson` (via `net.minecraft.util.GsonHelper`) for JSON serialization/deserialization.
- Mixin support must be enabled in `build.gradle` and referenced if needed.

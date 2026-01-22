# Hytale Server API Reference

This directory contains documentation for the decompiled Hytale Server API.

> **Important:** Before diving into the API, read [Common Pitfalls](./common-pitfalls.md) to avoid frequent mistakes with chunk lookups, commands, and block state management.

## Source Location

The decompiled source is located at:
```
D:\source\hytale\tools\decompiled-server\
```

## Key Packages

### Plugin System
`com.hypixel.hytale.server.core.plugin`

| Class | Description |
|-------|-------------|
| `JavaPlugin` | Base class for all plugins |
| `JavaPluginInit` | Initialization data for plugins |
| `PluginBase` | Abstract plugin foundation |
| `PluginManager` | Manages plugin lifecycle |

### Event System
`com.hypixel.hytale.server.core.event`

| Interface | Description |
|-----------|-------------|
| `IEvent` | Base event interface |
| `IAsyncEvent` | Async event marker |
| `ICancellable` | Cancellable event marker |

Common events:
- `PlayerConnectEvent`
- `PlayerDisconnectEvent`
- `ChatEvent`
- `ChunkPreLoadProcessEvent`

### Command System
`com.hypixel.hytale.server.core.command`

| Class | Description |
|-------|-------------|
| `CommandBase` | Base class for commands |
| `CommandContext` | Execution context |
| `CommandRegistry` | Command registration |
| `ArgTypes` | Argument type definitions (`INTEGER`, `STRING`, `DOUBLE`, etc.) |
| `RequiredArg<T>` | Required argument wrapper |
| `OptionalArg<T>` | Optional argument wrapper |

> **Note:** The command argument system uses `withRequiredArg()`/`withOptionalArg()` and `ctx.get()`. See [Common Pitfalls](./common-pitfalls.md#command-system) for correct usage.

### Registries
`com.hypixel.hytale.server.core.registry`

| Registry | Purpose |
|----------|---------|
| `CommandRegistry` | Register commands |
| `EventRegistry` | Register event listeners |
| `AssetRegistry` | Register custom assets |
| `BlockStateRegistry` | Register block states |
| `EntityRegistry` | Register entity types |
| `TaskRegistry` | Schedule tasks |

### Game Logic
`com.hypixel.hytale.server.game`

Core game mechanics and systems.

### World System
`com.hypixel.hytale.server.world`

World and chunk management.

### Entity System (ECS)
`com.hypixel.hytale.server.entity`

Entity-Component-System architecture.

### Network Protocol
`com.hypixel.hytale.server.network`

QUIC-based network protocol with 268+ packet types.

## Exploring the API

### Search for Classes

```powershell
# Find all event classes
Get-ChildItem -Recurse "D:\source\hytale\tools\decompiled-server" -Filter "*Event.java"

# Find plugin-related classes
Get-ChildItem -Recurse "D:\source\hytale\tools\decompiled-server\com\hypixel\hytale\server\core\plugin"
```

### Search for Methods

```powershell
# Find all methods containing "register"
Select-String -Path "D:\source\hytale\tools\decompiled-server\**\*.java" -Pattern "public.*register"
```

## Common Patterns

### Getting a Registry

```java
// In your plugin's setup() method
CommandRegistry commands = getCommandRegistry();
EventRegistry events = getEventRegistry();
AssetRegistry assets = getAssetRegistry();
```

### Registering Events

```java
// Player events use registerGlobal (server-wide events)
getEventRegistry().registerGlobal(PlayerConnectEvent.class, event -> {
    PlayerRef playerRef = event.getPlayerRef();
    if (playerRef != null) {
        // Handle event using playerRef.getUuid(), playerRef.sendMessage(), etc.
    }
});

// World events also use registerGlobal
getEventRegistry().registerGlobal(AddWorldEvent.class, event -> {
    World world = event.getWorld();
    // Handle world added
});
```

> **Important:** ECS events like `PlaceBlockEvent` and `BreakBlockEvent` require `EntityEventSystem` integration. See [Common Pitfalls](./common-pitfalls.md#ecs-events) for details.

### Creating Tasks

```java
// Delayed task
getTaskRegistry().runLater(() -> {
    // Execute after delay
}, 20); // 20 ticks = 1 second

// Repeating task
getTaskRegistry().runRepeating(() -> {
    // Execute repeatedly
}, 0, 20); // Every second
```

## External Documentation

- [Official Modding Docs](https://britakee-studios.gitbook.io/hytale-modding-documentation)
- [HytaleDocs API Reference](https://hytale-docs.com/docs/api/server-internals)
- [Community GitHub](https://github.com/HytaleModding)

## Notes

- The server JAR is **not obfuscated** as of Early Access
- API may change frequently during Early Access
- Official source code release planned 1-2 months post-launch
- `BlockState` is deprecated but still functional (as of Jan 2026)
- `Player.getPlayerRef()` is deprecated but still functional (as of Jan 2026)
- Many APIs differ from Minecraft/Bukkit patterns - always check decompiled source

## Additional Resources

- [Common Pitfalls](./common-pitfalls.md) - Avoid frequent API mistakes
- [First Plugin Guide](../guides/first-plugin.md) - Step-by-step tutorial

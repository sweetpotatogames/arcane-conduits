# Creating Your First Hytale Plugin

This guide walks you through creating a plugin from scratch.

## Plugin Structure

A Hytale plugin has this structure:

```
my-plugin/
├── build.gradle           # Build configuration
├── settings.gradle        # Project name
├── gradle.properties      # Version and group
├── src/main/
│   ├── java/              # Java source code
│   │   └── com/yourname/myplugin/
│   │       ├── MyPlugin.java        # Main entry point
│   │       └── commands/            # Custom commands
│   └── resources/
│       ├── manifest.json  # Plugin metadata
│       ├── Common/        # Shared assets
│       └── Server/        # Server-only data
└── run/                   # Server runtime (generated)
```

## The Main Plugin Class

Every plugin extends `JavaPlugin`:

```java
package com.yourname.myplugin;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;

public class MyPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public MyPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Plugin constructed: " + getName());
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up " + getName());

        // Register commands
        getCommandRegistry().registerCommand(new HelloCommand());

        // Register event listeners (use registerGlobal for player events)
        getEventRegistry().registerGlobal(PlayerConnectEvent.class, this::onPlayerConnect);
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log(getName() + " started!");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log(getName() + " shutting down...");
    }

    private void onPlayerConnect(PlayerConnectEvent event) {
        // Use getPlayerRef() to get the player reference
        PlayerRef playerRef = event.getPlayerRef();
        if (playerRef != null) {
            LOGGER.atInfo().log("Player connected: " + playerRef.getUuid());
        }
    }
}
```

## Plugin Lifecycle

1. **Constructor** - Called when plugin is instantiated
2. **preLoad()** - Async config loading (optional)
3. **setup()** - Register commands, events, assets
4. **start()** - Post-initialization logic
5. **shutdown()** - Cleanup before unload

## Creating Commands

```java
package com.yourname.myplugin.commands;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;

public class HelloCommand extends CommandBase {

    public HelloCommand() {
        super("hello", "Says hello to the player");
        setPermissionGroup(GameMode.Adventure); // Anyone can use
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        String playerName = ctx.getSource().getName();
        ctx.sendMessage(Message.raw("Hello, " + playerName + "!"));
    }
}
```

### Command with Arguments

Use `withRequiredArg()` or `withOptionalArg()` to define arguments, and `ctx.get()` to retrieve values:

```java
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;

public class TeleportCommand extends CommandBase {

    private final RequiredArg<Double> xArg;
    private final RequiredArg<Double> yArg;
    private final RequiredArg<Double> zArg;

    public TeleportCommand() {
        super("tp", "Teleport to coordinates");
        setPermissionGroup(GameMode.Creative); // Only Creative mode

        // Define typed arguments
        xArg = withRequiredArg("x", "X coordinate", ArgTypes.DOUBLE);
        yArg = withRequiredArg("y", "Y coordinate", ArgTypes.DOUBLE);
        zArg = withRequiredArg("z", "Z coordinate", ArgTypes.DOUBLE);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        // Must be a player to teleport
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("This command must be run by a player"));
            return;
        }

        double x = ctx.get(xArg);
        double y = ctx.get(yArg);
        double z = ctx.get(zArg);

        // Get player and teleport
        Player player = ctx.senderAs(Player.class);
        player.teleport(x, y, z);
        ctx.sendMessage(Message.raw("Teleported to " + x + ", " + y + ", " + z));
    }
}
```

> **Note:** See [Common API Pitfalls](../api/common-pitfalls.md) for more details on the command system.

## Registering Event Listeners

```java
@Override
protected void setup() {
    // Player events use registerGlobal (they're server-wide)
    getEventRegistry().registerGlobal(PlayerConnectEvent.class, event -> {
        PlayerRef playerRef = event.getPlayerRef();
        if (playerRef != null) {
            LOGGER.atInfo().log("Player joined: " + playerRef.getUuid());
        }
    });

    // Method reference
    getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onDisconnect);

    // World events also use registerGlobal
    getEventRegistry().registerGlobal(AddWorldEvent.class, this::onWorldAdd);
}

private void onDisconnect(PlayerDisconnectEvent event) {
    PlayerRef playerRef = event.getPlayerRef();
    if (playerRef != null) {
        LOGGER.atInfo().log("Player left: " + playerRef.getUuid());
    }
}
```

> **Important:** ECS events like `PlaceBlockEvent` and `BreakBlockEvent` require a different registration approach using `EntityEventSystem`. See [Common API Pitfalls](../api/common-pitfalls.md#ecs-events) for details.

## Configuration Files

```java
@Override
protected CompletableFuture<Void> preLoad() {
    // Load config asynchronously
    return withConfig("config.json", MyConfig.class)
        .thenAccept(config -> {
            this.config = config;
            LOGGER.atInfo().log("Config loaded: " + config.getSetting());
        });
}
```

## The manifest.json File

```json
{
    "Group": "com.yourname",
    "Name": "MyPlugin",
    "Version": "1.0.0",
    "Description": "My awesome plugin",
    "Authors": [
        { "Name": "Your Name" }
    ],
    "Website": "https://yoursite.com",
    "ServerVersion": ">=1.0.0",
    "Dependencies": {
        "SomeOtherPlugin": ">=1.0.0"
    },
    "OptionalDependencies": {
        "OptionalPlugin": "*"
    },
    "Main": "com.yourname.myplugin.MyPlugin",
    "IncludesAssetPack": true
}
```

## Building and Distributing

### Build the JAR

```bash
./gradlew build
```

Output: `build/libs/my-plugin-1.0.0.jar`

### Deploy to Mods Folder

The template includes tasks to automatically copy your plugin to the Hytale Mods folder:

```bash
# Deploy without version number (recommended for development)
./gradlew deployPlugin

# Deploy with version number in filename
./gradlew deployPluginVersioned
```

The `deployPlugin` task:
- Builds the project first
- Removes any previous versions of your plugin from the Mods folder
- Copies the JAR as `my-plugin.jar` (no version suffix)

### Run the Server

Launch the Hytale server with your plugin loaded:

```bash
# Run server with plugin from source (development mode)
./gradlew runServer

# Deploy plugin then run server (loads from Mods folder)
./gradlew runServerDeployed
```

The `runServer` task:
- Builds the project first
- Loads your plugin directly from the source directory
- Enables console input for server commands
- Use `Ctrl+C` to stop the server

The `runServerDeployed` task:
- Deploys the plugin to the Mods folder first
- Runs the server loading all mods from the Mods folder
- Better simulates production environment

> **Note:** Remember to run `auth login device` in the server console the first time to authenticate.

### Manual Install Location

If you prefer to copy manually:
- Windows: `%appdata%/Hytale/UserData/Mods/`
- Linux: `~/.local/share/Hytale/UserData/Mods/`
- macOS: `~/Library/Application Support/Hytale/UserData/Mods/`

## Debugging Tips

1. **Use the Logger**
   ```java
   LOGGER.atInfo().log("Debug message");
   LOGGER.atWarning().log("Warning!");
   LOGGER.atSevere().log("Error occurred");
   ```

2. **Enable Debug Mode**
   - Click the Bug icon instead of Play in IntelliJ
   - Set breakpoints in the left margin

3. **Hot Reload**
   - Press `Ctrl+F9` to rebuild
   - Some changes require server restart

## Common Issues

| Problem | Solution |
|---------|----------|
| Plugin not loading | Check `manifest.json` "Main" class path |
| Command not found | Verify `registerCommand()` in `setup()` |
| Events not firing | Check event class name and use `registerGlobal()` for player events |
| Connection refused | Run `auth login device` in server console |
| `getDataFolder()` not found | Use `getDataDirectory()` instead |
| `sendChatMessage()` not found | Use `sendMessage(Message.raw(...))` |
| `getTranslation()` not found | Use `getPosition()` on TransformComponent |
| ECS event has no `getEntityRef()` | Use `EntityEventSystem` integration |

> **See Also:** [Common API Pitfalls](../api/common-pitfalls.md) - Comprehensive list of API corrections and correct patterns.

## Next Steps

- Explore the decompiled API in `tools/decompiled-server/`
- Check vanilla assets for examples in `assets/vanilla/`
- Read [advanced patterns](https://britakee-studios.gitbook.io/hytale-modding-documentation/plugins-java-development/12-advanced-plugin-patterns)

# Hytale Modding Development Hub

Local development environment for Hytale plugin and mod development.

> **For AI Assistants:** See [CLAUDE.md](./CLAUDE.md) for detailed project navigation instructions.

## Quick Start

### Prerequisites
- **Java 25** (recommended) or Java 17+
- **IntelliJ IDEA** (Community Edition works)
- **Hytale** installed via official launcher
- **Git** for version control

### Project Structure

```
hytale/
├── docs/                    # Documentation
│   ├── api/                 # Decompiled API reference
│   ├── guides/              # Development guides
│   ├── examples/            # Code examples
│   └── assets/              # Asset documentation
├── tools/                   # Development utilities
│   ├── decompiled-server/   # Decompiled HytaleServer.jar
│   ├── scripts/             # Automation scripts
│   └── vineflower-*.jar     # Decompiler
├── templates/               # Project templates
│   └── plugin-template/     # Official plugin template
├── assets/                  # Game assets
│   ├── vanilla/             # Extracted vanilla assets
│   ├── docs/                # Asset schema documentation
│   └── custom/              # Your custom assets
├── plugins/                 # Your plugin projects
└── packs/                   # Your content packs
```

## Creating a Plugin

1. Copy `templates/plugin-template/` to `plugins/your-plugin-name/`
2. Update `settings.gradle` with your project name
3. Update `gradle.properties` with your details
4. Update `src/main/resources/manifest.json`
5. Open in IntelliJ IDEA and let Gradle sync
6. Run the `HytaleServer` configuration

```java
package com.example.myplugin;

import com.hypixel.hytale.plugin.JavaPlugin;
import com.hypixel.hytale.plugin.JavaPluginInit;

public class MyPlugin extends JavaPlugin {
    public MyPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    public void setup() {
        getLogger().info("MyPlugin setting up!");

        // Register events
        getEventRegistry().register(PlayerConnectEvent.class, event -> {
            getLogger().info("Player connected: " + event.getPlayer().getName());
        });

        // Register commands
        getCommandRegistry().registerCommand(new MyCommand());
    }

    @Override
    public void start() {
        getLogger().info("MyPlugin started!");
    }
}
```

## Creating a Pack

1. Copy `packs/example-pack/` to `packs/your-pack-name/`
2. Update `manifest.json` with your pack details
3. Add assets to `Common/` and `Server/` directories
4. Use the in-game Asset Editor or Blockbench for models

### Pack Structure
```
your-pack/
├── manifest.json
├── Common/
│   ├── Blocks/      # Block definitions (JSON)
│   ├── Items/       # Item definitions (JSON)
│   ├── Models/      # Blockbench models (.hym)
│   └── Textures/    # Texture files (.png)
└── Server/
    └── Data/        # Server-side data
```

## API Reference

The decompiled server source is in `tools/decompiled-server/`. Key packages:

| Package | Description |
|---------|-------------|
| `com.hypixel.hytale.server.core.plugin` | Plugin system |
| `com.hypixel.hytale.server.core.event` | Event system |
| `com.hypixel.hytale.server.core.command` | Command framework |
| `com.hypixel.hytale.server.core.registry` | All registries |
| `com.hypixel.hytale.server.game` | Game logic |
| `com.hypixel.hytale.server.world` | World/chunk handling |
| `com.hypixel.hytale.server.entity` | Entity system (ECS) |
| `com.hypixel.hytale.server.network` | QUIC protocol |

## Plugin Lifecycle

```
preLoad()   → Async configuration loading (CompletableFuture)
setup()     → Register commands, events, assets, components
start()     → Post-initialization logic
shutdown()  → Cleanup before registries reset
```

## Available Registries

- `getCommandRegistry()` - Register commands
- `getEventRegistry()` - Register event listeners
- `getAssetRegistry()` - Register custom assets
- `getBlockStateRegistry()` - Register block states
- `getEntityRegistry()` - Register entity types
- `getTaskRegistry()` - Schedule tasks

## Resources

- [Hytale Modding Docs](https://britakee-studios.gitbook.io/hytale-modding-documentation)
- [HytaleDocs Wiki](https://hytale-docs.com/)
- [Plugin Template](https://github.com/realBritakee/hytale-template-plugin)
- [HytaleModding GitHub](https://github.com/HytaleModding)
- [CurseForge](https://curseforge.com) - Mod distribution
- [Blockbench](https://www.blockbench.net) - Model creation

## Server Authentication

When running a local server for the first time:
1. Run the `HytaleServer` configuration
2. Execute `auth login device` in the terminal
3. Open the provided URL and authenticate
4. Run `auth persistence Encrypted` for persistent login

Connect your Hytale client to `127.0.0.1` to test.

---

*Generated: January 2026 | Hytale Early Access*

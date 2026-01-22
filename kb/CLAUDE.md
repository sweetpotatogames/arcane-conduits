# Claude Agent Instructions - Hytale Modding Hub

This file provides context for Claude agents working on this Hytale modding project.

## Project Overview

This is a **Hytale plugin and mod development environment** containing:
- Decompiled server API source code
- Extracted vanilla game assets
- Plugin and Pack templates
- Local documentation

Hytale is in **Early Access** (January 2026). The modding API may change frequently.

## Directory Structure

```
D:\source\hytale\
├── docs/                          # Documentation
│   ├── api/                       # API reference docs
│   ├── guides/                    # Development tutorials
│   │   ├── getting-started.md    # Setup instructions
│   │   └── first-plugin.md       # Plugin development guide
│   ├── examples/                  # Code examples
│   └── assets/                    # Asset documentation
├── tools/                         # Development tools
│   ├── decompiled-server/         # DECOMPILED SERVER SOURCE (15k+ Java files)
│   │   └── com/hypixel/hytale/    # Hytale API classes
│   ├── scripts/                   # Utility scripts
│   │   ├── decompile.ps1         # Re-decompile server JAR
│   │   └── extract-assets.ps1    # Re-extract assets
│   └── vineflower-1.11.2.jar     # Java decompiler
├── templates/                     # Project templates
│   └── plugin-template/           # Official Gradle plugin template
├── assets/                        # Game assets
│   ├── vanilla/                   # EXTRACTED VANILLA ASSETS (65k+ files)
│   │   ├── Common/               # Shared assets (Blocks, Items, Models, etc.)
│   │   ├── Server/               # Server-only data
│   │   └── manifest.json         # Asset manifest
│   ├── docs/                      # Asset schema documentation
│   └── custom/                    # User's custom assets
├── plugins/                       # User's plugin projects
├── packs/                         # User's content packs
│   └── example-pack/             # Pack template with manifest.json
└── README.md                      # Project overview
```

## Key Locations for Code Reference

### Decompiled Server API
**Location:** `D:\source\hytale\tools\decompiled-server\`

This contains the **full decompiled HytaleServer.jar** - use this to understand how the API works.

**Important packages:**
| Package | Path | Purpose |
|---------|------|---------|
| Plugin System | `com/hypixel/hytale/server/core/plugin/` | `JavaPlugin`, `PluginBase`, `PluginManager` |
| Events | `com/hypixel/hytale/server/core/event/` | Event interfaces and handlers |
| Commands | `com/hypixel/hytale/server/core/command/` | `CommandBase`, command registration |
| Registries | `com/hypixel/hytale/server/core/registry/` | All game registries |
| Entities | `com/hypixel/hytale/server/core/entity/` | Entity component system |
| World/Chunks | `com/hypixel/hytale/server/core/universe/` | World management |
| Assets | `com/hypixel/hytale/server/core/asset/` | Asset types and loading |
| Blocks | `com/hypixel/hytale/server/core/blocktype/` | Block definitions |

### Plugin Template
**Location:** `D:\source\hytale\templates\plugin-template\`

Copy this to create new plugins. Key files:
- `src/main/java/org/example/plugin/ExamplePlugin.java` - Main class example
- `src/main/java/org/example/plugin/ExampleCommand.java` - Command example
- `src/main/resources/manifest.json` - Plugin metadata (MUST update "Main" class path)
- `build.gradle` - Build configuration
- `settings.gradle` - Project name (update before opening in IDE)

### Vanilla Assets
**Location:** `D:\source\hytale\assets\vanilla\`

Use these as reference for creating custom content:
- `Common/Blocks/` - Block definitions (JSON)
- `Common/Items/` - Item definitions (JSON)
- `Common/NPC/` - NPC/entity definitions
- `Common/UI/` - UI layouts
- `Server/` - Server-side data

## Hytale Plugin Development Quick Reference

### Plugin Lifecycle
```java
public class MyPlugin extends JavaPlugin {
    public MyPlugin(JavaPluginInit init) { super(init); }  // Constructor
    protected void setup() { }   // Register commands, events, assets
    protected void start() { }   // Post-initialization
    protected void shutdown() { } // Cleanup
}
```

### Key APIs
```java
// In setup() method:
getCommandRegistry().registerCommand(new MyCommand());
getEventRegistry().register(PlayerConnectEvent.class, this::onConnect);
getLogger().atInfo().log("Message");
```

### manifest.json Required Fields
```json
{
    "Group": "com.example",
    "Name": "PluginName",
    "Version": "1.0.0",
    "Main": "com.example.pluginname.MainClass"  // MUST match actual class path
}
```

## Server Files Location

The original server files are at:
```
D:\games\hytale-downloader\
├── 2026.01.17-4b0f30090.zip      # Latest server build
├── latest-extract\
│   ├── Server\HytaleServer.jar   # Server JAR (source of decompiled code)
│   └── Assets.zip                # Game assets (already extracted)
└── QUICKSTART.md                 # Server download tool docs
```

## When Helping Users

1. **For API questions:** Search `tools/decompiled-server/com/hypixel/hytale/`
2. **For asset examples:** Check `assets/vanilla/Common/` for JSON schemas
3. **For plugin structure:** Reference `templates/plugin-template/`
4. **For tutorials:** Read `docs/guides/`

## External Resources

- [Hytale Modding Docs](https://britakee-studios.gitbook.io/hytale-modding-documentation)
- [HytaleDocs API Wiki](https://hytale-docs.com/)
- [Plugin Template GitHub](https://github.com/realBritakee/hytale-template-plugin)
- [HytaleModding Community](https://github.com/HytaleModding)

## Notes

- Server JAR is **not obfuscated** - decompiled source is clean and readable
- Java 25 is recommended (Java 17+ works for development)
- Plugins install to `%appdata%/Hytale/UserData/Mods/`
- Local server requires authentication via `auth login device` command

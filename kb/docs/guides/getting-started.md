# Getting Started with Hytale Modding

## Prerequisites

1. **Java 25** (or Java 17+)
   - Download from [OpenJDK](https://openjdk.org/) or Oracle
   - Set `JAVA_HOME` environment variable

2. **IntelliJ IDEA**
   - Download [Community Edition](https://www.jetbrains.com/idea/download/) (free)
   - Install the Gradle plugin (included by default)

3. **Hytale Game**
   - Install via the official Hytale Launcher
   - You need a valid Hytale account

4. **Git** (optional but recommended)
   - Download from [git-scm.com](https://git-scm.com/)

## Project Layout

```
D:\source\hytale\
├── docs/                    # This documentation
├── tools/
│   ├── decompiled-server/   # Decompiled API source
│   └── scripts/             # Utility scripts
├── templates/
│   └── plugin-template/     # Official template
├── assets/
│   └── vanilla/             # Extracted game assets
├── plugins/                 # Your plugin projects
└── packs/                   # Your content packs
```

## Quick Start: Create Your First Plugin

### 1. Copy the Template

```powershell
Copy-Item -Recurse "D:\source\hytale\templates\plugin-template" "D:\source\hytale\plugins\my-plugin"
```

### 2. Configure the Project

Edit `plugins/my-plugin/settings.gradle`:
```gradle
rootProject.name = 'MyPlugin'
```

Edit `plugins/my-plugin/gradle.properties`:
```properties
maven_group = com.yourname
version = 1.0.0
```

Edit `plugins/my-plugin/src/main/resources/manifest.json`:
```json
{
    "Group": "com.yourname",
    "Name": "MyPlugin",
    "Version": "1.0.0",
    "Description": "My first Hytale plugin!",
    "Authors": [{ "Name": "Your Name" }],
    "Main": "com.yourname.myplugin.MyPlugin"
}
```

### 3. Rename and Update Main Class

Rename `src/main/java/org/example/plugin/` to match your package:
```
src/main/java/com/yourname/myplugin/MyPlugin.java
```

### 4. Open in IntelliJ IDEA

1. File → Open → Select `plugins/my-plugin/`
2. Wait for Gradle sync to complete
3. The `HytaleServer` run configuration will be created automatically

### 5. Authenticate Your Server

1. Run `HytaleServer` configuration
2. In the console, type: `auth login device`
3. Open the URL shown and authenticate
4. Type: `auth persistence Encrypted`

### 6. Test Your Plugin

1. Launch your Hytale client
2. Connect to `127.0.0.1`
3. Type `/test` in-game
4. You should see: "Hello from MyPlugin v1.0.0!"

## Quick Start: Create a Content Pack

### 1. Copy the Pack Template

```powershell
Copy-Item -Recurse "D:\source\hytale\packs\example-pack" "D:\source\hytale\packs\my-pack"
```

### 2. Configure manifest.json

```json
{
    "Name": "My Pack",
    "Group": "com.yourname",
    "Version": "1.0.0",
    "Description": "My first content pack!"
}
```

### 3. Add Content

- `Common/Blocks/` - Block definitions (JSON)
- `Common/Items/` - Item definitions (JSON)
- `Common/Models/` - Blockbench models
- `Common/Textures/` - PNG textures
- `Server/Data/` - Server-side data

### 4. Install and Test

Copy your pack folder to:
```
%appdata%/Hytale/UserData/Mods/
```

Launch Hytale and your pack will be loaded.

## Exploring the API

The decompiled server source is your best reference:
```
D:\source\hytale\tools\decompiled-server\
```

Key packages to explore:
- `com/hypixel/hytale/server/core/plugin/` - Plugin base classes
- `com/hypixel/hytale/server/core/event/` - Event system
- `com/hypixel/hytale/server/core/command/` - Command framework

## Next Steps

- Read [Creating Your First Plugin](./first-plugin.md)
- Browse the [API Reference](../api/README.md)
- Explore vanilla assets in `assets/vanilla/`
- Check the [official docs](https://britakee-studios.gitbook.io/hytale-modding-documentation)

# Common API Pitfalls and Corrections

This document covers common mistakes when using the Hytale Server API, based on real issues encountered during plugin development. The decompiled API signatures don't always match what you might expect from similar game modding APIs.

> **Note:** This document was created during Early Access (January 2026). The API may change as Hytale development continues.

## Table of Contents

1. [Chunk Lookup Methods](#chunk-lookup-methods)
2. [Command System](#command-system)
3. [BlockType Methods](#blocktype-methods)
4. [ItemStack Methods](#itemstack-methods)
5. [Block Visual State Updates](#block-visual-state-updates)
6. [BlockState Deprecation](#blockstate-deprecation)
7. [BuilderCodec Patterns](#buildercodec-patterns)
8. [Plugin Base Methods](#plugin-base-methods)
9. [PlayerRef Methods](#playerref-methods)
10. [Player Component Methods](#player-component-methods)
11. [TransformComponent Methods](#transformcomponent-methods)
12. [ECS Events](#ecs-events)
13. [Teleport Component](#teleport-component)
14. [Inventory/ItemContainer System](#inventoryitemcontainer-system)

---

## Chunk Lookup Methods

### Problem

`World.getChunkIfLoaded()` does **not** accept `(int chunkX, int chunkZ)` parameters.

```java
// WRONG - This will not compile
WorldChunk chunk = world.getChunkIfLoaded(chunkX, chunkZ);
```

### Solution

Use `ChunkUtil.indexChunkFromBlock()` to convert block coordinates to a chunk index:

```java
import com.hypixel.hytale.math.util.ChunkUtil;

// CORRECT - Use ChunkUtil to get chunk index from block coordinates
long chunkIndex = ChunkUtil.indexChunkFromBlock(blockX, blockZ);
WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
```

If you have chunk coordinates (not block coordinates), use `ChunkUtil.indexChunk()`:

```java
// From chunk coordinates
long chunkIndex = ChunkUtil.indexChunk(chunkX, chunkZ);
WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
```

### Full Example

```java
private ConduitBlockState getConduitState(World world, Vector3i pos) {
    long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
    WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
    if (chunk == null) {
        return null;
    }

    // Use bitwise AND to get local chunk coordinates (0-31)
    BlockState state = chunk.getState(pos.x & 31, pos.y, pos.z & 31);
    if (state instanceof ConduitBlockState) {
        return (ConduitBlockState) state;
    }
    return null;
}
```

---

## Command System

### Problem 1: No `ctx.getArguments()` method

`CommandContext` does not have a `getArguments()` method that returns a String array.

```java
// WRONG - This method does not exist
String[] args = ctx.getArguments();
```

### Problem 2: No `ctx.getPlayer()` method

`CommandContext` does not have a `getPlayer()` method.

```java
// WRONG - This method does not exist
Player player = ctx.getPlayer();
```

### Problem 3: No `addArgument()` / `ctx.getArgument()` pattern

The argument system does not work with `addArgument()` and `ctx.getArgument()`.

```java
// WRONG - This pattern does not exist
addArgument("x", ArgumentType.DOUBLE);
double x = ctx.getArgument("x", Double.class);
```

### Solution: Use Subcommands and Typed Arguments

The correct pattern uses `withRequiredArg()` / `withOptionalArg()` to define arguments, and `ctx.get()` to retrieve values:

```java
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;

public class MyCommand extends CommandBase {
    private final RequiredArg<Integer> xArg;
    private final RequiredArg<Integer> yArg;
    private final RequiredArg<Integer> zArg;

    public MyCommand() {
        super("mycommand", "Description here");
        setPermissionGroup(GameMode.Creative);

        // Define typed arguments
        xArg = withRequiredArg("x", "X coordinate", ArgTypes.INTEGER);
        yArg = withRequiredArg("y", "Y coordinate", ArgTypes.INTEGER);
        zArg = withRequiredArg("z", "Z coordinate", ArgTypes.INTEGER);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        // Get argument values with ctx.get()
        int x = ctx.get(xArg);
        int y = ctx.get(yArg);
        int z = ctx.get(zArg);

        ctx.sendMessage(Message.raw("Coordinates: " + x + ", " + y + ", " + z));
    }
}
```

### Getting the Player

Use `ctx.isPlayer()` and `ctx.senderAs()`:

```java
@Override
protected void executeSync(@Nonnull CommandContext ctx) {
    // Check if command was run by a player
    if (!ctx.isPlayer()) {
        ctx.sendMessage(Message.raw("This command must be run by a player"));
        return;
    }

    // Get the player
    Player player = ctx.senderAs(Player.class);
    World world = player.getWorld();

    // ... rest of command logic
}
```

### Using Subcommands

For commands with multiple operations, use the subcommand pattern:

```java
public class MyPluginCommand extends CommandBase {

    public MyPluginCommand() {
        super("myplugin", "My plugin commands");
        setPermissionGroup(GameMode.Adventure);

        // Add subcommands
        addSubCommand(new StatusSubCommand());
        addSubCommand(new SetSubCommand());
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        // Show help when no subcommand provided
        ctx.sendMessage(Message.raw("Usage: /myplugin <status|set>"));
    }

    private class StatusSubCommand extends CommandBase {
        public StatusSubCommand() {
            super("status", "Show status");
            setPermissionGroup(GameMode.Adventure);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext ctx) {
            ctx.sendMessage(Message.raw("Status: OK"));
        }
    }

    private class SetSubCommand extends CommandBase {
        private final RequiredArg<String> valueArg;

        public SetSubCommand() {
            super("set", "Set a value");
            setPermissionGroup(GameMode.Creative);
            valueArg = withRequiredArg("value", "Value to set", ArgTypes.STRING);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext ctx) {
            String value = ctx.get(valueArg);
            ctx.sendMessage(Message.raw("Set value to: " + value));
        }
    }
}
```

### Available Argument Types

Common `ArgTypes` values:
- `ArgTypes.STRING` - Text strings
- `ArgTypes.INTEGER` - Integer numbers
- `ArgTypes.DOUBLE` - Floating-point numbers
- `ArgTypes.BOOLEAN` - true/false values
- `ArgTypes.PLAYER` - Player references

---

## BlockType Methods

### Problem

`BlockType.getName()` does **not** exist.

```java
// WRONG - This method does not exist
String name = blockType.getName();
```

### Solution

Use `BlockType.getId()` instead:

```java
// CORRECT
String id = blockType.getId();

// Example usage
if (blockType != null && blockType.getId().startsWith("myplugin:")) {
    // Handle custom block
}
```

---

## ItemStack Methods

### Problem 1: Wrong Package Path

`ItemStack` is **not** at `com.hypixel.hytale.server.core.inventory.item.ItemStack`.

### Solution

The correct import path:

```java
// CORRECT
import com.hypixel.hytale.server.core.inventory.ItemStack;
```

### Problem 2: No `getItemType()` method

`ItemStack.getItemType()` does **not** exist.

```java
// WRONG
String key = itemStack.getItemType().getKey();
```

### Solution

Use `getItem().getId()`:

```java
// CORRECT
import com.hypixel.hytale.server.core.inventory.ItemStack;

private boolean isMyPluginItem(ItemStack itemStack) {
    if (itemStack == null || itemStack.getItem() == null) {
        return false;
    }
    String itemId = itemStack.getItem().getId();
    return itemId != null && itemId.startsWith("myplugin:");
}
```

---

## Block Visual State Updates

### Problem

`World.getBlockAccessor()` does **not** exist. You cannot directly call `setBlockInteractionState()`.

```java
// WRONG - getBlockAccessor() does not exist
getChunk().getWorld().getBlockAccessor().setBlockInteractionState(
    getBlockX(), getBlockY(), getBlockZ(),
    getBlockType(),
    "powered",
    false
);
```

### Solution

Visual state updates should be handled through:

1. **Block State Data** - Store the state in your `BlockState` subclass and call `markNeedsSave()`
2. **Block Definition JSON** - Define visual variants in the block's JSON file that respond to state data

```java
public class MyBlockState extends BlockState {
    private int powerLevel = 0;

    public void setPowerLevel(int level) {
        if (level != this.powerLevel) {
            this.powerLevel = level;
            markNeedsSave();  // This triggers state sync to clients
        }
    }
}
```

The block definition JSON then specifies visual variants:

```json
{
    "Type": "Block",
    "Id": "myplugin:MyBlock",
    "State": {
        "Type": "MyBlockState",
        "Data": {
            "MaxPower": 15
        }
    },
    "InteractionStates": {
        "default": { "Model": "Models/MyBlock.json" },
        "powered_low": { "Model": "Models/MyBlock_Powered_Low.json" },
        "powered_high": { "Model": "Models/MyBlock_Powered_High.json" }
    }
}
```

---

## BlockState Deprecation

### Warning

`BlockState` in `com.hypixel.hytale.server.core.universe.world.meta` is marked **deprecated and for removal**.

```
warning: [removal] BlockState has been deprecated and marked for removal
```

### Current Status

As of January 2026 Early Access, `BlockState` is still functional despite the deprecation warning. Use `@SuppressWarnings("deprecation")` to suppress the warning:

```java
@SuppressWarnings("deprecation")  // BlockState is deprecated but still functional
public class MyBlockState extends BlockState {
    // ...
}
```

### Future Consideration

Monitor Hytale updates for a replacement API. The deprecation suggests Hypixel plans to replace this system, possibly with a more ECS-aligned approach.

---

## Quick Reference Table

| What You Might Try | What Actually Works |
|-------------------|---------------------|
| `world.getChunkIfLoaded(chunkX, chunkZ)` | `world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(blockX, blockZ))` |
| `ctx.getArguments()` | Use `withRequiredArg()` + `ctx.get(arg)` pattern |
| `ctx.getPlayer()` | `ctx.isPlayer()` + `ctx.senderAs(Player.class)` |
| `ctx.getArgument("name", Type.class)` | Define arg with `withRequiredArg()`, get with `ctx.get(arg)` |
| `blockType.getName()` | `blockType.getId()` |
| `itemStack.getItemType().getKey()` | `itemStack.getItem().getId()` |
| `world.getBlockAccessor().setBlockInteractionState(...)` | Use `markNeedsSave()` + JSON visual variants |

---

## BuilderCodec Patterns

### Problem 1: Wrong Import Path

`BuilderCodec` is **not** in `com.hypixel.hytale.codec`.

```java
// WRONG - Package does not exist
import com.hypixel.hytale.codec.BuilderCodec;
```

### Solution

The correct import path includes `.builder`:

```java
// CORRECT
import com.hypixel.hytale.codec.builder.BuilderCodec;
```

### Problem 2: Wrong `appendInherited()` Signature

`appendInherited()` requires **4 arguments**, not 3.

```java
// WRONG - Missing copier argument
.<Double>appendInherited(
    new KeyedCodec<>("Radius", Codec.DOUBLE),
    (o, v) -> o.radius = v,
    o -> o.radius
)
```

### Solution

Add the fourth argument (copier function):

```java
// CORRECT - 4 arguments: codec, setter, getter, copier
.<Double>appendInherited(
    new KeyedCodec<>("Radius", Codec.DOUBLE),
    (o, v) -> o.radius = v,       // setter
    o -> o.radius,                 // getter
    (o, p) -> o.radius = p.radius  // copier (from parent)
)
.add()  // Don't forget .add() to finalize
```

### Problem 3: `addDefault()` Does Not Exist

There is no `addDefault()` method on the codec builder chain.

```java
// WRONG - addDefault() does not exist
.<Double>appendInherited(...)
.add()
.addDefault(3.0);  // This will not compile
```

### Solution

Set defaults in your constructor instead:

```java
// CORRECT - Set defaults in the constructor
public class CaptureZone {
    private double radius = 3.0;  // Default value here

    public CaptureZone() {
        // Default constructor sets initial values
    }
}
```

### Full Example

```java
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.keyed.KeyedCodec;
import com.hypixel.hytale.codec.Codec;

public class MyConfig {
    private String name = "default";  // Default in field
    private int value = 100;          // Default in field

    public static final BuilderCodec<MyConfig> CODEC = BuilderCodec.builder(
            MyConfig.class, MyConfig::new
        )
        .<String>appendInherited(
            new KeyedCodec<>("Name", Codec.STRING),
            (o, v) -> o.name = v,
            o -> o.name,
            (o, p) -> o.name = p.name
        )
        .add()
        .<Integer>appendInherited(
            new KeyedCodec<>("Value", Codec.INT),
            (o, v) -> o.value = v,
            o -> o.value,
            (o, p) -> o.value = p.value
        )
        .add()
        .build();
}
```

---

## Plugin Base Methods

### Problem

`PluginBase.getDataFolder()` does **not** exist.

```java
// WRONG - This method does not exist
Path configPath = plugin.getDataFolder().resolve("config.json");
```

### Solution

Use `getDataDirectory()` instead:

```java
// CORRECT
Path configPath = plugin.getDataDirectory().resolve("config.json");
```

### Full Example

```java
import java.nio.file.Path;
import java.nio.file.Files;

public class MyPlugin extends JavaPlugin {
    private Path getConfigPath() {
        return getDataDirectory().resolve("config.json");
    }

    private void ensureConfigDir() throws IOException {
        Path dataDir = getDataDirectory();
        if (!Files.exists(dataDir)) {
            Files.createDirectories(dataDir);
        }
    }
}
```

---

## PlayerRef Methods

### Problem 1: Wrong UUID Method Name

`PlayerRef.getUUID()` does **not** exist.

```java
// WRONG - Method does not exist
UUID uuid = playerRef.getUUID();
```

### Solution

Use `getUuid()` (lowercase 'u' in 'Uuid'):

```java
// CORRECT
UUID uuid = playerRef.getUuid();
```

### Problem 2: Wrong Message Method

`PlayerRef.sendChatMessage()` does **not** exist.

```java
// WRONG - Method does not exist
playerRef.sendChatMessage("Hello!");
```

### Solution

Use `sendMessage()` with a `Message` object:

```java
// CORRECT
import com.hypixel.hytale.server.core.Message;

playerRef.sendMessage(Message.raw("Hello!"));

// With color formatting
playerRef.sendMessage(Message.raw("Error!").color("#FF0000"));

// With bold
playerRef.sendMessage(Message.raw("Important").bold(true));
```

---

## Player Component Methods

### Problem 1: `isOp()` Does Not Exist

`Player.isOp()` is **not** the correct permission check.

```java
// WRONG - isOp() does not exist
if (player.isOp()) {
    // admin action
}
```

### Solution

Use `hasPermission()` with a permission string:

```java
// CORRECT
if (player.hasPermission("myplugin.admin")) {
    // admin action
}

// Or check for a specific permission group
if (player.hasPermission("ctf.admin")) {
    // CTF admin action
}
```

### Problem 2: `getPlayerRef()` is Deprecated

`Player.getPlayerRef()` is marked **deprecated and for removal**.

```java
// WARNING - Deprecated and will be removed
PlayerRef playerRef = player.getPlayerRef();
```

### Current Solution

For now, suppress the warning:

```java
@SuppressWarnings("deprecation")
PlayerRef playerRef = player.getPlayerRef();
```

### Future Consideration

The deprecation suggests Hypixel is moving away from this pattern. Watch for alternative methods in future API updates, likely involving direct component access through the ECS system.

---

## TransformComponent Methods

### Problem

`TransformComponent.getTranslation()` does **not** exist.

```java
// WRONG - Method does not exist
Vector3d pos = transformComponent.getTranslation();
```

### Solution

Use `getPosition()` instead:

```java
// CORRECT
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;

Vector3d pos = transformComponent.getPosition();
```

### Full Example

```java
// Getting an entity's position
TransformComponent transform = commandBuffer.getComponent(
    entityRef,
    TransformComponent.getComponentType()
);
if (transform != null) {
    Vector3d position = transform.getPosition();
    Vector3f rotation = transform.getRotation();
}
```

---

## ECS Events

### Problem

ECS events like `PlaceBlockEvent`, `BreakBlockEvent`, and `DropItemEvent.PlayerRequest` do **not** have `getEntityRef()`.

```java
// WRONG - ECS events don't have getEntityRef()
plugin.getEventRegistry().register(PlaceBlockEvent.class, event -> {
    Ref<EntityStore> entity = event.getEntityRef();  // Does not exist!
});
```

### Why This Happens

Events that extend `CancellableEcsEvent` are processed through the Entity Component System, where entity context is provided differently - through system iteration rather than event methods.

### Solution

ECS events require an `EntityEventSystem` integration:

```java
// CORRECT - ECS events need EntityEventSystem
// 1. Create an EntitySystem that processes the event
// 2. Register it via plugin.getEntityRegistry()
// 3. Access entity context through the system's iteration mechanism
// 4. Check conditions and cancel events accordingly

// Example skeleton (actual implementation depends on API details):
public class BuildingProtectionSystem extends EntityEventSystem<PlaceBlockEvent> {
    @Override
    public void process(PlaceBlockEvent event, Ref<EntityStore> entity, CommandBuffer<EntityStore> buffer) {
        // Entity context available through method parameters
        if (isProtectedRegion(event.getPosition())) {
            event.cancel();
        }
    }
}
```

### Affected Events

The following events extend `CancellableEcsEvent` and require ECS integration:
- `PlaceBlockEvent`
- `BreakBlockEvent`
- `DropItemEvent.PlayerRequest`

### Working Alternative: Regular Events

These events work with the standard `EventRegistry`:
- `PlayerConnectEvent`
- `PlayerDisconnectEvent`
- `AddWorldEvent`

```java
// CORRECT - Regular events work with EventRegistry
plugin.getEventRegistry().registerGlobal(PlayerConnectEvent.class, event -> {
    PlayerRef playerRef = event.getPlayerRef();  // This works!
});
```

---

## Teleport Component

### Problem

`Teleport.createForPlayer()` factory methods may not resolve at compile time.

```java
// MAY NOT COMPILE - Factory method resolution issues
Teleport teleport = Teleport.createForPlayer(position, rotation);
```

### Solution

Use the constructor directly:

```java
// CORRECT - Use constructor
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;

Teleport teleport = new Teleport(position, rotation);

// Then add to command buffer
commandBuffer.addComponent(entityRef, Teleport.getComponentType(), teleport);
```

### Full Respawn Example

```java
public void respawnPlayer(World world, Ref<EntityStore> playerRef, CommandBuffer<EntityStore> buffer) {
    Vector3d spawnPos = new Vector3d(0, 64, 0);
    Vector3f spawnRot = new Vector3f(0, 0, 0);

    Teleport teleport = new Teleport(spawnPos, spawnRot);
    buffer.addComponent(playerRef, Teleport.getComponentType(), teleport);
}
```

---

## Inventory/ItemContainer System

### Problem

`ItemContainer.setItemStack()` does **not** exist for direct slot manipulation.

```java
// WRONG - Direct slot manipulation doesn't work this way
ItemContainer inventory = player.getInventory();
inventory.setItemStack(0, myItemStack);  // Does not exist!
```

### Why This Happens

The Hytale inventory system uses a **transaction-based API** for item manipulation. Direct slot assignment is not supported to maintain inventory consistency and handle edge cases properly.

### Solution (Simplified)

For simple item giving, use higher-level APIs when available:

```java
// Check if higher-level item giving API exists
// This varies by context - check PlayerRef or Player component methods
```

### Solution (Transaction-Based)

For complex inventory manipulation, you need to use the transaction system:

```java
// Transaction-based inventory manipulation
// Note: Exact API depends on current Hytale version
// Check decompiled source for ItemContainer and related transaction classes

// General pattern:
// 1. Begin a transaction
// 2. Make changes within the transaction
// 3. Commit or rollback the transaction
```

### Current Workaround

For plugins that need to give visual items (like a flag in CTF), consider:
1. Using custom entity attachments instead of inventory items
2. Using particle effects or other visual indicators
3. Waiting for higher-level inventory APIs in future Hytale updates

```java
// Example: Log that feature needs transaction API
plugin.getLogger().atInfo().log("Item giving feature pending inventory transaction API implementation");
```

---

## Quick Reference Table

| What You Might Try | What Actually Works |
|-------------------|---------------------|
| `world.getChunkIfLoaded(chunkX, chunkZ)` | `world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(blockX, blockZ))` |
| `ctx.getArguments()` | Use `withRequiredArg()` + `ctx.get(arg)` pattern |
| `ctx.getPlayer()` | `ctx.isPlayer()` + `ctx.senderAs(Player.class)` |
| `ctx.getArgument("name", Type.class)` | Define arg with `withRequiredArg()`, get with `ctx.get(arg)` |
| `blockType.getName()` | `blockType.getId()` |
| `itemStack.getItemType().getKey()` | `itemStack.getItem().getId()` |
| `world.getBlockAccessor().setBlockInteractionState(...)` | Use `markNeedsSave()` + JSON visual variants |
| `import codec.BuilderCodec` | `import codec.builder.BuilderCodec` |
| `appendInherited(codec, setter, getter)` | `appendInherited(codec, setter, getter, copier).add()` |
| `.addDefault(value)` | Set default in constructor/field initializer |
| `plugin.getDataFolder()` | `plugin.getDataDirectory()` |
| `playerRef.getUUID()` | `playerRef.getUuid()` |
| `playerRef.sendChatMessage(text)` | `playerRef.sendMessage(Message.raw(text))` |
| `player.isOp()` | `player.hasPermission("permission.string")` |
| `transform.getTranslation()` | `transform.getPosition()` |
| `ecsEvent.getEntityRef()` | Use `EntityEventSystem` integration |
| `Teleport.createForPlayer(pos, rot)` | `new Teleport(pos, rot)` |
| `inventory.setItemStack(slot, item)` | Use transaction-based API |

---

## See Also

- [First Plugin Guide](../guides/first-plugin.md) - Updated with correct patterns
- [API Overview](./README.md) - Package structure and key classes
- [Decompiled Source](../../tools/decompiled-server/) - Authoritative API reference

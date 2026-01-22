# D&D TTRPG Plugin - TODO List

This document tracks pending implementation work that requires ECS API research.

## Overview

Several features are stubbed out because they require access to `ComponentAccessor<EntityStore>` which cannot be obtained from `World`. The Hytale ECS system provides ComponentAccessor through system iteration, not as a retrievable object from World.

---

## High Priority - Core Functionality

### Movement Execution
**File:** `src/main/java/com/example/dnd/movement/GridMovementManager.java`

**Issue:** Cannot execute player movement because `player.getComponent()` doesn't exist.

**What's needed:**
- Get `Store<EntityStore>` and `Ref<EntityStore>` from Player component
- Access `PlayerInput` component via `store.getComponent(ref, PlayerInput.getComponentType())`
- Queue `PlayerInput.AbsoluteMovement` to move the player

**Current state:** Movement confirmation logs a message but doesn't actually move the player.

---

### Player Position Retrieval
**File:** `src/main/java/com/example/dnd/movement/GridMovementManager.java`

**Issue:** Cannot get player's current position because `player.getComponent(TransformComponent.getComponentType())` doesn't exist.

**What's needed:**
- Access `TransformComponent` to get player position
- Requires Store/Ref pattern similar to movement execution

**Current state:** Uses placeholder position `(0, 64, 0)` when starting movement phase.

---

## Medium Priority - Visual Feedback

### Target Info Retrieval
**Files:**
- `src/main/java/com/example/dnd/targeting/TargetManager.java`
- `src/main/java/com/example/dnd/targeting/TargetInfo.java`

**Issue:** Cannot get detailed target information because `world.getComponentAccessor()` doesn't exist.

**What's needed:**
- Get `ComponentAccessor<EntityStore>` from somewhere (ECS system context?)
- Access `NPCEntity` component for name/role
- Access `EntityStatMap` for health stats
- Access `TransformComponent` for position

**Current state:**
- `TargetInfo.fromEntityRef()` returns placeholder values
- `TargetManager.getTargetInfo()` returns null
- Target selection works but shows "Target selected" instead of name/HP

---

### Path Particle Rendering
**File:** `src/main/java/com/example/dnd/movement/PathRenderer.java`

**Issue:** Cannot spawn particles because:
- `world.getComponentAccessor()` doesn't exist
- `player.getEntityRef()` doesn't exist

**What's needed:**
- Get `ComponentAccessor<EntityStore>` for ParticleUtil
- Get player's entity `Ref<EntityStore>` for viewer list
- Call `ParticleUtil.spawnParticleEffect()` with proper parameters

**Current state:** Path rendering is disabled; no visual feedback for movement paths.

---

### Target Highlight Particles
**File:** `src/main/java/com/example/dnd/targeting/TargetHighlighter.java`

**Issue:** Same as path rendering - cannot access ECS components for particle spawning.

**What's needed:**
- Same as Path Particle Rendering above

**Current state:** Target highlighting is disabled; no visual ring around targeted entities.

---

## Low Priority - Validation

### Self-Targeting Prevention
**File:** `src/main/java/com/example/dnd/targeting/TargetManager.java`

**Issue:** Cannot prevent players from targeting themselves because `player.getEntityRef()` doesn't exist.

**What's needed:**
- Get player's entity `Ref<EntityStore>` to compare with target ref

**Current state:** Self-targeting check is disabled.

---

### NPC Type Validation
**File:** `src/main/java/com/example/dnd/targeting/TargetManager.java`

**Issue:** Cannot verify if an entity is an NPC because `world.getComponentAccessor()` doesn't exist.

**What's needed:**
- Access `NPCEntity` component to check entity type

**Current state:** All entities are considered valid targets.

---

## Research Notes

### Patterns That Don't Work
```java
// These methods do not exist:
world.getComponentAccessor()
player.getComponent(ComponentType)
player.getEntityRef()
world.getBlockAt(x, y, z)  // Use world.getBlock(x, y, z) instead
```

### Patterns That Do Work
```java
// Getting Player from command context (AbstractPlayerCommand):
Player player = store.getComponent(ref, Player.getComponentType());

// Getting block data:
int blockId = world.getBlock(x, y, z);  // Returns block ID, 0 = air

// PlayerRef access (deprecated but functional):
PlayerRef playerRef = player.getPlayerRef();
UUID playerId = playerRef.getUuid();
playerRef.sendMessage(Message.raw("text"));

// World player iteration (deprecated but functional):
for (Player player : world.getPlayers()) { ... }

// Entity reference from Entity:
Ref<EntityStore> ref = entity.getReference();
```

### Possible Solutions to Investigate

1. **EntityEventSystem pattern** - ECS events provide ComponentAccessor through system iteration
2. **Command context** - AbstractPlayerCommand provides Store and Ref
3. **Event handlers** - Some events may provide accessor context
4. **Custom systems** - Register EntitySystem that has access to ComponentAccessor

---

## Deprecation Warnings (Expected)

These methods are deprecated but still functional:
- `player.getPlayerRef()` - Will be removed in future API
- `world.getPlayers()` - Will be removed in future API

Monitor Hytale updates for replacement patterns.

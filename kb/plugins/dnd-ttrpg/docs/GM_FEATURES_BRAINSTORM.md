# Game Master (GM) Features - Brainstorm

> **Project:** D&D TTRPG Hytale Plugin
> **Created:** 2026-01-22
> **Status:** Planning / Brainstorming

---

## Overview

The GM (Game Master/Dungeon Master) needs special tools to run tabletop sessions in Hytale. This document collects ideas for GM-exclusive features.

---

## Core GM Capabilities

### 1. NPC Control & Possession

**Concept:** GM takes direct control of NPCs to roleplay encounters

- [ ] **Possess NPC** - Camera attaches to NPC, GM controls movement/actions
- [ ] **NPC Voice** - Chat messages appear as NPC name, not GM's name
- [ ] **Quick Swap** - Rapidly switch between controlling different NPCs
- [ ] **Unpossess** - Return to normal GM view
- [ ] **NPC Puppet Mode** - Control NPC while keeping GM camera (bird's eye)

**API Notes:**
- `MountNPC` packet can attach player to entity
- `PlayerInput` can be redirected to control mounted entity
- `NPCEntity.setRole()` and state control available

### 2. NPC Management

**Concept:** Spawn, configure, and manage NPCs in the world

- [ ] **Spawn NPC** - `/gm spawn <role> [name] [hp] [ac]`
- [ ] **Quick Spawn Palette** - UI with frequently used enemy types
- [ ] **NPC Stats Override** - Set custom HP, damage, AC for any NPC
- [ ] **NPC Naming** - Give NPCs custom display names
- [ ] **NPC Teams/Factions** - Group NPCs for initiative and targeting
- [ ] **Despawn/Remove** - Clean up NPCs after encounters
- [ ] **NPC Templates** - Save/load NPC configurations

**API Notes:**
- `NPCSpawnCommand` exists for spawning
- `EntityStatMap` for HP/stats modification
- `Role` system for NPC behavior

### 3. Combat Management

**Concept:** GM controls the flow of combat

- [ ] **Force Turn Order** - Manually reorder initiative
- [ ] **Insert Combatant** - Add NPC to existing initiative mid-combat
- [ ] **Remove Combatant** - Remove from initiative (death, flee, etc.)
- [ ] **Skip Turn** - Skip any combatant's turn
- [ ] **Undo Last Action** - Rollback recent combat actions
- [ ] **Pause Combat** - Freeze combat for discussion/ruling
- [ ] **Damage Override** - Manually set damage dealt
- [ ] **Heal/Damage Any** - Directly modify any creature's HP

### 4. World Control

**Concept:** GM manipulates the environment

- [ ] **Teleport Players** - Move players to specific locations
- [ ] **Teleport NPCs** - Reposition NPCs
- [ ] **Reveal/Hide Areas** - Fog of war control
- [ ] **Spawn Effects** - Visual/particle effects for drama
- [ ] **Day/Night Control** - Set time of day
- [ ] **Weather Control** - Change weather for atmosphere
- [ ] **Block Manipulation** - Quick terrain editing

**API Notes:**
- `Teleport` component available
- `ParticleUtil` for effects

### 5. Information & Visibility

**Concept:** GM sees more than players

- [ ] **See All Stats** - View any creature's full stat block
- [ ] **See Hidden** - Invisible/hidden creatures visible to GM
- [ ] **Player View Toggle** - See what a specific player sees
- [ ] **Combat Log** - Detailed log of all rolls and actions
- [ ] **Initiative Tracker** - Enhanced view with HP bars for all
- [ ] **Map Overview** - Full battlefield view without player limits

### 6. Communication Tools

**Concept:** GM communicates with players effectively

- [ ] **Whisper to Player** - Private message to one player
- [ ] **Narrator Text** - Styled narrative text to all players
- [ ] **NPC Speech Bubbles** - Visual speech from NPCs
- [ ] **Sound Effects** - Play sounds for atmosphere
- [ ] **Music Control** - Background music management
- [ ] **Handouts** - Show images/text to players

### 7. Session Management

**Concept:** Save and manage campaign state

- [ ] **Save Session** - Snapshot current state
- [ ] **Load Session** - Restore previous state
- [ ] **Player Notes** - GM notes per player
- [ ] **NPC Notes** - Notes attached to NPCs
- [ ] **Location Notes** - Notes for areas
- [ ] **Quest Tracker** - Track quest progress

---

## GM Permission System

### Role Hierarchy

```
Owner/Host
    └── Game Master (GM)
            └── Co-GM / Assistant GM
                    └── Player
                            └── Spectator
```

### GM-Only Commands (Proposed)

| Command | Description |
|---------|-------------|
| `/gm` | Toggle GM mode |
| `/gm spawn <role>` | Spawn NPC |
| `/gm possess <target>` | Take control of NPC |
| `/gm unpossess` | Return to GM view |
| `/gm tp <player> <location>` | Teleport player |
| `/gm damage <target> <amount>` | Deal damage |
| `/gm heal <target> <amount>` | Heal target |
| `/gm kill <target>` | Instantly kill |
| `/gm initiative add <npc>` | Add to initiative |
| `/gm initiative remove <target>` | Remove from initiative |
| `/gm initiative set <target> <value>` | Set initiative value |
| `/gm pause` | Pause combat |
| `/gm resume` | Resume combat |
| `/gm narrator <text>` | Broadcast narrator text |
| `/gm whisper <player> <text>` | Private message |

---

## UI Concepts

### GM Control Panel

```
┌─────────────────────────────────────┐
│ [GM Mode Active]          [Exit GM] │
├─────────────────────────────────────┤
│ Quick Actions:                      │
│ [Spawn] [Possess] [Damage] [Heal]   │
├─────────────────────────────────────┤
│ NPCs in Scene:                      │
│ ├─ Goblin Warrior (HP: 12/15)  [▶]  │
│ ├─ Goblin Archer (HP: 8/10)    [▶]  │
│ └─ Orc Chief (HP: 45/60)       [▶]  │
├─────────────────────────────────────┤
│ Players:                            │
│ ├─ Alice (HP: 32/35)                │
│ └─ Bob (HP: 18/28)                  │
└─────────────────────────────────────┘
```

### NPC Possession HUD

```
┌─────────────────────────────────────┐
│ CONTROLLING: Goblin Warrior         │
│ HP: 12/15  AC: 13                   │
│ [Attack] [Move] [Ability] [Release] │
└─────────────────────────────────────┘
```

---

## Priority Features (MVP)

### Must Have (Phase 1)
1. **GM Mode Toggle** - Enable/disable GM permissions
2. **NPC Spawning** - Basic spawn command
3. **NPC Possession** - Control NPC movement and attacks
4. **Damage/Heal Commands** - Direct HP manipulation
5. **Initiative Management** - Add/remove from combat

### Should Have (Phase 2)
6. **GM Control Panel UI** - Visual interface
7. **NPC Quick Actions** - Attack, move via buttons
8. **Narrator Text** - Atmospheric messaging
9. **Teleport Commands** - Move players/NPCs

### Nice to Have (Phase 3)
10. **Session Save/Load** - Persistence
11. **Fog of War** - Area visibility
12. **Sound/Music** - Atmosphere
13. **NPC Templates** - Reusable configurations

---

## Technical Considerations

### NPC Possession Implementation

```
1. GM clicks "Possess" on NPC
2. Server stores GM's original camera state
3. Server sends MountNPC packet to attach GM to NPC
4. Camera settings updated to follow NPC
5. PlayerInput from GM routed to NPC's movement
6. NPC's Role AI is suspended while possessed
7. GM clicks "Release" to unpossess
8. Server restores GM camera and position
9. NPC's Role AI resumes
```

### Permission Checking

```java
public boolean isGameMaster(Player player) {
    // Option 1: Check server permission
    return player.hasPermission("dnd.gm");

    // Option 2: Check plugin-managed GM list
    return gmManager.isGM(player.getUuid());

    // Option 3: Check if host/owner
    return player.isServerOwner();
}
```

### State Management

Need to track:
- Which player is the GM
- Which NPC the GM is possessing (if any)
- GM's original position/camera (for unpossess)
- Modified NPC stats (overrides)
- Session state for save/load

---

## Hytale API Reference

### NPC Control APIs

| Class | Purpose |
|-------|---------|
| `NPCEntity` | NPC behavior, role, state |
| `Role` | NPC role definition |
| `CombatSupport` | Attack sequences |
| `StateSupport` | Animation/state control |
| `MotionControllerWalk` | Ground movement |

### Player/Camera APIs

| Class | Purpose |
|-------|---------|
| `PlayerInput` | Movement control (AbsoluteMovement, RelativeMovement) |
| `CameraManager` | Camera manipulation |
| `MountNPC` | Attach player to entity |
| `Teleport` | Entity positioning |

### Permission APIs

| Class | Purpose |
|-------|---------|
| `HytalePermissions` | Permission strings |
| `PermissionHolder` | Permission checking |

---

## Open Questions

1. **Multiple GMs?** - Should we support co-GMs?
2. **GM Player Character?** - Can GM also play a character?
3. **Spectator Mode?** - Separate from GM mode?
4. **Persistence?** - How to save/load sessions?
5. **NPC AI Interruption?** - How to handle AI when GM possesses?
6. **Voice Chat Integration?** - Any Hytale voice APIs?

---

## Related Files

```
plugins/dnd-ttrpg/                    # Main plugin code
tools/decompiled-server/
├── com/hypixel/hytale/server/npc/    # NPC APIs
│   ├── entities/NPCEntity.java
│   ├── role/Role.java
│   ├── role/support/CombatSupport.java
│   └── commands/NPCSpawnCommand.java
├── com/hypixel/hytale/protocol/packets/
│   └── interaction/MountNPC.java     # Possession packet
└── com/hypixel/hytale/server/core/
    ├── permissions/                   # Permission system
    └── modules/entity/player/PlayerInput.java
```

---

## Ideas Backlog

_Add new ideas here as they come up:_

- Encounter builder UI
- Random encounter generator
- Loot table management
- XP tracking and leveling
- Rest mechanics (short/long rest)
- Condition tracking (poisoned, stunned, etc.)
- Concentration checks
- Death saving throws
- Advantage/disadvantage tracking
- Inspiration points
- Map markers and annotations
- Combat grid overlay
- Range indicators
- Area of effect templates (cone, sphere, line)
- Lair actions
- Legendary actions
- Environmental hazards

---

*Last updated: 2026-01-22*

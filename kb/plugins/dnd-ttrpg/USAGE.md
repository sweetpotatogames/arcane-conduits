# D&D TTRPG Plugin - Usage Guide

A Dungeons & Dragons 5th Edition-inspired tabletop RPG system for Hytale, featuring turn-based combat, character sheets, dice rolling, and grid-based tactical movement.

## Table of Contents

- [Getting Started](#getting-started)
- [Camera Views](#camera-views)
- [Character Sheets](#character-sheets)
- [Dice Rolling](#dice-rolling)
- [Combat System](#combat-system)
  - [Initiative](#initiative)
  - [Turn Management](#turn-management)
  - [Movement](#movement)
  - [Target Selection](#target-selection)
- [Command Reference](#command-reference)

---

## Getting Started

1. Install the plugin to your Hytale server's mods folder
2. Join the server
3. Use `/dnd camera topdown` to switch to an overhead tactical view
4. Use `/dnd sheet` to open your character sheet and set your stats
5. When ready for combat, have all players roll initiative with `/dnd initiative roll`

---

## Camera Views

The plugin provides CRPG-style camera views for tactical gameplay.

| Command | Description |
|---------|-------------|
| `/dnd camera topdown` | Top-down overhead view (default) |
| `/dnd camera isometric` | Isometric angled view |
| `/dnd camera reset` | Return to normal Hytale camera |

**Tip:** The top-down view works best for tactical combat, giving you a clear view of the battlefield.

---

## Character Sheets

Each player has a D&D 5e-style character sheet tracking abilities, HP, AC, and more.

### Opening Your Character Sheet

```
/dnd sheet
```

This opens a UI panel displaying all your character stats.

### Viewing Stats in Chat

```
/dnd sheet stats
```

Displays your character stats in the chat window.

### Setting Stats

```
/dnd sheet set <stat> <value>
```

**Available stats:**

| Stat | Aliases | Description |
|------|---------|-------------|
| `str` | `strength` | Strength ability score |
| `dex` | `dexterity` | Dexterity ability score |
| `con` | `constitution` | Constitution ability score |
| `int` | `intelligence` | Intelligence ability score |
| `wis` | `wisdom` | Wisdom ability score |
| `cha` | `charisma` | Charisma ability score |
| `hp` | - | Current hit points |
| `maxhp` | - | Maximum hit points |
| `ac` | - | Armor class |

**Examples:**
```
/dnd sheet set str 16      # Set Strength to 16
/dnd sheet set dex 14      # Set Dexterity to 14
/dnd sheet set maxhp 45    # Set Max HP to 45
/dnd sheet set hp 45       # Set current HP to 45
/dnd sheet set ac 18       # Set Armor Class to 18
```

### Ability Modifiers

Ability modifiers are calculated automatically using the standard D&D formula:
```
Modifier = (Ability Score - 10) / 2
```

| Score | Modifier |
|-------|----------|
| 1 | -5 |
| 8-9 | -1 |
| 10-11 | +0 |
| 12-13 | +1 |
| 14-15 | +2 |
| 16-17 | +3 |
| 18-19 | +4 |
| 20 | +5 |

---

## Dice Rolling

Roll any standard dice with modifiers. Results are broadcast to all players.

### Basic Syntax

```
/dnd roll <dice> [modifier]
```

### Dice Notation

- `d20` - Roll one d20
- `2d6` - Roll two d6
- `4d8` - Roll four d8
- `d20 5` - Roll d20 with +5 modifier
- `2d6 -2` - Roll 2d6 with -2 modifier

### Examples

```
/dnd roll d20           # Attack roll
/dnd roll d20 5         # Attack roll with +5 modifier
/dnd roll 2d6 3         # Damage roll: 2d6+3
/dnd roll 8d6           # Fireball damage
/dnd roll d100          # Percentile roll
```

**Output Example:**
```
[D&D] PlayerName rolled 2d6+3: [4, 6] + 3 = 13
```

---

## Combat System

The combat system uses D&D-style turn-based combat with initiative order, movement phases, and action phases.

### Combat Flow

1. **Pre-Combat:** All combatants roll initiative
2. **Combat Start:** DM/host starts combat
3. **Each Turn:**
   - Movement Phase: Move up to your speed
   - Action Phase: Attack, cast spells, or use abilities
4. **Combat End:** DM/host ends combat when encounter is resolved

---

### Initiative

Before combat begins, all participants roll initiative to determine turn order.

| Command | Description |
|---------|-------------|
| `/dnd initiative roll [modifier]` | Roll initiative (d20 + modifier) |
| `/dnd initiative list` | Show current initiative order |
| `/dnd initiative clear` | Clear all initiative (requires combat to be ended) |

**Example:**
```
/dnd initiative roll 3    # Roll initiative with +3 DEX modifier
```

**Output:**
```
[D&D] PlayerName rolled initiative: d20+3: [15] + 3 = 18
```

---

### Turn Management

Control the flow of combat turns.

| Command | Description |
|---------|-------------|
| `/dnd turn start` | Begin combat (requires initiative to be rolled) |
| `/dnd turn next` | End your turn and advance to next player |
| `/dnd turn end` | End combat entirely |
| `/dnd turn status` | Show current turn and phase |

### Combat HUD

When combat is active, a HUD appears showing:
- Current turn indicator
- Initiative order with all combatants
- Phase information (Movement/Action)
- Movement remaining (during movement phase)
- Target information (when target selected)

### Combat Control Panel

```
/dnd combat
```

Opens a UI panel with buttons for common combat actions.

---

### Movement

During your turn's Movement Phase, you can move using the grid-based movement system.

#### Movement Controls

| Action | How To |
|--------|--------|
| Select destination | **Left-click** on a block |
| Change destination | **Left-click** on a different block |
| Confirm movement | **Right-click** or `/dnd move confirm` |
| Cancel planned move | `/dnd move cancel` |
| Skip movement | `/dnd move skip` |

#### Movement Rules

- **Grid-Based:** 1 Hytale block = 1 D&D square = 5 feet
- **Default Speed:** 6 blocks (30 feet) per turn
- **Pathfinding:** Automatic A* pathfinding around obstacles
- **Diagonal Movement:** Configurable (1 or 1.5 blocks per diagonal)

#### Movement Display

When planning movement:
- **Green path particles:** Destination is reachable
- **Red path particles:** Destination is too far
- **HUD shows:** "Movement: X/Y blocks" remaining

#### Movement Commands

| Command | Description |
|---------|-------------|
| `/dnd move cancel` | Cancel your planned movement |
| `/dnd move confirm` | Execute your planned movement |
| `/dnd move skip` | Skip movement phase entirely |
| `/dnd move status` | Show detailed movement info |
| `/dnd move speed [blocks]` | Get or set your movement speed |
| `/dnd move diagonal [1\|2]` | Set diagonal cost (1=simple, 2=5e variant) |

**Setting Movement Speed:**
```
/dnd move speed 8     # Set speed to 8 blocks (40 feet) - e.g., for a Monk
/dnd move speed       # Show current speed
```

**Diagonal Movement Options:**
```
/dnd move diagonal 1   # Simple: 1 block per diagonal
/dnd move diagonal 2   # 5e Variant: 1.5 blocks per diagonal (alternating)
```

---

### Target Selection

Select enemy NPCs as targets for attacks and abilities. Selected targets show a visual highlight and HP information in your HUD.

#### Targeting Controls

| Action | How To |
|--------|--------|
| Select target | **Left-click** on an NPC |
| Deselect target | **Left-click** same NPC again, or `/dnd target clear` |

#### Target Display

When you have a target selected:
- **Particle ring:** Orange particles circle the target's feet (turns red at low HP)
- **HUD panel:** Shows target name, HP bar, and current/max HP

#### Target Commands

| Command | Description |
|---------|-------------|
| `/dnd target` | Show your current target |
| `/dnd target clear` | Clear your current target |
| `/dnd target info` | Show detailed target information |

**Target Info Output:**
```
[D&D] Target Information:
  Name: Goblin Warrior
  Role: NPC/Enemies/Goblin
  Health: 12/15 (80%)
  Position: (125.5, 64.0, -230.5)
  Status: Alive
```

**Note:** Targets are automatically cleared when:
- The target dies (HP reaches 0)
- Combat ends
- You manually deselect

---

## Command Reference

### All Commands at a Glance

| Command | Description |
|---------|-------------|
| **Camera** | |
| `/dnd camera [topdown\|isometric\|reset]` | Change camera view |
| **Character** | |
| `/dnd sheet` | Open character sheet UI |
| `/dnd sheet stats` | View stats in chat |
| `/dnd sheet set <stat> <value>` | Set a character stat |
| **Dice** | |
| `/dnd roll <dice> [modifier]` | Roll dice |
| **Initiative** | |
| `/dnd initiative roll [modifier]` | Roll initiative |
| `/dnd initiative list` | Show initiative order |
| `/dnd initiative clear` | Clear initiative |
| **Turn** | |
| `/dnd turn start` | Start combat |
| `/dnd turn next` | End turn, next player |
| `/dnd turn end` | End combat |
| `/dnd turn status` | Show turn status |
| **Combat UI** | |
| `/dnd combat` | Open combat control panel |
| **Movement** | |
| `/dnd move cancel` | Cancel planned movement |
| `/dnd move confirm` | Execute planned movement |
| `/dnd move skip` | Skip movement phase |
| `/dnd move status` | Show movement status |
| `/dnd move speed [blocks]` | Get/set movement speed |
| `/dnd move diagonal [1\|2]` | Set diagonal cost |
| **Target** | |
| `/dnd target` | Show current target |
| `/dnd target clear` | Clear current target |
| `/dnd target info` | Show detailed target info |

---

## Tips for Dungeon Masters

1. **Before Combat:**
   - Have all players set up their character sheets
   - Spawn enemy NPCs in the world
   - Have everyone roll initiative

2. **Starting Combat:**
   - Use `/dnd turn start` to begin
   - Combat HUD will appear for all combatants

3. **During Combat:**
   - Players move and act on their turns
   - Use `/dnd turn next` when a player's turn is complete
   - Players can target NPCs to see their HP

4. **Ending Combat:**
   - Use `/dnd turn end` when the encounter is resolved
   - All targets and HUDs are automatically cleared

---

## Troubleshooting

**Q: The combat HUD isn't showing up**
A: Make sure combat has been started with `/dnd turn start` and initiative was rolled first.

**Q: I can't move during combat**
A: Check if it's your turn and you're in the Movement Phase. Use `/dnd turn status` to check.

**Q: My movement path shows red**
A: The destination is beyond your remaining movement. Choose a closer destination or use `/dnd move skip` to skip movement.

**Q: I clicked an NPC but nothing happened**
A: Make sure you're in combat and it's your turn. Target selection works during both Movement and Action phases.

---

## Version History

- **1.0.0** - Initial release
  - Top-down and isometric camera views
  - Character sheets with D&D 5e ability scores
  - Dice rolling system
  - Turn-based combat with initiative
  - Grid-based movement with A* pathfinding
  - NPC targeting with HP display

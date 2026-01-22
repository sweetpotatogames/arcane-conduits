package com.example.dnd.commands;

import com.example.dnd.DndPlugin;
import com.example.dnd.character.Ability;
import com.example.dnd.character.CharacterSheet;
import com.example.dnd.ui.CharacterSheetPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Command to view and modify character sheets.
 * Usage: /dnd sheet [show|set|stats] [stat] [value]
 */
public class SheetCommand extends AbstractPlayerCommand {
    private final DndPlugin plugin;
    private final DefaultArg<String> actionArg;
    private final OptionalArg<String> statArg;
    private final OptionalArg<Integer> valueArg;

    public SheetCommand(DndPlugin plugin) {
        super("sheet", "server.commands.dnd.sheet.desc");
        this.plugin = plugin;

        actionArg = withDefaultArg("action", "Action: show, set, or stats", ArgTypes.STRING, "show", "show");
        statArg = withOptionalArg("stat", "Stat to set (for 'set' action)", ArgTypes.STRING);
        valueArg = withOptionalArg("value", "Value to set", ArgTypes.INTEGER);
    }

    @Override
    protected void execute(
        @Nonnull CommandContext context,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PlayerRef playerRef,
        @Nonnull World world
    ) {
        String action = context.get(actionArg);

        // Get or create character sheet for this player
        CharacterSheet sheet = plugin.getOrCreateCharacterSheet(playerRef.getUuid());

        switch (action.toLowerCase()) {
            case "show" -> openCharacterSheetUI(ref, store, playerRef, sheet);
            case "set" -> handleSet(context, playerRef, sheet);
            case "stats" -> showStats(playerRef, sheet);
            default -> playerRef.sendMessage(Message.raw("[D&D] Unknown action: " + action + ". Use: show, set, or stats"));
        }
    }

    private void openCharacterSheetUI(Ref<EntityStore> ref, Store<EntityStore> store,
                                       PlayerRef playerRef, CharacterSheet sheet) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            CharacterSheetPage page = new CharacterSheetPage(playerRef, sheet, plugin);
            player.getPageManager().openCustomPage(ref, store, page);
        }
    }

    private void handleSet(CommandContext context, PlayerRef playerRef, CharacterSheet sheet) {
        String stat = context.get(statArg);
        Integer value = context.get(valueArg);

        if (stat == null || value == null) {
            playerRef.sendMessage(Message.raw("[D&D] Usage: /dnd sheet set <stat> <value>"));
            playerRef.sendMessage(Message.raw("Stats: str, dex, con, int, wis, cha, hp, maxhp, ac"));
            return;
        }

        switch (stat.toLowerCase()) {
            case "str", "strength" -> {
                sheet.setAbilityScore(Ability.STRENGTH, value);
                playerRef.sendMessage(Message.raw("[D&D] Strength set to " + value));
            }
            case "dex", "dexterity" -> {
                sheet.setAbilityScore(Ability.DEXTERITY, value);
                playerRef.sendMessage(Message.raw("[D&D] Dexterity set to " + value));
            }
            case "con", "constitution" -> {
                sheet.setAbilityScore(Ability.CONSTITUTION, value);
                playerRef.sendMessage(Message.raw("[D&D] Constitution set to " + value));
            }
            case "int", "intelligence" -> {
                sheet.setAbilityScore(Ability.INTELLIGENCE, value);
                playerRef.sendMessage(Message.raw("[D&D] Intelligence set to " + value));
            }
            case "wis", "wisdom" -> {
                sheet.setAbilityScore(Ability.WISDOM, value);
                playerRef.sendMessage(Message.raw("[D&D] Wisdom set to " + value));
            }
            case "cha", "charisma" -> {
                sheet.setAbilityScore(Ability.CHARISMA, value);
                playerRef.sendMessage(Message.raw("[D&D] Charisma set to " + value));
            }
            case "hp" -> {
                sheet.setCurrentHp(value);
                playerRef.sendMessage(Message.raw("[D&D] HP set to " + value));
            }
            case "maxhp" -> {
                sheet.setMaxHp(value);
                playerRef.sendMessage(Message.raw("[D&D] Max HP set to " + value));
            }
            case "ac" -> {
                sheet.setArmorClass(value);
                playerRef.sendMessage(Message.raw("[D&D] AC set to " + value));
            }
            default -> playerRef.sendMessage(Message.raw("[D&D] Unknown stat: " + stat));
        }
    }

    private void showStats(PlayerRef playerRef, CharacterSheet sheet) {
        StringBuilder sb = new StringBuilder("[D&D] Character Stats:\n");
        sb.append(String.format("HP: %d/%d | AC: %d\n",
            sheet.getCurrentHp(), sheet.getMaxHp(), sheet.getArmorClass()));
        sb.append("Abilities:\n");
        for (Ability ability : Ability.values()) {
            int score = sheet.getAbilityScore(ability);
            String mod = CharacterSheet.formatModifier(sheet.getModifier(ability));
            sb.append(String.format("  %s: %d (%s)\n", ability.getAbbreviation(), score, mod));
        }
        playerRef.sendMessage(Message.raw(sb.toString()));
    }
}

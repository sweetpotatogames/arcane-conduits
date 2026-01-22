package com.example.dnd.ui;

import com.example.dnd.DndPlugin;
import com.example.dnd.character.Ability;
import com.example.dnd.character.CharacterSheet;
import com.example.dnd.character.DiceRoller;
import com.example.dnd.combat.CombatState;
import com.example.dnd.combat.TurnManager;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Interactive character sheet UI page.
 */
public class CharacterSheetPage extends InteractiveCustomUIPage<CharacterSheetPage.SheetEventData> {
    private final CharacterSheet sheet;
    private final DndPlugin plugin;

    public CharacterSheetPage(@Nonnull PlayerRef playerRef, @Nonnull CharacterSheet sheet, @Nonnull DndPlugin plugin) {
        super(playerRef, CustomPageLifetime.CanDismiss, SheetEventData.CODEC);
        this.sheet = sheet;
        this.plugin = plugin;
    }

    @Override
    public void build(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull UICommandBuilder cmd,
        @Nonnull UIEventBuilder events,
        @Nonnull Store<EntityStore> store
    ) {
        // Load the character sheet UI layout
        cmd.append("Pages/Dnd/CharacterSheet.ui");

        // Set player name in title
        cmd.set("#PlayerName.Text", playerRef.getUsername());

        // Set HP values
        cmd.set("#CurrentHp.Text", String.valueOf(sheet.getCurrentHp()));
        cmd.set("#MaxHp.Text", String.valueOf(sheet.getMaxHp()));
        cmd.set("#ArmorClass.Text", String.valueOf(sheet.getArmorClass()));

        // Set ability scores and modifiers
        for (Ability ability : Ability.values()) {
            String abbr = ability.getAbbreviation().toLowerCase();
            int score = sheet.getAbilityScore(ability);
            String mod = CharacterSheet.formatModifier(sheet.getModifier(ability));

            cmd.set("#" + abbr + "Score.Text", String.valueOf(score));
            cmd.set("#" + abbr + "Mod.Text", mod);

            // Bind ability roll button
            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#" + abbr + "Roll",
                new EventData().append("Action", "RollAbility").append("Value", ability.name()),
                false
            );
        }

        // Bind dice roll buttons
        int[] diceTypes = {4, 6, 8, 10, 12, 20};
        for (int die : diceTypes) {
            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#rollD" + die,
                new EventData().append("Action", "RollDice").append("Value", String.valueOf(die)),
                false
            );
        }

        // Bind HP adjustment buttons
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#hpMinus",
            new EventData().append("Action", "AdjustHp").append("Value", "-1"),
            false
        );
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#hpPlus",
            new EventData().append("Action", "AdjustHp").append("Value", "1"),
            false
        );
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#hpMinus5",
            new EventData().append("Action", "AdjustHp").append("Value", "-5"),
            false
        );
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#hpPlus5",
            new EventData().append("Action", "AdjustHp").append("Value", "5"),
            false
        );

        // Bind saving throw buttons
        for (Ability ability : Ability.values()) {
            String abbr = ability.getAbbreviation().toLowerCase();
            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#" + abbr + "Save",
                new EventData().append("Action", "RollSave").append("Value", ability.name()),
                false
            );
        }

        // Bind initiative roll
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#rollInitiative",
            new EventData().append("Action", "RollInitiative").append("Value", ""),
            false
        );

        // ========== Combat Section ==========
        World world = store.getExternalData().getWorld();
        TurnManager turnManager = plugin.getTurnManager();
        CombatState combatState = turnManager.getCombatState(world);
        UUID myUuid = playerRef.getUuid();

        // Set combat status
        boolean combatActive = combatState.isCombatActive();
        boolean isMyTurn = combatState.isPlayerTurn(myUuid);

        cmd.set("#combatStatusLabel.Text", combatActive ? "Combat Active" : "No Combat");
        cmd.set("#combatStatusLabel.Style.TextColor", combatActive ? "#4caf50" : "#888888");

        if (combatActive) {
            String currentPlayer = combatState.getCurrentPlayerName();
            cmd.set("#turnStatusLabel.Text", isMyTurn ? "Your turn!" : "Current: " + currentPlayer);
            cmd.set("#turnStatusLabel.Style.TextColor", isMyTurn ? "#4caf50" : "#ffeb3b");
        } else {
            cmd.set("#turnStatusLabel.Text", "");
        }

        // Bind End Turn button
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#endTurnBtn",
            new EventData().append("Action", "EndTurn").append("Value", ""),
            false
        );

        // Bind Open Combat Panel button
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#openCombatBtn",
            new EventData().append("Action", "OpenCombat").append("Value", ""),
            false
        );
    }

    @Override
    public void handleDataEvent(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull SheetEventData data
    ) {
        World world = store.getExternalData().getWorld();
        UICommandBuilder cmd = new UICommandBuilder();

        switch (data.action) {
            case "RollDice" -> {
                int die = Integer.parseInt(data.value);
                DiceRoller.DiceResult result = DiceRoller.roll(1, die, 0);
                broadcastRoll(world, "d" + die, result);
            }
            case "RollAbility" -> {
                Ability ability = Ability.valueOf(data.value);
                int mod = sheet.getModifier(ability);
                DiceRoller.DiceResult result = DiceRoller.rollD20(mod);
                broadcastRoll(world, ability.name() + " check", result);
            }
            case "RollSave" -> {
                Ability ability = Ability.valueOf(data.value);
                int mod = sheet.getSavingThrowBonus(ability);
                DiceRoller.DiceResult result = DiceRoller.rollD20(mod);
                broadcastRoll(world, ability.name() + " save", result);
            }
            case "RollInitiative" -> {
                int mod = sheet.getModifier(Ability.DEXTERITY);
                DiceRoller.DiceResult result = DiceRoller.rollD20(mod);
                broadcastRoll(world, "Initiative", result);
            }
            case "AdjustHp" -> {
                int delta = Integer.parseInt(data.value);
                sheet.setCurrentHp(sheet.getCurrentHp() + delta);
                cmd.set("#CurrentHp.Text", String.valueOf(sheet.getCurrentHp()));
            }
            case "EndTurn" -> {
                handleEndTurn(world, cmd);
            }
            case "OpenCombat" -> {
                handleOpenCombat(ref, store);
            }
        }

        sendUpdate(cmd, null, false);
    }

    private void handleEndTurn(World world, UICommandBuilder cmd) {
        TurnManager turnManager = plugin.getTurnManager();
        CombatState combatState = turnManager.getCombatState(world);

        if (!combatState.isCombatActive()) {
            playerRef.sendMessage(Message.raw("[D&D] No active combat!"));
            return;
        }

        if (!combatState.isPlayerTurn(playerRef.getUuid())) {
            playerRef.sendMessage(Message.raw("[D&D] It's not your turn!"));
            return;
        }

        combatState.nextTurn();
        String message = String.format("[D&D] Turn ended. Next: %s", combatState.getCurrentPlayerName());
        broadcastMessage(world, message);

        // Refresh all combat HUDs
        turnManager.refreshAllHuds(world);

        // Update combat status in this UI
        boolean isMyTurn = combatState.isPlayerTurn(playerRef.getUuid());
        String currentPlayer = combatState.getCurrentPlayerName();
        cmd.set("#turnStatusLabel.Text", isMyTurn ? "Your turn!" : "Current: " + currentPlayer);
        cmd.set("#turnStatusLabel.Style.TextColor", isMyTurn ? "#4caf50" : "#ffeb3b");
    }

    private void handleOpenCombat(Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            World world = store.getExternalData().getWorld();
            TurnManager turnManager = plugin.getTurnManager();
            CombatControlPage combatPage = new CombatControlPage(playerRef, turnManager, plugin, world);
            player.getPageManager().openCustomPage(ref, store, combatPage);
        }
    }

    private void broadcastRoll(World world, String rollType, DiceRoller.DiceResult result) {
        String message = String.format("[D&D] %s rolled %s: %s",
            playerRef.getUsername(), rollType, result.format());

        for (Player player : world.getPlayers()) {
            player.getPlayerRef().sendMessage(Message.raw(message));
        }
    }

    private void broadcastMessage(World world, String message) {
        for (Player player : world.getPlayers()) {
            player.getPlayerRef().sendMessage(Message.raw(message));
        }
    }

    /**
     * Event data for the character sheet page.
     */
    public static class SheetEventData {
        public static final BuilderCodec<SheetEventData> CODEC = BuilderCodec.builder(
                SheetEventData.class, SheetEventData::new)
            .append(new KeyedCodec<>("Action", Codec.STRING), (e, s) -> e.action = s, e -> e.action)
            .add()
            .append(new KeyedCodec<>("Value", Codec.STRING), (e, s) -> e.value = s, e -> e.value)
            .add()
            .build();

        private String action;
        private String value;

        public SheetEventData() {}
    }
}

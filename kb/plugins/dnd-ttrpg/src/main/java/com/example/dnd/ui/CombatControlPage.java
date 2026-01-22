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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Interactive combat control page with buttons for combat actions.
 * This is a modal page that blocks gameplay but supports button events.
 */
public class CombatControlPage extends InteractiveCustomUIPage<CombatControlPage.CombatEventData> {
    private final TurnManager turnManager;
    private final DndPlugin plugin;
    private final World world;

    public CombatControlPage(@Nonnull PlayerRef playerRef, @Nonnull TurnManager turnManager,
                             @Nonnull DndPlugin plugin, @Nonnull World world) {
        super(playerRef, CustomPageLifetime.CanDismiss, CombatEventData.CODEC);
        this.turnManager = turnManager;
        this.plugin = plugin;
        this.world = world;
    }

    @Override
    public void build(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull UICommandBuilder cmd,
        @Nonnull UIEventBuilder events,
        @Nonnull Store<EntityStore> store
    ) {
        cmd.append("Pages/Dnd/CombatControl.ui");

        CombatState state = turnManager.getCombatState(world);
        UUID myUuid = playerRef.getUuid();

        // Set combat status
        boolean combatActive = state.isCombatActive();
        cmd.set("#combatStatus.Text", combatActive ? "Combat Active" : "No Active Combat");
        cmd.set("#combatStatus.Style.TextColor", combatActive ? "#4caf50" : "#888888");

        // Set current turn info
        if (combatActive) {
            String currentPlayer = state.getCurrentPlayerName();
            boolean isMyTurn = state.isPlayerTurn(myUuid);

            cmd.set("#currentTurnLabel.Text", "Current Turn: " + currentPlayer);
            cmd.set("#turnPromptLabel.Text", isMyTurn ? "It's your turn!" : "Waiting for " + currentPlayer);
            cmd.set("#turnPromptLabel.Style.TextColor", isMyTurn ? "#4caf50" : "#ffeb3b");
        } else {
            cmd.set("#currentTurnLabel.Text", "No combat in progress");
            cmd.set("#turnPromptLabel.Text", "Start combat to begin");
            cmd.set("#turnPromptLabel.Style.TextColor", "#888888");
        }

        // Build initiative list
        buildInitiativeList(cmd, state, myUuid);

        // Bind action buttons
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#endTurnBtn",
            new EventData().append("Action", "EndTurn"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#rollInitBtn",
            new EventData().append("Action", "RollInitiative"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#openSheetBtn",
            new EventData().append("Action", "OpenSheet"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#startCombatBtn",
            new EventData().append("Action", "StartCombat"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#endCombatBtn",
            new EventData().append("Action", "EndCombat"),
            false
        );
    }

    /**
     * Build the initiative order list display.
     */
    private void buildInitiativeList(UICommandBuilder cmd, CombatState state, UUID myUuid) {
        List<UUID> order = state.getInitiativeOrder();
        Map<UUID, String> names = state.getPlayerNames();
        Map<UUID, Integer> rolls = state.getInitiativeRolls();
        UUID currentPlayer = state.getCurrentPlayer();

        // Update up to 8 initiative slots
        int maxSlots = 8;
        for (int i = 0; i < maxSlots; i++) {
            String slotId = "#initEntry" + i;

            if (i < order.size()) {
                UUID playerId = order.get(i);
                String name = names.getOrDefault(playerId, "Unknown");
                int roll = rolls.getOrDefault(playerId, 0);

                String text = String.format("%d. %s (%d)", i + 1, name, roll);
                cmd.set(slotId + ".Text", text);
                cmd.set(slotId + ".Visible", true);

                // Color coding
                boolean isCurrent = playerId.equals(currentPlayer);
                boolean isMe = playerId.equals(myUuid);

                String textColor;
                if (isCurrent && isMe) {
                    textColor = "#4caf50";
                } else if (isCurrent) {
                    textColor = "#ffeb3b";
                } else if (isMe) {
                    textColor = "#2196f3";
                } else {
                    textColor = "#cccccc";
                }
                cmd.set(slotId + ".Style.TextColor", textColor);
                cmd.set(slotId + ".Style.RenderBold", isCurrent);
            } else {
                cmd.set(slotId + ".Visible", false);
            }
        }

        // Show "No combatants" message if empty
        cmd.set("#noInitiativeMsg.Visible", order.isEmpty());
    }

    @Override
    public void handleDataEvent(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull CombatEventData data
    ) {
        CombatState state = turnManager.getCombatState(world);
        UICommandBuilder cmd = new UICommandBuilder();

        switch (data.action) {
            case "EndTurn" -> handleEndTurn(state, cmd);
            case "RollInitiative" -> handleRollInitiative(state, cmd);
            case "OpenSheet" -> handleOpenSheet(ref, store);
            case "StartCombat" -> handleStartCombat(state, cmd);
            case "EndCombat" -> handleEndCombat(state, cmd);
        }

        // Refresh the UI after any action
        refreshDisplay(cmd, state);
        sendUpdate(cmd, null, false);
    }

    private void handleEndTurn(CombatState state, UICommandBuilder cmd) {
        if (!state.isCombatActive()) {
            playerRef.sendMessage(Message.raw("[D&D] No active combat!"));
            return;
        }

        if (!state.isPlayerTurn(playerRef.getUuid())) {
            playerRef.sendMessage(Message.raw("[D&D] It's not your turn!"));
            return;
        }

        state.nextTurn();
        String message = String.format("[D&D] Turn ended. Next: %s", state.getCurrentPlayerName());
        broadcastMessage(message);

        // Refresh all combat HUDs
        turnManager.refreshAllHuds(world);
    }

    private void handleRollInitiative(CombatState state, UICommandBuilder cmd) {
        if (state.isCombatActive()) {
            playerRef.sendMessage(Message.raw("[D&D] Cannot roll initiative during active combat!"));
            return;
        }

        // Get player's DEX modifier from character sheet
        CharacterSheet sheet = plugin.getOrCreateCharacterSheet(playerRef.getUuid());
        int dexMod = sheet.getModifier(Ability.DEXTERITY);

        DiceRoller.DiceResult result = DiceRoller.rollD20(dexMod);
        state.addToInitiative(playerRef.getUuid(), playerRef.getUsername(), result.total());

        String message = String.format("[D&D] %s rolled initiative: %s",
            playerRef.getUsername(), result.format());
        broadcastMessage(message);
    }

    private void handleOpenSheet(Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            CharacterSheet sheet = plugin.getOrCreateCharacterSheet(playerRef.getUuid());
            CharacterSheetPage sheetPage = new CharacterSheetPage(playerRef, sheet, plugin);
            player.getPageManager().openCustomPage(ref, store, sheetPage);
        }
    }

    private void handleStartCombat(CombatState state, UICommandBuilder cmd) {
        if (state.isCombatActive()) {
            playerRef.sendMessage(Message.raw("[D&D] Combat is already active!"));
            return;
        }

        if (state.getInitiativeOrder().isEmpty()) {
            playerRef.sendMessage(Message.raw("[D&D] No combatants! Roll initiative first."));
            return;
        }

        state.startCombat();
        String message = String.format("[D&D] Combat started! First turn: %s",
            state.getCurrentPlayerName());
        broadcastMessage(message);

        // Show HUDs for all combatants
        turnManager.showCombatHuds(world);
    }

    private void handleEndCombat(CombatState state, UICommandBuilder cmd) {
        if (!state.isCombatActive()) {
            playerRef.sendMessage(Message.raw("[D&D] No active combat to end!"));
            return;
        }

        // Hide all combat HUDs first
        turnManager.hideCombatHuds(world);

        state.endCombat();
        broadcastMessage("[D&D] Combat ended!");
    }

    private void refreshDisplay(UICommandBuilder cmd, CombatState state) {
        UUID myUuid = playerRef.getUuid();
        boolean combatActive = state.isCombatActive();

        cmd.set("#combatStatus.Text", combatActive ? "Combat Active" : "No Active Combat");
        cmd.set("#combatStatus.Style.TextColor", combatActive ? "#4caf50" : "#888888");

        if (combatActive) {
            String currentPlayer = state.getCurrentPlayerName();
            boolean isMyTurn = state.isPlayerTurn(myUuid);

            cmd.set("#currentTurnLabel.Text", "Current Turn: " + currentPlayer);
            cmd.set("#turnPromptLabel.Text", isMyTurn ? "It's your turn!" : "Waiting for " + currentPlayer);
            cmd.set("#turnPromptLabel.Style.TextColor", isMyTurn ? "#4caf50" : "#ffeb3b");
        } else {
            cmd.set("#currentTurnLabel.Text", "No combat in progress");
            cmd.set("#turnPromptLabel.Text", "Start combat to begin");
            cmd.set("#turnPromptLabel.Style.TextColor", "#888888");
        }

        buildInitiativeList(cmd, state, myUuid);
    }

    private void broadcastMessage(String message) {
        for (Player player : world.getPlayers()) {
            player.getPlayerRef().sendMessage(Message.raw(message));
        }
    }

    /**
     * Event data for combat control actions.
     */
    public static class CombatEventData {
        public static final BuilderCodec<CombatEventData> CODEC = BuilderCodec.builder(
                CombatEventData.class, CombatEventData::new)
            .append(new KeyedCodec<>("Action", Codec.STRING), (e, s) -> e.action = s, e -> e.action)
            .add()
            .build();

        private String action;

        public CombatEventData() {}
    }
}

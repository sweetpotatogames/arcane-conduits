package com.example.dnd.ui;

import com.example.dnd.combat.CombatState;
import com.example.dnd.combat.TurnManager;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent HUD overlay showing turn order and combat status.
 * This is display-only (no button events) but shows continuously during combat.
 */
public class CombatHud extends CustomUIHud {
    private final TurnManager turnManager;
    private final World world;

    public CombatHud(@Nonnull PlayerRef playerRef, @Nonnull TurnManager turnManager, @Nonnull World world) {
        super(playerRef);
        this.turnManager = turnManager;
        this.world = world;
    }

    @Override
    protected void build(@Nonnull UICommandBuilder cmd) {
        cmd.append("Hud/Dnd/CombatHud.ui");
        updateTurnDisplay(cmd);
    }

    /**
     * Refresh the HUD with current combat state.
     */
    public void refresh() {
        UICommandBuilder cmd = new UICommandBuilder();
        updateTurnDisplay(cmd);
        update(false, cmd);
    }

    /**
     * Update the turn display elements.
     */
    private void updateTurnDisplay(UICommandBuilder cmd) {
        CombatState state = turnManager.getCombatState(world);

        // Set current turn player name
        String currentPlayer = state.getCurrentPlayerName();
        cmd.set("#currentTurnName.Text", currentPlayer);

        // Set turn prompt based on whether it's this player's turn
        UUID myUuid = getPlayerRef().getUuid();
        boolean isMyTurn = state.isPlayerTurn(myUuid);
        cmd.set("#turnPrompt.Text", isMyTurn ? "Your turn!" : "Waiting for " + currentPlayer + "...");

        // Set prompt color (green for your turn, gray otherwise)
        cmd.set("#turnPrompt.Style.TextColor", isMyTurn ? "#4caf50" : "#888888");

        // Build initiative order list
        buildInitiativeList(cmd, state, myUuid);

        // Show round number if we want to track that
        cmd.set("#roundLabel.Text", "Combat Active");
    }

    /**
     * Build the initiative order list display.
     */
    private void buildInitiativeList(UICommandBuilder cmd, CombatState state, UUID myUuid) {
        List<UUID> order = state.getInitiativeOrder();
        Map<UUID, String> names = state.getPlayerNames();
        Map<UUID, Integer> rolls = state.getInitiativeRolls();
        UUID currentPlayer = state.getCurrentPlayer();

        // We'll update up to 8 initiative slots in the HUD
        int maxSlots = 8;
        for (int i = 0; i < maxSlots; i++) {
            String slotId = "#initSlot" + i;

            if (i < order.size()) {
                UUID playerId = order.get(i);
                String name = names.getOrDefault(playerId, "Unknown");
                int roll = rolls.getOrDefault(playerId, 0);

                // Format: "1. PlayerName (15)"
                String text = String.format("%d. %s (%d)", i + 1, name, roll);
                cmd.set(slotId + ".Text", text);

                // Show slot
                cmd.set(slotId + ".Visible", true);

                // Highlight current turn player
                boolean isCurrent = playerId.equals(currentPlayer);
                boolean isMe = playerId.equals(myUuid);

                String textColor;
                if (isCurrent && isMe) {
                    textColor = "#4caf50"; // Green - my turn
                } else if (isCurrent) {
                    textColor = "#ffeb3b"; // Yellow - their turn
                } else if (isMe) {
                    textColor = "#2196f3"; // Blue - me, not my turn
                } else {
                    textColor = "#cccccc"; // Gray - others
                }
                cmd.set(slotId + ".Style.TextColor", textColor);

                // Bold if current turn
                cmd.set(slotId + ".Style.RenderBold", isCurrent);
            } else {
                // Hide unused slots
                cmd.set(slotId + ".Visible", false);
            }
        }
    }

    /**
     * Hide the HUD (clear it).
     */
    public void hide() {
        UICommandBuilder cmd = new UICommandBuilder();
        update(true, cmd); // Clear the HUD
    }
}

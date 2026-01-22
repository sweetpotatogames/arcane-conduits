package com.example.dnd.ui;

import com.example.dnd.combat.CombatState;
import com.example.dnd.combat.TurnManager;
import com.example.dnd.combat.TurnPhase;
import com.example.dnd.movement.GridMovementManager;
import com.example.dnd.movement.MovementState;
import com.example.dnd.targeting.TargetInfo;
import com.example.dnd.targeting.TargetManager;
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

        // Update movement display
        updateMovementDisplay(cmd, state, myUuid, isMyTurn);

        // Update target display
        updateTargetDisplay(cmd, myUuid);
    }

    /**
     * Update the movement information display.
     */
    private void updateMovementDisplay(UICommandBuilder cmd, CombatState combatState, UUID myUuid, boolean isMyTurn) {
        // Only show movement info during movement phase when it's player's turn
        boolean showMovement = isMyTurn && combatState.getCurrentPhase() == TurnPhase.MOVEMENT;

        cmd.set("#movementPanel.Visible", showMovement);

        if (!showMovement) {
            return;
        }

        MovementState moveState = GridMovementManager.get().getState(myUuid);
        if (moveState == null) {
            cmd.set("#movementLabel.Text", "Movement: --");
            cmd.set("#movementHint.Visible", false);
            return;
        }

        // Show movement remaining
        int remaining = moveState.getRemainingMovement();
        int total = moveState.getTotalMovement();
        int planned = moveState.getPlannedDistance();

        // Format: "Movement: 4/6 blocks (10ft planned)"
        String movementText;
        if (moveState.getPlannedDestination() != null) {
            int afterMove = remaining - planned;
            movementText = String.format("Movement: %d/%d blocks (%d planned)", afterMove, total, planned);
        } else {
            movementText = String.format("Movement: %d/%d blocks", remaining, total);
        }
        cmd.set("#movementLabel.Text", movementText);

        // Set color based on whether path is valid
        boolean canReach = moveState.canReachDestination();
        String movementColor = canReach ? "#4caf50" : "#f44336"; // Green or red
        cmd.set("#movementLabel.Style.TextColor", movementColor);

        // Show hint text
        if (moveState.getPlannedDestination() != null) {
            cmd.set("#movementHint.Text", canReach
                ? "Right-click to move, click elsewhere to change"
                : "Too far! Select a closer destination");
            cmd.set("#movementHint.Visible", true);
        } else {
            cmd.set("#movementHint.Text", "Click a block to set destination");
            cmd.set("#movementHint.Visible", true);
        }
    }

    /**
     * Update the target information display.
     */
    private void updateTargetDisplay(UICommandBuilder cmd, UUID myUuid) {
        TargetManager targetManager = TargetManager.get();
        TargetInfo targetInfo = targetManager.getTargetInfo(myUuid, world);

        // Show/hide target panel based on whether we have a valid target
        boolean hasTarget = targetInfo != null && targetInfo.isValid();
        cmd.set("#targetPanel.Visible", hasTarget);

        if (!hasTarget) {
            return;
        }

        // Set target name
        cmd.set("#targetName.Text", targetInfo.getName());

        // Set HP text
        String hpText = String.format("%.0f/%.0f", targetInfo.getCurrentHp(), targetInfo.getMaxHp());
        cmd.set("#hpText.Text", hpText);

        // Calculate HP bar width (max 118 pixels to fit within the 120px container with 1px borders)
        float hpPercent = targetInfo.getHpPercent();
        int barWidth = Math.max(1, (int) (118 * hpPercent));
        cmd.set("#hpBarFill.Anchor.Width", barWidth);

        // Set HP bar color based on percentage
        String hpColor;
        if (hpPercent > 0.5f) {
            hpColor = "#44aa44"; // Green
        } else if (hpPercent > 0.25f) {
            hpColor = "#aaaa44"; // Yellow
        } else {
            hpColor = "#aa4444"; // Red
        }
        cmd.set("#hpBarFill.Background.Color", hpColor);
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

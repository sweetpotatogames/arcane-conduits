package com.example.dnd.combat;

import com.example.dnd.movement.GridMovementManager;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.MouseButtonState;
import com.hypixel.hytale.protocol.MouseButtonType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.UUID;

/**
 * Handles player mouse actions during turn-based combat.
 *
 * During the MOVEMENT phase:
 * - Left-click: Select/update movement destination
 * - Right-click: Confirm and execute movement
 *
 * Blocks actions when it's not the player's turn.
 */
public class CombatEventHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final TurnManager turnManager;
    private final GridMovementManager movementManager;

    public CombatEventHandler(TurnManager turnManager) {
        this.turnManager = turnManager;
        this.movementManager = GridMovementManager.get();
    }

    /**
     * Handle mouse button events for turn-based combat.
     */
    public void onPlayerMouseButton(PlayerMouseButtonEvent event) {
        PlayerRef playerRef = event.getPlayerRefComponent();
        World world = event.getPlayerRef().getStore().getExternalData().getWorld();

        CombatState combatState = turnManager.getCombatState(world);

        // If no combat active, allow all actions
        if (!combatState.isCombatActive()) {
            return;
        }

        UUID playerId = playerRef.getUuid();

        // If it's not this player's turn, cancel the action
        if (!combatState.isPlayerTurn(playerId)) {
            event.setCancelled(true);

            // Notify the player
            String currentPlayer = combatState.getCurrentPlayerName();
            playerRef.sendMessage(Message.raw(
                "[D&D] Not your turn! Waiting for: " + currentPlayer
            ));

            LOGGER.atFine().log("Blocked action from %s - not their turn (current: %s)",
                playerRef.getUsername(), currentPlayer);
            return;
        }

        // Handle movement phase clicks
        if (combatState.getCurrentPhase() == TurnPhase.MOVEMENT) {
            handleMovementPhaseClick(event, world);
        }
    }

    /**
     * Handle mouse clicks during the movement phase.
     */
    private void handleMovementPhaseClick(PlayerMouseButtonEvent event, World world) {
        MouseButtonType buttonType = event.getMouseButton().mouseButtonType;
        MouseButtonState buttonState = event.getMouseButton().state;

        // Only process on button release (not press)
        if (buttonState != MouseButtonState.Released) {
            return;
        }

        Player player = event.getPlayer();
        Vector3i targetBlock = event.getTargetBlock();

        // Skip if no valid target block
        if (targetBlock == null) {
            return;
        }

        if (buttonType == MouseButtonType.Left) {
            // Left-click: Select/update destination
            event.setCancelled(true);
            movementManager.onBlockClicked(player, targetBlock, world);

            LOGGER.atFine().log("Movement click: Player %s selected block %s",
                player.getPlayerRef().getUsername(), targetBlock);

        } else if (buttonType == MouseButtonType.Right) {
            // Right-click: Confirm movement
            event.setCancelled(true);
            movementManager.confirmMovement(player, world);

            LOGGER.atFine().log("Movement confirm: Player %s confirmed movement",
                player.getPlayerRef().getUsername());
        }
    }
}

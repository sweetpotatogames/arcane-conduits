package com.example.dnd.combat;

import com.example.dnd.camera.CameraInputHandler;
import com.example.dnd.movement.GridMovementManager;
import com.example.dnd.targeting.TargetManager;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.MouseButtonState;
import com.hypixel.hytale.protocol.MouseButtonType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.UUID;

/**
 * Handles player mouse actions during turn-based combat.
 *
 * During the MOVEMENT phase:
 * - Left-click on block: Select/update movement destination
 * - Left-click on NPC: Select as target
 * - Right-click: Confirm and execute movement
 *
 * During the ACTION phase:
 * - Left-click on NPC: Select as target
 *
 * Blocks actions when it's not the player's turn.
 */
public class CombatEventHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final TurnManager turnManager;
    private final GridMovementManager movementManager;
    private final TargetManager targetManager;

    public CombatEventHandler(TurnManager turnManager) {
        this.turnManager = turnManager;
        this.movementManager = GridMovementManager.get();
        this.targetManager = TargetManager.get();
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

        // Handle phase-specific clicks
        TurnPhase currentPhase = combatState.getCurrentPhase();

        if (currentPhase == TurnPhase.MOVEMENT) {
            handleMovementPhaseClick(event, world);
        } else if (currentPhase == TurnPhase.ACTION) {
            handleActionPhaseClick(event, world);
        }
    }

    /**
     * Handle mouse clicks during the action phase.
     * Primary use: target selection for attacks/abilities.
     */
    @SuppressWarnings("deprecation")
    private void handleActionPhaseClick(PlayerMouseButtonEvent event, World world) {
        MouseButtonType buttonType = event.getMouseButton().mouseButtonType;
        MouseButtonState buttonState = event.getMouseButton().state;

        // Only process on button release
        if (buttonState != MouseButtonState.Released) {
            return;
        }

        Player player = event.getPlayer();

        // Left-click: Try to select entity as target
        if (buttonType == MouseButtonType.Left) {
            // getTargetEntity() returns Entity directly
            Entity targetEntity = event.getTargetEntity();
            if (targetEntity != null && targetManager.isValidTarget(targetEntity, world)) {
                event.setCancelled(true);
                targetManager.selectTarget(player, targetEntity, world);

                LOGGER.atFine().log("Target selection: Player %s clicked entity",
                    player.getPlayerRef().getUsername());
            }
        }
    }

    /**
     * Handle mouse clicks during the movement phase.
     */
    @SuppressWarnings("deprecation")
    private void handleMovementPhaseClick(PlayerMouseButtonEvent event, World world) {
        MouseButtonType buttonType = event.getMouseButton().mouseButtonType;
        MouseButtonState buttonState = event.getMouseButton().state;

        // Only process on button release (not press)
        if (buttonState != MouseButtonState.Released) {
            return;
        }

        Player player = event.getPlayer();

        if (buttonType == MouseButtonType.Left) {
            // Check if clicking on an entity first (for targeting)
            // getTargetEntity() returns Entity directly
            Entity targetEntity = event.getTargetEntity();
            if (targetEntity != null && targetManager.isValidTarget(targetEntity, world)) {
                // Clicked on a valid target - select it
                event.setCancelled(true);
                targetManager.selectTarget(player, targetEntity, world);

                LOGGER.atFine().log("Movement phase target: Player %s selected entity",
                    player.getPlayerRef().getUsername());
                return;
            }

            // Otherwise, handle as movement destination
            Vector3i targetBlock = event.getTargetBlock();
            if (targetBlock != null) {
                event.setCancelled(true);
                movementManager.onBlockClicked(player, targetBlock, world);

                LOGGER.atFine().log("Movement click: Player %s selected block %s",
                    player.getPlayerRef().getUsername(), targetBlock);
            }

        } else if (buttonType == MouseButtonType.Right) {
            // Right-click: Confirm movement (only if not being used for camera rotation)
            UUID playerId = player.getPlayerRef().getUuid();

            // Check if this right-click was used for camera rotation
            if (CameraInputHandler.get().isRightClickForCamera(playerId)) {
                LOGGER.atFine().log("Right-click ignored for combat: Player %s was rotating camera",
                    player.getPlayerRef().getUsername());
                return; // Don't process as movement confirm
            }

            Vector3i targetBlock = event.getTargetBlock();
            if (targetBlock != null) {
                event.setCancelled(true);
                movementManager.confirmMovement(player, world);

                LOGGER.atFine().log("Movement confirm: Player %s confirmed movement",
                    player.getPlayerRef().getUsername());
            }
        }
    }
}

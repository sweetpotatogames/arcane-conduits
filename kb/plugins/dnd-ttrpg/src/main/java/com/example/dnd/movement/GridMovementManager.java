package com.example.dnd.movement;

import com.example.dnd.DndPlugin;
import com.example.dnd.character.CharacterSheet;
import com.example.dnd.combat.CombatState;
import com.example.dnd.combat.TurnManager;
import com.example.dnd.combat.TurnPhase;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for D&D grid-based movement during combat.
 *
 * Coordinates:
 * - Click-to-move destination selection
 * - A* pathfinding around obstacles
 * - Path visualization with particles
 * - Movement execution (teleport to destination)
 * - Movement tracking per turn
 *
 * TODO: Movement execution requires ECS integration for component access.
 * The player.getComponent() pattern doesn't exist - need Store/Ref/ComponentAccessor.
 */
public class GridMovementManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static GridMovementManager instance;

    private final Map<UUID, MovementState> playerStates = new ConcurrentHashMap<>();
    private final GridPathfinder pathfinder;
    private final PathRenderer renderer;
    private final MovementConfig config;

    private GridMovementManager() {
        this.config = MovementConfig.get();
        this.pathfinder = new GridPathfinder(config);
        this.renderer = new PathRenderer();
    }

    public static GridMovementManager get() {
        if (instance == null) {
            instance = new GridMovementManager();
        }
        return instance;
    }

    /**
     * Called when a player clicks on a block during their turn's movement phase.
     * Updates the planned destination and path.
     */
    public void onBlockClicked(Player player, Vector3i targetBlock, World world) {
        UUID playerId = player.getPlayerRef().getUuid();
        MovementState state = playerStates.get(playerId);

        if (state == null || !state.isPlanning()) {
            LOGGER.atFine().log("Player %s clicked but not in planning mode", playerId);
            return;
        }

        // Calculate path from current position to target
        Vector3i currentPos = state.getStartPosition();
        int remainingMovement = state.getRemainingMovement();

        // Don't allow clicking on current position
        if (targetBlock.equals(currentPos)) {
            return;
        }

        // Find path to target
        List<Vector3i> path = pathfinder.findPath(world, currentPos, targetBlock, remainingMovement);

        if (path == null) {
            // No valid path found
            PlayerRef playerRef = player.getPlayerRef();
            playerRef.sendMessage(Message.raw("[D&D] Cannot reach that location!"));
            LOGGER.atFine().log("No path found from %s to %s within %d blocks",
                currentPos, targetBlock, remainingMovement);

            // Still show the target but as unreachable
            // Try to find a path ignoring distance for visualization
            List<Vector3i> unreachablePath = pathfinder.findPath(world, currentPos, targetBlock, 100);
            if (unreachablePath != null) {
                state.setPlannedDestination(targetBlock);
                state.setPathWaypoints(unreachablePath);
                renderer.renderPath(player, state, world);

                int distance = unreachablePath.size() - 1;
                playerRef.sendMessage(Message.raw(
                    String.format("[D&D] Target is %d blocks away (you have %d remaining)",
                        distance, remainingMovement)
                ));
            }
            return;
        }

        // Valid path found - update state
        state.setPlannedDestination(targetBlock);
        state.setPathWaypoints(path);

        // Render the path
        renderer.renderPath(player, state, world);

        // Notify player
        int distance = path.size() - 1;
        player.getPlayerRef().sendMessage(Message.raw(
            String.format("[D&D] Path set: %d blocks. Right-click to move, or click elsewhere to change.",
                distance)
        ));

        // Refresh HUD
        TurnManager.get().refreshAllHuds(world);

        LOGGER.atFine().log("Player %s selected destination %s, path length: %d",
            playerId, targetBlock, distance);
    }

    /**
     * Called when player confirms movement (right-click or command).
     * Executes the planned movement.
     */
    public void confirmMovement(Player player, World world) {
        UUID playerId = player.getPlayerRef().getUuid();
        MovementState state = playerStates.get(playerId);

        if (state == null || !state.isPlanning()) {
            player.getPlayerRef().sendMessage(Message.raw("[D&D] Not in movement planning mode!"));
            return;
        }

        if (state.getPlannedDestination() == null) {
            player.getPlayerRef().sendMessage(Message.raw("[D&D] Click a destination first!"));
            return;
        }

        if (!state.canReachDestination()) {
            player.getPlayerRef().sendMessage(Message.raw(
                "[D&D] Destination too far! Select a closer location."
            ));
            return;
        }

        // Execute the movement
        executeMovement(player, state, world);

        // Update state
        int distanceMoved = state.getPlannedDistance();
        state.commitMovement();

        // Clear path visualization
        renderer.clearPath(playerId);

        // Notify player
        int remaining = state.getRemainingMovement();
        player.getPlayerRef().sendMessage(Message.raw(
            String.format("[D&D] Moved %d blocks. %d blocks remaining this turn.", distanceMoved, remaining)
        ));

        // Check if movement is depleted
        if (remaining <= 0) {
            state.setPlanning(false);
            advanceToActionPhase(player, world);
        }

        // Refresh HUD
        TurnManager.get().refreshAllHuds(world);

        LOGGER.atInfo().log("Player %s moved %d blocks, %d remaining",
            playerId, distanceMoved, remaining);
    }

    /**
     * Execute the actual movement to the destination.
     * Currently uses instant teleport; can be enhanced with animation later.
     *
     * TODO: Implement proper movement execution once ECS patterns are researched.
     * The player.getComponent() pattern doesn't exist - need to access
     * PlayerInput component via Store.getComponent(ref, type).
     */
    @SuppressWarnings("unused")
    private void executeMovement(Player player, MovementState state, World world) {
        Vector3i destination = state.getPlannedDestination();

        // TODO: ECS movement disabled - needs API research
        // The following patterns no longer work:
        // - player.getComponent(PlayerInput.getComponentType())
        //
        // Needs investigation into correct patterns for:
        // - Getting Store/Ref from Player component
        // - Accessing PlayerInput component
        // - Queueing AbsoluteMovement

        // Destination coordinates for when this is implemented:
        // double x = destination.x + 0.5;
        // double y = destination.y;
        // double z = destination.z + 0.5;

        LOGGER.atInfo().log("Movement execution pending ECS implementation: target %s", destination);
    }

    /**
     * Advance the turn to the action phase after movement is complete.
     */
    private void advanceToActionPhase(Player player, World world) {
        CombatState combatState = TurnManager.get().getCombatState(world);
        combatState.setCurrentPhase(TurnPhase.ACTION);

        player.getPlayerRef().sendMessage(Message.raw(
            "[D&D] Movement complete. Now in ACTION phase."
        ));
    }

    /**
     * Cancel the planned movement (undo).
     */
    public void cancelPlannedMovement(Player player, World world) {
        UUID playerId = player.getPlayerRef().getUuid();
        MovementState state = playerStates.get(playerId);

        if (state == null) {
            return;
        }

        state.clearPlannedPath();
        renderer.clearPath(playerId);

        player.getPlayerRef().sendMessage(Message.raw("[D&D] Movement cancelled."));

        // Refresh HUD
        TurnManager.get().refreshAllHuds(world);
    }

    /**
     * Start the movement phase for a player.
     * Called when combat starts or when a turn begins.
     *
     * TODO: Getting player position requires ECS integration.
     * Currently uses placeholder position.
     */
    @SuppressWarnings("deprecation")
    public void startMovementPhase(Player player, World world) {
        UUID playerId = player.getPlayerRef().getUuid();

        // TODO: Get current position - needs ECS API research
        // The following pattern doesn't work:
        // - player.getComponent(TransformComponent.getComponentType())
        //
        // For now, use a placeholder position (0,64,0)
        // This should be replaced with actual position retrieval
        Vector3i position = new Vector3i(0, 64, 0);

        // Get movement speed from character sheet
        int moveSpeed = getCharacterMoveSpeed(playerId);

        // Create or reset movement state
        MovementState state = playerStates.computeIfAbsent(playerId, MovementState::new);
        state.resetForNewTurn(position, moveSpeed);

        player.getPlayerRef().sendMessage(Message.raw(
            String.format("[D&D] Your turn! Movement: %d blocks. Click to select destination.", moveSpeed)
        ));

        LOGGER.atInfo().log("Started movement phase for player %s at %s with %d blocks",
            playerId, position, moveSpeed);
    }

    /**
     * End the movement phase for a player.
     */
    public void endMovementPhase(Player player) {
        UUID playerId = player.getPlayerRef().getUuid();
        MovementState state = playerStates.get(playerId);

        if (state != null) {
            state.setPlanning(false);
            renderer.clearPath(playerId);
        }
    }

    /**
     * Get the movement speed for a character.
     * Defaults to 6 blocks (30ft) if no character sheet exists.
     */
    private int getCharacterMoveSpeed(UUID playerId) {
        CharacterSheet sheet = DndPlugin.get().getCharacterSheet(playerId);
        if (sheet != null) {
            // Convert D&D speed (in feet) to blocks: speed / 5
            return sheet.getSpeed() / 5;
        }
        return config.getDefaultMoveSpeed();
    }

    /**
     * Get the movement state for a player (may be null).
     */
    public MovementState getState(UUID playerId) {
        return playerStates.get(playerId);
    }

    /**
     * Check if a player is currently in movement planning mode.
     */
    public boolean isPlanning(UUID playerId) {
        MovementState state = playerStates.get(playerId);
        return state != null && state.isPlanning();
    }

    /**
     * Get the pathfinder for external use.
     */
    public GridPathfinder getPathfinder() {
        return pathfinder;
    }

    /**
     * Get the path renderer for external use.
     */
    public PathRenderer getRenderer() {
        return renderer;
    }

    /**
     * Get the movement config for external use.
     */
    public MovementConfig getConfig() {
        return config;
    }
}

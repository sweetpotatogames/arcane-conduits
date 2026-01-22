package com.example.dnd.movement;

import com.hypixel.hytale.math.vector.Vector3i;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Tracks each player's movement planning state during their turn.
 */
public class MovementState {
    private final UUID playerId;
    private Vector3i startPosition;           // Where turn started
    private Vector3i plannedDestination;      // Currently selected destination
    private List<Vector3i> pathWaypoints;     // Path blocks from start to destination
    private int totalMovement;                // Max movement in blocks (e.g., 6 = 30ft)
    private int usedMovement;                 // Already moved this turn
    private boolean planning;                 // In planning mode?

    public MovementState(UUID playerId) {
        this.playerId = playerId;
        this.pathWaypoints = new ArrayList<>();
        this.totalMovement = 6; // Default 30ft = 6 blocks
        this.usedMovement = 0;
        this.planning = false;
    }

    /**
     * Get remaining movement in blocks.
     */
    public int getRemainingMovement() {
        return totalMovement - usedMovement;
    }

    /**
     * Get the planned path distance in blocks.
     */
    public int getPlannedDistance() {
        if (pathWaypoints == null || pathWaypoints.isEmpty()) {
            return 0;
        }
        // Path length is the number of steps (waypoints - 1)
        return Math.max(0, pathWaypoints.size() - 1);
    }

    /**
     * Check if the planned destination is reachable with remaining movement.
     */
    public boolean canReachDestination() {
        return getPlannedDistance() <= getRemainingMovement();
    }

    /**
     * Reset state for a new turn.
     */
    public void resetForNewTurn(Vector3i position, int moveSpeed) {
        this.startPosition = position;
        this.plannedDestination = null;
        this.pathWaypoints = new ArrayList<>();
        this.totalMovement = moveSpeed;
        this.usedMovement = 0;
        this.planning = true;
    }

    /**
     * Clear planned path without resetting turn.
     */
    public void clearPlannedPath() {
        this.plannedDestination = null;
        this.pathWaypoints = new ArrayList<>();
    }

    /**
     * Mark movement as complete and add to used movement.
     */
    public void commitMovement() {
        if (plannedDestination != null && pathWaypoints != null) {
            usedMovement += getPlannedDistance();
            startPosition = plannedDestination;
            clearPlannedPath();
        }
    }

    // Getters and setters

    public UUID getPlayerId() {
        return playerId;
    }

    public Vector3i getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(Vector3i startPosition) {
        this.startPosition = startPosition;
    }

    public Vector3i getPlannedDestination() {
        return plannedDestination;
    }

    public void setPlannedDestination(Vector3i plannedDestination) {
        this.plannedDestination = plannedDestination;
    }

    public List<Vector3i> getPathWaypoints() {
        return Collections.unmodifiableList(pathWaypoints);
    }

    public void setPathWaypoints(List<Vector3i> pathWaypoints) {
        this.pathWaypoints = new ArrayList<>(pathWaypoints);
    }

    public int getTotalMovement() {
        return totalMovement;
    }

    public void setTotalMovement(int totalMovement) {
        this.totalMovement = totalMovement;
    }

    public int getUsedMovement() {
        return usedMovement;
    }

    public void setUsedMovement(int usedMovement) {
        this.usedMovement = usedMovement;
    }

    public boolean isPlanning() {
        return planning;
    }

    public void setPlanning(boolean planning) {
        this.planning = planning;
    }
}

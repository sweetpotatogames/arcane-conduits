package com.example.dnd.movement;

import com.hypixel.hytale.math.vector.Vector3i;

/**
 * Configurable movement rules for the D&D grid-based movement system.
 *
 * D&D 5e Conversion:
 * - Standard D&D: 1 square = 5 feet
 * - Hytale: 1 block = 1 meter
 * - Simplified Rule: 1 block = 5 feet (1 square)
 * - Typical move speed: 30 feet = 6 blocks per turn
 */
public class MovementConfig {
    private static MovementConfig instance;

    // Movement costs
    private double orthogonalCost = 1.0;    // Cost for N/S/E/W movement
    private double diagonalCost = 1.0;      // Cost for diagonal (configurable to 1.5)

    // Default movement speeds
    private int defaultMoveSpeed = 6;        // Default movement in blocks (30ft)

    // Pathfinding settings
    private int maxPathLength = 50;          // Maximum path length to search
    private boolean allowVerticalMovement = true;  // Allow moving up/down blocks
    private int maxStepHeight = 1;           // Maximum height difference per step

    private MovementConfig() {}

    public static MovementConfig get() {
        if (instance == null) {
            instance = new MovementConfig();
        }
        return instance;
    }

    /**
     * Calculate the movement cost between two adjacent positions.
     */
    public double getMoveCost(Vector3i from, Vector3i to) {
        int dx = Math.abs(from.x - to.x);
        int dz = Math.abs(from.z - to.z);
        int dy = Math.abs(from.y - to.y);

        // If there's vertical movement, add extra cost
        double verticalCost = dy > 0 ? dy : 0;

        // Check if diagonal (both x and z changed)
        boolean isDiagonal = dx > 0 && dz > 0;

        return (isDiagonal ? diagonalCost : orthogonalCost) + verticalCost;
    }

    /**
     * Calculate the heuristic distance between two points.
     * Uses Chebyshev distance for 8-directional movement.
     */
    public double getHeuristic(Vector3i from, Vector3i to) {
        int dx = Math.abs(from.x - to.x);
        int dy = Math.abs(from.y - to.y);
        int dz = Math.abs(from.z - to.z);

        // Chebyshev distance for 8-directional movement on XZ plane
        int horizontalDist = Math.max(dx, dz);

        // Include vertical distance
        return horizontalDist + dy;
    }

    // Getters and setters

    public double getOrthogonalCost() {
        return orthogonalCost;
    }

    public void setOrthogonalCost(double orthogonalCost) {
        this.orthogonalCost = orthogonalCost;
    }

    public double getDiagonalCost() {
        return diagonalCost;
    }

    public void setDiagonalCost(double diagonalCost) {
        this.diagonalCost = diagonalCost;
    }

    /**
     * Enable 5e variant diagonal movement (alternating 1-2-1-2 averages to 1.5).
     */
    public void setAlternatingDiagonals(boolean enabled) {
        this.diagonalCost = enabled ? 1.5 : 1.0;
    }

    public int getDefaultMoveSpeed() {
        return defaultMoveSpeed;
    }

    public void setDefaultMoveSpeed(int defaultMoveSpeed) {
        this.defaultMoveSpeed = defaultMoveSpeed;
    }

    public int getMaxPathLength() {
        return maxPathLength;
    }

    public void setMaxPathLength(int maxPathLength) {
        this.maxPathLength = maxPathLength;
    }

    public boolean isAllowVerticalMovement() {
        return allowVerticalMovement;
    }

    public void setAllowVerticalMovement(boolean allowVerticalMovement) {
        this.allowVerticalMovement = allowVerticalMovement;
    }

    public int getMaxStepHeight() {
        return maxStepHeight;
    }

    public void setMaxStepHeight(int maxStepHeight) {
        this.maxStepHeight = maxStepHeight;
    }
}

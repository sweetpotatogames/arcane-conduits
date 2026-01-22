package com.example.dnd.camera;

import java.util.UUID;

/**
 * Stores per-player camera state including pan, rotation, tilt, and zoom values.
 */
public class CameraState {
    private final UUID playerId;

    // Pan offset (XZ plane, camera-relative)
    private float panOffsetX = 0.0f;
    private float panOffsetZ = 0.0f;

    // Rotation
    private float yaw = 0.0f;                                      // radians
    private float pitch = (float) (-Math.PI / 2);                  // -90° default (straight down)
    private static final float MIN_PITCH = (float) (-Math.PI / 2); // -90°
    private static final float MAX_PITCH = (float) (-Math.PI / 6); // -30°

    // Zoom / distance
    private float distance = 25.0f;
    private static final float MIN_DISTANCE = 10.0f;
    private static final float MAX_DISTANCE = 40.0f;

    // View mode
    private CameraViewMode viewMode = CameraViewMode.TOPDOWN;

    // Input tracking for drag operations
    private boolean isPanning = false;
    private boolean isRotating = false;
    private long lastDragStartTime = 0;
    private float dragAccumulatedX = 0.0f;
    private float dragAccumulatedY = 0.0f;

    // Sensitivity constants
    private static final float PAN_SENSITIVITY = 0.05f;
    private static final float YAW_SENSITIVITY = 0.005f;
    private static final float PITCH_SENSITIVITY = 0.005f;

    public CameraState(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    // === Pan Methods ===

    public float getPanOffsetX() {
        return panOffsetX;
    }

    public float getPanOffsetZ() {
        return panOffsetZ;
    }

    /**
     * Adjust the pan offset by the given delta values.
     * Values are in camera-relative space (will be converted to world space when applied).
     */
    public void adjustPan(float dx, float dz) {
        this.panOffsetX += dx * PAN_SENSITIVITY;
        this.panOffsetZ += dz * PAN_SENSITIVITY;
        updateViewMode();
    }

    /**
     * Adjust pan by a fixed amount in a cardinal direction.
     */
    public void panDirection(float dx, float dz) {
        this.panOffsetX += dx;
        this.panOffsetZ += dz;
        updateViewMode();
    }

    public void resetPan() {
        this.panOffsetX = 0.0f;
        this.panOffsetZ = 0.0f;
    }

    // === Rotation Methods ===

    public float getYaw() {
        return yaw;
    }

    /**
     * Adjust the yaw (horizontal rotation) by the given delta in radians.
     */
    public void adjustYaw(float delta) {
        this.yaw += delta * YAW_SENSITIVITY;
        // Normalize yaw to [-PI, PI]
        while (this.yaw > Math.PI) this.yaw -= 2 * (float) Math.PI;
        while (this.yaw < -Math.PI) this.yaw += 2 * (float) Math.PI;
        updateViewMode();
    }

    /**
     * Set yaw to an absolute value in radians.
     */
    public void setYaw(float yaw) {
        this.yaw = yaw;
        // Normalize
        while (this.yaw > Math.PI) this.yaw -= 2 * (float) Math.PI;
        while (this.yaw < -Math.PI) this.yaw += 2 * (float) Math.PI;
        updateViewMode();
    }

    /**
     * Rotate yaw by degrees (for command usage).
     */
    public void rotateYawDegrees(float degrees) {
        float radians = (float) Math.toRadians(degrees);
        this.yaw += radians;
        // Normalize
        while (this.yaw > Math.PI) this.yaw -= 2 * (float) Math.PI;
        while (this.yaw < -Math.PI) this.yaw += 2 * (float) Math.PI;
        updateViewMode();
    }

    // === Pitch/Tilt Methods ===

    public float getPitch() {
        return pitch;
    }

    /**
     * Adjust the pitch (vertical tilt) by the given delta in radians.
     * Clamped between MIN_PITCH (-90°) and MAX_PITCH (-30°).
     */
    public void adjustPitch(float delta) {
        this.pitch += delta * PITCH_SENSITIVITY;
        this.pitch = Math.max(MIN_PITCH, Math.min(MAX_PITCH, this.pitch));
        updateViewMode();
    }

    /**
     * Set pitch to an absolute value in radians, clamped.
     */
    public void setPitch(float pitch) {
        this.pitch = Math.max(MIN_PITCH, Math.min(MAX_PITCH, pitch));
        updateViewMode();
    }

    /**
     * Adjust pitch by degrees (for command usage).
     * Positive = tilt up (less steep), Negative = tilt down (steeper).
     */
    public void adjustPitchDegrees(float degrees) {
        float radians = (float) Math.toRadians(degrees);
        this.pitch += radians;
        this.pitch = Math.max(MIN_PITCH, Math.min(MAX_PITCH, this.pitch));
        updateViewMode();
    }

    // === Distance/Zoom Methods ===

    public float getDistance() {
        return distance;
    }

    /**
     * Adjust the distance (zoom) by the given delta.
     * Clamped between MIN_DISTANCE (10) and MAX_DISTANCE (40).
     */
    public void adjustDistance(float delta) {
        this.distance += delta;
        this.distance = Math.max(MIN_DISTANCE, Math.min(MAX_DISTANCE, this.distance));
        updateViewMode();
    }

    /**
     * Set distance to an absolute value, clamped.
     */
    public void setDistance(float distance) {
        this.distance = Math.max(MIN_DISTANCE, Math.min(MAX_DISTANCE, distance));
        updateViewMode();
    }

    // === View Mode Methods ===

    public CameraViewMode getViewMode() {
        return viewMode;
    }

    public void setViewMode(CameraViewMode mode) {
        this.viewMode = mode;
    }

    /**
     * Update view mode to CUSTOM if values have been manually adjusted.
     */
    private void updateViewMode() {
        if (viewMode != CameraViewMode.FIRST_PERSON) {
            viewMode = CameraViewMode.CUSTOM;
        }
    }

    /**
     * Apply preset values for top-down view.
     */
    public void applyTopDownPreset() {
        this.pitch = (float) (-Math.PI / 2); // -90°
        this.distance = 25.0f;
        this.yaw = 0.0f;
        this.panOffsetX = 0.0f;
        this.panOffsetZ = 0.0f;
        this.viewMode = CameraViewMode.TOPDOWN;
    }

    /**
     * Apply preset values for isometric view.
     */
    public void applyIsometricPreset() {
        this.pitch = (float) (-Math.PI / 4); // -45°
        this.distance = 20.0f;
        this.yaw = 0.0f;
        this.panOffsetX = 0.0f;
        this.panOffsetZ = 0.0f;
        this.viewMode = CameraViewMode.ISOMETRIC;
    }

    /**
     * Apply first-person mode.
     */
    public void applyFirstPersonPreset() {
        this.viewMode = CameraViewMode.FIRST_PERSON;
    }

    /**
     * Reset all adjustments to default top-down view.
     */
    public void reset() {
        applyTopDownPreset();
    }

    // === Input Tracking Methods ===

    public boolean isPanning() {
        return isPanning;
    }

    public void setPanning(boolean panning) {
        this.isPanning = panning;
        if (panning) {
            lastDragStartTime = System.currentTimeMillis();
            dragAccumulatedX = 0.0f;
            dragAccumulatedY = 0.0f;
        }
    }

    public boolean isRotating() {
        return isRotating;
    }

    public void setRotating(boolean rotating) {
        this.isRotating = rotating;
        if (rotating) {
            lastDragStartTime = System.currentTimeMillis();
            dragAccumulatedX = 0.0f;
            dragAccumulatedY = 0.0f;
        }
    }

    public long getLastDragStartTime() {
        return lastDragStartTime;
    }

    public float getDragAccumulatedX() {
        return dragAccumulatedX;
    }

    public float getDragAccumulatedY() {
        return dragAccumulatedY;
    }

    public void accumulateDrag(float dx, float dy) {
        this.dragAccumulatedX += Math.abs(dx);
        this.dragAccumulatedY += Math.abs(dy);
    }

    /**
     * Check if accumulated drag is significant (for distinguishing drag from click).
     */
    public boolean hasSignificantDrag() {
        return dragAccumulatedX > 5.0f || dragAccumulatedY > 5.0f;
    }

    // === Utility Methods ===

    /**
     * Get pitch in degrees for display.
     */
    public float getPitchDegrees() {
        return (float) Math.toDegrees(pitch);
    }

    /**
     * Get yaw in degrees for display.
     */
    public float getYawDegrees() {
        return (float) Math.toDegrees(yaw);
    }

    @Override
    public String toString() {
        return String.format("CameraState{mode=%s, yaw=%.1f°, pitch=%.1f°, distance=%.1f, pan=(%.1f, %.1f)}",
            viewMode, getYawDegrees(), getPitchDegrees(), distance, panOffsetX, panOffsetZ);
    }
}

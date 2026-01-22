package com.example.dnd.camera;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.MouseButtonType;
import com.hypixel.hytale.protocol.MouseButtonState;
import com.hypixel.hytale.protocol.MouseMotionEvent;
import com.hypixel.hytale.protocol.Vector2i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseMotionEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.UUID;

/**
 * Handles mouse input for camera control (pan, rotate, tilt).
 *
 * Controls:
 * - Middle-mouse drag: Pan camera position on XZ plane
 * - Right-mouse drag (horizontal): Rotate camera yaw
 * - Right-mouse drag (vertical): Adjust camera pitch/tilt
 *
 * Combat compatibility:
 * - Quick right-click (minimal movement) is passed through for movement confirm
 * - Only sustained drag with significant movement triggers rotation
 */
public class CameraInputHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final CameraInputHandler INSTANCE = new CameraInputHandler();

    // Threshold to distinguish click from drag (in accumulated pixels)
    private static final float DRAG_THRESHOLD = 8.0f;

    // Minimum time (ms) to hold button before it can be considered a drag
    private static final long DRAG_TIME_THRESHOLD = 100;

    private final CameraManager cameraManager;

    private CameraInputHandler() {
        this.cameraManager = CameraManager.get();
    }

    public static CameraInputHandler get() {
        return INSTANCE;
    }

    /**
     * Handle mouse button events to track drag start/end.
     * Returns true if the event should be consumed (not passed to combat handler).
     */
    public boolean onMouseButton(PlayerMouseButtonEvent event) {
        MouseButtonType buttonType = event.getMouseButton().mouseButtonType;
        MouseButtonState buttonState = event.getMouseButton().state;

        PlayerRef playerRef = event.getPlayerRefComponent();
        UUID playerId = playerRef.getUuid();
        CameraState state = cameraManager.getOrCreateCameraState(playerId);

        // Handle middle mouse button for panning
        if (buttonType == MouseButtonType.Middle) {
            if (buttonState == MouseButtonState.Pressed) {
                state.setPanning(true);
                LOGGER.atFine().log("Camera pan started for %s", playerRef.getUsername());
                return true; // Consume the event
            } else if (buttonState == MouseButtonState.Released) {
                state.setPanning(false);
                LOGGER.atFine().log("Camera pan ended for %s", playerRef.getUsername());
                return true; // Consume the event
            }
        }

        // Handle right mouse button for rotation/tilt
        if (buttonType == MouseButtonType.Right) {
            if (buttonState == MouseButtonState.Pressed) {
                state.setRotating(true);
                LOGGER.atFine().log("Camera rotation tracking started for %s", playerRef.getUsername());
                // Don't consume - let combat handler also see the press
                return false;
            } else if (buttonState == MouseButtonState.Released) {
                boolean wasRotating = state.isRotating();
                boolean hadSignificantDrag = state.hasSignificantDrag();
                state.setRotating(false);

                if (wasRotating && hadSignificantDrag) {
                    // This was a camera rotation, consume the release
                    LOGGER.atFine().log("Camera rotation ended for %s (was dragging)", playerRef.getUsername());
                    return true;
                }
                // Small or no movement - let it pass through as a click for combat
                LOGGER.atFine().log("Right-click passed through for %s (no significant drag)", playerRef.getUsername());
                return false;
            }
        }

        return false;
    }

    /**
     * Handle mouse motion events for camera drag operations.
     */
    public void onMouseMotion(PlayerMouseMotionEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        PlayerRef playerRef = player.getPlayerRef();
        UUID playerId = playerRef.getUuid();
        CameraState state = cameraManager.getCameraState(playerId);

        if (state == null) return;

        // Skip if in first-person mode
        if (state.getViewMode() == CameraViewMode.FIRST_PERSON) return;

        MouseMotionEvent motion = event.getMouseMotion();
        if (motion == null || motion.relativeMotion == null) return;

        Vector2i relativeMotion = motion.relativeMotion;
        float dx = relativeMotion.x;
        float dy = relativeMotion.y;

        // Track accumulated drag distance
        if (state.isPanning() || state.isRotating()) {
            state.accumulateDrag(dx, dy);
        }

        // Handle panning (middle mouse drag)
        if (state.isPanning()) {
            handlePan(playerRef, state, dx, dy);
            event.setCancelled(true);
        }

        // Handle rotation/tilt (right mouse drag) - only if significant movement
        if (state.isRotating() && state.hasSignificantDrag()) {
            handleRotation(playerRef, state, dx, dy);
            event.setCancelled(true);
        }
    }

    /**
     * Handle camera panning from middle-mouse drag.
     */
    private void handlePan(PlayerRef playerRef, CameraState state, float dx, float dy) {
        // Pan on XZ plane (dx = X, dy = Z in screen space)
        // Invert because dragging right should move view left (camera moves right)
        state.adjustPan(-dx, -dy);
        cameraManager.applyCamera(playerRef, state);
    }

    /**
     * Handle camera rotation and tilt from right-mouse drag.
     */
    private void handleRotation(PlayerRef playerRef, CameraState state, float dx, float dy) {
        // Horizontal drag = yaw rotation
        if (Math.abs(dx) > 0.1f) {
            state.adjustYaw(dx);
        }

        // Vertical drag = pitch adjustment
        if (Math.abs(dy) > 0.1f) {
            state.adjustPitch(-dy); // Invert: drag up = tilt up (less steep)
        }

        cameraManager.applyCamera(playerRef, state);
    }

    /**
     * Check if the given button type is currently being used for camera control.
     * Used by combat handler to determine if an input should be blocked.
     */
    public boolean isButtonUsedForCamera(UUID playerId, MouseButtonType buttonType) {
        CameraState state = cameraManager.getCameraState(playerId);
        if (state == null) return false;

        if (buttonType == MouseButtonType.Middle && state.isPanning()) {
            return true;
        }

        if (buttonType == MouseButtonType.Right && state.isRotating() && state.hasSignificantDrag()) {
            return true;
        }

        return false;
    }

    /**
     * Check if right-click should be treated as camera rotation (not combat action).
     * Returns true if the player is currently performing a camera rotation drag.
     */
    public boolean isRightClickForCamera(UUID playerId) {
        CameraState state = cameraManager.getCameraState(playerId);
        if (state == null) return false;

        return state.isRotating() && state.hasSignificantDrag();
    }
}

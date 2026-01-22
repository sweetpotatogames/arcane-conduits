package com.example.dnd.camera;

import com.hypixel.hytale.protocol.ClientCameraView;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.MouseInputType;
import com.hypixel.hytale.protocol.MovementForceRotationType;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.PositionDistanceOffsetType;
import com.hypixel.hytale.protocol.RotationType;
import com.hypixel.hytale.protocol.ServerCameraSettings;
import com.hypixel.hytale.protocol.Vector3f;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages camera settings for the D&D top-down view.
 * Supports pan, rotate, tilt, zoom, and first-person view modes.
 */
public class CameraManager {
    private static final CameraManager INSTANCE = new CameraManager();

    // Per-player camera states
    private final Map<UUID, CameraState> cameraStates = new ConcurrentHashMap<>();

    // Camera configuration defaults
    private static final float DEFAULT_DISTANCE = 25.0F;
    private static final float TOPDOWN_PITCH = (float) (-Math.PI / 2); // Straight down
    private static final float ISOMETRIC_PITCH = (float) (-Math.PI / 4); // 45 degrees
    private static final float ISOMETRIC_DISTANCE = 20.0F;
    private static final float LERP_SPEED = 0.15F;

    private CameraManager() {}

    public static CameraManager get() {
        return INSTANCE;
    }

    /**
     * Get or create a camera state for the given player.
     */
    public CameraState getOrCreateCameraState(UUID playerId) {
        return cameraStates.computeIfAbsent(playerId, CameraState::new);
    }

    /**
     * Get the camera state for a player (may return null if not initialized).
     */
    public CameraState getCameraState(UUID playerId) {
        return cameraStates.get(playerId);
    }

    /**
     * Remove the camera state for a player (e.g., on disconnect).
     */
    public void removeCameraState(UUID playerId) {
        cameraStates.remove(playerId);
    }

    /**
     * Apply the camera settings based on the player's current camera state.
     */
    public void applyCamera(PlayerRef playerRef) {
        UUID playerId = playerRef.getUuid();
        CameraState state = getOrCreateCameraState(playerId);
        applyCamera(playerRef, state);
    }

    /**
     * Apply the given camera state to the player.
     */
    public void applyCamera(PlayerRef playerRef, CameraState state) {
        if (state.getViewMode() == CameraViewMode.FIRST_PERSON) {
            applyFirstPersonCamera(playerRef);
        } else {
            ServerCameraSettings settings = buildSettings(state);
            playerRef.getPacketHandler().writeNoCache(
                new SetServerCamera(ClientCameraView.Custom, true, settings)
            );
        }
    }

    /**
     * Build ServerCameraSettings from the given CameraState.
     */
    private ServerCameraSettings buildSettings(CameraState state) {
        ServerCameraSettings settings = new ServerCameraSettings();

        // Smooth camera movement
        settings.positionLerpSpeed = LERP_SPEED;
        settings.rotationLerpSpeed = LERP_SPEED;

        // Distance from player
        settings.distance = state.getDistance();

        // Third-person view settings
        settings.isFirstPerson = false;
        settings.displayCursor = true;
        settings.displayReticle = false;

        // Eye offset for better positioning
        settings.eyeOffset = true;
        settings.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffset;

        // Custom rotation (yaw and pitch from state)
        settings.rotationType = RotationType.Custom;
        settings.rotation = new Direction(state.getYaw(), state.getPitch(), 0.0F);
        settings.movementForceRotationType = MovementForceRotationType.Custom;

        // Calculate world-space position offset from camera-relative pan
        settings.positionOffset = calculateWorldOffset(state);

        // Mouse input for clicking on the ground plane
        settings.mouseInputType = MouseInputType.LookAtPlane;
        settings.planeNormal = new Vector3f(0.0F, 1.0F, 0.0F);

        // Enable mouse motion events for drag handling
        settings.sendMouseMotion = true;

        return settings;
    }

    /**
     * Calculate world-space position offset from camera-relative pan values.
     * Applies yaw rotation to convert camera-relative to world-space.
     */
    private Position calculateWorldOffset(CameraState state) {
        float panX = state.getPanOffsetX();
        float panZ = state.getPanOffsetZ();

        if (panX == 0 && panZ == 0) {
            return null;
        }

        // Rotate pan offset by yaw to get world-space offset
        float yaw = state.getYaw();
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);

        double worldX = panX * cos - panZ * sin;
        double worldZ = panX * sin + panZ * cos;

        return new Position(worldX, 0.0, worldZ);
    }

    /**
     * Apply the top-down CRPG camera to a player.
     * This is a convenience method that sets the preset and applies it.
     */
    public static void applyTopDownCamera(PlayerRef playerRef) {
        CameraManager manager = get();
        CameraState state = manager.getOrCreateCameraState(playerRef.getUuid());
        state.applyTopDownPreset();
        manager.applyCamera(playerRef, state);
    }

    /**
     * Apply the isometric camera to a player.
     */
    public static void applyIsometricCamera(PlayerRef playerRef) {
        CameraManager manager = get();
        CameraState state = manager.getOrCreateCameraState(playerRef.getUuid());
        state.applyIsometricPreset();
        manager.applyCamera(playerRef, state);
    }

    /**
     * Apply first-person camera to a player.
     */
    public static void applyFirstPersonCamera(PlayerRef playerRef) {
        ServerCameraSettings settings = new ServerCameraSettings();
        settings.isFirstPerson = true;
        settings.displayCursor = false;
        settings.displayReticle = true;
        settings.positionLerpSpeed = LERP_SPEED;
        settings.rotationLerpSpeed = LERP_SPEED;

        playerRef.getPacketHandler().writeNoCache(
            new SetServerCamera(ClientCameraView.Custom, true, settings)
        );
    }

    /**
     * Reset the camera to default third-person view.
     */
    public static void resetCamera(PlayerRef playerRef) {
        CameraManager manager = get();
        CameraState state = manager.getOrCreateCameraState(playerRef.getUuid());
        state.reset();

        playerRef.getPacketHandler().writeNoCache(
            new SetServerCamera(ClientCameraView.ThirdPerson, false, null)
        );
    }

    /**
     * Reset to the D&D top-down camera (not the default third-person).
     */
    public static void resetToDndCamera(PlayerRef playerRef) {
        applyTopDownCamera(playerRef);
    }

    // === Convenience methods for commands ===

    /**
     * Zoom in (decrease distance) by the specified amount.
     */
    public void zoomIn(PlayerRef playerRef, float amount) {
        CameraState state = getOrCreateCameraState(playerRef.getUuid());
        state.adjustDistance(-amount);
        applyCamera(playerRef, state);
    }

    /**
     * Zoom out (increase distance) by the specified amount.
     */
    public void zoomOut(PlayerRef playerRef, float amount) {
        CameraState state = getOrCreateCameraState(playerRef.getUuid());
        state.adjustDistance(amount);
        applyCamera(playerRef, state);
    }

    /**
     * Set zoom to a specific distance.
     */
    public void setZoom(PlayerRef playerRef, float distance) {
        CameraState state = getOrCreateCameraState(playerRef.getUuid());
        state.setDistance(distance);
        applyCamera(playerRef, state);
    }

    /**
     * Rotate camera left (decrease yaw) by degrees.
     */
    public void rotateLeft(PlayerRef playerRef, float degrees) {
        CameraState state = getOrCreateCameraState(playerRef.getUuid());
        state.rotateYawDegrees(-degrees);
        applyCamera(playerRef, state);
    }

    /**
     * Rotate camera right (increase yaw) by degrees.
     */
    public void rotateRight(PlayerRef playerRef, float degrees) {
        CameraState state = getOrCreateCameraState(playerRef.getUuid());
        state.rotateYawDegrees(degrees);
        applyCamera(playerRef, state);
    }

    /**
     * Set rotation to a specific yaw in degrees.
     */
    public void setRotation(PlayerRef playerRef, float degrees) {
        CameraState state = getOrCreateCameraState(playerRef.getUuid());
        state.setYaw((float) Math.toRadians(degrees));
        applyCamera(playerRef, state);
    }

    /**
     * Tilt camera up (increase pitch, less steep) by degrees.
     */
    public void tiltUp(PlayerRef playerRef, float degrees) {
        CameraState state = getOrCreateCameraState(playerRef.getUuid());
        state.adjustPitchDegrees(degrees);
        applyCamera(playerRef, state);
    }

    /**
     * Tilt camera down (decrease pitch, more steep) by degrees.
     */
    public void tiltDown(PlayerRef playerRef, float degrees) {
        CameraState state = getOrCreateCameraState(playerRef.getUuid());
        state.adjustPitchDegrees(-degrees);
        applyCamera(playerRef, state);
    }

    /**
     * Set tilt to a specific pitch in degrees (negative values, e.g., -45 to -90).
     */
    public void setTilt(PlayerRef playerRef, float degrees) {
        CameraState state = getOrCreateCameraState(playerRef.getUuid());
        state.setPitch((float) Math.toRadians(degrees));
        applyCamera(playerRef, state);
    }

    /**
     * Pan camera in a cardinal direction.
     */
    public void pan(PlayerRef playerRef, float dx, float dz) {
        CameraState state = getOrCreateCameraState(playerRef.getUuid());
        state.panDirection(dx, dz);
        applyCamera(playerRef, state);
    }

    /**
     * Reset pan offset to center.
     */
    public void resetPan(PlayerRef playerRef) {
        CameraState state = getOrCreateCameraState(playerRef.getUuid());
        state.resetPan();
        applyCamera(playerRef, state);
    }

    /**
     * Toggle first-person view mode.
     * Returns true if now in first-person, false if exited to previous mode.
     */
    public boolean toggleFirstPerson(PlayerRef playerRef) {
        CameraState state = getOrCreateCameraState(playerRef.getUuid());

        if (state.getViewMode() == CameraViewMode.FIRST_PERSON) {
            // Exit first-person, return to top-down
            state.applyTopDownPreset();
            applyCamera(playerRef, state);
            return false;
        } else {
            // Enter first-person
            state.applyFirstPersonPreset();
            applyCamera(playerRef, state);
            return true;
        }
    }
}

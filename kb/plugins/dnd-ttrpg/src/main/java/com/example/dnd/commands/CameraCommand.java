package com.example.dnd.commands;

import com.example.dnd.camera.CameraManager;
import com.example.dnd.camera.CameraState;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Command to control camera modes and adjustments.
 *
 * Usage:
 *   /dnd camera [mode]              - View presets: topdown, isometric, firstperson, reset
 *   /dnd camera zoom [in|out|VALUE] - Adjust zoom distance
 *   /dnd camera rotate [left|right|DEGREES] - Rotate camera yaw
 *   /dnd camera tilt [up|down|DEGREES]      - Adjust camera pitch
 *   /dnd camera pan [north|south|east|west|reset] - Pan camera position
 *   /dnd camera settings            - Display current camera configuration
 */
public class CameraCommand extends AbstractPlayerCommand {
    private static final float DEFAULT_ZOOM_STEP = 2.0f;
    private static final float DEFAULT_ROTATE_STEP = 15.0f;
    private static final float DEFAULT_TILT_STEP = 10.0f;
    private static final float DEFAULT_PAN_STEP = 5.0f;

    private final DefaultArg<String> actionArg;
    private final DefaultArg<String> valueArg;

    public CameraCommand() {
        super("camera", "server.commands.dnd.camera.desc");
        actionArg = withDefaultArg("action", "Camera action or mode",
            ArgTypes.STRING, "topdown", "topdown");
        valueArg = withDefaultArg("value", "Value for the action",
            ArgTypes.STRING, "", "");
    }

    @Override
    protected void execute(
        @Nonnull CommandContext context,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PlayerRef playerRef,
        @Nonnull World world
    ) {
        String action = context.get(actionArg).toLowerCase();
        String value = context.get(valueArg).toLowerCase();

        CameraManager cameraManager = CameraManager.get();

        switch (action) {
            // === View Mode Presets ===
            case "topdown" -> {
                CameraManager.applyTopDownCamera(playerRef);
                playerRef.sendMessage(Message.raw("[D&D] Switched to top-down camera view"));
            }
            case "isometric" -> {
                CameraManager.applyIsometricCamera(playerRef);
                playerRef.sendMessage(Message.raw("[D&D] Switched to isometric camera view"));
            }
            case "firstperson", "fp" -> {
                boolean isFirstPerson = cameraManager.toggleFirstPerson(playerRef);
                if (isFirstPerson) {
                    playerRef.sendMessage(Message.raw("[D&D] Switched to first-person view"));
                } else {
                    playerRef.sendMessage(Message.raw("[D&D] Exited first-person view"));
                }
            }
            case "reset" -> {
                CameraManager.resetToDndCamera(playerRef);
                playerRef.sendMessage(Message.raw("[D&D] Camera reset to default top-down view"));
            }

            // === Zoom Commands ===
            case "zoom" -> handleZoom(playerRef, cameraManager, value);

            // === Rotate Commands ===
            case "rotate" -> handleRotate(playerRef, cameraManager, value);

            // === Tilt Commands ===
            case "tilt" -> handleTilt(playerRef, cameraManager, value);

            // === Pan Commands ===
            case "pan" -> handlePan(playerRef, cameraManager, value);

            // === Settings Display ===
            case "settings", "info", "status" -> displaySettings(playerRef, cameraManager);

            // === Help ===
            case "help" -> displayHelp(playerRef);

            default -> {
                playerRef.sendMessage(Message.raw("[D&D] Unknown camera action: " + action));
                displayHelp(playerRef);
            }
        }
    }

    private void handleZoom(PlayerRef playerRef, CameraManager cameraManager, String value) {
        switch (value) {
            case "in", "+" -> {
                cameraManager.zoomIn(playerRef, DEFAULT_ZOOM_STEP);
                CameraState state = cameraManager.getCameraState(playerRef.getUuid());
                playerRef.sendMessage(Message.raw(String.format("[D&D] Zoomed in (distance: %.1f)", state.getDistance())));
            }
            case "out", "-" -> {
                cameraManager.zoomOut(playerRef, DEFAULT_ZOOM_STEP);
                CameraState state = cameraManager.getCameraState(playerRef.getUuid());
                playerRef.sendMessage(Message.raw(String.format("[D&D] Zoomed out (distance: %.1f)", state.getDistance())));
            }
            case "" -> {
                playerRef.sendMessage(Message.raw("[D&D] Usage: /dnd camera zoom <in|out|VALUE>"));
                playerRef.sendMessage(Message.raw("[D&D]   in/out: Adjust by 2 units"));
                playerRef.sendMessage(Message.raw("[D&D]   VALUE: Set exact distance (10-40)"));
            }
            default -> {
                try {
                    float distance = Float.parseFloat(value);
                    cameraManager.setZoom(playerRef, distance);
                    CameraState state = cameraManager.getCameraState(playerRef.getUuid());
                    playerRef.sendMessage(Message.raw(String.format("[D&D] Zoom set to %.1f", state.getDistance())));
                } catch (NumberFormatException e) {
                    playerRef.sendMessage(Message.raw("[D&D] Invalid zoom value: " + value + ". Use: in, out, or a number (10-40)"));
                }
            }
        }
    }

    private void handleRotate(PlayerRef playerRef, CameraManager cameraManager, String value) {
        switch (value) {
            case "left", "l", "ccw" -> {
                cameraManager.rotateLeft(playerRef, DEFAULT_ROTATE_STEP);
                CameraState state = cameraManager.getCameraState(playerRef.getUuid());
                playerRef.sendMessage(Message.raw(String.format("[D&D] Rotated left (yaw: %.1f°)", state.getYawDegrees())));
            }
            case "right", "r", "cw" -> {
                cameraManager.rotateRight(playerRef, DEFAULT_ROTATE_STEP);
                CameraState state = cameraManager.getCameraState(playerRef.getUuid());
                playerRef.sendMessage(Message.raw(String.format("[D&D] Rotated right (yaw: %.1f°)", state.getYawDegrees())));
            }
            case "" -> {
                playerRef.sendMessage(Message.raw("[D&D] Usage: /dnd camera rotate <left|right|DEGREES>"));
                playerRef.sendMessage(Message.raw("[D&D]   left/right: Rotate by 15°"));
                playerRef.sendMessage(Message.raw("[D&D]   DEGREES: Set exact rotation angle"));
            }
            default -> {
                try {
                    float degrees = Float.parseFloat(value);
                    cameraManager.setRotation(playerRef, degrees);
                    CameraState state = cameraManager.getCameraState(playerRef.getUuid());
                    playerRef.sendMessage(Message.raw(String.format("[D&D] Rotation set to %.1f°", state.getYawDegrees())));
                } catch (NumberFormatException e) {
                    playerRef.sendMessage(Message.raw("[D&D] Invalid rotation value: " + value + ". Use: left, right, or degrees"));
                }
            }
        }
    }

    private void handleTilt(PlayerRef playerRef, CameraManager cameraManager, String value) {
        switch (value) {
            case "up", "+" -> {
                cameraManager.tiltUp(playerRef, DEFAULT_TILT_STEP);
                CameraState state = cameraManager.getCameraState(playerRef.getUuid());
                playerRef.sendMessage(Message.raw(String.format("[D&D] Tilted up (pitch: %.1f°)", state.getPitchDegrees())));
            }
            case "down", "-" -> {
                cameraManager.tiltDown(playerRef, DEFAULT_TILT_STEP);
                CameraState state = cameraManager.getCameraState(playerRef.getUuid());
                playerRef.sendMessage(Message.raw(String.format("[D&D] Tilted down (pitch: %.1f°)", state.getPitchDegrees())));
            }
            case "" -> {
                playerRef.sendMessage(Message.raw("[D&D] Usage: /dnd camera tilt <up|down|DEGREES>"));
                playerRef.sendMessage(Message.raw("[D&D]   up/down: Adjust by 10°"));
                playerRef.sendMessage(Message.raw("[D&D]   DEGREES: Set exact pitch (-30 to -90)"));
            }
            default -> {
                try {
                    float degrees = Float.parseFloat(value);
                    cameraManager.setTilt(playerRef, degrees);
                    CameraState state = cameraManager.getCameraState(playerRef.getUuid());
                    playerRef.sendMessage(Message.raw(String.format("[D&D] Tilt set to %.1f°", state.getPitchDegrees())));
                } catch (NumberFormatException e) {
                    playerRef.sendMessage(Message.raw("[D&D] Invalid tilt value: " + value + ". Use: up, down, or degrees (-30 to -90)"));
                }
            }
        }
    }

    private void handlePan(PlayerRef playerRef, CameraManager cameraManager, String value) {
        switch (value) {
            case "north", "n", "forward" -> {
                cameraManager.pan(playerRef, 0, -DEFAULT_PAN_STEP);
                playerRef.sendMessage(Message.raw("[D&D] Panned north"));
            }
            case "south", "s", "back" -> {
                cameraManager.pan(playerRef, 0, DEFAULT_PAN_STEP);
                playerRef.sendMessage(Message.raw("[D&D] Panned south"));
            }
            case "east", "e", "right" -> {
                cameraManager.pan(playerRef, DEFAULT_PAN_STEP, 0);
                playerRef.sendMessage(Message.raw("[D&D] Panned east"));
            }
            case "west", "w", "left" -> {
                cameraManager.pan(playerRef, -DEFAULT_PAN_STEP, 0);
                playerRef.sendMessage(Message.raw("[D&D] Panned west"));
            }
            case "reset", "center" -> {
                cameraManager.resetPan(playerRef);
                playerRef.sendMessage(Message.raw("[D&D] Pan reset to center"));
            }
            case "" -> {
                playerRef.sendMessage(Message.raw("[D&D] Usage: /dnd camera pan <north|south|east|west|reset>"));
                playerRef.sendMessage(Message.raw("[D&D]   Pans camera 5 blocks in the specified direction"));
            }
            default -> playerRef.sendMessage(Message.raw("[D&D] Invalid pan direction: " + value + ". Use: north, south, east, west, or reset"));
        }
    }

    private void displaySettings(PlayerRef playerRef, CameraManager cameraManager) {
        CameraState state = cameraManager.getOrCreateCameraState(playerRef.getUuid());

        playerRef.sendMessage(Message.raw("[D&D] === Camera Settings ==="));
        playerRef.sendMessage(Message.raw(String.format("[D&D] Mode: %s", state.getViewMode())));
        playerRef.sendMessage(Message.raw(String.format("[D&D] Distance: %.1f (range: 10-40)", state.getDistance())));
        playerRef.sendMessage(Message.raw(String.format("[D&D] Yaw: %.1f°", state.getYawDegrees())));
        playerRef.sendMessage(Message.raw(String.format("[D&D] Pitch: %.1f° (range: -30 to -90)", state.getPitchDegrees())));
        playerRef.sendMessage(Message.raw(String.format("[D&D] Pan Offset: (%.1f, %.1f)", state.getPanOffsetX(), state.getPanOffsetZ())));
    }

    private void displayHelp(PlayerRef playerRef) {
        playerRef.sendMessage(Message.raw("[D&D] === Camera Commands ==="));
        playerRef.sendMessage(Message.raw("[D&D] /dnd camera topdown     - Top-down view preset"));
        playerRef.sendMessage(Message.raw("[D&D] /dnd camera isometric   - Isometric view preset"));
        playerRef.sendMessage(Message.raw("[D&D] /dnd camera firstperson - Toggle first-person"));
        playerRef.sendMessage(Message.raw("[D&D] /dnd camera reset       - Reset to defaults"));
        playerRef.sendMessage(Message.raw("[D&D] /dnd camera zoom <in|out|#> - Adjust distance"));
        playerRef.sendMessage(Message.raw("[D&D] /dnd camera rotate <left|right|#> - Rotate"));
        playerRef.sendMessage(Message.raw("[D&D] /dnd camera tilt <up|down|#> - Tilt angle"));
        playerRef.sendMessage(Message.raw("[D&D] /dnd camera pan <dir|reset> - Pan position"));
        playerRef.sendMessage(Message.raw("[D&D] /dnd camera settings    - Show current config"));
    }
}

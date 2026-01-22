package com.example.dnd;

import com.example.dnd.camera.CameraInputHandler;
import com.example.dnd.character.CharacterSheet;
import com.example.dnd.combat.CombatEventHandler;
import com.example.dnd.combat.TurnManager;
import com.example.dnd.commands.DndCommands;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseMotionEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * D&D TTRPG Prototype Plugin
 *
 * Features:
 * - Top-down CRPG camera view
 * - Turn-based combat system
 * - Character sheet with dice rolling
 */
public class DndPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static DndPlugin instance;

    // Managers
    private TurnManager turnManager;
    private CombatEventHandler combatEventHandler;
    private CameraInputHandler cameraInputHandler;

    // Character sheets per player
    private final Map<UUID, CharacterSheet> characterSheets = new ConcurrentHashMap<>();

    public DndPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        LOGGER.atInfo().log("[D&D] D&D TTRPG Plugin loading - version %s",
            getManifest().getVersion().toString());
    }

    public static DndPlugin get() {
        return instance;
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("[D&D] Setting up D&D TTRPG Plugin");

        // Initialize managers
        turnManager = TurnManager.get();
        combatEventHandler = new CombatEventHandler(turnManager);
        cameraInputHandler = CameraInputHandler.get();

        // Register commands
        getCommandRegistry().registerCommand(new DndCommands(this, turnManager));

        // Register event listeners for camera input (handles pan/rotate/tilt via mouse drag)
        getEventRegistry().register(PlayerMouseButtonEvent.class, this::onMouseButton);
        getEventRegistry().register(PlayerMouseMotionEvent.class, cameraInputHandler::onMouseMotion);

        LOGGER.atInfo().log("[D&D] Commands registered: /dnd camera, /dnd initiative, /dnd turn, /dnd roll, /dnd sheet, /dnd combat, /dnd move, /dnd target");
    }

    /**
     * Handle mouse button events - routes to camera handler first, then combat handler.
     */
    private void onMouseButton(PlayerMouseButtonEvent event) {
        // Let camera handler process first (for pan/rotate drag tracking)
        boolean consumedByCamera = cameraInputHandler.onMouseButton(event);

        if (consumedByCamera) {
            event.setCancelled(true);
            return;
        }

        // Pass to combat handler if not consumed by camera
        combatEventHandler.onPlayerMouseButton(event);
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("[D&D] D&D TTRPG Plugin started!");
        LOGGER.atInfo().log("[D&D] Use '/dnd camera topdown' to enable top-down view");
        LOGGER.atInfo().log("[D&D] Use '/dnd sheet' to open your character sheet");
        LOGGER.atInfo().log("[D&D] Use '/dnd combat' to open the combat control panel");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("[D&D] D&D TTRPG Plugin shutting down");
        characterSheets.clear();
    }

    /**
     * Get or create a character sheet for a player.
     */
    public CharacterSheet getOrCreateCharacterSheet(UUID playerId) {
        return characterSheets.computeIfAbsent(playerId, k -> new CharacterSheet());
    }

    /**
     * Get a character sheet for a player (may be null).
     */
    public CharacterSheet getCharacterSheet(UUID playerId) {
        return characterSheets.get(playerId);
    }

    /**
     * Get the turn manager.
     */
    public TurnManager getTurnManager() {
        return turnManager;
    }
}

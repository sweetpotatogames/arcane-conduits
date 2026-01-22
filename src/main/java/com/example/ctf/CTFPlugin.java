package com.example.ctf;

import com.example.ctf.arena.ArenaConfig;
import com.example.ctf.arena.ArenaManager;
import com.example.ctf.editor.CTFMarkerProvider;
import com.example.ctf.editor.CTFSetupCycleInteraction;
import com.example.ctf.editor.CTFSetupInteraction;
import com.example.ctf.match.MatchManager;
import com.example.ctf.protection.BuildingProtectionHandler;
import com.example.ctf.spawn.CTFRespawnController;
import com.example.ctf.spawn.CTFSpawnProvider;
import com.example.ctf.team.TeamManager;
import com.example.ctf.team.TeamVisualManager;
import com.example.ctf.ui.CTFAnnouncementManager;
import com.example.ctf.ui.CTFScoreHud;
import com.example.ctf.ui.CTFSoundManager;
import com.hypixel.hytale.server.core.universe.world.events.AddWorldEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Capture The Flag plugin for Hytale.
 *
 * Features:
 * - Flag items that players can carry
 * - Movement restrictions when carrying a flag:
 *   - Reduced movement speed
 *   - Disabled mantling (2-block climbs)
 *   - Acts as two-handed (blocks off-hand use)
 * - Team management with spawn points
 * - Capture zones for scoring
 * - Protected regions (no building)
 * - Match system with scoring
 */
public class CTFPlugin extends JavaPlugin {

    // Static instance for access from interactions
    private static CTFPlugin instance;

    // Core systems
    private FlagCarrierManager flagCarrierManager;
    private FlagEventHandler flagEventHandler;

    // Arena systems
    private Config<ArenaConfig> arenaConfig;
    private ArenaManager arenaManager;
    private BuildingProtectionHandler buildingProtectionHandler;

    // Team & match systems
    private TeamManager teamManager;
    private TeamVisualManager teamVisualManager;
    private MatchManager matchManager;

    // Spawn system
    private CTFSpawnProvider spawnProvider;
    private CTFRespawnController respawnController;

    // Editor integration
    private CTFMarkerProvider markerProvider;

    // UI & announcements
    private CTFAnnouncementManager announcementManager;
    private CTFSoundManager soundManager;

    /**
     * Gets the CTF plugin instance.
     * @return The plugin instance, or null if not loaded
     */
    @Nullable
    public static CTFPlugin get() {
        return instance;
    }

    public CTFPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;

        // Register arena config BEFORE setup is called
        arenaConfig = withConfig("arena", ArenaConfig.CODEC);

        // Register CTF interactions
        registerInteractions();
    }

    /**
     * Registers CTF interactions with the interaction codec.
     */
    private void registerInteractions() {
        Interaction.CODEC.register("CTFSetupInteraction", CTFSetupInteraction.class, CTFSetupInteraction.CODEC);
        Interaction.CODEC.register("CTFSetupCycleInteraction", CTFSetupCycleInteraction.class, CTFSetupCycleInteraction.CODEC);
        getLogger().atInfo().log("Registered CTF interactions");
    }

    @Override
    protected void setup() {
        getLogger().atInfo().log("CTF Plugin setting up...");

        // Initialize team manager first (other systems may depend on it)
        teamManager = new TeamManager(this);

        // Initialize team visual manager
        teamVisualManager = new TeamVisualManager(this);

        // Initialize arena manager (uses config loaded by withConfig)
        arenaManager = new ArenaManager(this, arenaConfig);

        // Initialize match manager
        matchManager = new MatchManager(this);

        // Initialize the flag carrier manager
        flagCarrierManager = new FlagCarrierManager(this);

        // Initialize event handlers
        flagEventHandler = new FlagEventHandler(this);
        // TODO: BuildingProtectionHandler needs ECS system rewrite
        // buildingProtectionHandler = new BuildingProtectionHandler(this);

        // Initialize spawn system
        spawnProvider = new CTFSpawnProvider(this, null);
        respawnController = new CTFRespawnController(this);

        // Initialize marker provider for editor integration
        markerProvider = new CTFMarkerProvider(this);

        // Initialize announcement and sound managers
        announcementManager = new CTFAnnouncementManager(this);
        soundManager = new CTFSoundManager(this);

        // Register marker provider for all worlds (existing and new)
        getEventRegistry().registerGlobal(AddWorldEvent.class, event -> {
            event.getWorld().getWorldMapManager().addMarkerProvider("ctf", markerProvider);
            getLogger().atInfo().log("Registered CTF markers for world: {}", event.getWorld().getName());
        });

        // Register commands
        getCommandRegistry().registerCommand(new CTFCommands(this));

        getLogger().atInfo().log("CTF Plugin setup complete!");
    }

    @Override
    protected void start() {
        getLogger().atInfo().log("CTF Plugin started!");

        // Log arena configuration status
        ArenaConfig config = arenaManager.getConfig();
        getLogger().atInfo().log("Arena config loaded:");
        getLogger().atInfo().log("  - Red spawns: {}", config.getRedSpawns().length);
        getLogger().atInfo().log("  - Blue spawns: {}", config.getBlueSpawns().length);
        getLogger().atInfo().log("  - Red capture zone: {}", config.getRedCaptureZone() != null ? "configured" : "not set");
        getLogger().atInfo().log("  - Blue capture zone: {}", config.getBlueCaptureZone() != null ? "configured" : "not set");
        getLogger().atInfo().log("  - Protected regions: {}", config.getProtectedRegions().length);
        getLogger().atInfo().log("  - Score limit: {}", config.getScoreLimit());
    }

    @Override
    protected void shutdown() {
        getLogger().atInfo().log("CTF Plugin shutting down...");

        // Save arena config
        if (arenaManager != null) {
            arenaManager.save();
        }

        // Clean up - restore all flag carriers to normal movement
        if (flagCarrierManager != null) {
            flagCarrierManager.cleanup();
        }

        // Clear team visuals
        if (teamVisualManager != null) {
            teamVisualManager.cleanup();
        }

        // Clear team assignments
        if (teamManager != null) {
            teamManager.clearTeams();
        }

        // Clean up score HUDs
        CTFScoreHud.cleanup();

        // Clear static instance
        instance = null;
    }

    // ==================== Getters ====================

    @Nonnull
    public FlagCarrierManager getFlagCarrierManager() {
        return flagCarrierManager;
    }

    @Nullable
    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    @Nullable
    public TeamManager getTeamManager() {
        return teamManager;
    }

    @Nullable
    public TeamVisualManager getTeamVisualManager() {
        return teamVisualManager;
    }

    @Nullable
    public MatchManager getMatchManager() {
        return matchManager;
    }

    @Nullable
    public CTFSpawnProvider getSpawnProvider() {
        return spawnProvider;
    }

    @Nullable
    public CTFRespawnController getRespawnController() {
        return respawnController;
    }

    @Nullable
    public CTFAnnouncementManager getAnnouncementManager() {
        return announcementManager;
    }

    @Nullable
    public CTFSoundManager getSoundManager() {
        return soundManager;
    }
}

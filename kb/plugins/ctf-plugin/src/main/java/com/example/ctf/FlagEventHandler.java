package com.example.ctf;

import com.example.ctf.match.MatchManager;
import com.example.ctf.match.MatchState;
import com.example.ctf.team.TeamManager;
import com.example.ctf.team.TeamVisualManager;
import com.example.ctf.ui.CTFScoreHud;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Handles CTF flag-related events:
 * - Drop item requests (G key) to drop the flag
 *
 * Note: Death detection is handled via FlagCarrierManager's tick system
 * which checks for dead carriers using the DeathComponent.
 */
public class FlagEventHandler {

    private final CTFPlugin plugin;

    public FlagEventHandler(@Nonnull CTFPlugin plugin) {
        this.plugin = plugin;
        registerEvents();
    }

    private void registerEvents() {
        // TODO: DropItemEvent.PlayerRequest is an ECS event and needs EntityEventSystem integration
        // The flag drop via G key feature is disabled until ECS event handling is implemented
        // plugin.getEventRegistry().register(
        //     DropItemEvent.PlayerRequest.class,
        //     this::onDropItemRequest
        // );

        // Listen for player connect/disconnect for HUD and team management
        plugin.getEventRegistry().registerGlobal(
            PlayerConnectEvent.class,
            this::onPlayerConnect
        );

        plugin.getEventRegistry().registerGlobal(
            PlayerDisconnectEvent.class,
            this::onPlayerDisconnect
        );

        plugin.getLogger().atInfo().log("FlagEventHandler: Event listeners registered");
    }

    /*
     * TODO: ECS Event Handler - Disabled until EntityEventSystem integration
     * Called when a player presses G to drop an item.
     * If they're dropping the flag item, we handle it specially.
     *
     * ECS events like DropItemEvent.PlayerRequest don't have getEntityRef()
     * because they're processed through the ECS system where entity context
     * is provided differently (via system iteration).
     *
     * To implement this feature, we need to:
     * 1. Register an EntityEventSystem that handles DropItemEvent.PlayerRequest
     * 2. The system will receive entity context through the ECS framework
     * 3. Check if the dropping player is a flag carrier and handle accordingly
     */

    /**
     * Called when a player connects to the server.
     * Shows score HUD if a match is active.
     */
    private void onPlayerConnect(@Nonnull PlayerConnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        if (playerRef == null) {
            return;
        }

        // If a match is active, show the score HUD to the new player
        MatchManager matchManager = plugin.getMatchManager();
        if (matchManager != null && matchManager.getState() == MatchState.ACTIVE) {
            int redScore = matchManager.getScore(FlagTeam.RED);
            int blueScore = matchManager.getScore(FlagTeam.BLUE);

            CTFScoreHud.showToPlayer(playerRef, plugin);
            CTFScoreHud hud = CTFScoreHud.getHud(playerRef.getUuid());
            if (hud != null) {
                hud.updateScore(redScore, blueScore);
            }
        }

        plugin.getLogger().atInfo().log("Player {} connected", playerRef.getUuid());
    }

    /**
     * Called when a player disconnects from the server.
     * Cleans up their HUD, team assignment, and visual effects.
     */
    private void onPlayerDisconnect(@Nonnull PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        if (playerRef == null) {
            return;
        }

        UUID playerUuid = playerRef.getUuid();

        // Clean up score HUD tracking
        CTFScoreHud.onPlayerDisconnect(playerUuid);

        // Clean up team visual tracking
        TeamVisualManager visualManager = plugin.getTeamVisualManager();
        if (visualManager != null) {
            visualManager.onPlayerDisconnect(playerUuid);
        }

        // Handle team manager disconnect (removes from team)
        TeamManager teamManager = plugin.getTeamManager();
        if (teamManager != null) {
            teamManager.handlePlayerDisconnect(playerUuid);
        }

        // Drop flag if carrying one
        FlagCarrierManager flagManager = plugin.getFlagCarrierManager();
        if (flagManager.isCarryingFlag(playerUuid)) {
            // Get position for drop
            Ref<EntityStore> entityRef = playerRef.getReference();
            Vector3d dropPosition = new Vector3d(0, 0, 0);
            if (entityRef != null && entityRef.isValid()) {
                TransformComponent transform = entityRef.getStore()
                    .getComponent(entityRef, TransformComponent.getComponentType());
                if (transform != null) {
                    dropPosition = transform.getPosition();
                }
            }
            flagManager.dropFlag(playerUuid, dropPosition);
        }

        plugin.getLogger().atInfo().log("Player {} disconnected, cleaned up CTF state", playerUuid);
    }

}

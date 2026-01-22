package com.example.dnd.combat;

import com.example.dnd.ui.CombatHud;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages turn-based combat across worlds.
 */
public class TurnManager {
    private static TurnManager instance;

    // Combat state per world
    private final Map<UUID, CombatState> worldCombatStates = new ConcurrentHashMap<>();

    // Active combat HUDs per player (player UUID -> HUD)
    private final Map<UUID, CombatHud> activeHuds = new ConcurrentHashMap<>();

    private TurnManager() {}

    /**
     * Get the singleton instance.
     */
    public static TurnManager get() {
        if (instance == null) {
            instance = new TurnManager();
        }
        return instance;
    }

    /**
     * Get or create the combat state for a world.
     */
    public CombatState getCombatState(World world) {
        return worldCombatStates.computeIfAbsent(
            world.getWorldConfig().getUuid(),
            k -> new CombatState()
        );
    }

    /**
     * Get combat state by world UUID directly.
     */
    public CombatState getCombatState(UUID worldId) {
        return worldCombatStates.computeIfAbsent(worldId, k -> new CombatState());
    }

    /**
     * Check if it's a player's turn in the given world.
     */
    public boolean isPlayerTurn(World world, UUID playerId) {
        CombatState state = worldCombatStates.get(world.getWorldConfig().getUuid());
        return state != null && state.isPlayerTurn(playerId);
    }

    /**
     * Check if combat is active in a world.
     */
    public boolean isCombatActive(World world) {
        CombatState state = worldCombatStates.get(world.getWorldConfig().getUuid());
        return state != null && state.isCombatActive();
    }

    /**
     * Clear combat state for a world.
     */
    public void clearCombatState(World world) {
        worldCombatStates.remove(world.getWorldConfig().getUuid());
    }

    // ========== HUD Lifecycle Management ==========

    /**
     * Show combat HUDs for all players in the initiative order.
     */
    public void showCombatHuds(World world) {
        CombatState state = getCombatState(world);
        if (!state.isCombatActive()) return;

        for (UUID playerId : state.getInitiativeOrder()) {
            Player player = findPlayerByUuid(world, playerId);
            if (player != null) {
                showHudForPlayer(player, world);
            }
        }
    }

    /**
     * Show the combat HUD for a specific player.
     */
    public void showHudForPlayer(Player player, World world) {
        PlayerRef playerRef = player.getPlayerRef();
        CombatHud hud = new CombatHud(playerRef, this, world);
        activeHuds.put(playerRef.getUuid(), hud);
        player.getHudManager().setCustomHud(playerRef, hud);
    }

    /**
     * Hide combat HUDs for all players in the world.
     */
    public void hideCombatHuds(World world) {
        CombatState state = getCombatState(world);

        for (UUID playerId : state.getInitiativeOrder()) {
            Player player = findPlayerByUuid(world, playerId);
            if (player != null) {
                hideHudForPlayer(player);
            }
        }
    }

    /**
     * Hide the combat HUD for a specific player.
     */
    public void hideHudForPlayer(Player player) {
        PlayerRef playerRef = player.getPlayerRef();
        CombatHud hud = activeHuds.remove(playerRef.getUuid());
        if (hud != null) {
            player.getHudManager().setCustomHud(playerRef, null);
        }
    }

    /**
     * Refresh all active combat HUDs in a world.
     */
    public void refreshAllHuds(World world) {
        CombatState state = getCombatState(world);

        for (UUID playerId : state.getInitiativeOrder()) {
            CombatHud hud = activeHuds.get(playerId);
            if (hud != null) {
                hud.refresh();
            }
        }
    }

    /**
     * Get the active HUD for a player.
     */
    public CombatHud getHud(UUID playerId) {
        return activeHuds.get(playerId);
    }

    /**
     * Find a player in the world by UUID.
     */
    private Player findPlayerByUuid(World world, UUID playerId) {
        for (Player player : world.getPlayers()) {
            if (player.getPlayerRef().getUuid().equals(playerId)) {
                return player;
            }
        }
        return null;
    }
}

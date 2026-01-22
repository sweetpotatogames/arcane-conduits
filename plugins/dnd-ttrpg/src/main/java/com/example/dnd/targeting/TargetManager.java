package com.example.dnd.targeting;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages target selection for players during combat.
 * Each player can have one selected target at a time.
 */
public class TargetManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static TargetManager instance;

    // Player UUID -> Selected target entity reference
    private final Map<UUID, Ref<EntityStore>> playerTargets = new ConcurrentHashMap<>();

    // Highlighter for visual feedback
    private final TargetHighlighter highlighter;

    private TargetManager() {
        this.highlighter = new TargetHighlighter();
    }

    public static TargetManager get() {
        if (instance == null) {
            instance = new TargetManager();
        }
        return instance;
    }

    /**
     * Select a target for a player.
     *
     * @param player The player selecting the target
     * @param targetEntity The entity to target
     * @param world The world
     * @return true if target was successfully selected
     */
    public boolean selectTarget(@Nonnull Player player, @Nonnull Entity targetEntity, @Nonnull World world) {
        UUID playerId = player.getPlayerRef().getUuid();
        Ref<EntityStore> targetRef = targetEntity.getReference();

        // Don't allow targeting self
        if (targetRef.equals(player.getEntityRef())) {
            player.getPlayerRef().sendMessage(Message.raw("[D&D] You cannot target yourself!"));
            return false;
        }

        // Check if this is the same target (toggle off)
        Ref<EntityStore> currentTarget = playerTargets.get(playerId);
        if (currentTarget != null && currentTarget.equals(targetRef)) {
            clearTarget(playerId, world);
            player.getPlayerRef().sendMessage(Message.raw("[D&D] Target cleared."));
            return false;
        }

        // Clear previous target highlight
        if (currentTarget != null) {
            highlighter.clearHighlight(playerId);
        }

        // Set new target
        playerTargets.put(playerId, targetRef);

        // Get target info for feedback
        ComponentAccessor<EntityStore> accessor = world.getComponentAccessor();
        TargetInfo info = TargetInfo.fromEntityRef(targetRef, accessor);

        if (info != null && info.isValid()) {
            // Show highlight on new target
            highlighter.highlightTarget(player, info, world);

            // Notify player
            player.getPlayerRef().sendMessage(Message.raw(
                String.format("[D&D] Target: %s (HP: %.0f/%.0f)",
                    info.getName(), info.getCurrentHp(), info.getMaxHp())
            ));

            LOGGER.atFine().log("Player %s selected target: %s",
                player.getPlayerRef().getUsername(), info.getName());

            return true;
        } else {
            // Invalid target
            playerTargets.remove(playerId);
            player.getPlayerRef().sendMessage(Message.raw("[D&D] Invalid target."));
            return false;
        }
    }

    /**
     * Clear the target for a player.
     */
    public void clearTarget(@Nonnull UUID playerId, @Nonnull World world) {
        Ref<EntityStore> removed = playerTargets.remove(playerId);
        if (removed != null) {
            highlighter.clearHighlight(playerId);
            LOGGER.atFine().log("Cleared target for player %s", playerId);
        }
    }

    /**
     * Clear all targets (e.g., when combat ends).
     */
    public void clearAllTargets(@Nonnull World world) {
        for (UUID playerId : playerTargets.keySet()) {
            highlighter.clearHighlight(playerId);
        }
        playerTargets.clear();
        LOGGER.atInfo().log("Cleared all targets");
    }

    /**
     * Get the current target info for a player.
     * Returns null if no target or target is invalid/dead.
     *
     * @param playerId The player's UUID
     * @param world The world
     * @return TargetInfo or null
     */
    @Nullable
    public TargetInfo getTargetInfo(@Nonnull UUID playerId, @Nonnull World world) {
        Ref<EntityStore> targetRef = playerTargets.get(playerId);
        if (targetRef == null || !targetRef.isValid()) {
            // Auto-clear invalid targets
            if (targetRef != null) {
                playerTargets.remove(playerId);
                highlighter.clearHighlight(playerId);
            }
            return null;
        }

        ComponentAccessor<EntityStore> accessor = world.getComponentAccessor();
        TargetInfo info = TargetInfo.fromEntityRef(targetRef, accessor);

        // Auto-clear dead targets
        if (info == null || !info.isAlive()) {
            playerTargets.remove(playerId);
            highlighter.clearHighlight(playerId);
            return null;
        }

        return info;
    }

    /**
     * Get the raw entity reference for a player's target.
     */
    @Nullable
    public Ref<EntityStore> getTargetRef(@Nonnull UUID playerId) {
        return playerTargets.get(playerId);
    }

    /**
     * Check if a player has a target selected.
     */
    public boolean hasTarget(@Nonnull UUID playerId) {
        Ref<EntityStore> ref = playerTargets.get(playerId);
        return ref != null && ref.isValid();
    }

    /**
     * Check if an entity is targeted by any player.
     */
    public boolean isEntityTargeted(@Nonnull Ref<EntityStore> entityRef) {
        for (Ref<EntityStore> target : playerTargets.values()) {
            if (target != null && target.equals(entityRef)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a clicked entity is a valid target (NPC, not player).
     */
    public boolean isValidTarget(@Nonnull Entity entity, @Nonnull World world) {
        Ref<EntityStore> ref = entity.getReference();
        ComponentAccessor<EntityStore> accessor = world.getComponentAccessor();

        // Check if it's an NPC
        NPCEntity npc = accessor.getComponent(ref, NPCEntity.getComponentType());
        if (npc != null) {
            return true;
        }

        // Could add other valid target types here (e.g., destructible objects)
        return false;
    }

    /**
     * Get the highlighter for external use.
     */
    public TargetHighlighter getHighlighter() {
        return highlighter;
    }

    /**
     * Update highlights for all targets (call periodically).
     */
    public void refreshHighlights(@Nonnull World world) {
        for (Map.Entry<UUID, Ref<EntityStore>> entry : playerTargets.entrySet()) {
            UUID playerId = entry.getKey();
            Ref<EntityStore> targetRef = entry.getValue();

            if (targetRef == null || !targetRef.isValid()) {
                continue;
            }

            // Find the player
            Player player = findPlayerByUuid(world, playerId);
            if (player == null) {
                continue;
            }

            // Get fresh target info
            TargetInfo info = TargetInfo.fromEntityRef(targetRef, world.getComponentAccessor());
            if (info != null && info.isAlive()) {
                highlighter.highlightTarget(player, info, world);
            }
        }
    }

    /**
     * Find a player in the world by UUID.
     */
    @Nullable
    private Player findPlayerByUuid(@Nonnull World world, @Nonnull UUID playerId) {
        for (Player player : world.getPlayers()) {
            if (player.getPlayerRef().getUuid().equals(playerId)) {
                return player;
            }
        }
        return null;
    }
}

package com.example.dnd.targeting;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages target selection for players during combat.
 * Each player can have one selected target at a time.
 *
 * TODO: Target info retrieval requires ECS integration.
 * The world.getComponentAccessor() pattern doesn't exist.
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
     *
     * TODO: Full target info retrieval requires ECS integration.
     */
    @SuppressWarnings("deprecation")
    public boolean selectTarget(@Nonnull Player player, @Nonnull Entity targetEntity, @Nonnull World world) {
        UUID playerId = player.getPlayerRef().getUuid();
        Ref<EntityStore> targetRef = targetEntity.getReference();

        // TODO: Self-targeting check disabled - player.getEntityRef() doesn't exist
        // Would need to compare entity references to prevent self-targeting

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

        // TODO: Get target info for feedback - needs ECS integration
        // The following pattern doesn't work:
        // - world.getComponentAccessor()
        // - TargetInfo.fromEntityRef(targetRef, accessor)
        //
        // For now, just confirm target selection without detailed info
        player.getPlayerRef().sendMessage(Message.raw("[D&D] Target selected."));

        LOGGER.atFine().log("Player %s selected target (entity ref: %s)",
            player.getPlayerRef().getUsername(), targetRef);

        return true;
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
     *
     * TODO: Returns null until ECS integration is implemented.
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

        // TODO: Target info retrieval disabled - needs ECS API research
        // The following pattern doesn't work:
        // - world.getComponentAccessor()
        // - TargetInfo.fromEntityRef(targetRef, accessor)
        //
        // For now, return null (no detailed target info available)
        return null;
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
     *
     * TODO: NPC type checking disabled - needs ECS API research.
     * Currently allows all entities as valid targets.
     */
    @SuppressWarnings("unused")
    public boolean isValidTarget(@Nonnull Entity entity, @Nonnull World world) {
        // TODO: Proper NPC type checking disabled - needs ECS API research
        // The following pattern doesn't work:
        // - world.getComponentAccessor()
        // - accessor.getComponent(ref, NPCEntity.getComponentType())
        //
        // For now, allow any entity as a valid target
        return entity != null && entity.getReference() != null && entity.getReference().isValid();
    }

    /**
     * Get the highlighter for external use.
     */
    public TargetHighlighter getHighlighter() {
        return highlighter;
    }

    /**
     * Update highlights for all targets (call periodically).
     *
     * TODO: Highlight refresh disabled - needs ECS API research.
     */
    @SuppressWarnings("deprecation")
    public void refreshHighlights(@Nonnull World world) {
        // TODO: Highlight refresh disabled - needs ECS API research
        // The following pattern doesn't work:
        // - world.getComponentAccessor()
        // - TargetInfo.fromEntityRef(targetRef, accessor)
        //
        // Highlights are currently not refreshed automatically
    }

    /**
     * Find a player in the world by UUID.
     */
    @Nullable
    @SuppressWarnings("deprecation")
    private Player findPlayerByUuid(@Nonnull World world, @Nonnull UUID playerId) {
        for (Player player : world.getPlayers()) {
            if (player.getPlayerRef().getUuid().equals(playerId)) {
                return player;
            }
        }
        return null;
    }
}

package com.example.dnd.targeting;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Renders particle highlights around targeted entities.
 * Each player can have one highlighted target - the highlight is only visible to that player.
 *
 * TODO: Particle rendering requires ECS integration that needs API research.
 * The world.getComponentAccessor() and player.getEntityRef() patterns have changed.
 */
public class TargetHighlighter {
    // Particle system for target highlight
    private static final String TARGET_PARTICLE = "Block/Block_Top_Glow";

    // Highlight colors - Color uses bytes (0-255), not floats
    // Orange: RGB(255, 102, 26)
    private static final Color TARGET_COLOR = new Color((byte)255, (byte)102, (byte)26);
    // Red for low HP: RGB(255, 26, 26)
    private static final Color TARGET_COLOR_LOW_HP = new Color((byte)255, (byte)26, (byte)26);

    // Number of particles in the ring around target
    private static final int RING_PARTICLE_COUNT = 8;
    private static final double RING_RADIUS = 0.6;
    private static final float PARTICLE_SCALE = 0.4f;

    // Refresh timing
    private static final long REFRESH_INTERVAL_MS = 400;

    // Per-player highlight state
    private final Map<UUID, HighlightState> activeHighlights = new HashMap<>();

    /**
     * Highlight a target for a specific player.
     *
     * @param player The player viewing the highlight
     * @param targetInfo The target information
     * @param world The world
     */
    @SuppressWarnings("deprecation")
    public void highlightTarget(@Nonnull Player player, @Nonnull TargetInfo targetInfo, @Nonnull World world) {
        UUID playerId = player.getPlayerRef().getUuid();

        // Create or update highlight state
        HighlightState state = activeHighlights.get(playerId);
        Ref<EntityStore> targetRef = targetInfo.getEntityRef();

        if (state != null && state.targetRef != null && state.targetRef.equals(targetRef)) {
            // Same target, just refresh if needed
            if (state.needsRefresh()) {
                spawnHighlightParticles(player, targetInfo, world);
                state.markRefreshed();
            }
            return;
        }

        // New target - update state
        state = new HighlightState(targetRef);
        activeHighlights.put(playerId, state);

        // Spawn particles
        spawnHighlightParticles(player, targetInfo, world);
        state.markRefreshed();
    }

    /**
     * Clear the highlight for a player.
     */
    public void clearHighlight(@Nonnull UUID playerId) {
        activeHighlights.remove(playerId);
        // Particles naturally despawn
    }

    /**
     * Clear all highlights.
     */
    public void clearAllHighlights() {
        activeHighlights.clear();
    }

    /**
     * Spawn highlight particles around the target.
     * Creates a ring of particles at the target's feet.
     *
     * TODO: Implement particle spawning once ECS patterns are researched.
     * The current ParticleUtil API requires ComponentAccessor and viewer list
     * which needs proper ECS integration.
     */
    private void spawnHighlightParticles(Player player, TargetInfo targetInfo, World world) {
        if (!targetInfo.isValid()) return;

        Vector3d targetPos = targetInfo.getPosition();

        // TODO: Particle rendering disabled - needs ECS API research
        // The following patterns no longer work:
        // - world.getComponentAccessor()
        // - player.getEntityRef()
        //
        // Needs investigation into correct patterns for:
        // - Getting ComponentAccessor from World
        // - Getting entity Ref from Player
        // - Using ParticleUtil.spawnParticleEffect

        // Placeholder: Log that we would spawn particles
        // Logger can be added if needed for debugging
    }

    /**
     * Check if a player has an active highlight.
     */
    public boolean hasActiveHighlight(@Nonnull UUID playerId) {
        return activeHighlights.containsKey(playerId);
    }

    /**
     * Internal state for tracking highlights.
     */
    private static class HighlightState {
        final Ref<EntityStore> targetRef;
        long lastRefreshTime;

        HighlightState(Ref<EntityStore> targetRef) {
            this.targetRef = targetRef;
            this.lastRefreshTime = 0;
        }

        boolean needsRefresh() {
            return System.currentTimeMillis() - lastRefreshTime >= REFRESH_INTERVAL_MS;
        }

        void markRefreshed() {
            lastRefreshTime = System.currentTimeMillis();
        }
    }
}

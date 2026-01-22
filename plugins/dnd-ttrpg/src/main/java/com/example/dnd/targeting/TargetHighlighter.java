package com.example.dnd.targeting;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Renders particle highlights around targeted entities.
 * Each player can have one highlighted target - the highlight is only visible to that player.
 */
public class TargetHighlighter {
    // Particle system for target highlight
    private static final String TARGET_PARTICLE = "Block/Block_Top_Glow";

    // Highlight colors
    private static final Color TARGET_COLOR = new Color(1.0f, 0.4f, 0.1f, 0.9f);  // Orange
    private static final Color TARGET_COLOR_LOW_HP = new Color(1.0f, 0.1f, 0.1f, 0.9f);  // Red for low HP

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
     */
    private void spawnHighlightParticles(Player player, TargetInfo targetInfo, World world) {
        if (!targetInfo.isValid()) return;

        Vector3d targetPos = targetInfo.getPosition();
        ComponentAccessor<EntityStore> accessor = world.getComponentAccessor();
        List<Ref<EntityStore>> viewers = Collections.singletonList(player.getEntityRef());

        // Choose color based on HP
        Color color = targetInfo.getHpPercent() < 0.25f ? TARGET_COLOR_LOW_HP : TARGET_COLOR;

        // Spawn ring of particles around target
        for (int i = 0; i < RING_PARTICLE_COUNT; i++) {
            double angle = (2 * Math.PI * i) / RING_PARTICLE_COUNT;
            double offsetX = Math.cos(angle) * RING_RADIUS;
            double offsetZ = Math.sin(angle) * RING_RADIUS;

            ParticleUtil.spawnParticleEffect(
                TARGET_PARTICLE,
                targetPos.getX() + offsetX,
                targetPos.getY() + 0.1,  // Slightly above ground
                targetPos.getZ() + offsetZ,
                0.0f, 0.0f, 0.0f,  // No rotation
                PARTICLE_SCALE,
                color,
                null,  // No source entity
                viewers,
                accessor
            );
        }

        // Add center marker for emphasis
        ParticleUtil.spawnParticleEffect(
            TARGET_PARTICLE,
            targetPos.getX(),
            targetPos.getY() + 0.05,
            targetPos.getZ(),
            0.0f, 0.0f, 0.0f,
            PARTICLE_SCALE * 0.5f,
            color,
            null,
            viewers,
            accessor
        );
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

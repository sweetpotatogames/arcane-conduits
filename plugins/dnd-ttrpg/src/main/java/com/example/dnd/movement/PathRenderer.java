package com.example.dnd.movement;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.*;

/**
 * Visualizes the planned movement path with particle effects.
 *
 * Uses vanilla Hytale particle systems to show:
 * - Waypoint markers along the path
 * - Destination marker at the end
 * - Path line/trail effect
 */
public class PathRenderer {
    // Particle system IDs (vanilla Hytale particles)
    private static final String PATH_PARTICLE = "Block/Block_Top_Glow";
    private static final String DESTINATION_PARTICLE = "Block/Block_Top_Glow";

    // Cached path rendering to avoid re-rendering same path
    private final Map<UUID, PathRenderState> renderStates = new HashMap<>();

    /**
     * Render the path for a player's movement state.
     *
     * @param player The player to render for
     * @param state The movement state with path information
     * @param world The world
     */
    public void renderPath(Player player, MovementState state, World world) {
        UUID playerId = player.getPlayerRef().getUuid();

        // Check if we need to update the rendering
        PathRenderState renderState = renderStates.get(playerId);
        List<Vector3i> path = state.getPathWaypoints();

        if (renderState != null && renderState.matches(path, state.getPlannedDestination())) {
            // Path hasn't changed, just refresh particles
            refreshParticles(player, renderState, world);
            return;
        }

        // Clear old rendering
        clearPath(playerId);

        if (state.getPlannedDestination() == null || path.isEmpty()) {
            return;
        }

        // Create new render state
        renderState = new PathRenderState(path, state.getPlannedDestination());
        renderStates.put(playerId, renderState);

        // Spawn particles along the path
        spawnPathParticles(player, path, state.canReachDestination(), world);
    }

    /**
     * Clear the rendered path for a player.
     */
    public void clearPath(UUID playerId) {
        PathRenderState state = renderStates.remove(playerId);
        if (state != null) {
            state.clear();
        }
        // Particles naturally despawn, but we track state for efficiency
    }

    /**
     * Spawn particles along the path.
     */
    private void spawnPathParticles(Player player, List<Vector3i> path, boolean isReachable, World world) {
        if (path.isEmpty()) return;

        Ref<EntityStore> playerRef = player.getEntityRef();
        List<Ref<EntityStore>> viewers = Collections.singletonList(playerRef);
        ComponentAccessor<EntityStore> accessor = world.getComponentAccessor();

        // Determine color based on reachability
        // Green for reachable, red for unreachable
        Color pathColor = isReachable
            ? new Color(0.3f, 0.8f, 0.3f, 0.8f)   // Green
            : new Color(0.8f, 0.3f, 0.3f, 0.8f);   // Red

        Color destColor = isReachable
            ? new Color(0.2f, 1.0f, 0.2f, 1.0f)    // Bright green
            : new Color(1.0f, 0.2f, 0.2f, 1.0f);   // Bright red

        // Spawn waypoint particles (skip first - that's the start position)
        for (int i = 1; i < path.size(); i++) {
            Vector3i waypoint = path.get(i);
            Vector3d particlePos = new Vector3d(
                waypoint.x + 0.5,  // Center of block
                waypoint.y + 0.1,  // Slightly above ground
                waypoint.z + 0.5
            );

            // Use different appearance for destination vs waypoints
            boolean isDestination = (i == path.size() - 1);
            String particleId = isDestination ? DESTINATION_PARTICLE : PATH_PARTICLE;
            Color color = isDestination ? destColor : pathColor;
            float scale = isDestination ? 0.8f : 0.5f;

            // Spawn the particle
            ParticleUtil.spawnParticleEffect(
                particleId,
                particlePos.getX(),
                particlePos.getY(),
                particlePos.getZ(),
                0.0f, 0.0f, 0.0f,  // No rotation
                scale,
                color,
                null,  // No source entity
                viewers,
                accessor
            );
        }
    }

    /**
     * Refresh particles for an existing path (called periodically).
     */
    private void refreshParticles(Player player, PathRenderState state, World world) {
        // Re-spawn particles to keep the path visible
        // Particles have a duration and will fade, so we periodically refresh
        if (state.needsRefresh()) {
            List<Vector3i> path = state.getPath();
            boolean reachable = true; // Assume reachable for refresh
            spawnPathParticles(player, path, reachable, world);
            state.markRefreshed();
        }
    }

    /**
     * Update all active path renderings (call from tick).
     */
    public void tick(World world, Map<UUID, Player> activePlayers) {
        for (Map.Entry<UUID, PathRenderState> entry : renderStates.entrySet()) {
            UUID playerId = entry.getKey();
            Player player = activePlayers.get(playerId);
            if (player != null) {
                refreshParticles(player, entry.getValue(), world);
            }
        }
    }

    /**
     * Internal state for tracking rendered paths.
     */
    private static class PathRenderState {
        private final List<Vector3i> path;
        private final Vector3i destination;
        private long lastRefreshTime;
        private static final long REFRESH_INTERVAL_MS = 500; // Refresh every 500ms

        PathRenderState(List<Vector3i> path, Vector3i destination) {
            this.path = new ArrayList<>(path);
            this.destination = destination;
            this.lastRefreshTime = System.currentTimeMillis();
        }

        boolean matches(List<Vector3i> otherPath, Vector3i otherDest) {
            if (otherDest == null || !otherDest.equals(destination)) {
                return false;
            }
            if (otherPath == null || otherPath.size() != path.size()) {
                return false;
            }
            return path.equals(otherPath);
        }

        boolean needsRefresh() {
            return System.currentTimeMillis() - lastRefreshTime >= REFRESH_INTERVAL_MS;
        }

        void markRefreshed() {
            lastRefreshTime = System.currentTimeMillis();
        }

        List<Vector3i> getPath() {
            return path;
        }

        void clear() {
            // Particles naturally despawn, nothing to actively clear
        }
    }
}

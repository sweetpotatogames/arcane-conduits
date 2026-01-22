package com.example.dnd.movement;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.*;

/**
 * Visualizes the planned movement path with particle effects.
 *
 * Uses vanilla Hytale particle systems to show:
 * - Waypoint markers along the path
 * - Destination marker at the end
 * - Path line/trail effect
 *
 * TODO: Particle rendering requires ECS integration that needs API research.
 * The world.getComponentAccessor() and player.getEntityRef() patterns have changed.
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

    // Path colors - Color uses bytes (0-255), not floats
    // Green for reachable: RGB(77, 204, 77)
    private static final Color PATH_COLOR_REACHABLE = new Color((byte)77, (byte)204, (byte)77);
    // Red for unreachable: RGB(204, 77, 77)
    private static final Color PATH_COLOR_UNREACHABLE = new Color((byte)204, (byte)77, (byte)77);
    // Bright green for destination: RGB(51, 255, 51)
    private static final Color DEST_COLOR_REACHABLE = new Color((byte)51, (byte)255, (byte)51);
    // Bright red for destination: RGB(255, 51, 51)
    private static final Color DEST_COLOR_UNREACHABLE = new Color((byte)255, (byte)51, (byte)51);

    /**
     * Spawn particles along the path.
     *
     * TODO: Implement particle spawning once ECS patterns are researched.
     * The current ParticleUtil API requires ComponentAccessor and viewer list
     * which needs proper ECS integration.
     */
    @SuppressWarnings("unused")
    private void spawnPathParticles(Player player, List<Vector3i> path, boolean isReachable, World world) {
        if (path.isEmpty()) return;

        // TODO: Particle rendering disabled - needs ECS API research
        // The following patterns no longer work:
        // - world.getComponentAccessor()
        // - player.getEntityRef()
        //
        // Needs investigation into correct patterns for:
        // - Getting ComponentAccessor from World
        // - Getting entity Ref from Player
        // - Using ParticleUtil.spawnParticleEffect

        // Placeholder: Colors are defined at class level for when this is implemented
        // Color pathColor = isReachable ? PATH_COLOR_REACHABLE : PATH_COLOR_UNREACHABLE;
        // Color destColor = isReachable ? DEST_COLOR_REACHABLE : DEST_COLOR_UNREACHABLE;
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

package com.arcane.conduits.core.power;

import com.arcane.conduits.blocks.state.ConduitBlockState;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages conduit power networks with caching and lazy recalculation.
 *
 * Networks are identified by any block position within them.
 * When a block is placed or broken, the affected network is marked dirty
 * and recalculated on the next tick.
 */
@SuppressWarnings("deprecation")  // BlockState is deprecated but still functional
public class ConduitNetworkManager {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Minimum ticks between network recalculations (prevents spam).
     */
    private static final long RECALC_COOLDOWN_MS = 50;

    /**
     * Set of positions that need network recalculation.
     */
    private final Set<Vector3i> dirtyPositions = ConcurrentHashMap.newKeySet();

    /**
     * Cache of recently calculated networks.
     * Key: chunk-local position hash, Value: network stats
     */
    private final Map<Long, NetworkCache> networkCache = new ConcurrentHashMap<>();

    /**
     * Timestamp of last recalculation per chunk.
     */
    private final Map<Long, Long> lastRecalcTime = new ConcurrentHashMap<>();

    /**
     * Power propagator for BFS updates.
     */
    private final PowerPropagator propagator;

    /**
     * Reference to the current world (set on first use).
     */
    private World currentWorld;

    public ConduitNetworkManager() {
        this.propagator = new PowerPropagator(this);
    }

    /**
     * Called when a conduit block is placed or broken at the given position.
     * Marks the network for recalculation.
     */
    public void invalidateNetworkAt(Vector3i position) {
        if (position == null) return;

        dirtyPositions.add(position.clone());

        // Invalidate cache for this chunk
        long chunkKey = getChunkKey(position.x >> 5, position.z >> 5);
        networkCache.remove(chunkKey);

        LOGGER.atFine().log("Network invalidated at %s", position);
    }

    /**
     * Called when power changes at a position.
     * Used for tracking and potential optimizations.
     */
    public void onPowerChanged(Vector3i position, int oldPower, int newPower) {
        // Currently just logging - could be used for cascading updates
        LOGGER.atFine().log("Power changed at %s: %d -> %d", position, oldPower, newPower);
    }

    /**
     * Process any pending network recalculations.
     * Should be called periodically (e.g., from a tick handler).
     */
    public void processDirtyNetworks(World world) {
        if (dirtyPositions.isEmpty()) {
            return;
        }

        this.currentWorld = world;

        long now = System.currentTimeMillis();
        Set<Vector3i> toProcess = new HashSet<>(dirtyPositions);
        dirtyPositions.clear();

        for (Vector3i pos : toProcess) {
            long chunkKey = getChunkKey(pos.x >> 5, pos.z >> 5);

            // Check cooldown
            Long lastTime = lastRecalcTime.get(chunkKey);
            if (lastTime != null && (now - lastTime) < RECALC_COOLDOWN_MS) {
                // Re-queue for later
                dirtyPositions.add(pos);
                continue;
            }

            // Recalculate the network
            propagator.recalculateNetwork(world, pos);
            lastRecalcTime.put(chunkKey, now);
        }
    }

    /**
     * Force immediate recalculation of a network.
     */
    public void recalculateNetworkNow(World world, Vector3i position) {
        if (world == null || position == null) return;

        this.currentWorld = world;
        propagator.recalculateNetwork(world, position);

        // Remove from dirty set if present
        dirtyPositions.remove(position);
    }

    /**
     * Propagate power from a source.
     */
    public void propagatePower(World world, Vector3i sourcePos, int power) {
        if (world == null || sourcePos == null) return;

        this.currentWorld = world;
        propagator.propagateFromSource(world, sourcePos, power);
    }

    /**
     * Clear power from a network.
     */
    public void clearPower(World world, Vector3i startPos) {
        if (world == null || startPos == null) return;

        this.currentWorld = world;
        propagator.clearNetwork(world, startPos);
    }

    /**
     * Get debug information about a network at the given position.
     */
    public NetworkDebugInfo getNetworkDebugInfo(World world, Vector3i position) {
        if (world == null || position == null) {
            return new NetworkDebugInfo(0, 0, 0, false);
        }

        // Count connected blocks (BFS)
        Set<Vector3i> visited = new HashSet<>();
        Queue<Vector3i> queue = new ArrayDeque<>();
        int totalPower = 0;
        int sourceCount = 0;

        queue.add(position.clone());

        int[][] directions = {
            {-1, 0, 0}, {1, 0, 0},
            {0, -1, 0}, {0, 1, 0},
            {0, 0, -1}, {0, 0, 1}
        };

        while (!queue.isEmpty() && visited.size() < 1000) {  // Limit for performance
            Vector3i pos = queue.poll();

            if (visited.contains(pos)) continue;

            long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
            WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
            if (chunk == null) continue;

            BlockState state = chunk.getState(pos.x & 31, pos.y, pos.z & 31);
            if (state instanceof ConduitBlockState conduit) {
                visited.add(pos.clone());
                totalPower += conduit.getPowerLevel();

                // Check for power sources (adjacent to this conduit)
                for (int[] dir : directions) {
                    Vector3i neighborPos = new Vector3i(
                        pos.x + dir[0], pos.y + dir[1], pos.z + dir[2]
                    );

                    if (neighborPos.y >= 0 && neighborPos.y < 320 &&
                        !visited.contains(neighborPos)) {
                        queue.add(neighborPos);
                    }
                }
            }
        }

        boolean isDirty = dirtyPositions.stream()
            .anyMatch(p -> visited.contains(p));

        return new NetworkDebugInfo(
            visited.size(),
            totalPower,
            sourceCount,
            isDirty
        );
    }

    /**
     * Shutdown the network manager.
     */
    public void shutdown() {
        dirtyPositions.clear();
        networkCache.clear();
        lastRecalcTime.clear();
        LOGGER.atInfo().log("ConduitNetworkManager shutdown complete");
    }

    // ==================== Helper Methods ====================

    private long getChunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    // ==================== Inner Classes ====================

    /**
     * Cached network information.
     */
    private static class NetworkCache {
        final Set<Vector3i> members;
        final int totalPower;
        final long timestamp;

        NetworkCache(Set<Vector3i> members, int totalPower) {
            this.members = members;
            this.totalPower = totalPower;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isValid() {
            return (System.currentTimeMillis() - timestamp) < 5000;  // 5 second validity
        }
    }

    /**
     * Debug information about a network.
     */
    public static class NetworkDebugInfo {
        public final int blockCount;
        public final int totalPower;
        public final int sourceCount;
        public final boolean isDirty;

        public NetworkDebugInfo(int blockCount, int totalPower, int sourceCount, boolean isDirty) {
            this.blockCount = blockCount;
            this.totalPower = totalPower;
            this.sourceCount = sourceCount;
            this.isDirty = isDirty;
        }

        @Override
        public String toString() {
            return String.format("Network[blocks=%d, totalPower=%d, sources=%d, dirty=%s]",
                blockCount, totalPower, sourceCount, isDirty);
        }
    }
}

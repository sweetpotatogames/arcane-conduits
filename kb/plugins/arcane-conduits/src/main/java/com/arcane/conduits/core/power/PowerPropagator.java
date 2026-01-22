package com.arcane.conduits.core.power;

import com.arcane.conduits.blocks.state.ConduitBlockState;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;

import java.util.*;

/**
 * Handles power propagation through conduit networks using BFS algorithm.
 *
 * Power propagates from sources through conduits with decay:
 * - Each conduit loses power based on its decay rate
 * - Power takes the maximum path (not cumulative)
 * - Networks are traversed breadth-first for efficiency
 */
@SuppressWarnings("deprecation")  // BlockState is deprecated but still functional
public class PowerPropagator {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Maximum propagation depth to prevent infinite loops.
     */
    private static final int MAX_PROPAGATION_DEPTH = 256;

    /**
     * Direction offsets for 6 cardinal directions.
     */
    private static final int[][] DIRECTIONS = {
        {-1, 0, 0},  // -X
        {1, 0, 0},   // +X
        {0, -1, 0},  // -Y
        {0, 1, 0},   // +Y
        {0, 0, -1},  // -Z
        {0, 0, 1}    // +Z
    };

    private final ConduitNetworkManager networkManager;

    public PowerPropagator(ConduitNetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    /**
     * Propagate power from a source position through the network.
     *
     * @param world       The world containing the conduits
     * @param sourcePos   The position of the power source
     * @param sourcePower The power level at the source (0-15)
     */
    public void propagateFromSource(World world, Vector3i sourcePos, int sourcePower) {
        if (world == null || sourcePos == null || sourcePower <= 0) {
            return;
        }

        // BFS queue: position -> power level at that position
        Queue<PropagationEntry> queue = new ArrayDeque<>();
        Map<Vector3i, Integer> visited = new HashMap<>();

        // Start from the source
        queue.add(new PropagationEntry(sourcePos.clone(), sourcePower, 0));

        while (!queue.isEmpty()) {
            PropagationEntry entry = queue.poll();

            // Skip if we've already visited with equal or higher power
            Integer existingPower = visited.get(entry.position);
            if (existingPower != null && existingPower >= entry.power) {
                continue;
            }

            // Skip if too deep
            if (entry.depth >= MAX_PROPAGATION_DEPTH) {
                continue;
            }

            // Mark as visited
            visited.put(entry.position.clone(), entry.power);

            // Update the block's power level
            updateBlockPower(world, entry.position, entry.power);

            // Get the conduit state to determine decay rate
            int decayRate = getDecayRate(world, entry.position);

            // Queue all connected neighbors
            for (int[] dir : DIRECTIONS) {
                Vector3i neighborPos = new Vector3i(
                    entry.position.x + dir[0],
                    entry.position.y + dir[1],
                    entry.position.z + dir[2]
                );

                // Check bounds
                if (neighborPos.y < 0 || neighborPos.y >= 320) {
                    continue;
                }

                // Check if neighbor is a conduit
                if (!isConduit(world, neighborPos)) {
                    continue;
                }

                // Calculate power after decay
                int newPower = Math.max(0, entry.power - decayRate);

                // Only propagate if there's still power
                if (newPower > 0) {
                    // Skip if we've already visited with higher power
                    Integer neighborExisting = visited.get(neighborPos);
                    if (neighborExisting == null || neighborExisting < newPower) {
                        queue.add(new PropagationEntry(neighborPos, newPower, entry.depth + 1));
                    }
                }
            }
        }

        LOGGER.atFine().log("Propagated power from %s: %d blocks updated", sourcePos, visited.size());
    }

    /**
     * Clear power from a network starting at the given position.
     * Used when a power source is removed.
     *
     * @param world    The world
     * @param startPos The starting position
     */
    public void clearNetwork(World world, Vector3i startPos) {
        if (world == null || startPos == null) {
            return;
        }

        // BFS to find all connected conduits
        Queue<Vector3i> queue = new ArrayDeque<>();
        Set<Vector3i> visited = new HashSet<>();

        queue.add(startPos.clone());

        while (!queue.isEmpty()) {
            Vector3i pos = queue.poll();

            if (visited.contains(pos)) {
                continue;
            }
            visited.add(pos.clone());

            // Clear power at this position
            updateBlockPower(world, pos, 0);

            // Queue neighbors
            for (int[] dir : DIRECTIONS) {
                Vector3i neighborPos = new Vector3i(
                    pos.x + dir[0],
                    pos.y + dir[1],
                    pos.z + dir[2]
                );

                if (neighborPos.y >= 0 && neighborPos.y < 320 &&
                    !visited.contains(neighborPos) &&
                    isConduit(world, neighborPos)) {
                    queue.add(neighborPos);
                }
            }
        }

        LOGGER.atFine().log("Cleared power from %s: %d blocks cleared", startPos, visited.size());
    }

    /**
     * Recalculate power for the entire network containing the given position.
     * Finds all power sources in the network and propagates from each.
     *
     * @param world    The world
     * @param startPos Any position in the network
     */
    public void recalculateNetwork(World world, Vector3i startPos) {
        if (world == null || startPos == null) {
            return;
        }

        // First, discover the entire network and find power sources
        Set<Vector3i> networkBlocks = new HashSet<>();
        List<PowerSourceEntry> powerSources = new ArrayList<>();

        discoverNetwork(world, startPos, networkBlocks, powerSources);

        // Clear all blocks in the network first
        for (Vector3i pos : networkBlocks) {
            updateBlockPower(world, pos, 0);
        }

        // Then propagate from each power source
        for (PowerSourceEntry source : powerSources) {
            propagateFromSource(world, source.position, source.power);
        }

        LOGGER.atInfo().log("Recalculated network from %s: %d blocks, %d sources",
            startPos, networkBlocks.size(), powerSources.size());
    }

    /**
     * Discover all blocks in a network and identify power sources.
     */
    private void discoverNetwork(World world, Vector3i startPos,
                                  Set<Vector3i> networkBlocks,
                                  List<PowerSourceEntry> powerSources) {
        Queue<Vector3i> queue = new ArrayDeque<>();
        queue.add(startPos.clone());

        while (!queue.isEmpty()) {
            Vector3i pos = queue.poll();

            if (networkBlocks.contains(pos)) {
                continue;
            }

            // Check if this is a conduit
            if (!isConduit(world, pos)) {
                // Check if it's a power source
                int sourcePower = getPowerSourceLevel(world, pos);
                if (sourcePower > 0) {
                    powerSources.add(new PowerSourceEntry(pos.clone(), sourcePower));
                }
                continue;
            }

            networkBlocks.add(pos.clone());

            // Queue neighbors
            for (int[] dir : DIRECTIONS) {
                Vector3i neighborPos = new Vector3i(
                    pos.x + dir[0],
                    pos.y + dir[1],
                    pos.z + dir[2]
                );

                if (neighborPos.y >= 0 && neighborPos.y < 320 &&
                    !networkBlocks.contains(neighborPos)) {
                    queue.add(neighborPos);
                }
            }
        }
    }

    /**
     * Update the power level of a block.
     */
    private void updateBlockPower(World world, Vector3i pos, int power) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
        WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
        if (chunk == null) {
            return;
        }

        BlockState state = chunk.getState(pos.x & 31, pos.y, pos.z & 31);
        if (state instanceof ConduitBlockState conduit) {
            conduit.setPowerLevel(power);
        }
    }

    /**
     * Check if a position contains a conduit block.
     */
    private boolean isConduit(World world, Vector3i pos) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
        WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
        if (chunk == null) {
            return false;
        }

        BlockState state = chunk.getState(pos.x & 31, pos.y, pos.z & 31);
        return state instanceof ConduitBlockState;
    }

    /**
     * Get the decay rate for a conduit at the given position.
     */
    private int getDecayRate(World world, Vector3i pos) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
        WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
        if (chunk == null) {
            return 1;
        }

        BlockState state = chunk.getState(pos.x & 31, pos.y, pos.z & 31);
        if (state instanceof ConduitBlockState conduit) {
            return conduit.getDecayRate();
        }
        return 1;
    }

    /**
     * Get the power level if the position contains a power source.
     * Returns 0 if not a power source.
     */
    private int getPowerSourceLevel(World world, Vector3i pos) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
        WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
        if (chunk == null) {
            return 0;
        }

        var blockType = chunk.getBlockType(pos.x & 31, pos.y, pos.z & 31);
        if (blockType == null) {
            return 0;
        }

        String id = blockType.getId();
        if (id != null && id.contains("Mana_Crystal_Core")) {
            return 15;  // Max power for crystal core
        }

        return 0;
    }

    // ==================== Helper Classes ====================

    /**
     * Entry in the propagation queue.
     */
    private static class PropagationEntry {
        final Vector3i position;
        final int power;
        final int depth;

        PropagationEntry(Vector3i position, int power, int depth) {
            this.position = position;
            this.power = power;
            this.depth = depth;
        }
    }

    /**
     * Represents a power source in the network.
     */
    private static class PowerSourceEntry {
        final Vector3i position;
        final int power;

        PowerSourceEntry(Vector3i position, int power) {
            this.position = position;
            this.power = power;
        }
    }
}

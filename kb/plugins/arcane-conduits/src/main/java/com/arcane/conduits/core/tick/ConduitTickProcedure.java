package com.arcane.conduits.core.tick;

import com.arcane.conduits.ArcaneConduitsPlugin;
import com.arcane.conduits.blocks.state.ConduitBlockState;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktick.BlockTickStrategy;
import com.hypixel.hytale.server.core.asset.type.blocktick.config.TickProcedure;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;

/**
 * Tick procedure for conduit blocks that handles power propagation.
 *
 * Each tick, a conduit:
 * 1. Reads power levels from all connected neighbors
 * 2. Takes the maximum power minus decay
 * 3. Updates its own power level if changed
 * 4. Notifies neighbors if power changed
 */
public class ConduitTickProcedure extends TickProcedure {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String TYPE_ID = "ConduitTick";

    public static final BuilderCodec<ConduitTickProcedure> CODEC = BuilderCodec.builder(
            ConduitTickProcedure.class,
            ConduitTickProcedure::new,
            TickProcedure.BASE_CODEC
        )
        .addField(
            new KeyedCodec<>("DecayRate", Codec.INTEGER),
            (proc, value) -> proc.decayRate = value,
            proc -> proc.decayRate
        )
        .build();

    /**
     * Power decay rate per block (default: 1).
     */
    private int decayRate = 1;

    // Direction offsets for the 6 cardinal directions
    private static final int[][] NEIGHBOR_OFFSETS = {
        {-1, 0, 0},  // -X (bit 0)
        {1, 0, 0},   // +X (bit 1)
        {0, -1, 0},  // -Y (bit 2)
        {0, 1, 0},   // +Y (bit 3)
        {0, 0, -1},  // -Z (bit 4)
        {0, 0, 1}    // +Z (bit 5)
    };

    public ConduitTickProcedure() {
        super();
    }

    @Override
    public BlockTickStrategy onTick(World world, WorldChunk chunk, int blockX, int blockY, int blockZ, int blockId) {
        // Get the conduit's block state
        ConduitBlockState conduitState = getConduitState(chunk, blockX, blockY, blockZ);
        if (conduitState == null) {
            // Not a conduit block state - sleep
            return BlockTickStrategy.SLEEP;
        }

        // Find the maximum power level from all connected neighbors
        int maxNeighborPower = 0;
        int newConnectionMask = 0;

        for (int dir = 0; dir < NEIGHBOR_OFFSETS.length; dir++) {
            int[] offset = NEIGHBOR_OFFSETS[dir];
            int nx = blockX + offset[0];
            int ny = blockY + offset[1];
            int nz = blockZ + offset[2];

            // Get neighbor power (handles cross-chunk lookups)
            int neighborPower = getNeighborPower(world, chunk, nx, ny, nz);

            if (neighborPower > 0) {
                maxNeighborPower = Math.max(maxNeighborPower, neighborPower);
                newConnectionMask |= (1 << dir);
            } else if (isConduitBlock(world, chunk, nx, ny, nz)) {
                // Connected but no power
                newConnectionMask |= (1 << dir);
            }
        }

        // Calculate new power with decay
        int newPower = Math.max(0, maxNeighborPower - decayRate);

        // Update connection mask if changed
        if (newConnectionMask != conduitState.getConnectionMask()) {
            conduitState.setConnectionMask(newConnectionMask);
        }

        // Update power level if changed
        int oldPower = conduitState.getPowerLevel();
        if (newPower != oldPower) {
            conduitState.setPowerLevel(newPower);

            // Notify the network manager of the change
            ArcaneConduitsPlugin plugin = ArcaneConduitsPlugin.getInstance();
            if (plugin != null && plugin.getNetworkManager() != null) {
                plugin.getNetworkManager().onPowerChanged(
                    new Vector3i(blockX, blockY, blockZ),
                    oldPower,
                    newPower
                );
            }
        }

        // Continue ticking to stay responsive to power changes
        return BlockTickStrategy.CONTINUE;
    }

    /**
     * Get the ConduitBlockState at the given position.
     */
    private ConduitBlockState getConduitState(WorldChunk chunk, int x, int y, int z) {
        if (chunk == null) return null;

        // Get block state at position
        BlockState state = chunk.getState(x & 31, y, z & 31);
        if (state instanceof ConduitBlockState) {
            return (ConduitBlockState) state;
        }
        return null;
    }

    /**
     * Get the power level from a neighboring block.
     * Returns 0 if not a conduit or chunk not loaded.
     */
    private int getNeighborPower(World world, WorldChunk sourceChunk, int x, int y, int z) {
        // Check bounds
        if (y < 0 || y >= 320) {
            return 0;
        }

        // Get the chunk containing this position
        WorldChunk neighborChunk = getChunkAt(world, sourceChunk, x, z);
        if (neighborChunk == null) {
            return 0;
        }

        // Get the block state
        BlockState state = neighborChunk.getState(x & 31, y, z & 31);
        if (state instanceof ConduitBlockState conduit) {
            return conduit.getOutputPower();
        }

        // Check if it's a power source block (future: implement power source interface)
        // For now, return 0 for non-conduit blocks
        return 0;
    }

    /**
     * Check if a block at the given position is a conduit.
     */
    private boolean isConduitBlock(World world, WorldChunk sourceChunk, int x, int y, int z) {
        if (y < 0 || y >= 320) {
            return false;
        }

        WorldChunk neighborChunk = getChunkAt(world, sourceChunk, x, z);
        if (neighborChunk == null) {
            return false;
        }

        BlockState state = neighborChunk.getState(x & 31, y, z & 31);
        return state instanceof ConduitBlockState;
    }

    /**
     * Get the chunk containing the given world coordinates.
     * Returns the source chunk if same chunk, or queries world.
     */
    private WorldChunk getChunkAt(World world, WorldChunk sourceChunk, int blockX, int blockZ) {
        int chunkX = blockX >> 5;  // Divide by 32
        int chunkZ = blockZ >> 5;

        // Check if it's the same chunk
        if (sourceChunk.getX() == chunkX && sourceChunk.getZ() == chunkZ) {
            return sourceChunk;
        }

        // Query the world for the neighboring chunk (only if loaded)
        long chunkIndex = ChunkUtil.indexChunkFromBlock(blockX, blockZ);
        return world.getChunkIfLoaded(chunkIndex);
    }

    /**
     * Get the decay rate for this tick procedure.
     */
    public int getDecayRate() {
        return decayRate;
    }
}

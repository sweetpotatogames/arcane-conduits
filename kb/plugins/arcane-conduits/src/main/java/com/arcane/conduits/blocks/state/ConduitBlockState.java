package com.arcane.conduits.blocks.state;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;

import javax.annotation.Nonnull;

/**
 * Block state for arcane conduits that store and transmit power levels.
 *
 * Power levels range from 0-15:
 * - 0: No power (dormant)
 * - 1-4: Weak mana flow
 * - 5-10: Moderate mana flow
 * - 11-14: Strong mana flow
 * - 15: Maximum power
 */
public class ConduitBlockState extends BlockState {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String TYPE_ID = "ConduitBlockState";

    public static final BuilderCodec<ConduitBlockState> CODEC = BuilderCodec.builder(
            ConduitBlockState.class,
            ConduitBlockState::new,
            BlockState.BASE_CODEC
        )
        .addField(
            new KeyedCodec<>("PowerLevel", Codec.INTEGER),
            (state, value) -> state.powerLevel = value,
            state -> state.powerLevel
        )
        .addField(
            new KeyedCodec<>("ConnectionMask", Codec.INTEGER),
            (state, value) -> state.connectionMask = value,
            state -> state.connectionMask
        )
        .build();

    /**
     * Current power level (0-15).
     */
    private int powerLevel = 0;

    /**
     * Bitmask representing connections to adjacent blocks.
     * Bits 0-5 represent: -X, +X, -Y, +Y, -Z, +Z
     */
    private int connectionMask = 0;

    /**
     * Maximum power level for this conduit type.
     */
    private int maxPower = 15;

    /**
     * Power decay rate per block.
     */
    private int decayRate = 1;

    public ConduitBlockState() {
        super();
    }

    // ==================== Power Level Methods ====================

    /**
     * Get the current power level (0-15).
     */
    public int getPowerLevel() {
        return powerLevel;
    }

    /**
     * Set the power level, clamping to valid range.
     */
    public void setPowerLevel(int level) {
        int newLevel = Math.max(0, Math.min(maxPower, level));
        if (newLevel != this.powerLevel) {
            this.powerLevel = newLevel;
            markNeedsSave();
            updateVisualState();
        }
    }

    /**
     * Check if this conduit has any power.
     */
    public boolean hasPower() {
        return powerLevel > 0;
    }

    /**
     * Get the power level after decay (for propagation to neighbors).
     */
    public int getOutputPower() {
        return Math.max(0, powerLevel - decayRate);
    }

    // ==================== Connection Methods ====================

    /**
     * Get the connection mask (bitmask for 6 directions).
     */
    public int getConnectionMask() {
        return connectionMask;
    }

    /**
     * Set the connection mask.
     */
    public void setConnectionMask(int mask) {
        if (mask != this.connectionMask) {
            this.connectionMask = mask;
            markNeedsSave();
        }
    }

    /**
     * Check if connected in a specific direction.
     * Direction bits: 0=-X, 1=+X, 2=-Y, 3=+Y, 4=-Z, 5=+Z
     */
    public boolean isConnected(int directionBit) {
        return (connectionMask & (1 << directionBit)) != 0;
    }

    /**
     * Set connection state for a specific direction.
     */
    public void setConnected(int directionBit, boolean connected) {
        if (connected) {
            connectionMask |= (1 << directionBit);
        } else {
            connectionMask &= ~(1 << directionBit);
        }
        markNeedsSave();
    }

    // ==================== Configuration ====================

    /**
     * Get maximum power this conduit can carry.
     */
    public int getMaxPower() {
        return maxPower;
    }

    /**
     * Set maximum power (from block definition).
     */
    public void setMaxPower(int max) {
        this.maxPower = max;
    }

    /**
     * Get power decay rate.
     */
    public int getDecayRate() {
        return decayRate;
    }

    /**
     * Set power decay rate (from block definition).
     */
    public void setDecayRate(int rate) {
        this.decayRate = rate;
    }

    // ==================== Visual State ====================

    /**
     * Update the visual/interaction state based on power level.
     * Note: Visual state updates are handled by block definition JSON states.
     * The power level stored in this state will be used by the client for rendering.
     */
    private void updateVisualState() {
        // Visual state is determined by power level stored in this state.
        // The block definition JSON specifies visual variants for different power levels.
        // No direct API call needed - the state save triggers client updates.
    }

    // ==================== Utility ====================

    /**
     * Get power level category string for debugging.
     */
    public String getPowerCategory() {
        if (powerLevel == 0) return "off";
        if (powerLevel <= 4) return "weak";
        if (powerLevel <= 10) return "moderate";
        if (powerLevel <= 14) return "strong";
        return "maximum";
    }

    @Override
    public String toString() {
        return String.format("ConduitBlockState[pos=%s, power=%d/%d, connections=%d]",
            getBlockPosition(), powerLevel, maxPower, Integer.bitCount(connectionMask));
    }

    // ==================== State Data (from JSON block definition) ====================

    /**
     * State data class for JSON block definitions.
     * Allows configuring max power and decay rate per block type.
     */
    public static class ConduitStateData extends StateData {

        public static final BuilderCodec<ConduitStateData> CODEC = BuilderCodec.builder(
                ConduitStateData.class,
                ConduitStateData::new,
                StateData.DEFAULT_CODEC
            )
            .addField(
                new KeyedCodec<>("MaxPower", Codec.INTEGER),
                (data, value) -> data.maxPower = value,
                data -> data.maxPower
            )
            .addField(
                new KeyedCodec<>("DecayRate", Codec.INTEGER),
                (data, value) -> data.decayRate = value,
                data -> data.decayRate
            )
            .build();

        private int maxPower = 15;
        private int decayRate = 1;

        public ConduitStateData() {
            super();
        }

        public int getMaxPower() {
            return maxPower;
        }

        public int getDecayRate() {
            return decayRate;
        }
    }
}

package com.arcane.conduits.blocks.state;

import com.arcane.conduits.ArcaneConduitsPlugin;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;

import javax.annotation.Nonnull;

/**
 * Block state for arcane power sources that generate power.
 *
 * Power sources output power to adjacent conduits without consuming anything
 * (for constant sources like Mana Crystal Core) or based on fuel/conditions.
 */
public class PowerSourceBlockState extends BlockState {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String TYPE_ID = "PowerSourceBlockState";

    public static final BuilderCodec<PowerSourceBlockState> CODEC = BuilderCodec.builder(
            PowerSourceBlockState.class,
            PowerSourceBlockState::new,
            BlockState.BASE_CODEC
        )
        .addField(
            new KeyedCodec<>("OutputPower", Codec.INTEGER),
            (state, value) -> state.outputPower = value,
            state -> state.outputPower
        )
        .addField(
            new KeyedCodec<>("Active", Codec.BOOLEAN),
            (state, value) -> state.active = value,
            state -> state.active
        )
        .build();

    /**
     * Power level this source outputs (0-15).
     */
    private int outputPower = 15;

    /**
     * Whether this power source is currently active.
     */
    private boolean active = true;

    /**
     * Whether this is a constant power source (no fuel required).
     */
    private boolean constant = true;

    public PowerSourceBlockState() {
        super();
    }

    // ==================== Power Output Methods ====================

    /**
     * Get the current output power level.
     */
    public int getOutputPower() {
        return active ? outputPower : 0;
    }

    /**
     * Set the output power level.
     */
    public void setOutputPower(int power) {
        int newPower = Math.max(0, Math.min(15, power));
        if (newPower != this.outputPower) {
            this.outputPower = newPower;
            markNeedsSave();
            propagatePower();
        }
    }

    /**
     * Check if this power source is active.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Set whether this power source is active.
     */
    public void setActive(boolean active) {
        if (active != this.active) {
            this.active = active;
            markNeedsSave();
            updateVisualState();
            propagatePower();
        }
    }

    /**
     * Toggle the power source on/off.
     */
    public void toggle() {
        setActive(!active);
    }

    // ==================== Configuration ====================

    /**
     * Check if this is a constant power source.
     */
    public boolean isConstant() {
        return constant;
    }

    /**
     * Set whether this is a constant power source.
     */
    public void setConstant(boolean constant) {
        this.constant = constant;
    }

    // ==================== Power Propagation ====================

    /**
     * Trigger power propagation to connected conduits.
     */
    private void propagatePower() {
        if (getChunk() == null || getChunk().getWorld() == null) {
            return;
        }

        ArcaneConduitsPlugin plugin = ArcaneConduitsPlugin.getInstance();
        if (plugin == null || plugin.getNetworkManager() == null) {
            return;
        }

        Vector3i pos = getBlockPosition();
        if (active) {
            plugin.getNetworkManager().propagatePower(
                getChunk().getWorld(),
                pos,
                outputPower
            );
        } else {
            plugin.getNetworkManager().clearPower(
                getChunk().getWorld(),
                pos
            );
        }
    }

    // ==================== Visual State ====================

    /**
     * Update the visual/interaction state.
     * Note: Visual state updates are handled by block definition JSON states.
     * The active state stored here will be used by the client for rendering.
     */
    private void updateVisualState() {
        // Visual state is determined by active state stored in this state.
        // The block definition JSON specifies visual variants for active/inactive.
        // No direct API call needed - the state save triggers client updates.
    }

    @Override
    public String toString() {
        return String.format("PowerSourceBlockState[pos=%s, output=%d, active=%s, constant=%s]",
            getBlockPosition(), outputPower, active, constant);
    }

    // ==================== State Data ====================

    /**
     * State data class for JSON block definitions.
     */
    public static class PowerSourceStateData extends StateData {

        public static final BuilderCodec<PowerSourceStateData> CODEC = BuilderCodec.builder(
                PowerSourceStateData.class,
                PowerSourceStateData::new,
                StateData.DEFAULT_CODEC
            )
            .addField(
                new KeyedCodec<>("OutputPower", Codec.INTEGER),
                (data, value) -> data.outputPower = value,
                data -> data.outputPower
            )
            .addField(
                new KeyedCodec<>("Constant", Codec.BOOLEAN),
                (data, value) -> data.constant = value,
                data -> data.constant
            )
            .build();

        private int outputPower = 15;
        private boolean constant = true;

        public PowerSourceStateData() {
            super();
        }

        public int getOutputPower() {
            return outputPower;
        }

        public boolean isConstant() {
            return constant;
        }
    }
}

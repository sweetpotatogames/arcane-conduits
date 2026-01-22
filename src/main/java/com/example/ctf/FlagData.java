package com.example.ctf;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Holds all data about a single flag in CTF.
 */
public class FlagData {

    private final FlagTeam team;
    private FlagState state;

    // Stand position (where flag spawns/returns to)
    @Nullable
    private Vector3d standPosition;

    // Current position (when dropped)
    @Nullable
    private Vector3d droppedPosition;

    // Carrier info (when carried)
    @Nullable
    private UUID carrierUuid;
    @Nullable
    private PlayerRef carrierRef;

    // Timing
    private long droppedAtTime;      // When the flag was dropped
    private long immunityExpiresAt;  // When pickup immunity expires

    // Constants
    public static final long IMMUNITY_DURATION_MS = 4000;  // 4 seconds
    public static final long RETURN_TIMEOUT_MS = 45000;    // 45 seconds

    public FlagData(@Nonnull FlagTeam team) {
        this.team = team;
        this.state = FlagState.AT_STAND;
    }

    // Getters
    @Nonnull
    public FlagTeam getTeam() {
        return team;
    }

    @Nonnull
    public FlagState getState() {
        return state;
    }

    @Nullable
    public Vector3d getStandPosition() {
        return standPosition;
    }

    @Nullable
    public Vector3d getDroppedPosition() {
        return droppedPosition;
    }

    @Nullable
    public UUID getCarrierUuid() {
        return carrierUuid;
    }

    @Nullable
    public PlayerRef getCarrierRef() {
        return carrierRef;
    }

    public boolean isCarried() {
        return state == FlagState.CARRIED;
    }

    public boolean isDropped() {
        return state == FlagState.DROPPED;
    }

    public boolean isAtStand() {
        return state == FlagState.AT_STAND;
    }

    /**
     * Checks if the dropped flag has pickup immunity active.
     */
    public boolean hasImmunity() {
        if (state != FlagState.DROPPED) {
            return false;
        }
        return System.currentTimeMillis() < immunityExpiresAt;
    }

    /**
     * Checks if the dropped flag should return to stand (timeout expired).
     */
    public boolean shouldReturnToStand() {
        if (state != FlagState.DROPPED) {
            return false;
        }
        return System.currentTimeMillis() > droppedAtTime + RETURN_TIMEOUT_MS;
    }

    /**
     * Gets the current position of the flag (stand, dropped, or carrier position).
     */
    @Nullable
    public Vector3d getCurrentPosition() {
        return switch (state) {
            case AT_STAND -> standPosition;
            case DROPPED -> droppedPosition;
            case CARRIED -> null; // Carrier position changes constantly
        };
    }

    // State transitions

    /**
     * Sets the flag's stand position.
     */
    public void setStandPosition(@Nonnull Vector3d position) {
        this.standPosition = position;
    }

    /**
     * Picks up the flag - transitions to CARRIED state.
     */
    public void pickup(@Nonnull UUID playerUuid, @Nonnull PlayerRef playerRef) {
        this.state = FlagState.CARRIED;
        this.carrierUuid = playerUuid;
        this.carrierRef = playerRef;
        this.droppedPosition = null;
        this.droppedAtTime = 0;
        this.immunityExpiresAt = 0;
    }

    /**
     * Drops the flag - transitions to DROPPED state with immunity.
     */
    public void drop(@Nonnull Vector3d position) {
        this.state = FlagState.DROPPED;
        this.droppedPosition = position;
        this.droppedAtTime = System.currentTimeMillis();
        this.immunityExpiresAt = droppedAtTime + IMMUNITY_DURATION_MS;
        this.carrierUuid = null;
        this.carrierRef = null;
    }

    /**
     * Returns the flag to its stand - transitions to AT_STAND state.
     */
    public void returnToStand() {
        this.state = FlagState.AT_STAND;
        this.droppedPosition = null;
        this.droppedAtTime = 0;
        this.immunityExpiresAt = 0;
        this.carrierUuid = null;
        this.carrierRef = null;
    }

    @Override
    public String toString() {
        return "FlagData{" +
            "team=" + team +
            ", state=" + state +
            ", carrier=" + carrierUuid +
            '}';
    }
}

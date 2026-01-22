package com.example.ctf;

/**
 * Represents the current state of a flag in CTF.
 */
public enum FlagState {
    /**
     * Flag is at its home stand, ready to be captured.
     */
    AT_STAND,

    /**
     * Flag is being carried by a player.
     */
    CARRIED,

    /**
     * Flag was dropped and is on the ground.
     * May have pickup immunity active.
     */
    DROPPED
}

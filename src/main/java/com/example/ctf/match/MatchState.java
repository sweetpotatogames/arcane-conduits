package com.example.ctf.match;

/**
 * Represents the current state of a CTF match.
 */
public enum MatchState {
    /**
     * Match is not active. Players can join teams, arena is being set up.
     */
    WAITING,

    /**
     * Match is in progress. Players can capture flags and score.
     */
    ACTIVE,

    /**
     * Match has ended. A winner has been declared or match was stopped.
     */
    ENDED
}

package com.example.dnd.combat;

/**
 * Represents the phases of a D&D turn.
 */
public enum TurnPhase {
    MOVEMENT("Movement Phase - Click to select destination"),
    ACTION("Action Phase - Use abilities or attacks"),
    BONUS_ACTION("Bonus Action Phase"),
    END_TURN("Turn Complete");

    private final String description;

    TurnPhase(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

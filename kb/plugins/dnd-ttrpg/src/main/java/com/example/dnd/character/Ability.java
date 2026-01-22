package com.example.dnd.character;

/**
 * D&D 5e ability scores.
 */
public enum Ability {
    STRENGTH("STR"),
    DEXTERITY("DEX"),
    CONSTITUTION("CON"),
    INTELLIGENCE("INT"),
    WISDOM("WIS"),
    CHARISMA("CHA");

    private final String abbreviation;

    Ability(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getAbbreviation() {
        return abbreviation;
    }
}

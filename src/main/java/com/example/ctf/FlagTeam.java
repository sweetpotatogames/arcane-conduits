package com.example.ctf;

/**
 * Represents the two teams in Capture The Flag.
 */
public enum FlagTeam {
    RED("Red", "#FF4444"),
    BLUE("Blue", "#4444FF");

    private final String displayName;
    private final String color;

    FlagTeam(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }

    public String getFlagItemId() {
        return "CTF_Flag_" + displayName;
    }

    /**
     * Gets the opposing team.
     */
    public FlagTeam getOpposite() {
        return this == RED ? BLUE : RED;
    }

    /**
     * Parses a team from a string (case-insensitive).
     */
    public static FlagTeam fromString(String name) {
        if (name == null) return null;
        String upper = name.toUpperCase();
        if (upper.equals("RED") || upper.equals("R")) return RED;
        if (upper.equals("BLUE") || upper.equals("B")) return BLUE;
        return null;
    }
}

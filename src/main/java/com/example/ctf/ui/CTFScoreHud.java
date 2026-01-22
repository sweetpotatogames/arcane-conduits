package com.example.ctf.ui;

import com.example.ctf.CTFPlugin;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom HUD for displaying CTF match scores.
 * Shows red and blue team scores at the top of the screen.
 */
public class CTFScoreHud extends CustomUIHud {

    private static final String RED_COLOR = "#FF4444";
    private static final String BLUE_COLOR = "#4488FF";

    private final CTFPlugin plugin;
    private int redScore;
    private int blueScore;

    // Track all active score HUDs by player UUID
    private static final Map<UUID, CTFScoreHud> activeHuds = new ConcurrentHashMap<>();

    public CTFScoreHud(@Nonnull PlayerRef playerRef, @Nonnull CTFPlugin plugin) {
        super(playerRef);
        this.plugin = plugin;
        this.redScore = 0;
        this.blueScore = 0;
    }

    @Override
    protected void build(@Nonnull UICommandBuilder commandBuilder) {
        // Build the score HUD using inline UI definition
        // Container at top center of screen
        commandBuilder.appendInline(null,
            "Group { " +
                "Id: CTFScoreContainer; " +
                "LayoutMode: Down; " +
                "Anchor: (Top: 10; HorizontalCenter: 0); " +
                "Style: (Alignment: Center); " +
                "Children: [" +
                    // Title
                    "Label { " +
                        "Id: CTFTitle; " +
                        "Text: 'CAPTURE THE FLAG'; " +
                        "Style: (FontSize: 14; FontWeight: Bold; Alignment: Center); " +
                    "}, " +
                    // Score container
                    "Group { " +
                        "Id: ScoreRow; " +
                        "LayoutMode: Right; " +
                        "Style: (Alignment: Center; Spacing: 20); " +
                        "Children: [" +
                            // Red team score
                            "Group { " +
                                "LayoutMode: Right; " +
                                "Style: (Spacing: 5); " +
                                "Children: [" +
                                    "Label { " +
                                        "Id: RedLabel; " +
                                        "Text: 'RED'; " +
                                        "Style: (FontSize: 18; FontWeight: Bold; Color: " + RED_COLOR + "); " +
                                    "}, " +
                                    "Label { " +
                                        "Id: RedScore; " +
                                        "Text: '0'; " +
                                        "Style: (FontSize: 24; FontWeight: Bold; Color: " + RED_COLOR + "); " +
                                    "}" +
                                "]" +
                            "}, " +
                            // Separator
                            "Label { " +
                                "Id: Separator; " +
                                "Text: '-'; " +
                                "Style: (FontSize: 24; FontWeight: Bold); " +
                            "}, " +
                            // Blue team score
                            "Group { " +
                                "LayoutMode: Right; " +
                                "Style: (Spacing: 5); " +
                                "Children: [" +
                                    "Label { " +
                                        "Id: BlueScore; " +
                                        "Text: '0'; " +
                                        "Style: (FontSize: 24; FontWeight: Bold; Color: " + BLUE_COLOR + "); " +
                                    "}, " +
                                    "Label { " +
                                        "Id: BlueLabel; " +
                                        "Text: 'BLUE'; " +
                                        "Style: (FontSize: 18; FontWeight: Bold; Color: " + BLUE_COLOR + "); " +
                                    "}" +
                                "]" +
                            "}" +
                        "]" +
                    "}" +
                "]" +
            "}"
        );
    }

    /**
     * Updates the displayed scores.
     *
     * @param redScore The red team's score
     * @param blueScore The blue team's score
     */
    public void updateScore(int redScore, int blueScore) {
        this.redScore = redScore;
        this.blueScore = blueScore;

        // Send update to client
        UICommandBuilder commandBuilder = new UICommandBuilder();
        commandBuilder.set("#RedScore.Text", String.valueOf(redScore));
        commandBuilder.set("#BlueScore.Text", String.valueOf(blueScore));
        this.update(false, commandBuilder);
    }

    /**
     * Gets the current red score being displayed.
     */
    public int getRedScore() {
        return redScore;
    }

    /**
     * Gets the current blue score being displayed.
     */
    public int getBlueScore() {
        return blueScore;
    }

    // ==================== Static Management Methods ====================

    /**
     * Shows the score HUD to a player.
     *
     * @param playerRef The player to show the HUD to
     * @param plugin The CTF plugin instance
     */
    public static void showToPlayer(@Nonnull PlayerRef playerRef, @Nonnull CTFPlugin plugin) {
        UUID playerUuid = playerRef.getUuid();

        // Create new HUD for this player
        CTFScoreHud hud = new CTFScoreHud(playerRef, plugin);
        activeHuds.put(playerUuid, hud);

        // Get HudManager from Player component and set custom HUD
        Player playerComponent = playerRef.getComponent(Player.getComponentType());
        if (playerComponent != null) {
            HudManager hudManager = playerComponent.getHudManager();
            hudManager.setCustomHud(playerRef, hud);
            plugin.getLogger().atInfo().log("Showing score HUD to player {}", playerUuid);
        }
    }

    /**
     * Hides the score HUD from a player.
     *
     * @param playerRef The player to hide the HUD from
     */
    public static void hideFromPlayer(@Nonnull PlayerRef playerRef) {
        UUID playerUuid = playerRef.getUuid();
        CTFScoreHud hud = activeHuds.remove(playerUuid);

        if (hud != null) {
            Player playerComponent = playerRef.getComponent(Player.getComponentType());
            if (playerComponent != null) {
                HudManager hudManager = playerComponent.getHudManager();
                hudManager.setCustomHud(playerRef, null);
            }
        }
    }

    /**
     * Updates scores on all active HUDs.
     *
     * @param redScore The red team's score
     * @param blueScore The blue team's score
     */
    public static void updateAllScores(int redScore, int blueScore) {
        for (CTFScoreHud hud : activeHuds.values()) {
            hud.updateScore(redScore, blueScore);
        }
    }

    /**
     * Shows the score HUD to all players in all worlds.
     *
     * @param plugin The CTF plugin instance
     * @param initialRedScore Initial red team score
     * @param initialBlueScore Initial blue team score
     */
    public static void showToAllPlayers(@Nonnull CTFPlugin plugin, int initialRedScore, int initialBlueScore) {
        for (World world : Universe.get().getWorlds().values()) {
            for (PlayerRef playerRef : world.getPlayerRefs()) {
                showToPlayer(playerRef, plugin);
                CTFScoreHud hud = activeHuds.get(playerRef.getUuid());
                if (hud != null) {
                    hud.updateScore(initialRedScore, initialBlueScore);
                }
            }
        }
    }

    /**
     * Hides the score HUD from all players.
     */
    public static void hideFromAllPlayers() {
        for (World world : Universe.get().getWorlds().values()) {
            for (PlayerRef playerRef : world.getPlayerRefs()) {
                hideFromPlayer(playerRef);
            }
        }
        activeHuds.clear();
    }

    /**
     * Gets the HUD for a specific player.
     *
     * @param playerUuid The player's UUID
     * @return The player's score HUD, or null if not showing
     */
    public static CTFScoreHud getHud(@Nonnull UUID playerUuid) {
        return activeHuds.get(playerUuid);
    }

    /**
     * Cleans up when a player disconnects.
     *
     * @param playerUuid The disconnecting player's UUID
     */
    public static void onPlayerDisconnect(@Nonnull UUID playerUuid) {
        activeHuds.remove(playerUuid);
    }

    /**
     * Clears all tracked HUDs. Called during plugin shutdown.
     */
    public static void cleanup() {
        activeHuds.clear();
    }
}

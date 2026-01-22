package com.example.ctf.ui;

import com.example.ctf.CTFPlugin;
import com.example.ctf.FlagTeam;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.KillFeedMessage;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.PlayerUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Centralized manager for all CTF game announcements.
 * Handles chat messages, titles, and notifications for game events.
 */
public class CTFAnnouncementManager {

    // Color constants for teams and events
    private static final String RED_COLOR = "#FF4444";
    private static final String BLUE_COLOR = "#4488FF";
    private static final String GOLD_COLOR = "#FFD700";
    private static final String GREEN_COLOR = "#55FF55";
    private static final String GRAY_COLOR = "#AAAAAA";

    // Title timing constants
    private static final float TITLE_DURATION = 3.0f;
    private static final float TITLE_FADE_IN = 0.5f;
    private static final float TITLE_FADE_OUT = 1.0f;

    private final CTFPlugin plugin;

    public CTFAnnouncementManager(@Nonnull CTFPlugin plugin) {
        this.plugin = plugin;
        plugin.getLogger().atInfo().log("CTFAnnouncementManager initialized");
    }

    // ==================== Flag Events ====================

    /**
     * Announces that a player picked up an enemy flag.
     *
     * @param playerUuid The UUID of the player who picked up the flag
     * @param playerName The display name of the player
     * @param flagTeam The team whose flag was picked up
     */
    public void announceFlagPickup(@Nonnull UUID playerUuid, @Nonnull String playerName, @Nonnull FlagTeam flagTeam) {
        String teamColor = getTeamColor(flagTeam);

        Message message = Message.raw("")
            .insert(Message.raw("[CTF] ").color(GOLD_COLOR))
            .insert(Message.raw(playerName).color(teamColor))
            .insert(Message.raw(" took the "))
            .insert(Message.raw(flagTeam.getDisplayName() + " flag").color(teamColor))
            .insert(Message.raw("!"));

        broadcastToAllWorlds(message);

        // Play pickup sound
        CTFSoundManager soundManager = plugin.getSoundManager();
        if (soundManager != null) {
            soundManager.playFlagPickup();
        }

        plugin.getLogger().atInfo().log("Announced flag pickup: {} took {} flag", playerName, flagTeam);
    }

    /**
     * Announces that a player dropped a flag.
     *
     * @param playerUuid The UUID of the player who dropped the flag
     * @param playerName The display name of the player
     * @param flagTeam The team whose flag was dropped
     */
    public void announceFlagDropped(@Nonnull UUID playerUuid, @Nonnull String playerName, @Nonnull FlagTeam flagTeam) {
        String teamColor = getTeamColor(flagTeam);

        Message message = Message.raw("")
            .insert(Message.raw("[CTF] ").color(GOLD_COLOR))
            .insert(Message.raw(playerName).color(GRAY_COLOR))
            .insert(Message.raw(" dropped the "))
            .insert(Message.raw(flagTeam.getDisplayName() + " flag").color(teamColor))
            .insert(Message.raw("!"));

        broadcastToAllWorlds(message);

        // Play drop sound
        CTFSoundManager soundManager = plugin.getSoundManager();
        if (soundManager != null) {
            soundManager.playFlagDrop();
        }

        plugin.getLogger().atInfo().log("Announced flag dropped: {} dropped {} flag", playerName, flagTeam);
    }

    /**
     * Announces that a flag was returned to its stand.
     *
     * @param flagTeam The team whose flag was returned
     * @param wasTimeout True if returned due to timeout, false if returned by player
     */
    public void announceFlagReturned(@Nonnull FlagTeam flagTeam, boolean wasTimeout) {
        String teamColor = getTeamColor(flagTeam);

        String reason = wasTimeout ? " (timeout)" : "";
        Message message = Message.raw("")
            .insert(Message.raw("[CTF] ").color(GOLD_COLOR))
            .insert(Message.raw("The "))
            .insert(Message.raw(flagTeam.getDisplayName() + " flag").color(teamColor))
            .insert(Message.raw(" has been returned!" + reason));

        broadcastToAllWorlds(message);

        // Play return sound
        CTFSoundManager soundManager = plugin.getSoundManager();
        if (soundManager != null) {
            soundManager.playFlagReturn();
        }

        plugin.getLogger().atInfo().log("Announced flag returned: {} flag{}", flagTeam, reason);
    }

    /**
     * Announces that a team captured a flag and scored.
     *
     * @param scoringTeam The team that scored
     * @param playerName The name of the player who captured
     * @param redScore Current red team score
     * @param blueScore Current blue team score
     */
    public void announceFlagCaptured(@Nonnull FlagTeam scoringTeam, @Nonnull String playerName, int redScore, int blueScore) {
        String teamColor = getTeamColor(scoringTeam);

        // Chat message
        Message chatMessage = Message.raw("")
            .insert(Message.raw("[CTF] ").color(GOLD_COLOR))
            .insert(Message.raw(playerName).color(teamColor))
            .insert(Message.raw(" captured the flag! "))
            .insert(Message.raw(scoringTeam.getDisplayName() + " SCORES!").color(teamColor));

        broadcastToAllWorlds(chatMessage);

        // Score display message
        Message scoreMessage = Message.raw("")
            .insert(Message.raw("[CTF] ").color(GOLD_COLOR))
            .insert(Message.raw("Score: "))
            .insert(Message.raw("Red " + redScore).color(RED_COLOR))
            .insert(Message.raw(" - "))
            .insert(Message.raw("Blue " + blueScore).color(BLUE_COLOR));

        broadcastToAllWorlds(scoreMessage);

        // Show title to all players
        Message primaryTitle = Message.raw(scoringTeam.getDisplayName().toUpperCase() + " SCORES!").color(teamColor);
        Message secondaryTitle = Message.raw("")
            .insert(Message.raw("Red " + redScore).color(RED_COLOR))
            .insert(Message.raw(" - "))
            .insert(Message.raw("Blue " + blueScore).color(BLUE_COLOR));

        showTitleToAllWorlds(primaryTitle, secondaryTitle, false);

        // Play capture fanfare
        CTFSoundManager soundManager = plugin.getSoundManager();
        if (soundManager != null) {
            soundManager.playFlagCapture();
        }

        plugin.getLogger().atInfo().log("Announced capture: {} by {}, score {}-{}", scoringTeam, playerName, redScore, blueScore);
    }

    // ==================== Match Events ====================

    /**
     * Announces that a match has started.
     */
    public void announceMatchStart() {
        // Chat message
        Message chatMessage = Message.raw("")
            .insert(Message.raw("[CTF] ").color(GOLD_COLOR))
            .insert(Message.raw("The match has started!").color(GREEN_COLOR))
            .insert(Message.raw(" Capture the enemy flag!"));

        broadcastToAllWorlds(chatMessage);

        // Big title
        Message primaryTitle = Message.raw("MATCH STARTED!").color(GREEN_COLOR);
        Message secondaryTitle = Message.raw("Capture the enemy flag!");

        showTitleToAllWorlds(primaryTitle, secondaryTitle, true);

        // Play match start horn
        CTFSoundManager soundManager = plugin.getSoundManager();
        if (soundManager != null) {
            soundManager.playMatchStart();
        }

        plugin.getLogger().atInfo().log("Announced match start");
    }

    /**
     * Announces that a match has ended.
     *
     * @param winner The winning team, or null for a tie
     * @param redScore Final red team score
     * @param blueScore Final blue team score
     */
    public void announceMatchEnd(@Nullable FlagTeam winner, int redScore, int blueScore) {
        String resultText;
        String resultColor;

        if (winner != null) {
            resultText = winner.getDisplayName().toUpperCase() + " WINS!";
            resultColor = getTeamColor(winner);
        } else {
            resultText = "IT'S A TIE!";
            resultColor = GOLD_COLOR;
        }

        // Chat message
        Message chatMessage = Message.raw("")
            .insert(Message.raw("[CTF] ").color(GOLD_COLOR))
            .insert(Message.raw("Match over! ").color(GRAY_COLOR))
            .insert(Message.raw(resultText).color(resultColor));

        broadcastToAllWorlds(chatMessage);

        // Final score
        Message scoreMessage = Message.raw("")
            .insert(Message.raw("[CTF] ").color(GOLD_COLOR))
            .insert(Message.raw("Final Score: "))
            .insert(Message.raw("Red " + redScore).color(RED_COLOR))
            .insert(Message.raw(" - "))
            .insert(Message.raw("Blue " + blueScore).color(BLUE_COLOR));

        broadcastToAllWorlds(scoreMessage);

        // Big title
        Message primaryTitle = Message.raw(resultText).color(resultColor);
        Message secondaryTitle = Message.raw("")
            .insert(Message.raw("Final: Red " + redScore).color(RED_COLOR))
            .insert(Message.raw(" - "))
            .insert(Message.raw("Blue " + blueScore).color(BLUE_COLOR));

        showTitleToAllWorlds(primaryTitle, secondaryTitle, true);

        // Play victory fanfare
        CTFSoundManager soundManager = plugin.getSoundManager();
        if (soundManager != null) {
            soundManager.playMatchEnd();
        }

        plugin.getLogger().atInfo().log("Announced match end: {}, final {}-{}",
            winner != null ? winner : "tie", redScore, blueScore);
    }

    // ==================== Team Events ====================

    /**
     * Announces that a player joined a team.
     *
     * @param playerUuid The UUID of the player
     * @param playerName The display name of the player
     * @param team The team they joined
     */
    public void announceTeamJoin(@Nonnull UUID playerUuid, @Nonnull String playerName, @Nonnull FlagTeam team) {
        String teamColor = getTeamColor(team);

        Message message = Message.raw("")
            .insert(Message.raw("[CTF] ").color(GOLD_COLOR))
            .insert(Message.raw(playerName).color(teamColor))
            .insert(Message.raw(" joined the "))
            .insert(Message.raw(team.getDisplayName() + " team").color(teamColor))
            .insert(Message.raw("."));

        broadcastToAllWorlds(message);

        plugin.getLogger().atInfo().log("Announced team join: {} joined {}", playerName, team);
    }

    /**
     * Announces that a player left their team.
     *
     * @param playerUuid The UUID of the player
     * @param playerName The display name of the player
     * @param team The team they left
     */
    public void announceTeamLeave(@Nonnull UUID playerUuid, @Nonnull String playerName, @Nonnull FlagTeam team) {
        String teamColor = getTeamColor(team);

        Message message = Message.raw("")
            .insert(Message.raw("[CTF] ").color(GOLD_COLOR))
            .insert(Message.raw(playerName).color(GRAY_COLOR))
            .insert(Message.raw(" left the "))
            .insert(Message.raw(team.getDisplayName() + " team").color(teamColor))
            .insert(Message.raw("."));

        broadcastToAllWorlds(message);

        plugin.getLogger().atInfo().log("Announced team leave: {} left {}", playerName, team);
    }

    // ==================== Helper Methods ====================

    /**
     * Gets the color code for a team.
     */
    @Nonnull
    private String getTeamColor(@Nonnull FlagTeam team) {
        return switch (team) {
            case RED -> RED_COLOR;
            case BLUE -> BLUE_COLOR;
        };
    }

    /**
     * Broadcasts a message to all players in all worlds.
     */
    private void broadcastToAllWorlds(@Nonnull Message message) {
        for (World world : Universe.get().getWorlds().values()) {
            world.execute(() -> {
                Store<EntityStore> store = world.getEntityStore().getStore();
                PlayerUtil.broadcastMessageToPlayers(null, message, store);
            });
        }
    }

    /**
     * Shows an event title to all players in all worlds.
     *
     * @param primaryTitle The main title text
     * @param secondaryTitle The subtitle text
     * @param isMajor True for major events (larger display)
     */
    private void showTitleToAllWorlds(@Nonnull Message primaryTitle, @Nonnull Message secondaryTitle, boolean isMajor) {
        for (World world : Universe.get().getWorlds().values()) {
            world.execute(() -> {
                for (PlayerRef playerRef : world.getPlayerRefs()) {
                    EventTitleUtil.showEventTitleToPlayer(
                        playerRef,
                        primaryTitle,
                        secondaryTitle,
                        isMajor,
                        null,
                        TITLE_DURATION,
                        TITLE_FADE_IN,
                        TITLE_FADE_OUT
                    );
                }
            });
        }
    }

    /**
     * Sends a message to a specific player.
     *
     * @param playerRef The player to send to
     * @param message The message to send
     */
    public void sendToPlayer(@Nonnull PlayerRef playerRef, @Nonnull Message message) {
        playerRef.sendMessage(message);
    }

    /**
     * Shows a title to a specific player.
     *
     * @param playerRef The player to show the title to
     * @param primaryTitle The main title
     * @param secondaryTitle The subtitle
     * @param isMajor True for major events
     */
    public void showTitleToPlayer(@Nonnull PlayerRef playerRef, @Nonnull Message primaryTitle,
                                   @Nonnull Message secondaryTitle, boolean isMajor) {
        EventTitleUtil.showEventTitleToPlayer(
            playerRef,
            primaryTitle,
            secondaryTitle,
            isMajor,
            null,
            TITLE_DURATION,
            TITLE_FADE_IN,
            TITLE_FADE_OUT
        );
    }

    // ==================== Kill Feed Methods ====================

    /**
     * Sends a custom kill feed entry to all players.
     * Used for special CTF-related kill messages.
     *
     * @param killerMessage The killer's display name (with team color)
     * @param victimMessage The victim's display name (with team color)
     * @param icon Optional icon to display (e.g., weapon icon)
     */
    public void sendKillFeedEntry(@Nullable Message killerMessage, @Nullable Message victimMessage,
                                   @Nullable String icon) {
        KillFeedMessage packet = new KillFeedMessage(
            killerMessage != null ? killerMessage.getFormattedMessage() : null,
            victimMessage != null ? victimMessage.getFormattedMessage() : null,
            icon
        );

        for (World world : Universe.get().getWorlds().values()) {
            world.execute(() -> {
                for (PlayerRef playerRef : world.getPlayerRefs()) {
                    playerRef.getPacketHandler().writeNoCache(packet);
                }
            });
        }
    }

    /**
     * Announces a flag carrier kill in the kill feed.
     *
     * @param killerName The killer's name
     * @param killerTeam The killer's team
     * @param victimName The flag carrier's name
     * @param victimTeam The flag carrier's team
     * @param flagTeam The team whose flag was being carried
     */
    public void announceKillFeedFlagCarrierKill(@Nonnull String killerName, @Nonnull FlagTeam killerTeam,
                                                 @Nonnull String victimName, @Nonnull FlagTeam victimTeam,
                                                 @Nonnull FlagTeam flagTeam) {
        String killerColor = getTeamColor(killerTeam);
        String victimColor = getTeamColor(victimTeam);

        Message killerMessage = Message.raw(killerName).color(killerColor);
        Message victimMessage = Message.raw("")
            .insert(Message.raw(victimName).color(victimColor))
            .insert(Message.raw(" [FLAG]").color(GOLD_COLOR));

        sendKillFeedEntry(killerMessage, victimMessage, null);

        plugin.getLogger().atInfo().log("Kill feed: {} stopped flag carrier {}", killerName, victimName);
    }

    /**
     * Creates a team-colored player message for kill feed.
     *
     * @param playerName The player's name
     * @param team The player's team (null for no team color)
     * @return A Message with team coloring
     */
    @Nonnull
    public Message createTeamColoredMessage(@Nonnull String playerName, @Nullable FlagTeam team) {
        if (team != null) {
            return Message.raw(playerName).color(getTeamColor(team));
        }
        return Message.raw(playerName);
    }

    /**
     * Exposes team color for use in kill feed event handlers.
     *
     * @param team The team
     * @return The hex color string
     */
    @Nonnull
    public String getTeamColorPublic(@Nonnull FlagTeam team) {
        return getTeamColor(team);
    }
}

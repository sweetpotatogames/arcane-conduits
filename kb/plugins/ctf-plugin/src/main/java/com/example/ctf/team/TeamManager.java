package com.example.ctf.team;

import com.example.ctf.CTFPlugin;
import com.example.ctf.FlagTeam;
import com.example.ctf.ui.CTFAnnouncementManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Manages player team assignments for CTF.
 * Players must be assigned to a team to participate in the match.
 */
public class TeamManager {

    private final CTFPlugin plugin;
    private final Map<UUID, FlagTeam> playerTeams;
    private final Set<UUID> redTeamPlayers;
    private final Set<UUID> blueTeamPlayers;

    public TeamManager(@Nonnull CTFPlugin plugin) {
        this.plugin = plugin;
        this.playerTeams = new HashMap<>();
        this.redTeamPlayers = new HashSet<>();
        this.blueTeamPlayers = new HashSet<>();
    }

    /**
     * Assigns a player to a team.
     *
     * @param playerUuid The player's UUID
     * @param team The team to join
     * @return true if the assignment was successful
     */
    public boolean assignTeam(@Nonnull UUID playerUuid, @Nonnull FlagTeam team) {
        return assignTeam(playerUuid, team, null);
    }

    /**
     * Assigns a player to a team with announcement.
     *
     * @param playerUuid The player's UUID
     * @param team The team to join
     * @param playerName The player's display name for announcements (optional)
     * @return true if the assignment was successful
     */
    public boolean assignTeam(@Nonnull UUID playerUuid, @Nonnull FlagTeam team, @Nullable String playerName) {
        return assignTeam(playerUuid, team, playerName, null);
    }

    /**
     * Assigns a player to a team with announcement and visual effect.
     *
     * @param playerUuid The player's UUID
     * @param team The team to join
     * @param playerName The player's display name for announcements (optional)
     * @param playerRef The player reference for applying visual effects (optional)
     * @return true if the assignment was successful
     */
    public boolean assignTeam(@Nonnull UUID playerUuid, @Nonnull FlagTeam team,
                               @Nullable String playerName, @Nullable PlayerRef playerRef) {
        // Remove from current team if any (silently, we'll announce the new team)
        FlagTeam oldTeam = playerTeams.remove(playerUuid);
        if (oldTeam != null) {
            getTeamSet(oldTeam).remove(playerUuid);
        }

        // Assign to new team
        playerTeams.put(playerUuid, team);
        getTeamSet(team).add(playerUuid);

        plugin.getLogger().atInfo().log("Player {} joined {} team", playerUuid, team.getDisplayName());

        // Apply team visual effect
        if (playerRef != null) {
            TeamVisualManager visualManager = plugin.getTeamVisualManager();
            if (visualManager != null) {
                visualManager.applyTeamEffect(playerRef, team);
            }
        }

        // Announce team join
        if (playerName != null) {
            CTFAnnouncementManager announcementManager = plugin.getAnnouncementManager();
            if (announcementManager != null) {
                announcementManager.announceTeamJoin(playerUuid, playerName, team);
            }
        }

        return true;
    }

    /**
     * Removes a player from their current team.
     *
     * @param playerUuid The player's UUID
     * @return The team they left, or null if they weren't on a team
     */
    @Nullable
    public FlagTeam leaveTeam(@Nonnull UUID playerUuid) {
        return leaveTeam(playerUuid, null);
    }

    /**
     * Removes a player from their current team with announcement.
     *
     * @param playerUuid The player's UUID
     * @param playerName The player's display name for announcements (optional)
     * @return The team they left, or null if they weren't on a team
     */
    @Nullable
    public FlagTeam leaveTeam(@Nonnull UUID playerUuid, @Nullable String playerName) {
        return leaveTeam(playerUuid, playerName, null);
    }

    /**
     * Removes a player from their current team with announcement and visual effect removal.
     *
     * @param playerUuid The player's UUID
     * @param playerName The player's display name for announcements (optional)
     * @param playerRef The player reference for removing visual effects (optional)
     * @return The team they left, or null if they weren't on a team
     */
    @Nullable
    public FlagTeam leaveTeam(@Nonnull UUID playerUuid, @Nullable String playerName, @Nullable PlayerRef playerRef) {
        FlagTeam currentTeam = playerTeams.remove(playerUuid);
        if (currentTeam != null) {
            getTeamSet(currentTeam).remove(playerUuid);
            plugin.getLogger().atInfo().log("Player {} left {} team", playerUuid, currentTeam.getDisplayName());

            // Remove team visual effect
            if (playerRef != null) {
                TeamVisualManager visualManager = plugin.getTeamVisualManager();
                if (visualManager != null) {
                    visualManager.removeTeamEffect(playerRef);
                }
            }

            // Announce team leave
            if (playerName != null) {
                CTFAnnouncementManager announcementManager = plugin.getAnnouncementManager();
                if (announcementManager != null) {
                    announcementManager.announceTeamLeave(playerUuid, playerName, currentTeam);
                }
            }
        }
        return currentTeam;
    }

    /**
     * Gets the team a player is assigned to.
     *
     * @param playerUuid The player's UUID
     * @return The player's team, or null if not assigned
     */
    @Nullable
    public FlagTeam getPlayerTeam(@Nonnull UUID playerUuid) {
        return playerTeams.get(playerUuid);
    }

    /**
     * Checks if a player is on a team.
     *
     * @param playerUuid The player's UUID
     * @return true if the player is assigned to a team
     */
    public boolean isOnTeam(@Nonnull UUID playerUuid) {
        return playerTeams.containsKey(playerUuid);
    }

    /**
     * Checks if a player is on a specific team.
     *
     * @param playerUuid The player's UUID
     * @param team The team to check
     * @return true if the player is on the specified team
     */
    public boolean isOnTeam(@Nonnull UUID playerUuid, @Nonnull FlagTeam team) {
        return team.equals(playerTeams.get(playerUuid));
    }

    /**
     * Gets all players on a specific team.
     *
     * @param team The team
     * @return An unmodifiable set of player UUIDs
     */
    @Nonnull
    public Set<UUID> getTeamPlayers(@Nonnull FlagTeam team) {
        return Collections.unmodifiableSet(getTeamSet(team));
    }

    /**
     * Gets the size of a team.
     *
     * @param team The team
     * @return The number of players on the team
     */
    public int getTeamSize(@Nonnull FlagTeam team) {
        return getTeamSet(team).size();
    }

    /**
     * Auto-assigns a player to the team with fewer players for balance.
     *
     * @param playerUuid The player's UUID
     * @return The team they were assigned to
     */
    @Nonnull
    public FlagTeam autoAssignTeam(@Nonnull UUID playerUuid) {
        FlagTeam team = getSmallerTeam();
        assignTeam(playerUuid, team);
        return team;
    }

    /**
     * Gets the team with fewer players. If tied, returns RED.
     *
     * @return The team with fewer players
     */
    @Nonnull
    public FlagTeam getSmallerTeam() {
        int redSize = redTeamPlayers.size();
        int blueSize = blueTeamPlayers.size();
        return redSize <= blueSize ? FlagTeam.RED : FlagTeam.BLUE;
    }

    /**
     * Checks if the teams are balanced (differ by at most 1 player).
     *
     * @return true if teams are balanced
     */
    public boolean areTeamsBalanced() {
        return Math.abs(redTeamPlayers.size() - blueTeamPlayers.size()) <= 1;
    }

    /**
     * Gets the total number of players on teams.
     *
     * @return The total player count
     */
    public int getTotalPlayers() {
        return playerTeams.size();
    }

    /**
     * Clears all team assignments.
     */
    public void clearTeams() {
        playerTeams.clear();
        redTeamPlayers.clear();
        blueTeamPlayers.clear();
        plugin.getLogger().atInfo().log("All team assignments cleared");
    }

    /**
     * Called when a player disconnects. Removes them from their team.
     *
     * @param playerUuid The disconnecting player's UUID
     */
    public void handlePlayerDisconnect(@Nonnull UUID playerUuid) {
        leaveTeam(playerUuid);
    }

    @Nonnull
    private Set<UUID> getTeamSet(@Nonnull FlagTeam team) {
        return switch (team) {
            case RED -> redTeamPlayers;
            case BLUE -> blueTeamPlayers;
        };
    }
}

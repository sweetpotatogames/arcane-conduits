package com.example.ctf.spawn;

import com.example.ctf.CTFPlugin;
import com.example.ctf.FlagTeam;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Spawn provider for CTF that returns team-appropriate spawn points.
 * Falls back to world spawn if player has no team or no spawns configured.
 */
public class CTFSpawnProvider implements ISpawnProvider {

    private final CTFPlugin plugin;
    @Nullable
    private final ISpawnProvider fallbackProvider;

    public CTFSpawnProvider(@Nonnull CTFPlugin plugin, @Nullable ISpawnProvider fallbackProvider) {
        this.plugin = plugin;
        this.fallbackProvider = fallbackProvider;
    }

    @Override
    public Transform getSpawnPoint(@Nonnull World world, @Nonnull UUID playerUuid) {
        // Get the player's team
        FlagTeam team = plugin.getTeamManager().getPlayerTeam(playerUuid);

        if (team != null) {
            // Get spawn point for this team
            Transform spawnPoint = plugin.getArenaManager().getSpawnPoint(team, playerUuid);
            if (spawnPoint != null) {
                return spawnPoint.clone();
            }
        }

        // Fall back to default world spawn
        if (fallbackProvider != null) {
            return fallbackProvider.getSpawnPoint(world, playerUuid);
        }

        // Ultimate fallback - world origin (shouldn't happen in practice)
        return new Transform(0, 64, 0);
    }

    @Override
    public Transform[] getSpawnPoints() {
        // Return all spawn points from both teams
        Transform[] redSpawns = plugin.getArenaManager().getConfig().getRedSpawns();
        Transform[] blueSpawns = plugin.getArenaManager().getConfig().getBlueSpawns();

        Transform[] allSpawns = new Transform[redSpawns.length + blueSpawns.length];
        System.arraycopy(redSpawns, 0, allSpawns, 0, redSpawns.length);
        System.arraycopy(blueSpawns, 0, allSpawns, redSpawns.length, blueSpawns.length);

        return allSpawns;
    }

    @Override
    public boolean isWithinSpawnDistance(@Nonnull Vector3d position, double distance) {
        double distanceSquared = distance * distance;

        for (Transform point : getSpawnPoints()) {
            if (position.distanceSquaredTo(point.getPosition()) < distanceSquared) {
                return true;
            }
        }

        return false;
    }
}

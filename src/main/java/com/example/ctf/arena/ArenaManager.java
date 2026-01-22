package com.example.ctf.arena;

import com.example.ctf.CTFPlugin;
import com.example.ctf.FlagTeam;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.util.Config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Manages CTF arena configuration including spawn points, capture zones, and protected regions.
 * Handles persistence via the plugin config system.
 */
public class ArenaManager {

    private final CTFPlugin plugin;
    private final Config<ArenaConfig> config;

    // Pending protected region creation (for two-step command)
    private final Map<UUID, PendingRegion> pendingRegions;

    public ArenaManager(@Nonnull CTFPlugin plugin, @Nonnull Config<ArenaConfig> config) {
        this.plugin = plugin;
        this.config = config;
        this.pendingRegions = new HashMap<>();
    }

    /**
     * Gets the arena configuration.
     */
    @Nonnull
    public ArenaConfig getConfig() {
        return config.get();
    }

    /**
     * Saves the arena configuration to disk.
     */
    public void save() {
        config.save().thenRun(() -> {
            plugin.getLogger().atInfo().log("Arena configuration saved");
        });
    }

    // ==================== Spawn Points ====================

    /**
     * Adds a spawn point for a team.
     *
     * @param team The team
     * @param transform The spawn transform (position + rotation)
     */
    public void addSpawnPoint(@Nonnull FlagTeam team, @Nonnull Transform transform) {
        ArenaConfig cfg = getConfig();
        Transform[] currentSpawns = cfg.getSpawns(team);
        Transform[] newSpawns = Arrays.copyOf(currentSpawns, currentSpawns.length + 1);
        newSpawns[newSpawns.length - 1] = transform;

        switch (team) {
            case RED -> cfg.setRedSpawns(newSpawns);
            case BLUE -> cfg.setBlueSpawns(newSpawns);
        }

        plugin.getLogger().atInfo().log("Added {} spawn point at {}", team, transform.getPosition());
    }

    /**
     * Clears all spawn points for a team.
     *
     * @param team The team
     */
    public void clearSpawnPoints(@Nonnull FlagTeam team) {
        ArenaConfig cfg = getConfig();
        switch (team) {
            case RED -> cfg.setRedSpawns(new Transform[0]);
            case BLUE -> cfg.setBlueSpawns(new Transform[0]);
        }
        plugin.getLogger().atInfo().log("Cleared all {} spawn points", team);
    }

    /**
     * Gets a spawn point for a player. Uses hash-based selection for consistency.
     *
     * @param team The player's team
     * @param playerUuid The player's UUID (for spawn selection)
     * @return A spawn transform, or null if no spawns configured
     */
    @Nullable
    public Transform getSpawnPoint(@Nonnull FlagTeam team, @Nonnull UUID playerUuid) {
        Transform[] spawns = getConfig().getSpawns(team);
        if (spawns.length == 0) {
            return null;
        }
        // Use player UUID hash for consistent spawn assignment
        int index = Math.abs(playerUuid.hashCode()) % spawns.length;
        return spawns[index];
    }

    /**
     * Gets the number of spawn points for a team.
     */
    public int getSpawnCount(@Nonnull FlagTeam team) {
        return getConfig().getSpawns(team).length;
    }

    // ==================== Capture Zones ====================

    /**
     * Sets the capture zone for a team.
     *
     * @param team The team (where this team captures enemy flags)
     * @param center The center position
     * @param radius The capture radius
     */
    public void setCaptureZone(@Nonnull FlagTeam team, @Nonnull Vector3d center, double radius) {
        ArenaConfig cfg = getConfig();
        CaptureZone zone = new CaptureZone(center, radius);

        switch (team) {
            case RED -> cfg.setRedCaptureZone(zone);
            case BLUE -> cfg.setBlueCaptureZone(zone);
        }

        plugin.getLogger().atInfo().log("Set {} capture zone at {} with radius {}", team, center, radius);
    }

    /**
     * Checks if a position is within a team's capture zone.
     *
     * @param team The team whose capture zone to check
     * @param position The position to check
     * @return true if the position is in the capture zone
     */
    public boolean isInCaptureZone(@Nonnull FlagTeam team, @Nonnull Vector3d position) {
        CaptureZone zone = getConfig().getCaptureZone(team);
        return zone != null && zone.contains(position);
    }

    // ==================== Protected Regions ====================

    /**
     * Starts creating a protected region by marking position 1.
     *
     * @param playerUuid The player creating the region
     * @param name The region name
     * @param position Position 1
     */
    public void startProtectedRegion(@Nonnull UUID playerUuid, @Nonnull String name, @Nonnull Vector3d position) {
        pendingRegions.put(playerUuid, new PendingRegion(name, position));
    }

    /**
     * Completes creating a protected region by setting position 2.
     *
     * @param playerUuid The player creating the region
     * @param position Position 2
     * @return The created region, or null if no pending region
     */
    @Nullable
    public ProtectedRegion finishProtectedRegion(@Nonnull UUID playerUuid, @Nonnull Vector3d position) {
        PendingRegion pending = pendingRegions.remove(playerUuid);
        if (pending == null) {
            return null;
        }

        ProtectedRegion region = new ProtectedRegion(pending.name, pending.position1, position);
        addProtectedRegion(region);
        return region;
    }

    /**
     * Checks if a player has a pending region creation.
     */
    @Nullable
    public String getPendingRegionName(@Nonnull UUID playerUuid) {
        PendingRegion pending = pendingRegions.get(playerUuid);
        return pending != null ? pending.name : null;
    }

    /**
     * Adds a protected region.
     *
     * @param region The region to add
     */
    public void addProtectedRegion(@Nonnull ProtectedRegion region) {
        ArenaConfig cfg = getConfig();
        ProtectedRegion[] current = cfg.getProtectedRegions();
        ProtectedRegion[] newRegions = Arrays.copyOf(current, current.length + 1);
        newRegions[newRegions.length - 1] = region;
        cfg.setProtectedRegions(newRegions);
        plugin.getLogger().atInfo().log("Added protected region '{}': {} to {}", region.getName(), region.getMin(), region.getMax());
    }

    /**
     * Removes a protected region by name.
     *
     * @param name The region name
     * @return true if the region was removed
     */
    public boolean removeProtectedRegion(@Nonnull String name) {
        ArenaConfig cfg = getConfig();
        ProtectedRegion[] current = cfg.getProtectedRegions();
        ProtectedRegion[] filtered = Arrays.stream(current)
            .filter(r -> !r.getName().equalsIgnoreCase(name))
            .toArray(ProtectedRegion[]::new);

        if (filtered.length == current.length) {
            return false; // Not found
        }

        cfg.setProtectedRegions(filtered);
        plugin.getLogger().atInfo().log("Removed protected region '{}'", name);
        return true;
    }

    /**
     * Checks if a block position is within any protected region.
     *
     * @param blockPos The block position
     * @return true if the block is protected
     */
    public boolean isBlockProtected(@Nonnull Vector3i blockPos) {
        for (ProtectedRegion region : getConfig().getProtectedRegions()) {
            if (region.containsBlock(blockPos)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets all protected region names.
     */
    @Nonnull
    public List<String> getProtectedRegionNames() {
        return Arrays.stream(getConfig().getProtectedRegions())
            .map(ProtectedRegion::getName)
            .toList();
    }

    /**
     * Gets a protected region by name.
     */
    @Nullable
    public ProtectedRegion getProtectedRegion(@Nonnull String name) {
        for (ProtectedRegion region : getConfig().getProtectedRegions()) {
            if (region.getName().equalsIgnoreCase(name)) {
                return region;
            }
        }
        return null;
    }

    // ==================== Score Limit ====================

    /**
     * Gets the score limit from config.
     */
    public int getScoreLimit() {
        return getConfig().getScoreLimit();
    }

    /**
     * Sets the score limit in config.
     */
    public void setScoreLimit(int limit) {
        getConfig().setScoreLimit(limit);
    }

    // ==================== Direct Position Methods ====================

    /**
     * Adds a protected region directly from min/max positions.
     * Used by the setup tool interaction.
     */
    public void addProtectedRegion(@Nonnull String name, @Nonnull Vector3d min, @Nonnull Vector3d max) {
        ProtectedRegion region = new ProtectedRegion(name, min, max);
        addProtectedRegion(region);
    }

    // ==================== Preset Management ====================

    /**
     * Gets the presets directory path.
     */
    @Nonnull
    private Path getPresetsDir() {
        return plugin.getDataDirectory().resolve("presets");
    }

    /**
     * Saves the current arena configuration as a named preset.
     * TODO: Implement proper JSON serialization using Codec API
     *
     * @param name The preset name
     * @return true if saved successfully
     */
    public boolean savePreset(@Nonnull String name) {
        // TODO: Codec serialization needs proper implementation
        plugin.getLogger().atWarning().log("Preset saving not yet implemented");
        return false;
    }

    /**
     * Loads a preset and applies it to the current configuration.
     * TODO: Implement proper JSON deserialization using Codec API
     *
     * @param name The preset name
     * @return true if loaded successfully
     */
    public boolean loadPreset(@Nonnull String name) {
        // TODO: Codec deserialization needs proper implementation
        plugin.getLogger().atWarning().log("Preset loading not yet implemented");
        return false;
    }

    /**
     * Deletes a saved preset.
     *
     * @param name The preset name
     * @return true if deleted successfully
     */
    public boolean deletePreset(@Nonnull String name) {
        try {
            Path presetFile = getPresetsDir().resolve(name + ".json");
            if (!Files.exists(presetFile)) {
                return false;
            }
            Files.delete(presetFile);
            plugin.getLogger().atInfo().log("Deleted preset '{}'", name);
            return true;
        } catch (IOException e) {
            plugin.getLogger().atWarning().withCause(e).log("Failed to delete preset '{}'", name);
            return false;
        }
    }

    /**
     * Lists all available presets.
     *
     * @return List of preset names (without .json extension)
     */
    @Nonnull
    public List<String> listPresets() {
        List<String> presets = new ArrayList<>();
        Path presetsDir = getPresetsDir();

        if (!Files.exists(presetsDir)) {
            return presets;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(presetsDir, "*.json")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                presets.add(fileName.substring(0, fileName.length() - 5)); // Remove .json
            }
        } catch (IOException e) {
            plugin.getLogger().atWarning().withCause(e).log("Failed to list presets");
        }

        Collections.sort(presets);
        return presets;
    }

    // ==================== Internal Classes ====================

    private static class PendingRegion {
        final String name;
        final Vector3d position1;

        PendingRegion(String name, Vector3d position1) {
            this.name = name;
            this.position1 = position1;
        }
    }
}

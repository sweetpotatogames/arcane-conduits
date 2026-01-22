package com.example.ctf.editor;

import com.example.ctf.CTFPlugin;
import com.example.ctf.FlagData;
import com.example.ctf.FlagState;
import com.example.ctf.FlagTeam;
import com.example.ctf.arena.ArenaConfig;
import com.example.ctf.arena.CaptureZone;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.packets.worldmap.MapMarker;
import com.hypixel.hytale.server.core.asset.type.gameplay.GameplayConfig;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;
import com.hypixel.hytale.server.core.util.PositionUtil;

import javax.annotation.Nonnull;

/**
 * Provides map markers for CTF elements:
 * - Flag stands (red/blue flag icons)
 * - Capture zones (circular markers)
 * - Team spawn points
 * - Dropped flags
 */
public class CTFMarkerProvider implements WorldMapManager.MarkerProvider {

    private final CTFPlugin plugin;

    public CTFMarkerProvider(@Nonnull CTFPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void update(
        @Nonnull World world,
        @Nonnull GameplayConfig gameplayConfig,
        @Nonnull WorldMapTracker tracker,
        int chunkViewRadius,
        int playerChunkX,
        int playerChunkZ
    ) {
        // Show flag stand markers
        addFlagStandMarkers(tracker, chunkViewRadius, playerChunkX, playerChunkZ);

        // Show capture zone markers
        addCaptureZoneMarkers(tracker, chunkViewRadius, playerChunkX, playerChunkZ);

        // Show spawn point markers
        addSpawnMarkers(tracker, chunkViewRadius, playerChunkX, playerChunkZ);

        // Show dropped flag markers
        addDroppedFlagMarkers(tracker, chunkViewRadius, playerChunkX, playerChunkZ);
    }

    private void addFlagStandMarkers(WorldMapTracker tracker, int chunkViewRadius, int playerChunkX, int playerChunkZ) {
        for (FlagTeam team : FlagTeam.values()) {
            FlagData flagData = plugin.getFlagCarrierManager().getFlagData(team);
            Vector3d standPos = flagData.getStandPosition();

            if (standPos != null) {
                String markerId = "ctf_stand_" + team.name().toLowerCase();
                String markerName = team.getDisplayName() + " Flag Stand";
                String markerImage = team == FlagTeam.RED ? "Icons/CTF/Flag_Red.png" : "Icons/CTF/Flag_Blue.png";

                // Show flag at stand only if it's actually there
                if (flagData.getState() == FlagState.AT_STAND) {
                    tracker.trySendMarker(
                        chunkViewRadius,
                        playerChunkX,
                        playerChunkZ,
                        standPos,
                        0f,
                        markerId,
                        markerName,
                        standPos,
                        (id, name, pos) -> new MapMarker(id, name, markerImage, PositionUtil.toTransformPacket(new Transform(pos)), null)
                    );
                } else {
                    // Show empty stand marker
                    tracker.trySendMarker(
                        chunkViewRadius,
                        playerChunkX,
                        playerChunkZ,
                        standPos,
                        0f,
                        markerId + "_empty",
                        markerName + " (Empty)",
                        standPos,
                        (id, name, pos) -> new MapMarker(id, name, "Icons/CTF/Stand_Empty.png", PositionUtil.toTransformPacket(new Transform(pos)), null)
                    );
                }
            }
        }
    }

    private void addCaptureZoneMarkers(WorldMapTracker tracker, int chunkViewRadius, int playerChunkX, int playerChunkZ) {
        if (plugin.getArenaManager() == null) return;

        ArenaConfig config = plugin.getArenaManager().getConfig();

        for (FlagTeam team : FlagTeam.values()) {
            CaptureZone zone = config.getCaptureZone(team);
            if (zone != null) {
                Vector3d center = zone.getCenter();
                String markerId = "ctf_capture_" + team.name().toLowerCase();
                String markerName = team.getDisplayName() + " Capture Zone";
                String markerImage = team == FlagTeam.RED ? "Icons/CTF/Capture_Red.png" : "Icons/CTF/Capture_Blue.png";

                tracker.trySendMarker(
                    chunkViewRadius,
                    playerChunkX,
                    playerChunkZ,
                    center,
                    0f,
                    markerId,
                    markerName,
                    center,
                    (id, name, pos) -> new MapMarker(id, name, markerImage, PositionUtil.toTransformPacket(new Transform(pos)), null)
                );
            }
        }
    }

    private void addSpawnMarkers(WorldMapTracker tracker, int chunkViewRadius, int playerChunkX, int playerChunkZ) {
        if (plugin.getArenaManager() == null) return;

        ArenaConfig config = plugin.getArenaManager().getConfig();

        for (FlagTeam team : FlagTeam.values()) {
            Transform[] spawns = config.getSpawns(team);
            String markerImage = team == FlagTeam.RED ? "Icons/CTF/Spawn_Red.png" : "Icons/CTF/Spawn_Blue.png";

            for (int i = 0; i < spawns.length; i++) {
                Transform spawn = spawns[i];
                Vector3d pos = spawn.getPosition();
                String markerId = "ctf_spawn_" + team.name().toLowerCase() + "_" + i;
                String markerName = team.getDisplayName() + " Spawn #" + (i + 1);

                tracker.trySendMarker(
                    chunkViewRadius,
                    playerChunkX,
                    playerChunkZ,
                    pos,
                    spawn.getRotation().getYaw(),
                    markerId,
                    markerName,
                    pos,
                    (id, name, position) -> new MapMarker(id, name, markerImage, PositionUtil.toTransformPacket(spawn), null)
                );
            }
        }
    }

    private void addDroppedFlagMarkers(WorldMapTracker tracker, int chunkViewRadius, int playerChunkX, int playerChunkZ) {
        for (FlagTeam team : FlagTeam.values()) {
            FlagData flagData = plugin.getFlagCarrierManager().getFlagData(team);

            if (flagData.getState() == FlagState.DROPPED) {
                Vector3d droppedPos = flagData.getDroppedPosition();
                if (droppedPos != null) {
                    String markerId = "ctf_dropped_" + team.name().toLowerCase();
                    String markerName = team.getDisplayName() + " Flag (Dropped)";
                    String markerImage = team == FlagTeam.RED ? "Icons/CTF/Flag_Red_Dropped.png" : "Icons/CTF/Flag_Blue_Dropped.png";

                    tracker.trySendMarker(
                        chunkViewRadius,
                        playerChunkX,
                        playerChunkZ,
                        droppedPos,
                        0f,
                        markerId,
                        markerName,
                        droppedPos,
                        (id, name, pos) -> new MapMarker(id, name, markerImage, PositionUtil.toTransformPacket(new Transform(pos)), null)
                    );
                }
            }
        }
    }
}

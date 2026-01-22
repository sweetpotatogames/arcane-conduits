package com.example.ctf.spawn;

import com.example.ctf.CTFPlugin;
import com.example.ctf.FlagTeam;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.asset.type.gameplay.respawn.RespawnController;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Respawn controller for CTF that respawns players at their team's spawn point.
 * Falls back to world spawn if player has no team or no spawns configured.
 */
public class CTFRespawnController implements RespawnController {

    private final CTFPlugin plugin;

    public CTFRespawnController(@Nonnull CTFPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void respawnPlayer(@Nonnull World world, @Nonnull Ref<EntityStore> playerReference, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        // Get player UUID
        UUIDComponent uuidComponent = commandBuffer.getComponent(playerReference, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            fallbackRespawn(world, playerReference, commandBuffer);
            return;
        }

        UUID playerUuid = uuidComponent.getUuid();
        FlagTeam team = plugin.getTeamManager().getPlayerTeam(playerUuid);

        Transform spawnPoint = null;

        if (team != null) {
            // Get team-specific spawn point
            spawnPoint = plugin.getArenaManager().getSpawnPoint(team, playerUuid);
        }

        if (spawnPoint == null) {
            // Fall back to world spawn
            fallbackRespawn(world, playerReference, commandBuffer);
            return;
        }

        // Teleport to team spawn - use constructor directly
        Teleport teleportComponent = new Teleport(
            spawnPoint.getPosition(),
            spawnPoint.getRotation()
        );
        commandBuffer.addComponent(playerReference, Teleport.getComponentType(), teleportComponent);

        plugin.getLogger().atInfo().log("Respawned player {} at {} team spawn", playerUuid, team);
    }

    private void fallbackRespawn(@Nonnull World world, @Nonnull Ref<EntityStore> playerReference, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        // Use world's default spawn provider
        ISpawnProvider spawnProvider = world.getWorldConfig().getSpawnProvider();
        if (spawnProvider != null) {
            Transform spawnPoint = spawnProvider.getSpawnPoint(playerReference, commandBuffer);
            Teleport teleportComponent = new Teleport(
                spawnPoint.getPosition(),
                spawnPoint.getRotation()
            );
            commandBuffer.addComponent(playerReference, Teleport.getComponentType(), teleportComponent);
        }
    }
}

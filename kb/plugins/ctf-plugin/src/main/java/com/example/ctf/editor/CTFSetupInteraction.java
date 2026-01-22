package com.example.ctf.editor;

import com.example.ctf.CTFPlugin;
import com.example.ctf.FlagTeam;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interaction handler for the CTF Setup Tool.
 *
 * Primary click (left): Sets position for current mode
 * Secondary click (right): Handled by CTFSetupCycleInteraction
 *
 * Modes:
 * - RED_SPAWN: Add red team spawn point
 * - BLUE_SPAWN: Add blue team spawn point
 * - RED_CAPTURE: Set red capture zone center
 * - BLUE_CAPTURE: Set blue capture zone center
 * - RED_FLAG: Set red flag stand position
 * - BLUE_FLAG: Set blue flag stand position
 * - PROTECT_1: Mark first corner of protected region
 * - PROTECT_2: Mark second corner and create region
 */
public class CTFSetupInteraction extends SimpleInstantInteraction {

    @Nonnull
    public static final BuilderCodec<CTFSetupInteraction> CODEC = BuilderCodec.builder(
            CTFSetupInteraction.class, CTFSetupInteraction::new, SimpleInstantInteraction.CODEC
        )
        .documentation("CTF Setup Tool interaction - sets positions based on current mode.")
        .build();

    /**
     * Setup modes for the CTF Setup Tool.
     */
    public enum SetupMode {
        RED_SPAWN("Red Spawn", "Sets a spawn point for the Red team"),
        BLUE_SPAWN("Blue Spawn", "Sets a spawn point for the Blue team"),
        RED_CAPTURE("Red Capture Zone", "Sets the capture zone for Red team"),
        BLUE_CAPTURE("Blue Capture Zone", "Sets the capture zone for Blue team"),
        RED_FLAG("Red Flag Stand", "Sets the Red flag stand position"),
        BLUE_FLAG("Blue Flag Stand", "Sets the Blue flag stand position"),
        PROTECT_1("Protection Corner 1", "Marks the first corner of a protected region"),
        PROTECT_2("Protection Corner 2", "Marks the second corner and creates the region");

        private final String displayName;
        private final String description;

        SetupMode(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public SetupMode next() {
            SetupMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    // Per-player setup state
    private static final Map<UUID, SetupMode> playerModes = new ConcurrentHashMap<>();
    private static final Map<UUID, Vector3i> pendingCorner1 = new ConcurrentHashMap<>();
    private static final Map<UUID, String> pendingRegionNames = new ConcurrentHashMap<>();

    // Default capture zone radius
    private static final double DEFAULT_CAPTURE_RADIUS = 3.0;

    public CTFSetupInteraction() {
    }

    /**
     * Gets the current setup mode for a player.
     */
    public static SetupMode getPlayerMode(UUID playerUuid) {
        return playerModes.getOrDefault(playerUuid, SetupMode.RED_SPAWN);
    }

    /**
     * Sets the setup mode for a player.
     */
    public static void setPlayerMode(UUID playerUuid, SetupMode mode) {
        playerModes.put(playerUuid, mode);
    }

    /**
     * Cycles to the next setup mode for a player.
     */
    public static SetupMode cyclePlayerMode(UUID playerUuid) {
        SetupMode current = getPlayerMode(playerUuid);
        SetupMode next = current.next();
        playerModes.put(playerUuid, next);
        return next;
    }

    /**
     * Sets the pending region name for a player (used for PROTECT modes).
     */
    public static void setPendingRegionName(UUID playerUuid, String name) {
        pendingRegionNames.put(playerUuid, name);
    }

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        if (type != InteractionType.Primary) {
            return;
        }

        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        assert commandBuffer != null;

        Ref<EntityStore> ref = context.getEntity();
        Player playerComponent = commandBuffer.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) {
            return;
        }

        // Get PlayerRef for messaging
        @SuppressWarnings("deprecation")
        PlayerRef playerRef = playerComponent.getPlayerRef();
        if (playerRef == null) {
            return;
        }

        // Check if player has admin permissions
        if (!playerComponent.hasPermission("ctf.admin")) {
            playerRef.sendMessage(Message.raw("§cYou need admin permissions to use the CTF Setup Tool."));
            return;
        }

        UUIDComponent uuidComponent = commandBuffer.getComponent(ref, UUIDComponent.getComponentType());
        assert uuidComponent != null;
        UUID playerUuid = uuidComponent.getUuid();

        // Get the block the player is looking at
        Vector3i targetBlock = TargetUtil.getTargetBlock(ref, 200.0, commandBuffer);
        if (targetBlock == null) {
            playerRef.sendMessage(Message.raw("§cNo block in range. Look at a block and try again."));
            return;
        }

        // Get player's current transform for rotation data
        TransformComponent transformComponent = commandBuffer.getComponent(ref, TransformComponent.getComponentType());
        assert transformComponent != null;

        // Get the CTF plugin
        CTFPlugin plugin = CTFPlugin.get();
        if (plugin == null || plugin.getArenaManager() == null) {
            playerRef.sendMessage(Message.raw("§cCTF Plugin is not initialized."));
            return;
        }

        SetupMode mode = getPlayerMode(playerUuid);
        Vector3d blockCenter = new Vector3d(targetBlock.getX() + 0.5, targetBlock.getY() + 1.0, targetBlock.getZ() + 0.5);

        switch (mode) {
            case RED_SPAWN -> handleSpawnSet(playerRef, plugin, FlagTeam.RED, blockCenter, transformComponent);
            case BLUE_SPAWN -> handleSpawnSet(playerRef, plugin, FlagTeam.BLUE, blockCenter, transformComponent);
            case RED_CAPTURE -> handleCaptureSet(playerRef, plugin, FlagTeam.RED, blockCenter);
            case BLUE_CAPTURE -> handleCaptureSet(playerRef, plugin, FlagTeam.BLUE, blockCenter);
            case RED_FLAG -> handleFlagStandSet(playerRef, plugin, FlagTeam.RED, blockCenter);
            case BLUE_FLAG -> handleFlagStandSet(playerRef, plugin, FlagTeam.BLUE, blockCenter);
            case PROTECT_1 -> handleProtectCorner1(playerRef, playerUuid, targetBlock);
            case PROTECT_2 -> handleProtectCorner2(playerRef, playerUuid, plugin, targetBlock);
        }
    }

    private void handleSpawnSet(PlayerRef playerRef, CTFPlugin plugin, FlagTeam team, Vector3d position, TransformComponent transform) {
        // Create transform with player's rotation for spawn direction
        Transform spawnTransform = new Transform(position, transform.getRotation());
        plugin.getArenaManager().addSpawnPoint(team, spawnTransform);

        String color = team == FlagTeam.RED ? "§c" : "§9";
        playerRef.sendMessage(Message.raw(color + "Added " + team.getDisplayName() + " spawn point at " + formatPosition(position)));
    }

    private void handleCaptureSet(PlayerRef playerRef, CTFPlugin plugin, FlagTeam team, Vector3d position) {
        plugin.getArenaManager().setCaptureZone(team, position, DEFAULT_CAPTURE_RADIUS);

        String color = team == FlagTeam.RED ? "§c" : "§9";
        playerRef.sendMessage(Message.raw(color + "Set " + team.getDisplayName() + " capture zone at " + formatPosition(position) + " (radius: " + DEFAULT_CAPTURE_RADIUS + ")"));
    }

    private void handleFlagStandSet(PlayerRef playerRef, CTFPlugin plugin, FlagTeam team, Vector3d position) {
        plugin.getFlagCarrierManager().setFlagStandPosition(team, position);

        String color = team == FlagTeam.RED ? "§c" : "§9";
        playerRef.sendMessage(Message.raw(color + "Set " + team.getDisplayName() + " flag stand at " + formatPosition(position)));
    }

    private void handleProtectCorner1(PlayerRef playerRef, UUID playerUuid, Vector3i block) {
        pendingCorner1.put(playerUuid, block);
        playerRef.sendMessage(Message.raw("§eProtected region corner 1 set at " + formatBlockPosition(block)));
        playerRef.sendMessage(Message.raw("§7Now click on the opposite corner (or cycle to PROTECT_2 mode first)."));

        // Auto-advance to corner 2 mode
        playerModes.put(playerUuid, SetupMode.PROTECT_2);
        playerRef.sendMessage(Message.raw("§aSwitched to: Protection Corner 2"));
    }

    private void handleProtectCorner2(PlayerRef playerRef, UUID playerUuid, CTFPlugin plugin, Vector3i block) {
        Vector3i corner1 = pendingCorner1.get(playerUuid);
        if (corner1 == null) {
            playerRef.sendMessage(Message.raw("§cNo first corner set. Switch to PROTECT_1 mode and set corner 1 first."));
            return;
        }

        String regionName = pendingRegionNames.getOrDefault(playerUuid, "region_" + System.currentTimeMillis());

        Vector3d min = new Vector3d(corner1.x, corner1.y, corner1.z);
        Vector3d max = new Vector3d(block.x, block.y, block.z);

        plugin.getArenaManager().addProtectedRegion(regionName, min, max);

        // Clear pending data
        pendingCorner1.remove(playerUuid);
        pendingRegionNames.remove(playerUuid);

        playerRef.sendMessage(Message.raw("§aCreated protected region '" + regionName + "' from " + formatBlockPosition(corner1) + " to " + formatBlockPosition(block)));

        // Return to first protect mode for next region
        playerModes.put(playerUuid, SetupMode.PROTECT_1);
        playerRef.sendMessage(Message.raw("§aSwitched to: Protection Corner 1"));
    }

    private String formatPosition(Vector3d pos) {
        return String.format("(%.1f, %.1f, %.1f)", pos.x, pos.y, pos.z);
    }

    private String formatBlockPosition(Vector3i pos) {
        return String.format("(%d, %d, %d)", pos.x, pos.y, pos.z);
    }

    @Nonnull
    @Override
    public String toString() {
        return "CTFSetupInteraction{} " + super.toString();
    }
}

package com.example.ctf;

import com.example.ctf.team.TeamManager;
import com.example.ctf.ui.CTFAnnouncementManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.MovementSettings;
import com.hypixel.hytale.protocol.packets.inventory.SetActiveSlot;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages CTF flags and their carriers.
 *
 * Restrictions when carrying a flag:
 * - Movement speed reduced to 80%
 * - Sprint speed reduced to 70%
 * - Mantling (2-block climb) disabled
 * - Cannot switch to other hotbar slots (locked to flag slot)
 * - Two-handed weapons blocked
 * - Can only drop flag with G key
 */
public class FlagCarrierManager {

    // Speed reduction multipliers when carrying a flag
    private static final float FLAG_SPEED_MULTIPLIER = 0.80f;
    private static final float FLAG_SPRINT_MULTIPLIER = 0.70f;
    private static final float FLAG_CLIMB_MULTIPLIER = 0.0f;

    // How often to run tick checks (in milliseconds)
    private static final long TICK_INTERVAL_MS = 50;

    // Flag slot is always slot 0 (first hotbar slot)
    private static final byte FLAG_SLOT = 0;

    private final CTFPlugin plugin;

    // Track flag data for each team
    private final Map<FlagTeam, FlagData> flags = new EnumMap<>(FlagTeam.class);

    // Track carriers: UUID -> saved movement settings
    private final Map<UUID, SavedMovementSettings> carrierSettings = new ConcurrentHashMap<>();

    // Scheduled task for periodic checks
    private ScheduledFuture<?> tickTask;

    public FlagCarrierManager(@Nonnull CTFPlugin plugin) {
        this.plugin = plugin;

        // Initialize flag data for each team
        for (FlagTeam team : FlagTeam.values()) {
            flags.put(team, new FlagData(team));
        }

        startTickTask();
        plugin.getLogger().atInfo().log("FlagCarrierManager initialized");
    }

    /**
     * Starts the periodic tick task for flag management.
     */
    private void startTickTask() {
        tickTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
            this::tick,
            TICK_INTERVAL_MS,
            TICK_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * Periodic tick - handles slot enforcement, death detection, capture detection, and flag timeout checks.
     */
    private void tick() {
        // Check for dead carriers, captures, and enforce slot locks
        for (var entry : carrierSettings.entrySet()) {
            UUID playerUuid = entry.getKey();
            try {
                // Check if carrier died
                if (checkCarrierDeath(playerUuid)) {
                    continue; // Flag was dropped, skip other checks
                }

                // Check for flag capture
                if (checkCapture(playerUuid)) {
                    continue; // Flag was captured, skip slot enforcement
                }

                enforceSlotForCarrier(playerUuid);
            } catch (Exception e) {
                plugin.getLogger().atWarning().withCause(e)
                    .log("Error processing tick for player {}", playerUuid);
            }
        }

        // Check for dropped flags that should return to stand
        for (FlagData flagData : flags.values()) {
            if (flagData.shouldReturnToStand()) {
                returnFlagToStand(flagData.getTeam(), true); // true = timeout
                plugin.getLogger().atInfo().log("{} flag returned to stand (timeout)",
                    flagData.getTeam().getDisplayName());
            }
        }
    }

    /**
     * Checks if a flag carrier is in position to capture and processes the capture if so.
     *
     * @param playerUuid The UUID of the flag carrier
     * @return true if a capture occurred
     */
    private boolean checkCapture(@Nonnull UUID playerUuid) {
        // Get managers - if not initialized, skip capture detection
        if (plugin.getTeamManager() == null || plugin.getArenaManager() == null || plugin.getMatchManager() == null) {
            return false;
        }

        // Only process captures during an active match
        if (!plugin.getMatchManager().isMatchActive()) {
            return false;
        }

        // Get what flag the player is carrying
        FlagTeam carriedFlagTeam = getCarriedFlagTeam(playerUuid);
        if (carriedFlagTeam == null) {
            return false;
        }

        // Get the carrier's team
        FlagTeam carrierTeam = plugin.getTeamManager().getPlayerTeam(playerUuid);
        if (carrierTeam == null) {
            return false; // Player must be on a team to capture
        }

        // Can only capture the enemy flag (not your own)
        if (carriedFlagTeam == carrierTeam) {
            return false;
        }

        // Standard CTF rule: your own flag must be at its stand to capture
        FlagData ownFlagData = flags.get(carrierTeam);
        if (!ownFlagData.isAtStand()) {
            return false; // Can't capture while your flag is stolen
        }

        // Get the carrier's position
        PlayerRef playerRef = findPlayerRef(playerUuid);
        if (playerRef == null) {
            return false;
        }

        Ref<EntityStore> entityRef = playerRef.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return false;
        }

        TransformComponent transform = entityRef.getStore()
            .getComponent(entityRef, TransformComponent.getComponentType());
        if (transform == null) {
            return false;
        }

        Vector3d position = transform.getPosition();

        // Check if carrier is in their team's capture zone
        if (!plugin.getArenaManager().isInCaptureZone(carrierTeam, position)) {
            return false;
        }

        // CAPTURE! Process the score
        processCapture(playerUuid, carrierTeam, carriedFlagTeam);
        return true;
    }

    /**
     * Processes a successful flag capture.
     */
    private void processCapture(@Nonnull UUID playerUuid, @Nonnull FlagTeam scoringTeam, @Nonnull FlagTeam capturedFlagTeam) {
        plugin.getLogger().atInfo().log("{} team captured the {} flag! (Player: {})",
            scoringTeam.getDisplayName(), capturedFlagTeam.getDisplayName(), playerUuid);

        // Get player name for announcement before returning flag
        String playerName = "Unknown";
        PlayerRef playerRef = findPlayerRef(playerUuid);
        if (playerRef != null) {
            playerName = playerRef.getUsername();
        }

        // Return the captured flag to its stand (don't announce this as it's part of capture)
        FlagData flagData = flags.get(capturedFlagTeam);
        flagData.returnToStand(); // Direct call to avoid double announcement

        // Add score for the capturing team (this returns if they won)
        plugin.getMatchManager().addScore(scoringTeam);

        // Announce the capture with updated scores
        CTFAnnouncementManager announcementManager = plugin.getAnnouncementManager();
        if (announcementManager != null && plugin.getMatchManager() != null) {
            int redScore = plugin.getMatchManager().getScore(FlagTeam.RED);
            int blueScore = plugin.getMatchManager().getScore(FlagTeam.BLUE);
            announcementManager.announceFlagCaptured(scoringTeam, playerName, redScore, blueScore);
        }
    }

    /**
     * Checks if a flag carrier has died and drops the flag if so.
     *
     * @param playerUuid The UUID of the carrier to check
     * @return true if the carrier died and flag was dropped
     */
    private boolean checkCarrierDeath(@Nonnull UUID playerUuid) {
        PlayerRef playerRef = findPlayerRef(playerUuid);
        if (playerRef == null) {
            // Can't find player ref - they may have disconnected, drop the flag
            handleCarrierDisconnect(playerUuid);
            return true;
        }

        Ref<EntityStore> entityRef = playerRef.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            handleCarrierDisconnect(playerUuid);
            return true;
        }

        // Check if player has the DeathComponent (meaning they're dead)
        DeathComponent deathComponent = entityRef.getStore()
            .getComponent(entityRef, DeathComponent.getComponentType());

        if (deathComponent != null) {
            // Player is dead - drop the flag at their position
            TransformComponent transform = entityRef.getStore()
                .getComponent(entityRef, TransformComponent.getComponentType());

            Vector3d deathPosition;
            if (transform != null) {
                deathPosition = transform.getPosition();
            } else {
                // Fallback to flag stand position
                FlagTeam team = getCarriedFlagTeam(playerUuid);
                if (team != null) {
                    Vector3d standPos = flags.get(team).getStandPosition();
                    deathPosition = standPos != null ? standPos : new Vector3d(0, 0, 0);
                } else {
                    deathPosition = new Vector3d(0, 0, 0);
                }
            }

            // Check for killer to announce in kill feed
            FlagTeam carriedFlagTeam = getCarriedFlagTeam(playerUuid);
            announceCarrierKill(playerUuid, playerRef, deathComponent, carriedFlagTeam);

            dropFlag(playerUuid, deathPosition);
            plugin.getLogger().atInfo().log("Player {} died while carrying flag, dropped at {}",
                playerUuid, deathPosition);
            return true;
        }

        return false;
    }

    /**
     * Announces a flag carrier kill in the kill feed.
     *
     * @param victimUuid The UUID of the flag carrier who died
     * @param victimRef The player ref of the victim
     * @param deathComponent The death component with killer info
     * @param carriedFlagTeam The team whose flag was being carried
     */
    private void announceCarrierKill(@Nonnull UUID victimUuid, @Nonnull PlayerRef victimRef,
                                      @Nonnull DeathComponent deathComponent, @Nullable FlagTeam carriedFlagTeam) {
        if (carriedFlagTeam == null) {
            return;
        }

        Damage damage = deathComponent.getDeathInfo();
        if (damage == null) {
            return;
        }

        // Get victim's name and team
        String victimName = victimRef.getUsername();
        TeamManager teamManager = plugin.getTeamManager();
        FlagTeam victimTeam = teamManager != null ? teamManager.getPlayerTeam(victimUuid) : null;

        // Check if killed by another player
        Damage.Source source = damage.getSource();
        if (source instanceof Damage.EntitySource entitySource) {
            Ref<EntityStore> killerRef = entitySource.getRef();
            if (killerRef != null && killerRef.isValid()) {
                // Try to find the killer's PlayerRef
                Player killerPlayer = killerRef.getStore().getComponent(killerRef, Player.getComponentType());
                if (killerPlayer != null && killerPlayer.getPlayerRef() != null) {
                    PlayerRef killerPlayerRef = killerPlayer.getPlayerRef();
                    String killerName = killerPlayerRef.getUsername();
                    UUID killerUuid = killerPlayerRef.getUuid();
                    FlagTeam killerTeam = teamManager != null ? teamManager.getPlayerTeam(killerUuid) : null;

                    // Announce in kill feed
                    CTFAnnouncementManager announcementManager = plugin.getAnnouncementManager();
                    if (announcementManager != null && killerTeam != null && victimTeam != null) {
                        announcementManager.announceKillFeedFlagCarrierKill(
                            killerName, killerTeam,
                            victimName, victimTeam,
                            carriedFlagTeam
                        );
                    }
                }
            }
        }
    }

    /**
     * Handles a carrier disconnecting - drops flag at their last known position.
     */
    private void handleCarrierDisconnect(@Nonnull UUID playerUuid) {
        FlagTeam team = getCarriedFlagTeam(playerUuid);
        if (team == null) {
            return;
        }

        // Use the flag stand position as fallback when player disconnects
        FlagData flagData = flags.get(team);
        Vector3d dropPosition = flagData.getStandPosition();
        if (dropPosition == null) {
            dropPosition = new Vector3d(0, 0, 0);
        }

        // Force drop the flag
        flagData.drop(dropPosition);
        carrierSettings.remove(playerUuid);

        plugin.getLogger().atInfo().log("Player {} disconnected while carrying {} flag, dropped at {}",
            playerUuid, team.getDisplayName(), dropPosition);
    }

    /**
     * Enforces the slot lock for a flag carrier.
     */
    private void enforceSlotForCarrier(@Nonnull UUID playerUuid) {
        PlayerRef playerRef = findPlayerRef(playerUuid);
        if (playerRef == null) {
            return;
        }

        Ref<EntityStore> entityRef = playerRef.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        Player player = entityRef.getStore().getComponent(entityRef, Player.getComponentType());
        if (player == null) {
            return;
        }

        Inventory inventory = player.getInventory();
        byte currentSlot = inventory.getActiveHotbarSlot();

        // If player switched away from the flag slot, force them back
        if (currentSlot != FLAG_SLOT) {
            inventory.setActiveHotbarSlot(FLAG_SLOT);
            SetActiveSlot packet = new SetActiveSlot(Inventory.HOTBAR_SECTION_ID, FLAG_SLOT);
            playerRef.getPacketHandler().write(packet);
        }
    }

    /**
     * Called when a player picks up a flag.
     *
     * @param player The player picking up the flag
     * @param team The team whose flag is being picked up
     * @return true if the flag was successfully picked up
     */
    public boolean pickupFlag(@Nonnull Player player, @Nonnull FlagTeam team) {
        PlayerRef playerRef = player.getPlayerRef();
        if (playerRef == null) {
            return false;
        }

        UUID playerUuid = playerRef.getUuid();
        FlagData flagData = flags.get(team);

        // Can't pick up if already carried
        if (flagData.isCarried()) {
            return false;
        }

        // Check immunity if dropped
        if (flagData.isDropped() && flagData.hasImmunity()) {
            return false;
        }

        // Apply flag carrier restrictions
        if (!applyCarrierRestrictions(player, playerUuid)) {
            return false;
        }

        // Give the visual flag item
        giveFlagItem(player, team);

        // Update flag data
        flagData.pickup(playerUuid, playerRef);

        plugin.getLogger().atInfo().log("Player {} picked up {} flag!",
            playerUuid, team.getDisplayName());

        // Announce the pickup
        CTFAnnouncementManager announcementManager = plugin.getAnnouncementManager();
        if (announcementManager != null) {
            String playerName = playerRef.getUsername();
            announcementManager.announceFlagPickup(playerUuid, playerName, team);
        }

        return true;
    }

    /**
     * Called when a player drops their flag (G key or death).
     *
     * @param playerUuid The UUID of the player dropping the flag
     * @param dropPosition Where the flag should be dropped
     */
    public void dropFlag(@Nonnull UUID playerUuid, @Nonnull Vector3d dropPosition) {
        // Find which flag this player is carrying
        FlagData carriedFlag = null;
        for (FlagData flagData : flags.values()) {
            if (playerUuid.equals(flagData.getCarrierUuid())) {
                carriedFlag = flagData;
                break;
            }
        }

        if (carriedFlag == null) {
            return;
        }

        // Get the player to restore settings
        PlayerRef playerRef = carriedFlag.getCarrierRef();
        if (playerRef != null) {
            Ref<EntityStore> entityRef = playerRef.getReference();
            if (entityRef != null && entityRef.isValid()) {
                Player player = entityRef.getStore().getComponent(entityRef, Player.getComponentType());
                if (player != null) {
                    // Remove flag item from inventory
                    removeFlagItem(player, carriedFlag.getTeam());
                    // Restore movement settings
                    removeCarrierRestrictions(player, playerUuid);
                }
            }
        }

        // Update flag state
        FlagTeam droppedFlagTeam = carriedFlag.getTeam();
        carriedFlag.drop(dropPosition);

        plugin.getLogger().atInfo().log("{} flag dropped at {}",
            droppedFlagTeam.getDisplayName(), dropPosition);

        // Announce the drop
        CTFAnnouncementManager announcementManager = plugin.getAnnouncementManager();
        if (announcementManager != null && playerRef != null) {
            String playerName = playerRef.getUsername();
            announcementManager.announceFlagDropped(playerUuid, playerName, droppedFlagTeam);
        }
    }

    /**
     * Returns a flag to its stand (called when own team touches dropped flag or timeout).
     *
     * @param team The team whose flag should return
     */
    public void returnFlagToStand(@Nonnull FlagTeam team) {
        returnFlagToStand(team, false);
    }

    /**
     * Returns a flag to its stand (called when own team touches dropped flag or timeout).
     *
     * @param team The team whose flag should return
     * @param wasTimeout True if this is a timeout return
     */
    public void returnFlagToStand(@Nonnull FlagTeam team, boolean wasTimeout) {
        FlagData flagData = flags.get(team);

        // If carried, force drop first
        if (flagData.isCarried()) {
            UUID carrierUuid = flagData.getCarrierUuid();
            if (carrierUuid != null) {
                Vector3d dropPos = flagData.getStandPosition();
                if (dropPos == null) {
                    dropPos = new Vector3d(0, 0, 0);
                }
                dropFlag(carrierUuid, dropPos);
            }
        }

        // Only announce if it was actually dropped (not at stand already)
        boolean wasDropped = flagData.isDropped();

        flagData.returnToStand();

        plugin.getLogger().atInfo().log("{} flag returned to stand",
            team.getDisplayName());

        // Announce the return if it was dropped
        if (wasDropped) {
            CTFAnnouncementManager announcementManager = plugin.getAnnouncementManager();
            if (announcementManager != null) {
                announcementManager.announceFlagReturned(team, wasTimeout);
            }
        }
    }

    /**
     * Sets the stand position for a team's flag.
     */
    public void setFlagStandPosition(@Nonnull FlagTeam team, @Nonnull Vector3d position) {
        flags.get(team).setStandPosition(position);
        plugin.getLogger().atInfo().log("Set {} flag stand at {}",
            team.getDisplayName(), position);
    }

    /**
     * Applies movement restrictions to a flag carrier.
     */
    private boolean applyCarrierRestrictions(@Nonnull Player player, @Nonnull UUID playerUuid) {
        PlayerRef playerRef = player.getPlayerRef();
        if (playerRef == null) {
            return false;
        }

        Ref<EntityStore> entityRef = playerRef.getReference();
        if (entityRef == null) {
            return false;
        }

        MovementManager movementManager = entityRef.getStore()
            .getComponent(entityRef, MovementManager.getComponentType());

        if (movementManager == null) {
            plugin.getLogger().atWarning().log("Could not get MovementManager for player {}", playerUuid);
            return false;
        }

        // Save original settings
        MovementSettings original = movementManager.getSettings();
        SavedMovementSettings saved = new SavedMovementSettings(
            original.baseSpeed,
            original.forwardSprintSpeedMultiplier,
            original.climbSpeed,
            original.climbSpeedLateral,
            original.climbUpSprintSpeed,
            original.climbDownSprintSpeed
        );
        carrierSettings.put(playerUuid, saved);

        // Apply flag carrier restrictions
        MovementSettings settings = movementManager.getSettings();
        settings.baseSpeed *= FLAG_SPEED_MULTIPLIER;
        settings.forwardSprintSpeedMultiplier *= FLAG_SPRINT_MULTIPLIER;
        settings.climbSpeed = FLAG_CLIMB_MULTIPLIER;
        settings.climbSpeedLateral = FLAG_CLIMB_MULTIPLIER;
        settings.climbUpSprintSpeed = FLAG_CLIMB_MULTIPLIER;
        settings.climbDownSprintSpeed = FLAG_CLIMB_MULTIPLIER;

        // Send updated settings to client
        movementManager.update(playerRef.getPacketHandler());

        // Force the active slot to the flag slot
        Inventory inventory = player.getInventory();
        inventory.setActiveHotbarSlot(FLAG_SLOT);

        SetActiveSlot packet = new SetActiveSlot(Inventory.HOTBAR_SECTION_ID, FLAG_SLOT);
        playerRef.getPacketHandler().write(packet);

        plugin.getLogger().atInfo().log("Applied flag carrier restrictions to player {}", playerUuid);
        return true;
    }

    /**
     * Removes movement restrictions from a flag carrier.
     */
    private void removeCarrierRestrictions(@Nonnull Player player, @Nonnull UUID playerUuid) {
        SavedMovementSettings saved = carrierSettings.remove(playerUuid);
        if (saved == null) {
            return;
        }

        PlayerRef playerRef = player.getPlayerRef();
        if (playerRef == null) {
            return;
        }

        Ref<EntityStore> entityRef = playerRef.getReference();
        if (entityRef == null) {
            return;
        }

        MovementManager movementManager = entityRef.getStore()
            .getComponent(entityRef, MovementManager.getComponentType());

        if (movementManager == null) {
            return;
        }

        // Restore original settings
        MovementSettings settings = movementManager.getSettings();
        settings.baseSpeed = saved.baseSpeed();
        settings.forwardSprintSpeedMultiplier = saved.forwardSprintSpeedMultiplier();
        settings.climbSpeed = saved.climbSpeed();
        settings.climbSpeedLateral = saved.climbSpeedLateral();
        settings.climbUpSprintSpeed = saved.climbUpSprintSpeed();
        settings.climbDownSprintSpeed = saved.climbDownSprintSpeed();

        // Send updated settings to client
        movementManager.update(playerRef.getPacketHandler());

        plugin.getLogger().atInfo().log("Restored normal movement for player {}", playerUuid);
    }

    /**
     * Gives the visual flag item to a player.
     * TODO: Inventory API requires transaction system - implement when API is better understood
     */
    private void giveFlagItem(@Nonnull Player player, @Nonnull FlagTeam team) {
        // TODO: ItemContainer uses transaction-based API, not direct setItemStack
        // The flag visual item feature is disabled until proper inventory API integration
        plugin.getLogger().atInfo().log("Flag visual item feature pending inventory API implementation");
    }

    /**
     * Removes the flag item from a player's inventory.
     * TODO: Inventory API requires transaction system - implement when API is better understood
     */
    private void removeFlagItem(@Nonnull Player player, @Nonnull FlagTeam team) {
        // TODO: ItemContainer uses transaction-based API, not direct setItemStack
        // The flag visual item feature is disabled until proper inventory API implementation
    }

    /**
     * Finds a PlayerRef by UUID.
     */
    @Nullable
    private PlayerRef findPlayerRef(@Nonnull UUID playerUuid) {
        // Check all tracked flags for a matching carrier
        for (FlagData flagData : flags.values()) {
            if (playerUuid.equals(flagData.getCarrierUuid())) {
                return flagData.getCarrierRef();
            }
        }
        return null;
    }

    // Public query methods

    /**
     * Checks if a player is currently carrying any flag.
     */
    public boolean isCarryingFlag(@Nonnull UUID playerUuid) {
        return carrierSettings.containsKey(playerUuid);
    }

    /**
     * Gets the flag team being carried by a player, or null if not carrying.
     */
    @Nullable
    public FlagTeam getCarriedFlagTeam(@Nonnull UUID playerUuid) {
        for (FlagData flagData : flags.values()) {
            if (playerUuid.equals(flagData.getCarrierUuid())) {
                return flagData.getTeam();
            }
        }
        return null;
    }

    /**
     * Gets the carrier UUID for a team's flag, or null if not carried.
     */
    @Nullable
    public UUID getFlagCarrier(@Nonnull FlagTeam team) {
        return flags.get(team).getCarrierUuid();
    }

    /**
     * Gets the current state of a team's flag.
     */
    @Nonnull
    public FlagState getFlagState(@Nonnull FlagTeam team) {
        return flags.get(team).getState();
    }

    /**
     * Gets the FlagData for a team.
     */
    @Nonnull
    public FlagData getFlagData(@Nonnull FlagTeam team) {
        return flags.get(team);
    }

    /**
     * Returns count of players currently carrying flags.
     */
    public int getFlagCarrierCount() {
        return carrierSettings.size();
    }

    /**
     * Cleans up resources. Called on plugin shutdown.
     */
    public void cleanup() {
        plugin.getLogger().atInfo().log("Cleaning up FlagCarrierManager");

        if (tickTask != null) {
            tickTask.cancel(false);
        }

        // Restore all carriers to normal
        for (FlagTeam team : FlagTeam.values()) {
            FlagData flagData = flags.get(team);
            if (flagData.isCarried()) {
                returnFlagToStand(team);
            }
        }

        carrierSettings.clear();
    }

    /**
     * Stores the original movement settings to restore when dropping the flag.
     */
    private record SavedMovementSettings(
        float baseSpeed,
        float forwardSprintSpeedMultiplier,
        float climbSpeed,
        float climbSpeedLateral,
        float climbUpSprintSpeed,
        float climbDownSprintSpeed
    ) {}
}

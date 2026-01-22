package com.example.ctf.team;

import com.example.ctf.CTFPlugin;
import com.example.ctf.FlagTeam;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages team visual effects for players.
 * Applies colored tint effects to distinguish team members.
 *
 * Requires team effect assets to be defined in plugin resources:
 * - CTF_Team_Red (red tint effect)
 * - CTF_Team_Blue (blue tint effect)
 */
public class TeamVisualManager {

    private final CTFPlugin plugin;

    // Effect IDs (must match asset file names)
    private String redTeamEffectId = "CTF_Team_Red";
    private String blueTeamEffectId = "CTF_Team_Blue";

    // Cached effect indices (resolved lazily)
    private int redTeamEffectIndex = -1;
    private int blueTeamEffectIndex = -1;
    private boolean effectsResolved = false;

    // Track which players have team effects applied
    private final Map<UUID, FlagTeam> playerTeamEffects = new ConcurrentHashMap<>();

    public TeamVisualManager(@Nonnull CTFPlugin plugin) {
        this.plugin = plugin;
        plugin.getLogger().atInfo().log("TeamVisualManager initialized");
    }

    /**
     * Resolves effect indices from effect IDs.
     */
    private void resolveEffects() {
        if (effectsResolved) {
            return;
        }

        redTeamEffectIndex = resolveEffectIndex(redTeamEffectId);
        blueTeamEffectIndex = resolveEffectIndex(blueTeamEffectId);

        effectsResolved = true;
        plugin.getLogger().atInfo().log("Team effect indices resolved: red={}, blue={}",
            redTeamEffectIndex, blueTeamEffectIndex);
    }

    private int resolveEffectIndex(@Nonnull String effectId) {
        try {
            int index = EntityEffect.getAssetMap().getIndex(effectId);
            if (index != Integer.MIN_VALUE && index > 0) {
                return index;
            }
        } catch (Exception e) {
            plugin.getLogger().atWarning().log("Team effect not found: {} (this is expected if assets aren't loaded)", effectId);
        }
        return 0;
    }

    /**
     * Applies the team visual effect to a player.
     *
     * @param playerRef The player
     * @param team The team to apply visuals for
     */
    public void applyTeamEffect(@Nonnull PlayerRef playerRef, @Nonnull FlagTeam team) {
        resolveEffects();

        int effectIndex = getEffectIndex(team);
        if (effectIndex == 0) {
            plugin.getLogger().atWarning().log("Cannot apply team effect - effect not found for {}", team);
            return;
        }

        Ref<EntityStore> entityRef = playerRef.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        Store<EntityStore> store = entityRef.getStore();
        EffectControllerComponent effectController = store.getComponent(entityRef, EffectControllerComponent.getComponentType());

        if (effectController == null) {
            plugin.getLogger().atWarning().log("Player {} has no EffectControllerComponent", playerRef.getUuid());
            return;
        }

        EntityEffect effect = EntityEffect.getAssetMap().getAsset(effectIndex);
        if (effect == null) {
            plugin.getLogger().atWarning().log("Could not load effect asset for index {}", effectIndex);
            return;
        }

        // Remove any existing team effect first
        UUID playerUuid = playerRef.getUuid();
        FlagTeam currentTeam = playerTeamEffects.get(playerUuid);
        if (currentTeam != null && currentTeam != team) {
            removeTeamEffectInternal(entityRef, store, effectController, currentTeam);
        }

        // Apply the new team effect as infinite
        effectController.addInfiniteEffect(entityRef, effectIndex, effect, store);
        playerTeamEffects.put(playerUuid, team);

        plugin.getLogger().atInfo().log("Applied {} team effect to player {}", team, playerUuid);
    }

    /**
     * Removes the team visual effect from a player.
     *
     * @param playerRef The player
     */
    public void removeTeamEffect(@Nonnull PlayerRef playerRef) {
        resolveEffects();

        UUID playerUuid = playerRef.getUuid();
        FlagTeam currentTeam = playerTeamEffects.remove(playerUuid);
        if (currentTeam == null) {
            return; // No effect to remove
        }

        Ref<EntityStore> entityRef = playerRef.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        Store<EntityStore> store = entityRef.getStore();
        EffectControllerComponent effectController = store.getComponent(entityRef, EffectControllerComponent.getComponentType());

        if (effectController == null) {
            return;
        }

        removeTeamEffectInternal(entityRef, store, effectController, currentTeam);
        plugin.getLogger().atInfo().log("Removed {} team effect from player {}", currentTeam, playerUuid);
    }

    private void removeTeamEffectInternal(@Nonnull Ref<EntityStore> entityRef, @Nonnull Store<EntityStore> store,
                                           @Nonnull EffectControllerComponent effectController, @Nonnull FlagTeam team) {
        int effectIndex = getEffectIndex(team);
        if (effectIndex > 0) {
            effectController.removeEffect(entityRef, effectIndex, store);
        }
    }

    /**
     * Called when a player respawns. Reapplies their team effect if they have one.
     *
     * @param playerRef The respawning player
     */
    public void onPlayerRespawn(@Nonnull PlayerRef playerRef) {
        UUID playerUuid = playerRef.getUuid();
        FlagTeam team = playerTeamEffects.get(playerUuid);

        if (team != null) {
            // Re-apply the team effect after respawn
            applyTeamEffect(playerRef, team);
            plugin.getLogger().atInfo().log("Reapplied {} team effect to player {} after respawn", team, playerUuid);
        }
    }

    /**
     * Called when a player disconnects. Cleans up their tracked effect.
     *
     * @param playerUuid The disconnecting player's UUID
     */
    public void onPlayerDisconnect(@Nonnull UUID playerUuid) {
        playerTeamEffects.remove(playerUuid);
    }

    /**
     * Gets the effect index for a team.
     */
    private int getEffectIndex(@Nonnull FlagTeam team) {
        return switch (team) {
            case RED -> redTeamEffectIndex;
            case BLUE -> blueTeamEffectIndex;
        };
    }

    /**
     * Gets the current team effect applied to a player, if any.
     */
    @Nullable
    public FlagTeam getPlayerTeamEffect(@Nonnull UUID playerUuid) {
        return playerTeamEffects.get(playerUuid);
    }

    /**
     * Clears all tracked team effects. Call on shutdown.
     */
    public void cleanup() {
        playerTeamEffects.clear();
    }

    /**
     * Configure custom effect IDs. Must be called before effects are applied.
     */
    public void configureEffects(@Nonnull String redEffectId, @Nonnull String blueEffectId) {
        this.redTeamEffectId = redEffectId;
        this.blueTeamEffectId = blueEffectId;
        this.effectsResolved = false; // Force re-resolve
    }
}

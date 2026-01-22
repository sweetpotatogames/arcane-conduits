package com.example.ctf.ui;

import com.example.ctf.CTFPlugin;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Manages sound effects for CTF game events.
 * Uses existing Hytale sound events where possible.
 *
 * Sound indices are resolved at runtime from sound event IDs.
 * If a sound is not found, it will be silently skipped.
 */
public class CTFSoundManager {

    private final CTFPlugin plugin;

    // Sound event IDs (these reference existing game sounds)
    // These can be configured or replaced with actual asset IDs
    private String flagPickupSound = "UI_Positive_01"; // Pickup/collect sound
    private String flagDropSound = "UI_Negative_01"; // Drop/fail sound
    private String flagCaptureSound = "UI_LevelUp"; // Achievement/fanfare
    private String flagReturnSound = "UI_Positive_02"; // Return/restore
    private String matchStartSound = "UI_Alert_01"; // Horn/alert
    private String matchEndSound = "UI_Victory"; // Victory fanfare

    // Cached sound indices (resolved lazily)
    private int flagPickupSoundIndex = -1;
    private int flagDropSoundIndex = -1;
    private int flagCaptureSoundIndex = -1;
    private int flagReturnSoundIndex = -1;
    private int matchStartSoundIndex = -1;
    private int matchEndSoundIndex = -1;

    private boolean soundsResolved = false;

    public CTFSoundManager(@Nonnull CTFPlugin plugin) {
        this.plugin = plugin;
        plugin.getLogger().atInfo().log("CTFSoundManager initialized");
    }

    /**
     * Resolves sound indices from sound event IDs.
     * Called lazily on first sound play.
     */
    private void resolveSounds() {
        if (soundsResolved) {
            return;
        }

        flagPickupSoundIndex = resolveSoundIndex(flagPickupSound);
        flagDropSoundIndex = resolveSoundIndex(flagDropSound);
        flagCaptureSoundIndex = resolveSoundIndex(flagCaptureSound);
        flagReturnSoundIndex = resolveSoundIndex(flagReturnSound);
        matchStartSoundIndex = resolveSoundIndex(matchStartSound);
        matchEndSoundIndex = resolveSoundIndex(matchEndSound);

        soundsResolved = true;
        plugin.getLogger().atInfo().log("Sound indices resolved: pickup={}, drop={}, capture={}, return={}, start={}, end={}",
            flagPickupSoundIndex, flagDropSoundIndex, flagCaptureSoundIndex,
            flagReturnSoundIndex, matchStartSoundIndex, matchEndSoundIndex);
    }

    /**
     * Resolves a sound index from a sound ID.
     * Returns -1 if not found (which will cause SoundUtil to silently skip).
     */
    private int resolveSoundIndex(@Nonnull String soundId) {
        try {
            int index = SoundEvent.getAssetMap().getIndex(soundId);
            if (index > 0) {
                return index;
            }
        } catch (Exception e) {
            plugin.getLogger().atWarning().log("Sound event not found: {}", soundId);
        }
        return 0; // 0 is treated as "no sound" by SoundUtil
    }

    // ==================== Flag Event Sounds ====================

    /**
     * Plays the flag pickup sound to all players.
     */
    public void playFlagPickup() {
        resolveSounds();
        broadcastSound(flagPickupSoundIndex, SoundCategory.SFX);
    }

    /**
     * Plays the flag drop sound to all players.
     */
    public void playFlagDrop() {
        resolveSounds();
        broadcastSound(flagDropSoundIndex, SoundCategory.SFX);
    }

    /**
     * Plays the flag capture sound to all players.
     */
    public void playFlagCapture() {
        resolveSounds();
        broadcastSound(flagCaptureSoundIndex, SoundCategory.SFX, 1.2f, 1.0f);
    }

    /**
     * Plays the flag return sound to all players.
     */
    public void playFlagReturn() {
        resolveSounds();
        broadcastSound(flagReturnSoundIndex, SoundCategory.SFX);
    }

    // ==================== Match Event Sounds ====================

    /**
     * Plays the match start sound to all players.
     */
    public void playMatchStart() {
        resolveSounds();
        broadcastSound(matchStartSoundIndex, SoundCategory.SFX, 1.0f, 0.8f);
    }

    /**
     * Plays the match end sound to all players.
     */
    public void playMatchEnd() {
        resolveSounds();
        broadcastSound(matchEndSoundIndex, SoundCategory.SFX, 1.0f, 1.0f);
    }

    // ==================== Helper Methods ====================

    /**
     * Broadcasts a 2D sound to all players in all worlds.
     */
    private void broadcastSound(int soundIndex, @Nonnull SoundCategory category) {
        broadcastSound(soundIndex, category, 1.0f, 1.0f);
    }

    /**
     * Broadcasts a 2D sound to all players in all worlds with custom volume/pitch.
     */
    private void broadcastSound(int soundIndex, @Nonnull SoundCategory category, float volume, float pitch) {
        if (soundIndex == 0) {
            return; // No sound configured
        }

        for (World world : Universe.get().getWorlds().values()) {
            world.execute(() -> {
                Store<EntityStore> store = world.getEntityStore().getStore();
                SoundUtil.playSoundEvent2d(soundIndex, category, volume, pitch, store);
            });
        }
    }

    /**
     * Plays a 2D sound to a specific player.
     */
    public void playSoundToPlayer(@Nonnull PlayerRef playerRef, int soundIndex, @Nonnull SoundCategory category) {
        if (soundIndex == 0) {
            return;
        }
        SoundUtil.playSoundEvent2dToPlayer(playerRef, soundIndex, category);
    }

    // ==================== Configuration ====================

    /**
     * Sets custom sound event IDs. Call before sounds are played.
     */
    public void configureSounds(String pickupId, String dropId, String captureId,
                                 String returnId, String startId, String endId) {
        this.flagPickupSound = pickupId;
        this.flagDropSound = dropId;
        this.flagCaptureSound = captureId;
        this.flagReturnSound = returnId;
        this.matchStartSound = startId;
        this.matchEndSound = endId;
        this.soundsResolved = false; // Force re-resolve
    }
}

package com.example.ctf.arena;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.math.vector.Transform;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Configuration data for a CTF arena.
 * Stores spawn points, capture zones, and protected regions.
 * Persisted to JSON via plugin config system.
 */
public class ArenaConfig {

    public static final BuilderCodec<ArenaConfig> CODEC = BuilderCodec.builder(ArenaConfig.class, ArenaConfig::new)
        .<Transform[]>appendInherited(
            new KeyedCodec<>("RedSpawns", ArrayCodec.ofBuilderCodec(Transform.CODEC_DEGREES, Transform[]::new)),
            (o, v) -> o.redSpawns = v,
            o -> o.redSpawns,
            (o, p) -> o.redSpawns = p.redSpawns
        )
        .add()
        .<Transform[]>appendInherited(
            new KeyedCodec<>("BlueSpawns", ArrayCodec.ofBuilderCodec(Transform.CODEC_DEGREES, Transform[]::new)),
            (o, v) -> o.blueSpawns = v,
            o -> o.blueSpawns,
            (o, p) -> o.blueSpawns = p.blueSpawns
        )
        .add()
        .<CaptureZone>appendInherited(
            new KeyedCodec<>("RedCaptureZone", CaptureZone.CODEC),
            (o, v) -> o.redCaptureZone = v,
            o -> o.redCaptureZone,
            (o, p) -> o.redCaptureZone = p.redCaptureZone
        )
        .add()
        .<CaptureZone>appendInherited(
            new KeyedCodec<>("BlueCaptureZone", CaptureZone.CODEC),
            (o, v) -> o.blueCaptureZone = v,
            o -> o.blueCaptureZone,
            (o, p) -> o.blueCaptureZone = p.blueCaptureZone
        )
        .add()
        .<ProtectedRegion[]>appendInherited(
            new KeyedCodec<>("ProtectedRegions", ArrayCodec.ofBuilderCodec(ProtectedRegion.CODEC, ProtectedRegion[]::new)),
            (o, v) -> o.protectedRegions = v,
            o -> o.protectedRegions,
            (o, p) -> o.protectedRegions = p.protectedRegions
        )
        .add()
        .<Integer>appendInherited(
            new KeyedCodec<>("ScoreLimit", Codec.INTEGER),
            (o, v) -> o.scoreLimit = v,
            o -> o.scoreLimit,
            (o, p) -> o.scoreLimit = p.scoreLimit
        )
        .add()
        .build();

    private Transform[] redSpawns;
    private Transform[] blueSpawns;
    private CaptureZone redCaptureZone;
    private CaptureZone blueCaptureZone;
    private ProtectedRegion[] protectedRegions;
    private int scoreLimit;

    public ArenaConfig() {
        this.redSpawns = new Transform[0];
        this.blueSpawns = new Transform[0];
        this.redCaptureZone = null;
        this.blueCaptureZone = null;
        this.protectedRegions = new ProtectedRegion[0];
        this.scoreLimit = 3;
    }

    @Nonnull
    public Transform[] getRedSpawns() {
        return redSpawns;
    }

    public void setRedSpawns(@Nonnull Transform[] redSpawns) {
        this.redSpawns = redSpawns;
    }

    @Nonnull
    public Transform[] getBlueSpawns() {
        return blueSpawns;
    }

    public void setBlueSpawns(@Nonnull Transform[] blueSpawns) {
        this.blueSpawns = blueSpawns;
    }

    @Nullable
    public CaptureZone getRedCaptureZone() {
        return redCaptureZone;
    }

    public void setRedCaptureZone(@Nullable CaptureZone redCaptureZone) {
        this.redCaptureZone = redCaptureZone;
    }

    @Nullable
    public CaptureZone getBlueCaptureZone() {
        return blueCaptureZone;
    }

    public void setBlueCaptureZone(@Nullable CaptureZone blueCaptureZone) {
        this.blueCaptureZone = blueCaptureZone;
    }

    @Nonnull
    public ProtectedRegion[] getProtectedRegions() {
        return protectedRegions;
    }

    public void setProtectedRegions(@Nonnull ProtectedRegion[] protectedRegions) {
        this.protectedRegions = protectedRegions;
    }

    public int getScoreLimit() {
        return scoreLimit;
    }

    public void setScoreLimit(int scoreLimit) {
        this.scoreLimit = scoreLimit;
    }

    /**
     * Checks if a spawn point exists for the given team.
     */
    public boolean hasSpawns(@Nonnull com.example.ctf.FlagTeam team) {
        return switch (team) {
            case RED -> redSpawns.length > 0;
            case BLUE -> blueSpawns.length > 0;
        };
    }

    /**
     * Gets the spawns for the given team.
     */
    @Nonnull
    public Transform[] getSpawns(@Nonnull com.example.ctf.FlagTeam team) {
        return switch (team) {
            case RED -> redSpawns;
            case BLUE -> blueSpawns;
        };
    }

    /**
     * Gets the capture zone for the given team (where that team captures enemy flags).
     */
    @Nullable
    public CaptureZone getCaptureZone(@Nonnull com.example.ctf.FlagTeam team) {
        return switch (team) {
            case RED -> redCaptureZone;
            case BLUE -> blueCaptureZone;
        };
    }
}

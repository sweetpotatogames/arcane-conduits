package com.example.ctf.arena;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.math.vector.Vector3d;

import javax.annotation.Nonnull;

/**
 * Represents a spherical capture zone where flag carriers can score.
 * Each team has their own capture zone at their base.
 */
public class CaptureZone {

    public static final double DEFAULT_RADIUS = 3.0;

    public static final BuilderCodec<CaptureZone> CODEC = BuilderCodec.builder(CaptureZone.class, CaptureZone::new)
        .<Vector3d>appendInherited(
            new KeyedCodec<>("Center", Vector3d.CODEC),
            (o, v) -> o.center = v,
            o -> o.center,
            (o, p) -> o.center = p.center
        )
        .addValidator(Validators.nonNull())
        .add()
        .<Double>appendInherited(
            new KeyedCodec<>("Radius", Codec.DOUBLE),
            (o, v) -> o.radius = v,
            o -> o.radius,
            (o, p) -> o.radius = p.radius
        )
        .add()
        .build();

    private Vector3d center;
    private double radius;

    public CaptureZone() {
        this.center = new Vector3d(0, 0, 0);
        this.radius = DEFAULT_RADIUS;
    }

    public CaptureZone(@Nonnull Vector3d center, double radius) {
        this.center = center;
        this.radius = radius;
    }

    public CaptureZone(@Nonnull Vector3d center) {
        this(center, DEFAULT_RADIUS);
    }

    /**
     * Checks if a position is within this capture zone.
     *
     * @param position The position to check
     * @return true if the position is within the zone radius
     */
    public boolean contains(@Nonnull Vector3d position) {
        double dx = position.getX() - center.getX();
        double dy = position.getY() - center.getY();
        double dz = position.getZ() - center.getZ();
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        return distanceSquared <= radius * radius;
    }

    @Nonnull
    public Vector3d getCenter() {
        return center;
    }

    public void setCenter(@Nonnull Vector3d center) {
        this.center = center;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }
}

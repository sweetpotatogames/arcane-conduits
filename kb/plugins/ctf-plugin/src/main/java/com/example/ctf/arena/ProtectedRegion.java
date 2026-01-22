package com.example.ctf.arena;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;

import javax.annotation.Nonnull;

/**
 * Represents an axis-aligned bounding box (AABB) region where building is restricted.
 * Used to protect flag rooms and other important areas from player modification.
 */
public class ProtectedRegion {

    public static final BuilderCodec<ProtectedRegion> CODEC = BuilderCodec.builder(ProtectedRegion.class, ProtectedRegion::new)
        .<String>appendInherited(
            new KeyedCodec<>("Name", Codec.STRING),
            (o, v) -> o.name = v,
            o -> o.name,
            (o, p) -> o.name = p.name
        )
        .addValidator(Validators.nonNull())
        .add()
        .<Vector3d>appendInherited(
            new KeyedCodec<>("Min", Vector3d.CODEC),
            (o, v) -> o.min = v,
            o -> o.min,
            (o, p) -> o.min = p.min
        )
        .addValidator(Validators.nonNull())
        .add()
        .<Vector3d>appendInherited(
            new KeyedCodec<>("Max", Vector3d.CODEC),
            (o, v) -> o.max = v,
            o -> o.max,
            (o, p) -> o.max = p.max
        )
        .addValidator(Validators.nonNull())
        .add()
        .build();

    private String name;
    private Vector3d min;
    private Vector3d max;

    public ProtectedRegion() {
        this.name = "unnamed";
        this.min = new Vector3d(0, 0, 0);
        this.max = new Vector3d(0, 0, 0);
    }

    public ProtectedRegion(@Nonnull String name, @Nonnull Vector3d min, @Nonnull Vector3d max) {
        this.name = name;
        // Normalize min/max to ensure min values are actually smaller
        this.min = new Vector3d(
            Math.min(min.getX(), max.getX()),
            Math.min(min.getY(), max.getY()),
            Math.min(min.getZ(), max.getZ())
        );
        this.max = new Vector3d(
            Math.max(min.getX(), max.getX()),
            Math.max(min.getY(), max.getY()),
            Math.max(min.getZ(), max.getZ())
        );
    }

    /**
     * Checks if a block position is within this protected region.
     *
     * @param blockPos The block position to check
     * @return true if the block is within the protected region
     */
    public boolean containsBlock(@Nonnull Vector3i blockPos) {
        return blockPos.getX() >= min.getX() && blockPos.getX() <= max.getX()
            && blockPos.getY() >= min.getY() && blockPos.getY() <= max.getY()
            && blockPos.getZ() >= min.getZ() && blockPos.getZ() <= max.getZ();
    }

    /**
     * Checks if a position is within this protected region.
     *
     * @param position The position to check
     * @return true if the position is within the protected region
     */
    public boolean contains(@Nonnull Vector3d position) {
        return position.getX() >= min.getX() && position.getX() <= max.getX()
            && position.getY() >= min.getY() && position.getY() <= max.getY()
            && position.getZ() >= min.getZ() && position.getZ() <= max.getZ();
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public void setName(@Nonnull String name) {
        this.name = name;
    }

    @Nonnull
    public Vector3d getMin() {
        return min;
    }

    public void setMin(@Nonnull Vector3d min) {
        this.min = min;
    }

    @Nonnull
    public Vector3d getMax() {
        return max;
    }

    public void setMax(@Nonnull Vector3d max) {
        this.max = max;
    }
}

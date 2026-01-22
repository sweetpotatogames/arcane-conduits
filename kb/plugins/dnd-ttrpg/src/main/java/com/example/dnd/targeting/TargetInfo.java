package com.example.dnd.targeting;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Immutable snapshot of target entity information.
 * Used to display target info in HUD without holding entity references.
 *
 * TODO: Component accessor is required for fromEntityRef() but cannot
 * be obtained from World. ECS integration research needed.
 */
public class TargetInfo {
    private final Ref<EntityStore> entityRef;
    private final String name;
    private final String roleName;
    private final float currentHp;
    private final float maxHp;
    private final Vector3d position;
    private final boolean valid;

    private TargetInfo(
        Ref<EntityStore> entityRef,
        String name,
        String roleName,
        float currentHp,
        float maxHp,
        Vector3d position,
        boolean valid
    ) {
        this.entityRef = entityRef;
        this.name = name;
        this.roleName = roleName;
        this.currentHp = currentHp;
        this.maxHp = maxHp;
        this.position = position;
        this.valid = valid;
    }

    /**
     * Create a TargetInfo from an entity reference.
     * Returns null if the entity is not a valid target.
     *
     * TODO: Component accessor integration disabled - needs ECS API research.
     * The accessor parameter is required but currently not used because
     * we can't obtain a ComponentAccessor from World.
     *
     * When ECS integration is implemented, this method should:
     * - Get Entity component to validate the entity
     * - Get NPCEntity component for name/role info
     * - Get EntityStatMap for health stats
     * - Get TransformComponent for position
     */
    @Nullable
    @SuppressWarnings("unused")
    public static TargetInfo fromEntityRef(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull ComponentAccessor<EntityStore> accessor
    ) {
        if (!entityRef.isValid()) {
            return null;
        }

        // TODO: ECS component access disabled - needs API research
        // For now, return a minimal TargetInfo with placeholder values
        return new TargetInfo(
            entityRef,
            "Target",      // placeholder name
            "unknown",     // placeholder role
            100,           // placeholder current HP
            100,           // placeholder max HP
            new Vector3d(), // placeholder position
            true
        );
    }

    /**
     * Create an invalid/empty target info.
     */
    public static TargetInfo invalid() {
        return new TargetInfo(null, "", "", 0, 0, new Vector3d(), false);
    }

    /**
     * Format a role name for display (e.g., "goblin_warrior" -> "Goblin Warrior").
     */
    private static String formatRoleName(String roleName) {
        if (roleName == null || roleName.isEmpty()) {
            return "Unknown";
        }

        // Remove path prefix if present (e.g., "NPC/Enemies/Goblin" -> "Goblin")
        int lastSlash = roleName.lastIndexOf('/');
        if (lastSlash >= 0) {
            roleName = roleName.substring(lastSlash + 1);
        }

        // Replace underscores with spaces and capitalize words
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : roleName.toCharArray()) {
            if (c == '_' || c == '-') {
                result.append(' ');
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    // Getters

    @Nullable
    public Ref<EntityStore> getEntityRef() {
        return entityRef;
    }

    public String getName() {
        return name;
    }

    public String getRoleName() {
        return roleName;
    }

    public float getCurrentHp() {
        return currentHp;
    }

    public float getMaxHp() {
        return maxHp;
    }

    public Vector3d getPosition() {
        return position;
    }

    public boolean isValid() {
        return valid && entityRef != null && entityRef.isValid();
    }

    /**
     * Check if the target is alive (HP > 0).
     */
    public boolean isAlive() {
        return isValid() && currentHp > 0;
    }

    /**
     * Get HP as a percentage (0.0 to 1.0).
     */
    public float getHpPercent() {
        if (maxHp <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(1, currentHp / maxHp));
    }

    /**
     * Get HP as a percentage (0 to 100).
     */
    public int getHpPercentInt() {
        return (int) (getHpPercent() * 100);
    }

    @Override
    public String toString() {
        return String.format("TargetInfo{name='%s', hp=%.0f/%.0f, valid=%s}",
            name, currentHp, maxHp, valid);
    }
}

package com.example.ctf;

import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemWeapon;
import com.hypixel.hytale.server.core.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Handles weapon restrictions for flag carriers.
 *
 * Flag carriers cannot use two-handed weapons (indicated by RenderDualWielded=true in item config).
 * This allows them to use:
 * - The flag (one hand)
 * - One-handed weapons, shields, tools in off-hand
 */
public class WeaponRestrictionHandler {

    /**
     * Checks if an item is a two-handed weapon.
     *
     * @param itemStack The item to check
     * @return true if the item is a two-handed weapon
     */
    public static boolean isTwoHandedWeapon(@Nullable ItemStack itemStack) {
        if (itemStack == null || ItemStack.isEmpty(itemStack)) {
            return false;
        }

        Item item = itemStack.getItem();
        if (item == null) {
            return false;
        }

        ItemWeapon weapon = item.getWeapon();
        if (weapon == null) {
            return false;
        }

        // Check if the weapon requires both hands (dual wielded render = two-handed)
        return isTwoHandedFromConfig(weapon);
    }

    /**
     * Checks if an item ID represents a two-handed weapon.
     *
     * @param itemId The item ID to check
     * @return true if the item is a two-handed weapon
     */
    public static boolean isTwoHandedWeapon(@Nullable String itemId) {
        if (itemId == null || itemId.equals("Empty")) {
            return false;
        }

        Item item = Item.getAssetMap().getAsset(itemId);
        if (item == null) {
            return false;
        }

        ItemWeapon weapon = item.getWeapon();
        if (weapon == null) {
            return false;
        }

        return isTwoHandedFromConfig(weapon);
    }

    /**
     * Extracts the two-handed property from weapon config.
     * Uses reflection since the field is protected.
     */
    private static boolean isTwoHandedFromConfig(@Nonnull ItemWeapon weapon) {
        try {
            // The renderDualWielded field indicates two-handed weapons
            var field = weapon.getClass().getDeclaredField("renderDualWielded");
            field.setAccessible(true);
            return (boolean) field.get(weapon);
        } catch (Exception e) {
            // If we can't access the field, assume it's not two-handed
            return false;
        }
    }

    /**
     * Checks if a player can carry the flag based on their current held item.
     * They cannot carry if they have a two-handed weapon in the flag slot.
     *
     * @param currentItem The item currently in the flag slot (slot 0)
     * @return true if the player can carry the flag
     */
    public static boolean canCarryFlag(@Nullable ItemStack currentItem) {
        // If no item or empty, they can carry
        if (currentItem == null || ItemStack.isEmpty(currentItem)) {
            return true;
        }

        // If they have a two-handed weapon, they cannot carry the flag
        // The flag would replace this item anyway, but we could warn them
        return !isTwoHandedWeapon(currentItem);
    }

    /**
     * Common two-handed weapon types for reference.
     * These typically have RenderDualWielded=true in their item config.
     *
     * Examples:
     * - Bows (require both hands to draw)
     * - Great swords / two-handed swords
     * - Staffs
     * - Heavy weapons
     *
     * One-handed weapons that work with flag:
     * - Short swords
     * - Daggers
     * - Shields
     * - Tools (pickaxe, axe, etc.)
     * - Consumables
     */
    private WeaponRestrictionHandler() {
        // Utility class
    }
}

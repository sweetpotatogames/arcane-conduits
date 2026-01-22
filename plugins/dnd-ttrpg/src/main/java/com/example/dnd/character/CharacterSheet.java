package com.example.dnd.character;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * D&D 5e character sheet data model.
 */
public class CharacterSheet {
    // Ability scores
    private final Map<Ability, Integer> abilityScores = new EnumMap<>(Ability.class);

    // Combat stats
    private int maxHp = 10;
    private int currentHp = 10;
    private int tempHp = 0;
    private int armorClass = 10;
    private int proficiencyBonus = 2;
    private int speed = 30;

    // Skill proficiencies
    private final Set<Skill> skillProficiencies = EnumSet.noneOf(Skill.class);

    // Saving throw proficiencies
    private final Set<Ability> savingThrowProficiencies = EnumSet.noneOf(Ability.class);

    public CharacterSheet() {
        // Initialize all abilities to 10
        for (Ability ability : Ability.values()) {
            abilityScores.put(ability, 10);
        }
    }

    /**
     * Get an ability score.
     */
    public int getAbilityScore(Ability ability) {
        return abilityScores.getOrDefault(ability, 10);
    }

    /**
     * Set an ability score.
     */
    public void setAbilityScore(Ability ability, int score) {
        abilityScores.put(ability, Math.max(1, Math.min(30, score)));
    }

    /**
     * Calculate the modifier for an ability: (score - 10) / 2
     */
    public int getModifier(Ability ability) {
        return (getAbilityScore(ability) - 10) / 2;
    }

    /**
     * Get the bonus for a skill check.
     */
    public int getSkillBonus(Skill skill) {
        int mod = getModifier(skill.getAbility());
        if (skillProficiencies.contains(skill)) {
            return mod + proficiencyBonus;
        }
        return mod;
    }

    /**
     * Get the bonus for a saving throw.
     */
    public int getSavingThrowBonus(Ability ability) {
        int mod = getModifier(ability);
        if (savingThrowProficiencies.contains(ability)) {
            return mod + proficiencyBonus;
        }
        return mod;
    }

    /**
     * Format a modifier as a string (+X or -X).
     */
    public static String formatModifier(int mod) {
        return mod >= 0 ? "+" + mod : String.valueOf(mod);
    }

    // HP management
    public int getMaxHp() { return maxHp; }
    public void setMaxHp(int maxHp) { this.maxHp = Math.max(1, maxHp); }

    public int getCurrentHp() { return currentHp; }
    public void setCurrentHp(int hp) { this.currentHp = Math.max(0, Math.min(maxHp + tempHp, hp)); }

    public int getTempHp() { return tempHp; }
    public void setTempHp(int tempHp) { this.tempHp = Math.max(0, tempHp); }

    /**
     * Take damage. Returns true if unconscious (0 HP).
     */
    public boolean takeDamage(int damage) {
        if (tempHp > 0) {
            if (damage <= tempHp) {
                tempHp -= damage;
                return false;
            }
            damage -= tempHp;
            tempHp = 0;
        }
        currentHp = Math.max(0, currentHp - damage);
        return currentHp == 0;
    }

    /**
     * Heal damage.
     */
    public void heal(int amount) {
        currentHp = Math.min(maxHp, currentHp + amount);
    }

    // Other stats
    public int getArmorClass() { return armorClass; }
    public void setArmorClass(int ac) { this.armorClass = ac; }

    public int getProficiencyBonus() { return proficiencyBonus; }
    public void setProficiencyBonus(int bonus) { this.proficiencyBonus = bonus; }

    public int getSpeed() { return speed; }
    public void setSpeed(int speed) { this.speed = speed; }

    /**
     * Get movement speed in Hytale blocks.
     * Converts D&D feet to blocks (1 block = 5 feet).
     */
    public int getSpeedInBlocks() {
        return speed / 5;
    }

    /**
     * Set movement speed in Hytale blocks.
     * Converts blocks to D&D feet (1 block = 5 feet).
     */
    public void setSpeedInBlocks(int blocks) {
        this.speed = blocks * 5;
    }

    // Proficiency management
    public boolean hasSkillProficiency(Skill skill) {
        return skillProficiencies.contains(skill);
    }

    public void addSkillProficiency(Skill skill) {
        skillProficiencies.add(skill);
    }

    public void removeSkillProficiency(Skill skill) {
        skillProficiencies.remove(skill);
    }

    public Set<Skill> getSkillProficiencies() {
        return EnumSet.copyOf(skillProficiencies);
    }

    public boolean hasSavingThrowProficiency(Ability ability) {
        return savingThrowProficiencies.contains(ability);
    }

    public void addSavingThrowProficiency(Ability ability) {
        savingThrowProficiencies.add(ability);
    }
}

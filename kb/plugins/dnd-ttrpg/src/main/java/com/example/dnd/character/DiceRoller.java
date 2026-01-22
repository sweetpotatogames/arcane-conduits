package com.example.dnd.character;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Utility class for rolling dice.
 */
public class DiceRoller {
    private static final Random RANDOM = new Random();

    /**
     * Result of a dice roll.
     */
    public record DiceResult(int total, int[] rolls, int modifier, String expression) {
        /**
         * Format the result for display.
         */
        public String format() {
            String rollsStr = Arrays.stream(rolls)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(", "));
            if (modifier == 0) {
                return String.format("%s = [%s] = %d", expression, rollsStr, total);
            }
            return String.format("%s = [%s] %+d = %d", expression, rollsStr, modifier, total);
        }
    }

    /**
     * Roll dice with the given parameters.
     */
    public static DiceResult roll(int numDice, int dieType, int modifier) {
        int[] rolls = new int[numDice];
        int total = modifier;
        for (int i = 0; i < numDice; i++) {
            rolls[i] = RANDOM.nextInt(dieType) + 1;
            total += rolls[i];
        }
        String expr = numDice + "d" + dieType;
        return new DiceResult(total, rolls, modifier, expr);
    }

    /**
     * Roll a d20 with modifier.
     */
    public static DiceResult rollD20(int modifier) {
        return roll(1, 20, modifier);
    }

    /**
     * Roll a d20 with advantage.
     */
    public static DiceResult rollD20Advantage(int modifier) {
        int roll1 = RANDOM.nextInt(20) + 1;
        int roll2 = RANDOM.nextInt(20) + 1;
        int best = Math.max(roll1, roll2);
        return new DiceResult(best + modifier, new int[]{roll1, roll2}, modifier, "d20 (adv)");
    }

    /**
     * Roll a d20 with disadvantage.
     */
    public static DiceResult rollD20Disadvantage(int modifier) {
        int roll1 = RANDOM.nextInt(20) + 1;
        int roll2 = RANDOM.nextInt(20) + 1;
        int worst = Math.min(roll1, roll2);
        return new DiceResult(worst + modifier, new int[]{roll1, roll2}, modifier, "d20 (dis)");
    }
}

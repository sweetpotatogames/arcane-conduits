package com.example.dnd.commands;

import com.example.dnd.character.DiceRoller;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command to roll dice.
 * Usage: /dnd roll <dice> [modifier]
 * Examples: /dnd roll d20, /dnd roll 2d6, /dnd roll d20 5
 */
public class RollCommand extends AbstractPlayerCommand {
    // Pattern to match dice notation: XdY or dY
    private static final Pattern DICE_PATTERN = Pattern.compile("^(\\d*)d(\\d+)$", Pattern.CASE_INSENSITIVE);

    private final RequiredArg<String> diceArg;
    private final DefaultArg<Integer> modifierArg;

    public RollCommand() {
        super("roll", "server.commands.dnd.roll.desc");
        diceArg = withRequiredArg("dice", "Dice to roll (e.g., d20, 2d6, 4d8)", ArgTypes.STRING);
        modifierArg = withDefaultArg("modifier", "Modifier to add to the roll", ArgTypes.INTEGER, 0, "0");
    }

    @Override
    protected void execute(
        @Nonnull CommandContext context,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PlayerRef playerRef,
        @Nonnull World world
    ) {
        String diceStr = context.get(diceArg);
        int modifier = context.get(modifierArg);

        Matcher matcher = DICE_PATTERN.matcher(diceStr);
        if (!matcher.matches()) {
            playerRef.sendMessage(Message.raw("[D&D] Invalid dice format. Use: d20, 2d6, 4d8, etc."));
            return;
        }

        String numStr = matcher.group(1);
        int numDice = numStr.isEmpty() ? 1 : Integer.parseInt(numStr);
        int dieType = Integer.parseInt(matcher.group(2));

        // Validate
        if (numDice < 1 || numDice > 100) {
            playerRef.sendMessage(Message.raw("[D&D] Number of dice must be between 1 and 100."));
            return;
        }
        if (dieType < 1 || dieType > 100) {
            playerRef.sendMessage(Message.raw("[D&D] Die type must be between 1 and 100."));
            return;
        }

        DiceRoller.DiceResult result = DiceRoller.roll(numDice, dieType, modifier);

        // Broadcast to all players
        String rollMessage = String.format("[D&D] %s rolled %s", playerRef.getUsername(), result.format());
        broadcastMessage(world, rollMessage);
    }

    @SuppressWarnings("deprecation")
    private void broadcastMessage(World world, String message) {
        for (Player player : world.getPlayers()) {
            player.getPlayerRef().sendMessage(Message.raw(message));
        }
    }
}

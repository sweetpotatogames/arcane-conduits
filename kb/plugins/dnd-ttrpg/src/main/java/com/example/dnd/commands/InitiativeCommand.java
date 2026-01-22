package com.example.dnd.commands;

import com.example.dnd.character.DiceRoller;
import com.example.dnd.combat.CombatState;
import com.example.dnd.combat.TurnManager;
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

/**
 * Command to manage initiative.
 * Usage: /dnd initiative <roll|list|clear> [modifier]
 */
public class InitiativeCommand extends AbstractPlayerCommand {
    private final TurnManager turnManager;
    private final RequiredArg<String> actionArg;
    private final DefaultArg<Integer> modifierArg;

    public InitiativeCommand(TurnManager turnManager) {
        super("initiative", "server.commands.dnd.initiative.desc");
        this.turnManager = turnManager;

        actionArg = withRequiredArg("action", "Action: roll, list, or clear", ArgTypes.STRING);
        modifierArg = withDefaultArg("modifier", "Initiative modifier (for roll)", ArgTypes.INTEGER, 0, "0");
    }

    @Override
    protected void execute(
        @Nonnull CommandContext context,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PlayerRef playerRef,
        @Nonnull World world
    ) {
        String action = context.get(actionArg);
        CombatState combatState = turnManager.getCombatState(world);

        switch (action.toLowerCase()) {
            case "roll" -> handleRoll(context, playerRef, world, combatState);
            case "list" -> handleList(playerRef, combatState);
            case "clear" -> handleClear(playerRef, combatState);
            default -> playerRef.sendMessage(Message.raw("[D&D] Unknown action: " + action + ". Use: roll, list, or clear"));
        }
    }

    private void handleRoll(CommandContext context, PlayerRef playerRef, World world, CombatState combatState) {
        if (combatState.isCombatActive()) {
            playerRef.sendMessage(Message.raw("[D&D] Cannot roll initiative while combat is active!"));
            return;
        }

        int modifier = context.get(modifierArg);
        DiceRoller.DiceResult result = DiceRoller.rollD20(modifier);

        combatState.addToInitiative(playerRef.getUuid(), playerRef.getUsername(), result.total());

        // Broadcast the roll to all players
        String rollMessage = String.format("[D&D] %s rolled initiative: %s",
            playerRef.getUsername(), result.format());
        broadcastMessage(world, rollMessage);
    }

    private void handleList(PlayerRef playerRef, CombatState combatState) {
        playerRef.sendMessage(Message.raw("[D&D] " + combatState.getInitiativeListString()));
    }

    private void handleClear(PlayerRef playerRef, CombatState combatState) {
        if (combatState.isCombatActive()) {
            playerRef.sendMessage(Message.raw("[D&D] Cannot clear initiative while combat is active! End combat first."));
            return;
        }
        combatState.clear();
        playerRef.sendMessage(Message.raw("[D&D] Initiative cleared."));
    }

    @SuppressWarnings("deprecation")
    private void broadcastMessage(World world, String message) {
        for (Player player : world.getPlayers()) {
            player.getPlayerRef().sendMessage(Message.raw(message));
        }
    }
}

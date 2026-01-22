package com.example.dnd.commands;

import com.example.dnd.combat.CombatState;
import com.example.dnd.combat.TurnManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Command to control turn-based combat.
 * Usage: /dnd turn <start|next|end|status>
 */
public class TurnCommand extends AbstractPlayerCommand {
    private final TurnManager turnManager;
    private final RequiredArg<String> actionArg;

    public TurnCommand(TurnManager turnManager) {
        super("turn", "server.commands.dnd.turn.desc");
        this.turnManager = turnManager;

        actionArg = withRequiredArg("action", "Action: start, next, end, or status", ArgTypes.STRING);
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
            case "start" -> handleStart(playerRef, world, combatState);
            case "next" -> handleNext(playerRef, world, combatState);
            case "end" -> handleEnd(world, combatState);
            case "status" -> handleStatus(playerRef, combatState);
            default -> playerRef.sendMessage(Message.raw("[D&D] Unknown action: " + action + ". Use: start, next, end, or status"));
        }
    }

    private void handleStart(PlayerRef playerRef, World world, CombatState combatState) {
        if (combatState.isCombatActive()) {
            playerRef.sendMessage(Message.raw("[D&D] Combat is already active!"));
            return;
        }

        if (combatState.getInitiativeOrder().isEmpty()) {
            playerRef.sendMessage(Message.raw("[D&D] No combatants! Use /dnd initiative roll first."));
            return;
        }

        combatState.startCombat();
        String message = String.format("[D&D] Combat started! First turn: %s",
            combatState.getCurrentPlayerName());
        broadcastMessage(world, message);

        // Show combat HUDs for all combatants
        turnManager.showCombatHuds(world);
    }

    private void handleNext(PlayerRef playerRef, World world, CombatState combatState) {
        if (!combatState.isCombatActive()) {
            playerRef.sendMessage(Message.raw("[D&D] No active combat! Use /dnd turn start first."));
            return;
        }

        // Only the current player or an operator can advance the turn
        // Note: checking operator status would require additional API - simplified for prototype
        if (!combatState.isPlayerTurn(playerRef.getUuid())) {
            playerRef.sendMessage(Message.raw("[D&D] Only the current player can advance turns."));
            return;
        }

        combatState.nextTurn();
        String message = String.format("[D&D] Next turn: %s", combatState.getCurrentPlayerName());
        broadcastMessage(world, message);

        // Refresh all combat HUDs to show new turn
        turnManager.refreshAllHuds(world);
    }

    private void handleEnd(World world, CombatState combatState) {
        if (!combatState.isCombatActive()) {
            return;
        }

        // Hide all combat HUDs before ending combat
        turnManager.hideCombatHuds(world);

        combatState.endCombat();
        broadcastMessage(world, "[D&D] Combat ended!");
    }

    private void handleStatus(PlayerRef playerRef, CombatState combatState) {
        if (!combatState.isCombatActive()) {
            playerRef.sendMessage(Message.raw("[D&D] No active combat."));
            return;
        }

        String status = String.format("[D&D] Current turn: %s | Phase: %s",
            combatState.getCurrentPlayerName(),
            combatState.getCurrentPhase().getDescription());
        playerRef.sendMessage(Message.raw(status));
    }

    @SuppressWarnings("deprecation")
    private void broadcastMessage(World world, String message) {
        for (Player player : world.getPlayers()) {
            player.getPlayerRef().sendMessage(Message.raw(message));
        }
    }
}

package com.example.dnd.commands;

import com.example.dnd.DndPlugin;
import com.example.dnd.character.CharacterSheet;
import com.example.dnd.combat.CombatState;
import com.example.dnd.combat.TurnManager;
import com.example.dnd.combat.TurnPhase;
import com.example.dnd.movement.GridMovementManager;
import com.example.dnd.movement.MovementConfig;
import com.example.dnd.movement.MovementState;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Command for controlling grid-based movement during combat.
 *
 * Usage:
 *   /dnd move cancel        - Cancel planned movement
 *   /dnd move confirm       - Confirm and execute movement
 *   /dnd move speed [blocks] - Get/set movement speed (in blocks)
 *   /dnd move status        - Show current movement status
 *   /dnd move skip          - Skip movement phase
 *   /dnd move diagonal [1|1.5] - Set diagonal movement cost
 */
public class MoveCommand extends AbstractPlayerCommand {
    private final TurnManager turnManager;
    private final RequiredArg<String> actionArg;
    private final OptionalArg<Integer> valueArg;

    public MoveCommand(TurnManager turnManager) {
        super("move", "server.commands.dnd.move.desc");
        this.turnManager = turnManager;

        actionArg = withRequiredArg("action", "Action: cancel, confirm, speed, status, skip, diagonal",
            ArgTypes.STRING);
        valueArg = withOptionalArg("value", "Optional value for speed or diagonal cost",
            ArgTypes.INTEGER);
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
        Integer value = context.get(valueArg);
        // Get Player component from the entity ref
        Player player = store.getComponent(ref, Player.getComponentType());
        UUID playerId = playerRef.getUuid();

        switch (action.toLowerCase()) {
            case "cancel" -> handleCancel(player, world);
            case "confirm" -> handleConfirm(player, world);
            case "speed" -> handleSpeed(playerRef, playerId, value);
            case "status" -> handleStatus(playerRef, playerId, world);
            case "skip" -> handleSkip(player, playerRef, world);
            case "diagonal" -> handleDiagonal(playerRef, value);
            default -> playerRef.sendMessage(Message.raw(
                "[D&D] Unknown action: " + action + ". Use: cancel, confirm, speed, status, skip, diagonal"
            ));
        }
    }

    private void handleCancel(Player player, World world) {
        GridMovementManager.get().cancelPlannedMovement(player, world);
    }

    private void handleConfirm(Player player, World world) {
        GridMovementManager.get().confirmMovement(player, world);
    }

    private void handleSpeed(PlayerRef playerRef, UUID playerId, Integer value) {
        CharacterSheet sheet = DndPlugin.get().getOrCreateCharacterSheet(playerId);

        if (value == null) {
            // Get current speed
            int blocks = sheet.getSpeedInBlocks();
            int feet = sheet.getSpeed();
            playerRef.sendMessage(Message.raw(
                String.format("[D&D] Your movement speed: %d blocks (%d ft)", blocks, feet)
            ));
        } else {
            // Set speed
            sheet.setSpeedInBlocks(value);
            playerRef.sendMessage(Message.raw(
                String.format("[D&D] Movement speed set to %d blocks (%d ft)", value, value * 5)
            ));
        }
    }

    private void handleStatus(PlayerRef playerRef, UUID playerId, World world) {
        CombatState combatState = turnManager.getCombatState(world);

        if (!combatState.isCombatActive()) {
            playerRef.sendMessage(Message.raw("[D&D] No active combat."));
            return;
        }

        MovementState moveState = GridMovementManager.get().getState(playerId);
        if (moveState == null) {
            playerRef.sendMessage(Message.raw("[D&D] No movement state (not your turn?)."));
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[D&D] Movement Status:\n");
        sb.append(String.format("  Total: %d blocks\n", moveState.getTotalMovement()));
        sb.append(String.format("  Used: %d blocks\n", moveState.getUsedMovement()));
        sb.append(String.format("  Remaining: %d blocks\n", moveState.getRemainingMovement()));

        if (moveState.getPlannedDestination() != null) {
            sb.append(String.format("  Planned: %d blocks to %s\n",
                moveState.getPlannedDistance(),
                moveState.getPlannedDestination()));
            sb.append(String.format("  Reachable: %s\n",
                moveState.canReachDestination() ? "Yes" : "No - too far!"));
        } else {
            sb.append("  No destination selected\n");
        }

        sb.append(String.format("  Planning mode: %s", moveState.isPlanning() ? "Active" : "Inactive"));

        playerRef.sendMessage(Message.raw(sb.toString()));
    }

    private void handleSkip(Player player, PlayerRef playerRef, World world) {
        CombatState combatState = turnManager.getCombatState(world);

        if (!combatState.isCombatActive()) {
            playerRef.sendMessage(Message.raw("[D&D] No active combat."));
            return;
        }

        if (!combatState.isPlayerTurn(playerRef.getUuid())) {
            playerRef.sendMessage(Message.raw("[D&D] Not your turn!"));
            return;
        }

        if (combatState.getCurrentPhase() != TurnPhase.MOVEMENT) {
            playerRef.sendMessage(Message.raw("[D&D] Not in movement phase."));
            return;
        }

        // End movement phase
        GridMovementManager.get().endMovementPhase(player);

        // Advance to action phase
        combatState.setCurrentPhase(TurnPhase.ACTION);
        playerRef.sendMessage(Message.raw("[D&D] Movement skipped. Now in ACTION phase."));

        // Refresh HUDs
        turnManager.refreshAllHuds(world);
    }

    private void handleDiagonal(PlayerRef playerRef, Integer value) {
        MovementConfig config = GridMovementManager.get().getConfig();

        if (value == null) {
            // Get current diagonal cost
            double cost = config.getDiagonalCost();
            String mode = cost == 1.0 ? "Simple (1 block)" : "5e Variant (1.5 blocks avg)";
            playerRef.sendMessage(Message.raw(
                String.format("[D&D] Diagonal movement: %s (cost: %.1f)", mode, cost)
            ));
        } else {
            // Set diagonal cost
            if (value == 1) {
                config.setDiagonalCost(1.0);
                playerRef.sendMessage(Message.raw(
                    "[D&D] Diagonal movement set to simple (1 block per diagonal)"
                ));
            } else if (value == 2) {
                config.setAlternatingDiagonals(true);
                playerRef.sendMessage(Message.raw(
                    "[D&D] Diagonal movement set to 5e variant (1.5 blocks per diagonal)"
                ));
            } else {
                playerRef.sendMessage(Message.raw(
                    "[D&D] Invalid value. Use 1 for simple or 2 for 5e variant."
                ));
            }
        }
    }
}

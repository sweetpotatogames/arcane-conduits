package com.example.dnd.commands;

import com.example.dnd.targeting.TargetInfo;
import com.example.dnd.targeting.TargetManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Command for managing target selection during combat.
 *
 * Usage:
 *   /dnd target           - Show current target info
 *   /dnd target clear     - Clear current target
 *   /dnd target info      - Show detailed target info
 */
public class TargetCommand extends AbstractPlayerCommand {
    private final OptionalArg<String> actionArg;

    public TargetCommand() {
        super("target", "server.commands.dnd.target.desc");

        actionArg = withOptionalArg("action", "Action: clear, info (default: show current target)",
            ArgTypes.STRING);
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
        UUID playerId = playerRef.getUuid();
        TargetManager targetManager = TargetManager.get();

        if (action == null) {
            // Default: show current target
            handleShowTarget(playerRef, playerId, world, targetManager);
        } else {
            switch (action.toLowerCase()) {
                case "clear" -> handleClear(playerRef, playerId, world, targetManager);
                case "info" -> handleInfo(playerRef, playerId, world, targetManager);
                default -> playerRef.sendMessage(Message.raw(
                    "[D&D] Unknown action: " + action + ". Use: clear, info"
                ));
            }
        }
    }

    private void handleShowTarget(PlayerRef playerRef, UUID playerId, World world, TargetManager targetManager) {
        TargetInfo info = targetManager.getTargetInfo(playerId, world);

        if (info == null || !info.isValid()) {
            playerRef.sendMessage(Message.raw("[D&D] No target selected. Click an NPC to select a target."));
            return;
        }

        playerRef.sendMessage(Message.raw(
            String.format("[D&D] Current target: %s (HP: %.0f/%.0f)",
                info.getName(), info.getCurrentHp(), info.getMaxHp())
        ));
    }

    private void handleClear(PlayerRef playerRef, UUID playerId, World world, TargetManager targetManager) {
        if (!targetManager.hasTarget(playerId)) {
            playerRef.sendMessage(Message.raw("[D&D] No target to clear."));
            return;
        }

        targetManager.clearTarget(playerId, world);
        playerRef.sendMessage(Message.raw("[D&D] Target cleared."));
    }

    private void handleInfo(PlayerRef playerRef, UUID playerId, World world, TargetManager targetManager) {
        TargetInfo info = targetManager.getTargetInfo(playerId, world);

        if (info == null || !info.isValid()) {
            playerRef.sendMessage(Message.raw("[D&D] No target selected."));
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[D&D] Target Information:\n");
        sb.append(String.format("  Name: %s\n", info.getName()));
        sb.append(String.format("  Role: %s\n", info.getRoleName()));
        sb.append(String.format("  Health: %.0f/%.0f (%.0f%%)\n",
            info.getCurrentHp(), info.getMaxHp(), info.getHpPercent() * 100));
        sb.append(String.format("  Position: (%.1f, %.1f, %.1f)\n",
            info.getPosition().getX(),
            info.getPosition().getY(),
            info.getPosition().getZ()));
        sb.append(String.format("  Status: %s", info.isAlive() ? "Alive" : "Dead"));

        playerRef.sendMessage(Message.raw(sb.toString()));
    }
}

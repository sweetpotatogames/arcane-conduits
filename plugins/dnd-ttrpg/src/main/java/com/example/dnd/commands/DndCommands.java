package com.example.dnd.commands;

import com.example.dnd.DndPlugin;
import com.example.dnd.combat.TurnManager;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

/**
 * Parent command collection for all D&D commands.
 * All commands are accessed via /dnd <subcommand>
 */
public class DndCommands extends AbstractCommandCollection {

    public DndCommands(DndPlugin plugin, TurnManager turnManager) {
        super("dnd", "D&D TTRPG commands");

        // Add all subcommands
        addSubCommand(new CameraCommand());
        addSubCommand(new InitiativeCommand(turnManager));
        addSubCommand(new TurnCommand(turnManager));
        addSubCommand(new RollCommand());
        addSubCommand(new SheetCommand(plugin));
        addSubCommand(new CombatCommand(plugin, turnManager));
        addSubCommand(new MoveCommand(turnManager));
        addSubCommand(new TargetCommand());
    }
}

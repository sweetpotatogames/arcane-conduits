package com.example.dnd.commands;

import com.example.dnd.DndPlugin;
import com.example.dnd.combat.TurnManager;
import com.example.dnd.ui.CombatControlPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Command to open the combat control panel.
 * Usage: /dnd combat
 */
public class CombatCommand extends AbstractPlayerCommand {
    private final DndPlugin plugin;
    private final TurnManager turnManager;

    public CombatCommand(DndPlugin plugin, TurnManager turnManager) {
        super("combat", "server.commands.dnd.combat.desc");
        this.plugin = plugin;
        this.turnManager = turnManager;
    }

    @Override
    protected void execute(
        @Nonnull CommandContext context,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PlayerRef playerRef,
        @Nonnull World world
    ) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            CombatControlPage page = new CombatControlPage(playerRef, turnManager, plugin, world);
            player.getPageManager().openCustomPage(ref, store, page);
        }
    }
}

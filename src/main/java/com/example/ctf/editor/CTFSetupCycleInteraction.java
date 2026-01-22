package com.example.ctf.editor;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Secondary interaction for the CTF Setup Tool.
 * Cycles through the available setup modes on right-click.
 */
public class CTFSetupCycleInteraction extends SimpleInstantInteraction {

    @Nonnull
    public static final BuilderCodec<CTFSetupCycleInteraction> CODEC = BuilderCodec.builder(
            CTFSetupCycleInteraction.class, CTFSetupCycleInteraction::new, SimpleInstantInteraction.CODEC
        )
        .documentation("CTF Setup Tool cycle interaction - cycles through setup modes.")
        .build();

    public CTFSetupCycleInteraction() {
    }

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        if (type != InteractionType.Secondary) {
            return;
        }

        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        assert commandBuffer != null;

        Ref<EntityStore> ref = context.getEntity();
        Player playerComponent = commandBuffer.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) {
            return;
        }

        // Get PlayerRef for messaging
        @SuppressWarnings("deprecation")
        PlayerRef playerRef = playerComponent.getPlayerRef();
        if (playerRef == null) {
            return;
        }

        // Check if player has admin permissions
        if (!playerComponent.hasPermission("ctf.admin")) {
            playerRef.sendMessage(Message.raw("§cYou need admin permissions to use the CTF Setup Tool."));
            return;
        }

        UUIDComponent uuidComponent = commandBuffer.getComponent(ref, UUIDComponent.getComponentType());
        assert uuidComponent != null;
        UUID playerUuid = uuidComponent.getUuid();

        // Cycle to next mode
        CTFSetupInteraction.SetupMode nextMode = CTFSetupInteraction.cyclePlayerMode(playerUuid);

        // Get color based on mode
        String color = getModeColor(nextMode);

        // Send mode change message
        playerRef.sendMessage(Message.raw(color + "CTF Setup Mode: " + nextMode.getDisplayName()));
        playerRef.sendMessage(Message.raw("§7" + nextMode.getDescription()));

        // Show additional hints for certain modes
        switch (nextMode) {
            case PROTECT_1 -> playerRef.sendMessage(Message.raw("§7Tip: Use /ctf protect name <name> to set a region name first."));
            case RED_CAPTURE, BLUE_CAPTURE -> playerRef.sendMessage(Message.raw("§7Tip: Use /ctf setcapture <team> <radius> for custom radius."));
        }
    }

    private String getModeColor(CTFSetupInteraction.SetupMode mode) {
        return switch (mode) {
            case RED_SPAWN, RED_CAPTURE, RED_FLAG -> "§c"; // Red
            case BLUE_SPAWN, BLUE_CAPTURE, BLUE_FLAG -> "§9"; // Blue
            case PROTECT_1, PROTECT_2 -> "§e"; // Yellow
        };
    }

    @Nonnull
    @Override
    public String toString() {
        return "CTFSetupCycleInteraction{} " + super.toString();
    }
}

package com.arcane.conduits.commands;

import com.arcane.conduits.ArcaneConduitsPlugin;
import com.arcane.conduits.blocks.state.ConduitBlockState;
import com.arcane.conduits.core.power.ConduitNetworkManager;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;

import javax.annotation.Nonnull;

/**
 * Debug command for testing and inspecting conduit networks.
 *
 * Usage:
 *   /conduit power <x> <y> <z>  - Get power level at position
 *   /conduit network <x> <y> <z> - Get network info at position
 *   /conduit recalc <x> <y> <z> - Force recalculate network
 *   /conduit set <x> <y> <z> <power> - Set power level (testing)
 */
public class ConduitDebugCommand extends CommandBase {

    public ConduitDebugCommand() {
        super("conduit", "Debug commands for Arcane Conduits plugin.");
        this.setPermissionGroup(GameMode.Creative);  // Requires creative/OP

        // Add subcommands
        addSubCommand(new PowerSubCommand());
        addSubCommand(new NetworkSubCommand());
        addSubCommand(new RecalcSubCommand());
        addSubCommand(new SetPowerSubCommand());
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        ctx.sendMessage(Message.raw("=== Arcane Conduits Debug Commands ==="));
        ctx.sendMessage(Message.raw("  /conduit power <x> <y> <z> - Get power level"));
        ctx.sendMessage(Message.raw("  /conduit network <x> <y> <z> - Get network info"));
        ctx.sendMessage(Message.raw("  /conduit recalc <x> <y> <z> - Force recalculate"));
        ctx.sendMessage(Message.raw("  /conduit set <x> <y> <z> <power> - Set power (0-15)"));
    }

    // ==================== Power Subcommand ====================

    private class PowerSubCommand extends CommandBase {
        private final RequiredArg<Integer> xArg;
        private final RequiredArg<Integer> yArg;
        private final RequiredArg<Integer> zArg;

        public PowerSubCommand() {
            super("power", "Get power level at position");
            setPermissionGroup(GameMode.Creative);
            xArg = withRequiredArg("x", "X coordinate", ArgTypes.INTEGER);
            yArg = withRequiredArg("y", "Y coordinate", ArgTypes.INTEGER);
            zArg = withRequiredArg("z", "Z coordinate", ArgTypes.INTEGER);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext ctx) {
            if (!ctx.isPlayer()) {
                ctx.sendMessage(Message.raw("Command must be run by a player"));
                return;
            }

            Vector3i pos = new Vector3i(ctx.get(xArg), ctx.get(yArg), ctx.get(zArg));

            Player player = ctx.senderAs(Player.class);
            World world = player.getWorld();
            if (world == null) {
                ctx.sendMessage(Message.raw("Player not in a world"));
                return;
            }

            ConduitBlockState state = getConduitState(world, pos);
            if (state == null) {
                ctx.sendMessage(Message.raw("No conduit at " + formatPos(pos)));
                return;
            }

            ctx.sendMessage(Message.raw(String.format(
                "Conduit at %s: power=%d/%d (%s), connections=%d, decay=%d",
                formatPos(pos),
                state.getPowerLevel(),
                state.getMaxPower(),
                state.getPowerCategory(),
                Integer.bitCount(state.getConnectionMask()),
                state.getDecayRate()
            )));
        }
    }

    // ==================== Network Subcommand ====================

    private class NetworkSubCommand extends CommandBase {
        private final RequiredArg<Integer> xArg;
        private final RequiredArg<Integer> yArg;
        private final RequiredArg<Integer> zArg;

        public NetworkSubCommand() {
            super("network", "Get network info at position");
            setPermissionGroup(GameMode.Creative);
            xArg = withRequiredArg("x", "X coordinate", ArgTypes.INTEGER);
            yArg = withRequiredArg("y", "Y coordinate", ArgTypes.INTEGER);
            zArg = withRequiredArg("z", "Z coordinate", ArgTypes.INTEGER);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext ctx) {
            if (!ctx.isPlayer()) {
                ctx.sendMessage(Message.raw("Command must be run by a player"));
                return;
            }

            Vector3i pos = new Vector3i(ctx.get(xArg), ctx.get(yArg), ctx.get(zArg));

            Player player = ctx.senderAs(Player.class);
            World world = player.getWorld();
            if (world == null) {
                ctx.sendMessage(Message.raw("Player not in a world"));
                return;
            }

            ArcaneConduitsPlugin plugin = ArcaneConduitsPlugin.getInstance();
            if (plugin == null || plugin.getNetworkManager() == null) {
                ctx.sendMessage(Message.raw("Plugin not initialized"));
                return;
            }

            ConduitNetworkManager.NetworkDebugInfo info =
                plugin.getNetworkManager().getNetworkDebugInfo(world, pos);

            ctx.sendMessage(Message.raw(String.format(
                "Network at %s: %s",
                formatPos(pos),
                info.toString()
            )));
        }
    }

    // ==================== Recalc Subcommand ====================

    private class RecalcSubCommand extends CommandBase {
        private final RequiredArg<Integer> xArg;
        private final RequiredArg<Integer> yArg;
        private final RequiredArg<Integer> zArg;

        public RecalcSubCommand() {
            super("recalc", "Force recalculate network");
            setPermissionGroup(GameMode.Creative);
            xArg = withRequiredArg("x", "X coordinate", ArgTypes.INTEGER);
            yArg = withRequiredArg("y", "Y coordinate", ArgTypes.INTEGER);
            zArg = withRequiredArg("z", "Z coordinate", ArgTypes.INTEGER);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext ctx) {
            if (!ctx.isPlayer()) {
                ctx.sendMessage(Message.raw("Command must be run by a player"));
                return;
            }

            Vector3i pos = new Vector3i(ctx.get(xArg), ctx.get(yArg), ctx.get(zArg));

            Player player = ctx.senderAs(Player.class);
            World world = player.getWorld();
            if (world == null) {
                ctx.sendMessage(Message.raw("Player not in a world"));
                return;
            }

            ArcaneConduitsPlugin plugin = ArcaneConduitsPlugin.getInstance();
            if (plugin == null || plugin.getNetworkManager() == null) {
                ctx.sendMessage(Message.raw("Plugin not initialized"));
                return;
            }

            plugin.getNetworkManager().recalculateNetworkNow(world, pos);
            ctx.sendMessage(Message.raw("Network recalculated at " + formatPos(pos)));
        }
    }

    // ==================== Set Power Subcommand ====================

    private class SetPowerSubCommand extends CommandBase {
        private final RequiredArg<Integer> xArg;
        private final RequiredArg<Integer> yArg;
        private final RequiredArg<Integer> zArg;
        private final RequiredArg<Integer> powerArg;

        public SetPowerSubCommand() {
            super("set", "Set power level at position");
            setPermissionGroup(GameMode.Creative);
            xArg = withRequiredArg("x", "X coordinate", ArgTypes.INTEGER);
            yArg = withRequiredArg("y", "Y coordinate", ArgTypes.INTEGER);
            zArg = withRequiredArg("z", "Z coordinate", ArgTypes.INTEGER);
            powerArg = withRequiredArg("power", "Power level (0-15)", ArgTypes.INTEGER);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext ctx) {
            if (!ctx.isPlayer()) {
                ctx.sendMessage(Message.raw("Command must be run by a player"));
                return;
            }

            Vector3i pos = new Vector3i(ctx.get(xArg), ctx.get(yArg), ctx.get(zArg));
            int power = ctx.get(powerArg);

            if (power < 0 || power > 15) {
                ctx.sendMessage(Message.raw("Power must be 0-15"));
                return;
            }

            Player player = ctx.senderAs(Player.class);
            World world = player.getWorld();
            if (world == null) {
                ctx.sendMessage(Message.raw("Player not in a world"));
                return;
            }

            ConduitBlockState state = getConduitState(world, pos);
            if (state == null) {
                ctx.sendMessage(Message.raw("No conduit at " + formatPos(pos)));
                return;
            }

            state.setPowerLevel(power);
            ctx.sendMessage(Message.raw(String.format(
                "Set power to %d at %s",
                power, formatPos(pos)
            )));
        }
    }

    // ==================== Helper Methods ====================

    private ConduitBlockState getConduitState(World world, Vector3i pos) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
        WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
        if (chunk == null) {
            return null;
        }

        BlockState state = chunk.getState(pos.x & 31, pos.y, pos.z & 31);
        if (state instanceof ConduitBlockState) {
            return (ConduitBlockState) state;
        }
        return null;
    }

    private String formatPos(Vector3i pos) {
        return String.format("(%d, %d, %d)", pos.x, pos.y, pos.z);
    }
}

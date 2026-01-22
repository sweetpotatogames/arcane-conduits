package com.example.ctf;

import com.example.ctf.arena.ArenaManager;
import com.example.ctf.match.MatchManager;
import com.example.ctf.match.MatchState;
import com.example.ctf.team.TeamManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;

/**
 * CTF commands for managing the Capture The Flag game mode.
 */
public class CTFCommands extends CommandBase {

    private final CTFPlugin plugin;

    public CTFCommands(@Nonnull CTFPlugin plugin) {
        super("ctf", "Capture The Flag commands");
        this.plugin = plugin;

        // Add subcommands
        addSubCommand(new StatusSubCommand());
        addSubCommand(new StartSubCommand());
        addSubCommand(new EndSubCommand());
        addSubCommand(new ResetSubCommand());
        addSubCommand(new ScoreSubCommand());
        addSubCommand(new TeamJoinSubCommand());
        addSubCommand(new TeamLeaveSubCommand());
        addSubCommand(new SaveSubCommand());
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        ctx.sendMessage(Message.raw("=== CTF Plugin Commands ==="));
        ctx.sendMessage(Message.raw("- /ctf status - Show game status"));
        ctx.sendMessage(Message.raw("- /ctf start - Start match"));
        ctx.sendMessage(Message.raw("- /ctf end - End match"));
        ctx.sendMessage(Message.raw("- /ctf reset - Reset match"));
        ctx.sendMessage(Message.raw("- /ctf score - Show scores"));
        ctx.sendMessage(Message.raw("- /ctf join <red|blue> - Join a team"));
        ctx.sendMessage(Message.raw("- /ctf leave - Leave your team"));
        ctx.sendMessage(Message.raw("- /ctf save - Save arena config"));
    }

    // ==================== Status ====================

    private class StatusSubCommand extends CommandBase {
        public StatusSubCommand() {
            super("status", "Shows current flag and match status");
            setPermissionGroup(GameMode.Adventure);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext ctx) {
            FlagCarrierManager flagManager = plugin.getFlagCarrierManager();
            MatchManager matchManager = plugin.getMatchManager();

            ctx.sendMessage(Message.raw("=== CTF Status ==="));

            if (matchManager != null) {
                MatchState state = matchManager.getState();
                ctx.sendMessage(Message.raw("Match: " + state + " | " + matchManager.getScoreString()));
            }

            for (FlagTeam team : FlagTeam.values()) {
                FlagData flagData = flagManager.getFlagData(team);
                FlagState state = flagData.getState();
                String status = switch (state) {
                    case AT_STAND -> "At stand";
                    case CARRIED -> "Carried";
                    case DROPPED -> "Dropped";
                };
                ctx.sendMessage(Message.raw(team.getDisplayName() + " flag: " + status));
            }
        }
    }

    // ==================== Match Commands ====================

    private class StartSubCommand extends CommandBase {
        public StartSubCommand() {
            super("start", "Start the CTF match");
            setPermissionGroup(GameMode.Creative);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext ctx) {
            MatchManager matchManager = plugin.getMatchManager();
            if (matchManager == null) {
                ctx.sendMessage(Message.raw("Match system not initialized"));
                return;
            }

            if (matchManager.startMatch()) {
                ctx.sendMessage(Message.raw("Match started!"));
            } else {
                ctx.sendMessage(Message.raw("Could not start match. Current state: " + matchManager.getState()));
            }
        }
    }

    private class EndSubCommand extends CommandBase {
        public EndSubCommand() {
            super("end", "End the CTF match");
            setPermissionGroup(GameMode.Creative);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext ctx) {
            MatchManager matchManager = plugin.getMatchManager();
            if (matchManager == null) {
                ctx.sendMessage(Message.raw("Match system not initialized"));
                return;
            }

            if (matchManager.endMatch()) {
                ctx.sendMessage(Message.raw("Match ended."));
            } else {
                ctx.sendMessage(Message.raw("Match is already ended."));
            }
        }
    }

    private class ResetSubCommand extends CommandBase {
        public ResetSubCommand() {
            super("reset", "Reset the CTF match");
            setPermissionGroup(GameMode.Creative);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext ctx) {
            MatchManager matchManager = plugin.getMatchManager();
            if (matchManager == null) {
                ctx.sendMessage(Message.raw("Match system not initialized"));
                return;
            }

            matchManager.resetMatch();
            ctx.sendMessage(Message.raw("Match reset. Scores cleared."));
        }
    }

    private class ScoreSubCommand extends CommandBase {
        public ScoreSubCommand() {
            super("score", "Show current scores");
            setPermissionGroup(GameMode.Adventure);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext ctx) {
            MatchManager matchManager = plugin.getMatchManager();
            if (matchManager == null) {
                ctx.sendMessage(Message.raw("Match system not initialized"));
                return;
            }

            ctx.sendMessage(Message.raw("Score: " + matchManager.getScoreString()));
            ctx.sendMessage(Message.raw("First to " + matchManager.getScoreLimit() + " wins."));
        }
    }

    // ==================== Team Commands ====================

    private class TeamJoinSubCommand extends CommandBase {
        private final OptionalArg<String> teamArg;

        public TeamJoinSubCommand() {
            super("join", "Join a team (red or blue)");
            setPermissionGroup(GameMode.Adventure);
            teamArg = withOptionalArg("team", "Team to join (red or blue)", ArgTypes.STRING);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext ctx) {
            TeamManager teamManager = plugin.getTeamManager();
            if (teamManager == null) {
                ctx.sendMessage(Message.raw("Team system not initialized"));
                return;
            }

            if (!ctx.isPlayer()) {
                ctx.sendMessage(Message.raw("This command must be run as a player"));
                return;
            }

            String teamStr = ctx.get(teamArg);
            if (teamStr == null || teamStr.isEmpty()) {
                ctx.sendMessage(Message.raw("Usage: /ctf join <red|blue>"));
                return;
            }

            FlagTeam team = FlagTeam.fromString(teamStr);
            if (team == null) {
                ctx.sendMessage(Message.raw("Invalid team. Use 'red' or 'blue'"));
                return;
            }

            Player player = ctx.senderAs(Player.class);
            PlayerRef playerRef = player.getPlayerRef();
            if (playerRef == null) {
                ctx.sendMessage(Message.raw("Could not get player reference"));
                return;
            }

            teamManager.assignTeam(playerRef.getUuid(), team, playerRef.getUsername(), playerRef);
            ctx.sendMessage(Message.raw("You joined the " + team.getDisplayName() + " team!"));
        }
    }

    private class TeamLeaveSubCommand extends CommandBase {
        public TeamLeaveSubCommand() {
            super("leave", "Leave your current team");
            setPermissionGroup(GameMode.Adventure);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext ctx) {
            TeamManager teamManager = plugin.getTeamManager();
            if (teamManager == null) {
                ctx.sendMessage(Message.raw("Team system not initialized"));
                return;
            }

            if (!ctx.isPlayer()) {
                ctx.sendMessage(Message.raw("This command must be run as a player"));
                return;
            }

            Player player = ctx.senderAs(Player.class);
            PlayerRef playerRef = player.getPlayerRef();
            if (playerRef == null) {
                ctx.sendMessage(Message.raw("Could not get player reference"));
                return;
            }

            FlagTeam leftTeam = teamManager.leaveTeam(playerRef.getUuid(), playerRef.getUsername(), playerRef);
            if (leftTeam != null) {
                ctx.sendMessage(Message.raw("You left the " + leftTeam.getDisplayName() + " team."));
            } else {
                ctx.sendMessage(Message.raw("You are not on a team."));
            }
        }
    }

    // ==================== Arena Commands ====================

    private class SaveSubCommand extends CommandBase {
        public SaveSubCommand() {
            super("save", "Save arena configuration");
            setPermissionGroup(GameMode.Creative);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext ctx) {
            ArenaManager arenaManager = plugin.getArenaManager();
            if (arenaManager == null) {
                ctx.sendMessage(Message.raw("Arena system not initialized"));
                return;
            }

            arenaManager.save();
            ctx.sendMessage(Message.raw("Arena configuration saved."));
        }
    }
}

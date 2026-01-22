package com.arcane.conduits;

import com.arcane.conduits.blocks.state.ConduitBlockState;
import com.arcane.conduits.blocks.state.PowerSourceBlockState;
import com.arcane.conduits.commands.ConduitDebugCommand;
import com.arcane.conduits.core.power.ConduitNetworkManager;
import com.arcane.conduits.core.tick.ConduitTickProcedure;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktick.config.TickProcedure;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;

import javax.annotation.Nonnull;

/**
 * Arcane Conduits - A magical automation plugin for Hytale.
 *
 * Features wire-like conduit blocks that carry 0-15 power levels,
 * enabling resource processing, base defense, transportation, and farming automation.
 */
public class ArcaneConduitsPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static ArcaneConduitsPlugin instance;

    private ConduitNetworkManager networkManager;

    public ArcaneConduitsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        LOGGER.atInfo().log("Arcane Conduits v%s initializing...",
            this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up Arcane Conduits...");

        // Register custom tick procedures
        registerTickProcedures();

        // Register custom block states
        registerBlockStates();

        // Register event handlers
        registerEventHandlers();

        // Register commands
        registerCommands();

        // Initialize network manager
        networkManager = new ConduitNetworkManager();

        LOGGER.atInfo().log("Arcane Conduits setup complete.");
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Arcane Conduits started successfully!");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Arcane Conduits shutting down...");
        if (networkManager != null) {
            networkManager.shutdown();
        }
    }

    private void registerTickProcedures() {
        // Register the conduit tick procedure for power propagation
        TickProcedure.CODEC.register(
            ConduitTickProcedure.TYPE_ID,
            ConduitTickProcedure.class,
            ConduitTickProcedure.CODEC
        );
        LOGGER.atInfo().log("Registered tick procedure: %s", ConduitTickProcedure.TYPE_ID);
    }

    private void registerBlockStates() {
        // Register conduit block state
        getBlockStateRegistry().registerBlockState(
            ConduitBlockState.class,
            ConduitBlockState.TYPE_ID,
            ConduitBlockState.CODEC,
            ConduitBlockState.ConduitStateData.class,
            ConduitBlockState.ConduitStateData.CODEC
        );
        LOGGER.atInfo().log("Registered block state: %s", ConduitBlockState.TYPE_ID);

        // Register power source block state
        getBlockStateRegistry().registerBlockState(
            PowerSourceBlockState.class,
            PowerSourceBlockState.TYPE_ID,
            PowerSourceBlockState.CODEC,
            PowerSourceBlockState.PowerSourceStateData.class,
            PowerSourceBlockState.PowerSourceStateData.CODEC
        );
        LOGGER.atInfo().log("Registered block state: %s", PowerSourceBlockState.TYPE_ID);
    }

    private void registerEventHandlers() {
        // Handle block placement - invalidate networks when conduits are placed
        getEventRegistry().register(PlaceBlockEvent.class, this::onBlockPlaced);

        // Handle block breaking - invalidate networks when conduits are broken
        getEventRegistry().register(BreakBlockEvent.class, this::onBlockBroken);

        LOGGER.atInfo().log("Registered event handlers for block placement/breaking");
    }

    private void registerCommands() {
        getCommandRegistry().registerCommand(new ConduitDebugCommand());
        LOGGER.atInfo().log("Registered debug commands");
    }

    private void onBlockPlaced(PlaceBlockEvent event) {
        // Check if the placed block is a conduit
        if (isConduitBlock(event.getItemInHand())) {
            // Invalidate any existing network at this position
            networkManager.invalidateNetworkAt(event.getTargetBlock());
        }
    }

    private void onBlockBroken(BreakBlockEvent event) {
        // Check if the broken block is a conduit
        if (event.getBlockType() != null && isConduitBlockType(event.getBlockType().getId())) {
            // Invalidate the network at this position
            networkManager.invalidateNetworkAt(event.getTargetBlock());
        }
    }

    private boolean isConduitBlock(com.hypixel.hytale.server.core.inventory.ItemStack itemStack) {
        if (itemStack == null || itemStack.getItem() == null) return false;
        String itemName = itemStack.getItem().getId();
        return itemName != null && itemName.startsWith("arcaneconduits:");
    }

    private boolean isConduitBlockType(String blockTypeName) {
        return blockTypeName != null && blockTypeName.startsWith("arcaneconduits:");
    }

    /**
     * Get the singleton instance of the plugin.
     */
    public static ArcaneConduitsPlugin getInstance() {
        return instance;
    }

    /**
     * Get the network manager for conduit power networks.
     */
    public ConduitNetworkManager getNetworkManager() {
        return networkManager;
    }
}

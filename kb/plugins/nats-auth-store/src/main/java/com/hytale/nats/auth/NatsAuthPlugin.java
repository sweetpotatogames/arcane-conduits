package com.hytale.nats.auth;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.auth.AuthCredentialStoreProvider;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;

/**
 * Plugin that registers a NATS KV-backed authentication credential store provider.
 * This allows storing OAuth tokens and profile information in a NATS JetStream KV store.
 */
public class NatsAuthPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public NatsAuthPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("NATS Auth Store plugin loaded - version " + this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        registerNatsProvider();
    }

    private void registerNatsProvider() {
        AuthCredentialStoreProvider.CODEC.register(
            NatsAuthCredentialStoreProvider.ID,
            NatsAuthCredentialStoreProvider.class,
            NatsAuthCredentialStoreProvider.CODEC
        );
        LOGGER.atInfo().log("Registered NATS auth credential store provider with Type: '%s'", NatsAuthCredentialStoreProvider.ID);
    }
}

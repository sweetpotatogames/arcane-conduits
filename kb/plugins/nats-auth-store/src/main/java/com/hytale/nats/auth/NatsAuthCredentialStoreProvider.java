package com.hytale.nats.auth;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.auth.AuthCredentialStoreProvider;
import com.hypixel.hytale.server.core.auth.IAuthCredentialStore;

import javax.annotation.Nonnull;

/**
 * Authentication credential store provider that persists credentials to a NATS JetStream KV store.
 * <p>
 * Configuration options (in config.json):
 * <pre>
 * "AuthCredentialStore": {
 *     "Type": "Nats",
 *     "ServerUrl": "nats://localhost:4222",
 *     "Bucket": "auth_credentials",
 *     "KeyPrefix": "hytale/auth/"
 * }
 * </pre>
 */
public class NatsAuthCredentialStoreProvider implements AuthCredentialStoreProvider {

    public static final String ID = "Nats";
    public static final String DEFAULT_SERVER_URL = "nats://localhost:4222";
    public static final String DEFAULT_BUCKET = "auth_credentials";
    public static final String DEFAULT_KEY_PREFIX = "hytale/auth/";

    public static final BuilderCodec<NatsAuthCredentialStoreProvider> CODEC = BuilderCodec.builder(
            NatsAuthCredentialStoreProvider.class, NatsAuthCredentialStoreProvider::new
        )
        .append(new KeyedCodec<>("ServerUrl", Codec.STRING), (o, v) -> o.serverUrl = v, o -> o.serverUrl)
        .add()
        .append(new KeyedCodec<>("Bucket", Codec.STRING), (o, v) -> o.bucket = v, o -> o.bucket)
        .add()
        .append(new KeyedCodec<>("KeyPrefix", Codec.STRING), (o, v) -> o.keyPrefix = v, o -> o.keyPrefix)
        .add()
        .build();

    private String serverUrl = DEFAULT_SERVER_URL;
    private String bucket = DEFAULT_BUCKET;
    private String keyPrefix = DEFAULT_KEY_PREFIX;

    public NatsAuthCredentialStoreProvider() {
    }

    @Nonnull
    @Override
    public IAuthCredentialStore createStore() {
        return new NatsAuthCredentialStore(serverUrl, bucket, keyPrefix);
    }

    @Nonnull
    @Override
    public String toString() {
        return "NatsAuthCredentialStoreProvider{" +
            "serverUrl='" + serverUrl + '\'' +
            ", bucket='" + bucket + '\'' +
            ", keyPrefix='" + keyPrefix + '\'' +
            '}';
    }
}

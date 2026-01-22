package com.hytale.nats.auth;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.auth.IAuthCredentialStore;
import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.KeyValue;
import io.nats.client.KeyValueManagement;
import io.nats.client.KeyValueOptions;
import io.nats.client.Nats;
import io.nats.client.api.KeyValueConfiguration;
import io.nats.client.api.KeyValueEntry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Level;

/**
 * IAuthCredentialStore implementation that stores credentials in a NATS JetStream KV store.
 * <p>
 * Keys stored:
 * <ul>
 *   <li>{prefix}tokens - JSON containing accessToken, refreshToken, expiresAt</li>
 *   <li>{prefix}profile - UUID string of the selected profile</li>
 * </ul>
 */
public class NatsAuthCredentialStore implements IAuthCredentialStore {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final String TOKENS_KEY = "tokens";
    private static final String PROFILE_KEY = "profile";

    private final String serverUrl;
    private final String bucketName;
    private final String keyPrefix;

    private Connection connection;
    private KeyValue kvStore;

    private OAuthTokens cachedTokens = new OAuthTokens(null, null, null);
    @Nullable
    private UUID cachedProfile;

    public NatsAuthCredentialStore(@Nonnull String serverUrl, @Nonnull String bucketName, @Nonnull String keyPrefix) {
        this.serverUrl = serverUrl;
        this.bucketName = bucketName;
        this.keyPrefix = keyPrefix;
        load();
    }

    private synchronized boolean ensureConnected() {
        if (connection != null && connection.getStatus() == Connection.Status.CONNECTED) {
            return true;
        }

        try {
            LOGGER.at(Level.INFO).log("Connecting to NATS server at %s", serverUrl);
            connection = Nats.connect(serverUrl);
            kvStore = getOrCreateBucket();
            LOGGER.at(Level.INFO).log("Connected to NATS KV bucket '%s'", bucketName);
            return true;
        } catch (IOException | InterruptedException e) {
            LOGGER.at(Level.WARNING).withCause(e).log("Failed to connect to NATS server at %s", serverUrl);
            connection = null;
            kvStore = null;
            return false;
        }
    }

    private KeyValue getOrCreateBucket() throws IOException {
        try {
            KeyValueManagement kvm = connection.keyValueManagement();

            // Check if bucket exists by trying to get its status
            try {
                kvm.getStatus(bucketName);
                return connection.keyValue(bucketName);
            } catch (JetStreamApiException e) {
                // Bucket doesn't exist, create it
                LOGGER.at(Level.INFO).log("Creating NATS KV bucket '%s'", bucketName);
                KeyValueConfiguration config = KeyValueConfiguration.builder()
                    .name(bucketName)
                    .description("Hytale authentication credentials")
                    .build();
                kvm.create(config);
                return connection.keyValue(bucketName);
            }
        } catch (JetStreamApiException e) {
            throw new IOException("Failed to access NATS KV bucket: " + e.getMessage(), e);
        }
    }

    private void load() {
        if (!ensureConnected()) {
            return;
        }

        try {
            // Load tokens
            KeyValueEntry tokensEntry = kvStore.get(keyPrefix + TOKENS_KEY);
            if (tokensEntry != null) {
                String tokensJson = new String(tokensEntry.getValue(), StandardCharsets.UTF_8);
                cachedTokens = parseTokensJson(tokensJson);
                LOGGER.at(Level.INFO).log("Loaded tokens from NATS KV");
            }

            // Load profile
            KeyValueEntry profileEntry = kvStore.get(keyPrefix + PROFILE_KEY);
            if (profileEntry != null) {
                String profileStr = new String(profileEntry.getValue(), StandardCharsets.UTF_8);
                cachedProfile = UUID.fromString(profileStr);
                LOGGER.at(Level.INFO).log("Loaded profile from NATS KV: %s", cachedProfile);
            }
        } catch (IOException | JetStreamApiException e) {
            LOGGER.at(Level.WARNING).withCause(e).log("Failed to load credentials from NATS KV");
        } catch (IllegalArgumentException e) {
            LOGGER.at(Level.WARNING).withCause(e).log("Failed to parse credentials from NATS KV");
        }
    }

    private void saveTokens() {
        if (!ensureConnected()) {
            return;
        }

        try {
            String tokensJson = formatTokensJson(cachedTokens);
            kvStore.put(keyPrefix + TOKENS_KEY, tokensJson.getBytes(StandardCharsets.UTF_8));
            LOGGER.at(Level.INFO).log("Saved tokens to NATS KV");
        } catch (IOException | JetStreamApiException e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to save tokens to NATS KV");
        }
    }

    private void saveProfile() {
        if (!ensureConnected()) {
            return;
        }

        try {
            if (cachedProfile != null) {
                kvStore.put(keyPrefix + PROFILE_KEY, cachedProfile.toString().getBytes(StandardCharsets.UTF_8));
            } else {
                // Delete the key if profile is null
                try {
                    kvStore.delete(keyPrefix + PROFILE_KEY);
                } catch (JetStreamApiException e) {
                    // Key might not exist, ignore
                }
            }
            LOGGER.at(Level.INFO).log("Saved profile to NATS KV: %s", cachedProfile);
        } catch (IOException | JetStreamApiException e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to save profile to NATS KV");
        }
    }

    @Override
    public void setTokens(@Nonnull OAuthTokens tokens) {
        this.cachedTokens = tokens;
        saveTokens();
    }

    @Nonnull
    @Override
    public OAuthTokens getTokens() {
        return cachedTokens;
    }

    @Override
    public void setProfile(@Nullable UUID uuid) {
        this.cachedProfile = uuid;
        saveProfile();
    }

    @Nullable
    @Override
    public UUID getProfile() {
        return cachedProfile;
    }

    @Override
    public void clear() {
        cachedTokens = new OAuthTokens(null, null, null);
        cachedProfile = null;

        if (!ensureConnected()) {
            return;
        }

        try {
            try {
                kvStore.delete(keyPrefix + TOKENS_KEY);
            } catch (JetStreamApiException e) {
                // Key might not exist
            }
            try {
                kvStore.delete(keyPrefix + PROFILE_KEY);
            } catch (JetStreamApiException e) {
                // Key might not exist
            }
            LOGGER.at(Level.INFO).log("Cleared credentials from NATS KV");
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).withCause(e).log("Failed to clear credentials from NATS KV");
        }
    }

    private String formatTokensJson(OAuthTokens tokens) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        boolean first = true;
        if (tokens.accessToken() != null) {
            sb.append("\"accessToken\":\"").append(escapeJson(tokens.accessToken())).append("\"");
            first = false;
        }
        if (tokens.refreshToken() != null) {
            if (!first) sb.append(",");
            sb.append("\"refreshToken\":\"").append(escapeJson(tokens.refreshToken())).append("\"");
            first = false;
        }
        if (tokens.accessTokenExpiresAt() != null) {
            if (!first) sb.append(",");
            sb.append("\"expiresAt\":\"").append(tokens.accessTokenExpiresAt().toString()).append("\"");
        }

        sb.append("}");
        return sb.toString();
    }

    private OAuthTokens parseTokensJson(String json) {
        String accessToken = extractJsonString(json, "accessToken");
        String refreshToken = extractJsonString(json, "refreshToken");
        String expiresAtStr = extractJsonString(json, "expiresAt");
        Instant expiresAt = expiresAtStr != null ? Instant.parse(expiresAtStr) : null;
        return new OAuthTokens(accessToken, refreshToken, expiresAt);
    }

    @Nullable
    private String extractJsonString(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);
        if (start == -1) {
            return null;
        }
        start += searchKey.length();
        int end = json.indexOf("\"", start);
        if (end == -1) {
            return null;
        }
        return unescapeJson(json.substring(start, end));
    }

    private String escapeJson(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    private String unescapeJson(String value) {
        return value
            .replace("\\t", "\t")
            .replace("\\r", "\r")
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\");
    }
}

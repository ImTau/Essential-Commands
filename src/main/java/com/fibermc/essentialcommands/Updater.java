package com.fibermc.essentialcommands;

import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.ModMetadata;

public final class Updater {

    private Updater() {}

    private static final String MODRINTH_SLUG = "essential-commands";

    public static void checkForUpdates() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(1500)).build();

        ModMetadata modMetadata = EssentialCommands.MOD_METADATA;
        if (modMetadata == null) {
            EssentialCommands.LOGGER.warn("Failed to check for Essential Commands updates.");
            return;
        }
        String currentVersionStr = modMetadata.getVersion().getFriendlyString();

        // The MC version the server/client is currently running, e.g. "26.1" or "1.21.6".
        String mcVersion = FabricLoader.getInstance()
            .getModContainer("minecraft")
            .map(c -> c.getMetadata().getVersion().getFriendlyString())
            .orElse(null);
        if (mcVersion == null) {
            EssentialCommands.LOGGER.warn(
                "Could not determine Minecraft version; skipping update check."
            );
            return;
        }

        // game_versions and loaders are JSON-array query params, so they must be URL-encoded.
        String gameVersionsParam = encode("[\"%s\"]".formatted(mcVersion));
        String loadersParam = encode("[\"fabric\"]");
        String uri =
            "https://api.modrinth.com/v2/project/%s/version?game_versions=%s&loaders=%s".formatted(
                MODRINTH_SLUG,
                gameVersionsParam,
                loadersParam
            );

        client
            .sendAsync(
                HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .version(HttpClient.Version.HTTP_2)
                    .header(
                        "User-Agent",
                        "jpcode.dev/essential-commands/%s".formatted(
                            modMetadata.getVersion().getFriendlyString()
                        )
                    )
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofInputStream()
            )
            .thenApply(response -> parseLatestVersionFromResponse(response, mcVersion))
            .thenAccept(latestVersionStr -> {
                if (latestVersionStr.isEmpty()) {
                    return;
                }
                compareAndNotify(currentVersionStr, latestVersionStr.get());
            });
    }

    private static Optional<String> parseLatestVersionFromResponse(
        HttpResponse<InputStream> response,
        String mcVersion
    ) {
        if (response.statusCode() != 200) {
            EssentialCommands.LOGGER.warn(
                "Update check failed: Modrinth returned status {}.",
                response.statusCode()
            );
            return Optional.empty();
        }

        // Response body: [ { "version_number": "x.y.z-mcA.B", ... }, ... ], newest-first.
        try (
            JsonReader reader = new JsonReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8)
            )
        ) {
            reader.beginArray();
            if (!reader.hasNext()) {
                EssentialCommands.LOGGER.info(
                    "No Essential Commands release found for Minecraft {}.",
                    mcVersion
                );
                return Optional.empty();
            }
            reader.beginObject();
            while (reader.hasNext()) {
                if (reader.nextName().equals("version_number")) {
                    return Optional.of(reader.nextString());
                }
                reader.skipValue();
            }
        } catch (IOException e) {
            EssentialCommands.LOGGER.warn("Failed to parse update check response.", e);
        }
        return Optional.empty();
    }

    private static void compareAndNotify(String currentVersionStr, String latestVersionStr) {
        try {
            Version currentVers = Version.parse(stripMinecraftVersion(currentVersionStr));
            Version latestVers = Version.parse(stripMinecraftVersion(latestVersionStr));

            if (latestVers.compareTo(currentVers) > 0) {
                String updateMessage = String.format(
                    "A new version of Essential Commands is available. Current: '%s' Latest: '%s'. Get the new version at %s",
                    currentVersionStr,
                    latestVersionStr,
                    "https://modrinth.com/mod/essential-commands"
                );
                EssentialCommands.LOGGER.info(updateMessage);
                ServerLifecycleEvents.SERVER_STARTED.register(server ->
                    EssentialCommands.LOGGER.info(updateMessage)
                );
            } else {
                EssentialCommands.LOGGER.info("Essential Commands is up to date!");
            }
        } catch (VersionParsingException e) {
            e.printStackTrace();
        }
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String stripMinecraftVersion(String versionStr) {
        return versionStr.substring(0, versionStr.indexOf("-mc"));
    }
}

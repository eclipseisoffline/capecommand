package xyz.eclipseisoffline.capecommand;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import org.geysermc.api.Geyser;

public class CapeConfig {
    private static final Path CONFIG_FILE = Path.of("playercapes.json");

    private final Map<UUID, Cape> playerCapes = new HashMap<>();
    private final List<GameProfile> capeCommandPlayers = new ArrayList<>();
    private final Path configFile;
    private final boolean geyserAvailable;

    private CapeConfig(Path configFile) {
        boolean geyserAvailable;
        try {
            Class.forName("org.geysermc.api.Geyser");
            geyserAvailable = true;
            CapeCommand.LOGGER.info("Geyser compatibility enabled!");
        } catch (NoClassDefFoundError | ClassNotFoundException error) {
            geyserAvailable = false;
        }
        this.configFile = configFile;
        this.geyserAvailable = geyserAvailable;
    }

    public Cape getPlayerCape(GameProfile profile) {
        return playerCapes.get(profile.id());
    }

    public void setPlayerCape(GameProfile profile, Cape cape) {
        playerCapes.put(profile.id(), cape);
        writeToConfig();
    }

    public void resetPlayerCape(GameProfile profile) {
        playerCapes.remove(profile.id());
        writeToConfig();
    }

    public void registerCapeCommandPlayer(GameProfile profile) {
        capeCommandPlayers.add(profile);
    }

    public boolean hasCapeCommand(ServerPlayer player) {
        return capeCommandPlayers.contains(player.getGameProfile()) || (geyserAvailable && Geyser.api().isBedrockPlayer(player.getUUID()));
    }

    public void unregisterCapeCommandPlayer(ServerPlayer player) {
        capeCommandPlayers.remove(player.getGameProfile());
    }

    public boolean isGeyserAvailable() {
        return geyserAvailable;
    }

    private void writeToConfig() {
        JsonObject capesJson = new JsonObject();
        for (Entry<UUID, Cape> playerCape : playerCapes.entrySet()) {
            capesJson.addProperty(playerCape.getKey().toString(), playerCape.getValue().toString());
        }

        try {
            Files.writeString(configFile, capesJson.toString());
        } catch (IOException exception) {
            CapeCommand.LOGGER.warn("Failed to save player cape config!", exception);
        }
    }

    public static CapeConfig readFromConfig(Path configDir) {
        Path capeConfigPath = configDir.resolve(CONFIG_FILE);
        CapeConfig loaded = new CapeConfig(capeConfigPath);

        if (Files.exists(capeConfigPath)) {
            try {
                JsonObject capesJson = JsonParser.parseString(Files.readString(capeConfigPath)).getAsJsonObject();
                for (Entry<String, JsonElement> playerCape : capesJson.entrySet()) {
                    try {
                        loaded.playerCapes.put(UUID.fromString(playerCape.getKey()), Cape.valueOf(playerCape.getValue().getAsString()));
                    } catch (IllegalArgumentException exception) {
                        CapeCommand.LOGGER.warn("Read invalid cape for UUID {}! ({})", playerCape.getKey(), playerCape.getValue().getAsString());
                    }
                }
            } catch (IOException exception) {
                CapeCommand.LOGGER.warn("Failed to read player cape config!", exception);
            }
        }
        return loaded;
    }
}

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
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;

public class CapeConfig {

    private static final Path CONFIG_FILE = Path.of("playercapes.json");
    private final Map<UUID, Cape> playerCapes = new HashMap<>();
    private final List<GameProfile> capeCommandPlayers = new ArrayList<>();

    public Cape getPlayerCape(GameProfile gameProfile) {
        return playerCapes.get(gameProfile.getId());
    }

    public void setPlayerCape(GameProfile gameProfile, Cape cape) {
        playerCapes.put(gameProfile.getId(), cape);
        writeToConfig();
    }

    public void resetPlayerCape(GameProfile gameProfile) {
        playerCapes.remove(gameProfile.getId());
        writeToConfig();
    }

    public void registerCapeCommandPlayer(GameProfile serverPlayerEntity) {
        capeCommandPlayers.add(serverPlayerEntity);
    }

    public boolean hasCapeCommand(ServerPlayerEntity serverPlayerEntity) {
        return capeCommandPlayers.contains(serverPlayerEntity.getGameProfile());
    }

    public void unregisterCapeCommandPlayer(ServerPlayerEntity serverPlayerEntity) {
        capeCommandPlayers.remove(serverPlayerEntity.getGameProfile());
    }

    private void writeToConfig() {
        JsonObject capesJson = new JsonObject();
        for (Entry<UUID, Cape> playerCape : playerCapes.entrySet()) {
            capesJson.addProperty(playerCape.getKey().toString(), playerCape.getValue().toString());
        }

        Path capeConfigPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        try {
            Files.writeString(capeConfigPath, capesJson.toString());
        } catch (IOException exception) {
            CapeCommand.LOGGER.warn("Failed to save player cape config!", exception);
        }
    }

    public void readFromConfig() {
        Path capeConfigPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        if (Files.exists(capeConfigPath)) {
            try {
                JsonObject capesJson = JsonParser.parseString(Files.readString(capeConfigPath))
                        .getAsJsonObject();
                for (Entry<String, JsonElement> playerCape : capesJson.entrySet()) {
                    try {
                        playerCapes.put(UUID.fromString(playerCape.getKey()),
                                Cape.valueOf(playerCape.getValue().getAsString()));
                    } catch (IllegalArgumentException exception) {
                        CapeCommand.LOGGER.warn("Read invalid cape for UUID " + playerCape.getKey()
                                + "! (" + playerCape.getValue().getAsString() + ")");
                    }
                }
            } catch (IOException exception) {
                CapeCommand.LOGGER.warn("Failed to read player cape config!", exception);
            }
        }
    }
}

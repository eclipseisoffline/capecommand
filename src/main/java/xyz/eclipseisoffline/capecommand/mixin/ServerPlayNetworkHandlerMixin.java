package xyz.eclipseisoffline.capecommand.mixin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Entry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import xyz.eclipseisoffline.capecommand.CapeCommand;
import xyz.eclipseisoffline.capecommand.network.PlayerListS2CPacketEntriesUpdater;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin extends ServerCommonNetworkHandler {

    @Shadow
    public ServerPlayerEntity player;

    public ServerPlayNetworkHandlerMixin(MinecraftServer server,
            ClientConnection connection,
            ConnectedClientData clientData) {
        super(server, connection, clientData);
    }

    @Override
    public void send(Packet<?> packet, @Nullable PacketCallbacks callbacks) {
        if (packet instanceof PlayerListS2CPacket playerListS2CPacket) {
            if (playerListS2CPacket.getActions().contains(Action.ADD_PLAYER)) {
                List<Entry> entries = new ArrayList<>();
                for (Entry entry : playerListS2CPacket.getEntries()) {
                    GameProfile profile = entry.profile();
                    if (profile != null
                            && (CapeCommand.CONFIG.hasCapeCommand(player) || profile.getId().equals(player.getUuid()))
                            && CapeCommand.CONFIG.getPlayerCape(profile) != null) {
                        GameProfile newProfile = new GameProfile(profile.getId(), profile.getName());

                        assert entry.profile() != null;
                        newProfile.getProperties().putAll(entry.profile().getProperties());
                        setCustomCapeInGameProfile(newProfile);
                        profile = newProfile;
                    }
                    entries.add(new Entry(entry.profileId(), profile, entry.listed(),
                            entry.latency(), entry.gameMode(), entry.displayName(), entry.showHat(),
                            entry.listOrder(), entry.chatSession()));
                }
                ((PlayerListS2CPacketEntriesUpdater) playerListS2CPacket).capeCommand$setEntries(
                        entries);
            }
        }
        super.send(packet, callbacks);
    }

    @Unique
    private void setCustomCapeInGameProfile(GameProfile gameProfile) {
        Property texturesProperty = gameProfile.getProperties().get("textures").stream().findAny()
                .orElse(null);
        JsonObject textures;
        if (texturesProperty != null) {
            String texturesJson = new String(Base64.getDecoder().decode(texturesProperty.value()));
            textures = JsonParser.parseString(texturesJson).getAsJsonObject();
        } else {
            // Create an empty textures object for offline players / dev accounts
            textures = new JsonObject();
            textures.add("textures", new JsonObject());
        }

        JsonObject capeObject;
        if (textures.getAsJsonObject("textures").get("CAPE") != null) {
            capeObject = textures.getAsJsonObject("textures").getAsJsonObject("CAPE");
        } else {
            capeObject = new JsonObject();
            textures.getAsJsonObject("textures").add("CAPE", capeObject);
        }
        capeObject.remove("url");
        capeObject.addProperty("url", CapeCommand.CONFIG.getPlayerCape(gameProfile).getCapeURL());

        textures.remove("signatureRequired");

        String newTextures = Base64.getEncoder().encodeToString(textures.toString().getBytes());
        Property newTexturesProperty = new Property("textures", newTextures);

        gameProfile.getProperties().removeAll("textures");
        gameProfile.getProperties().put("textures", newTexturesProperty);
    }
}

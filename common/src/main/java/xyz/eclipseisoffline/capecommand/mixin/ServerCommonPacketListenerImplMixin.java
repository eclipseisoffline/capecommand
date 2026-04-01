package xyz.eclipseisoffline.capecommand.mixin;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.mojang.authlib.properties.PropertyMap;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import xyz.eclipseisoffline.capecommand.Cape;
import xyz.eclipseisoffline.capecommand.CapeCommand;
import xyz.eclipseisoffline.capecommand.network.ClientboundPlayerInfoUpdatePacketEntriesUpdater;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin implements ServerCommonPacketListener {

    @WrapOperation(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerCommonPacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V"))
    public void modifyPlayerListPacket(ServerCommonPacketListenerImpl instance, Packet<?> packet, @Nullable ChannelFutureListener futureListener, Operation<Void> original) {
        if (instance instanceof ServerGamePacketListenerImpl gamePacketListener && packet instanceof ClientboundPlayerInfoUpdatePacket playerInfoUpdatePacket) {
            ServerPlayer player = gamePacketListener.player;
            if (playerInfoUpdatePacket.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) {
                List<ClientboundPlayerInfoUpdatePacket.Entry> entries = new ArrayList<>();
                for (ClientboundPlayerInfoUpdatePacket.Entry entry : playerInfoUpdatePacket.entries()) {
                    GameProfile profile = entry.profile();
                    if (profile != null) {
                        Cape cape = CapeCommand.CONFIG.getPlayerCape(profile);
                        if (cape != null && (CapeCommand.CONFIG.hasCapeCommand(player) || entry.profileId().equals(player.getUUID()))) {
                            profile = new GameProfile(profile.id(), profile.name(), capeCommand$setCustomCapeInGameProfile(profile.properties(), cape));
                        }
                        entries.add(new ClientboundPlayerInfoUpdatePacket.Entry(entry.profileId(), profile, entry.listed(),
                                entry.latency(), entry.gameMode(), entry.displayName(), entry.showHat(),
                                entry.listOrder(), entry.chatSession()));
                    }
                }
                ((ClientboundPlayerInfoUpdatePacketEntriesUpdater) playerInfoUpdatePacket).capeCommand$setEntries(entries);
            }
        }
        original.call(instance, packet, futureListener);
    }

    @Unique
    private static PropertyMap capeCommand$setCustomCapeInGameProfile(PropertyMap properties, Cape cape) {
        Property texturesProperty = properties.get("textures").stream().findAny()
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
        capeObject.addProperty("url", cape.getCapeURL());

        textures.remove("signatureRequired");

        String newTextures = Base64.getEncoder().encodeToString(textures.toString().getBytes());
        Property newTexturesProperty = new Property("textures", newTextures);

        Multimap<String, Property> newProperties = MultimapBuilder.hashKeys().arrayListValues().build();
        newProperties.putAll(properties);
        newProperties.removeAll("textures");
        newProperties.put("textures", newTexturesProperty);
        return new PropertyMap(newProperties);
    }
}

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
import net.minecraft.network.listener.ServerCommonPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Entry;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import xyz.eclipseisoffline.capecommand.Cape;
import xyz.eclipseisoffline.capecommand.CapeCommand;
import xyz.eclipseisoffline.capecommand.network.PlayerListS2CPacketEntriesUpdater;

@Mixin(ServerCommonNetworkHandler.class)
public abstract class ServerCommonNetworkHandlerMixin implements ServerCommonPacketListener {

    @WrapOperation(method = "sendPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerCommonNetworkHandler;send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;)V"))
    public void modifyPlayerListPacket(ServerCommonNetworkHandler instance, Packet<?> packet, @Nullable ChannelFutureListener channelFutureListener, Operation<Void> original) {
        if (instance instanceof ServerPlayNetworkHandler playNetworkHandler
                && packet instanceof PlayerListS2CPacket playerListS2CPacket) {
            ServerPlayerEntity player = playNetworkHandler.player;
            if (playerListS2CPacket.getActions().contains(Action.ADD_PLAYER)) {
                List<Entry> entries = new ArrayList<>();
                for (Entry entry : playerListS2CPacket.getEntries()) {
                    GameProfile profile = entry.profile();
                    if (profile != null) {
                        Cape cape = CapeCommand.CONFIG.getPlayerCape(profile);
                        if (cape != null && (CapeCommand.CONFIG.hasCapeCommand(player) || entry.profileId().equals(player.getUuid()))) {
                            profile = new GameProfile(profile.id(), profile.name(), setCustomCapeInGameProfile(profile.properties(), cape));
                        }
                        entries.add(new Entry(entry.profileId(), profile, entry.listed(),
                                entry.latency(), entry.gameMode(), entry.displayName(), entry.showHat(),
                                entry.listOrder(), entry.chatSession()));
                    }
                }
                ((PlayerListS2CPacketEntriesUpdater) playerListS2CPacket).capeCommand$setEntries(entries);
            }
        }
        original.call(instance, packet, channelFutureListener);
    }

    @Unique
    private static PropertyMap setCustomCapeInGameProfile(PropertyMap properties, Cape cape) {
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

package xyz.eclipseisoffline.capecommand.fabric;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import xyz.eclipseisoffline.capecommand.CapeCommand;

import java.nio.file.Path;
import java.util.function.Consumer;

public class CapeCommandFabric extends CapeCommand implements ModInitializer, ClientModInitializer {

    @Override
    public void onInitialize() {
        initialize();
    }

    @Override
    public void onInitializeClient() {
        initializeClient();
    }

    @Override
    protected void registerCommands(Consumer<CommandDispatcher<CommandSourceStack>> registerer) {
        CommandRegistrationCallback.EVENT.register((dispatcher, _, _) -> registerer.accept(dispatcher));
    }

    @Override
    protected void registerConfigurationNetworkingHandler(Consumer<ServerConfigurationPacketListenerImpl> handler) {
        ServerConfigurationConnectionEvents.CONFIGURE.register((listener, _) -> handler.accept(listener));
    }

    @Override
    protected <T extends CustomPacketPayload> void registerClientboundConfigurationCustomPayloadType(CustomPacketPayload.Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec) {
        PayloadTypeRegistry.clientboundConfiguration().register(type, codec);
    }

    @Override
    protected boolean canSendCustomPayload(ServerConfigurationPacketListenerImpl configurationPacketListener, CustomPacketPayload.Type<?> type) {
        return ServerConfigurationNetworking.canSend(configurationPacketListener, type);
    }

    @Override
    protected <T extends CustomPacketPayload> void registerClientboundCustomPayloadHandler(CustomPacketPayload.Type<T> type, Consumer<T> handler) {
        ClientConfigurationNetworking.registerGlobalReceiver(type, (payload, _) -> handler.accept(payload));
    }

    @Override
    protected Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }
}

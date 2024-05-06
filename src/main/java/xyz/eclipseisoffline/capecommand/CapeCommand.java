package xyz.eclipseisoffline.capecommand;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.S2CConfigurationChannelEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.CustomPayload.Id;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.eclipseisoffline.capecommand.mixin.ServerConfigurationNetworkHandlerAccessor;

public class CapeCommand implements ModInitializer, ClientModInitializer {
    public static final Id<CustomPayload> INSTALLED_ID = new Id<>(
            Identifier.of("capecommand", "installed"));
    public static final Logger LOGGER = LoggerFactory.getLogger("CapeCommand");
    public static final CapeConfig CONFIG = new CapeConfig();

    @Override
    public void onInitialize() {
        LOGGER.info("Initialising cape command");
        LOGGER.info("Trying to load config, if it exists");
        CONFIG.readFromConfig();

        LOGGER.info("Registering cape command");
        CommandRegistrationCallback.EVENT.register(
                ((commandDispatcher, commandRegistryAccess, registrationEnvironment) -> commandDispatcher.register(
                        CommandManager.literal("cape")
                                .requires(ServerCommandSource::isExecutedByPlayer)
                                .then(CommandManager.argument("name", StringArgumentType.word())
                                        .suggests(new CapeCommandSuggestionProvider())
                                        .executes(context -> {
                                            String capeString = StringArgumentType.getString(
                                                    context, "name");
                                            Cape cape;
                                            try {
                                                cape = Cape.valueOf(capeString.toUpperCase());
                                            } catch (IllegalArgumentException exception) {
                                                throw new SimpleCommandExceptionType(Text.of("Unknown cape")).create();
                                            }

                                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                            if (cape.requiresClient() && !CONFIG.hasCapeCommand(player)) {
                                                throw new SimpleCommandExceptionType(Text.of("This cape requires you to install the Cape Command mod locally.")).create();
                                            }

                                            CONFIG.setPlayerCape(context.getSource()
                                                    .getPlayerOrThrow().getGameProfile(), cape);

                                            String clientNote = "Note that this cape is only visible to you and other players that have Cape Command installed.";
                                            context.getSource().sendFeedback(() -> Text.of(
                                                    "Cape saved. Relog for it to apply. "
                                                            + clientNote), true);

                                            return 0;
                                        }))
                                .then(CommandManager.literal("reset")
                                        .executes(context -> {
                                            CONFIG.resetPlayerCape(context.getSource()
                                                    .getPlayerOrThrow().getGameProfile());
                                            context.getSource().sendFeedback(() -> Text.of(
                                                    "Cape reset. Relog for it to apply."), true);
                                            return 0;
                                        })))));

        LOGGER.info("Registering server network handlers");
        S2CConfigurationChannelEvents.REGISTER.register((handler, sender, server, channels) -> {
            if (ServerConfigurationNetworking.canSend(handler, INSTALLED_ID)) {
                GameProfile profile = ((ServerConfigurationNetworkHandlerAccessor) handler).getProfile();
                LOGGER.info("Player {} has cape commands installed client side", profile.getName());
                CONFIG.registerCapeCommandPlayer(profile);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register(
                ((handler, server) -> CONFIG.unregisterCapeCommandPlayer(handler.getPlayer())));
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("Registering client network handlers");
        PayloadTypeRegistry.configurationS2C().register(INSTALLED_ID,
                new PacketCodec<>() {
                    @Override
                    public CustomPayload decode(PacketByteBuf buf) {
                        throw new AssertionError();
                    }

                    @Override
                    public void encode(PacketByteBuf buf, CustomPayload value) {
                        throw new AssertionError();
                    }
                });
        ClientConfigurationNetworking.registerGlobalReceiver(INSTALLED_ID, (payload, context) -> {});
    }
}

package xyz.eclipseisoffline.capecommand;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.eclipseisoffline.capecommand.mixin.ServerConfigurationNetworkHandlerAccessor;
import xyz.eclipseisoffline.capecommand.network.CapeCommandInstalledPacket;

public class CapeCommand implements ModInitializer, ClientModInitializer {

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
                                .then(CommandManager.argument("name", StringArgumentType.word())
                                        .suggests(new CapeCommandSuggestionProvider())
                                        .executes(context -> {
                                            if (!context.getSource().isExecutedByPlayer()) {
                                                context.getSource().sendError(
                                                        Text.of("Capes can only be applied to players!"));
                                                return 1;
                                            }

                                            String capeString = StringArgumentType.getString(
                                                    context, "name");
                                            Cape cape;
                                            try {
                                                cape = Cape.valueOf(capeString.toUpperCase());
                                            } catch (IllegalArgumentException exception) {
                                                context.getSource()
                                                        .sendError(Text.of("Unknown cape"));
                                                return 1;
                                            }

                                            assert context.getSource().getPlayer() != null;
                                            CONFIG.setPlayerCape(context.getSource().getPlayer()
                                                    .getGameProfile(), cape);

                                            String clientNote = cape.requiresClient()
                                                    ? "Note that this cape is only visible to players with the Cape Command installed."
                                                    : "Note that this cape is only visible to you and other players that have Cape Command installed.";
                                            context.getSource().sendFeedback(() -> Text.of(
                                                    "Cape saved. Relog for it to apply. "
                                                            + clientNote), false);

                                            return 0;
                                        }))
                                .then(CommandManager.literal("reset")
                                        .executes(context -> {
                                            if (!context.getSource().isExecutedByPlayer()) {
                                                context.getSource().sendError(
                                                        Text.of("Capes can only be applied to players!"));
                                                return 1;
                                            }

                                            assert context.getSource().getPlayer() != null;
                                            CONFIG.resetPlayerCape(context.getSource().getPlayer()
                                                    .getGameProfile());
                                            context.getSource().sendFeedback(() -> Text.of(
                                                    "Cape reset. Relog for it to apply."), false);
                                            return 0;
                                        })))));

        LOGGER.info("Registering server network handlers");
        ServerConfigurationNetworking.registerGlobalReceiver(CapeCommandInstalledPacket.TYPE,
                ((packet, networkHandler, responseSender) -> {
                    GameProfile profile = ((ServerConfigurationNetworkHandlerAccessor) networkHandler).getProfile();
                    LOGGER.info("Player " + profile.getName()
                            + " has cape commands installed client side");
                    CONFIG.registerCapeCommandPlayer(profile);
                }));
        ServerPlayConnectionEvents.DISCONNECT.register(
                ((handler, server) -> CONFIG.unregisterCapeCommandPlayer(handler.getPlayer())));
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("Registering client network handlers");
        ClientConfigurationConnectionEvents.INIT.register(((handler, client) -> handler.sendPacket(
                ClientPlayNetworking.createC2SPacket(new CapeCommandInstalledPacket()))));
    }
}

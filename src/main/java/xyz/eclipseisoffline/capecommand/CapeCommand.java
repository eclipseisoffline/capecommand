package xyz.eclipseisoffline.capecommand;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.S2CConfigurationChannelEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerPosition;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.CustomPayload.Id;
import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.eclipseisoffline.capecommand.mixin.EntityAccessor;
import xyz.eclipseisoffline.capecommand.mixin.ServerChunkLoadingManagerAccessor;
import xyz.eclipseisoffline.capecommand.mixin.ServerConfigurationNetworkHandlerAccessor;

import java.util.List;

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
                ((dispatcher, registry, environment) -> dispatcher.register(
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

                                            CONFIG.setPlayerCape(context.getSource().getPlayerOrThrow().getGameProfile(), cape);

                                            reloadPlayerSkin(context.getSource());
                                            context.getSource().sendFeedback(() -> Text.of("Cape saved. Note that this cape is only visible to you and other players that have Cape Command installed."), true);

                                            return 0;
                                        }))
                                .then(CommandManager.literal("reset")
                                        .executes(context -> {
                                            CONFIG.resetPlayerCape(context.getSource()
                                                    .getPlayerOrThrow().getGameProfile());
                                            reloadPlayerSkin(context.getSource());
                                            context.getSource().sendFeedback(() -> Text.of(
                                                    "Cape reset"), true);
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

    private void reloadPlayerSkin(ServerCommandSource source) throws CommandSyntaxException {
        ServerChunkLoadingManager chunkManager = source.getWorld().getChunkManager().chunkLoadingManager;
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ServerChunkLoadingManager.EntityTracker trackedPlayer = ((ServerChunkLoadingManagerAccessor) chunkManager).getEntityTrackers().get(player.getId());

        for (ServerPlayerEntity other : source.getServer().getPlayerManager().getPlayerList()) {
            boolean same = other == player;
            if (!same) {
                trackedPlayer.stopTracking(other);
            }

            other.networkHandler.sendPacket(new PlayerRemoveS2CPacket(List.of(player.getUuid())));
            other.networkHandler.sendPacket(PlayerListS2CPacket.entryFromPlayer(List.of(player)));
            if (same) {
                // "Respawn" the player to reload the skin on their client

                // Close any menus open
                player.playerScreenHandler.onClosed(player);
                if (player.currentScreenHandler != null && player.shouldCloseHandledScreenOnRespawn()) {
                    player.onHandledScreenClosed();
                }

                // Respawn player, which will show a "Loading terrain" screen
                player.networkHandler.sendPacket(new PlayerRespawnS2CPacket(other.createCommonPlayerSpawnInfo(source.getWorld()), PlayerRespawnS2CPacket.KEEP_ALL));

                // This is necessary to close the "Loading terrain" screen and go back to the world
                player.networkHandler.requestTeleport(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
                player.networkHandler.syncWithPlayerPosition();

                source.getWorld().removePlayer(player, Entity.RemovalReason.CHANGED_DIMENSION);
                ((EntityAccessor) player).invokeUnsetRemoved();
                source.getWorld().onDimensionChanged(player);
                player.clearActiveItem();

                player.networkHandler.sendPacket(new PlayerAbilitiesS2CPacket(player.getAbilities()));
                source.getServer().getPlayerManager().sendWorldInfo(player, source.getWorld());

                // Client clears these when respawning
                source.getServer().getPlayerManager().sendPlayerStatus(player);
                source.getServer().getPlayerManager().sendStatusEffects(player);

                continue;
            }
            trackedPlayer.updateTrackedStatus(other);
        }
    }
}

package xyz.eclipseisoffline.capecommand;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity.RemovalReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.eclipseisoffline.capecommand.mixin.EntityAccessor;
import xyz.eclipseisoffline.capecommand.mixin.ChunkMapAccessor;
import xyz.eclipseisoffline.capecommand.mixin.ServerPlayerEntityAccessor;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public abstract class CapeCommand {
    public static final CustomPacketPayload.Type<CustomPacketPayload> INSTALLED_ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("capecommand", "installed"));
    public static final Logger LOGGER = LoggerFactory.getLogger("CapeCommand");
    private static CapeConfig config;

    public void initialize() {
        LOGGER.info("Initialising cape command");
        LOGGER.info("Trying to load config, if it exists");
        config = CapeConfig.readFromConfig(getConfigDir());

        LOGGER.info("Registering cape command");
        registerCommands(dispatcher -> dispatcher.register(
                Commands.literal("cape")
                        .requires(CommandSourceStack::isPlayer)
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(new CapeCommandSuggestionProvider())
                                .executes(context -> {
                                    String capeString = StringArgumentType.getString(context, "name");
                                    Cape cape;
                                    try {
                                        cape = Cape.valueOf(capeString.toUpperCase());
                                    } catch (IllegalArgumentException exception) {
                                        throw new SimpleCommandExceptionType(Component.literal("Unknown cape")).create();
                                    }

                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    if (cape.requiresClient() && !config.hasCapeCommand(player)) {
                                        throw new SimpleCommandExceptionType(Component.literal("This cape requires you to install the Cape Command mod locally")).create();
                                    }

                                    config.setPlayerCape(context.getSource().getPlayerOrException().getGameProfile(), cape);

                                    reloadPlayerSkin(context.getSource());
                                    context.getSource().sendSuccess(() -> Component.literal("Now wearing cape \"" + capeString.toLowerCase() + "\""), true);

                                    if (config.isGeyserAvailable()) {
                                        context.getSource().sendSuccess(() -> Component.literal("Note that this cape is only visible to you, bedrock players, and other Java players that have Cape Command installed"), false);
                                    } else {
                                        context.getSource().sendSuccess(() -> Component.literal("Note that this cape is only visible to you and other players that have Cape Command installed"), false);
                                    }

                                    return 0;
                                })
                        )
                        .then(Commands.literal("reset")
                                .executes(context -> {
                                    config.resetPlayerCape(context.getSource().getPlayerOrException().getGameProfile());
                                    reloadPlayerSkin(context.getSource());
                                    context.getSource().sendSuccess(() -> Component.literal("Cape reset"), true);
                                    return 0;
                                })
                        )
                )
        );

        /*LOGGER.info("Registering server network handlers");
        S2CConfigurationChannelEvents.REGISTER.register((handler, sender, server, channels) -> {
            if (ServerConfigurationNetworking.canSend(handler, INSTALLED_ID)) {
                GameProfile profile = ((ServerConfigurationNetworkHandlerAccessor) handler).getProfile();
                LOGGER.info("Player {} has cape commands installed client side", profile.name());
                CONFIG.registerCapeCommandPlayer(profile);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register(
                ((handler, server) -> CONFIG.unregisterCapeCommandPlayer(handler.getPlayer())));*/
    }

    /*@Override
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
    }*/

    protected abstract void registerCommands(Consumer<CommandDispatcher<CommandSourceStack>> registerer);

    protected abstract Path getConfigDir();

    public static CapeConfig getConfig() {
        if (config == null) {
            throw new IllegalStateException("CapeConfig accessed before it was loaded");
        }
        return config;
    }

    private void reloadPlayerSkin(CommandSourceStack source) throws CommandSyntaxException {
        ChunkMap chunkMap = source.getLevel().getChunkSource().chunkMap;
        ServerPlayer player = source.getPlayerOrException();
        ChunkMap.TrackedEntity trackedPlayer = ((ChunkMapAccessor) chunkMap).getEntityMap().get(player.getId());

        for (ServerPlayer other : source.getServer().getPlayerList().getPlayers()) {
            other.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(player.getUUID())));
            other.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(player)));

            if (other != player) {
                trackedPlayer.removePlayer(other);
                trackedPlayer.updatePlayer(other);
            } else {
                // "Respawn" the player to reload the skin on their client TODO CHECK THIS

                // Close any menus open
                /*player.inventoryMenu.removed(player);
                if (player.hasContainerOpen()) {
                    player.doCloseContainer();
                }*/

                // Respawn player, which will show a "Loading terrain" screen
                player.connection.send(new ClientboundRespawnPacket(player.createCommonSpawnInfo(source.getLevel()), ClientboundRespawnPacket.KEEP_ALL_DATA));

                // This is necessary to close the "Loading terrain" screen and go back to the world
                player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
                player.connection.resetPosition();

                source.getLevel().removePlayerImmediately(player, RemovalReason.CHANGED_DIMENSION);
                ((EntityAccessor) player).invokeUnsetRemoved();
                source.getLevel().addDuringTeleport(player);
                player.stopUsingItem();

                player.connection.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
                source.getServer().getPlayerList().sendLevelInfo(player, source.getLevel());

                // Client clears these when respawning
                source.getServer().getPlayerList().sendAllPlayerInfo(player);
                source.getServer().getPlayerList().sendActivePlayerEffects(player);
                ((ServerPlayerEntityAccessor) player).setLastSentExp(-1);
                ((ServerPlayerEntityAccessor) player).setLastSentHealth(-1.0F);
                //((ServerPlayerEntityAccessor) player).setSyncedFoodLevel(-1);
            }
        }
    }
}

package xyz.eclipseisoffline.capecommand.fabric;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import xyz.eclipseisoffline.capecommand.CapeCommand;

import java.nio.file.Path;
import java.util.function.Consumer;

public class CapeCommandFabric extends CapeCommand implements ModInitializer {

    @Override
    public void onInitialize() {
        initialize();
    }

    @Override
    protected void registerCommands(Consumer<CommandDispatcher<CommandSourceStack>> registerer) {
        CommandRegistrationCallback.EVENT.register((dispatcher, _, _) -> registerer.accept(dispatcher));
    }

    @Override
    protected Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }
}

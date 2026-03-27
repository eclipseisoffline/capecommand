package xyz.eclipseisoffline.capecommand;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class CapeCommandSuggestionProvider implements SuggestionProvider<ServerCommandSource> {

    @Override
    public CompletableFuture<Suggestions> getSuggestions(
            CommandContext<ServerCommandSource> context, SuggestionsBuilder builder)
            throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        boolean hasClient = CapeCommand.CONFIG.hasCapeCommand(player);

        return CommandSource.suggestMatching(
                Arrays.stream(Cape.values())
                        .filter(cape -> !cape.requiresClient() || hasClient)
                        .map(cape -> cape.toString().toLowerCase()), builder);
    }
}

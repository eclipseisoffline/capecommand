package xyz.eclipseisoffline.capecommand;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class CapeCommandSuggestionProvider implements SuggestionProvider<CommandSourceStack> {

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean hasClient = CapeCommand.CONFIG.hasCapeCommand(player);

        return SharedSuggestionProvider.suggest(Arrays.stream(Cape.values())
                .filter(cape -> !cape.requiresClient() || hasClient)
                .map(cape -> cape.toString().toLowerCase()), builder);
    }
}

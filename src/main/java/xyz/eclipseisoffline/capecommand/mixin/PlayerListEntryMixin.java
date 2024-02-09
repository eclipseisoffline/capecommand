package xyz.eclipseisoffline.capecommand.mixin;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerListEntry.class)
public class PlayerListEntryMixin {

    @Redirect(method = "method_52806", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/SkinTextures;secure()Z"))
    private static boolean skinIsAlwaysSigned(SkinTextures instance) {
        return true;
    }
}

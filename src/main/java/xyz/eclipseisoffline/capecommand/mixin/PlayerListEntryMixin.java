package xyz.eclipseisoffline.capecommand.mixin;

import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(PlayerListEntry.class)
public class PlayerListEntryMixin {

    @ModifyArg(method = "texturesSupplier", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/PlayerSkinProvider;supplySkinTextures(Lcom/mojang/authlib/GameProfile;Z)Ljava/util/function/Supplier;"))
    private static boolean skinIsAlwaysSigned(boolean requireSecure) {
        return true;
    }
}

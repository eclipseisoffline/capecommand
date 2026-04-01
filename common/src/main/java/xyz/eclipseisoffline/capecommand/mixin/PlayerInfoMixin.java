package xyz.eclipseisoffline.capecommand.mixin;

import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(PlayerInfo.class)
public class PlayerInfoMixin {

    @ModifyArg(method = "createSkinLookup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/SkinManager;createLookup(Lcom/mojang/authlib/GameProfile;Z)Ljava/util/function/Supplier;"))
    private static boolean skinIsAlwaysSigned(boolean requireSecure) {
        return false;
    }
}

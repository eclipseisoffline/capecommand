package xyz.eclipseisoffline.capecommand.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerPlayer.class)
public interface ServerPlayerEntityAccessor {

    @Accessor("lastSentExp")
    void setLastSentExp(int lastSentExp);

    @Accessor("lastSentHealth")
    void setLastSentHealth(float lastSentHealth);

    @Accessor("lastSentFood")
    void setLastSentFood(int lastSentFood);
}

package xyz.eclipseisoffline.capecommand.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerPlayerEntity.class)
public interface ServerPlayerEntityAccessor {

    @Accessor("syncedExperience")
    void setSyncedExperience(int syncedExperience);

    @Accessor("syncedHealth")
    void setSyncedHealth(float syncedHealth);

    @Accessor("syncedFoodLevel")
    void setSyncedFoodLevel(int syncedFoodLevel);
}

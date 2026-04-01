package xyz.eclipseisoffline.capecommand.mixin;

import java.util.List;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import xyz.eclipseisoffline.capecommand.network.ClientboundPlayerInfoUpdatePacketEntriesUpdater;

@Mixin(ClientboundPlayerInfoUpdatePacket.class)
public class ClientboundPlayerInfoUpdatePacketMixin implements ClientboundPlayerInfoUpdatePacketEntriesUpdater {

    @Shadow
    @Final
    @Mutable
    private List<ClientboundPlayerInfoUpdatePacket.Entry> entries;

    @Override
    @Unique
    public void capeCommand$setEntries(List<ClientboundPlayerInfoUpdatePacket.Entry> entries) {
        this.entries = entries;
    }
}

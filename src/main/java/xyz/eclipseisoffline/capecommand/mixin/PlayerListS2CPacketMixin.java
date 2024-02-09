package xyz.eclipseisoffline.capecommand.mixin;

import java.util.List;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Entry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import xyz.eclipseisoffline.capecommand.network.PlayerListS2CPacketEntriesUpdater;

@Mixin(PlayerListS2CPacket.class)
public class PlayerListS2CPacketMixin implements PlayerListS2CPacketEntriesUpdater {

    @Shadow
    @Final
    @Mutable
    private List<Entry> entries;

    @Override
    @Unique
    public void capeCommand$setEntries(List<Entry> entries) {
        this.entries = entries;
    }
}

package xyz.eclipseisoffline.capecommand.network;

import java.util.List;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;

public interface ClientboundPlayerInfoUpdatePacketEntriesUpdater {

    void capeCommand$setEntries(List<ClientboundPlayerInfoUpdatePacket.Entry> entries);
}

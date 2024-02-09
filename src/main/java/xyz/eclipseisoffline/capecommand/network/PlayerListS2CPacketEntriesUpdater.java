package xyz.eclipseisoffline.capecommand.network;

import java.util.List;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Entry;

public interface PlayerListS2CPacketEntriesUpdater {

    void capeCommand$setEntries(List<Entry> entries);
}

package xyz.eclipseisoffline.capecommand.network;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public record CapeCommandInstalledPacket() implements FabricPacket {
    public static final PacketType<CapeCommandInstalledPacket> TYPE = PacketType.create(new Identifier("capecommand", "installed"), CapeCommandInstalledPacket::new);

    public CapeCommandInstalledPacket(PacketByteBuf packetBuffer) {
        this();
    }

    @Override
    public void write(PacketByteBuf packetBuffer) {}

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}

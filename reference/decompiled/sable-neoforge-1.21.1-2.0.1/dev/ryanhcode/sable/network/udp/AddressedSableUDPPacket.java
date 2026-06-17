package dev.ryanhcode.sable.network.udp;

import java.net.InetSocketAddress;

public record AddressedSableUDPPacket(SableUDPPacket packet, InetSocketAddress address) {
}

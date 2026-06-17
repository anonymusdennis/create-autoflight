package dev.ryanhcode.sable.mixinterface.udp;

import io.netty.channel.Channel;

public interface ConnectionExtension {
   void sable$setUDPChannel(Channel var1);

   Channel sable$getUDPChannel();
}

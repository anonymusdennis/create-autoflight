package dev.ryanhcode.sable.mixin.udp;

import dev.ryanhcode.sable.network.udp.SableUDPServer;
import java.util.function.BooleanSupplier;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MinecraftServer.class})
public class MinecraftServerMixin {
   @Unique
   private long sable$lastPingTime = 0L;

   @Inject(
      method = {"tickServer"},
      at = {@At("TAIL")}
   )
   private void sable$keepUdpSocketsAlive(BooleanSupplier booleanSupplier, CallbackInfo ci) {
      SableUDPServer server = SableUDPServer.getServer((MinecraftServer)this);
      if (server != null) {
         long time = System.currentTimeMillis();
         if (time - this.sable$lastPingTime > 2500L) {
            server.sendPings();
            this.sable$lastPingTime = time;
         }
      }
   }
}

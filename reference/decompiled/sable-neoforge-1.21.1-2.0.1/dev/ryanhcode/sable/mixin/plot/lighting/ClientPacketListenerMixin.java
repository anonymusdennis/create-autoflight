package dev.ryanhcode.sable.mixin.plot.lighting;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({ClientPacketListener.class})
public class ClientPacketListenerMixin {
   @WrapOperation(
      method = {"handleLevelChunkWithLight"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/multiplayer/ClientLevel;queueLightUpdate(Ljava/lang/Runnable;)V"
      )}
   )
   private void sable$queueLightData(
      ClientLevel instance, Runnable task, Operation<Void> original, @Local(argsOnly = true) ClientboundLevelChunkWithLightPacket packet
   ) {
      ClientSubLevelContainer container = SubLevelContainer.getContainer(instance);
      if (container.inBounds(packet.getX(), packet.getZ())) {
         task.run();
         instance.getLightEngine().runLightUpdates();
      } else {
         original.call(new Object[]{instance, task});
      }
   }
}

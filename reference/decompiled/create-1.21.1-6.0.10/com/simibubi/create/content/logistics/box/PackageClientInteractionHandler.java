package com.simibubi.create.content.logistics.box;

import com.simibubi.create.foundation.mixin.accessor.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

@EventBusSubscriber({Dist.CLIENT})
public class PackageClientInteractionHandler {
   @SubscribeEvent
   @OnlyIn(Dist.CLIENT)
   public static void onPlayerPunchPackage(AttackEntityEvent event) {
      Player attacker = event.getEntity();
      if (attacker.level().isClientSide()) {
         Minecraft mc = Minecraft.getInstance();
         if (attacker == mc.player) {
            if (event.getTarget() instanceof PackageEntity) {
               ((MinecraftAccessor)mc).create$setMissTime(10);
            }
         }
      }
   }
}

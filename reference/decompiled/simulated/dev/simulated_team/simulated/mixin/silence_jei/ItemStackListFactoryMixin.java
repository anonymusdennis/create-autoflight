package dev.simulated_team.simulated.mixin.silence_jei;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.simulated_team.simulated.service.SimTabService;
import mezz.jei.library.plugins.vanilla.ingredients.ItemStackListFactory;
import net.minecraft.world.item.CreativeModeTab;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin({ItemStackListFactory.class})
public class ItemStackListFactoryMixin {
   @WrapOperation(
      method = {"create"},
      at = {@At(
         value = "INVOKE",
         target = "Lorg/apache/logging/log4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V",
         ordinal = 0
      )}
   )
   private static void simulated$error(Logger instance, String string, Object o, Object o1, Operation<Void> original, @Local CreativeModeTab tab) {
      if (tab != SimTabService.INSTANCE.getCreativeTab()) {
         original.call(new Object[]{instance, string, o, o1});
      }
   }

   @WrapOperation(
      method = {"addFromTab"},
      at = {@At(
         value = "INVOKE",
         target = "Lorg/apache/logging/log4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V",
         ordinal = 0
      )}
   )
   private static void simulated$error(
      Logger instance, String string, Object o, Object o1, Object o2, Operation<Void> original, @Local(argsOnly = true) CreativeModeTab tab
   ) {
      if (tab != SimTabService.INSTANCE.getCreativeTab()) {
         original.call(new Object[]{instance, string, o, o1, o2});
      }
   }
}

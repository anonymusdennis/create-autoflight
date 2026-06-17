package dev.simulated_team.simulated.mixin.dynamic_stress;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipHelper;
import dev.simulated_team.simulated.api.CustomStressImpactTooltipProvider;
import dev.simulated_team.simulated.data.SimLang;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({KineticStats.class})
public class KineticStatsMixin {
   @WrapOperation(
      method = {"getKineticStats"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/createmod/catnip/lang/LangBuilder;add(Lnet/createmod/catnip/lang/LangBuilder;)Lnet/createmod/catnip/lang/LangBuilder;",
         ordinal = 0
      )},
      remap = false
   )
   private static LangBuilder aeronautics$getKinetidStats(
      LangBuilder instance,
      LangBuilder otherBuilder,
      Operation<LangBuilder> original,
      @Local(argsOnly = true) Block block,
      @Local(name = {"impactId"}) StressImpact impactId
   ) {
      return block instanceof CustomStressImpactTooltipProvider impact
         ? instance.add(SimLang.text(TooltipHelper.makeProgressBar(impact.getBarLength(), impact.getFilledBarLength()))).style(impactId.getAbsoluteColor())
         : instance.add(otherBuilder);
   }

   @WrapOperation(
      method = {"getKineticStats"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/createmod/catnip/lang/LangBuilder;add(Lnet/createmod/catnip/lang/LangBuilder;)Lnet/createmod/catnip/lang/LangBuilder;",
         ordinal = 2
      )},
      remap = false
   )
   private static LangBuilder aeronautics$getKinetidStats2(
      LangBuilder instance, LangBuilder otherBuilder, Operation<LangBuilder> original, @Local(argsOnly = true) Block block
   ) {
      return block instanceof CustomStressImpactTooltipProvider impact
         ? instance.add(otherBuilder.text(" x ")).add(impact.getCustomImpactLang())
         : instance.add(otherBuilder);
   }

   @WrapOperation(
      method = {"getKineticStats"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/createmod/catnip/lang/LangBuilder;translate(Ljava/lang/String;[Ljava/lang/Object;)Lnet/createmod/catnip/lang/LangBuilder;",
         ordinal = 0
      )},
      remap = false
   )
   private static LangBuilder aeronautics$getKinetidStats4(
      LangBuilder instance,
      String langKey,
      Object[] args,
      Operation<LangBuilder> original,
      @Local(argsOnly = true) Block block,
      @Local(name = {"impactId"}) StressImpact impactId
   ) {
      return block instanceof CustomStressImpactTooltipProvider
         ? SimLang.space().translate("tooltip.dynamic_stress_impact", new Object[0]).style(impactId.getAbsoluteColor())
         : instance.translate(langKey, args);
   }
}

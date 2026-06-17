package dev.eriksonn.aeronautics.mixin.steam_vent;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.simibubi.create.content.fluids.tank.BoilerData;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.eriksonn.aeronautics.data.AeroLang;
import dev.eriksonn.aeronautics.index.AeroBlocks;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BoilerData.class})
public abstract class BoilerDataMixin {
   @Shadow
   public int attachedEngines;
   @Shadow
   @Final
   private static float passiveEngineEfficiency;
   @Unique
   private int aeronautics$attachedVents = 0;

   @Shadow
   public abstract float getEngineEfficiency(int var1);

   @Shadow
   public abstract boolean isPassive(int var1);

   @Inject(
      method = {"evaluate"},
      at = {@At("HEAD")}
   )
   private void aeronautics$countVents1(FluidTankBlockEntity controller, CallbackInfoReturnable<Boolean> cir, @Share("prevVents") LocalIntRef prevVents) {
      prevVents.set(this.aeronautics$attachedVents);
      this.aeronautics$attachedVents = 0;
   }

   @Inject(
      method = {"evaluate"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
         ordinal = 0
      )}
   )
   private void aeronautics$countVents2(FluidTankBlockEntity controller, CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 1) BlockState attachedState) {
      if (AeroBlocks.STEAM_VENT.has(attachedState)) {
         this.aeronautics$attachedVents++;
      }
   }

   @ModifyReturnValue(
      method = {"evaluate"},
      at = {@At("RETURN")}
   )
   private boolean aeronautics$countVents3(boolean original, @Share("prevVents") LocalIntRef prevVents) {
      return original || this.aeronautics$attachedVents != prevVents.get();
   }

   @ModifyReturnValue(
      method = {"isActive"},
      at = {@At("RETURN")}
   )
   private boolean aeronautics$activeWithVents(boolean original) {
      return original || this.aeronautics$attachedVents > 0;
   }

   @ModifyExpressionValue(
      method = {"getEngineEfficiency", "updateOcclusion"},
      at = {@At(
         value = "FIELD",
         target = "Lcom/simibubi/create/content/fluids/tank/BoilerData;attachedEngines:I"
      )}
   )
   private int aeronautics$ventEfficiency(int original) {
      return original + this.aeronautics$attachedVents;
   }

   @ModifyExpressionValue(
      method = {"addToGoggleTooltip"},
      at = {@At(
         value = "FIELD",
         target = "Lcom/simibubi/create/content/fluids/tank/BoilerData;attachedEngines:I",
         ordinal = 0
      ), @At(
         value = "FIELD",
         target = "Lcom/simibubi/create/content/fluids/tank/BoilerData;attachedEngines:I",
         ordinal = 2
      )}
   )
   private int aeronautics$countSteamConsumer1(int original) {
      return original + this.aeronautics$attachedVents;
   }

   @Redirect(
      method = {"addToGoggleTooltip"},
      at = @At(
         value = "INVOKE",
         target = "Lcom/simibubi/create/content/fluids/tank/BoilerData;getEngineEfficiency(I)F"
      )
   )
   private float aeronautics$trueMaxSU1(BoilerData instance, int boilerSize) {
      return this.isPassive(boilerSize) ? passiveEngineEfficiency : 1.0F;
   }

   @Redirect(
      method = {"addToGoggleTooltip"},
      at = @At(
         value = "INVOKE",
         target = "Ljava/lang/Math;max(II)I"
      )
   )
   private int aeronautics$trueMaxSU2(int boilerLevel, int attachedEngines) {
      return Math.max(boilerLevel, 1);
   }

   @Inject(
      method = {"addToGoggleTooltip"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/createmod/catnip/lang/LangBuilder;forGoggles(Ljava/util/List;)V",
         shift = Shift.AFTER,
         ordinal = 2
      )},
      cancellable = true
   )
   private void aeronautics$reformatBoilerTooltip(
      List<Component> tooltip,
      boolean isPlayerSneaking,
      int boilerSize,
      CallbackInfoReturnable<Boolean> cir,
      @Local double totalSU,
      @Local(ordinal = 1) int boilerLevel
   ) {
      CreateLang.number(totalSU).translate("generic.unit.stress", new Object[0]).style(ChatFormatting.AQUA).forGoggles(tooltip, 1);
      boilerLevel = Math.max(boilerLevel, 1);
      float efficiency = this.isPassive(boilerSize) ? this.getEngineEfficiency(boilerSize) / passiveEngineEfficiency : this.getEngineEfficiency(boilerSize);
      double engineSU = totalSU * (double)efficiency * (double)this.attachedEngines / (double)boilerLevel;
      double ventSU = totalSU * (double)efficiency * (double)this.aeronautics$attachedVents / (double)boilerLevel;
      if (engineSU > 0.0 || ventSU > 0.0) {
         AeroLang.translate("tooltip.capacity_used").style(ChatFormatting.GRAY).forGoggles(tooltip);
      }

      if (engineSU > 0.0) {
         CreateLang.number(engineSU)
            .translate("generic.unit.stress", new Object[0])
            .style(ChatFormatting.AQUA)
            .space()
            .add(
               (this.attachedEngines == 1
                     ? CreateLang.translate("boiler.via_one_engine", new Object[0])
                     : CreateLang.translate("boiler.via_engines", new Object[]{this.attachedEngines}))
                  .style(ChatFormatting.DARK_GRAY)
            )
            .forGoggles(tooltip, 1);
      }

      if (ventSU > 0.0) {
         CreateLang.number(ventSU)
            .translate("generic.unit.stress", new Object[0])
            .style(ChatFormatting.AQUA)
            .space()
            .add(
               (this.aeronautics$attachedVents == 1
                     ? AeroLang.translate("boiler.via_one_vent")
                     : AeroLang.translate("boiler.via_vents", this.aeronautics$attachedVents))
                  .style(ChatFormatting.DARK_GRAY)
            )
            .forGoggles(tooltip, 1);
      }

      cir.setReturnValue(true);
   }

   @Inject(
      method = {"clear"},
      at = {@At("TAIL")}
   )
   private void aeronautics$clearVents(CallbackInfo ci) {
      this.aeronautics$attachedVents = 0;
   }

   @Inject(
      method = {"write"},
      at = {@At("TAIL")}
   )
   private void aeronautics$writeVentData(CallbackInfoReturnable<CompoundTag> cir, @Local CompoundTag nbt) {
      nbt.putInt("SimVents", this.aeronautics$attachedVents);
   }

   @Inject(
      method = {"read"},
      at = {@At("TAIL")}
   )
   private void aeronautics$readVentData(CompoundTag nbt, int boilerSize, CallbackInfo ci) {
      this.aeronautics$attachedVents = nbt.getInt("SimVents");
   }
}

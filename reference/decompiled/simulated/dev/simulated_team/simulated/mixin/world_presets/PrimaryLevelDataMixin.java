package dev.simulated_team.simulated.mixin.world_presets;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import dev.simulated_team.simulated.mixin_interface.PrimaryLevelDataExtension;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.dimension.end.EndDragonFight.Data;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.PrimaryLevelData.SpecialWorldProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PrimaryLevelData.class})
public class PrimaryLevelDataMixin implements PrimaryLevelDataExtension {
   @Unique
   private static final String simulated$WORLD_PRESET_KEY = "simulated:world_preset";
   @Shadow
   private Data endDragonFightData;
   private ResourceLocation simulated$worldPresetKey = WorldPresets.NORMAL.location();

   @Inject(
      method = {"parse"},
      at = {@At("RETURN")},
      remap = false
   )
   private static <T> void simulated$parse(
      Dynamic<T> dynamic,
      LevelSettings levelSettings,
      SpecialWorldProperty specialWorldProperty,
      WorldOptions worldOptions,
      Lifecycle lifecycle,
      CallbackInfoReturnable<PrimaryLevelData> cir
   ) {
      DataResult<String> string = dynamic.get("simulated:world_preset").asString();
      if (string.isSuccess()) {
         ((PrimaryLevelDataExtension)cir.getReturnValue()).setPreset(ResourceLocation.parse((String)string.getOrThrow()));
      }
   }

   @Inject(
      method = {"setTagData"},
      at = {@At("TAIL")},
      remap = false
   )
   private void simulated$setTagData(RegistryAccess registryAccess, CompoundTag compoundTag, CompoundTag compoundTag2, CallbackInfo ci) {
      compoundTag.putString("simulated:world_preset", this.getPreset().toString());
   }

   @Override
   public ResourceLocation getPreset() {
      return this.simulated$worldPresetKey;
   }

   @Override
   public void setPreset(ResourceLocation resourceLocation) {
      this.simulated$worldPresetKey = resourceLocation;
   }

   @Override
   public void setEndDragonFight(Data endDragonFight) {
      this.endDragonFightData = endDragonFight;
   }
}

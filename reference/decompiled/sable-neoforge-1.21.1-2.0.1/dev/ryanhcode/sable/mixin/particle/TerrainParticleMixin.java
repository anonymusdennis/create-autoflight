package dev.ryanhcode.sable.mixin.particle;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({TerrainParticle.class})
public abstract class TerrainParticleMixin extends Particle {
   @Shadow
   @Final
   private BlockPos pos;

   protected TerrainParticleMixin(ClientLevel clientLevel, double d, double e, double f) {
      super(clientLevel, d, e, f);
   }

   @Redirect(
      method = {"getLightColor"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/LevelRenderer;getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I"
      )
   )
   private int sable$getLightColor(BlockAndTintGetter blockAndTintGetter, BlockPos blockPos, @Local int existingColor) {
      ClientSubLevelContainer container = SubLevelContainer.getContainer(Minecraft.getInstance().level);

      assert container != null;

      SubLevel subLevel = Sable.HELPER.getContainingClient(this.pos);
      if (subLevel instanceof ClientSubLevel clientSubLevel) {
         int color = LevelRenderer.getLightColor(blockAndTintGetter, blockPos);
         return clientSubLevel.scaleLightColor(color);
      } else {
         return container.inBounds(blockPos) ? existingColor : LevelRenderer.getLightColor(blockAndTintGetter, blockPos);
      }
   }
}

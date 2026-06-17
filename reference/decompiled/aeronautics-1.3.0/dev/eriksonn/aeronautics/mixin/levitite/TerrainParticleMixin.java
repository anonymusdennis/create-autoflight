package dev.eriksonn.aeronautics.mixin.levitite;

import dev.eriksonn.aeronautics.index.AeroTags;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({TerrainParticle.class})
public abstract class TerrainParticleMixin extends Particle {
   @Unique
   private float aeronautics$extraYFriction = 1.0F;

   protected TerrainParticleMixin(ClientLevel level, double x, double y, double z) {
      super(level, x, y, z);
   }

   @Inject(
      method = {"<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDDLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V"},
      at = {@At("TAIL")}
   )
   private void levititeParticles(
      ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, BlockState state, BlockPos pos, CallbackInfo ci
   ) {
      if (state.is(AeroTags.BlockTags.LEVITITE)) {
         this.gravity = 0.0F;
         this.xd *= 0.5;
         this.yd *= 0.5;
         this.zd *= 0.5;
         this.friction = 0.9F;
         this.aeronautics$extraYFriction = 0.99F;
      }
   }

   public void tick() {
      super.tick();
      this.yd = this.yd * (double)this.aeronautics$extraYFriction;
   }
}

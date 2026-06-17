package com.simibubi.create.content.fluids.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.processing.basin.BasinBlock;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Quaternionf;

public class BasinFluidParticle extends FluidStackParticle {
   BlockPos basinPos;
   Vec3 targetPos;
   Vec3 centerOfBasin;
   float yOffset;

   public BasinFluidParticle(ClientLevel world, FluidStack fluid, double x, double y, double z, double vx, double vy, double vz) {
      super(world, fluid, x, y, z, vx, vy, vz);
      this.gravity = 0.0F;
      this.xd = 0.0;
      this.yd = 0.0;
      this.zd = 0.0;
      this.yOffset = world.random.nextFloat() * 1.0F / 32.0F;
      y += (double)this.yOffset;
      this.quadSize = 0.0F;
      this.lifetime = 60;
      Vec3 currentPos = new Vec3(x, y, z);
      this.basinPos = BlockPos.containing(currentPos);
      this.centerOfBasin = VecHelper.getCenterOf(this.basinPos);
      if (vx != 0.0) {
         this.lifetime = 20;
         Vec3 centerOf = VecHelper.getCenterOf(this.basinPos);
         Vec3 diff = currentPos.subtract(centerOf).multiply(1.0, 0.0, 1.0).normalize().scale(0.375);
         this.targetPos = centerOf.add(diff);
         x = this.centerOfBasin.x;
         this.xo = this.centerOfBasin.x;
         z = this.centerOfBasin.z;
         this.zo = this.centerOfBasin.z;
      }
   }

   @Override
   public void tick() {
      super.tick();
      this.quadSize = this.targetPos != null
         ? Math.max(0.03125F, 1.0F * (float)this.age / (float)this.lifetime / 8.0F)
         : 0.125F * (1.0F - (float)Math.abs(this.age - this.lifetime / 2) / (1.0F * (float)this.lifetime));
      if (this.age % 2 == 0) {
         if (!AllBlocks.BASIN.has(this.level.getBlockState(this.basinPos)) && !BasinBlock.isBasin(this.level, this.basinPos)) {
            this.remove();
            return;
         }

         BlockEntity blockEntity = this.level.getBlockEntity(this.basinPos);
         if (blockEntity instanceof BasinBlockEntity) {
            float totalUnits = ((BasinBlockEntity)blockEntity).getTotalFluidUnits(0.0F);
            if (totalUnits < 1.0F) {
               totalUnits = 0.0F;
            }

            float fluidLevel = Mth.clamp(totalUnits / 2000.0F, 0.0F, 1.0F);
            this.y = (double)(0.125F + (float)this.basinPos.getY() + 0.75F * fluidLevel + this.yOffset);
         }
      }

      if (this.targetPos != null) {
         float progess = 1.0F * (float)this.age / (float)this.lifetime;
         Vec3 currentPos = this.centerOfBasin.add(this.targetPos.subtract(this.centerOfBasin).scale((double)progess));
         this.x = currentPos.x;
         this.z = currentPos.z;
      }
   }

   public void render(VertexConsumer vb, Camera info, float pt) {
      Quaternionf rotation = info.rotation();
      Quaternionf prevRotation = new Quaternionf(rotation);
      rotation.set(-1.0F, 0.0F, 0.0F, 1.0F);
      rotation.normalize();
      super.render(vb, info, pt);
      rotation.set(0.0F, 0.0F, 0.0F, 1.0F);
      rotation.mul(prevRotation);
   }

   @Override
   protected boolean canEvaporate() {
      return false;
   }
}

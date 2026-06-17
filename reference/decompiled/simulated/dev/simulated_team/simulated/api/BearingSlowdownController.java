package dev.simulated_team.simulated.api;

import com.simibubi.create.content.contraptions.Contraption;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public class BearingSlowdownController {
   public static final float TIMER_SCALE = 3.5F;
   public static final float SMOOTHING_FACTOR = 2.5F;
   private float maxTime;
   private float initialVelocity;
   private float clampedTime;
   private float countdown;
   private float initialAngle;
   private BearingSlowdownController.ContraptionSymmetry symmetry;
   private float linearDistance;
   private float scaledOffset;
   private float targetStoppingPoint;
   private float angleOffset;

   public void generate(float maxTime, float initialAngle, float initialVelocity, Direction facingDirection, Contraption attachedContraption) {
      this.generate(maxTime, initialAngle, initialVelocity, this.getSymmetry(facingDirection, attachedContraption));
   }

   public void generate(float maxTime, float initialAngle, float initialVelocity, BearingSlowdownController.ContraptionSymmetry symmetry) {
      this.maxTime = maxTime;
      this.maxTime = Math.min(this.maxTime, 30.0F);
      this.symmetry = symmetry;
      this.initialAngle = initialAngle;
      this.initialVelocity = initialVelocity;
      this.maxTime = this.getMaxTime() * (float)Math.sqrt((double)(this.symmetry.getAngle() / 90.0F));
      this.generateConstants();
      this.applyVelocityClamping();
      this.countdown = this.clampedTime;
   }

   BearingSlowdownController.ContraptionSymmetry getSymmetry(Direction facingDirection, Contraption attachedContraption) {
      boolean halfSymmetry = true;
      boolean quarterSymmetry = true;
      Map<BlockPos, StructureBlockInfo> Blocks = attachedContraption.getBlocks();

      for (Entry<BlockPos, StructureBlockInfo> entry : Blocks.entrySet()) {
         Block current = entry.getValue().state().getBlock();
         BlockPos R1 = this.Rotate90(entry.getKey(), facingDirection);
         BlockPos R2 = this.Rotate90(R1, facingDirection);
         if (!Blocks.containsKey(R1) || !Blocks.get(R1).state().getBlock().equals(current)) {
            quarterSymmetry = false;
         }

         if (!Blocks.containsKey(R2) || !Blocks.get(R2).state().getBlock().equals(current)) {
            halfSymmetry = false;
         }
      }

      if (quarterSymmetry) {
         return BearingSlowdownController.ContraptionSymmetry.QUARTER;
      } else {
         return halfSymmetry ? BearingSlowdownController.ContraptionSymmetry.HALF : BearingSlowdownController.ContraptionSymmetry.NONE;
      }
   }

   private void generateConstants() {
      float symmetryAngle = this.symmetry.getAngle();
      this.linearDistance = this.getMaxTime() * this.getInitialVelocity();
      float optimalStoppingPoint = this.initialAngle + this.linearDistance / 2.5F;
      this.targetStoppingPoint = symmetryAngle * (float)Math.round(optimalStoppingPoint / symmetryAngle);
      this.angleOffset = this.initialAngle - this.targetStoppingPoint;
      this.scaledOffset = this.angleOffset * 2.5F + this.linearDistance;
   }

   private void applyVelocityClamping() {
      float maxVelocity = this.getMaxVelocity();
      float targetVelocityClamp = 120.0F / this.getMaxTime();
      if (maxVelocity < targetVelocityClamp) {
         this.clampedTime = this.getMaxTime() * maxVelocity / targetVelocityClamp;
         this.clampedTime = Math.max(this.clampedTime, 2.0F);
         this.linearDistance = this.clampedTime * this.getInitialVelocity();
      } else {
         this.clampedTime = this.getMaxTime();
      }
   }

   private float getMaxVelocity() {
      float A = 3.5F * this.angleOffset + this.linearDistance;
      float B = 3.5F * (2.5F * this.angleOffset + this.linearDistance);
      float normalizedTimeAtMaxVelocity = (A + this.linearDistance) / B;
      float maxVelocity = Math.abs(this.getInitialVelocity());
      if ((double)Math.abs(B) > 0.001 && normalizedTimeAtMaxVelocity > 0.0F && normalizedTimeAtMaxVelocity < 1.0F) {
         float estimatedTurnaroundVelocity = -A * (float)Math.pow((double)(1.5F * A / B), 1.5) / this.getMaxTime();
         estimatedTurnaroundVelocity = Math.abs(estimatedTurnaroundVelocity);
         maxVelocity = Math.max(estimatedTurnaroundVelocity, maxVelocity);
      }

      return maxVelocity;
   }

   private BlockPos Rotate90(BlockPos pos, Direction dir) {
      int x1 = pos.getX();
      int y1 = pos.getY();
      int z1 = pos.getZ();
      int x2 = dir.getStepX();
      int y2 = dir.getStepY();
      int z2 = dir.getStepZ();
      int dotProduct = x1 * x2 + y1 * y2 + z1 * z2;
      return new BlockPos(y1 * z2 - z1 * y2 + dotProduct * x2, z1 * x2 - x1 * z2 + dotProduct * y2, x1 * y2 - y1 * x2 + dotProduct * z2);
   }

   public float getTime() {
      return this.clampedTime - this.countdown;
   }

   public float getCountdown() {
      return this.countdown;
   }

   public boolean stepGoal() {
      if (this.countdown > 0.0F) {
         this.countdown--;
         if ((double)this.countdown <= 0.5) {
            this.countdown = 0.0F;
            return true;
         }
      }

      return false;
   }

   public float getAngle(float partialTick) {
      float time = this.getTime() + partialTick;
      if (time >= this.clampedTime) {
         return this.targetStoppingPoint;
      } else {
         float normalizedTime = time / this.clampedTime;
         float lerpParameter = (float)Math.pow((double)(1.0F - normalizedTime), 2.5);
         return this.targetStoppingPoint + (this.angleOffset + normalizedTime * this.scaledOffset) * lerpParameter;
      }
   }

   public float getSpeed(float partialTick) {
      float time = this.getTime() + partialTick;
      if (time >= this.clampedTime) {
         return 0.0F;
      } else {
         float normalizedTime = time / this.clampedTime;
         float lerpDerivative = (float)Math.pow((double)(1.0F - normalizedTime), 1.5);
         return (this.linearDistance - 3.5F * normalizedTime * this.scaledOffset) * lerpDerivative / this.clampedTime;
      }
   }

   public void deserializeFromNBT(CompoundTag nbt) {
      this.countdown = nbt.getFloat("CurrentTime");
      this.maxTime = nbt.getFloat("DisassemblyTimerTotal");
      this.symmetry = BearingSlowdownController.ContraptionSymmetry.values()[nbt.getInt("Symmetry")];
      this.initialAngle = nbt.getFloat("InitialSlowdownAngle");
      this.initialVelocity = nbt.getFloat("InitialSlowdownVelocity");
      this.generateConstants();
      this.applyVelocityClamping();
   }

   public void serializeIntoNBT(CompoundTag nbt) {
      nbt.putFloat("CurrentTime", this.countdown);
      nbt.putFloat("DisassemblyTimerTotal", this.getMaxTime());
      nbt.putInt("Symmetry", this.symmetry.ordinal());
      nbt.putFloat("InitialSlowdownAngle", this.initialAngle);
      nbt.putFloat("InitialSlowdownVelocity", this.getInitialVelocity());
   }

   public float getMaxTime() {
      return this.maxTime;
   }

   public float getInitialVelocity() {
      return this.initialVelocity;
   }

   public static enum ContraptionSymmetry {
      NONE(360.0F),
      HALF(180.0F),
      QUARTER(90.0F);

      final float angle;

      private ContraptionSymmetry(float angle) {
         this.angle = angle;
      }

      public float getAngle() {
         return this.angle;
      }
   }
}

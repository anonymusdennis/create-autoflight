package dev.eriksonn.aeronautics.content.ponder.instructions;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlockEntity;
import dev.simulated_team.simulated.api.BearingSlowdownController;
import java.util.ArrayList;
import java.util.List;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class PropellerRotateInstruction extends PonderInstruction {
   BlockPos pos;
   List<ElementLink<WorldSectionElement>> contraptions;
   long activatedContraptionsIndex = 0L;
   final Direction direction;
   final Vec3 normal;
   float lastAngle;
   float targetSpeed;
   final float originalSails;
   final float originalSpeed;
   float currentSpeed;
   float sailSmoothingAmount;
   boolean stopped;
   BearingSlowdownController slowdownController = null;
   PropellerParticleSpawningInstruction.ParticleSpawner spawner = null;
   float particleSpeedScale;
   float particleAmountScale;

   public PropellerRotateInstruction(
      BlockPos pos, ElementLink<WorldSectionElement> contraption, Direction direction, float targetSpeed, float sailSmoothingAmount
   ) {
      this.pos = pos;
      this.contraptions = new ArrayList<>();
      this.contraptions.add(contraption);
      this.direction = direction;
      this.originalSpeed = this.targetSpeed = targetSpeed;
      this.originalSails = this.sailSmoothingAmount = sailSmoothingAmount;
      Vec3i n = direction.getNormal();
      this.normal = new Vec3((double)Math.abs(n.getX()), (double)Math.abs(n.getY()), (double)Math.abs(n.getZ()));
   }

   public void reset(PonderScene scene) {
      super.reset(scene);
      this.sailSmoothingAmount = this.originalSails;
      this.targetSpeed = this.originalSpeed;
      this.currentSpeed = 0.0F;
      this.lastAngle = 0.0F;
      this.slowdownController = null;
      this.stopped = false;
   }

   public boolean isComplete() {
      return this.stopped;
   }

   public void tick(PonderScene scene) {
      if (scene.getWorld().getBlockEntity(this.pos) instanceof PropellerBearingBlockEntity bearing) {
         float angle = bearing.getInterpolatedAngle(0.0F);
         if (this.slowdownController != null) {
            this.stopped = this.slowdownController.stepGoal();
            this.currentSpeed = this.slowdownController.getSpeed(0.0F);
            angle = this.slowdownController.getAngle(0.0F);
         } else {
            this.currentSpeed = Mth.lerp(
               0.4F / (float)Math.sqrt((double)this.sailSmoothingAmount), this.currentSpeed, KineticBlockEntity.convertToAngular(this.targetSpeed)
            );
            angle += this.currentSpeed;
         }

         if (this.spawner != null) {
            this.spawner.particleSpeed = this.currentSpeed * this.particleSpeedScale / 200.0F;
            this.spawner.particleAmount = Math.abs(this.currentSpeed) * this.particleAmountScale / 10.0F;
            this.spawner.tick(scene);
         }

         for (ElementLink<WorldSectionElement> contraption : this.contraptions) {
            WorldSectionElement link = (WorldSectionElement)scene.resolve(contraption);
            if (link != null) {
               this.updateLinkAngle(link, angle, false);
            }
         }

         bearing.setAngle(angle);
      }
   }

   public void addSection(PonderScene scene, ElementLink<WorldSectionElement> section) {
      this.contraptions.add(section);
      if (scene.getWorld().getBlockEntity(this.pos) instanceof PropellerBearingBlockEntity bearing) {
         float angle = bearing.getInterpolatedAngle(0.0F);
         WorldSectionElement link = (WorldSectionElement)scene.resolve(section);
         if (link != null) {
            this.updateLinkAngle(link, angle, true);
         }
      }
   }

   void updateLinkAngle(WorldSectionElement link, float angle, boolean forced) {
      Vec3 v = link.getAnimatedRotation();
      double d = v.dot(this.normal);
      link.setAnimatedRotation(v.add(this.normal.scale((double)angle - d)), forced);
   }
}

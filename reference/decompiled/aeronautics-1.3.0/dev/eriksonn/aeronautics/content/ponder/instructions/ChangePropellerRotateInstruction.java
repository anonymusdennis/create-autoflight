package dev.eriksonn.aeronautics.content.ponder.instructions;

import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlockEntity;
import dev.simulated_team.simulated.api.BearingSlowdownController;
import dev.simulated_team.simulated.api.BearingSlowdownController.ContraptionSymmetry;
import java.util.function.BiConsumer;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public abstract class ChangePropellerRotateInstruction extends PonderInstruction {
   private final PropellerRotateInstruction instruction;
   private final BiConsumer<PropellerRotateInstruction, PonderScene> consumer;

   ChangePropellerRotateInstruction(PropellerRotateInstruction instruction, BiConsumer<PropellerRotateInstruction, PonderScene> consumer) {
      this.instruction = instruction;
      this.consumer = consumer;
   }

   public boolean isComplete() {
      return true;
   }

   public void tick(PonderScene scene) {
      this.consumer.accept(this.instruction, scene);
   }

   public static class AddSection extends ChangePropellerRotateInstruction {
      public AddSection(PropellerRotateInstruction instruction, ElementLink<WorldSectionElement> link) {
         super(instruction, (i, s) -> i.addSection(s, link));
      }
   }

   public static class SetParticles extends ChangePropellerRotateInstruction {
      public SetParticles(
         PropellerRotateInstruction instruction,
         @Nullable ElementLink<WorldSectionElement> link,
         float particleAmount,
         float particleSpeed,
         float radius,
         boolean hasCollision
      ) {
         super(
            instruction,
            (i, s) -> {
               i.particleSpeedScale = particleSpeed;
               i.particleAmountScale = particleAmount;
               i.spawner = new PropellerParticleSpawningInstruction.ParticleSpawner(
                  link, i.pos.offset(i.direction.getNormal()), i.direction, 0.0F, 0.0F, radius, hasCollision
               );
            }
         );
      }

      public SetParticles(
         PropellerRotateInstruction instruction,
         BlockPos location,
         @Nullable ElementLink<WorldSectionElement> link,
         float particleAmount,
         float particleSpeed,
         float radius,
         boolean hasCollision
      ) {
         super(instruction, (i, s) -> {
            i.particleSpeedScale = particleSpeed;
            i.particleAmountScale = particleAmount;
            i.spawner = new PropellerParticleSpawningInstruction.ParticleSpawner(link, location, i.direction, 0.0F, 0.0F, radius, hasCollision);
         });
      }
   }

   public static class SetRotationRate extends ChangePropellerRotateInstruction {
      public SetRotationRate(PropellerRotateInstruction instruction, float targetSpeed) {
         super(instruction, (i, s) -> i.targetSpeed = targetSpeed);
      }
   }

   public static class StopParticles extends ChangePropellerRotateInstruction {
      public StopParticles(PropellerRotateInstruction instruction) {
         super(instruction, (i, s) -> i.spawner = null);
      }
   }

   public static class StopRotation extends ChangePropellerRotateInstruction {
      public StopRotation(PropellerRotateInstruction instruction, float duration) {
         super(instruction, (i, s) -> {
            if (s.getWorld().getBlockEntity(i.pos) instanceof PropellerBearingBlockEntity bearing) {
               float angle = bearing.getInterpolatedAngle(0.0F);
               i.slowdownController = new BearingSlowdownController();
               i.slowdownController.generate(duration, angle, i.currentSpeed, ContraptionSymmetry.QUARTER);
            }
         });
      }
   }
}

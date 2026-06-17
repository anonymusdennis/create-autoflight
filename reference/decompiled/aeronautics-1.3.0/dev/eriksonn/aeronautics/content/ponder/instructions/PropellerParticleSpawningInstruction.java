package dev.eriksonn.aeronautics.content.ponder.instructions;

import com.mojang.math.Axis;
import dev.eriksonn.aeronautics.content.particle.PropellerAirParticleData;
import dev.simulated_team.simulated.mixin.accessor.WorldSectionElementAccessor;
import dev.simulated_team.simulated.util.SimMathUtils;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

public class PropellerParticleSpawningInstruction extends TickingInstruction {
   PropellerParticleSpawningInstruction.ParticleSpawner spawner;

   public PropellerParticleSpawningInstruction(
      @Nullable ElementLink<WorldSectionElement> link,
      BlockPos location,
      Direction direction,
      int ticks,
      float particleAmount,
      float particleSpeed,
      float radius
   ) {
      this(link, location, direction, ticks, particleAmount, particleSpeed, radius, false);
   }

   public PropellerParticleSpawningInstruction(
      @Nullable ElementLink<WorldSectionElement> link,
      BlockPos location,
      Direction direction,
      int ticks,
      float particleAmount,
      float particleSpeed,
      float radius,
      boolean hasCollision
   ) {
      super(false, ticks);
      this.spawner = new PropellerParticleSpawningInstruction.ParticleSpawner(link, location, direction, particleAmount, particleSpeed, radius, hasCollision);
   }

   public void tick(PonderScene scene) {
      super.tick(scene);
      this.spawner.tick(scene);
   }

   public static class ParticleSpawner {
      protected final BlockPos location;
      protected ElementLink<WorldSectionElement> link;
      protected float radius;
      protected final Quaternionf rot = new Quaternionf();
      protected float particleAmount;
      protected float particleSpeed;
      protected boolean hasCollision;

      ParticleSpawner(
         @Nullable ElementLink<WorldSectionElement> link,
         BlockPos location,
         Direction direction,
         float particleAmount,
         float particleSpeed,
         float radius,
         boolean hasCollision
      ) {
         this.link = link;
         this.location = location;
         this.hasCollision = hasCollision;
         this.radius = radius;
         this.particleAmount = particleAmount;
         this.particleSpeed = particleSpeed / 20.0F;
         if (direction.getAxis().isHorizontal()) {
            this.rot.set(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(direction.getOpposite())));
         }

         this.rot.mul(Axis.XP.rotationDegrees(-90.0F - AngleHelper.verticalAngle(direction)));
      }

      void tick(PonderScene scene) {
         PonderLevel level = scene.getWorld();
         float particleCount = this.particleAmount + level.random.nextFloat() - 1.0F;
         Vec3 totalOffset = VecHelper.getCenterOf(this.location);
         Quaternionf elementRot = new Quaternionf();
         if (this.link != null) {
            WorldSectionElement element = (WorldSectionElement)scene.resolve(this.link);
            if (element != null) {
               Vec3 elementOffset = element.getAnimatedOffset();
               Vec3 rotation = new Vec3(
                  Math.toRadians(element.getAnimatedRotation().x),
                  Math.toRadians(element.getAnimatedRotation().y),
                  Math.toRadians(element.getAnimatedRotation().z)
               );
               elementRot.mul(new Quaternionf((float)Math.sin(rotation.x / 2.0), 0.0F, 0.0F, (float)Math.cos(rotation.x / 2.0)));
               elementRot.mul(new Quaternionf(0.0F, 0.0F, (float)Math.sin(rotation.z / 2.0), (float)Math.cos(rotation.z / 2.0)));
               elementRot.mul(new Quaternionf(0.0F, (float)Math.sin(rotation.y / 2.0), 0.0F, (float)Math.cos(rotation.y / 2.0)));
               totalOffset = totalOffset.subtract(((WorldSectionElementAccessor)element).getCenterOfRotation());
               totalOffset = SimMathUtils.rotateQuatReverse(totalOffset, elementRot);
               totalOffset = totalOffset.add(((WorldSectionElementAccessor)element).getCenterOfRotation());
               totalOffset = totalOffset.add(elementOffset);
            }
         }

         for (int i = 0; (float)i < particleCount; i++) {
            double R = (double)this.radius * Math.sqrt((double)level.random.nextFloat());
            double angle = (Math.PI * 2) * (double)level.random.nextFloat();
            Vec3 randomOffset = VecHelper.offsetRandomly(Vec3.ZERO, RandomSource.create(), 0.5F);
            randomOffset = new Vec3(randomOffset.x, 0.0, randomOffset.z);
            Vec3 particlePos = new Vec3(Math.cos(angle) * R, 0.0, Math.sin(angle) * R).add(randomOffset);
            Vec3 speedVector = new Vec3(0.0, (double)this.particleSpeed, 0.0);
            particlePos = SimMathUtils.rotateQuatReverse(particlePos, this.rot);
            particlePos = SimMathUtils.rotateQuatReverse(particlePos, elementRot);
            speedVector = SimMathUtils.rotateQuatReverse(speedVector, this.rot);
            speedVector = SimMathUtils.rotateQuatReverse(speedVector, elementRot);
            particlePos = particlePos.add(totalOffset);
            level.addParticle(
               new PropellerAirParticleData(this.hasCollision, true),
               particlePos.x,
               particlePos.y,
               particlePos.z,
               speedVector.x(),
               speedVector.y(),
               speedVector.z()
            );
         }
      }
   }
}

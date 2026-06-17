package dev.eriksonn.aeronautics.content.ponder.instructions;

import com.mojang.math.Axis;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.gyroscopic_propeller_bearing.GyroscopicPropellerBearingBlockEntity;
import java.util.Objects;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3d;

public class CustomGyroBearingTiltInstruction extends TickingInstruction {
   protected final BlockPos location;
   protected final int ticks;
   protected final ElementLink<WorldSectionElement> link;
   protected final boolean directMotion;
   protected final boolean reversed;
   protected WorldSectionElement element;
   protected Quaternionf blockRot;
   protected Vec3 blockNormal;

   public CustomGyroBearingTiltInstruction(ElementLink<WorldSectionElement> link, BlockPos location, int ticks, boolean directMotion) {
      this(link, location, ticks, directMotion, false);
   }

   public CustomGyroBearingTiltInstruction(ElementLink<WorldSectionElement> link, BlockPos location, int ticks, boolean directMotion, boolean reversed) {
      super(false, ticks);
      this.location = location;
      this.ticks = ticks;
      this.link = link;
      this.directMotion = directMotion;
      this.reversed = reversed;
   }

   static Quaternionf getBlockStateOrientation(Direction facing) {
      Quaternionf orientation;
      if (facing.getAxis().isHorizontal()) {
         orientation = Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing.getOpposite()));
      } else {
         orientation = new Quaternionf();
      }

      orientation.mul(Axis.XP.rotationDegrees(-90.0F - AngleHelper.verticalAngle(facing)));
      return orientation;
   }

   protected final void firstTick(PonderScene scene) {
      super.firstTick(scene);
      PonderLevel level = scene.getWorld();
      if (this.link != null) {
         this.element = Objects.requireNonNull((WorldSectionElement)scene.resolve(this.link), "element");
      }

      if (level.getBlockState(this.location).hasProperty(BlockStateProperties.FACING)) {
         Quaternionf q = getBlockStateOrientation((Direction)level.getBlockState(this.location).getValue(BlockStateProperties.FACING));
         this.blockNormal = Vec3.atLowerCornerOf(((Direction)level.getBlockState(this.location).getValue(BlockStateProperties.FACING)).getNormal());
         this.blockRot = new Quaternionf(q);
      }
   }

   public void tick(PonderScene scene) {
      super.tick(scene);
      PonderLevel level = scene.getWorld();
      if (level.getBlockEntity(this.location) instanceof GyroscopicPropellerBearingBlockEntity gbe) {
         Vector3d target = new Vector3d(0.0, 1.0, 0.0);
         if (this.element != null) {
            Vec3 rot = this.element.getAnimatedRotation();
            target.set(0.0, 1.0, 0.0);
            target.rotateX((float) (Math.PI / 180.0) * -rot.x).rotateZ((float) (Math.PI / 180.0) * -rot.z).rotateY((float) (Math.PI / 180.0) * -rot.y);
         }

         float lerpAmount = 1.0F;
         if (!this.directMotion) {
            lerpAmount = 1.0F - (float)super.remainingTicks / (float)super.totalTicks;
         }

         if (this.reversed) {
            lerpAmount = 1.0F - lerpAmount;
         }

         gbe.setStrictTilt(target, (double)lerpAmount, 1.0);
      }
   }
}

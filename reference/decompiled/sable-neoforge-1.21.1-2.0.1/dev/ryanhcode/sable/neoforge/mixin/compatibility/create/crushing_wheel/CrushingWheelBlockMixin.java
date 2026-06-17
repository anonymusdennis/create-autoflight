package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.crushing_wheel;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlock;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin({CrushingWheelBlock.class})
public abstract class CrushingWheelBlockMixin extends RotatedPillarKineticBlock implements IBE<CrushingWheelBlockEntity> {
   public CrushingWheelBlockMixin(Properties arg) {
      super(arg);
   }

   @Overwrite
   public void entityInside(BlockState state, Level level, BlockPos pos, Entity entityIn) {
      SubLevel subLevel = Sable.HELPER.getContaining(level, pos);
      Vec3 entityPos = entityIn.position();
      if (subLevel != null) {
         entityPos = subLevel.logicalPose().transformPositionInverse(entityPos);
      }

      if (!(entityPos.y() < (double)((float)pos.getY() + 1.25F)) && entityIn.onGround()) {
         float speed = this.getBlockEntityOptional(level, pos).<Float>map(KineticBlockEntity::getSpeed).orElse(0.0F);
         double x = 0.0;
         double z = 0.0;
         double entityX = entityPos.x();
         double entityZ = entityPos.z();
         if (state.getValue(AXIS) == Axis.X) {
            z = (double)(speed / 20.0F);
            x += ((double)((float)pos.getX() + 0.5F) - entityX) * 0.1F;
         }

         if (state.getValue(AXIS) == Axis.Z) {
            x = (double)(speed / -20.0F);
            z += ((double)((float)pos.getZ() + 0.5F) - entityZ) * 0.1F;
         }

         Vec3 impulse = new Vec3(x, 0.0, z);
         if (subLevel != null) {
            impulse = subLevel.logicalPose().transformNormal(impulse);
         }

         entityIn.setDeltaMovement(entityIn.getDeltaMovement().add(impulse));
      }
   }
}

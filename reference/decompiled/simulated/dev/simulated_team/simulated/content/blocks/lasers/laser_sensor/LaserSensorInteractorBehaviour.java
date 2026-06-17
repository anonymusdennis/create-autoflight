package dev.simulated_team.simulated.content.blocks.lasers.laser_sensor;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import dev.ryanhcode.sable.Sable;
import dev.simulated_team.simulated.content.blocks.lasers.LaserBehaviour;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.Nullable;

public class LaserSensorInteractorBehaviour extends LaserBehaviour {
   public static final BehaviourType<LaserSensorInteractorBehaviour> TYPE = new BehaviourType();
   private LaserSensorBlockEntity previousSensor = null;
   public Supplier<Integer> directPower;
   public final Predicate<LaserSensorBlockEntity> filter;

   public LaserSensorInteractorBehaviour(
      SmartBlockEntity be, Supplier<Couple<Vec3>> positions, Supplier<Float> range, Supplier<Integer> directPower, Predicate<LaserSensorBlockEntity> filter
   ) {
      super(be, positions, range);
      this.directPower = directPower;
      this.filter = filter;
   }

   @Override
   public void tick() {
      Level level = this.blockEntity.getLevel();
      if (level != null) {
         super.tick();
         if (!this.checkAndUpdateSensor(this.getBlockHitResult(), this.getEntityHitResult())) {
            this.resetPrevData();
         }
      }
   }

   private boolean checkAndUpdateSensor(@Nullable BlockHitResult bhr, @Nullable EntityHitResult ehr) {
      if (bhr == null || bhr.getType() == Type.MISS) {
         return false;
      } else if (this.getClosestHitResult() instanceof EntityHitResult) {
         return false;
      } else {
         BlockEntity be = this.getWorld().getBlockEntity(bhr.getBlockPos());
         if (be instanceof LaserSensorBlockEntity lbe) {
            if (this.getProperFacing(be.getBlockState()) != bhr.getDirection() || !this.filter.test(lbe)) {
               return false;
            }

            this.updateHitSensor(lbe, bhr);
         }

         return true;
      }
   }

   private Direction getProperFacing(BlockState sensor) {
      Direction normal = (Direction)sensor.getValue(LaserSensorBlock.FACING);
      AttachFace target = (AttachFace)sensor.getValue(LaserSensorBlock.TARGET);
      if (target.getSerializedName().equals("ceiling")) {
         normal = Direction.UP;
      }

      if (target.getSerializedName().equals("floor")) {
         normal = Direction.DOWN;
      }

      return normal;
   }

   private void updateHitSensor(LaserSensorBlockEntity sensorBE, BlockHitResult context) {
      if (sensorBE != this.previousSensor) {
         this.resetPrevData();
      }

      float distance = (float)Math.sqrt(
         Sable.HELPER.distanceSquaredWithSubLevels(sensorBE.getLevel(), (Position)this.getLaserPositions().get().get(true), context.getLocation())
      );
      sensorBE.updateFromPointer((double)distance, this.directPower.get());
      this.previousSensor = sensorBE;
   }

   private void resetPrevData() {
      this.previousSensor = null;
   }

   @Override
   public BehaviourType<?> getType() {
      return super.getType();
   }
}

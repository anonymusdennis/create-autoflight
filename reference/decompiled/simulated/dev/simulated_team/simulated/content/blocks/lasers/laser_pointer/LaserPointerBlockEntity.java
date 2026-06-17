package dev.simulated_team.simulated.content.blocks.lasers.laser_pointer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.simulated_team.simulated.content.blocks.lasers.AbstractLaserBlockEntity;
import dev.simulated_team.simulated.content.blocks.lasers.laser_sensor.LaserSensorBlockEntity;
import dev.simulated_team.simulated.content.blocks.lasers.laser_sensor.LaserSensorInteractorBehaviour;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.service.SimConfigService;
import dev.simulated_team.simulated.util.SimColors;
import dev.simulated_team.simulated.util.SimLevelUtil;
import java.util.List;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.NotNull;

public class LaserPointerBlockEntity extends AbstractLaserBlockEntity implements ClipboardCloneable {
   private ScrollValueBehaviour range;
   public LaserSensorInteractorBehaviour sensorInteraction;
   private boolean rainbow;
   public int laserColor = SimColors.MEDIA_OURPLE;
   protected int bestPower;
   public Vec3 currentHitPos = Vec3.ZERO;

   public LaserPointerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      int rangeMax = (Integer)SimConfigService.INSTANCE.server().blocks.laserPointerRange.get();
      this.range = new ScrollValueBehaviour(
            SimLang.translate("laser_pointer.max_length").component(), this, new LaserPointerBlockEntity.RangeValueBoxTransform()
         )
         .between(1, rangeMax);
      this.range.value = rangeMax;
      this.sensorInteraction = new LaserSensorInteractorBehaviour(this, this::gatherStartAndEnd, this::getRaycastLength, this::getPower, this::matchesSensor);
      this.sensorInteraction.setShouldCast(this::shouldCast);
      behaviours.add(this.sensorInteraction);
      behaviours.add(this.range);
   }

   public void tick() {
      if (this.level != null && SimLevelUtil.isAreaActuallyLoaded(this.level, this.worldPosition, 2)) {
         if (!this.level.isClientSide || this.isVirtual()) {
            int currentPower = this.level.getBestNeighborSignal(this.worldPosition);
            if (currentPower != this.bestPower) {
               this.bestPower = currentPower;
               this.sendData();
            }
         }

         super.tick();
         if (!this.shouldCast()) {
            this.currentHitPos = Vec3.ZERO;
         } else {
            if (!this.isVirtual()) {
               BlockHitResult context = this.sensorInteraction.getBlockHitResult();
               if (context.getType() != Type.MISS) {
                  this.currentHitPos = context.getLocation();
               } else {
                  this.currentHitPos = Vec3.ZERO;
               }
            }
         }
      }
   }

   public boolean isAmethyst() {
      return this.laserColor == SimColors.MEDIA_OURPLE && !this.isRainbow();
   }

   public int getPower() {
      return this.getBlockState().getValue(LaserPointerBlock.INVERTED) ? 15 - this.bestPower : this.bestPower;
   }

   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      this.laserColor = tag.contains("LaserColor", 99) ? tag.getInt("LaserColor") : SimColors.MEDIA_OURPLE;
      this.bestPower = tag.getInt("BestPower");
      this.rainbow = tag.getBoolean("Rainbow");
      this.currentHitPos = this.readHitPos(tag);
      super.read(tag, registries, clientPacket);
   }

   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      tag.putInt("LaserColor", this.laserColor);
      tag.putInt("BestPower", this.bestPower);
      tag.putBoolean("Rainbow", this.isRainbow());
      this.writeHitPos(tag);
      super.write(tag, registries, clientPacket);
   }

   private void writeHitPos(CompoundTag tag) {
      tag.put("HitPos", VecHelper.writeNBT(this.currentHitPos));
   }

   private Vec3 readHitPos(CompoundTag tag) {
      Vec3 currentHit = Vec3.ZERO;
      if (tag.contains("HitPos")) {
         currentHit = VecHelper.readNBT(tag.getList("HitPos", 10));
      }

      return currentHit;
   }

   @Override
   public Direction getDirection() {
      return (Direction)this.getBlockState().getValue(LaserPointerBlock.FACING);
   }

   @Override
   public float getRaycastLength() {
      return (float)this.range.value;
   }

   @Override
   public boolean shouldCast() {
      return this.getPower() != 0;
   }

   public void setLaserColor(int color) {
      this.laserColor = color;
      this.setChanged();
      this.sendData();
   }

   public int getLaserColor() {
      return this.laserColor;
   }

   public boolean matchesSensor(LaserSensorBlockEntity sensor) {
      return sensor.filterColor(this.laserColor, this.rainbow);
   }

   public String getClipboardKey() {
      return "LaserPointer";
   }

   public boolean writeToClipboard(@NotNull Provider registries, CompoundTag tag, Direction side) {
      tag.putInt("Color", this.laserColor);
      tag.putBoolean("Rainbow", this.isRainbow());
      return true;
   }

   public boolean readFromClipboard(@NotNull Provider registries, CompoundTag tag, Player player, Direction side, boolean simulate) {
      if (simulate) {
         return true;
      } else {
         this.setLaserColor(tag.getInt("Color"));
         this.setRainbow(tag.getBoolean("Rainbow"));
         return true;
      }
   }

   public boolean isRainbow() {
      return this.rainbow;
   }

   public void setRainbow(boolean rainbow) {
      this.rainbow = rainbow;
      if (!this.getLevel().isClientSide) {
         this.notifyUpdate();
      }
   }

   private static class RangeValueBoxTransform extends Sided {
      protected boolean isSideActive(BlockState state, Direction direction) {
         return ((Direction)state.getValue(LaserPointerBlock.FACING)).getOpposite() == direction;
      }

      protected Vec3 getSouthLocation() {
         return VecHelper.voxelSpace(8.0, 8.0, 15.5);
      }

      public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
         super.rotate(level, pos, state, ms);
         Direction facing = (Direction)state.getValue(LaserPointerBlock.FACING);
         if (facing.getAxis() != Axis.Y) {
            if (this.getSide() == Direction.UP) {
               TransformStack.of(ms).rotateZDegrees(-AngleHelper.horizontalAngle(facing) + 180.0F);
            }
         }
      }
   }
}

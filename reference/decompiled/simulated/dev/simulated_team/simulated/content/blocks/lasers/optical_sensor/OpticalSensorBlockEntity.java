package dev.simulated_team.simulated.content.blocks.lasers.optical_sensor;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.content.redstone.DirectedDirectionalBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.content.blocks.lasers.AbstractLaserBlockEntity;
import dev.simulated_team.simulated.content.blocks.lasers.LaserBehaviour;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.service.SimConfigService;
import dev.simulated_team.simulated.service.SimFluidService;
import java.awt.Color;
import java.util.List;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3dc;

public class OpticalSensorBlockEntity extends AbstractLaserBlockEntity implements Clearable, ClipboardCloneable {
   private FilteringBehaviour filter;
   private ScrollValueBehaviour range;
   public LaserBehaviour laser;
   private Block hitBlock = Blocks.AIR;
   private float rayDistance = this.getRaycastLength();
   private float lastRayDistance = this.getRaycastLength();
   private float opacity = 1.0F;

   public Block getHitBlock() {
      return this.hitBlock;
   }

   public float getHitBlockDistance() {
      if (this.hitBlock.defaultBlockState().isAir()) {
         return this.getRaycastLength();
      } else {
         Vector3dc pos = Sable.HELPER.projectOutOfSubLevel(this.getLevel(), JOMLConversion.atCenterOf(this.getBlockPos()));
         Vector3dc hitPos = Sable.HELPER.projectOutOfSubLevel(this.getLevel(), JOMLConversion.toJOML(this.laser.getBlockHitResult().getLocation()));
         return (float)pos.distance(hitPos);
      }
   }

   public boolean hasHit() {
      return !this.hitBlock.defaultBlockState().isAir();
   }

   public OpticalSensorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(this.filter = new FilteringBehaviour(this, new OpticalSensorBlockEntity.FilterValueBoxTransform()));
      int maxRange = (Integer)SimConfigService.INSTANCE.server().blocks.opticalSensorRange.get();
      behaviours.add(
         this.range = new OpticalSensorBlockEntity.RangeScrollValueBehaviour(
               SimLang.translate("optical_sensor.max_length").component(), this, new OpticalSensorBlockEntity.RangeValueBoxTransform()
            )
            .between(1, maxRange)
      );
      this.range.value = maxRange;
      behaviours.add(this.laser = new LaserBehaviour(this, this::gatherStartAndEnd, this::getRaycastLength));
   }

   public void tick() {
      super.tick();
      if (!this.isVirtual()) {
         BlockHitResult context = this.laser.getBlockHitResult();
         if (context != null && this.hasLevel()) {
            this.rayDistance = (float)Math.sqrt(
               Sable.HELPER.distanceSquaredWithSubLevels(this.level, (Position)this.laser.getLaserPositions().get().get(true), context.getLocation())
            );
            boolean shouldPower = this.checkFilter(context);
            if (this.lastRayDistance != this.rayDistance || (Boolean)this.getBlockState().getValue(OpticalSensorBlock.POWERED) != shouldPower) {
               this.level.setBlockAndUpdate(this.worldPosition, (BlockState)this.getBlockState().setValue(OpticalSensorBlock.POWERED, shouldPower));
               this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
               this.invalidateRenderBoundingBox();
            }

            this.lastRayDistance = this.rayDistance;
         }

         if (SimFluidService.INSTANCE.getFluidInItem(this.filter.getFilter()) != null) {
            this.laser.setFluidCollide(Fluid.ANY);
         } else {
            this.laser.setFluidCollide(Fluid.NONE);
         }
      }
   }

   private boolean checkFilter(BlockHitResult context) {
      BlockState hitBlock = this.level.getBlockState(context.getBlockPos());
      FluidState hitFluid = this.level.getFluidState(context.getBlockPos());
      boolean passed = false;
      ItemStack filterItem = this.filter.getFilter();
      if (context.getType() != Type.MISS) {
         if (!filterItem.isEmpty()) {
            net.minecraft.world.level.material.Fluid fluidInItem = SimFluidService.INSTANCE.getFluidInItem(filterItem);
            if (fluidInItem != null && !hitFluid.isEmpty()) {
               passed = fluidInItem.isSame(hitFluid.getType());
            } else {
               passed = !hitBlock.isAir() && this.filter.test(new ItemStack(hitBlock.getBlock()));
            }
         } else {
            passed = true;
         }
      }

      this.hitBlock = passed ? hitBlock.getBlock() : Blocks.AIR;
      return passed;
   }

   @Override
   public Direction getDirection() {
      AttachFace target = (AttachFace)this.getBlockState().getValue(OpticalSensorBlock.TARGET);
      if (target == AttachFace.CEILING) {
         return Direction.UP;
      } else {
         return target == AttachFace.FLOOR ? Direction.DOWN : (Direction)this.getBlockState().getValue(OpticalSensorBlock.FACING);
      }
   }

   @Override
   public boolean shouldCast() {
      return true;
   }

   @Override
   public float getRaycastLength() {
      return (float)this.range.getValue() + 0.5F;
   }

   public int getRange() {
      return this.range.getValue();
   }

   public void setRange(int blocks) {
      int max = (Integer)SimConfigService.INSTANCE.server().blocks.opticalSensorRange.get();
      this.range.setValue(Math.clamp((long)blocks, 1, max));
   }

   public float getRayDistance() {
      return this.rayDistance;
   }

   public boolean tryApplyDye(ItemStack item) {
      if (item.getItem() instanceof DyeItem dyeItem) {
         Color color = new Color(dyeItem.getDyeColor().getTextColor());
         if (color.getRed() == color.getGreen() && color.getGreen() == color.getBlue()) {
            this.opacity = (float)color.getRed() / 255.0F;
            this.opacity = this.opacity * this.opacity;
            this.setChanged();
            this.sendData();
            return true;
         }
      }

      return false;
   }

   public float getOpacity() {
      return this.opacity;
   }

   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.read(tag, registries, clientPacket);
      this.opacity = Math.clamp(tag.contains("Opacity") ? tag.getFloat("Opacity") : 1.0F, 0.0F, 1.0F);
   }

   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.write(tag, registries, clientPacket);
      tag.putFloat("Opacity", this.opacity);
   }

   public void clearContent() {
      this.filter.setFilter(ItemStack.EMPTY);
   }

   public String getClipboardKey() {
      return "OpticalSensor";
   }

   public boolean writeToClipboard(@NotNull Provider registries, CompoundTag tag, Direction side) {
      tag.putFloat("Opacity", this.getOpacity());
      return true;
   }

   public boolean readFromClipboard(@NotNull Provider registries, CompoundTag tag, Player player, Direction side, boolean simulate) {
      if (simulate) {
         return true;
      } else {
         this.opacity = tag.getFloat("Opacity");
         return true;
      }
   }

   private static class FilterValueBoxTransform extends Sided {
      protected boolean isSideActive(BlockState state, Direction direction) {
         return (switch ((AttachFace)state.getValue(OpticalSensorBlock.TARGET)) {
            case FLOOR, CEILING -> (Direction)state.getValue(OpticalSensorBlock.FACING);
            default -> Direction.UP;
         }).getAxis() == direction.getAxis();
      }

      protected Vec3 getSouthLocation() {
         return VecHelper.voxelSpace(8.0, 8.0, 15.5);
      }

      public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
         super.rotate(level, pos, state, ms);
         Direction facing = (Direction)state.getValue(DirectedDirectionalBlock.FACING);
         if (facing.getAxis() != Axis.Y) {
            if (this.getSide() == Direction.UP) {
               TransformStack.of(ms).rotateZDegrees(-AngleHelper.horizontalAngle(facing) + 180.0F);
            }
         }
      }
   }

   private static class RangeScrollValueBehaviour extends ScrollValueBehaviour {
      public RangeScrollValueBehaviour(Component label, SmartBlockEntity be, ValueBoxTransform slot) {
         super(label, be, slot);
      }

      public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
         return new ValueSettingsBoard(
            this.label,
            this.max,
            15,
            ImmutableList.of(Component.translatable("simulated.unit.length_blocks")),
            new ValueSettingsFormatter(this::formatSettings)
         );
      }

      public MutableComponent formatSettings(ValueSettings settings) {
         int value = Math.max(1, settings.value());
         return Component.literal(String.valueOf(value));
      }
   }

   private static class RangeValueBoxTransform extends Sided {
      protected boolean isSideActive(BlockState state, Direction direction) {
         DirectedDirectionalBlock.getTargetDirection(state);
         return DirectedDirectionalBlock.getTargetDirection(state).getOpposite() == direction;
      }

      protected Vec3 getSouthLocation() {
         return VecHelper.voxelSpace(8.0, 8.0, 15.5);
      }

      public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
         super.rotate(level, pos, state, ms);
         Direction facing = (Direction)state.getValue(DirectedDirectionalBlock.FACING);
         if (facing.getAxis() != Axis.Y) {
            if (this.getSide() == Direction.UP) {
               TransformStack.of(ms).rotateZDegrees(-AngleHelper.horizontalAngle(facing) + 180.0F);
            }
         }
      }
   }
}

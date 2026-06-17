package com.simibubi.create.content.kinetics.waterwheel;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.fluid.FluidHelper;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class WaterWheelBlockEntity extends GeneratingKineticBlockEntity {
   public static final Map<Axis, Set<BlockPos>> SMALL_OFFSETS = new EnumMap<>(Axis.class);
   public static final Map<Axis, Set<BlockPos>> LARGE_OFFSETS = new EnumMap<>(Axis.class);
   public int flowScore;
   public BlockState material = Blocks.SPRUCE_PLANKS.defaultBlockState();

   public WaterWheelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.setLazyTickRate(60);
   }

   protected int getSize() {
      return 1;
   }

   protected Set<BlockPos> getOffsetsToCheck() {
      return (this.getSize() == 1 ? SMALL_OFFSETS : LARGE_OFFSETS).get(this.getAxis());
   }

   public ItemInteractionResult applyMaterialIfValid(ItemStack stack) {
      if (stack.getItem() instanceof BlockItem blockItem) {
         BlockState material = blockItem.getBlock().defaultBlockState();
         if (material == this.material) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         } else if (!material.is(BlockTags.PLANKS)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         } else if (this.level.isClientSide() && !this.isVirtual()) {
            return ItemInteractionResult.SUCCESS;
         } else {
            this.material = material;
            this.notifyUpdate();
            this.level.levelEvent(2001, this.worldPosition, Block.getId(material));
            return ItemInteractionResult.SUCCESS;
         }
      } else {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   protected Axis getAxis() {
      Axis axis = Axis.X;
      BlockState blockState = this.getBlockState();
      if (blockState.getBlock() instanceof IRotate irotate) {
         axis = irotate.getRotationAxis(blockState);
      }

      return axis;
   }

   @Override
   public void lazyTick() {
      super.lazyTick();
      this.determineAndApplyFlowScore();
   }

   public void determineAndApplyFlowScore() {
      Vec3 wheelPlane = Vec3.atLowerCornerOf(new Vec3i(1, 1, 1).subtract(Direction.get(AxisDirection.POSITIVE, this.getAxis()).getNormal()));
      int flowScore = 0;
      boolean lava = false;

      for (BlockPos blockPos : this.getOffsetsToCheck()) {
         BlockPos targetPos = blockPos.offset(this.worldPosition);
         Vec3 flowAtPos = this.getFlowVectorAtPosition(targetPos).multiply(wheelPlane);
         lava |= FluidHelper.isLava(this.level.getFluidState(targetPos).getType());
         if (flowAtPos.lengthSqr() != 0.0) {
            flowAtPos = flowAtPos.normalize();
            Vec3 normal = Vec3.atLowerCornerOf(blockPos).normalize();
            Vec3 positiveMotion = VecHelper.rotate(normal, 90.0, this.getAxis());
            double dot = flowAtPos.dot(positiveMotion);
            if (Math.abs(dot) > 0.5) {
               flowScore = (int)((double)flowScore + Math.signum(dot));
            }
         }
      }

      if (flowScore != 0 && !this.level.isClientSide()) {
         this.award(lava ? AllAdvancements.LAVA_WHEEL : AllAdvancements.WATER_WHEEL);
      }

      this.setFlowScoreAndUpdate(flowScore);
   }

   public Vec3 getFlowVectorAtPosition(BlockPos pos) {
      FluidState fluid = this.level.getFluidState(pos);
      Vec3 vec = fluid.getFlow(this.level, pos);
      BlockState blockState = this.level.getBlockState(pos);
      if (blockState.getBlock() == Blocks.BUBBLE_COLUMN) {
         vec = new Vec3(0.0, blockState.getValue(BubbleColumnBlock.DRAG_DOWN) ? -1.0 : 1.0, 0.0);
      }

      return vec;
   }

   public void setFlowScoreAndUpdate(int score) {
      if (this.flowScore != score) {
         this.flowScore = score;
         this.updateGeneratedRotation();
         this.setChanged();
      }
   }

   private void redraw() {
      if (!this.isVirtual()) {
         this.requestModelDataUpdate();
      }

      if (this.hasLevel()) {
         this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 16);
         this.level.getChunkSource().getLightEngine().checkBlock(this.worldPosition);
      }
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      super.addBehaviours(behaviours);
      this.registerAwardables(behaviours, new CreateAdvancement[]{AllAdvancements.LAVA_WHEEL, AllAdvancements.WATER_WHEEL});
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      this.flowScore = compound.getInt("FlowScore");
      BlockState prevMaterial = this.material;
      if (compound.contains("Material")) {
         this.material = NbtUtils.readBlockState(this.blockHolderGetter(), compound.getCompound("Material"));
         if (this.material.isAir()) {
            this.material = Blocks.SPRUCE_PLANKS.defaultBlockState();
         }

         if (clientPacket && prevMaterial != this.material) {
            this.redraw();
         }
      }
   }

   @Override
   public void writeSafe(CompoundTag tag, Provider registries) {
      super.writeSafe(tag, registries);
      tag.put("Material", NbtUtils.writeBlockState(this.material));
   }

   @Override
   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.write(compound, registries, clientPacket);
      compound.putInt("FlowScore", this.flowScore);
      compound.put("Material", NbtUtils.writeBlockState(this.material));
   }

   @Override
   protected AABB createRenderBoundingBox() {
      return new AABB(this.worldPosition).inflate((double)this.getSize());
   }

   @Override
   public float getGeneratedSpeed() {
      return (float)(Mth.clamp(this.flowScore, -1, 1) * 8 / this.getSize());
   }

   static {
      for (Axis axis : Iterate.axes) {
         HashSet<BlockPos> offsets = new HashSet<>();

         for (Direction d : Iterate.directions) {
            if (d.getAxis() != axis) {
               offsets.add(BlockPos.ZERO.relative(d));
            }
         }

         SMALL_OFFSETS.put(axis, offsets);
         offsets = new HashSet<>();

         for (Direction dx : Iterate.directions) {
            if (dx.getAxis() != axis) {
               BlockPos centralOffset = BlockPos.ZERO.relative(dx, 2);
               offsets.add(centralOffset);

               for (Direction d2 : Iterate.directions) {
                  if (d2.getAxis() != axis && d2.getAxis() != dx.getAxis()) {
                     offsets.add(centralOffset.relative(d2));
                  }
               }
            }
         }

         LARGE_OFFSETS.put(axis, offsets);
      }
   }
}

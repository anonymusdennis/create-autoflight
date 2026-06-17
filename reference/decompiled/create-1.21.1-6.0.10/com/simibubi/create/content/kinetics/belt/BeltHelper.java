package com.simibubi.create.content.kinetics.belt;

import com.simibubi.create.AllTags;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import java.util.Map;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;

public class BeltHelper {
   public static Map<Item, Boolean> uprightCache = new Object2BooleanOpenHashMap();
   public static final ResourceManagerReloadListener LISTENER = resourceManager -> uprightCache.clear();

   public static boolean isItemUpright(ItemStack stack) {
      return uprightCache.computeIfAbsent(stack.getItem(), item -> {
         boolean isFluidHandler = stack.getCapability(FluidHandler.ITEM) != null;
         boolean useUpright = AllTags.AllItemTags.UPRIGHT_ON_BELT.matches(stack);
         boolean forceDisableUpright = !AllTags.AllItemTags.NOT_UPRIGHT_ON_BELT.matches(stack);
         return (isFluidHandler || useUpright) && forceDisableUpright;
      });
   }

   public static BeltBlockEntity getSegmentBE(LevelAccessor world, BlockPos pos) {
      if (world instanceof Level l && !l.isLoaded(pos)) {
         return null;
      }

      BlockEntity blockEntity = world.getBlockEntity(pos);
      return !(blockEntity instanceof BeltBlockEntity) ? null : (BeltBlockEntity)blockEntity;
   }

   public static BeltBlockEntity getControllerBE(LevelAccessor world, BlockPos pos) {
      BeltBlockEntity segment = getSegmentBE(world, pos);
      if (segment == null) {
         return null;
      } else {
         BlockPos controllerPos = segment.controller;
         return controllerPos == null ? null : getSegmentBE(world, controllerPos);
      }
   }

   public static BeltBlockEntity getBeltForOffset(BeltBlockEntity controller, float offset) {
      return getBeltAtSegment(controller, (int)Math.floor((double)offset));
   }

   public static BeltBlockEntity getBeltAtSegment(BeltBlockEntity controller, int segment) {
      BlockPos pos = getPositionForOffset(controller, segment);
      BlockEntity be = controller.getLevel().getBlockEntity(pos);
      return be != null && be instanceof BeltBlockEntity ? (BeltBlockEntity)be : null;
   }

   public static BlockPos getPositionForOffset(BeltBlockEntity controller, int offset) {
      BlockPos pos = controller.getBlockPos();
      Vec3i vec = controller.getBeltFacing().getNormal();
      BeltSlope slope = (BeltSlope)controller.getBlockState().getValue(BeltBlock.SLOPE);
      int verticality = slope == BeltSlope.DOWNWARD ? -1 : (slope == BeltSlope.UPWARD ? 1 : 0);
      return pos.offset(offset * vec.getX(), Mth.clamp(offset, 0, controller.beltLength - 1) * verticality, offset * vec.getZ());
   }

   public static Vec3 getVectorForOffset(BeltBlockEntity controller, float offset) {
      BeltSlope slope = (BeltSlope)controller.getBlockState().getValue(BeltBlock.SLOPE);
      int verticality = slope == BeltSlope.DOWNWARD ? -1 : (slope == BeltSlope.UPWARD ? 1 : 0);
      float verticalMovement = (float)verticality;
      if ((double)offset < 0.5) {
         verticalMovement = 0.0F;
      }

      verticalMovement *= Math.min(offset, (float)controller.beltLength - 0.5F) - 0.5F;
      Vec3 vec = VecHelper.getCenterOf(controller.getBlockPos());
      Vec3 horizontalMovement = Vec3.atLowerCornerOf(controller.getBeltFacing().getNormal()).scale((double)(offset - 0.5F));
      if (slope == BeltSlope.VERTICAL) {
         horizontalMovement = Vec3.ZERO;
      }

      return vec.add(horizontalMovement).add(0.0, (double)verticalMovement, 0.0);
   }

   public static Vec3 getBeltVector(BlockState state) {
      BeltSlope slope = (BeltSlope)state.getValue(BeltBlock.SLOPE);
      int verticality = slope == BeltSlope.DOWNWARD ? -1 : (slope == BeltSlope.UPWARD ? 1 : 0);
      Vec3 horizontalMovement = Vec3.atLowerCornerOf(((Direction)state.getValue(BeltBlock.HORIZONTAL_FACING)).getNormal());
      return slope == BeltSlope.VERTICAL
         ? new Vec3(0.0, (double)((Direction)state.getValue(BeltBlock.HORIZONTAL_FACING)).getAxisDirection().getStep(), 0.0)
         : new Vec3(0.0, (double)verticality, 0.0).add(horizontalMovement);
   }
}

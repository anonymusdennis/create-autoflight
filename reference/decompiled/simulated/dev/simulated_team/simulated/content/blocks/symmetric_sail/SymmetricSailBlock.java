package dev.simulated_team.simulated.content.blocks.symmetric_sail;

import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.foundation.utility.BlockHelper;
import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.service.SimItemService;
import dev.simulated_team.simulated.util.placement_helpers.SymmetricSailPlacementHelper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class SymmetricSailBlock extends RotatedPillarBlock implements IWrenchable, BlockSubLevelLiftProvider, SpecialBlockItemRequirement {
   private static final int placementHelperId = PlacementHelpers.register(
      new SymmetricSailPlacementHelper(SymmetricSailBlock::checkItem, SymmetricSailBlock::checkState)
   );
   protected final DyeColor color;

   public SymmetricSailBlock(Properties properties, DyeColor color) {
      super(properties);
      this.color = color;
   }

   private static boolean checkItem(ItemStack i) {
      if (i.getItem() instanceof BlockItem bi && bi.getBlock() instanceof SymmetricSailBlock) {
         return true;
      }

      return false;
   }

   private static boolean checkState(BlockState state) {
      return state.getBlock() instanceof SymmetricSailBlock;
   }

   public static SymmetricSailBlock withCanvas(Properties properties, DyeColor color) {
      return new SymmetricSailBlock(properties, color);
   }

   public void applyDye(BlockState state, Level world, BlockPos pos, Vec3 hit, @Nullable DyeColor color) {
      BlockState newState = SimBlocks.DYED_SYMMETRIC_SAILS.get(color).getDefaultState();
      newState = BlockHelper.copyProperties(state, newState);
      if (state != newState) {
         world.setBlockAndUpdate(pos, newState);
      } else {
         for (Direction d : IPlacementHelper.orderedByDistanceExceptAxis(pos, hit, (Axis)state.getValue(AXIS))) {
            BlockPos offset = pos.relative(d);
            BlockState adjacentState = world.getBlockState(offset);
            Block block = adjacentState.getBlock();
            if (block instanceof SymmetricSailBlock && state.getValue(AXIS) == adjacentState.getValue(AXIS) && state != adjacentState) {
               world.setBlockAndUpdate(offset, newState);
               return;
            }
         }

         List<BlockPos> frontier = new ArrayList<>();
         frontier.add(pos);
         Set<BlockPos> visited = new HashSet<>();
         int timeout = 100;

         while (!frontier.isEmpty() && timeout-- >= 0) {
            BlockPos currentPos = frontier.removeFirst();
            visited.add(currentPos);

            for (Direction dx : Iterate.directions) {
               if (dx.getAxis() != state.getValue(AXIS)) {
                  BlockPos offset = currentPos.relative(dx);
                  if (!visited.contains(offset)) {
                     BlockState adjacentState = world.getBlockState(offset);
                     Block block = adjacentState.getBlock();
                     if (block instanceof SymmetricSailBlock && adjacentState.getValue(AXIS) == state.getValue(AXIS)) {
                        if (state != adjacentState) {
                           world.setBlockAndUpdate(offset, newState);
                        }

                        frontier.add(offset);
                        visited.add(offset);
                     }
                  }
               }
            }
         }
      }
   }

   protected ItemInteractionResult useItemOn(
      ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult
   ) {
      ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
      DyeColor color = SimItemService.getDyeColor(heldItem);
      if (color != null) {
         if (!level.isClientSide) {
            level.playSound(null, blockPos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.1F - level.random.nextFloat() * 0.2F);
         }

         this.applyDye(blockState, level, blockPos, blockHitResult.getLocation(), color);
         return ItemInteractionResult.SUCCESS;
      } else {
         IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
         if (placementHelper.matchesItem(heldItem)) {
            placementHelper.getOffset(player, level, blockState, blockPos, blockHitResult)
               .placeInWorld(level, (BlockItem)heldItem.getItem(), player, interactionHand, blockHitResult);
            return ItemInteractionResult.SUCCESS;
         } else {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         }
      }
   }

   public DyeColor getColor() {
      return this.color;
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      return (BlockState)this.defaultBlockState().setValue(AXIS, pContext.getNearestLookingDirection().getAxis());
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext ctx) {
      return SimBlockShapes.SYMMETRIC_SAIL.get((Axis)pState.getValue(AXIS));
   }

   public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
      super.fallOn(level, state, pos, entity, 0.0F);
   }

   public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
      if (entity.isSuppressingBounce()) {
         super.updateEntityAfterFallOn(level, entity);
      } else {
         this.bounce(entity);
      }
   }

   private void bounce(Entity pEntity) {
      Vec3 Vec3 = pEntity.getDeltaMovement();
      if (Vec3.y < 0.0) {
         double d0 = pEntity instanceof LivingEntity ? 1.0 : 0.8;
         pEntity.setDeltaMovement(Vec3.x, -Vec3.y * 0.26F * d0, Vec3.z);
      }
   }

   public float sable$getLiftScalar() {
      return 0.0F;
   }

   public float sable$getParallelDragScalar() {
      return 1.75F;
   }

   public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
      return SimBlocks.WHITE_SYMMETRIC_SAIL.asStack();
   }

   @NotNull
   public Direction sable$getNormal(BlockState blockState) {
      return Direction.get(AxisDirection.POSITIVE, (Axis)blockState.getValue(AXIS));
   }

   public ItemRequirement getRequiredItems(BlockState state, @org.jetbrains.annotations.Nullable BlockEntity blockEntity) {
      return new ItemRequirement(ItemUseType.CONSUME, SimBlocks.WHITE_SYMMETRIC_SAIL.asStack());
   }
}

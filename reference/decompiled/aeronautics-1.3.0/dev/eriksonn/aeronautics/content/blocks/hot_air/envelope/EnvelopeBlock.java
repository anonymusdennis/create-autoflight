package dev.eriksonn.aeronautics.content.blocks.hot_air.envelope;

import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.foundation.utility.BlockHelper;
import dev.eriksonn.aeronautics.index.AeroBlocks;
import dev.simulated_team.simulated.service.SimItemService;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.List;
import java.util.Set;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class EnvelopeBlock extends CasingBlock implements Envelope, SpecialBlockItemRequirement {
   private static final BlockPos[] DIRECTION_OFFSETS = new BlockPos[]{
      new BlockPos(1, 0, 0),
      new BlockPos(-1, 0, 0),
      new BlockPos(0, 1, 0),
      new BlockPos(0, -1, 0),
      new BlockPos(0, 0, 1),
      new BlockPos(0, 0, -1),
      new BlockPos(1, 1, 0),
      new BlockPos(-1, -1, 0),
      new BlockPos(1, -1, 0),
      new BlockPos(-1, 1, 0),
      new BlockPos(1, 0, 1),
      new BlockPos(-1, 0, -1),
      new BlockPos(1, 0, -1),
      new BlockPos(-1, 0, 1),
      new BlockPos(0, 1, 1),
      new BlockPos(0, -1, -1),
      new BlockPos(0, -1, 1),
      new BlockPos(0, 1, -1)
   };
   protected final DyeColor color;

   public EnvelopeBlock(Properties properties, DyeColor color) {
      super(properties);
      this.color = color;
   }

   protected static void applyDye(BlockState state, Level level, BlockPos pos, DyeColor color) {
      BlockState newEnvelopeState = BlockHelper.copyProperties(state, AeroBlocks.DYED_ENVELOPE_BLOCKS.get(color).getDefaultState());
      BlockState newEncasedEnvelopeState = BlockHelper.copyProperties(state, AeroBlocks.ENVELOPE_ENCASED_SHAFTS.get(color).getDefaultState());
      if (!selfDye(level, pos, state, color)) {
         boolean hasDyed = false;

         for (Direction d : Iterate.directions) {
            BlockPos offset = pos.relative(d);
            BlockState adjacentState = level.getBlockState(offset);
            if (selfDye(level, offset, adjacentState, color)) {
               hasDyed = true;
            }
         }

         if (!hasDyed) {
            List<BlockPos> frontier = new ObjectArrayList();
            frontier.add(pos);
            Set<BlockPos> visited = new ObjectOpenHashSet();
            float timeout = 125.0F;

            while (!frontier.isEmpty() && !(timeout-- < 0.0F)) {
               BlockPos currentPos = frontier.removeFirst();
               visited.add(currentPos);

               for (BlockPos dx : DIRECTION_OFFSETS) {
                  BlockPos offsetPos = currentPos.offset(dx);
                  if (!visited.contains(offsetPos)) {
                     BlockState adjacentState = level.getBlockState(offsetPos);
                     if (multiDye(level, offsetPos, adjacentState, newEnvelopeState) || multiDye(level, offsetPos, adjacentState, newEncasedEnvelopeState)) {
                        frontier.add(offsetPos);
                        visited.add(offsetPos);
                     }
                  }
               }
            }
         }
      }
   }

   static boolean selfDye(Level level, BlockPos pos, BlockState state, DyeColor color) {
      if (state.getBlock() instanceof EnvelopeBlock eb && eb.getColor() != color) {
         level.setBlockAndUpdate(pos, AeroBlocks.DYED_ENVELOPE_BLOCKS.get(color).getDefaultState());
         return true;
      }

      if (state.getBlock() instanceof EnvelopeEncasedShaftBlock eb && eb.getColor() != color) {
         Axis axis = eb.getRotationAxis(state);
         level.setBlockAndUpdate(
            pos, (BlockState)AeroBlocks.ENVELOPE_ENCASED_SHAFTS.get(color).getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, axis)
         );
         return true;
      }

      return false;
   }

   static boolean multiDye(Level Level, BlockPos pos, BlockState state, BlockState newState) {
      if (state.getBlock() instanceof EnvelopeBlock && newState.getBlock() instanceof EnvelopeBlock) {
         if (state != newState) {
            Level.setBlockAndUpdate(pos, newState);
         }

         return true;
      } else if (state.getBlock() instanceof EnvelopeEncasedShaftBlock && newState.getBlock() instanceof EnvelopeEncasedShaftBlock) {
         if (state != newState) {
            Axis axis = (Axis)state.getValue(RotatedPillarKineticBlock.AXIS);
            Level.setBlockAndUpdate(pos, (BlockState)newState.setValue(RotatedPillarKineticBlock.AXIS, axis));
         }

         return true;
      } else {
         return false;
      }
   }

   protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
      return 1;
   }

   protected ItemInteractionResult useItemOn(
      ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult
   ) {
      DyeColor color = SimItemService.getDyeColor(itemStack);
      if (color != null) {
         if (!level.isClientSide()) {
            level.playSound(null, blockPos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.1F - level.random.nextFloat() * 0.2F);
         }

         applyDye(blockState, level, blockPos, color);
         return ItemInteractionResult.SUCCESS;
      } else {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   public ItemStack getCloneItemStack(LevelReader levelReader, BlockPos blockPos, BlockState blockState) {
      return AeroBlocks.DYED_ENVELOPE_BLOCKS.get(this.color).asStack();
   }

   @Override
   public DyeColor getColor() {
      return this.color;
   }

   public void fallOn(Level pLevel, BlockState pState, BlockPos pPos, Entity pEntity, float pFallDistance) {
      if (pEntity.isSuppressingBounce()) {
         super.fallOn(pLevel, pState, pPos, pEntity, pFallDistance);
      } else {
         pEntity.causeFallDamage(pFallDistance, 0.0F, pLevel.damageSources().fall());
      }
   }

   public void updateEntityAfterFallOn(BlockGetter pLevel, Entity pEntity) {
      if (pEntity.isSuppressingBounce()) {
         super.updateEntityAfterFallOn(pLevel, pEntity);
      } else {
         this.bounceUp(pEntity);
      }
   }

   private void bounceUp(Entity entity) {
      Vec3 vec3 = entity.getDeltaMovement();
      if (vec3.y < 0.0) {
         double scale = 0.65 * (entity instanceof LivingEntity ? 1.0 : 0.8);
         entity.setDeltaMovement(vec3.x, -vec3.y * scale, vec3.z);
      }
   }

   public ItemRequirement getRequiredItems(BlockState state, @Nullable BlockEntity blockEntity) {
      ItemStack stack = AeroBlocks.WHITE_ENVELOPE_BLOCK.asStack();
      return new ItemRequirement(ItemUseType.CONSUME, stack);
   }
}

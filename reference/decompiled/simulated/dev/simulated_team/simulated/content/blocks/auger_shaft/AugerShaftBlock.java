package dev.simulated_team.simulated.content.blocks.auger_shaft;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.logistics.chute.ChuteBlock;
import com.simibubi.create.content.logistics.funnel.AbstractFunnelBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.placement.PoleHelper;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimSoundEvents;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class AugerShaftBlock extends RotatedPillarKineticBlock implements IBE<AugerShaftBlockEntity> {
   public static final int placementHelperId = PlacementHelpers.register(new AugerShaftBlock.PlacementHelper());
   public static final EnumProperty<AugerShaftBlock.BarrelSection> SECTION = EnumProperty.create("section", AugerShaftBlock.BarrelSection.class);
   public static final BooleanProperty COG = BooleanProperty.create("cog");
   public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
   public static final BooleanProperty EAST = BlockStateProperties.EAST;
   public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
   public static final BooleanProperty WEST = BlockStateProperties.WEST;
   public static final BooleanProperty UP = BlockStateProperties.UP;
   public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
   public static final BooleanProperty ENCASED = BooleanProperty.create("encased");
   public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = ImmutableMap.copyOf((Map)Util.make(Maps.newEnumMap(Direction.class), map -> {
      map.put(Direction.NORTH, NORTH);
      map.put(Direction.EAST, EAST);
      map.put(Direction.SOUTH, SOUTH);
      map.put(Direction.WEST, WEST);
      map.put(Direction.UP, UP);
      map.put(Direction.DOWN, DOWN);
   }));

   public AugerShaftBlock(Properties properties) {
      super(properties);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)super.defaultBlockState()
                                    .setValue(SECTION, AugerShaftBlock.BarrelSection.SINGLE))
                                 .setValue(COG, false))
                              .setValue(NORTH, false))
                           .setValue(EAST, false))
                        .setValue(SOUTH, false))
                     .setValue(WEST, false))
                  .setValue(UP, false))
               .setValue(DOWN, false))
            .setValue(ENCASED, false)
      );
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder.add(new Property[]{SECTION, COG, NORTH, EAST, SOUTH, WEST, UP, DOWN, ENCASED}));
   }

   private boolean connects(BlockPos pos, BlockState state, BlockPos otherPos, BlockState otherState) {
      return otherState.getBlock() == this && otherState.getValue(AXIS) == state.getValue(AXIS);
   }

   protected ItemInteractionResult useItemOn(
      ItemStack heldItem, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult
   ) {
      IPlacementHelper helper = PlacementHelpers.get(placementHelperId);
      if (helper.matchesItem(heldItem)) {
         return helper.getOffset(player, level, blockState, blockPos, blockHitResult)
            .placeInWorld(level, (BlockItem)heldItem.getItem(), player, interactionHand, blockHitResult);
      } else {
         if (!(blockState.getBlock() instanceof AugerCogBlock)) {
            Boolean encased = (Boolean)blockState.getValue(ENCASED);
            if (encased && player.getItemInHand(interactionHand).is((Item)AllItems.WRENCH.get())) {
               if (level.isClientSide) {
                  return ItemInteractionResult.SUCCESS;
               }

               level.setBlockAndUpdate(blockPos, (BlockState)blockState.cycle(ENCASED));
               level.levelEvent(2001, blockPos, Block.getId(AllBlocks.INDUSTRIAL_IRON_BLOCK.getDefaultState()));
               return ItemInteractionResult.SUCCESS;
            }

            if (!encased && player.getItemInHand(interactionHand).is(AllBlocks.INDUSTRIAL_IRON_BLOCK.asItem())) {
               if (level.isClientSide) {
                  return ItemInteractionResult.SUCCESS;
               }

               level.setBlockAndUpdate(blockPos, (BlockState)blockState.cycle(ENCASED));
               level.playSound(null, blockPos, SimSoundEvents.AUGER_SHAFT_ENCASING.event(), SoundSource.BLOCKS, 0.5F, 1.05F);
               return ItemInteractionResult.SUCCESS;
            }
         }

         return super.useItemOn(heldItem, blockState, level, blockPos, player, interactionHand, blockHitResult);
      }
   }

   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      Level level = context.getLevel();
      return level.isClientSide ? InteractionResult.SUCCESS : this.transformAuger(state, SimBlocks.AUGER_COG.getDefaultState(), context, level);
   }

   @Nullable
   protected InteractionResult transformAuger(BlockState state, BlockState newState, UseOnContext context, Level level) {
      RegistryAccess reg = level.registryAccess();
      AugerShaftBlockEntity abe = (AugerShaftBlockEntity)this.getBlockEntity(level, context.getClickedPos());
      if (abe != null) {
         CompoundTag tag = new CompoundTag();
         abe.write(tag, reg, false);
         abe.beingWrenched = true;
         KineticBlockEntity.switchToBlockState(level, context.getClickedPos(), (BlockState)newState.setValue(AXIS, (Axis)state.getValue(AXIS)));
         AugerShaftBlockEntity newBE = (AugerShaftBlockEntity)this.getBlockEntity(level, context.getClickedPos());
         if (newBE != null) {
            newBE.read(tag, reg, false);
            newBE.notifyUpdate();
            IWrenchable.playRotateSound(level, context.getClickedPos());
            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.PASS;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      Axis axis = (Axis)state.getValue(AXIS);
      Direction directionPos = Direction.get(AxisDirection.POSITIVE, axis);
      Direction directionNegative = Direction.get(AxisDirection.NEGATIVE, axis);
      BlockPos posPos = pos.relative(directionPos);
      BlockPos posNeg = pos.relative(directionNegative);
      BlockState statePos = level.getBlockState(posPos);
      BlockState stateNeg = level.getBlockState(posNeg);
      AugerShaftBlock.BarrelSection section = AugerShaftBlock.BarrelSection.SINGLE;
      if (this.connects(pos, state, posPos, statePos) && !this.connects(pos, state, posNeg, stateNeg)) {
         section = AugerShaftBlock.BarrelSection.END;
      } else if (!this.connects(pos, state, posPos, statePos) && this.connects(pos, state, posNeg, stateNeg)) {
         section = AugerShaftBlock.BarrelSection.FRONT;
      } else if (this.connects(pos, state, posPos, statePos) && this.connects(pos, state, posNeg, stateNeg)) {
         section = AugerShaftBlock.BarrelSection.MIDDLE;
      }

      BlockState mutState = (BlockState)state.setValue(SECTION, section);
      boolean isFunnel = neighborState.getBlock() instanceof AbstractFunnelBlock;
      boolean hasHorizontalFacing = neighborState.hasProperty(BlockStateProperties.HORIZONTAL_FACING);
      boolean hasFacing = neighborState.hasProperty(BlockStateProperties.FACING);
      if ((
            !isFunnel
               || (!hasHorizontalFacing || neighborState.getValue(BlockStateProperties.HORIZONTAL_FACING) != dir)
                  && (!hasFacing || neighborState.getValue(BlockStateProperties.FACING) != dir)
         )
         && (!dir.getAxis().isVertical() || !(neighborState.getBlock() instanceof ChuteBlock))) {
         mutState = (BlockState)mutState.setValue((Property)PROPERTY_BY_DIRECTION.get(dir), false);
      } else {
         mutState = (BlockState)mutState.setValue((Property)PROPERTY_BY_DIRECTION.get(dir), true);
      }

      return super.updateShape(mutState, dir, neighborState, level, pos, neighborPos);
   }

   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      AugerShaftBlock.BarrelSection section = (AugerShaftBlock.BarrelSection)state.getValue(SECTION);
      if (!(Boolean)state.getValue(ENCASED) && !section.equals(AugerShaftBlock.BarrelSection.SINGLE)) {
         Axis axis = (Axis)state.getValue(AXIS);
         return !section.equals(AugerShaftBlock.BarrelSection.MIDDLE)
            ? SimBlockShapes.AUGER_END_SHAPE
               .get(
                  section.equals(AugerShaftBlock.BarrelSection.FRONT)
                     ? Direction.get(AxisDirection.NEGATIVE, axis)
                     : Direction.get(AxisDirection.POSITIVE, axis)
               )
            : SimBlockShapes.FOURTEEN_VOXEL_POLE.get(axis);
      } else {
         return Shapes.block();
      }
   }

   protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
      super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
   }

   public Axis getRotationAxis(BlockState state) {
      return (Axis)state.getValue(AXIS);
   }

   public Class<AugerShaftBlockEntity> getBlockEntityClass() {
      return AugerShaftBlockEntity.class;
   }

   public BlockEntityType<? extends AugerShaftBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends AugerShaftBlockEntity>)SimBlockEntityTypes.AUGER_SHAFT.get();
   }

   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      return face.getAxis() == this.getRotationAxis(state);
   }

   public static enum BarrelSection implements StringRepresentable {
      FRONT,
      MIDDLE,
      END,
      SINGLE;

      public String getSerializedName() {
         return this.toString().toLowerCase(Locale.ROOT);
      }
   }

   @MethodsReturnNonnullByDefault
   private static class PlacementHelper extends PoleHelper<Axis> {
      private PlacementHelper() {
         super(
            state -> state.getBlock() instanceof AugerShaftBlock, state -> (Axis)state.getValue(RotatedPillarKineticBlock.AXIS), RotatedPillarKineticBlock.AXIS
         );
      }

      public Predicate<ItemStack> getItemPredicate() {
         return i -> {
            if (i.getItem() instanceof BlockItem bi && bi.getBlock() instanceof AugerShaftBlock) {
               return true;
            }

            return false;
         };
      }

      public Predicate<BlockState> getStatePredicate() {
         return Predicates.or(SimBlocks.AUGER_SHAFT::has, SimBlocks.AUGER_COG::has);
      }

      public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray) {
         return super.getOffset(player, world, state, pos, ray);
      }
   }
}

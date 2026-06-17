package com.simibubi.create.content.kinetics.simpleRelays.encased;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.contraption.transformable.TransformableBlock;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.decoration.encasing.EncasedBlock;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.block.IBE;
import java.util.function.Supplier;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class EncasedCogwheelBlock
   extends RotatedPillarKineticBlock
   implements ICogWheel,
   IBE<SimpleKineticBlockEntity>,
   SpecialBlockItemRequirement,
   TransformableBlock,
   EncasedBlock {
   public static final BooleanProperty TOP_SHAFT = BooleanProperty.create("top_shaft");
   public static final BooleanProperty BOTTOM_SHAFT = BooleanProperty.create("bottom_shaft");
   protected final boolean isLarge;
   private final Supplier<Block> casing;

   public EncasedCogwheelBlock(Properties properties, boolean large, Supplier<Block> casing) {
      super(properties);
      this.isLarge = large;
      this.casing = casing;
      this.registerDefaultState((BlockState)((BlockState)this.defaultBlockState().setValue(TOP_SHAFT, false)).setValue(BOTTOM_SHAFT, false));
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder.add(new Property[]{TOP_SHAFT, BOTTOM_SHAFT}));
   }

   public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
      if (target instanceof BlockHitResult) {
         return ((BlockHitResult)target).getDirection().getAxis() != this.getRotationAxis(state)
            ? (this.isLarge ? AllBlocks.LARGE_COGWHEEL.asStack() : AllBlocks.COGWHEEL.asStack())
            : this.getCasing().asItem().getDefaultInstance();
      } else {
         return super.getCloneItemStack(state, target, level, pos, player);
      }
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState placedOn = context.getLevel().getBlockState(context.getClickedPos().relative(context.getClickedFace().getOpposite()));
      BlockState stateForPlacement = super.getStateForPlacement(context);
      if (ICogWheel.isSmallCog(placedOn)) {
         stateForPlacement = (BlockState)stateForPlacement.setValue(AXIS, ((IRotate)placedOn.getBlock()).getRotationAxis(placedOn));
      }

      return stateForPlacement;
   }

   public boolean skipRendering(BlockState pState, BlockState pAdjacentBlockState, Direction pDirection) {
      return pState.getBlock() == pAdjacentBlockState.getBlock() && pState.getValue(AXIS) == pAdjacentBlockState.getValue(AXIS);
   }

   @Override
   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      if (context.getClickedFace().getAxis() != state.getValue(AXIS)) {
         return super.onWrenched(state, context);
      } else {
         Level level = context.getLevel();
         if (level.isClientSide) {
            return InteractionResult.SUCCESS;
         } else {
            BlockPos pos = context.getClickedPos();
            KineticBlockEntity.switchToBlockState(
               level, pos, (BlockState)state.cycle(context.getClickedFace().getAxisDirection() == AxisDirection.POSITIVE ? TOP_SHAFT : BOTTOM_SHAFT)
            );
            IWrenchable.playRotateSound(level, pos);
            return InteractionResult.SUCCESS;
         }
      }
   }

   @Override
   public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
      originalState = this.swapShaftsForRotation(originalState, Rotation.CLOCKWISE_90, targetedFace.getAxis());
      return (BlockState)originalState.setValue(
         RotatedPillarKineticBlock.AXIS,
         VoxelShaper.axisAsFace((Axis)originalState.getValue(RotatedPillarKineticBlock.AXIS)).getClockWise(targetedFace.getAxis()).getAxis()
      );
   }

   @Override
   public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
      if (context.getLevel().isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         context.getLevel().levelEvent(2001, context.getClickedPos(), Block.getId(state));
         KineticBlockEntity.switchToBlockState(
            context.getLevel(),
            context.getClickedPos(),
            (BlockState)(this.isLarge ? AllBlocks.LARGE_COGWHEEL : AllBlocks.COGWHEEL).getDefaultState().setValue(AXIS, (Axis)state.getValue(AXIS))
         );
         return InteractionResult.SUCCESS;
      }
   }

   @Override
   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      return face.getAxis() == state.getValue(AXIS) && (Boolean)state.getValue(face.getAxisDirection() == AxisDirection.POSITIVE ? TOP_SHAFT : BOTTOM_SHAFT);
   }

   @Override
   protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
      if (newState.getBlock() instanceof EncasedCogwheelBlock && oldState.getBlock() instanceof EncasedCogwheelBlock) {
         if (newState.getValue(TOP_SHAFT) != oldState.getValue(TOP_SHAFT)) {
            return false;
         }

         if (newState.getValue(BOTTOM_SHAFT) != oldState.getValue(BOTTOM_SHAFT)) {
            return false;
         }
      }

      return super.areStatesKineticallyEquivalent(oldState, newState);
   }

   @Override
   public boolean isSmallCog() {
      return !this.isLarge;
   }

   @Override
   public boolean isLargeCog() {
      return this.isLarge;
   }

   public boolean canSurvive(BlockState state, LevelReader worldIn, BlockPos pos) {
      return CogWheelBlock.isValidCogwheelPosition(ICogWheel.isLargeCog(state), worldIn, pos, (Axis)state.getValue(AXIS));
   }

   @Override
   public Axis getRotationAxis(BlockState state) {
      return (Axis)state.getValue(AXIS);
   }

   public BlockState swapShafts(BlockState state) {
      boolean bottom = (Boolean)state.getValue(BOTTOM_SHAFT);
      boolean top = (Boolean)state.getValue(TOP_SHAFT);
      state = (BlockState)state.setValue(BOTTOM_SHAFT, top);
      return (BlockState)state.setValue(TOP_SHAFT, bottom);
   }

   public BlockState swapShaftsForRotation(BlockState state, Rotation rotation, Axis rotationAxis) {
      if (rotation == Rotation.NONE) {
         return state;
      } else {
         Axis axis = (Axis)state.getValue(AXIS);
         if (axis == rotationAxis) {
            return state;
         } else if (rotation == Rotation.CLOCKWISE_180) {
            return this.swapShafts(state);
         } else {
            boolean clockwise = rotation == Rotation.CLOCKWISE_90;
            if (rotationAxis == Axis.X) {
               if (axis == Axis.Z && !clockwise || axis == Axis.Y && clockwise) {
                  return this.swapShafts(state);
               }
            } else if (rotationAxis == Axis.Y) {
               if (axis == Axis.X && !clockwise || axis == Axis.Z && clockwise) {
                  return this.swapShafts(state);
               }
            } else if (rotationAxis == Axis.Z && (axis == Axis.Y && !clockwise || axis == Axis.X && clockwise)) {
               return this.swapShafts(state);
            }

            return state;
         }
      }
   }

   public BlockState mirror(BlockState state, Mirror mirror) {
      Axis axis = (Axis)state.getValue(AXIS);
      return (axis != Axis.X || mirror != Mirror.FRONT_BACK) && (axis != Axis.Z || mirror != Mirror.LEFT_RIGHT) ? state : this.swapShafts(state);
   }

   @Override
   public BlockState rotate(BlockState state, Rotation rotation) {
      state = this.swapShaftsForRotation(state, rotation, Axis.Y);
      return super.rotate(state, rotation);
   }

   @Override
   public BlockState transform(BlockState state, StructureTransform transform) {
      if (transform.mirror != null) {
         state = this.mirror(state, transform.mirror);
      }

      if (transform.rotationAxis == Axis.Y) {
         return this.rotate(state, transform.rotation);
      } else {
         state = this.swapShaftsForRotation(state, transform.rotation, transform.rotationAxis);
         return (BlockState)state.setValue(AXIS, transform.rotateAxis((Axis)state.getValue(AXIS)));
      }
   }

   @Override
   public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
      return ItemRequirement.of(this.isLarge ? AllBlocks.LARGE_COGWHEEL.getDefaultState() : AllBlocks.COGWHEEL.getDefaultState(), be);
   }

   @Override
   public Class<SimpleKineticBlockEntity> getBlockEntityClass() {
      return SimpleKineticBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends SimpleKineticBlockEntity> getBlockEntityType() {
      return this.isLarge ? (BlockEntityType)AllBlockEntityTypes.ENCASED_LARGE_COGWHEEL.get() : (BlockEntityType)AllBlockEntityTypes.ENCASED_COGWHEEL.get();
   }

   @Override
   public Block getCasing() {
      return this.casing.get();
   }

   @Override
   public void handleEncasing(BlockState state, Level level, BlockPos pos, ItemStack heldItem, Player player, InteractionHand hand, BlockHitResult ray) {
      BlockState encasedState = (BlockState)this.defaultBlockState().setValue(AXIS, (Axis)state.getValue(AXIS));

      for (Direction d : Iterate.directionsInAxis((Axis)state.getValue(AXIS))) {
         BlockState adjacentState = level.getBlockState(pos.relative(d));
         Block var15 = adjacentState.getBlock();
         if (var15 instanceof IRotate) {
            IRotate def = (IRotate)var15;
            if (def.hasShaftTowards(level, pos.relative(d), adjacentState, d.getOpposite())) {
               encasedState = (BlockState)encasedState.cycle(d.getAxisDirection() == AxisDirection.POSITIVE ? TOP_SHAFT : BOTTOM_SHAFT);
            }
         }
      }

      KineticBlockEntity.switchToBlockState(level, pos, encasedState);
   }
}

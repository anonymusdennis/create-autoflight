package dev.simulated_team.simulated.content.blocks.altitude_sensor;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.multiloader.CommonRedstoneBlock;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AltitudeSensorBlock extends FaceAttachedHorizontalDirectionalBlock implements IBE<AltitudeSensorBlockEntity>, IWrenchable, CommonRedstoneBlock {
   public static final EnumProperty<AltitudeSensorBlock.FaceType> DIAL = EnumProperty.create("dial", AltitudeSensorBlock.FaceType.class);
   public static final MapCodec<AltitudeSensorBlock> CODEC = simpleCodec(AltitudeSensorBlock::new);

   public AltitudeSensorBlock(Properties pProperties) {
      super(pProperties);
   }

   @NotNull
   protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
      return CODEC;
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Direction facing = context.getHorizontalDirection().getOpposite();
      AttachFace face = AttachFace.FLOOR;
      if (context.getClickedFace() == Direction.DOWN) {
         face = AttachFace.CEILING;
      } else if (context.getClickedFace().getAxis().isHorizontal()) {
         face = AttachFace.WALL;
         facing = context.getClickedFace();
      }

      return (BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing)).setValue(FACE, face))
         .setValue(DIAL, AltitudeSensorBlock.FaceType.LINEAR);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
      pBuilder.add(new Property[]{FACING, FACE, DIAL});
      super.createBlockStateDefinition(pBuilder);
   }

   @NotNull
   public VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
      if (state.getValue(FACE) == AttachFace.FLOOR) {
         return SimBlockShapes.ALTITUDE_SENSOR_FLOOR.get((Direction)state.getValue(BlockStateProperties.HORIZONTAL_FACING));
      } else {
         return state.getValue(FACE) == AttachFace.CEILING
            ? SimBlockShapes.ALTITUDE_SENSOR_CEILING.get((Direction)state.getValue(BlockStateProperties.HORIZONTAL_FACING))
            : SimBlockShapes.ALTITUDE_SENSOR_WALL.get((Direction)state.getValue(BlockStateProperties.HORIZONTAL_FACING));
      }
   }

   public Class<AltitudeSensorBlockEntity> getBlockEntityClass() {
      return AltitudeSensorBlockEntity.class;
   }

   public BlockEntityType<? extends AltitudeSensorBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends AltitudeSensorBlockEntity>)SimBlockEntityTypes.ALTITUDE_SENSOR.get();
   }

   public boolean isSignalSource(BlockState state) {
      return true;
   }

   public int getSignal(@NotNull BlockState state, BlockGetter level, @NotNull BlockPos pos, @NotNull Direction direction) {
      AltitudeSensorBlockEntity be = (AltitudeSensorBlockEntity)level.getBlockEntity(pos);
      return be.signal;
   }

   protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
      return direction != Direction.UP ? 0 : this.getSignal(state, level, pos, direction);
   }

   @Override
   public boolean commonConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
      return direction != null;
   }

   @Override
   public boolean commonCheckWeakPower(BlockState state, SignalGetter level, BlockPos pos, Direction side) {
      return true;
   }

   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      if (context.getClickedFace() == state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
         IWrenchable.playRotateSound(context.getLevel(), context.getClickedPos());
         AltitudeSensorBlock.FaceType faceType = (AltitudeSensorBlock.FaceType)state.getValue(DIAL);
         if (faceType == AltitudeSensorBlock.FaceType.LINEAR) {
            faceType = AltitudeSensorBlock.FaceType.RADIAL;
         } else {
            faceType = AltitudeSensorBlock.FaceType.LINEAR;
         }

         context.getLevel().setBlock(context.getClickedPos(), (BlockState)state.setValue(DIAL, faceType), 3);
         return InteractionResult.SUCCESS;
      } else {
         return super.onWrenched(state, context);
      }
   }

   @NotNull
   protected ItemInteractionResult useItemOn(
      @NotNull ItemStack stack,
      @NotNull BlockState state,
      @NotNull Level level,
      @NotNull BlockPos pos,
      @NotNull Player player,
      @NotNull InteractionHand hand,
      @NotNull BlockHitResult hitResult
   ) {
      return AllItems.WRENCH.isIn(stack) ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION : this.onBlockEntityUseItemOn(level, pos, be -> {
         if (level.isClientSide) {
            this.withBlockEntityDo(level, pos, AltitudeSensorScreen::open);
         }

         return ItemInteractionResult.SUCCESS;
      });
   }

   public static enum FaceType implements StringRepresentable {
      LINEAR,
      RADIAL;

      @NotNull
      public String getSerializedName() {
         return this.toString().toLowerCase(Locale.ROOT);
      }
   }
}

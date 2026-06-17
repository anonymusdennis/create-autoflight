package dev.eriksonn.aeronautics.content.blocks.hot_air.steam_vent;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.eriksonn.aeronautics.index.AeroBlockEntityTypes;
import dev.eriksonn.aeronautics.index.AeroBlockShapes;
import dev.eriksonn.aeronautics.index.AeroTags;
import java.util.Locale;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class SteamVentBlock extends Block implements IBE<SteamVentBlockEntity>, SimpleWaterloggedBlock, IWrenchable {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
   public static final EnumProperty<SteamVentBlock.Variant> VARIANT = EnumProperty.create("variant", SteamVentBlock.Variant.class);

   public SteamVentBlock(Properties pProperties) {
      super(pProperties);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(BlockStateProperties.WATERLOGGED, false)).setValue(POWERED, false)
      );
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder.add(new Property[]{POWERED, BlockStateProperties.WATERLOGGED, VARIANT, FACING}));
   }

   @NotNull
   protected ItemInteractionResult useItemOn(
      ItemStack itemStack,
      @NotNull BlockState blockState,
      @NotNull Level level,
      @NotNull BlockPos blockPos,
      @NotNull Player player,
      @NotNull InteractionHand interactionHand,
      @NotNull BlockHitResult blockHitResult
   ) {
      SteamVentBlock.Variant conversion = SteamVentBlock.Variant.getConversionFromItem(itemStack.getItem());
      if (conversion != null) {
         SteamVentBlock.Variant current = (SteamVentBlock.Variant)blockState.getValue(VARIANT);
         if (conversion != current) {
            level.setBlockAndUpdate(blockPos, (BlockState)blockState.setValue(VARIANT, conversion));
            level.playLocalSound(
               (double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ(), SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F, false
            );
            return ItemInteractionResult.SUCCESS;
         }
      }

      return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
   }

   public void onPlace(@NotNull BlockState pState, @NotNull Level pLevel, BlockPos pPos, @NotNull BlockState pOldState, boolean pIsMoving) {
      FluidTankBlock.updateBoilerState(pState, pLevel, pPos.relative(Direction.DOWN));
      this.withBlockEntityDo(pLevel, pPos, SteamVentBlockEntity::getAndCacheTank);
      this.withBlockEntityDo(pLevel, pPos, x -> {
         if (!x.updateRawSignal()) {
            x.signalSync();
         }
      });
   }

   public void onRemove(BlockState pState, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean pIsMoving) {
      this.withBlockEntityDo(level, pos, x -> x.rawSignalStrength = 0);
      if (pState.hasBlockEntity() && (!pState.is(newState.getBlock()) || !newState.hasBlockEntity())) {
         level.removeBlockEntity(pos);
      }

      for (Direction dir : Iterate.directions) {
         if (level.getBlockEntity(pos.relative(dir)) instanceof SteamVentBlockEntity vent) {
            vent.signalSync();
         }
      }

      FluidTankBlock.updateBoilerState(pState, level, pos.relative(Direction.DOWN));
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      return (BlockState)((BlockState)((BlockState)super.getStateForPlacement(context).setValue(POWERED, level.hasNeighborSignal(pos)))
            .setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER))
         .setValue(FACING, context.getPlayer().isShiftKeyDown() ? context.getHorizontalDirection().getOpposite() : context.getHorizontalDirection());
   }

   public void neighborChanged(BlockState state, Level level, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
      if (!level.isClientSide) {
         this.withBlockEntityDo(level, pos, SteamVentBlockEntity::updateRawSignal);
      }
   }

   public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      return SteamEngineBlock.canAttach(pLevel, pPos, Direction.DOWN);
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return AeroBlockShapes.STEAM_VENT.get(Axis.Y);
   }

   public FluidState getFluidState(BlockState state) {
      return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
   }

   public Class<SteamVentBlockEntity> getBlockEntityClass() {
      return SteamVentBlockEntity.class;
   }

   public BlockEntityType<? extends SteamVentBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends SteamVentBlockEntity>)AeroBlockEntityTypes.STEAM_VENT.get();
   }

   public static enum Variant implements StringRepresentable {
      GOLD,
      IRON;

      public static SteamVentBlock.Variant getConversionFromItem(Item item) {
         if (item.builtInRegistryHolder().is(AeroTags.ItemTags.GOLD_SHEET)) {
            return GOLD;
         } else {
            return item.builtInRegistryHolder().is(AeroTags.ItemTags.IRON_SHEET) ? IRON : null;
         }
      }

      public String getSerializedName() {
         return this.toString().toLowerCase(Locale.ROOT);
      }
   }
}

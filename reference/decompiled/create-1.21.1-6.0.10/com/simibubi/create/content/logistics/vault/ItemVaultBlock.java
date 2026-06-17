package com.simibubi.create.content.logistics.vault;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import org.jetbrains.annotations.Nullable;

public class ItemVaultBlock extends Block implements IWrenchable, IBE<ItemVaultBlockEntity> {
   public static final Property<Axis> HORIZONTAL_AXIS = BlockStateProperties.HORIZONTAL_AXIS;
   public static final BooleanProperty LARGE = BooleanProperty.create("large");
   public static final SoundType SILENCED_METAL = new DeferredSoundType(
      0.1F,
      1.5F,
      () -> SoundEvents.NETHERITE_BLOCK_BREAK,
      () -> SoundEvents.NETHERITE_BLOCK_STEP,
      () -> SoundEvents.NETHERITE_BLOCK_PLACE,
      () -> SoundEvents.NETHERITE_BLOCK_HIT,
      () -> SoundEvents.NETHERITE_BLOCK_FALL
   );

   public ItemVaultBlock(Properties p_i48440_1_) {
      super(p_i48440_1_);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(LARGE, false));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
      pBuilder.add(new Property[]{HORIZONTAL_AXIS, LARGE});
      super.createBlockStateDefinition(pBuilder);
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      if (pContext.getPlayer() == null || !pContext.getPlayer().isShiftKeyDown()) {
         BlockState placedOn = pContext.getLevel().getBlockState(pContext.getClickedPos().relative(pContext.getClickedFace().getOpposite()));
         Axis preferredAxis = getVaultBlockAxis(placedOn);
         if (preferredAxis != null) {
            return (BlockState)this.defaultBlockState().setValue(HORIZONTAL_AXIS, preferredAxis);
         }
      }

      return (BlockState)this.defaultBlockState().setValue(HORIZONTAL_AXIS, pContext.getHorizontalDirection().getAxis());
   }

   public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
      if (pOldState.getBlock() != pState.getBlock()) {
         if (!pIsMoving) {
            this.withBlockEntityDo(pLevel, pPos, ItemVaultBlockEntity::updateConnectivity);
         }
      }
   }

   @Override
   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      if (context.getClickedFace().getAxis().isVertical()) {
         if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof ItemVaultBlockEntity vault) {
            ConnectivityHandler.splitMulti(vault);
            vault.removeController(true);
         }

         state = (BlockState)state.setValue(LARGE, false);
      }

      return IWrenchable.super.onWrenched(state, context);
   }

   public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean pIsMoving) {
      if (state.hasBlockEntity() && (state.getBlock() != newState.getBlock() || !newState.hasBlockEntity())) {
         if (!(world.getBlockEntity(pos) instanceof ItemVaultBlockEntity vaultBE)) {
            return;
         }

         ItemHelper.dropContents(world, pos, vaultBE.inventory);
         world.removeBlockEntity(pos);
         ConnectivityHandler.splitMulti(vaultBE);
      }
   }

   public static boolean isVault(BlockState state) {
      return AllBlocks.ITEM_VAULT.has(state);
   }

   @Nullable
   public static Axis getVaultBlockAxis(BlockState state) {
      return !isVault(state) ? null : (Axis)state.getValue(HORIZONTAL_AXIS);
   }

   public static boolean isLarge(BlockState state) {
      return !isVault(state) ? false : (Boolean)state.getValue(LARGE);
   }

   public BlockState rotate(BlockState state, Rotation rot) {
      Axis axis = (Axis)state.getValue(HORIZONTAL_AXIS);
      return (BlockState)state.setValue(HORIZONTAL_AXIS, rot.rotate(Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE)).getAxis());
   }

   public BlockState mirror(BlockState state, Mirror mirrorIn) {
      return state;
   }

   public SoundType getSoundType(BlockState state, LevelReader world, BlockPos pos, Entity entity) {
      SoundType soundType = super.getSoundType(state, world, pos, entity);
      return entity != null && entity.getPersistentData().contains("SilenceVaultSound") ? SILENCED_METAL : soundType;
   }

   public boolean hasAnalogOutputSignal(BlockState p_149740_1_) {
      return true;
   }

   public int getAnalogOutputSignal(BlockState pState, Level pLevel, BlockPos pPos) {
      return ItemHelper.calcRedstoneFromBlockEntity(this, pLevel, pPos);
   }

   @Override
   public BlockEntityType<? extends ItemVaultBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends ItemVaultBlockEntity>)AllBlockEntityTypes.ITEM_VAULT.get();
   }

   @Override
   public Class<ItemVaultBlockEntity> getBlockEntityClass() {
      return ItemVaultBlockEntity.class;
   }
}

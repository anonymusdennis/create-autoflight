package com.simibubi.create.content.trains.signal;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.Optional;
import net.createmod.catnip.lang.Lang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

public class SignalBlock extends Block implements IBE<SignalBlockEntity>, IWrenchable {
   public static final EnumProperty<SignalBlock.SignalType> TYPE = EnumProperty.create("type", SignalBlock.SignalType.class);
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

   public SignalBlock(Properties p_53182_) {
      super(p_53182_);
      this.registerDefaultState((BlockState)((BlockState)this.defaultBlockState().setValue(TYPE, SignalBlock.SignalType.ENTRY_SIGNAL)).setValue(POWERED, false));
   }

   @Override
   public Class<SignalBlockEntity> getBlockEntityClass() {
      return SignalBlockEntity.class;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
      super.createBlockStateDefinition(pBuilder.add(new Property[]{TYPE, POWERED}));
   }

   public boolean shouldCheckWeakPower(BlockState state, SignalGetter level, BlockPos pos, Direction side) {
      return false;
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      return (BlockState)this.defaultBlockState().setValue(POWERED, pContext.getLevel().hasNeighborSignal(pContext.getClickedPos()));
   }

   public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
      if (!pLevel.isClientSide) {
         boolean powered = (Boolean)pState.getValue(POWERED);
         Optional<SignalBlockEntity> ste = this.getBlockEntityOptional(pLevel, pPos);
         boolean neighborPowered = false;
         if (ste.isEmpty() || !ste.get().computerBehaviour.hasAttachedComputer()) {
            neighborPowered = pLevel.hasNeighborSignal(pPos);
         }

         if (powered != neighborPowered) {
            if (powered) {
               pLevel.scheduleTick(pPos, this, 4);
            } else {
               pLevel.setBlock(pPos, (BlockState)pState.cycle(POWERED), 2);
            }
         }
      }
   }

   public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRand) {
      Optional<SignalBlockEntity> ste = this.getBlockEntityOptional(pLevel, pPos);
      if ((ste.isEmpty() || !ste.get().computerBehaviour.hasAttachedComputer()) && (Boolean)pState.getValue(POWERED) && !pLevel.hasNeighborSignal(pPos)) {
         pLevel.setBlock(pPos, (BlockState)pState.cycle(POWERED), 2);
      }
   }

   public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
      IBE.onRemove(state, worldIn, pos, newState);
   }

   @Override
   public BlockEntityType<? extends SignalBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends SignalBlockEntity>)AllBlockEntityTypes.TRACK_SIGNAL.get();
   }

   @Override
   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      if (level.isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         this.withBlockEntityDo(level, pos, ste -> {
            Player player = context.getPlayer();
            if (ste.computerBehaviour.hasAttachedComputer()) {
               if (player != null) {
                  player.displayClientMessage(CreateLang.translateDirect("track_signal.mode_controlled_by_computer"), true);
               }
            } else {
               SignalBoundary signal = ste.getSignal();
               if (signal != null) {
                  signal.cycleSignalType(pos);
                  if (player != null) {
                     player.displayClientMessage(CreateLang.translateDirect("track_signal.mode_change." + signal.getTypeFor(pos).getSerializedName()), true);
                  }
               } else if (player != null) {
                  player.displayClientMessage(CreateLang.translateDirect("track_signal.cannot_change_mode"), true);
               }
            }
         });
         return InteractionResult.SUCCESS;
      }
   }

   public boolean hasAnalogOutputSignal(BlockState pState) {
      return true;
   }

   public int getAnalogOutputSignal(BlockState pState, Level blockAccess, BlockPos pPos) {
      return this.getBlockEntityOptional(blockAccess, pPos).filter(SignalBlockEntity::isPowered).map($ -> 15).orElse(0);
   }

   public static enum SignalType implements StringRepresentable {
      ENTRY_SIGNAL,
      CROSS_SIGNAL;

      public String getSerializedName() {
         return Lang.asId(this.name());
      }
   }
}

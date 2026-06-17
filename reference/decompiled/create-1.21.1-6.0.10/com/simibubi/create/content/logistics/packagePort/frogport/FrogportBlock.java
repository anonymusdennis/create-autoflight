package com.simibubi.create.content.logistics.packagePort.frogport;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class FrogportBlock extends Block implements IBE<FrogportBlockEntity>, IWrenchable {
   public FrogportBlock(Properties pProperties) {
      super(pProperties);
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return AllShapes.PACKAGE_PORT;
   }

   @Nullable
   public PushReaction getPistonPushReaction(BlockState state) {
      return PushReaction.NORMAL;
   }

   public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
      super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
      if (pPlacer != null) {
         AdvancementBehaviour.setPlacedBy(pLevel, pPos, pPlacer);
         this.withBlockEntityDo(pLevel, pPos, be -> {
            Vec3 diff = VecHelper.getCenterOf(pPos).subtract(pPlacer.position());
            be.passiveYaw = (float)(Mth.atan2(diff.x, diff.z) * 180.0F / (float)Math.PI);
            be.passiveYaw = (float)Math.round(be.passiveYaw / 11.25F) * 11.25F;
            be.notifyUpdate();
         });
      }
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      return this.onBlockEntityUseItemOn(level, pos, be -> be.use(player));
   }

   @Override
   public Class<FrogportBlockEntity> getBlockEntityClass() {
      return FrogportBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends FrogportBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends FrogportBlockEntity>)AllBlockEntityTypes.PACKAGE_FROGPORT.get();
   }

   public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
      IBE.onRemove(pState, pLevel, pPos, pNewState);
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   public boolean hasAnalogOutputSignal(BlockState pState) {
      return true;
   }

   public int getAnalogOutputSignal(BlockState pState, Level pLevel, BlockPos pPos) {
      return this.getBlockEntityOptional(pLevel, pPos).map(pbe -> pbe.getComparatorOutput()).orElse(0);
   }
}

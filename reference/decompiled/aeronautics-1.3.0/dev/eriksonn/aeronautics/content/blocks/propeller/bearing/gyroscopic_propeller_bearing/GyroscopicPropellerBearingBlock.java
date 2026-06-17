package dev.eriksonn.aeronautics.content.blocks.propeller.bearing.gyroscopic_propeller_bearing;

import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.eriksonn.aeronautics.data.AeroLang;
import dev.eriksonn.aeronautics.index.AeroBlockEntityTypes;
import dev.eriksonn.aeronautics.index.AeroBlockShapes;
import dev.simulated_team.simulated.api.CustomStressImpactTooltipProvider;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GyroscopicPropellerBearingBlock extends BearingBlock implements IBE<GyroscopicPropellerBearingBlockEntity>, CustomStressImpactTooltipProvider {
   public GyroscopicPropellerBearingBlock(Properties properties) {
      super(properties);
   }

   public LangBuilder getCustomImpactLang() {
      return AeroLang.translate("propeller.sails");
   }

   public int getBarLength() {
      return 3;
   }

   public int getFilledBarLength() {
      return 3;
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (!player.mayBuild()) {
         return ItemInteractionResult.FAIL;
      } else if (player.isShiftKeyDown()) {
         return ItemInteractionResult.FAIL;
      } else if (stack.isEmpty()) {
         if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
         } else {
            this.withBlockEntityDo(level, pos, te -> {
               if (te.isRunning()) {
                  te.startDisassemblySlowdown();
               } else {
                  te.setAssembleNextTick(true);
               }
            });
            return ItemInteractionResult.SUCCESS;
         }
      } else {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      InteractionResult result = super.onWrenched(state, context);
      Level level = context.getLevel();
      if (level.isClientSide && result.consumesAction()) {
         BlockState newState = this.getRotatedBlockState(state, context.getClickedFace());
         level.setBlock(context.getClickedPos(), newState, 2);
         this.withBlockEntityDo(context.getLevel(), context.getClickedPos(), be -> be.forceTilt(newState));
      }

      return result;
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext ctx) {
      return AeroBlockShapes.PROPELLER_BEARING.get((Direction)pState.getValue(FACING));
   }

   public Class<GyroscopicPropellerBearingBlockEntity> getBlockEntityClass() {
      return GyroscopicPropellerBearingBlockEntity.class;
   }

   public BlockEntityType<? extends GyroscopicPropellerBearingBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends GyroscopicPropellerBearingBlockEntity>)AeroBlockEntityTypes.GYROSCOPIC_PROPELLER_BEARING.get();
   }
}

package dev.eriksonn.aeronautics.content.blocks.mounted_potato_cannon;

import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import dev.eriksonn.aeronautics.index.AeroBlockEntityTypes;
import dev.eriksonn.aeronautics.index.AeroBlockShapes;
import dev.simulated_team.simulated.multiloader.inventory.ContainerSlot;
import dev.simulated_team.simulated.multiloader.inventory.ItemInfoWrapper;
import dev.simulated_team.simulated.util.DirectionalAxisShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MountedPotatoCannonBlock extends DirectionalAxisKineticBlock implements IBE<MountedPotatoCannonBlockEntity> {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final BooleanProperty BLOCKED = BooleanProperty.create("blocked");
   DirectionalAxisShaper MOUNTED_POTATO_CANNON = DirectionalAxisShaper.make(AeroBlockShapes.MOUNTED_POTATO_CANNON);
   DirectionalAxisShaper MOUNTED_POTATO_CANNON_BLOCKED = DirectionalAxisShaper.make(AeroBlockShapes.MOUNTED_POTATO_CANNON_BLOCKED);

   public MountedPotatoCannonBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(BLOCKED, false));
   }

   public Class<MountedPotatoCannonBlockEntity> getBlockEntityClass() {
      return MountedPotatoCannonBlockEntity.class;
   }

   public BlockEntityType<? extends MountedPotatoCannonBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends MountedPotatoCannonBlockEntity>)AeroBlockEntityTypes.MOUNTED_POTATO_CANNON.get();
   }

   protected ItemInteractionResult useItemOn(
      ItemStack heldItem, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult
   ) {
      if (level.getBlockEntity(blockPos) instanceof MountedPotatoCannonBlockEntity be) {
         ContainerSlot slot = be.getInventory().slot;
         if (heldItem.isEmpty() && slot.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         }

         ItemInfoWrapper info = ItemInfoWrapper.generateFromStack(heldItem);
         if (slot.isEmpty() || slot.getType() == heldItem.getItem()) {
            long inserted = (long)slot.insertStack(info, Math.min(heldItem.getCount(), 16), true);
            if (inserted > 0L) {
               if (!level.isClientSide) {
                  slot.insertStack(info, Math.min(heldItem.getCount(), 16), false);
               }

               if (!player.hasInfiniteMaterials()) {
                  heldItem.shrink((int)inserted);
               }

               return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
         }

         if (slot.getType() != heldItem.getItem() && slot.canInsert(info)) {
            ItemStack extracted = slot.getStack().copy();
            player.getInventory().placeItemBackInInventory(extracted);
            slot.setStack(ItemStack.EMPTY);
            if (!level.isClientSide) {
               long inserted = (long)slot.insertStack(info, Math.min(heldItem.getCount(), 16), false);
               heldItem.shrink((int)inserted);
            }

            return ItemInteractionResult.SUCCESS;
         }
      }

      return super.useItemOn(heldItem, blockState, level, blockPos, player, interactionHand, blockHitResult);
   }

   public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
      if (!level.isClientSide) {
         boolean previouslyPowered = (Boolean)state.getValue(POWERED);
         if (previouslyPowered != level.hasNeighborSignal(pos)) {
            level.setBlock(pos, (BlockState)state.cycle(POWERED), 2);
         }

         this.withBlockEntityDo(level, pos, SyncedBlockEntity::sendData);
      }
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{POWERED, BLOCKED});
      super.createBlockStateDefinition(builder);
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)super.getStateForPlacement(context).setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
   }

   public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
      return state.getValue(BLOCKED)
         ? this.MOUNTED_POTATO_CANNON_BLOCKED.get((Direction)state.getValue(FACING), (Boolean)state.getValue(AXIS_ALONG_FIRST_COORDINATE))
         : this.MOUNTED_POTATO_CANNON.get((Direction)state.getValue(FACING), (Boolean)state.getValue(AXIS_ALONG_FIRST_COORDINATE));
   }
}

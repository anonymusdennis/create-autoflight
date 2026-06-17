package com.simibubi.create.content.equipment.clipboard;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import java.util.List;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.NotNull;

public class ClipboardBlock extends FaceAttachedHorizontalDirectionalBlock implements IBE<ClipboardBlockEntity>, IWrenchable, ProperWaterloggedBlock {
   public static final BooleanProperty WRITTEN = BooleanProperty.create("written");
   public static final MapCodec<ClipboardBlock> CODEC = simpleCodec(ClipboardBlock::new);

   public ClipboardBlock(Properties pProperties) {
      super(pProperties);
      this.registerDefaultState((BlockState)((BlockState)this.defaultBlockState().setValue(WATERLOGGED, false)).setValue(WRITTEN, false));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
      super.createBlockStateDefinition(pBuilder.add(new Property[]{WRITTEN, FACE, FACING, WATERLOGGED}));
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      BlockState stateForPlacement = super.getStateForPlacement(pContext);
      if (stateForPlacement == null) {
         return null;
      } else {
         if (stateForPlacement.getValue(FACE) != AttachFace.WALL) {
            stateForPlacement = (BlockState)stateForPlacement.setValue(FACING, ((Direction)stateForPlacement.getValue(FACING)).getOpposite());
         }

         return (BlockState)this.withWater(stateForPlacement, pContext).setValue(WRITTEN, !pContext.getItemInHand().isComponentsPatchEmpty());
      }
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return (switch ((AttachFace)pState.getValue(FACE)) {
         case FLOOR -> AllShapes.CLIPBOARD_FLOOR;
         case CEILING -> AllShapes.CLIPBOARD_CEILING;
         default -> AllShapes.CLIPBOARD_WALL;
      }).get((Direction)pState.getValue(FACING));
   }

   public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      return !pLevel.getBlockState(pPos.relative(getConnectedDirection(pState).getOpposite())).canBeReplaced();
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (player.isShiftKeyDown()) {
         this.breakAndCollect(state, level, pos, player);
         return InteractionResult.SUCCESS;
      } else {
         return this.onBlockEntityUse(level, pos, cbe -> {
            if (level.isClientSide()) {
               CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> this.openScreen(player, cbe.components(), pos));
            }

            return InteractionResult.SUCCESS;
         });
      }
   }

   @OnlyIn(Dist.CLIENT)
   private void openScreen(Player player, DataComponentMap components, BlockPos pos) {
      if (Minecraft.getInstance().player == player) {
         ScreenOpener.open(new ClipboardScreen(player.getInventory().selected, components, pos));
      }
   }

   public void attack(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
      this.breakAndCollect(pState, pLevel, pPos, pPlayer);
   }

   private void breakAndCollect(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
      if (!(pPlayer instanceof FakePlayer)) {
         if (!pLevel.isClientSide) {
            ItemStack cloneItemStack = this.getCloneItemStack(pLevel, pPos, pState);
            pLevel.destroyBlock(pPos, false);
            if (pLevel.getBlockState(pPos) != pState) {
               Inventory inv = pPlayer.getInventory();
               ItemStack selected = inv.getSelected();
               if (selected.isEmpty()) {
                  inv.setItem(inv.selected, cloneItemStack);
               } else {
                  inv.placeItemBackInInventory(cloneItemStack);
               }
            }
         }
      }
   }

   public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
      return this.applyComponentsToDropStack(new ItemStack(this), level.getBlockEntity(pos));
   }

   public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
      if (level.getBlockEntity(pos) instanceof ClipboardBlockEntity cbe) {
         if (!level.isClientSide && !player.isCreative()) {
            Block.popResource(level, pos, this.applyComponentsToDropStack(new ItemStack(this), cbe));
            return state;
         } else {
            return state;
         }
      } else {
         return state;
      }
   }

   public List<ItemStack> getDrops(BlockState pState, net.minecraft.world.level.storage.loot.LootParams.Builder pBuilder) {
      if (pBuilder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof ClipboardBlockEntity cbe) {
         ItemStack var5 = this.applyComponentsToDropStack(new ItemStack(this), cbe);
         pBuilder.withDynamicDrop(ShulkerBoxBlock.CONTENTS, c -> c.accept(var5.copy()));
         return ImmutableList.of(var5);
      } else {
         return super.getDrops(pState, pBuilder);
      }
   }

   private ItemStack applyComponentsToDropStack(ItemStack stack, BlockEntity blockEntity) {
      if (blockEntity instanceof ClipboardBlockEntity cbe) {
         stack.applyComponents(cbe.components());
         return stack;
      } else {
         return stack;
      }
   }

   public FluidState getFluidState(BlockState pState) {
      return this.fluidState(pState);
   }

   public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
      this.updateWater(pLevel, pState, pCurrentPos);
      return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
   }

   @Override
   public Class<ClipboardBlockEntity> getBlockEntityClass() {
      return ClipboardBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends ClipboardBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends ClipboardBlockEntity>)AllBlockEntityTypes.CLIPBOARD.get();
   }

   @NotNull
   protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
      return CODEC;
   }
}

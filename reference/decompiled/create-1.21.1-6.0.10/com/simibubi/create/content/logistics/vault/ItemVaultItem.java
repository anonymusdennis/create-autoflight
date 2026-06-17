package com.simibubi.create.content.logistics.vault;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.symmetryWand.SymmetryWandItem;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ItemVaultItem extends BlockItem {
   public ItemVaultItem(Block p_i48527_1_, Properties p_i48527_2_) {
      super(p_i48527_1_, p_i48527_2_);
   }

   public InteractionResult place(BlockPlaceContext ctx) {
      InteractionResult initialResult = super.place(ctx);
      if (!initialResult.consumesAction()) {
         return initialResult;
      } else {
         this.tryMultiPlace(ctx);
         return initialResult;
      }
   }

   protected boolean updateCustomBlockEntityTag(BlockPos blockPos, Level level, Player player, ItemStack itemStack, BlockState blockState) {
      MinecraftServer minecraftserver = level.getServer();
      if (minecraftserver == null) {
         return false;
      } else {
         CustomData blockEntityData = (CustomData)itemStack.get(DataComponents.BLOCK_ENTITY_DATA);
         if (blockEntityData != null) {
            CompoundTag nbt = blockEntityData.copyTag();
            nbt.remove("Length");
            nbt.remove("Size");
            nbt.remove("Controller");
            nbt.remove("LastKnownPos");
            BlockEntity.addEntityType(nbt, ((IBE)this.getBlock()).getBlockEntityType());
            itemStack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(nbt));
         }

         return super.updateCustomBlockEntityTag(blockPos, level, player, itemStack, blockState);
      }
   }

   private void tryMultiPlace(BlockPlaceContext ctx) {
      Player player = ctx.getPlayer();
      if (player != null) {
         if (!player.isShiftKeyDown()) {
            Direction face = ctx.getClickedFace();
            ItemStack stack = ctx.getItemInHand();
            Level world = ctx.getLevel();
            BlockPos pos = ctx.getClickedPos();
            BlockPos placedOnPos = pos.relative(face.getOpposite());
            BlockState placedOnState = world.getBlockState(placedOnPos);
            if (ItemVaultBlock.isVault(placedOnState)) {
               if (!SymmetryWandItem.presentInHotbar(player)) {
                  ItemVaultBlockEntity tankAt = ConnectivityHandler.partAt((BlockEntityType<?>)AllBlockEntityTypes.ITEM_VAULT.get(), world, placedOnPos);
                  if (tankAt != null) {
                     ItemVaultBlockEntity controllerBE = tankAt.getControllerBE();
                     if (controllerBE != null) {
                        int width = controllerBE.radius;
                        if (width != 1) {
                           int tanksToPlace = 0;
                           Axis vaultBlockAxis = ItemVaultBlock.getVaultBlockAxis(placedOnState);
                           if (vaultBlockAxis != null) {
                              if (face.getAxis() == vaultBlockAxis) {
                                 Direction vaultFacing = Direction.fromAxisAndDirection(vaultBlockAxis, AxisDirection.POSITIVE);
                                 BlockPos startPos = face == vaultFacing.getOpposite()
                                    ? controllerBE.getBlockPos().relative(vaultFacing.getOpposite())
                                    : controllerBE.getBlockPos().relative(vaultFacing, controllerBE.length);
                                 if (VecHelper.getCoordinate(startPos, vaultBlockAxis) == VecHelper.getCoordinate(pos, vaultBlockAxis)) {
                                    for (int xOffset = 0; xOffset < width; xOffset++) {
                                       for (int zOffset = 0; zOffset < width; zOffset++) {
                                          BlockPos offsetPos = vaultBlockAxis == Axis.X
                                             ? startPos.offset(0, xOffset, zOffset)
                                             : startPos.offset(xOffset, zOffset, 0);
                                          BlockState blockState = world.getBlockState(offsetPos);
                                          if (!ItemVaultBlock.isVault(blockState)) {
                                             if (!blockState.canBeReplaced()) {
                                                return;
                                             }

                                             tanksToPlace++;
                                          }
                                       }
                                    }

                                    if (player.isCreative() || stack.getCount() >= tanksToPlace) {
                                       for (int xOffset = 0; xOffset < width; xOffset++) {
                                          for (int zOffsetx = 0; zOffsetx < width; zOffsetx++) {
                                             BlockPos offsetPos = vaultBlockAxis == Axis.X
                                                ? startPos.offset(0, xOffset, zOffsetx)
                                                : startPos.offset(xOffset, zOffsetx, 0);
                                             BlockState blockState = world.getBlockState(offsetPos);
                                             if (!ItemVaultBlock.isVault(blockState)) {
                                                BlockPlaceContext context = BlockPlaceContext.at(ctx, offsetPos, face);
                                                player.getPersistentData().putBoolean("SilenceVaultSound", true);
                                                super.place(context);
                                                player.getPersistentData().remove("SilenceVaultSound");
                                             }
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }
}

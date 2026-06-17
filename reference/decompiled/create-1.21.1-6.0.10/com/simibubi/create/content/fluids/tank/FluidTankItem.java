package com.simibubi.create.content.fluids.tank;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.symmetryWand.SymmetryWandItem;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.neoforged.neoforge.fluids.FluidStack;

public class FluidTankItem extends BlockItem {
   public FluidTankItem(Block p_i48527_1_, Properties p_i48527_2_) {
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
            nbt.remove("Luminosity");
            nbt.remove("Size");
            nbt.remove("Height");
            nbt.remove("Controller");
            nbt.remove("LastKnownPos");
            if (nbt.contains("TankContent")) {
               FluidStack fluid = FluidStack.parseOptional(minecraftserver.registryAccess(), nbt.getCompound("TankContent"));
               if (!fluid.isEmpty()) {
                  fluid.setAmount(Math.min(FluidTankBlockEntity.getCapacityMultiplier(), fluid.getAmount()));
                  nbt.put("TankContent", fluid.saveOptional(minecraftserver.registryAccess()));
               }
            }

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
            if (face.getAxis().isVertical()) {
               ItemStack stack = ctx.getItemInHand();
               Level world = ctx.getLevel();
               BlockPos pos = ctx.getClickedPos();
               BlockPos placedOnPos = pos.relative(face.getOpposite());
               BlockState placedOnState = world.getBlockState(placedOnPos);
               if (FluidTankBlock.isTank(placedOnState)) {
                  if (!SymmetryWandItem.presentInHotbar(player)) {
                     boolean creative = this.getBlock().equals(AllBlocks.CREATIVE_FLUID_TANK.get());
                     FluidTankBlockEntity tankAt = ConnectivityHandler.partAt(
                        creative ? (BlockEntityType)AllBlockEntityTypes.CREATIVE_FLUID_TANK.get() : (BlockEntityType)AllBlockEntityTypes.FLUID_TANK.get(),
                        world,
                        placedOnPos
                     );
                     if (tankAt != null) {
                        FluidTankBlockEntity controllerBE = tankAt.getControllerBE();
                        if (controllerBE != null) {
                           int width = controllerBE.width;
                           if (width != 1) {
                              int tanksToPlace = 0;
                              BlockPos startPos = face == Direction.DOWN
                                 ? controllerBE.getBlockPos().below()
                                 : controllerBE.getBlockPos().above(controllerBE.height);
                              if (startPos.getY() == pos.getY()) {
                                 for (int xOffset = 0; xOffset < width; xOffset++) {
                                    for (int zOffset = 0; zOffset < width; zOffset++) {
                                       BlockPos offsetPos = startPos.offset(xOffset, 0, zOffset);
                                       BlockState blockState = world.getBlockState(offsetPos);
                                       if (!FluidTankBlock.isTank(blockState)) {
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
                                          BlockPos offsetPos = startPos.offset(xOffset, 0, zOffsetx);
                                          BlockState blockState = world.getBlockState(offsetPos);
                                          if (!FluidTankBlock.isTank(blockState)) {
                                             BlockPlaceContext context = BlockPlaceContext.at(ctx, offsetPos, face);
                                             player.getPersistentData().putBoolean("SilenceTankSound", true);
                                             super.place(context);
                                             player.getPersistentData().remove("SilenceTankSound");
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

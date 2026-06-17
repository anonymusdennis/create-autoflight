package com.simibubi.create.content.kinetics.belt;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class BeltSlicer {
   public static ItemInteractionResult useWrench(
      BlockState state, Level world, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit, BeltSlicer.Feedback feedBack
   ) {
      BeltBlockEntity controllerBE = BeltHelper.getControllerBE(world, pos);
      if (controllerBE == null) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else if ((Boolean)state.getValue(BeltBlock.CASING) && hit.getDirection() != Direction.UP) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else if (state.getValue(BeltBlock.PART) == BeltPart.PULLEY && hit.getDirection().getAxis() != Axis.Y) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else {
         int beltLength = controllerBE.beltLength;
         if (beltLength == 2) {
            return ItemInteractionResult.FAIL;
         } else {
            BlockPos beltVector = BlockPos.containing(BeltHelper.getBeltVector(state));
            BeltPart part = (BeltPart)state.getValue(BeltBlock.PART);
            List<BlockPos> beltChain = BeltBlock.getBeltChain(world, controllerBE.getBlockPos());
            boolean creative = player.isCreative();
            if (hoveringEnd(state, hit)) {
               if (world.isClientSide) {
                  return ItemInteractionResult.SUCCESS;
               } else {
                  for (BlockPos blockPos : beltChain) {
                     BeltBlockEntity belt = BeltHelper.getSegmentBE(world, blockPos);
                     if (belt != null) {
                        belt.detachKinetics();
                        belt.invalidateItemHandler();
                        belt.beltLength = 0;
                     }
                  }

                  BeltInventory inventory = controllerBE.inventory;
                  BlockPos next = part == BeltPart.END ? pos.subtract(beltVector) : pos.offset(beltVector);
                  BlockState replacedState = world.getBlockState(next);
                  BeltBlockEntity segmentBE = BeltHelper.getSegmentBE(world, next);
                  KineticBlockEntity.switchToBlockState(
                     world,
                     next,
                     ProperWaterloggedBlock.withWater(
                        world, (BlockState)state.setValue(BeltBlock.CASING, segmentBE != null && segmentBE.casing != BeltBlockEntity.CasingType.NONE), next
                     )
                  );
                  world.setBlock(pos, ProperWaterloggedBlock.withWater(world, Blocks.AIR.defaultBlockState(), pos), 67);
                  world.removeBlockEntity(pos);
                  world.levelEvent(2001, pos, Block.getId(state));
                  if (!creative && AllBlocks.BELT.has(replacedState) && replacedState.getValue(BeltBlock.PART) == BeltPart.PULLEY) {
                     player.getInventory().placeItemBackInInventory(AllBlocks.SHAFT.asStack());
                  }

                  if (part == BeltPart.END && inventory != null) {
                     List<TransportedItemStack> toEject = new ArrayList<>();

                     for (TransportedItemStack transportedItemStack : inventory.getTransportedItems()) {
                        if (transportedItemStack.beltPosition > (float)(beltLength - 1)) {
                           toEject.add(transportedItemStack);
                        }
                     }

                     toEject.forEach(inventory::eject);
                     toEject.forEach(inventory.getTransportedItems()::remove);
                  }

                  if (part == BeltPart.START && segmentBE != null && inventory != null) {
                     controllerBE.inventory = null;
                     segmentBE.inventory = null;
                     segmentBE.setController(next);

                     for (TransportedItemStack transportedItemStackx : inventory.getTransportedItems()) {
                        transportedItemStackx.beltPosition--;
                        if (transportedItemStackx.beltPosition <= 0.0F) {
                           ItemEntity entity = new ItemEntity(
                              world,
                              (double)((float)pos.getX() + 0.5F),
                              (double)((float)pos.getY() + 0.6875F),
                              (double)((float)pos.getZ() + 0.5F),
                              transportedItemStackx.stack
                           );
                           entity.setDeltaMovement(Vec3.ZERO);
                           entity.setDefaultPickUpDelay();
                           entity.hurtMarked = true;
                           world.addFreshEntity(entity);
                        } else {
                           segmentBE.getInventory().addItem(transportedItemStackx);
                        }
                     }
                  }

                  return ItemInteractionResult.SUCCESS;
               }
            } else {
               BeltBlockEntity segmentBEx = BeltHelper.getSegmentBE(world, pos);
               if (segmentBEx == null) {
                  return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
               } else {
                  int hitSegment = segmentBEx.index;
                  Vec3 centerOf = VecHelper.getCenterOf(hit.getBlockPos());
                  Vec3 subtract = hit.getLocation().subtract(centerOf);
                  boolean towardPositive = subtract.dot(Vec3.atLowerCornerOf(beltVector)) > 0.0;
                  BlockPos nextx = !towardPositive ? pos.subtract(beltVector) : pos.offset(beltVector);
                  if (hitSegment != 0 && (hitSegment != 1 || towardPositive)) {
                     if (hitSegment != controllerBE.beltLength - 1 && (hitSegment != controllerBE.beltLength - 2 || !towardPositive)) {
                        if (!creative) {
                           int requiredShafts = 0;
                           if (!segmentBEx.hasPulley()) {
                              requiredShafts++;
                           }

                           BlockState other = world.getBlockState(nextx);
                           if (AllBlocks.BELT.has(other) && other.getValue(BeltBlock.PART) == BeltPart.MIDDLE) {
                              requiredShafts++;
                           }

                           int amountRetrieved = 0;
                           boolean beltFound = false;
                           int i = 0;

                           while (true) {
                              if (i >= player.getInventory().getContainerSize()) {
                                 if (!world.isClientSide) {
                                    player.getInventory().placeItemBackInInventory(AllBlocks.SHAFT.asStack(amountRetrieved));
                                    if (beltFound) {
                                       player.getInventory().placeItemBackInInventory(AllItems.BELT_CONNECTOR.asStack());
                                    }
                                 }

                                 return ItemInteractionResult.FAIL;
                              }

                              if (amountRetrieved == requiredShafts && beltFound) {
                                 break;
                              }

                              ItemStack itemstack = player.getInventory().getItem(i);
                              if (!itemstack.isEmpty()) {
                                 int count = itemstack.getCount();
                                 if (AllItems.BELT_CONNECTOR.isIn(itemstack) && !beltFound) {
                                    if (!world.isClientSide) {
                                       itemstack.shrink(1);
                                    }

                                    beltFound = true;
                                 } else if (AllBlocks.SHAFT.isIn(itemstack)) {
                                    int taken = Math.min(count, requiredShafts - amountRetrieved);
                                    if (!world.isClientSide) {
                                       if (taken == count) {
                                          player.getInventory().setItem(i, ItemStack.EMPTY);
                                       } else {
                                          itemstack.shrink(taken);
                                       }
                                    }

                                    amountRetrieved += taken;
                                 }
                              }

                              i++;
                           }
                        }

                        if (!world.isClientSide) {
                           for (BlockPos blockPosx : beltChain) {
                              BeltBlockEntity belt = BeltHelper.getSegmentBE(world, blockPosx);
                              if (belt != null) {
                                 belt.detachKinetics();
                                 belt.invalidateItemHandler();
                                 belt.beltLength = 0;
                              }
                           }

                           BeltInventory inventoryx = controllerBE.inventory;
                           KineticBlockEntity.switchToBlockState(
                              world, pos, (BlockState)state.setValue(BeltBlock.PART, towardPositive ? BeltPart.END : BeltPart.START)
                           );
                           KineticBlockEntity.switchToBlockState(
                              world, nextx, (BlockState)world.getBlockState(nextx).setValue(BeltBlock.PART, towardPositive ? BeltPart.START : BeltPart.END)
                           );
                           world.playSound(null, pos, SoundEvents.WOOL_HIT, player == null ? SoundSource.BLOCKS : SoundSource.PLAYERS, 0.5F, 2.3F);
                           BeltBlockEntity newController = towardPositive ? BeltHelper.getSegmentBE(world, nextx) : segmentBEx;
                           if (newController != null && inventoryx != null) {
                              newController.inventory = null;
                              newController.setController(newController.getBlockPos());
                              Iterator<TransportedItemStack> iterator = inventoryx.getTransportedItems().iterator();

                              while (iterator.hasNext()) {
                                 TransportedItemStack transportedItemStackxx = iterator.next();
                                 float newPosition = transportedItemStackxx.beltPosition - (float)hitSegment - (float)(towardPositive ? 1 : 0);
                                 if (!(newPosition <= 0.0F)) {
                                    transportedItemStackxx.beltPosition = newPosition;
                                    iterator.remove();
                                    newController.getInventory().addItem(transportedItemStackxx);
                                 }
                              }
                           }
                        }

                        return ItemInteractionResult.SUCCESS;
                     } else {
                        return ItemInteractionResult.FAIL;
                     }
                  } else {
                     return ItemInteractionResult.FAIL;
                  }
               }
            }
         }
      }
   }

   public static ItemInteractionResult useConnector(
      BlockState state, Level world, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit, BeltSlicer.Feedback feedBack
   ) {
      BeltBlockEntity controllerBE = BeltHelper.getControllerBE(world, pos);
      if (controllerBE == null) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else {
         int beltLength = controllerBE.beltLength;
         if (beltLength == BeltConnectorItem.maxLength()) {
            return ItemInteractionResult.FAIL;
         } else {
            BlockPos beltVector = BlockPos.containing(BeltHelper.getBeltVector(state));
            BeltPart part = (BeltPart)state.getValue(BeltBlock.PART);
            Direction facing = (Direction)state.getValue(BeltBlock.HORIZONTAL_FACING);
            List<BlockPos> beltChain = BeltBlock.getBeltChain(world, controllerBE.getBlockPos());
            boolean creative = player.isCreative();
            if (!hoveringEnd(state, hit)) {
               return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            } else {
               BlockPos next = part == BeltPart.START ? pos.subtract(beltVector) : pos.offset(beltVector);
               BeltBlockEntity mergedController = null;
               int mergedBeltLength = 0;
               BlockState nextState = world.getBlockState(next);
               if (!nextState.canBeReplaced()) {
                  if (!AllBlocks.BELT.has(nextState)) {
                     return ItemInteractionResult.FAIL;
                  }

                  if (!beltStatesCompatible(state, nextState)) {
                     return ItemInteractionResult.FAIL;
                  }

                  mergedController = BeltHelper.getControllerBE(world, next);
                  if (mergedController == null) {
                     return ItemInteractionResult.FAIL;
                  }

                  if (mergedController.beltLength + beltLength > BeltConnectorItem.maxLength()) {
                     return ItemInteractionResult.FAIL;
                  }

                  mergedBeltLength = mergedController.beltLength;
                  if (!world.isClientSide) {
                     boolean flipBelt = facing != nextState.getValue(BeltBlock.HORIZONTAL_FACING);
                     Optional<DyeColor> color = controllerBE.color;

                     for (BlockPos blockPos : BeltBlock.getBeltChain(world, mergedController.getBlockPos())) {
                        BeltBlockEntity belt = BeltHelper.getSegmentBE(world, blockPos);
                        if (belt != null) {
                           belt.detachKinetics();
                           belt.invalidateItemHandler();
                           belt.beltLength = 0;
                           belt.color = color;
                           if (flipBelt) {
                              world.setBlock(blockPos, flipBelt(world.getBlockState(blockPos)), 67);
                           }
                        }
                     }

                     if (flipBelt && mergedController.inventory != null) {
                        for (TransportedItemStack transportedItemStack : mergedController.inventory.getTransportedItems()) {
                           transportedItemStack.beltPosition = (float)mergedBeltLength - transportedItemStack.beltPosition;
                           transportedItemStack.prevBeltPosition = (float)mergedBeltLength - transportedItemStack.prevBeltPosition;
                        }
                     }

                     beltChain = BeltBlock.getBeltChain(world, mergedController.getBlockPos());
                  }
               }

               if (!world.isClientSide) {
                  for (BlockPos blockPosx : beltChain) {
                     BeltBlockEntity belt = BeltHelper.getSegmentBE(world, blockPosx);
                     if (belt != null) {
                        belt.detachKinetics();
                        belt.invalidateItemHandler();
                        belt.beltLength = 0;
                     }
                  }

                  BeltInventory inventory = controllerBE.inventory;
                  KineticBlockEntity.switchToBlockState(world, pos, (BlockState)state.setValue(BeltBlock.PART, BeltPart.MIDDLE));
                  if (mergedController == null) {
                     world.setBlock(next, ProperWaterloggedBlock.withWater(world, (BlockState)state.setValue(BeltBlock.CASING, false), next), 67);
                     BeltBlockEntity segmentBE = BeltHelper.getSegmentBE(world, next);
                     if (segmentBE != null) {
                        segmentBE.color = controllerBE.color;
                     }

                     world.playSound(null, pos, SoundEvents.WOOL_PLACE, player == null ? SoundSource.BLOCKS : SoundSource.PLAYERS, 0.5F, 1.0F);
                     if (part == BeltPart.START && segmentBE != null && inventory != null) {
                        segmentBE.setController(next);

                        for (TransportedItemStack transportedItemStack : inventory.getTransportedItems()) {
                           transportedItemStack.beltPosition++;
                           segmentBE.getInventory().addItem(transportedItemStack);
                        }
                     }
                  } else {
                     BeltInventory mergedInventory = mergedController.inventory;
                     world.playSound(null, pos, SoundEvents.WOOL_HIT, player == null ? SoundSource.BLOCKS : SoundSource.PLAYERS, 0.5F, 1.3F);
                     BeltBlockEntity segmentBEx = BeltHelper.getSegmentBE(world, next);
                     KineticBlockEntity.switchToBlockState(
                        world,
                        next,
                        (BlockState)((BlockState)state.setValue(BeltBlock.CASING, segmentBEx != null && segmentBEx.casing != BeltBlockEntity.CasingType.NONE))
                           .setValue(BeltBlock.PART, BeltPart.MIDDLE)
                     );
                     if (!creative) {
                        player.getInventory().placeItemBackInInventory(AllBlocks.SHAFT.asStack(2));
                        player.getInventory().placeItemBackInInventory(AllItems.BELT_CONNECTOR.asStack());
                     }

                     for (BlockPos blockPosxx : BeltBlock.getBeltChain(world, controllerBE.getBlockPos())) {
                        BeltBlockEntity belt = BeltHelper.getSegmentBE(world, blockPosxx);
                        if (belt != null) {
                           belt.invalidateItemHandler();
                        }
                     }

                     BlockPos search = controllerBE.getBlockPos();

                     for (int i = 0; i < 10000; i++) {
                        BlockState blockState = world.getBlockState(search);
                        if (!AllBlocks.BELT.has(blockState)) {
                           break;
                        }

                        if (blockState.getValue(BeltBlock.PART) == BeltPart.START) {
                           BeltBlockEntity newController = BeltHelper.getSegmentBE(world, search);
                           if (newController != controllerBE && inventory != null) {
                              newController.setController(search);
                              controllerBE.inventory = null;

                              for (TransportedItemStack transportedItemStack : inventory.getTransportedItems()) {
                                 transportedItemStack.beltPosition += (float)mergedBeltLength;
                                 newController.getInventory().addItem(transportedItemStack);
                              }
                           }

                           if (newController != mergedController && mergedInventory != null) {
                              newController.setController(search);
                              mergedController.inventory = null;

                              for (TransportedItemStack transportedItemStack : mergedInventory.getTransportedItems()) {
                                 if (newController == controllerBE) {
                                    transportedItemStack.beltPosition += (float)beltLength;
                                 }

                                 newController.getInventory().addItem(transportedItemStack);
                              }
                           }
                           break;
                        }

                        search = search.subtract(beltVector);
                     }
                  }
               }

               return ItemInteractionResult.SUCCESS;
            }
         }
      }
   }

   static boolean beltStatesCompatible(BlockState state, BlockState nextState) {
      Direction facing1 = (Direction)state.getValue(BeltBlock.HORIZONTAL_FACING);
      BeltSlope slope1 = (BeltSlope)state.getValue(BeltBlock.SLOPE);
      Direction facing2 = (Direction)nextState.getValue(BeltBlock.HORIZONTAL_FACING);
      BeltSlope slope2 = (BeltSlope)nextState.getValue(BeltBlock.SLOPE);
      switch (slope1) {
         case UPWARD:
            if (slope2 == BeltSlope.DOWNWARD) {
               return facing1 == facing2.getOpposite();
            }

            return slope2 == slope1 && facing1 == facing2;
         case DOWNWARD:
            if (slope2 == BeltSlope.UPWARD) {
               return facing1 == facing2.getOpposite();
            }

            return slope2 == slope1 && facing1 == facing2;
         default:
            return slope2 == slope1 && facing2.getAxis() == facing1.getAxis();
      }
   }

   static BlockState flipBelt(BlockState state) {
      Direction facing = (Direction)state.getValue(BeltBlock.HORIZONTAL_FACING);
      BeltSlope slope = (BeltSlope)state.getValue(BeltBlock.SLOPE);
      BeltPart part = (BeltPart)state.getValue(BeltBlock.PART);
      if (slope == BeltSlope.UPWARD) {
         state = (BlockState)state.setValue(BeltBlock.SLOPE, BeltSlope.DOWNWARD);
      } else if (slope == BeltSlope.DOWNWARD) {
         state = (BlockState)state.setValue(BeltBlock.SLOPE, BeltSlope.UPWARD);
      }

      if (part == BeltPart.END) {
         state = (BlockState)state.setValue(BeltBlock.PART, BeltPart.START);
      } else if (part == BeltPart.START) {
         state = (BlockState)state.setValue(BeltBlock.PART, BeltPart.END);
      }

      return (BlockState)state.setValue(BeltBlock.HORIZONTAL_FACING, facing.getOpposite());
   }

   static boolean hoveringEnd(BlockState state, BlockHitResult hit) {
      BeltPart part = (BeltPart)state.getValue(BeltBlock.PART);
      if (part != BeltPart.MIDDLE && part != BeltPart.PULLEY) {
         Vec3 beltVector = BeltHelper.getBeltVector(state);
         Vec3 centerOf = VecHelper.getCenterOf(hit.getBlockPos());
         Vec3 subtract = hit.getLocation().subtract(centerOf);
         return subtract.dot(beltVector) > 0.0 == (part == BeltPart.END);
      } else {
         return false;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static void tickHoveringInformation() {
      Minecraft mc = Minecraft.getInstance();
      HitResult target = mc.hitResult;
      if (target != null && target instanceof BlockHitResult result) {
         ClientLevel world = mc.level;
         BlockPos pos = result.getBlockPos();
         BlockState state = world.getBlockState(pos);
         ItemStack held = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
         ItemStack heldOffHand = mc.player.getItemInHand(InteractionHand.OFF_HAND);
         if (!mc.player.isShiftKeyDown()) {
            if (AllBlocks.BELT.has(state)) {
               BeltSlicer.Feedback feedback = new BeltSlicer.Feedback();
               if (!AllItems.WRENCH.isIn(held) && !AllItems.WRENCH.isIn(heldOffHand)) {
                  if (!AllItems.BELT_CONNECTOR.isIn(held) && !AllItems.BELT_CONNECTOR.isIn(heldOffHand)) {
                     return;
                  }

                  useConnector(state, world, pos, mc.player, InteractionHand.MAIN_HAND, result, feedback);
               } else {
                  useWrench(state, world, pos, mc.player, InteractionHand.MAIN_HAND, result, feedback);
               }

               if (feedback.langKey != null) {
                  mc.player.displayClientMessage(CreateLang.translateDirect(feedback.langKey).withStyle(feedback.formatting), true);
               } else {
                  mc.player.displayClientMessage(CommonComponents.EMPTY, true);
               }

               if (feedback.bb != null) {
                  Outliner.getInstance().chaseAABB("BeltSlicer", feedback.bb).lineWidth(0.0625F).colored(feedback.color);
               }
            }
         }
      }
   }

   public static class Feedback {
      int color = 16777215;
      AABB bb;
      String langKey;
      ChatFormatting formatting = ChatFormatting.WHITE;
   }
}

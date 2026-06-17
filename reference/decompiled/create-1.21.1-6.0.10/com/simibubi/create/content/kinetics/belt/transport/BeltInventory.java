package com.simibubi.create.content.kinetics.belt.transport;

import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class BeltInventory {
   final BeltBlockEntity belt;
   private final List<TransportedItemStack> items;
   final List<TransportedItemStack> toInsert;
   final List<TransportedItemStack> toRemove;
   boolean beltMovementPositive;
   final float SEGMENT_WINDOW = 0.75F;
   TransportedItemStack lazyClientItem;

   public BeltInventory(BeltBlockEntity be) {
      this.belt = be;
      this.items = new LinkedList<>();
      this.toInsert = new LinkedList<>();
      this.toRemove = new LinkedList<>();
   }

   public void tick() {
      if (this.lazyClientItem != null) {
         if (this.lazyClientItem.locked) {
            this.lazyClientItem = null;
         } else {
            this.lazyClientItem.locked = true;
         }
      }

      if (!this.toInsert.isEmpty() || !this.toRemove.isEmpty()) {
         this.toInsert.forEach(this::insert);
         this.toInsert.clear();
         this.items.removeAll(this.toRemove);
         this.toRemove.clear();
         this.belt.notifyUpdate();
      }

      if (this.belt.getSpeed() != 0.0F) {
         if (this.beltMovementPositive != this.belt.getDirectionAwareBeltMovementSpeed() > 0.0F) {
            this.beltMovementPositive = !this.beltMovementPositive;
            Collections.reverse(this.items);
            this.belt.notifyUpdate();
         }

         TransportedItemStack stackInFront = null;
         TransportedItemStack currentItem = null;
         Iterator<TransportedItemStack> iterator = this.items.iterator();
         float beltSpeed = this.belt.getDirectionAwareBeltMovementSpeed();
         Direction movementFacing = this.belt.getMovementFacing();
         boolean horizontal = this.belt.getBlockState().getValue(BeltBlock.SLOPE) == BeltSlope.HORIZONTAL;
         float spacing = 1.0F;
         Level world = this.belt.getLevel();
         boolean onClient = world.isClientSide && !this.belt.isVirtual();
         BeltInventory.Ending ending = BeltInventory.Ending.UNRESOLVED;

         while (iterator.hasNext()) {
            stackInFront = currentItem;
            currentItem = iterator.next();
            currentItem.prevBeltPosition = currentItem.beltPosition;
            currentItem.prevSideOffset = currentItem.sideOffset;
            if (currentItem.stack.isEmpty()) {
               iterator.remove();
               currentItem = null;
            } else {
               float movement = beltSpeed;
               if (onClient) {
                  movement = beltSpeed * ServerSpeedProvider.get();
               }

               if (!world.isClientSide || !currentItem.locked) {
                  if (currentItem.lockedExternally) {
                     currentItem.lockedExternally = false;
                  } else {
                     boolean noMovement = false;
                     float currentPos = currentItem.beltPosition;
                     if (stackInFront != null) {
                        float diff = stackInFront.beltPosition - currentPos;
                        if (Math.abs(diff) <= spacing) {
                           noMovement = true;
                        }

                        movement = this.beltMovementPositive ? Math.min(movement, diff - spacing) : Math.max(movement, diff + spacing);
                     }

                     float diffToEnd = this.beltMovementPositive ? (float)this.belt.beltLength - currentPos : -currentPos;
                     if (Math.abs(diffToEnd) < Math.abs(movement) + 1.0F) {
                        if (ending == BeltInventory.Ending.UNRESOLVED) {
                           ending = this.resolveEnding();
                        }

                        diffToEnd += this.beltMovementPositive ? -ending.margin : ending.margin;
                     }

                     float limitedMovement = this.beltMovementPositive ? Math.min(movement, diffToEnd) : Math.max(movement, diffToEnd);
                     float nextOffset = currentItem.beltPosition + limitedMovement;
                     if (!onClient && horizontal) {
                        ItemStack item = currentItem.stack;
                        if (this.handleBeltProcessingAndCheckIfRemoved(currentItem, nextOffset, noMovement)) {
                           iterator.remove();
                           this.belt.notifyUpdate();
                           continue;
                        }

                        if (item != currentItem.stack) {
                           this.belt.notifyUpdate();
                        }

                        if (currentItem.locked) {
                           continue;
                        }
                     }

                     if (!BeltFunnelInteractionHandler.checkForFunnels(this, currentItem, nextOffset)
                        && !noMovement
                        && !BeltTunnelInteractionHandler.flapTunnelsAndCheckIfStuck(this, currentItem, nextOffset)
                        && !BeltCrusherInteractionHandler.checkForCrushers(this, currentItem, nextOffset)) {
                        currentItem.beltPosition += limitedMovement;
                        float diffToMiddle = currentItem.getTargetSideOffset() - currentItem.sideOffset;
                        currentItem.sideOffset = currentItem.sideOffset
                           + Mth.clamp(diffToMiddle * Math.abs(limitedMovement) * 6.0F, -Math.abs(diffToMiddle), Math.abs(diffToMiddle));
                        currentPos = currentItem.beltPosition;
                        if (limitedMovement != movement && !onClient) {
                           int lastOffset = this.beltMovementPositive ? this.belt.beltLength - 1 : 0;
                           BlockPos nextPosition = BeltHelper.getPositionForOffset(this.belt, this.beltMovementPositive ? this.belt.beltLength : -1);
                           if (ending != BeltInventory.Ending.FUNNEL) {
                              if (ending == BeltInventory.Ending.INSERT) {
                                 DirectBeltInputBehaviour inputBehaviour = BlockEntityBehaviour.get(world, nextPosition, DirectBeltInputBehaviour.TYPE);
                                 if (inputBehaviour != null && inputBehaviour.canInsertFromSide(movementFacing)) {
                                    ItemStack remainder = inputBehaviour.handleInsertion(currentItem, movementFacing, false);
                                    if (!ItemStack.matches(remainder, currentItem.stack)) {
                                       currentItem.stack = remainder;
                                       if (remainder.isEmpty()) {
                                          this.lazyClientItem = currentItem;
                                          this.lazyClientItem.locked = false;
                                          iterator.remove();
                                       } else {
                                          currentItem.stack = remainder;
                                       }

                                       BeltTunnelInteractionHandler.flapTunnel(this, lastOffset, movementFacing, false);
                                       this.belt.notifyUpdate();
                                    }
                                 }
                              } else if (ending != BeltInventory.Ending.BLOCKED && ending == BeltInventory.Ending.EJECT) {
                                 this.eject(currentItem);
                                 iterator.remove();
                                 BeltTunnelInteractionHandler.flapTunnel(this, lastOffset, movementFacing, false);
                                 this.belt.notifyUpdate();
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

   protected boolean handleBeltProcessingAndCheckIfRemoved(TransportedItemStack currentItem, float nextOffset, boolean noMovement) {
      int currentSegment = (int)currentItem.beltPosition;
      if (currentItem.locked) {
         BeltProcessingBehaviour processingBehaviour = this.getBeltProcessingAtSegment(currentSegment);
         TransportedItemStackHandlerBehaviour stackHandlerBehaviour = this.getTransportedItemStackHandlerAtSegment(currentSegment);
         if (stackHandlerBehaviour == null) {
            return false;
         } else if (processingBehaviour == null) {
            currentItem.locked = false;
            this.belt.notifyUpdate();
            return false;
         } else {
            BeltProcessingBehaviour.ProcessingResult result = processingBehaviour.handleHeldItem(currentItem, stackHandlerBehaviour);
            if (result == BeltProcessingBehaviour.ProcessingResult.REMOVE) {
               return true;
            } else if (result == BeltProcessingBehaviour.ProcessingResult.HOLD) {
               return false;
            } else {
               currentItem.locked = false;
               this.belt.notifyUpdate();
               return false;
            }
         }
      } else if (noMovement) {
         return false;
      } else {
         if (currentItem.beltPosition > 0.5F || this.beltMovementPositive) {
            int firstUpcomingSegment = (int)(currentItem.beltPosition + (this.beltMovementPositive ? 0.5F : -0.5F));
            int step = this.beltMovementPositive ? 1 : -1;

            for (int segment = firstUpcomingSegment;
               this.beltMovementPositive ? (float)segment + 0.5F <= nextOffset : (float)segment + 0.5F >= nextOffset;
               segment += step
            ) {
               BeltProcessingBehaviour processingBehaviour = this.getBeltProcessingAtSegment(segment);
               TransportedItemStackHandlerBehaviour stackHandlerBehaviour = this.getTransportedItemStackHandlerAtSegment(segment);
               if (processingBehaviour != null
                  && stackHandlerBehaviour != null
                  && !BeltProcessingBehaviour.isBlocked(this.belt.getLevel(), BeltHelper.getPositionForOffset(this.belt, segment))) {
                  BeltProcessingBehaviour.ProcessingResult result = processingBehaviour.handleReceivedItem(currentItem, stackHandlerBehaviour);
                  if (result == BeltProcessingBehaviour.ProcessingResult.REMOVE) {
                     return true;
                  }

                  if (result == BeltProcessingBehaviour.ProcessingResult.HOLD) {
                     currentItem.beltPosition = (float)segment + 0.5F + (this.beltMovementPositive ? 0.001953125F : -0.001953125F);
                     currentItem.locked = true;
                     this.belt.notifyUpdate();
                     return false;
                  }
               }
            }
         }

         return false;
      }
   }

   protected BeltProcessingBehaviour getBeltProcessingAtSegment(int segment) {
      return BlockEntityBehaviour.get(this.belt.getLevel(), BeltHelper.getPositionForOffset(this.belt, segment).above(2), BeltProcessingBehaviour.TYPE);
   }

   protected TransportedItemStackHandlerBehaviour getTransportedItemStackHandlerAtSegment(int segment) {
      return BlockEntityBehaviour.get(this.belt.getLevel(), BeltHelper.getPositionForOffset(this.belt, segment), TransportedItemStackHandlerBehaviour.TYPE);
   }

   private BeltInventory.Ending resolveEnding() {
      Level world = this.belt.getLevel();
      BlockPos nextPosition = BeltHelper.getPositionForOffset(this.belt, this.beltMovementPositive ? this.belt.beltLength : -1);
      DirectBeltInputBehaviour inputBehaviour = BlockEntityBehaviour.get(world, nextPosition, DirectBeltInputBehaviour.TYPE);
      if (inputBehaviour != null) {
         return BeltInventory.Ending.INSERT;
      } else {
         return BlockHelper.hasBlockSolidSide(world.getBlockState(nextPosition), world, nextPosition, this.belt.getMovementFacing().getOpposite())
            ? BeltInventory.Ending.BLOCKED
            : BeltInventory.Ending.EJECT;
      }
   }

   public boolean canInsertAt(int segment) {
      return this.canInsertAtFromSide(segment, Direction.UP);
   }

   public boolean canInsertAtFromSide(int segment, Direction side) {
      float segmentPos = (float)segment;
      if (this.belt.getMovementFacing() == side.getOpposite()) {
         return false;
      } else {
         if (this.belt.getMovementFacing() != side) {
            segmentPos += 0.5F;
         } else if (!this.beltMovementPositive) {
            segmentPos++;
         }

         for (TransportedItemStack stack : this.items) {
            if (this.isBlocking(segment, side, segmentPos, stack)) {
               return false;
            }
         }

         for (TransportedItemStack stackx : this.toInsert) {
            if (this.isBlocking(segment, side, segmentPos, stackx)) {
               return false;
            }
         }

         return true;
      }
   }

   private boolean isBlocking(int segment, Direction side, float segmentPos, TransportedItemStack stack) {
      float currentPos = stack.beltPosition;
      return stack.insertedAt == segment
         && stack.insertedFrom == side
         && (this.beltMovementPositive ? currentPos <= segmentPos + 1.0F : currentPos >= segmentPos - 1.0F);
   }

   public void addItem(TransportedItemStack newStack) {
      this.toInsert.add(newStack);
   }

   private void insert(TransportedItemStack newStack) {
      if (this.items.isEmpty()) {
         this.items.add(newStack);
      } else {
         int index = 0;

         for (TransportedItemStack stack : this.items) {
            if (stack.compareTo(newStack) > 0 == this.beltMovementPositive) {
               break;
            }

            index++;
         }

         this.items.add(index, newStack);
      }
   }

   public TransportedItemStack getStackAtOffset(int offset) {
      float min = (float)offset;
      float max = (float)(offset + 1);

      for (TransportedItemStack stack : this.items) {
         if (!this.toRemove.contains(stack) && !(stack.beltPosition > max) && stack.beltPosition > min) {
            return stack;
         }
      }

      return null;
   }

   public void read(CompoundTag nbt, Provider registries) {
      this.items.clear();
      nbt.getList("Items", 10).forEach(inbt -> this.items.add(TransportedItemStack.read((CompoundTag)inbt, registries)));
      if (nbt.contains("LazyItem")) {
         this.lazyClientItem = TransportedItemStack.read(nbt.getCompound("LazyItem"), registries);
      }

      this.beltMovementPositive = nbt.getBoolean("PositiveOrder");
   }

   public CompoundTag write(Provider registries) {
      if (!this.toInsert.isEmpty() || !this.toRemove.isEmpty()) {
         this.toInsert.forEach(this::insert);
         this.toInsert.clear();
         this.items.removeAll(this.toRemove);
         this.toRemove.clear();
      }

      CompoundTag nbt = new CompoundTag();
      ListTag itemsNBT = new ListTag();
      this.items.forEach(stack -> itemsNBT.add(stack.serializeNBT(registries)));
      nbt.put("Items", itemsNBT);
      if (this.lazyClientItem != null) {
         nbt.put("LazyItem", this.lazyClientItem.serializeNBT(registries));
      }

      nbt.putBoolean("PositiveOrder", this.beltMovementPositive);
      return nbt;
   }

   public void eject(TransportedItemStack stack) {
      ItemStack ejected = stack.stack;
      Vec3 outPos = BeltHelper.getVectorForOffset(this.belt, stack.beltPosition);
      float movementSpeed = Math.max(Math.abs(this.belt.getBeltMovementSpeed()), 0.125F);
      Vec3 outMotion = Vec3.atLowerCornerOf(this.belt.getBeltChainDirection()).scale((double)movementSpeed).add(0.0, 0.125, 0.0);
      outPos = outPos.add(outMotion.normalize().scale(0.001));
      ItemEntity entity = new ItemEntity(this.belt.getLevel(), outPos.x, outPos.y + 0.375, outPos.z, ejected);
      entity.setDeltaMovement(outMotion);
      entity.setDefaultPickUpDelay();
      entity.hurtMarked = true;
      this.belt.getLevel().addFreshEntity(entity);
   }

   public void ejectAll() {
      this.items.forEach(this::eject);
      this.items.clear();
   }

   public void applyToEachWithin(
      float position, float maxDistanceToPosition, Function<TransportedItemStack, TransportedItemStackHandlerBehaviour.TransportedResult> processFunction
   ) {
      boolean dirty = false;

      for (TransportedItemStack transported : this.items) {
         if (!this.toRemove.contains(transported)) {
            ItemStack stackBefore = transported.stack.copy();
            if (!(Math.abs(position - transported.beltPosition) >= maxDistanceToPosition)) {
               TransportedItemStackHandlerBehaviour.TransportedResult result = processFunction.apply(transported);
               if (result != null && !result.didntChangeFrom(stackBefore)) {
                  dirty = true;
                  if (result.hasHeldOutput()) {
                     TransportedItemStack held = result.getHeldOutput();
                     held.beltPosition = (float)((int)position) + 0.5F - (this.beltMovementPositive ? 0.001953125F : -0.001953125F);
                     this.toInsert.add(held);
                  }

                  this.toInsert.addAll(result.getOutputs());
                  this.toRemove.add(transported);
               }
            }
         }
      }

      if (dirty) {
         this.belt.notifyUpdate();
      }
   }

   public List<TransportedItemStack> getTransportedItems() {
      return this.items;
   }

   @Nullable
   public TransportedItemStack getLazyClientItem() {
      return this.lazyClientItem;
   }

   private static enum Ending {
      UNRESOLVED(0.0F),
      EJECT(0.0F),
      INSERT(0.25F),
      FUNNEL(0.5F),
      BLOCKED(0.45F);

      private float margin;

      private Ending(float f) {
         this.margin = f;
      }
   }
}

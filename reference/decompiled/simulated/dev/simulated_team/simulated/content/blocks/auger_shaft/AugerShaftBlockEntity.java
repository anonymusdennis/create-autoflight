package dev.simulated_team.simulated.content.blocks.auger_shaft;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticEffectHandler;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.content.redstone.displayLink.source.ItemThroughputDisplaySource;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.ryanhcode.sable.util.LevelAccelerator;
import dev.simulated_team.simulated.content.blocks.auger_shaft.auger_groups.AugerDistributor;
import dev.simulated_team.simulated.content.particle.AugerIndicatorParticleData;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.multiloader.inventory.ContainerSlot;
import dev.simulated_team.simulated.multiloader.inventory.InventoryLoaderWrapper;
import dev.simulated_team.simulated.multiloader.inventory.ItemInfoWrapper;
import dev.simulated_team.simulated.service.SimInventoryService;
import dev.simulated_team.simulated.util.Observable;
import dev.simulated_team.simulated.util.SimDirectionUtil;
import java.util.List;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus.Internal;

public class AugerShaftBlockEntity extends KineticBlockEntity implements ItemReciever, Observable, Clearable {
   private static final float MAX_AUGER_SPEED_TICKS = 8.0F;
   private LevelAccelerator accelerator;
   private AugerDistributor attachedGroup;
   public AugerActorInventory actorInventory;
   public AugerInventory inventory;
   private final LerpedFloat updateTracker = LerpedFloat.linear();
   public int intDirection;
   public Direction flowDirection;
   @Internal
   public boolean beingWrenched;
   private int itemsMoved;
   private boolean maxSpeed;
   private boolean observed = false;
   private int particleCooldown = 100;

   public AugerShaftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.inventory = new AugerInventory(this);
      this.actorInventory = new AugerActorInventory(this, 8);
      this.setLazyTickRate(20);
      this.updateTracker.chase(1.0, 0.0, Chaser.LINEAR);
      this.effects = new AugerShaftBlockEntity.AugerKineticEffectHandler(this);
   }

   public void tick() {
      assert this.level != null;

      if (this.accelerator == null) {
         this.accelerator = new LevelAccelerator(this.level);
      }

      this.intDirection = Mth.sign((double)this.getSpeed());
      this.flowDirection = Direction.get(
         this.intDirection == 1 ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE, (Axis)this.getBlockState().getValue(AugerShaftBlock.AXIS)
      );
      super.tick();
      this.handleUpdateTracking();
      this.accelerator.clearCache();
      if (this.level.isClientSide) {
         this.particleCooldown--;
         if (this.observed && this.particleCooldown < 0) {
            this.particleCooldown = 100;
            this.effects.spawnRotationIndicators();
         }

         this.observed = false;
      }
   }

   private void handleUpdateTracking() {
      assert this.level != null;

      if (this.getSpeed() == 0.0F) {
         this.resetUpdateTracker();
      } else {
         if (this.inventory.isEmpty()) {
            if (!this.level.isClientSide) {
               this.extract();
            }

            if (this.inventory.isEmpty()) {
               this.resetUpdateTracker();
               return;
            }
         }

         this.updateTracker.chase(1.0, (double)this.getItemSpeed(), Chaser.LINEAR);
         this.updateTracker.tickChaser();
         if (this.updateTracker.settled()) {
            this.handleItemPassed();
            this.resetUpdateTracker();
         }
      }
   }

   private void extract() {
      assert this.level != null;

      if (!this.actorInventory.isEmpty()) {
         ContainerSlot largestSlot = null;

         for (ContainerSlot populatedSlot : this.actorInventory.getPopulatedSlots()) {
            if (largestSlot == null || largestSlot.getStack().getCount() < populatedSlot.getStack().getCount()) {
               largestSlot = populatedSlot;
            }
         }

         if (largestSlot != null) {
            ItemStack extracted = this.actorInventory.extractSlot(largestSlot.getIndex(), largestSlot.getStack().getCount(), false);
            this.inventory.insertSlot(extracted, 0, false);
         }
      }

      if (this.inventory.isEmpty()) {
         Direction antiFlowDir = this.flowDirection.getOpposite();
         BlockPos antiFlowPos = this.worldPosition.relative(antiFlowDir);
         if (!(this.accelerator.getBlockState(antiFlowPos).getBlock() instanceof AugerShaftBlock)) {
            InventoryLoaderWrapper wrapped = SimInventoryService.INSTANCE.getInventory(this.level.getBlockEntity(antiFlowPos), antiFlowDir);
            if (wrapped != null) {
               ItemStack extracted = wrapped.extractAny(16, true, false);
               if (!extracted.isEmpty()) {
                  this.inventory.insertSlot(wrapped.extractAny(16, false, false), 0, false);
               }
            }
         }
      }
   }

   private void handleItemPassed() {
      assert this.level != null && !this.level.isClientSide;

      if (this.level.getBlockEntity(this.worldPosition.relative(this.flowDirection)) instanceof AugerShaftBlockEntity abe) {
         int beforeCount = this.inventory.getItem(0).getCount();
         this.inventory.slot.setStack(abe.inventory.insertSlot(this.inventory.slot.getStack(), 0, false));
         int totalMoved = beforeCount - this.inventory.slot.getStack().getCount();
         this.itemsMoved += totalMoved;
         if (totalMoved != 0) {
            this.notifyUpdate();
         }
      }
   }

   private float getItemSpeed() {
      float totalTicks = 8.0F / (Math.abs(this.getSpeed()) / 256.0F);
      this.maxSpeed = totalTicks == 8.0F;
      return 1.0F / totalTicks;
   }

   private void resetUpdateTracker() {
      this.updateTracker.startWithValue(0.0);
   }

   public void lazyTick() {
      super.lazyTick();
      if (!this.level.isClientSide && this.getSpeed() != 0.0F) {
         this.refreshActors();
      }

      if (!this.level.isClientSide) {
         DisplayLinkBlock.sendToGatherers(this.level, this.getBlockPos(), (dlbe, a) -> a.itemReceived(dlbe, this.itemsMoved), ItemThroughputDisplaySource.class);
         this.itemsMoved = 0;
      }

      if (this.level.isClientSide && this.maxSpeed && this.itemsMoved > 0) {
         this.sendObserved(this.getBlockPos());
      }
   }

   private void refreshActors() {
      Direction dir = this.flowDirection.getOpposite();
      Axis axis = dir.getAxis();
      BlockPos relPos = this.getBlockPos().relative(dir);
      if (this.accelerator.getBlockState(relPos).hasBlockEntity()) {
         if (this.level.getBlockEntity(relPos) instanceof BlockHarvester harvester) {
            if (this.attachedGroup != null) {
               this.attachedGroup.removeReceiver(this);
            }

            AugerDistributor distributor = harvester.simulated$getAssociatedDistributor();
            if (distributor != null) {
               this.attachedGroup = distributor;
               this.attachedGroup.addReceiver(this);
            } else {
               this.attachedGroup = new AugerDistributor();
               this.attachedGroup.addReceiver(this);
            }
         }

         if (this.attachedGroup != null) {
            this.attachedGroup.gatherAndAssociateHarvesters(SimDirectionUtil.getSurroundingDirections(axis), relPos, this.getLevel(), this.accelerator);
         }
      }
   }

   public void destroy() {
      super.destroy();
      if (!this.level.isClientSide && !this.beingWrenched) {
         Containers.dropContents(this.level, this.worldPosition, this.inventory);
         this.inventory.clearContent();
         this.actorInventory.clearAndDropContents(this.level, this.worldPosition);
         this.resetUpdateTracker();
      }
   }

   protected void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.write(compound, registries, clientPacket);
      compound.put("Inventory", this.inventory.write(registries));
      compound.put("ActorInventory", this.actorInventory.write(registries));
      if (!clientPacket) {
         compound.putFloat("Progress", this.updateTracker.getValue());
      }
   }

   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      this.inventory.read(registries, compound.getCompound("Inventory"));
      this.actorInventory.read(registries, compound.getCompound("ActorInventory"));
      if (!clientPacket) {
         this.updateTracker.setValue((double)compound.getFloat("Progress"));
      }
   }

   public AugerInventory getInventory() {
      return this.inventory;
   }

   public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
      if (this.getSpeed() > 0.0F) {
         float perTickSpeed = this.getItemSpeed();
         float perSecondSpeed = perTickSpeed * 20.0F;
         SimLang.translate("auger_shaft.item_flow", Math.floor((double)(perSecondSpeed * 100.0F)) / 100.0).style(ChatFormatting.YELLOW).forGoggles(tooltip);
         added = true;
      }

      int actorItems = this.actorInventory.storedItemCount;
      if (actorItems > 0) {
         SimLang.translate("auger_shaft.actor_items", actorItems).style(ChatFormatting.GRAY).forGoggles(tooltip);
         added = true;
      }

      int count = this.inventory.slot.getStack().getCount();
      if (count > 0) {
         CreateLang.translate(
               "tooltip.chute.contains", new Object[]{Component.translatable(this.inventory.slot.getStack().getDescriptionId()).getString(), count}
            )
            .style(ChatFormatting.GREEN)
            .forGoggles(tooltip);
         added = true;
      }

      this.observed = true;
      return added;
   }

   @Override
   public ItemStack onRecieveItem(ItemStack item, BlockPos fromPos) {
      if (this.isSpeedRequirementFulfilled() && !this.isOverStressed()) {
         ItemInfoWrapper info = ItemInfoWrapper.generateFromStack(item);
         long amountInserted = (long)this.actorInventory.insertGeneral(info, item.getCount(), true);
         if (amountInserted > 0L) {
            this.actorInventory.insertGeneral(info, item.getCount(), false);
            item = item.copy();
            item.shrink((int)amountInserted);
            return item;
         } else {
            return item;
         }
      } else {
         return item;
      }
   }

   @Override
   public boolean removed() {
      return this.isRemoved();
   }

   @Override
   public boolean isActive() {
      return this.getSpeed() != 0.0F;
   }

   public void clearContent() {
      this.inventory.clearContent();
      this.actorInventory.clearContent();
   }

   public class AugerKineticEffectHandler extends KineticEffectHandler {
      public AugerKineticEffectHandler(final KineticBlockEntity kte) {
         super(kte);
      }

      public void spawnRotationIndicators() {
         AugerShaftBlockEntity auger = AugerShaftBlockEntity.this;
         float speed = auger.getSpeed();
         if (speed != 0.0F) {
            BlockState state = auger.getBlockState();
            if (state.getBlock() instanceof KineticBlock kb) {
               float radius1 = kb.getParticleInitialRadius();
               float radius2 = kb.getParticleTargetRadius();
               Direction direction = auger.flowDirection;
               BlockPos pos = auger.getBlockPos();
               Level level = auger.getLevel();
               if (direction != null && auger.speed != 0.0F) {
                  if (level != null) {
                     Vec3 vec = VecHelper.getCenterOf(pos);
                     SpeedLevel speedLevel = SpeedLevel.of(speed);
                     int color = speedLevel.getColor();
                     int particleSpeed = speedLevel.getParticleSpeed();
                     particleSpeed *= (int)Math.signum(speed);

                     for (int i = 0; i < 3; i++) {
                        AugerIndicatorParticleData particleData = new AugerIndicatorParticleData(
                           color, (float)particleSpeed, radius1, radius2, (float)i / 3.0F, 10, direction
                        );
                        if (level instanceof ServerLevel serverLevel) {
                           serverLevel.sendParticles(particleData, vec.x, vec.y, vec.z, 20, 0.0, 0.0, 0.0, 1.0);
                        } else {
                           for (int j = 0; j < 20; j++) {
                              level.addParticle(particleData, vec.x, vec.y, vec.z, 0.0, 0.0, 0.0);
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

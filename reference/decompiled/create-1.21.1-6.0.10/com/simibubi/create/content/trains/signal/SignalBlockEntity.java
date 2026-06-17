package com.simibubi.create.content.trains.signal;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.compat.computercraft.events.SignalStateChangeEvent;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import java.util.List;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

public class SignalBlockEntity extends SmartBlockEntity implements TransformableBlockEntity {
   public TrackTargetingBehaviour<SignalBoundary> edgePoint;
   private SignalBlockEntity.SignalState state = SignalBlockEntity.SignalState.INVALID;
   private SignalBlockEntity.OverlayState overlay = SignalBlockEntity.OverlayState.SKIP;
   private int switchToRedAfterTrainEntered;
   private boolean lastReportedPower = false;
   public AbstractComputerBehaviour computerBehaviour;

   public SignalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      if (Mods.COMPUTERCRAFT.isLoaded()) {
         event.registerBlockEntity(
            PeripheralCapability.get(),
            (BlockEntityType)AllBlockEntityTypes.TRACK_SIGNAL.get(),
            (be, context) -> be.computerBehaviour.getPeripheralCapability()
         );
      }
   }

   @Override
   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.write(tag, registries, clientPacket);
      NBTHelper.writeEnum(tag, "State", this.state);
      NBTHelper.writeEnum(tag, "Overlay", this.overlay);
      tag.putBoolean("Power", this.lastReportedPower);
   }

   @Override
   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.read(tag, registries, clientPacket);
      this.state = (SignalBlockEntity.SignalState)NBTHelper.readEnum(tag, "State", SignalBlockEntity.SignalState.class);
      this.overlay = (SignalBlockEntity.OverlayState)NBTHelper.readEnum(tag, "Overlay", SignalBlockEntity.OverlayState.class);
      this.lastReportedPower = tag.getBoolean("Power");
      this.invalidateRenderBoundingBox();
   }

   @Nullable
   public SignalBoundary getSignal() {
      return this.edgePoint.getEdgePoint();
   }

   public boolean isPowered() {
      return this.state == SignalBlockEntity.SignalState.RED;
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      this.edgePoint = new TrackTargetingBehaviour<>(this, EdgePointType.SIGNAL);
      behaviours.add(this.edgePoint);
      behaviours.add(this.computerBehaviour = ComputerCraftProxy.behaviour(this));
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level.isClientSide) {
         SignalBoundary boundary = this.getSignal();
         if (boundary == null) {
            this.enterState(SignalBlockEntity.SignalState.INVALID);
            this.setOverlay(SignalBlockEntity.OverlayState.RENDER);
         } else {
            BlockState blockState = this.getBlockState();
            blockState.getOptionalValue(SignalBlock.POWERED).ifPresent(powered -> {
               if (this.lastReportedPower != powered) {
                  this.lastReportedPower = powered;
                  boundary.updateBlockEntityPower(this);
                  this.notifyUpdate();
               }
            });
            blockState.getOptionalValue(SignalBlock.TYPE).ifPresent(stateType -> {
               SignalBlock.SignalType targetType = boundary.getTypeFor(this.worldPosition);
               if (stateType != targetType) {
                  this.level.setBlock(this.worldPosition, (BlockState)blockState.setValue(SignalBlock.TYPE, targetType), 3);
                  this.refreshBlockState();
               }
            });
            this.enterState(boundary.getStateFor(this.worldPosition));
            this.setOverlay(boundary.getOverlayFor(this.worldPosition));
         }
      }
   }

   public boolean getReportedPower() {
      return this.lastReportedPower;
   }

   public SignalBlockEntity.SignalState getState() {
      return this.state;
   }

   public SignalBlockEntity.OverlayState getOverlay() {
      return this.overlay;
   }

   public void setOverlay(SignalBlockEntity.OverlayState state) {
      if (this.overlay != state) {
         this.overlay = state;
         this.notifyUpdate();
      }
   }

   public void enterState(SignalBlockEntity.SignalState state) {
      if (this.switchToRedAfterTrainEntered > 0) {
         this.switchToRedAfterTrainEntered--;
      }

      if (this.state != state) {
         if (state != SignalBlockEntity.SignalState.RED || this.switchToRedAfterTrainEntered <= 0) {
            this.state = state;
            this.switchToRedAfterTrainEntered = state != SignalBlockEntity.SignalState.GREEN && state != SignalBlockEntity.SignalState.YELLOW ? 0 : 15;
            if (this.computerBehaviour.hasAttachedComputer()) {
               this.computerBehaviour.prepareComputerEvent(new SignalStateChangeEvent(state));
            }

            this.notifyUpdate();
         }
      }
   }

   @Override
   public void invalidate() {
      super.invalidate();
      this.computerBehaviour.removePeripheral();
   }

   @Override
   protected AABB createRenderBoundingBox() {
      return new AABB(Vec3.atLowerCornerOf(this.worldPosition), Vec3.atLowerCornerOf(this.edgePoint.getGlobalPosition())).inflate(2.0);
   }

   @Override
   public void transform(BlockEntity be, StructureTransform transform) {
      this.edgePoint.transform(be, transform);
   }

   public static enum OverlayState {
      RENDER,
      SKIP,
      DUAL;
   }

   public static enum SignalState {
      RED,
      YELLOW,
      GREEN,
      INVALID;

      public boolean isRedLight(float renderTime) {
         return this == RED || this == INVALID && renderTime % 40.0F < 3.0F;
      }

      public boolean isYellowLight(float renderTime) {
         return this == YELLOW;
      }

      public boolean isGreenLight(float renderTime) {
         return this == GREEN;
      }
   }
}

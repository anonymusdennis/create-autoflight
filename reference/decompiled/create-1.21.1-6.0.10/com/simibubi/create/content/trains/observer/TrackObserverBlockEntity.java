package com.simibubi.create.content.trains.observer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.Create;
import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.compat.computercraft.events.TrainPassEvent;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

public class TrackObserverBlockEntity extends SmartBlockEntity implements TransformableBlockEntity, Clearable {
   public TrackTargetingBehaviour<TrackObserver> edgePoint;
   private FilteringBehaviour filtering;
   public AbstractComputerBehaviour computerBehaviour;
   @Nullable
   public UUID passingTrainUUID;

   public TrackObserverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      if (Mods.COMPUTERCRAFT.isLoaded()) {
         event.registerBlockEntity(
            PeripheralCapability.get(),
            (BlockEntityType)AllBlockEntityTypes.TRACK_OBSERVER.get(),
            (be, context) -> be.computerBehaviour.getPeripheralCapability()
         );
      }
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(this.edgePoint = new TrackTargetingBehaviour<>(this, EdgePointType.OBSERVER));
      behaviours.add(this.filtering = this.createFilter().withCallback(this::onFilterChanged));
      behaviours.add(this.computerBehaviour = ComputerCraftProxy.behaviour(this));
      this.filtering.setLabel(CreateLang.translateDirect("logistics.train_observer.cargo_filter"));
   }

   private void onFilterChanged(ItemStack newFilter) {
      if (!this.level.isClientSide()) {
         TrackObserver observer = this.getObserver();
         if (observer != null) {
            observer.setFilterAndNotify(this.level, newFilter);
         }
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level.isClientSide()) {
         boolean shouldBePowered = false;
         TrackObserver observer = this.getObserver();
         if (observer != null) {
            shouldBePowered = observer.isActivated();
         }

         if (this.isBlockPowered() != shouldBePowered) {
            if (observer != null && this.computerBehaviour.hasAttachedComputer()) {
               if (shouldBePowered) {
                  this.passingTrainUUID = observer.getCurrentTrain();
               }

               if (this.passingTrainUUID != null) {
                  this.computerBehaviour.prepareComputerEvent(new TrainPassEvent(Create.RAILWAYS.trains.get(this.passingTrainUUID), shouldBePowered));
                  if (!shouldBePowered) {
                     this.passingTrainUUID = null;
                  }
               }
            }

            BlockState blockState = this.getBlockState();
            if (blockState.hasProperty(TrackObserverBlock.POWERED)) {
               this.level.setBlock(this.worldPosition, (BlockState)blockState.setValue(TrackObserverBlock.POWERED, shouldBePowered), 3);
            }

            DisplayLinkBlock.notifyGatherers(this.level, this.worldPosition);
         }
      }
   }

   @Nullable
   public TrackObserver getObserver() {
      return this.edgePoint.getEdgePoint();
   }

   public ItemStack getFilter() {
      return this.filtering.getFilter();
   }

   public boolean isBlockPowered() {
      return this.getBlockState().getOptionalValue(TrackObserverBlock.POWERED).orElse(false);
   }

   @Override
   protected AABB createRenderBoundingBox() {
      return new AABB(Vec3.atLowerCornerOf(this.worldPosition), Vec3.atLowerCornerOf(this.edgePoint.getGlobalPosition())).inflate(2.0);
   }

   @Override
   public void transform(BlockEntity be, StructureTransform transform) {
      this.edgePoint.transform(be, transform);
   }

   public FilteringBehaviour createFilter() {
      return new FilteringBehaviour(this, new ValueBoxTransform() {
         @Override
         public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
            TransformStack.of(ms).rotateXDegrees(90.0F);
         }

         @Override
         public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            return new Vec3(0.5, 0.96875, 0.5);
         }
      });
   }

   @Override
   public void invalidate() {
      super.invalidate();
      this.computerBehaviour.removePeripheral();
   }

   public void clearContent() {
      this.filtering.setFilter(ItemStack.EMPTY);
   }
}

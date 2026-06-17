package com.simibubi.create.content.fluids.tank;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.util.List;
import java.util.Objects;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

public class FluidTankBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IMultiBlockEntityContainer.Fluid {
   private static final int MAX_SIZE = 3;
   protected IFluidHandler fluidCapability;
   protected boolean forceFluidLevelUpdate;
   protected FluidTank tankInventory = this.createInventory();
   protected BlockPos controller;
   protected BlockPos lastKnownPos;
   protected boolean updateConnectivity;
   protected boolean updateCapability;
   protected boolean window;
   protected int luminosity;
   protected int width;
   protected int height;
   public BoilerData boiler;
   private static final int SYNC_RATE = 8;
   protected int syncCooldown;
   protected boolean queuedSync;
   private LerpedFloat fluidLevel;

   public FluidTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.forceFluidLevelUpdate = true;
      this.updateConnectivity = false;
      this.updateCapability = false;
      this.window = true;
      this.height = 1;
      this.width = 1;
      this.boiler = new BoilerData();
      this.refreshCapability();
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      event.registerBlockEntity(FluidHandler.BLOCK, (BlockEntityType)AllBlockEntityTypes.FLUID_TANK.get(), (be, context) -> {
         if (be.fluidCapability == null) {
            be.refreshCapability();
         }

         return be.fluidCapability;
      });
   }

   protected SmartFluidTank createInventory() {
      return new SmartFluidTank(getCapacityMultiplier(), this::onFluidStackChanged);
   }

   protected void updateConnectivity() {
      this.updateConnectivity = false;
      if (!this.level.isClientSide) {
         if (this.isController()) {
            ConnectivityHandler.formMulti(this);
         }
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (this.syncCooldown > 0) {
         this.syncCooldown--;
         if (this.syncCooldown == 0 && this.queuedSync) {
            this.sendData();
         }
      }

      if (this.lastKnownPos == null) {
         this.lastKnownPos = this.getBlockPos();
      } else if (!this.lastKnownPos.equals(this.worldPosition) && this.worldPosition != null) {
         this.onPositionChanged();
         return;
      }

      if (this.updateCapability) {
         this.updateCapability = false;
         this.refreshCapability();
      }

      if (this.updateConnectivity) {
         this.updateConnectivity();
      }

      if (this.fluidLevel != null) {
         this.fluidLevel.tickChaser();
      }

      if (this.isController()) {
         this.boiler.tick(this);
      }
   }

   @Override
   public void lazyTick() {
      super.lazyTick();
      if (this.isController()) {
         this.boiler.updateOcclusion(this);
      }
   }

   @Override
   public BlockPos getLastKnownPos() {
      return this.lastKnownPos;
   }

   @Override
   public boolean isController() {
      return this.controller == null
         || this.worldPosition.getX() == this.controller.getX()
            && this.worldPosition.getY() == this.controller.getY()
            && this.worldPosition.getZ() == this.controller.getZ();
   }

   @Override
   public void initialize() {
      super.initialize();
      this.sendData();
      if (this.level.isClientSide) {
         this.invalidateRenderBoundingBox();
      }
   }

   private void onPositionChanged() {
      this.removeController(true);
      this.lastKnownPos = this.worldPosition;
   }

   protected void onFluidStackChanged(FluidStack newFluidStack) {
      if (this.hasLevel()) {
         FluidType attributes = newFluidStack.getFluid().getFluidType();
         int luminosity = (int)((float)attributes.getLightLevel(newFluidStack) / 1.2F);
         boolean reversed = attributes.isLighterThanAir();
         int maxY = (int)(this.getFillState() * (float)this.height + 1.0F);

         for (int yOffset = 0; yOffset < this.height; yOffset++) {
            boolean isBright = reversed ? this.height - yOffset <= maxY : yOffset < maxY;
            int actualLuminosity = isBright ? luminosity : (luminosity > 0 ? 1 : 0);

            for (int xOffset = 0; xOffset < this.width; xOffset++) {
               for (int zOffset = 0; zOffset < this.width; zOffset++) {
                  BlockPos pos = this.worldPosition.offset(xOffset, yOffset, zOffset);
                  FluidTankBlockEntity tankAt = ConnectivityHandler.partAt(this.getType(), this.level, pos);
                  if (tankAt != null) {
                     this.level.updateNeighbourForOutputSignal(pos, tankAt.getBlockState().getBlock());
                     if (tankAt.luminosity != actualLuminosity) {
                        tankAt.setLuminosity(actualLuminosity);
                     }
                  }
               }
            }
         }

         if (!this.level.isClientSide) {
            this.setChanged();
            this.sendData();
         }

         if (this.isVirtual()) {
            if (this.fluidLevel == null) {
               this.fluidLevel = LerpedFloat.linear().startWithValue((double)this.getFillState());
            }

            this.fluidLevel.chase((double)this.getFillState(), 0.5, Chaser.EXP);
         }
      }
   }

   protected void setLuminosity(int luminosity) {
      if (!this.level.isClientSide) {
         if (this.luminosity != luminosity) {
            this.luminosity = luminosity;
            this.sendData();
         }
      }
   }

   public FluidTankBlockEntity getControllerBE() {
      if (!this.isController() && this.hasLevel()) {
         BlockEntity blockEntity = this.level.getBlockEntity(this.controller);
         return blockEntity instanceof FluidTankBlockEntity ? (FluidTankBlockEntity)blockEntity : null;
      } else {
         return this;
      }
   }

   public void applyFluidTankSize(int blocks) {
      this.tankInventory.setCapacity(blocks * getCapacityMultiplier());
      int overflow = this.tankInventory.getFluidAmount() - this.tankInventory.getCapacity();
      if (overflow > 0) {
         this.tankInventory.drain(overflow, FluidAction.EXECUTE);
      }

      this.forceFluidLevelUpdate = true;
   }

   @Override
   public void removeController(boolean keepFluids) {
      if (!this.level.isClientSide) {
         this.updateConnectivity = true;
         if (!keepFluids) {
            this.applyFluidTankSize(1);
         }

         this.controller = null;
         this.width = 1;
         this.height = 1;
         this.boiler.clear();
         this.onFluidStackChanged(this.tankInventory.getFluid());
         BlockState state = this.getBlockState();
         if (FluidTankBlock.isTank(state)) {
            state = (BlockState)state.setValue(FluidTankBlock.BOTTOM, true);
            state = (BlockState)state.setValue(FluidTankBlock.TOP, true);
            state = (BlockState)state.setValue(FluidTankBlock.SHAPE, this.window ? FluidTankBlock.Shape.WINDOW : FluidTankBlock.Shape.PLAIN);
            this.getLevel().setBlock(this.worldPosition, state, 22);
         }

         this.refreshCapability();
         this.setChanged();
         this.sendData();
      }
   }

   public void toggleWindows() {
      FluidTankBlockEntity be = this.getControllerBE();
      if (be != null) {
         if (!be.boiler.isActive()) {
            be.setWindows(!be.window);
         }
      }
   }

   public void updateBoilerTemperature() {
      FluidTankBlockEntity be = this.getControllerBE();
      if (be != null) {
         if (be.boiler.isActive()) {
            be.boiler.needsHeatLevelUpdate = true;
         }
      }
   }

   public void sendDataImmediately() {
      this.syncCooldown = 0;
      this.queuedSync = false;
      this.sendData();
   }

   @Override
   public void sendData() {
      if (this.syncCooldown > 0) {
         this.queuedSync = true;
      } else {
         super.sendData();
         this.queuedSync = false;
         this.syncCooldown = 8;
      }
   }

   public void setWindows(boolean window) {
      this.window = window;

      for (int yOffset = 0; yOffset < this.height; yOffset++) {
         for (int xOffset = 0; xOffset < this.width; xOffset++) {
            for (int zOffset = 0; zOffset < this.width; zOffset++) {
               BlockPos pos = this.worldPosition.offset(xOffset, yOffset, zOffset);
               BlockState blockState = this.level.getBlockState(pos);
               if (FluidTankBlock.isTank(blockState)) {
                  FluidTankBlock.Shape shape = FluidTankBlock.Shape.PLAIN;
                  if (window) {
                     if (this.width == 1) {
                        shape = FluidTankBlock.Shape.WINDOW;
                     }

                     if (this.width == 2) {
                        shape = xOffset == 0
                           ? (zOffset == 0 ? FluidTankBlock.Shape.WINDOW_NW : FluidTankBlock.Shape.WINDOW_SW)
                           : (zOffset == 0 ? FluidTankBlock.Shape.WINDOW_NE : FluidTankBlock.Shape.WINDOW_SE);
                     }

                     if (this.width == 3 && Math.abs(Math.abs(xOffset) - Math.abs(zOffset)) == 1) {
                        shape = FluidTankBlock.Shape.WINDOW;
                     }
                  }

                  this.level.setBlock(pos, (BlockState)blockState.setValue(FluidTankBlock.SHAPE, shape), 22);
                  this.level.getChunkSource().getLightEngine().checkBlock(pos);
               }
            }
         }
      }
   }

   public void updateBoilerState() {
      if (this.isController()) {
         boolean wasBoiler = this.boiler.isActive();
         boolean changed = this.boiler.evaluate(this);
         if (wasBoiler != this.boiler.isActive()) {
            if (this.boiler.isActive()) {
               this.setWindows(false);
            }

            for (int yOffset = 0; yOffset < this.height; yOffset++) {
               for (int xOffset = 0; xOffset < this.width; xOffset++) {
                  for (int zOffset = 0; zOffset < this.width; zOffset++) {
                     if (this.level.getBlockEntity(this.worldPosition.offset(xOffset, yOffset, zOffset)) instanceof FluidTankBlockEntity fbe) {
                        fbe.refreshCapability();
                     }
                  }
               }
            }
         }

         if (changed) {
            this.notifyUpdate();
            this.boiler.checkPipeOrganAdvancement(this);
         }
      }
   }

   @Override
   public void setController(BlockPos controller) {
      if (!this.level.isClientSide || this.isVirtual()) {
         if (!controller.equals(this.controller)) {
            this.controller = controller;
            this.refreshCapability();
            this.setChanged();
            this.sendData();
         }
      }
   }

   void refreshCapability() {
      this.fluidCapability = this.handlerForCapability();
      this.invalidateCapabilities();
   }

   private IFluidHandler handlerForCapability() {
      return (IFluidHandler)(this.isController()
         ? (this.boiler.isActive() ? this.boiler.createHandler() : this.tankInventory)
         : (this.getControllerBE() != null ? this.getControllerBE().handlerForCapability() : new FluidTank(0)));
   }

   @Override
   public BlockPos getController() {
      return this.isController() ? this.worldPosition : this.controller;
   }

   @Override
   protected AABB createRenderBoundingBox() {
      return this.isController()
         ? super.createRenderBoundingBox().expandTowards((double)(this.width - 1), (double)(this.height - 1), (double)(this.width - 1))
         : super.createRenderBoundingBox();
   }

   @Nullable
   public FluidTankBlockEntity getOtherFluidTankBlockEntity(Direction direction) {
      BlockEntity otherBE = this.level.getBlockEntity(this.worldPosition.relative(direction));
      return otherBE instanceof FluidTankBlockEntity ? (FluidTankBlockEntity)otherBE : null;
   }

   @Override
   public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      FluidTankBlockEntity controllerBE = this.getControllerBE();
      if (controllerBE == null) {
         return false;
      } else {
         return controllerBE.boiler.addToGoggleTooltip(tooltip, isPlayerSneaking, controllerBE.getTotalTankSize())
            ? true
            : this.containedFluidTooltip(
               tooltip, isPlayerSneaking, (IFluidHandler)this.level.getCapability(FluidHandler.BLOCK, controllerBE.getBlockPos(), null)
            );
      }
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      BlockPos controllerBefore = this.controller;
      int prevSize = this.width;
      int prevHeight = this.height;
      int prevLum = this.luminosity;
      this.updateConnectivity = compound.contains("Uninitialized");
      this.luminosity = compound.getInt("Luminosity");
      this.lastKnownPos = null;
      if (compound.contains("LastKnownPos")) {
         this.lastKnownPos = NBTHelper.readBlockPos(compound, "LastKnownPos");
      }

      this.controller = null;
      if (compound.contains("Controller")) {
         this.controller = NBTHelper.readBlockPos(compound, "Controller");
      }

      if (this.isController()) {
         this.window = compound.getBoolean("Window");
         this.width = compound.getInt("Size");
         this.height = compound.getInt("Height");
         this.tankInventory.setCapacity(this.getTotalTankSize() * getCapacityMultiplier());
         this.tankInventory.readFromNBT(registries, compound.getCompound("TankContent"));
         if (this.tankInventory.getSpace() < 0) {
            this.tankInventory.drain(-this.tankInventory.getSpace(), FluidAction.EXECUTE);
         }
      }

      this.boiler.read(compound.getCompound("Boiler"), this.width * this.width * this.height);
      if (compound.contains("ForceFluidLevel") || this.fluidLevel == null) {
         this.fluidLevel = LerpedFloat.linear().startWithValue((double)this.getFillState());
      }

      this.updateCapability = true;
      if (clientPacket) {
         boolean changeOfController = !Objects.equals(controllerBefore, this.controller);
         if (changeOfController || prevSize != this.width || prevHeight != this.height) {
            if (this.hasLevel()) {
               this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 16);
            }

            if (this.isController()) {
               this.tankInventory.setCapacity(getCapacityMultiplier() * this.getTotalTankSize());
            }

            this.invalidateRenderBoundingBox();
         }

         if (this.isController()) {
            float fillState = this.getFillState();
            if (compound.contains("ForceFluidLevel") || this.fluidLevel == null) {
               this.fluidLevel = LerpedFloat.linear().startWithValue((double)fillState);
            }

            this.fluidLevel.chase((double)fillState, 0.5, Chaser.EXP);
         }

         if (this.luminosity != prevLum && this.hasLevel()) {
            this.level.getChunkSource().getLightEngine().checkBlock(this.worldPosition);
         }

         if (compound.contains("LazySync")) {
            this.fluidLevel.chase((double)this.fluidLevel.getChaseTarget(), 0.125, Chaser.EXP);
         }
      }
   }

   public float getFillState() {
      return (float)this.tankInventory.getFluidAmount() / (float)this.tankInventory.getCapacity();
   }

   @Override
   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      if (this.updateConnectivity) {
         compound.putBoolean("Uninitialized", true);
      }

      compound.put("Boiler", this.boiler.write());
      if (this.lastKnownPos != null) {
         compound.put("LastKnownPos", NbtUtils.writeBlockPos(this.lastKnownPos));
      }

      if (!this.isController()) {
         compound.put("Controller", NbtUtils.writeBlockPos(this.controller));
      }

      if (this.isController()) {
         compound.putBoolean("Window", this.window);
         compound.put("TankContent", this.tankInventory.writeToNBT(registries, new CompoundTag()));
         compound.putInt("Size", this.width);
         compound.putInt("Height", this.height);
      }

      compound.putInt("Luminosity", this.luminosity);
      super.write(compound, registries, clientPacket);
      if (clientPacket) {
         if (this.forceFluidLevelUpdate) {
            compound.putBoolean("ForceFluidLevel", true);
         }

         if (this.queuedSync) {
            compound.putBoolean("LazySync", true);
         }

         this.forceFluidLevelUpdate = false;
      }
   }

   @Override
   public void writeSafe(CompoundTag compound, Provider registries) {
      if (this.isController()) {
         compound.putBoolean("Window", this.window);
         compound.putInt("Size", this.width);
         compound.putInt("Height", this.height);
      }
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      this.registerAwardables(behaviours, new CreateAdvancement[]{AllAdvancements.STEAM_ENGINE_MAXED, AllAdvancements.PIPE_ORGAN});
   }

   public FluidTank getTankInventory() {
      return this.tankInventory;
   }

   public int getTotalTankSize() {
      return this.width * this.width * this.height;
   }

   public static int getMaxSize() {
      return 3;
   }

   public static int getCapacityMultiplier() {
      return (Integer)AllConfigs.server().fluids.fluidTankCapacity.get() * 1000;
   }

   public static int getMaxHeight() {
      return (Integer)AllConfigs.server().fluids.fluidTankMaxHeight.get();
   }

   public LerpedFloat getFluidLevel() {
      return this.fluidLevel;
   }

   public void setFluidLevel(LerpedFloat fluidLevel) {
      this.fluidLevel = fluidLevel;
   }

   @Override
   public void preventConnectivityUpdate() {
      this.updateConnectivity = false;
   }

   @Override
   public void notifyMultiUpdated() {
      BlockState state = this.getBlockState();
      if (FluidTankBlock.isTank(state)) {
         state = (BlockState)state.setValue(FluidTankBlock.BOTTOM, this.getController().getY() == this.getBlockPos().getY());
         state = (BlockState)state.setValue(FluidTankBlock.TOP, this.getController().getY() + this.height - 1 == this.getBlockPos().getY());
         this.level.setBlock(this.getBlockPos(), state, 6);
      }

      if (this.isController()) {
         this.setWindows(this.window);
      }

      this.onFluidStackChanged(this.tankInventory.getFluid());
      this.updateBoilerState();
      this.setChanged();
   }

   @Override
   public void setExtraData(@Nullable Object data) {
      if (data instanceof Boolean) {
         this.window = (Boolean)data;
      }
   }

   @Nullable
   @Override
   public Object getExtraData() {
      return this.window;
   }

   @Override
   public Object modifyExtraData(Object data) {
      return data instanceof Boolean windows ? windows | this.window : data;
   }

   @Override
   public Axis getMainConnectionAxis() {
      return Axis.Y;
   }

   @Override
   public int getMaxLength(Axis longAxis, int width) {
      return longAxis == Axis.Y ? getMaxHeight() : this.getMaxWidth();
   }

   @Override
   public int getMaxWidth() {
      return 3;
   }

   @Override
   public int getHeight() {
      return this.height;
   }

   @Override
   public void setHeight(int height) {
      this.height = height;
   }

   @Override
   public int getWidth() {
      return this.width;
   }

   @Override
   public void setWidth(int width) {
      this.width = width;
   }

   @Override
   public boolean hasTank() {
      return true;
   }

   @Override
   public int getTankSize(int tank) {
      return getCapacityMultiplier();
   }

   @Override
   public void setTankSize(int tank, int blocks) {
      this.applyFluidTankSize(blocks);
   }

   @Override
   public IFluidTank getTank(int tank) {
      return this.tankInventory;
   }

   @Override
   public FluidStack getFluid(int tank) {
      return this.tankInventory.getFluid().copy();
   }
}

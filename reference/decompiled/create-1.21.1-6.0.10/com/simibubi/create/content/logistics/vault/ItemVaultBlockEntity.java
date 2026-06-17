package com.simibubi.create.content.logistics.vault;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.foundation.ICapabilityProvider;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryWrapper;
import com.simibubi.create.foundation.mixin.accessor.ItemStackHandlerAccessor;
import com.simibubi.create.foundation.utility.SameSizeCombinedInvWrapper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.util.List;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ItemVaultBlockEntity extends SmartBlockEntity implements IMultiBlockEntityContainer.Inventory, Clearable {
   protected ICapabilityProvider<IItemHandler> itemCapability = null;
   protected InventoryIdentifier invId;
   protected ItemStackHandler inventory = new ItemStackHandler((Integer)AllConfigs.server().logistics.vaultCapacity.get()) {
      protected void onContentsChanged(int slot) {
         super.onContentsChanged(slot);
         ItemVaultBlockEntity.this.updateComparators();
         ItemVaultBlockEntity.this.level.blockEntityChanged(ItemVaultBlockEntity.this.worldPosition);
      }
   };
   protected BlockPos controller;
   protected BlockPos lastKnownPos;
   protected boolean updateConnectivity;
   protected int radius = 1;
   protected int length = 1;

   public ItemVaultBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      event.registerBlockEntity(ItemHandler.BLOCK, (BlockEntityType)AllBlockEntityTypes.ITEM_VAULT.get(), (be, context) -> {
         be.initCapability();
         return be.itemCapability == null ? null : be.itemCapability.getCapability();
      });
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
   }

   protected void updateConnectivity() {
      this.updateConnectivity = false;
      if (!this.level.isClientSide()) {
         if (this.isController()) {
            ConnectivityHandler.formMulti(this);
         }
      }
   }

   protected void updateComparators() {
      ItemVaultBlockEntity controllerBE = this.getControllerBE();
      if (controllerBE != null) {
         this.level.blockEntityChanged(controllerBE.worldPosition);
         BlockPos pos = controllerBE.getBlockPos();
         int radius = controllerBE.radius;
         int length = controllerBE.length;
         Axis axis = controllerBE.getMainConnectionAxis();
         int zMax = axis == Axis.X ? radius : length;
         int xMax = axis == Axis.Z ? radius : length;
         MutableBlockPos updatePos = new MutableBlockPos();
         MutableBlockPos provokingPos = new MutableBlockPos();

         for (int y = 0; y < radius; y++) {
            for (int z = 0; z < zMax; z++) {
               for (int x = 0; x < xMax; x++) {
                  int sectionX = SectionPos.blockToSectionCoord(pos.getX() + x);
                  int sectionZ = SectionPos.blockToSectionCoord(pos.getZ() + z);
                  if (this.level.hasChunk(sectionX, sectionZ)) {
                     provokingPos.setWithOffset(pos, x, y, z);
                     Block provokingBlock = this.level.getBlockState(provokingPos).getBlock();
                     if (y == 0) {
                        updateComaratorsInner(this.level, provokingBlock, provokingPos, updatePos, Direction.DOWN);
                     }

                     if (y == radius - 1) {
                        updateComaratorsInner(this.level, provokingBlock, provokingPos, updatePos, Direction.UP);
                     }

                     if (z == 0) {
                        updateComaratorsInner(this.level, provokingBlock, provokingPos, updatePos, Direction.NORTH);
                     }

                     if (z == zMax - 1) {
                        updateComaratorsInner(this.level, provokingBlock, provokingPos, updatePos, Direction.SOUTH);
                     }

                     if (x == 0) {
                        updateComaratorsInner(this.level, provokingBlock, provokingPos, updatePos, Direction.WEST);
                     }

                     if (x == xMax - 1) {
                        updateComaratorsInner(this.level, provokingBlock, provokingPos, updatePos, Direction.EAST);
                     }
                  }
               }
            }
         }
      }
   }

   private static void updateComaratorsInner(Level level, Block provokingBlock, BlockPos provokingPos, MutableBlockPos updatePos, Direction direction) {
      updatePos.setWithOffset(provokingPos, direction);
      int sectionX = SectionPos.blockToSectionCoord(updatePos.getX());
      int sectionZ = SectionPos.blockToSectionCoord(updatePos.getZ());
      if (level.hasChunk(sectionX, sectionZ)) {
         BlockState blockstate = level.getBlockState(updatePos);
         blockstate.onNeighborChange(level, updatePos, provokingPos);
         if (blockstate.isRedstoneConductor(level, updatePos)) {
            updatePos.move(direction);
            blockstate = level.getBlockState(updatePos);
            if (blockstate.getWeakChanges(level, updatePos)) {
               level.neighborChanged(blockstate, updatePos, provokingBlock, provokingPos, false);
            }
         }
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (this.lastKnownPos == null) {
         this.lastKnownPos = this.getBlockPos();
      } else if (!this.lastKnownPos.equals(this.worldPosition) && this.worldPosition != null) {
         this.onPositionChanged();
         return;
      }

      if (this.updateConnectivity) {
         this.updateConnectivity();
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

   private void onPositionChanged() {
      this.removeController(true);
      this.lastKnownPos = this.worldPosition;
   }

   public ItemVaultBlockEntity getControllerBE() {
      if (this.isController()) {
         return this;
      } else {
         BlockEntity blockEntity = this.level.getBlockEntity(this.controller);
         return blockEntity instanceof ItemVaultBlockEntity ? (ItemVaultBlockEntity)blockEntity : null;
      }
   }

   @Override
   public void removeController(boolean keepContents) {
      if (!this.level.isClientSide()) {
         this.updateConnectivity = true;
         this.controller = null;
         this.radius = 1;
         this.length = 1;
         BlockState state = this.getBlockState();
         if (ItemVaultBlock.isVault(state)) {
            state = (BlockState)state.setValue(ItemVaultBlock.LARGE, false);
            this.getLevel().setBlock(this.worldPosition, state, 22);
         }

         this.itemCapability = null;
         this.invalidateCapabilities();
         this.setChanged();
         this.sendData();
      }
   }

   @Override
   public void setController(BlockPos controller) {
      if (!this.level.isClientSide || this.isVirtual()) {
         if (!controller.equals(this.controller)) {
            this.controller = controller;
            this.itemCapability = null;
            this.invalidateCapabilities();
            this.setChanged();
            this.sendData();
         }
      }
   }

   @Override
   public BlockPos getController() {
      return this.isController() ? this.worldPosition : this.controller;
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      BlockPos controllerBefore = this.controller;
      int prevSize = this.radius;
      int prevLength = this.length;
      this.updateConnectivity = compound.contains("Uninitialized");
      this.lastKnownPos = null;
      if (compound.contains("LastKnownPos")) {
         this.lastKnownPos = NBTHelper.readBlockPos(compound, "LastKnownPos");
      }

      this.controller = null;
      if (compound.contains("Controller")) {
         this.controller = NBTHelper.readBlockPos(compound, "Controller");
      }

      if (this.isController()) {
         this.radius = compound.getInt("Size");
         this.length = compound.getInt("Length");
      }

      if (!clientPacket) {
         this.inventory.deserializeNBT(registries, compound.getCompound("Inventory"));
      } else {
         boolean changeOfController = controllerBefore == null ? this.controller != null : !controllerBefore.equals(this.controller);
         if (this.hasLevel() && (changeOfController || prevSize != this.radius || prevLength != this.length)) {
            this.level.setBlocksDirty(this.getBlockPos(), Blocks.AIR.defaultBlockState(), this.getBlockState());
         }
      }
   }

   @Override
   protected void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      if (this.updateConnectivity) {
         compound.putBoolean("Uninitialized", true);
      }

      if (this.lastKnownPos != null) {
         compound.put("LastKnownPos", NbtUtils.writeBlockPos(this.lastKnownPos));
      }

      if (!this.isController()) {
         compound.put("Controller", NbtUtils.writeBlockPos(this.controller));
      }

      if (this.isController()) {
         compound.putInt("Size", this.radius);
         compound.putInt("Length", this.length);
      }

      super.write(compound, registries, clientPacket);
      if (!clientPacket) {
         compound.putString("StorageType", "CombinedInv");
         compound.put("Inventory", this.inventory.serializeNBT(registries));
      }
   }

   public void clearContent() {
      ((ItemStackHandlerAccessor)this.inventory).create$getStacks().clear();
   }

   public ItemStackHandler getInventoryOfBlock() {
      return this.inventory;
   }

   public InventoryIdentifier getInvId() {
      this.initCapability();
      return this.invId;
   }

   public void applyInventoryToBlock(ItemStackHandler handler) {
      for (int i = 0; i < this.inventory.getSlots(); i++) {
         this.inventory.setStackInSlot(i, i < handler.getSlots() ? handler.getStackInSlot(i) : ItemStack.EMPTY);
      }
   }

   private void initCapability() {
      if (this.itemCapability == null || this.itemCapability.getCapability() == null) {
         if (!this.isController()) {
            ItemVaultBlockEntity controllerBE = this.getControllerBE();
            if (controllerBE != null) {
               controllerBE.initCapability();
               this.itemCapability = ICapabilityProvider.of(() -> {
                  if (controllerBE.isRemoved()) {
                     return null;
                  } else {
                     return controllerBE.itemCapability == null ? null : controllerBE.itemCapability.getCapability();
                  }
               });
               this.invId = controllerBE.invId;
            }
         } else {
            boolean alongZ = ItemVaultBlock.getVaultBlockAxis(this.getBlockState()) == Axis.Z;
            IItemHandlerModifiable[] invs = new IItemHandlerModifiable[this.length * this.radius * this.radius];

            for (int yOffset = 0; yOffset < this.length; yOffset++) {
               for (int xOffset = 0; xOffset < this.radius; xOffset++) {
                  for (int zOffset = 0; zOffset < this.radius; zOffset++) {
                     BlockPos vaultPos = alongZ ? this.worldPosition.offset(xOffset, zOffset, yOffset) : this.worldPosition.offset(yOffset, xOffset, zOffset);
                     ItemVaultBlockEntity vaultAt = ConnectivityHandler.partAt((BlockEntityType<?>)AllBlockEntityTypes.ITEM_VAULT.get(), this.level, vaultPos);
                     invs[yOffset * this.radius * this.radius + xOffset * this.radius + zOffset] = vaultAt != null ? vaultAt.inventory : new ItemStackHandler();
                  }
               }
            }

            this.itemCapability = ICapabilityProvider.of(new VersionedInventoryWrapper(SameSizeCombinedInvWrapper.create(invs)));
            BlockPos farCorner = alongZ
               ? this.worldPosition.offset(this.radius, this.radius, this.length)
               : this.worldPosition.offset(this.length, this.radius, this.radius);
            BoundingBox bounds = BoundingBox.fromCorners(this.worldPosition, farCorner);
            this.invId = new InventoryIdentifier.Bounds(bounds);
         }
      }
   }

   public static int getMaxLength(int radius) {
      return radius * 3;
   }

   @Override
   public void preventConnectivityUpdate() {
      this.updateConnectivity = false;
   }

   @Override
   public void notifyMultiUpdated() {
      BlockState state = this.getBlockState();
      if (ItemVaultBlock.isVault(state)) {
         this.level.setBlock(this.getBlockPos(), (BlockState)state.setValue(ItemVaultBlock.LARGE, this.radius > 2), 6);
      }

      this.itemCapability = null;
      this.invalidateCapabilities();
      this.setChanged();
   }

   @Override
   public Axis getMainConnectionAxis() {
      return this.getMainAxisOf(this);
   }

   @Override
   public int getMaxLength(Axis longAxis, int width) {
      return longAxis == Axis.Y ? this.getMaxWidth() : getMaxLength(width);
   }

   @Override
   public int getMaxWidth() {
      return 3;
   }

   @Override
   public int getHeight() {
      return this.length;
   }

   @Override
   public int getWidth() {
      return this.radius;
   }

   @Override
   public void setHeight(int height) {
      this.length = height;
   }

   @Override
   public void setWidth(int width) {
      this.radius = width;
   }

   @Override
   public boolean hasInventory() {
      return true;
   }
}

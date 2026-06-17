package com.simibubi.create.content.schematics.table;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.mixin.accessor.ItemStackHandlerAccessor;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.utility.IInteractionChecker;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Clearable;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class SchematicTableBlockEntity extends SmartBlockEntity implements MenuProvider, IInteractionChecker, Clearable {
   public SchematicTableBlockEntity.SchematicTableInventory inventory = new SchematicTableBlockEntity.SchematicTableInventory();
   public boolean isUploading;
   public String uploadingSchematic = null;
   public float uploadingProgress = 0.0F;
   public boolean sendUpdate;

   public SchematicTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      this.inventory.deserializeNBT(registries, compound.getCompound("Inventory"));
      super.read(compound, registries, clientPacket);
      if (clientPacket) {
         if (compound.contains("Uploading")) {
            this.isUploading = true;
            this.uploadingSchematic = compound.getString("Schematic");
            this.uploadingProgress = compound.getFloat("Progress");
         } else {
            this.isUploading = false;
            this.uploadingSchematic = null;
            this.uploadingProgress = 0.0F;
         }
      }
   }

   @Override
   protected void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      compound.put("Inventory", this.inventory.serializeNBT(registries));
      super.write(compound, registries, clientPacket);
      if (clientPacket && this.isUploading) {
         compound.putBoolean("Uploading", true);
         compound.putString("Schematic", this.uploadingSchematic);
         compound.putFloat("Progress", this.uploadingProgress);
      }
   }

   public void clearContent() {
      ((ItemStackHandlerAccessor)this.inventory).create$getStacks().clear();
   }

   @Override
   public void tick() {
      if (this.sendUpdate) {
         this.sendUpdate = false;
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 6);
      }
   }

   public void startUpload(String schematic) {
      this.isUploading = true;
      this.uploadingProgress = 0.0F;
      this.uploadingSchematic = schematic;
      this.sendUpdate = true;
      this.inventory.setStackInSlot(0, ItemStack.EMPTY);
   }

   public void finishUpload() {
      this.isUploading = false;
      this.uploadingProgress = 0.0F;
      this.uploadingSchematic = null;
      this.sendUpdate = true;
   }

   public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
      return SchematicTableMenu.create(id, inv, this);
   }

   public Component getDisplayName() {
      return CreateLang.translateDirect("gui.schematicTable.title");
   }

   @Override
   public boolean canPlayerUse(Player player) {
      return this.level != null && this.level.getBlockEntity(this.worldPosition) == this
         ? player.distanceToSqr((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 0.5, (double)this.worldPosition.getZ() + 0.5)
            <= 64.0
         : false;
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
   }

   public class SchematicTableInventory extends ItemStackHandler {
      public SchematicTableInventory() {
         super(2);
      }

      protected void onContentsChanged(int slot) {
         super.onContentsChanged(slot);
         SchematicTableBlockEntity.this.setChanged();
      }
   }
}

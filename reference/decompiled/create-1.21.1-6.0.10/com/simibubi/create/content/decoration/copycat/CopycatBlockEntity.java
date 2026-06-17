package com.simibubi.create.content.decoration.copycat;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity;
import com.simibubi.create.api.schematic.nbt.PartialSafeNBT;
import com.simibubi.create.api.schematic.requirement.SpecialBlockEntityItemRequirement;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.redstone.RoseQuartzLampBlock;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;

public class CopycatBlockEntity extends SmartBlockEntity implements SpecialBlockEntityItemRequirement, TransformableBlockEntity, PartialSafeNBT, Clearable {
   private BlockState material = AllBlocks.COPYCAT_BASE.getDefaultState();
   private ItemStack consumedItem = ItemStack.EMPTY;

   public CopycatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public BlockState getMaterial() {
      return this.material;
   }

   public boolean hasCustomMaterial() {
      return !AllBlocks.COPYCAT_BASE.has(this.getMaterial());
   }

   public void setMaterial(BlockState blockState) {
      BlockState wrapperState = this.getBlockState();
      if (!this.material.is(blockState.getBlock())) {
         for (Direction side : Iterate.directions) {
            BlockPos neighbour = this.worldPosition.relative(side);
            BlockState neighbourState = this.level.getBlockState(neighbour);
            if (neighbourState == wrapperState && this.level.getBlockEntity(neighbour) instanceof CopycatBlockEntity cbe) {
               BlockState otherMaterial = cbe.getMaterial();
               if (otherMaterial.is(blockState.getBlock())) {
                  blockState = otherMaterial;
                  break;
               }
            }
         }
      }

      this.material = blockState;
      if (!this.level.isClientSide()) {
         this.notifyUpdate();
      } else {
         this.redraw();
      }
   }

   public boolean cycleMaterial() {
      if (this.material.hasProperty(TrapDoorBlock.HALF) && this.material.getOptionalValue(TrapDoorBlock.OPEN).orElse(false)) {
         this.setMaterial((BlockState)this.material.cycle(TrapDoorBlock.HALF));
      } else if (this.material.hasProperty(BlockStateProperties.FACING)) {
         this.setMaterial((BlockState)this.material.cycle(BlockStateProperties.FACING));
      } else if (this.material.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
         this.setMaterial(
            (BlockState)this.material
               .setValue(BlockStateProperties.HORIZONTAL_FACING, ((Direction)this.material.getValue(BlockStateProperties.HORIZONTAL_FACING)).getClockWise())
         );
      } else if (this.material.hasProperty(BlockStateProperties.AXIS)) {
         this.setMaterial((BlockState)this.material.cycle(BlockStateProperties.AXIS));
      } else if (this.material.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
         this.setMaterial((BlockState)this.material.cycle(BlockStateProperties.HORIZONTAL_AXIS));
      } else if (this.material.hasProperty(BlockStateProperties.LIT)) {
         this.setMaterial((BlockState)this.material.cycle(BlockStateProperties.LIT));
      } else {
         if (!this.material.hasProperty(RoseQuartzLampBlock.POWERING)) {
            return false;
         }

         this.setMaterial((BlockState)this.material.cycle(RoseQuartzLampBlock.POWERING));
      }

      return true;
   }

   public ItemStack getConsumedItem() {
      return this.consumedItem;
   }

   public void setConsumedItem(ItemStack stack) {
      this.consumedItem = stack.copyWithCount(1);
      this.setChanged();
   }

   private void redraw() {
      if (!this.isVirtual()) {
         this.requestModelDataUpdate();
      }

      if (this.level != null) {
         this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 16);
         this.updateLight();
      }
   }

   private void updateLight() {
      if (this.level != null) {
         AuxiliaryLightManager lightManager = this.level.getAuxLightManager(this.getBlockPos());
         if (lightManager != null) {
            lightManager.setLightAt(this.getBlockPos(), this.material.getLightEmission(this.level, this.getBlockPos()));
         }
      }
   }

   public void onLoad() {
      super.onLoad();
      this.updateLight();
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
   }

   @Override
   public ItemRequirement getRequiredItems(BlockState state) {
      return this.consumedItem.isEmpty() ? ItemRequirement.NONE : new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, this.consumedItem);
   }

   @Override
   public void transform(BlockEntity be, StructureTransform transform) {
      this.material = transform.apply(this.material);
      this.notifyUpdate();
   }

   @Override
   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.read(tag, registries, clientPacket);
      this.consumedItem = ItemStack.parseOptional(registries, tag.getCompound("Item"));
      BlockState prevMaterial = this.material;
      if (!tag.contains("Material")) {
         this.consumedItem = ItemStack.EMPTY;
      } else {
         this.material = NbtUtils.readBlockState(this.blockHolderGetter(), tag.getCompound("Material"));
         if (this.material != null && !clientPacket) {
            BlockState blockState = this.getBlockState();
            if (blockState == null) {
               return;
            }

            if (!(blockState.getBlock() instanceof CopycatBlock cb)) {
               return;
            }

            BlockState acceptedBlockState = cb.getAcceptedBlockState(this.level, this.worldPosition, this.consumedItem, null);
            if (acceptedBlockState != null && this.material.is(acceptedBlockState.getBlock())) {
               return;
            }

            this.consumedItem = ItemStack.EMPTY;
            this.material = AllBlocks.COPYCAT_BASE.getDefaultState();
         }

         if (clientPacket && prevMaterial != this.material) {
            this.redraw();
         }
      }
   }

   @Override
   public void writeSafe(CompoundTag tag, Provider registries) {
      super.writeSafe(tag, registries);
      ItemStack stackWithoutComponents = new ItemStack(this.consumedItem.getItemHolder(), this.consumedItem.getCount(), DataComponentPatch.EMPTY);
      this.write(tag, registries, stackWithoutComponents, this.material);
   }

   @Override
   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.write(tag, registries, clientPacket);
      this.write(tag, registries, this.consumedItem, this.material);
   }

   protected void write(CompoundTag tag, Provider registries, ItemStack stack, BlockState material) {
      tag.put("Item", stack.saveOptional(registries));
      tag.put("Material", NbtUtils.writeBlockState(material));
   }

   public ModelData getModelData() {
      return ModelData.builder().with(CopycatModel.MATERIAL_PROPERTY, this.material).build();
   }

   public void clearContent() {
      this.material = AllBlocks.COPYCAT_BASE.getDefaultState();
      this.consumedItem = ItemStack.EMPTY;
   }
}

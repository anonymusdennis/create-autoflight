package com.simibubi.create.content.equipment.clipboard;

import com.mojang.datafixers.util.Pair;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.logistics.AddressEditBoxHelper;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import java.util.UUID;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ClipboardBlockEntity extends SmartBlockEntity {
   private UUID lastEdit;

   public ClipboardBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   public void initialize() {
      super.initialize();
      this.updateWrittenState();
   }

   public void onEditedBy(Player player) {
      this.lastEdit = player.getUUID();
      this.notifyUpdate();
      this.updateWrittenState();
   }

   @Override
   public void lazyTick() {
      super.lazyTick();
      if (this.level.isClientSide()) {
         CatnipServices.PLATFORM.executeOnClientOnly(() -> this::advertiseToAddressHelper);
      }
   }

   public void updateWrittenState() {
      BlockState blockState = this.getBlockState();
      if (AllBlocks.CLIPBOARD.has(blockState)) {
         if (!this.level.isClientSide()) {
            boolean isWritten = (Boolean)blockState.getValue(ClipboardBlock.WRITTEN);
            boolean shouldBeWritten = this.components().has(AllDataComponents.CLIPBOARD_CONTENT);
            if (isWritten != shouldBeWritten) {
               this.level.setBlockAndUpdate(this.worldPosition, (BlockState)blockState.setValue(ClipboardBlock.WRITTEN, shouldBeWritten));
            }
         }
      }
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
   }

   @Override
   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.write(tag, registries, clientPacket);
      if (clientPacket) {
         DataComponentMap.CODEC
            .encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), this.components())
            .result()
            .ifPresent(encoded -> tag.put("components", encoded));
         if (this.lastEdit != null) {
            tag.putUUID("LastEdit", this.lastEdit);
         }
      }
   }

   @Override
   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.read(tag, registries, clientPacket);
      if (clientPacket) {
         if (tag.contains("components")) {
            DataComponentMap.CODEC
               .decode(registries.createSerializationContext(NbtOps.INSTANCE), tag.getCompound("components"))
               .result()
               .<DataComponentMap>map(Pair::getFirst)
               .ifPresent(this::setComponents);
         }

         CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> this.readClientSide(tag));
      }
   }

   @OnlyIn(Dist.CLIENT)
   private void readClientSide(CompoundTag tag) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.screen instanceof ClipboardScreen cs) {
         if (!tag.contains("LastEdit") || !tag.getUUID("LastEdit").equals(mc.player.getUUID())) {
            if (this.worldPosition.equals(cs.targetedBlock)) {
               cs.reopenWith((ClipboardContent)this.components().getOrDefault(AllDataComponents.CLIPBOARD_CONTENT, ClipboardContent.EMPTY));
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   private void advertiseToAddressHelper() {
      AddressEditBoxHelper.advertiseClipboard(this);
   }

   public void setComponents(DataComponentMap components) {
      super.setComponents(components);
   }
}

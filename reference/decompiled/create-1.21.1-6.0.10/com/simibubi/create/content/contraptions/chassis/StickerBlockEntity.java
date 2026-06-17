package com.simibubi.create.content.contraptions.chassis;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.contraptions.glue.SuperGlueItem;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import java.util.List;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class StickerBlockEntity extends SmartBlockEntity {
   LerpedFloat piston = LerpedFloat.linear();
   boolean update = false;
   public AbstractComputerBehaviour computerBehaviour;

   public StickerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      if (Mods.COMPUTERCRAFT.isLoaded()) {
         event.registerBlockEntity(
            PeripheralCapability.get(), (BlockEntityType)AllBlockEntityTypes.STICKER.get(), (be, context) -> be.computerBehaviour.getPeripheralCapability()
         );
      }
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(this.computerBehaviour = ComputerCraftProxy.behaviour(this));
   }

   @Override
   public void initialize() {
      super.initialize();
      if (this.level.isClientSide) {
         this.piston.startWithValue(this.isBlockStateExtended() ? 1.0 : 0.0);
      }
   }

   public boolean isBlockStateExtended() {
      BlockState blockState = this.getBlockState();
      return AllBlocks.STICKER.has(blockState) && (Boolean)blockState.getValue(StickerBlock.EXTENDED);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level.isClientSide) {
         this.piston.tickChaser();
         if (this.isAttachedToBlock() && this.piston.getValue(0.0F) != this.piston.getValue() && this.piston.getValue() == 1.0F) {
            SuperGlueItem.spawnParticles(this.level, this.worldPosition, (Direction)this.getBlockState().getValue(StickerBlock.FACING), true);
            CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> this.playSound(true));
         }

         if (this.update) {
            this.update = false;
            int target = this.isBlockStateExtended() ? 1 : 0;
            if (this.isAttachedToBlock() && target == 0 && this.piston.getChaseTarget() == 1.0F) {
               CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> this.playSound(false));
            }

            this.piston.chase((double)target, 0.4F, Chaser.LINEAR);
            CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> VisualizationHelper.queueUpdate(this));
         }
      }
   }

   public boolean isAttachedToBlock() {
      BlockState blockState = this.getBlockState();
      if (!AllBlocks.STICKER.has(blockState)) {
         return false;
      } else {
         Direction direction = (Direction)blockState.getValue(StickerBlock.FACING);
         return SuperGlueEntity.isValidFace(this.level, this.worldPosition.relative(direction), direction.getOpposite());
      }
   }

   @Override
   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.write(tag, registries, clientPacket);
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      if (clientPacket) {
         this.update = true;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public void playSound(boolean attach) {
      AllSoundEvents.SLIME_ADDED.play(this.level, Minecraft.getInstance().player, this.worldPosition, 0.35F, attach ? 0.75F : 0.2F);
   }

   @Override
   public void invalidate() {
      super.invalidate();
      this.computerBehaviour.removePeripheral();
   }
}

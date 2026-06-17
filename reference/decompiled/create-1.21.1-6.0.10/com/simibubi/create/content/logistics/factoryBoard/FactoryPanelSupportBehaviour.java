package com.simibubi.create.content.logistics.factoryBoard;

import com.mojang.serialization.Codec;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

public class FactoryPanelSupportBehaviour extends BlockEntityBehaviour {
   public static final BehaviourType<FactoryPanelSupportBehaviour> TYPE = new BehaviourType<>();
   private List<FactoryPanelPosition> linkedPanels;
   private boolean changed;
   private Supplier<Boolean> outputPower;
   private Supplier<Boolean> isOutput;
   private Runnable onNotify;

   public FactoryPanelSupportBehaviour(SmartBlockEntity be, Supplier<Boolean> isOutput, Supplier<Boolean> outputPower, Runnable onNotify) {
      super(be);
      this.isOutput = isOutput;
      this.outputPower = outputPower;
      this.onNotify = onNotify;
      this.linkedPanels = new ArrayList<>();
   }

   public boolean shouldPanelBePowered() {
      return this.isOutput() && this.outputPower.get();
   }

   public boolean isOutput() {
      return this.isOutput.get();
   }

   public void notifyLink() {
      this.onNotify.run();
   }

   @Override
   public void destroy() {
      for (FactoryPanelPosition panelPos : this.linkedPanels) {
         if (this.getWorld().isLoaded(panelPos.pos())) {
            FactoryPanelBehaviour behaviour = FactoryPanelBehaviour.at(this.getWorld(), panelPos);
            behaviour.targetedByLinks.remove(this.getPos());
            behaviour.blockEntity.notifyUpdate();
         }
      }

      super.destroy();
   }

   public void notifyPanels() {
      if (!this.getWorld().isClientSide()) {
         Iterator<FactoryPanelPosition> iterator = this.linkedPanels.iterator();

         while (iterator.hasNext()) {
            FactoryPanelPosition panelPos = iterator.next();
            if (this.getWorld().isLoaded(panelPos.pos())) {
               FactoryPanelBehaviour behaviour = FactoryPanelBehaviour.at(this.getWorld(), panelPos);
               if (behaviour == null) {
                  iterator.remove();
                  this.changed = true;
               } else {
                  behaviour.checkForRedstoneInput();
               }
            }
         }
      }
   }

   @Nullable
   public Boolean shouldBePoweredTristate() {
      Iterator<FactoryPanelPosition> iterator = this.linkedPanels.iterator();

      while (iterator.hasNext()) {
         FactoryPanelPosition panelPos = iterator.next();
         if (!this.getWorld().isLoaded(panelPos.pos())) {
            return null;
         }

         FactoryPanelBehaviour behaviour = FactoryPanelBehaviour.at(this.getWorld(), panelPos);
         if (behaviour == null) {
            iterator.remove();
            this.changed = true;
         } else if (behaviour.isActive() && behaviour.satisfied && behaviour.count != 0) {
            return true;
         }
      }

      return false;
   }

   public List<FactoryPanelPosition> getLinkedPanels() {
      return this.linkedPanels;
   }

   public void connect(FactoryPanelBehaviour panel) {
      FactoryPanelPosition panelPosition = panel.getPanelPosition();
      if (!this.linkedPanels.contains(panelPosition)) {
         this.linkedPanels.add(panelPosition);
         this.changed = true;
      }
   }

   public void disconnect(FactoryPanelBehaviour panel) {
      this.linkedPanels.remove(panel.getPanelPosition());
      this.changed = true;
   }

   @Override
   public void tick() {
      super.tick();
      if (this.changed) {
         this.changed = false;
         if (!this.isOutput()) {
            this.notifyLink();
         }

         this.blockEntity.setChanged();
      }
   }

   @Override
   public void write(CompoundTag nbt, Provider registries, boolean clientPacket) {
      nbt.put("LinkedGauges", (Tag)CatnipCodecUtils.encode(Codec.list(FactoryPanelPosition.CODEC), registries, this.linkedPanels).orElseThrow());
   }

   @Override
   public void read(CompoundTag nbt, Provider registries, boolean clientPacket) {
      this.linkedPanels.clear();
      CatnipCodecUtils.decode(Codec.list(FactoryPanelPosition.CODEC), registries, nbt.get("LinkedGauges")).ifPresent(this.linkedPanels::addAll);
   }

   @Override
   public BehaviourType<?> getType() {
      return TYPE;
   }
}

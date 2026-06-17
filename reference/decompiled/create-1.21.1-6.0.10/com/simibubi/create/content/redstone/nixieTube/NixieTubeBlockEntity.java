package com.simibubi.create.content.redstone.nixieTube;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.content.trains.signal.SignalBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.DynamicComponent;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Optional;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NixieTubeBlockEntity extends SmartBlockEntity {
   private static final Couple<String> EMPTY = Couple.create("", "");
   private static final String EMPTY_COMPONENT_JSON = "\"\"";
   private int redstoneStrength;
   private Optional<DynamicComponent> customText = Optional.empty();
   private int nixieIndex;
   private Couple<String> displayedStrings;
   public AbstractComputerBehaviour computerBehaviour;
   private WeakReference<SignalBlockEntity> cachedSignalTE;
   @Nullable
   public SignalBlockEntity.SignalState signalState;
   @Nullable
   public NixieTubeBlockEntity.ComputerSignal computerSignal;

   public NixieTubeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.redstoneStrength = 0;
      this.cachedSignalTE = new WeakReference<>(null);
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      if (Mods.COMPUTERCRAFT.isLoaded()) {
         event.registerBlockEntity(
            PeripheralCapability.get(), (BlockEntityType)AllBlockEntityTypes.NIXIE_TUBE.get(), (be, context) -> be.computerBehaviour.getPeripheralCapability()
         );
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level.isClientSide) {
         this.signalState = null;
         if (this.computerBehaviour.hasAttachedComputer()) {
            if (this.level.isClientSide && this.cachedSignalTE.get() != null) {
               this.cachedSignalTE = new WeakReference<>(null);
            }
         } else {
            this.computerSignal = null;
            SignalBlockEntity signalBlockEntity = this.cachedSignalTE.get();
            if (signalBlockEntity != null && !signalBlockEntity.isRemoved()) {
               this.signalState = signalBlockEntity.getState();
            } else {
               Direction facing = NixieTubeBlock.getFacing(this.getBlockState());
               if (this.level.getBlockEntity(this.worldPosition.relative(facing.getOpposite())) instanceof SignalBlockEntity signal) {
                  this.signalState = signal.getState();
                  this.cachedSignalTE = new WeakReference<>(signal);
               }
            }
         }
      }
   }

   @Override
   public void initialize() {
      if (this.level.isClientSide) {
         this.updateDisplayedStrings();
      }
   }

   public boolean reactsToRedstone() {
      return !this.computerBehaviour.hasAttachedComputer() && this.customText.isEmpty();
   }

   public Couple<String> getDisplayedStrings() {
      return this.displayedStrings == null ? EMPTY : this.displayedStrings;
   }

   public MutableComponent getFullText() {
      return this.customText.map(DynamicComponent::get).orElse(Component.literal(this.redstoneStrength + ""));
   }

   public void updateRedstoneStrength(int signalStrength) {
      this.clearCustomText();
      this.redstoneStrength = signalStrength;
      DisplayLinkBlock.notifyGatherers(this.level, this.worldPosition);
      this.notifyUpdate();
   }

   public void displayCustomText(String tagElement, int nixiePositionInRow) {
      if (tagElement != null) {
         if (!this.customText.filter(d -> d.sameAs(tagElement)).isPresent()) {
            DynamicComponent component = this.customText.orElseGet(DynamicComponent::new);
            component.displayCustomText(this.level, this.worldPosition, tagElement);
            this.customText = Optional.of(component);
            this.nixieIndex = nixiePositionInRow;
            DisplayLinkBlock.notifyGatherers(this.level, this.worldPosition);
            this.notifyUpdate();
         }
      }
   }

   public void displayEmptyText(int nixiePositionInRow) {
      this.displayCustomText("\"\"", nixiePositionInRow);
   }

   public void updateDisplayedStrings() {
      if (this.signalState == null && this.computerSignal == null) {
         this.customText
            .map(DynamicComponent::resolve)
            .ifPresentOrElse(
               fullText -> this.displayedStrings = Couple.create(
                     this.charOrEmpty(fullText, this.nixieIndex * 2), this.charOrEmpty(fullText, this.nixieIndex * 2 + 1)
                  ),
               () -> this.displayedStrings = Couple.create(this.redstoneStrength < 10 ? "0" : "1", String.valueOf(this.redstoneStrength % 10))
            );
      }
   }

   public void clearCustomText() {
      this.nixieIndex = 0;
      this.customText = Optional.empty();
   }

   public int getRedstoneStrength() {
      return this.redstoneStrength;
   }

   @Override
   protected void read(CompoundTag nbt, Provider registries, boolean clientPacket) {
      super.read(nbt, registries, clientPacket);
      if (nbt.contains("CustomText")) {
         DynamicComponent component = this.customText.orElseGet(DynamicComponent::new);
         component.read(this.worldPosition, nbt, registries);
         if (component.isValid()) {
            this.customText = Optional.of(component);
            this.nixieIndex = nbt.getInt("CustomTextIndex");
         } else {
            this.customText = Optional.empty();
            this.nixieIndex = 0;
         }
      } else {
         this.customText = Optional.empty();
         this.nixieIndex = 0;
      }

      if (this.customText.isEmpty()) {
         this.redstoneStrength = nbt.getInt("RedstoneStrength");
      }

      if (clientPacket || this.isVirtual()) {
         if (nbt.contains("ComputerSignal")) {
            byte[] encodedComputerSignal = nbt.getByteArray("ComputerSignal");
            if (this.computerSignal == null) {
               this.computerSignal = new NixieTubeBlockEntity.ComputerSignal();
            }

            this.computerSignal.decode(encodedComputerSignal);
         } else {
            this.computerSignal = null;
         }

         this.updateDisplayedStrings();
      }
   }

   @Override
   protected void write(CompoundTag nbt, Provider registries, boolean clientPacket) {
      super.write(nbt, registries, clientPacket);
      if (this.customText.isPresent()) {
         nbt.putInt("CustomTextIndex", this.nixieIndex);
         this.customText.get().write(nbt, registries);
      } else {
         nbt.putInt("RedstoneStrength", this.redstoneStrength);
      }

      if (clientPacket && this.computerSignal != null) {
         nbt.putByteArray("ComputerSignal", this.computerSignal.encode());
      }
   }

   private String charOrEmpty(String string, int index) {
      return string.length() <= index ? " " : string.substring(index, index + 1);
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(this.computerBehaviour = ComputerCraftProxy.behaviour(this));
   }

   @Override
   public void invalidate() {
      super.invalidate();
      this.computerBehaviour.removePeripheral();
   }

   public static final class ComputerSignal {
      @NotNull
      public NixieTubeBlockEntity.ComputerSignal.TubeDisplay first = new NixieTubeBlockEntity.ComputerSignal.TubeDisplay();
      @NotNull
      public NixieTubeBlockEntity.ComputerSignal.TubeDisplay second = new NixieTubeBlockEntity.ComputerSignal.TubeDisplay();

      public void decode(byte[] encoded) {
         this.first.decode(encoded, 0);
         this.second.decode(encoded, 7);
      }

      public byte[] encode() {
         byte[] encoded = new byte[14];
         this.first.encode(encoded, 0);
         this.second.encode(encoded, 7);
         return encoded;
      }

      public static final class TubeDisplay {
         public static final int ENCODED_SIZE = 7;
         public byte r = 63;
         public byte g = 63;
         public byte b = 63;
         public byte blinkPeriod = 0;
         public byte blinkOffTime = 0;
         public byte glowWidth = 1;
         public byte glowHeight = 1;

         public void decode(byte[] data, int offset) {
            this.r = data[offset];
            this.g = data[offset + 1];
            this.b = data[offset + 2];
            this.blinkPeriod = data[offset + 3];
            this.blinkOffTime = data[offset + 4];
            this.glowWidth = data[offset + 5];
            this.glowHeight = data[offset + 6];
         }

         public void encode(byte[] data, int offset) {
            data[offset] = this.r;
            data[offset + 1] = this.g;
            data[offset + 2] = this.b;
            data[offset + 3] = this.blinkPeriod;
            data[offset + 4] = this.blinkOffTime;
            data[offset + 5] = this.glowWidth;
            data[offset + 6] = this.glowHeight;
         }
      }
   }
}

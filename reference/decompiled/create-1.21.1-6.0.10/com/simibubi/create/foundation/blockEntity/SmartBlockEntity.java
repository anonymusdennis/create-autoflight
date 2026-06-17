package com.simibubi.create.foundation.blockEntity;

import com.simibubi.create.api.event.BlockEntityBehaviourEvent;
import com.simibubi.create.api.schematic.nbt.PartialSafeNBT;
import com.simibubi.create.api.schematic.requirement.SpecialBlockEntityItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.IInteractionChecker;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.createmod.ponder.api.VirtualBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;

public abstract class SmartBlockEntity
   extends CachedRenderBBBlockEntity
   implements PartialSafeNBT,
   IInteractionChecker,
   SpecialBlockEntityItemRequirement,
   VirtualBlockEntity {
   private final Map<BehaviourType<?>, BlockEntityBehaviour> behaviours = new Reference2ObjectArrayMap();
   private boolean initialized = false;
   private boolean firstNbtRead = true;
   protected int lazyTickRate;
   protected int lazyTickCounter;
   private boolean chunkUnloaded;
   private boolean virtualMode;

   public SmartBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.setLazyTickRate(10);
      ArrayList<BlockEntityBehaviour> list = new ArrayList<>();
      this.addBehaviours(list);
      list.forEach(b -> this.behaviours.put(b.getType(), b));
   }

   public abstract void addBehaviours(List<BlockEntityBehaviour> var1);

   public void addBehavioursDeferred(List<BlockEntityBehaviour> behaviours) {
   }

   public void initialize() {
      if (this.firstNbtRead) {
         this.firstNbtRead = false;
         NeoForge.EVENT_BUS.post(new BlockEntityBehaviourEvent(this, this.behaviours));
      }

      this.forEachBehaviour(BlockEntityBehaviour::initialize);
      this.lazyTick();
   }

   public void tick() {
      if (!this.initialized && this.hasLevel()) {
         this.initialize();
         this.initialized = true;
      }

      if (this.lazyTickCounter-- <= 0) {
         this.lazyTickCounter = this.lazyTickRate;
         this.lazyTick();
      }

      this.forEachBehaviour(BlockEntityBehaviour::tick);
   }

   public void lazyTick() {
   }

   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.saveAdditional(tag, registries);
      this.forEachBehaviour(tb -> tb.write(tag, registries, clientPacket));
   }

   @Override
   public void writeSafe(CompoundTag tag, Provider registries) {
      super.saveAdditional(tag, registries);
      this.forEachBehaviour(tb -> {
         if (tb.isSafeNBT()) {
            tb.writeSafe(tag, registries);
         }
      });
   }

   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      if (this.firstNbtRead) {
         this.firstNbtRead = false;
         ArrayList<BlockEntityBehaviour> list = new ArrayList<>();
         this.addBehavioursDeferred(list);
         list.forEach(b -> this.behaviours.put(b.getType(), b));
         NeoForge.EVENT_BUS.post(new BlockEntityBehaviourEvent(this, this.behaviours));
      }

      super.loadAdditional(tag, registries);
      this.forEachBehaviour(tb -> tb.read(tag, registries, clientPacket));
   }

   protected void loadAdditional(@NotNull CompoundTag tag, @NotNull Provider registries) {
      this.read(tag, registries, false);
   }

   public void onChunkUnloaded() {
      super.onChunkUnloaded();
      this.chunkUnloaded = true;
   }

   public final void setRemoved() {
      super.setRemoved();
      if (!this.chunkUnloaded) {
         this.remove();
      }

      this.invalidate();
   }

   public void invalidate() {
      this.forEachBehaviour(BlockEntityBehaviour::unload);
   }

   public void remove() {
   }

   public void destroy() {
      this.forEachBehaviour(BlockEntityBehaviour::destroy);
   }

   public final void saveAdditional(CompoundTag tag, Provider registries) {
      this.write(tag, registries, false);
   }

   @Override
   public final void readClient(CompoundTag tag, Provider registries) {
      this.read(tag, registries, true);
   }

   @Override
   public final CompoundTag writeClient(CompoundTag tag, Provider registries) {
      this.write(tag, registries, true);
      return tag;
   }

   public <T extends BlockEntityBehaviour> T getBehaviour(BehaviourType<T> type) {
      return (T)this.behaviours.get(type);
   }

   public void forEachBehaviour(Consumer<BlockEntityBehaviour> action) {
      this.getAllBehaviours().forEach(action);
   }

   public Collection<BlockEntityBehaviour> getAllBehaviours() {
      return this.behaviours.values();
   }

   public void attachBehaviourLate(BlockEntityBehaviour behaviour) {
      this.behaviours.put(behaviour.getType(), behaviour);
      behaviour.blockEntity = this;
      behaviour.initialize();
   }

   @Override
   public ItemRequirement getRequiredItems(BlockState state) {
      return this.getAllBehaviours().stream().reduce(ItemRequirement.NONE, (r, b) -> r.union(b.getRequiredItems()), ItemRequirement::union);
   }

   public void removeBehaviour(BehaviourType<?> type) {
      BlockEntityBehaviour remove = this.behaviours.remove(type);
      if (remove != null) {
         remove.unload();
      }
   }

   public void setLazyTickRate(int slowTickRate) {
      this.lazyTickRate = slowTickRate;
      this.lazyTickCounter = slowTickRate;
   }

   public void markVirtual() {
      this.virtualMode = true;
   }

   public boolean isVirtual() {
      return this.virtualMode;
   }

   public boolean isChunkUnloaded() {
      return this.chunkUnloaded;
   }

   @Override
   public boolean canPlayerUse(Player player) {
      return this.level != null && this.level.getBlockEntity(this.worldPosition) == this
         ? player.distanceToSqr((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 0.5, (double)this.worldPosition.getZ() + 0.5)
            <= 64.0
         : false;
   }

   public void sendToMenu(RegistryFriendlyByteBuf buffer) {
      buffer.writeBlockPos(this.getBlockPos());
      buffer.writeNbt(this.getUpdateTag(buffer.registryAccess()));
   }

   public void refreshBlockState() {
      this.setBlockState(this.getLevel().getBlockState(this.getBlockPos()));
   }

   public void registerAwardables(List<BlockEntityBehaviour> behaviours, CreateAdvancement... advancements) {
      for (BlockEntityBehaviour behaviour : behaviours) {
         if (behaviour instanceof AdvancementBehaviour ab) {
            ab.add(advancements);
            return;
         }
      }

      behaviours.add(new AdvancementBehaviour(this, advancements));
   }

   public void award(CreateAdvancement advancement) {
      AdvancementBehaviour behaviour = this.getBehaviour(AdvancementBehaviour.TYPE);
      if (behaviour != null) {
         behaviour.awardPlayer(advancement);
      }
   }

   public void awardIfNear(CreateAdvancement advancement, int range) {
      AdvancementBehaviour behaviour = this.getBehaviour(AdvancementBehaviour.TYPE);
      if (behaviour != null) {
         behaviour.awardPlayerIfNear(advancement, range);
      }
   }
}

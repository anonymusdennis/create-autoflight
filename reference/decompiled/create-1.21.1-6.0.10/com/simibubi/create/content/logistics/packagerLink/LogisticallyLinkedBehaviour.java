package com.simibubi.create.content.logistics.packagerLink;

import com.google.common.cache.Cache;
import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.utility.TickBasedCache;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import net.createmod.catnip.data.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

public class LogisticallyLinkedBehaviour extends BlockEntityBehaviour {
   public static final BehaviourType<LogisticallyLinkedBehaviour> TYPE = new BehaviourType<>();
   public static final AtomicInteger LINK_ID_GENERATOR = new AtomicInteger();
   public int linkId;
   public int redstonePower;
   public UUID freqId;
   private boolean addedGlobally = false;
   private boolean loadedGlobally = false;
   private boolean global = false;
   private static final Cache<UUID, Cache<Integer, WeakReference<LogisticallyLinkedBehaviour>>> LINKS = new TickBasedCache<>(20, true);
   private static final Cache<UUID, Cache<Integer, WeakReference<LogisticallyLinkedBehaviour>>> CLIENT_LINKS = new TickBasedCache<>(20, true, true);

   public LogisticallyLinkedBehaviour(SmartBlockEntity be, boolean global) {
      super(be);
      this.global = global;
      this.linkId = LINK_ID_GENERATOR.getAndIncrement();
      this.freqId = UUID.randomUUID();
   }

   public static Collection<LogisticallyLinkedBehaviour> getAllPresent(UUID freq, boolean sortByPriority) {
      return getAllPresent(freq, sortByPriority, false);
   }

   public static Collection<LogisticallyLinkedBehaviour> getAllPresent(UUID freq, boolean sortByPriority, boolean clientSide) {
      Cache<Integer, WeakReference<LogisticallyLinkedBehaviour>> cache = (Cache<Integer, WeakReference<LogisticallyLinkedBehaviour>>)(clientSide
            ? CLIENT_LINKS
            : LINKS)
         .getIfPresent(freq);
      if (cache == null) {
         return Collections.emptyList();
      } else {
         Stream<LogisticallyLinkedBehaviour> stream = new LinkedList(cache.asMap().values())
            .stream()
            .map(Reference::get)
            .filter(LogisticallyLinkedBehaviour::isValidLink);
         if (sortByPriority) {
            stream = stream.sorted((e1, e2) -> Integer.compare(e1.redstonePower, e2.redstonePower));
         }

         return stream.toList();
      }
   }

   public static void keepAlive(LogisticallyLinkedBehaviour behaviour) {
      boolean onClient = behaviour.blockEntity.getLevel().isClientSide;
      if (behaviour.redstonePower != 15) {
         try {
            Cache<Integer, WeakReference<LogisticallyLinkedBehaviour>> cache = (Cache<Integer, WeakReference<LogisticallyLinkedBehaviour>>)(onClient
                  ? CLIENT_LINKS
                  : LINKS)
               .get(behaviour.freqId, () -> new TickBasedCache(400, false));
            if (cache == null) {
               return;
            }

            WeakReference<LogisticallyLinkedBehaviour> reference = (WeakReference<LogisticallyLinkedBehaviour>)cache.get(
               behaviour.linkId, () -> new WeakReference<>(behaviour)
            );
            cache.put(behaviour.linkId, reference.get() != behaviour ? new WeakReference<>(behaviour) : reference);
         } catch (ExecutionException var4) {
            var4.printStackTrace();
         }
      }
   }

   public static void remove(LogisticallyLinkedBehaviour behaviour) {
      Cache<Integer, WeakReference<LogisticallyLinkedBehaviour>> cache = (Cache<Integer, WeakReference<LogisticallyLinkedBehaviour>>)LINKS.getIfPresent(
         behaviour.freqId
      );
      if (cache != null) {
         cache.invalidate(behaviour.linkId);
      }
   }

   @Override
   public void unload() {
      if (this.loadedGlobally && this.global && this.getWorld() != null) {
         Create.LOGISTICS.linkInvalidated(this.freqId, this.getGlobalPos());
      }

      super.unload();
      remove(this);
   }

   @Override
   public void lazyTick() {
      keepAlive(this);
   }

   @Override
   public void initialize() {
      super.initialize();
      if (!this.getWorld().isClientSide) {
         if (!this.loadedGlobally && this.global) {
            this.loadedGlobally = true;
            Create.LOGISTICS.linkLoaded(this.freqId, this.getGlobalPos());
            keepAlive(this);
         }

         if (!this.addedGlobally && this.global) {
            this.addedGlobally = true;
            this.blockEntity.setChanged();
            if (this.blockEntity instanceof PackagerLinkBlockEntity plbe) {
               Create.LOGISTICS.linkAdded(this.freqId, this.getGlobalPos(), plbe.placedBy);
            }
         }
      }
   }

   private GlobalPos getGlobalPos() {
      return GlobalPos.of(this.getWorld().dimension(), this.getPos());
   }

   @Override
   public void destroy() {
      super.destroy();
      if (this.addedGlobally && this.global && this.getWorld() != null) {
         Create.LOGISTICS.linkRemoved(this.freqId, this.getGlobalPos());
      }
   }

   public void redstonePowerChanged(int power) {
      if (power != this.redstonePower) {
         this.redstonePower = power;
         this.blockEntity.setChanged();
         if (power == 15) {
            remove(this);
         } else {
            keepAlive(this);
         }
      }
   }

   public Pair<PackagerBlockEntity, PackagingRequest> processRequest(
      ItemStack stack,
      int amount,
      String address,
      int linkIndex,
      MutableBoolean finalLink,
      int orderId,
      @Nullable PackageOrderWithCrafts context,
      @Nullable IdentifiedInventory ignoredHandler
   ) {
      return this.blockEntity instanceof PackagerLinkBlockEntity plbe
         ? plbe.processRequest(stack, amount, address, linkIndex, finalLink, orderId, context, ignoredHandler)
         : null;
   }

   public InventorySummary getSummary(@Nullable IdentifiedInventory ignoredHandler) {
      return this.blockEntity instanceof PackagerLinkBlockEntity plbe ? plbe.fetchSummaryFromPackager(ignoredHandler) : InventorySummary.EMPTY;
   }

   public void deductFromAccurateSummary(ItemStackHandler packageContents) {
      InventorySummary summary = (InventorySummary)LogisticsManager.ACCURATE_SUMMARIES.getIfPresent(this.freqId);
      if (summary != null) {
         for (int i = 0; i < packageContents.getSlots(); i++) {
            ItemStack orderedStack = packageContents.getStackInSlot(i);
            if (!orderedStack.isEmpty()) {
               summary.add(orderedStack, -Math.min(summary.getCountOf(orderedStack), orderedStack.getCount()));
            }
         }
      }
   }

   public boolean mayInteract(Player player) {
      return Create.LOGISTICS.mayInteract(this.freqId, player);
   }

   public boolean mayInteractMessage(Player player) {
      boolean mayInteract = Create.LOGISTICS.mayInteract(this.freqId, player);
      if (!mayInteract) {
         player.displayClientMessage(CreateLang.translate("logistically_linked.protected").style(ChatFormatting.RED).component(), true);
      }

      return mayInteract;
   }

   public boolean mayAdministrate(Player player) {
      return Create.LOGISTICS.mayAdministrate(this.freqId, player);
   }

   public static boolean isValidLink(LogisticallyLinkedBehaviour link) {
      return link != null && !link.blockEntity.isRemoved() && !link.blockEntity.isChunkUnloaded();
   }

   @Override
   public boolean isSafeNBT() {
      return true;
   }

   @Override
   public void writeSafe(CompoundTag tag, Provider registries) {
      tag.putUUID("Freq", this.freqId);
   }

   @Override
   public void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.write(tag, registries, clientPacket);
      tag.putUUID("Freq", this.freqId);
      tag.putInt("Power", this.redstonePower);
      tag.putBoolean("Added", this.addedGlobally);
   }

   @Override
   public void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.read(tag, registries, clientPacket);
      if (tag.hasUUID("Freq")) {
         this.freqId = tag.getUUID("Freq");
      }

      this.redstonePower = tag.getInt("Power");
      this.addedGlobally = tag.getBoolean("Added");
   }

   @Override
   public BehaviourType<?> getType() {
      return TYPE;
   }

   public static enum RequestType {
      RESTOCK,
      REDSTONE,
      PLAYER;
   }
}

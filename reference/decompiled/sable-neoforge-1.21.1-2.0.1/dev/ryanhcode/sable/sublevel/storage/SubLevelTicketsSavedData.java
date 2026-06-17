package dev.ryanhcode.sable.sublevel.storage;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicket;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelTicketInfo;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;
import org.jetbrains.annotations.NotNull;

public class SubLevelTicketsSavedData extends SavedData {
   public static final String FILE_ID = "sable_sub_level_force_load_tickets";
   private final ServerLevel level;

   private SubLevelTicketsSavedData(ServerLevel level) {
      this.level = level;
   }

   public static SubLevelTicketsSavedData getOrLoad(ServerLevel level) {
      return (SubLevelTicketsSavedData)level.getChunkSource()
         .getDataStorage()
         .computeIfAbsent(
            new Factory(() -> new SubLevelTicketsSavedData(level), (tag, provider) -> load(level, tag), DataFixTypes.LEVEL),
            "sable_sub_level_force_load_tickets"
         );
   }

   private static SubLevelTicketsSavedData load(ServerLevel level, CompoundTag tag) {
      SubLevelTicketsSavedData data = new SubLevelTicketsSavedData(level);
      ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
      if (container == null) {
         return data;
      } else {
         Object2ObjectMap<UUID, SubLevelTicketInfo> newTickets = new Object2ObjectOpenHashMap();
         ListTag ticketInfos = tag.getList("tickets", 10);

         for (int i = 0; i < ticketInfos.size(); i++) {
            CompoundTag infoTag = ticketInfos.getCompound(i);
            UUID subLevelId = infoTag.getUUID("uuid");
            ListTag entriesTag = infoTag.getList("entries", 10);
            ObjectSet<SubLevelLoadingTicket<?>> tickets = new ObjectArraySet();

            for (int j = 0; j < entriesTag.size(); j++) {
               CompoundTag entryTag = entriesTag.getCompound(j);
               SubLevelLoadingTicket<?> ticket = deserializeTicket(subLevelId, entryTag);
               if (ticket != null) {
                  tickets.add(ticket);
               }
            }

            GlobalSavedSubLevelPointer pointer = null;
            if (infoTag.contains("pointer")) {
               pointer = (GlobalSavedSubLevelPointer)((Pair)GlobalSavedSubLevelPointer.CODEC.decode(NbtOps.INSTANCE, infoTag.get("pointer")).getOrThrow())
                  .getFirst();
            }

            if (!tickets.isEmpty()) {
               newTickets.put(subLevelId, new SubLevelTicketInfo(pointer, tickets));
            }
         }

         container.loadTickets(newTickets);
         return data;
      }
   }

   private static <T> SubLevelLoadingTicket<T> deserializeTicket(UUID subLevelId, CompoundTag tag) {
      ResourceLocation typeName = ResourceLocation.parse(tag.getString("type"));
      SubLevelLoadingTicketType<T> type = (SubLevelLoadingTicketType<T>)SubLevelLoadingTicketType.byName(typeName);
      if (type == null) {
         Sable.LOGGER.error("Unknown sub-level loading ticket type: {}", typeName);
         return null;
      } else {
         Tag keyTag = tag.get("key");
         T key = (T)type.codec()
            .parse(NbtOps.INSTANCE, keyTag)
            .resultOrPartial(error -> Sable.LOGGER.warn("Failed to deserialize ticket key for type {}: {}", typeName, error))
            .orElse(null);
         return key == null ? null : new SubLevelLoadingTicket<>(type, subLevelId, key);
      }
   }

   private static <T> CompoundTag serializeTicket(SubLevelLoadingTicket<T> ticket) {
      SubLevelLoadingTicketType<T> type = ticket.getType();
      Codec<T> codec = type.codec();
      return codec.encodeStart(NbtOps.INSTANCE, ticket.getKey())
         .resultOrPartial(error -> Sable.LOGGER.warn("Failed to serialize ticket key for type {}: {}", type.name(), error))
         .map(keyTag -> {
            CompoundTag tag = new CompoundTag();
            tag.putString("type", type.name().toString());
            tag.put("key", keyTag);
            return tag;
         })
         .orElse(null);
   }

   @NotNull
   public CompoundTag save(CompoundTag compoundTag, Provider provider) {
      ServerSubLevelContainer container = SubLevelContainer.getContainer(this.level);

      assert container != null : "Sub-level container is null";

      ListTag ticketInfos = new ListTag();
      Map<UUID, SubLevelTicketInfo> allTickets = container.getAllTickets();

      for (Entry<UUID, SubLevelTicketInfo> entry : allTickets.entrySet()) {
         UUID uuid = entry.getKey();
         SubLevelTicketInfo info = entry.getValue();
         GlobalSavedSubLevelPointer pointer;
         if (container.getSubLevel(uuid) instanceof ServerSubLevel serverSubLevel) {
            pointer = serverSubLevel.getLastSerializationPointer();
         } else {
            pointer = info.getPointer();
         }

         CompoundTag infoTag = new CompoundTag();
         infoTag.putUUID("uuid", uuid);
         ListTag entriesTag = new ListTag();
         ObjectIterator var14 = info.tickets().iterator();

         while (var14.hasNext()) {
            SubLevelLoadingTicket<?> ticket = (SubLevelLoadingTicket<?>)var14.next();
            CompoundTag entryTag = serializeTicket(ticket);
            if (entryTag != null) {
               entriesTag.add(entryTag);
            }
         }

         if (!entriesTag.isEmpty()) {
            if (pointer != null) {
               infoTag.put("pointer", (Tag)GlobalSavedSubLevelPointer.CODEC.encodeStart(NbtOps.INSTANCE, pointer).getOrThrow());
            } else {
               Sable.LOGGER
                  .error(
                     "Pointer is null for Sub-level loading ticket. This shouldn't happen- we won't be able to load the sub-level in on the next world load."
                  );
            }

            infoTag.put("entries", entriesTag);
            ticketInfos.add(infoTag);
         }
      }

      compoundTag.put("tickets", ticketInfos);
      return compoundTag;
   }
}

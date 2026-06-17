package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DataResult.Error;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import dev.simulated_team.simulated.Simulated;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LinkedTypewriterEntries {
   private final Int2ObjectLinkedOpenHashMap<LinkedTypewriterEntries.KeyboardEntry> keyMap = new Int2ObjectLinkedOpenHashMap();
   private final Set<LinkedTypewriterEntries.KeyboardEntry> newlyActivatedKeyboardEntries = new HashSet<>();
   private final Set<LinkedTypewriterEntries.KeyboardEntry> newlyDeactivatedKeyboardEntries = new HashSet<>();

   public static LinkedTypewriterEntries readKeys(Provider registryAccess, ListTag tags, BlockPos pos) {
      LinkedTypewriterEntries keys = new LinkedTypewriterEntries();

      for (Tag tag : tags) {
         RegistryOps<Tag> ops = registryAccess.createSerializationContext(NbtOps.INSTANCE);
         DataResult<Pair<LinkedTypewriterEntries.KeyboardEntry, Tag>> result = LinkedTypewriterEntries.KeyboardEntry.CODEC.decode(ops, tag);
         if (result.isError()) {
            Simulated.LOGGER.error(((Error)result.error().get()).message());
         } else {
            LinkedTypewriterEntries.KeyboardEntry entry = (LinkedTypewriterEntries.KeyboardEntry)((Pair)result.getOrThrow()).getFirst();
            entry.setLocation(pos);
            keys.setKey(entry.glfwKeyCode, entry);
         }
      }

      return keys;
   }

   public void updateNetworks(Level level) {
      if (!level.isClientSide) {
         for (LinkedTypewriterEntries.KeyboardEntry keyboardEntry : this.newlyActivatedKeyboardEntries) {
            Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(level, keyboardEntry);
         }

         this.newlyActivatedKeyboardEntries.clear();

         for (LinkedTypewriterEntries.KeyboardEntry keyboardEntry : this.newlyDeactivatedKeyboardEntries) {
            Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, keyboardEntry);
         }

         this.newlyDeactivatedKeyboardEntries.clear();
      }
   }

   public void activateKey(int index, LinkedTypewriterBlockEntity lbe) {
      LinkedTypewriterEntries.KeyboardEntry frequency = this.getEntry(index);
      if (frequency != null) {
         frequency.activate();
         this.newlyActivatedKeyboardEntries.add(frequency);
      }
   }

   public void deactivateKey(int index) {
      LinkedTypewriterEntries.KeyboardEntry frequency = this.getEntry(index);
      if (frequency != null) {
         frequency.deactivate();
         this.newlyDeactivatedKeyboardEntries.add(frequency);
      }
   }

   public void deactivateAll() {
      this.keyMap.forEach((index, key) -> {
         if (key.isAlive()) {
            this.newlyDeactivatedKeyboardEntries.add(key);
         }

         key.deactivate();
      });
   }

   public void setKey(int index, @Nullable LinkedTypewriterEntries.KeyboardEntry keyboardEntry) {
      if (keyboardEntry == null) {
         this.keyMap.remove(index);
      } else {
         if (this.keyMap.containsKey(index)) {
            ((LinkedTypewriterEntries.KeyboardEntry)this.keyMap.get(index)).deactivate();
         }

         this.keyMap.put(index, keyboardEntry);
      }
   }

   public LinkedTypewriterEntries.KeyboardEntry getEntry(int key) {
      return (LinkedTypewriterEntries.KeyboardEntry)this.keyMap.get(key);
   }

   public void clearAll() {
      this.deactivateAll();
      this.keyMap.clear();
      this.newlyDeactivatedKeyboardEntries.clear();
      this.newlyActivatedKeyboardEntries.clear();
   }

   public void addAll(Map<Integer, LinkedTypewriterEntries.KeyboardEntry> newMap) {
      this.keyMap.putAll(newMap);
   }

   public ListTag saveKeys(Provider registryAccess) {
      ListTag tags = new ListTag();
      if (this.keyMap.isEmpty()) {
         return tags;
      } else {
         ObjectBidirectionalIterator var3 = this.keyMap.entrySet().iterator();

         while (var3.hasNext()) {
            Entry<Integer, LinkedTypewriterEntries.KeyboardEntry> set = (Entry<Integer, LinkedTypewriterEntries.KeyboardEntry>)var3.next();
            RegistryOps<Tag> ops = registryAccess.createSerializationContext(NbtOps.INSTANCE);
            DataResult<Tag> result = LinkedTypewriterEntries.KeyboardEntry.CODEC.encodeStart(ops, set.getValue());
            if (result.isError()) {
               Simulated.LOGGER.error(((Error)result.error().get()).message());
            } else {
               tags.add((Tag)result.getOrThrow());
            }
         }

         return tags;
      }
   }

   public List<LinkedTypewriterEntries.KeyboardEntry> getEntries() {
      return List.copyOf(this.keyMap.sequencedValues());
   }

   public int getSize() {
      return this.keyMap.size();
   }

   public Map<Integer, LinkedTypewriterEntries.KeyboardEntry> getKeyMap() {
      return this.keyMap;
   }

   public static class KeyboardEntry implements IRedstoneLinkable {
      public static final Codec<LinkedTypewriterEntries.KeyboardEntry> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
                  ItemStack.OPTIONAL_CODEC.fieldOf("FirstItem").forGetter(LinkedTypewriterEntries.KeyboardEntry::getFirstAsItemStack),
                  ItemStack.OPTIONAL_CODEC.fieldOf("SecondItem").forGetter(LinkedTypewriterEntries.KeyboardEntry::getSecondAsItemStack),
                  Codec.INT.fieldOf("GLFWKey").forGetter(LinkedTypewriterEntries.KeyboardEntry::getGLFWKeyCode)
               )
               .apply(instance, LinkedTypewriterEntries.KeyboardEntry::createFromCodec)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, LinkedTypewriterEntries.KeyboardEntry> STREAM_CODEC = StreamCodec.composite(
         ItemStack.OPTIONAL_STREAM_CODEC,
         LinkedTypewriterEntries.KeyboardEntry::getFirstAsItemStack,
         ItemStack.OPTIONAL_STREAM_CODEC,
         LinkedTypewriterEntries.KeyboardEntry::getSecondAsItemStack,
         ByteBufCodecs.INT,
         LinkedTypewriterEntries.KeyboardEntry::getGLFWKeyCode,
         LinkedTypewriterEntries.KeyboardEntry::createFromCodec
      );
      public final int glfwKeyCode;
      private final Frequency first;
      private final Frequency second;
      private boolean currentlyActive;
      private BlockPos pos;

      public KeyboardEntry(Frequency first, Frequency second, int constant, BlockPos pos) {
         if (first == null) {
            first = Frequency.EMPTY;
         }

         if (second == null) {
            second = Frequency.EMPTY;
         }

         this.first = first;
         this.second = second;
         this.currentlyActive = false;
         this.pos = pos;
         this.glfwKeyCode = constant;
      }

      private KeyboardEntry(Frequency first, Frequency second, int constant) {
         this(first, second, constant, null);
      }

      public static LinkedTypewriterEntries.KeyboardEntry createFromCodec(ItemStack first, ItemStack second, int glfwKey) {
         Frequency firstFreq = Frequency.of(first);
         Frequency secondFreq = Frequency.of(second);
         return new LinkedTypewriterEntries.KeyboardEntry(firstFreq, secondFreq, glfwKey);
      }

      private static Optional<Item> mapItem(Item item) {
         return item == Items.AIR ? Optional.empty() : Optional.of(item);
      }

      @NotNull
      private static Item mapOptional(Optional<Item> optional) {
         return optional.orElse(Items.AIR);
      }

      public void activate() {
         this.currentlyActive = true;
      }

      public void deactivate() {
         this.currentlyActive = false;
      }

      public Couple<Frequency> getAsCouple() {
         return Couple.create(this.first, this.second);
      }

      public int getGLFWKeyCode() {
         return this.glfwKeyCode;
      }

      public Frequency getFirst() {
         return this.first;
      }

      public ItemStack getFirstAsItemStack() {
         return this.getFirst().getStack();
      }

      private Item getFirstItem() {
         return this.getFirstAsItemStack().getItem();
      }

      public Frequency getSecond() {
         return this.second;
      }

      public ItemStack getSecondAsItemStack() {
         return this.getSecond().getStack();
      }

      private Item getSecondItem() {
         return this.getSecondAsItemStack().getItem();
      }

      public int getTransmittedStrength() {
         return this.isAlive() ? 15 : 0;
      }

      public void setReceivedStrength(int i) {
      }

      public boolean isListening() {
         return false;
      }

      public boolean isAlive() {
         return this.currentlyActive;
      }

      public Couple<Frequency> getNetworkKey() {
         return this.getAsCouple();
      }

      public BlockPos getLocation() {
         return this.pos;
      }

      public void setLocation(BlockPos newPos) {
         this.pos = newPos;
      }
   }
}

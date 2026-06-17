package dev.simulated_team.simulated.registrate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.Builder;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.simulated_team.simulated.client.BlockPropertiesTooltip;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import dev.simulated_team.simulated.index.SimDataComponents;
import dev.simulated_team.simulated.index.SimRegistries;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;

public class SimulatedRegistrate extends CreateRegistrate {
   public static final Set<String> MODS = new HashSet<>();
   public static final List<Supplier<Item>> TAB_ITEMS = Collections.synchronizedList(new ArrayList<>());
   public static final Map<ResourceLocation, ResourceLocation> ITEM_TO_SECTION = new ConcurrentHashMap<>();
   private static final Map<ResourceLocation, Supplier<ItemLike>> NAVIGATION_TARGET_ITEMS = new ConcurrentHashMap<>();
   private ResourceLocation currentSection;

   public SimulatedRegistrate(ResourceLocation initialSection, String modId) {
      super(modId);
      this.currentSection = initialSection;
      MODS.add(modId);
   }

   public SimulatedRegistrate inSection(ResourceLocation section) {
      this.currentSection = section;
      return this;
   }

   public <T> Codec<T> byNameCodecExpanded(ResourceKey<? extends Registry<T>> key) {
      return ResourceLocation.CODEC.flatXmap(resourceLoc -> {
         T gatheredEntry = null;

         for (RegistryEntry<T, T> entry : this.getAll(key)) {
            if (entry.getId().equals(resourceLoc)) {
               gatheredEntry = (T)entry.get();
               break;
            }
         }

         return gatheredEntry != null ? DataResult.success(gatheredEntry) : DataResult.error(() -> "Unknown registry element in " + key + ":" + resourceLoc);
      }, T -> {
         ResourceLocation id = null;

         for (RegistryEntry<T, T> entry : this.getAll(key)) {
            if (entry.is(T)) {
               id = entry.getId();
               break;
            }
         }

         return id != null ? DataResult.success(id) : DataResult.error(() -> "Unknown registry element in " + key + ":" + T);
      });
   }

   public static ResourceLocation sectionOf(Item item) {
      return ITEM_TO_SECTION.get(BuiltInRegistries.ITEM.getKey(item));
   }

   @NotNull
   protected <R, T extends R> RegistryEntry<R, T> accept(
      String name,
      ResourceKey<? extends Registry<R>> type,
      Builder<R, T, ?, ?> builder,
      NonNullSupplier<? extends T> creator,
      NonNullFunction<DeferredHolder<R, T>, ? extends RegistryEntry<R, T>> entryFactory
   ) {
      RegistryEntry<R, T> entry = super.accept(name, type, builder, creator, entryFactory);
      if (type.equals(Registries.ITEM)) {
         TAB_ITEMS.add(entry::get);
         ITEM_TO_SECTION.put(entry.getId(), this.currentSection);
      }

      return entry;
   }

   public void addExtraItem(ResourceLocation item) {
      TAB_ITEMS.add(() -> (Item)BuiltInRegistries.ITEM.get(item));
      ITEM_TO_SECTION.put(item, this.currentSection);
   }

   public <T extends NavigationTarget> RegistryEntry<NavigationTarget, T> navTarget(
      String name, NonNullSupplier<T> navTableItem, Supplier<ItemLike> itemSupplier
   ) {
      RegistryEntry<NavigationTarget, T> entry = this.simple((CreateRegistrate)this.self(), name, SimRegistries.Keys.NAVIGATION_TARGET, navTableItem);
      NAVIGATION_TARGET_ITEMS.put(entry.getId(), itemSupplier);
      return entry;
   }

   public <T extends NavigationTarget> RegistryEntry<NavigationTarget, T> navTarget(String name, NonNullSupplier<T> navTableItem, ItemLike item) {
      return this.navTarget(name, navTableItem, (Supplier<ItemLike>)(() -> item));
   }

   public <T extends BlockPropertiesTooltip.Entry> RegistryEntry<BlockPropertiesTooltip.Entry, T> propertyTooltip(
      String name, NonNullSupplier<T> tooltipFunction
   ) {
      return this.simple((CreateRegistrate)this.self(), name, SimRegistries.Keys.PROPERTY_TOOLTIP, tooltipFunction);
   }

   public static void onAddDefaultComponents(BiConsumer<ItemLike, Consumer<net.minecraft.core.component.DataComponentPatch.Builder>> modify) {
      for (Entry<ResourceLocation, Supplier<ItemLike>> entry : NAVIGATION_TARGET_ITEMS.entrySet()) {
         NavigationTarget target = (NavigationTarget)SimRegistries.NAVIGATION_TARGET.get(entry.getKey());
         ItemLike item = entry.getValue().get();
         modify.accept(item, builder -> builder.set(SimDataComponents.TARGET, target));
      }
   }
}

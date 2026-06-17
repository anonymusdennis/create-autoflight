package com.simibubi.create;

import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import com.simibubi.create.content.decoration.palettes.AllPaletteBlocks;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlock;
import com.simibubi.create.content.kinetics.crank.ValveHandleBlock;
import com.simibubi.create.content.logistics.box.PackageStyles;
import com.simibubi.create.content.logistics.packagePort.postbox.PostboxBlock;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlock;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.TagDependentIngredientItem;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.CreativeModeTab.DisplayItemsGenerator;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import net.minecraft.world.item.CreativeModeTab.Output;
import net.minecraft.world.item.CreativeModeTab.TabVisibility;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.ApiStatus.Internal;

public class AllCreativeModeTabs {
   private static final DeferredRegister<CreativeModeTab> REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "create");
   public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BASE_CREATIVE_TAB = REGISTER.register(
      "base",
      () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.create.base"))
            .withTabsBefore(new ResourceKey[]{CreativeModeTabs.SPAWN_EGGS})
            .icon(() -> AllBlocks.COGWHEEL.asStack())
            .displayItems(new AllCreativeModeTabs.RegistrateDisplayItemsGenerator(true, BASE_CREATIVE_TAB))
            .build()
   );
   public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PALETTES_CREATIVE_TAB = REGISTER.register(
      "palettes",
      () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.create.palettes"))
            .withTabsBefore(new ResourceKey[]{BASE_CREATIVE_TAB.getKey()})
            .icon(() -> AllPaletteBlocks.ORNATE_IRON_WINDOW.asStack())
            .displayItems(new AllCreativeModeTabs.RegistrateDisplayItemsGenerator(false, PALETTES_CREATIVE_TAB))
            .build()
   );

   @Internal
   public static void register(IEventBus modEventBus) {
      REGISTER.register(modEventBus);
   }

   private static class RegistrateDisplayItemsGenerator implements DisplayItemsGenerator {
      private static final Predicate<Item> IS_ITEM_3D_PREDICATE;
      private final boolean addItems;
      private final DeferredHolder<CreativeModeTab, CreativeModeTab> tabFilter;

      public RegistrateDisplayItemsGenerator(boolean addItems, DeferredHolder<CreativeModeTab, CreativeModeTab> tabFilter) {
         this.addItems = addItems;
         this.tabFilter = tabFilter;
      }

      private static Predicate<Item> makeExclusionPredicate() {
         Set<Item> exclusions = new ReferenceOpenHashSet();
         List<ItemProviderEntry<?, ?>> simpleExclusions = List.of(
            AllItems.INCOMPLETE_PRECISION_MECHANISM,
            AllItems.INCOMPLETE_REINFORCED_SHEET,
            AllItems.INCOMPLETE_TRACK,
            AllItems.CHROMATIC_COMPOUND,
            AllItems.SHADOW_STEEL,
            AllItems.REFINED_RADIANCE,
            AllItems.COPPER_BACKTANK_PLACEABLE,
            AllItems.NETHERITE_BACKTANK_PLACEABLE,
            AllItems.MINECART_CONTRAPTION,
            AllItems.FURNACE_MINECART_CONTRAPTION,
            AllItems.CHEST_MINECART_CONTRAPTION,
            AllItems.SCHEMATIC,
            AllItems.SHOPPING_LIST,
            AllBlocks.ANDESITE_ENCASED_SHAFT,
            AllBlocks.BRASS_ENCASED_SHAFT,
            AllBlocks.ANDESITE_ENCASED_COGWHEEL,
            AllBlocks.BRASS_ENCASED_COGWHEEL,
            AllBlocks.ANDESITE_ENCASED_LARGE_COGWHEEL,
            AllBlocks.BRASS_ENCASED_LARGE_COGWHEEL,
            AllBlocks.MYSTERIOUS_CUCKOO_CLOCK,
            AllBlocks.ELEVATOR_CONTACT,
            AllBlocks.SHADOW_STEEL_CASING,
            AllBlocks.REFINED_RADIANCE_CASING
         );
         List<ItemEntry<TagDependentIngredientItem>> tagDependentExclusions = List.of(
            AllItems.CRUSHED_OSMIUM,
            AllItems.CRUSHED_PLATINUM,
            AllItems.CRUSHED_SILVER,
            AllItems.CRUSHED_TIN,
            AllItems.CRUSHED_LEAD,
            AllItems.CRUSHED_QUICKSILVER,
            AllItems.CRUSHED_BAUXITE,
            AllItems.CRUSHED_URANIUM,
            AllItems.CRUSHED_NICKEL
         );
         exclusions.addAll(PackageStyles.RARE_BOXES);

         for (ItemProviderEntry<?, ?> entry : simpleExclusions) {
            exclusions.add(entry.asItem());
         }

         for (ItemEntry<TagDependentIngredientItem> entry : tagDependentExclusions) {
            TagDependentIngredientItem item = (TagDependentIngredientItem)entry.get();
            if (item.shouldHide()) {
               exclusions.add(entry.asItem());
            }
         }

         return exclusions::contains;
      }

      private static List<AllCreativeModeTabs.RegistrateDisplayItemsGenerator.ItemOrdering> makeOrderings() {
         List<AllCreativeModeTabs.RegistrateDisplayItemsGenerator.ItemOrdering> orderings = new ReferenceArrayList();
         Map<ItemProviderEntry<?, ?>, ItemProviderEntry<?, ?>> simpleBeforeOrderings = Map.of(
            AllItems.EMPTY_BLAZE_BURNER, AllBlocks.BLAZE_BURNER, AllItems.SCHEDULE, AllBlocks.TRACK_STATION
         );
         Map<ItemProviderEntry<?, ?>, ItemProviderEntry<?, ?>> simpleAfterOrderings = Map.of(AllItems.VERTICAL_GEARBOX, AllBlocks.GEARBOX);
         simpleBeforeOrderings.forEach(
            (entry, otherEntry) -> orderings.add(AllCreativeModeTabs.RegistrateDisplayItemsGenerator.ItemOrdering.before(entry.asItem(), otherEntry.asItem()))
         );
         simpleAfterOrderings.forEach(
            (entry, otherEntry) -> orderings.add(AllCreativeModeTabs.RegistrateDisplayItemsGenerator.ItemOrdering.after(entry.asItem(), otherEntry.asItem()))
         );
         PackageStyles.STANDARD_BOXES.forEach(item -> {
            if (RegisteredObjectsHelper.getKeyOrThrow(item).getNamespace().equals("create")) {
               orderings.add(AllCreativeModeTabs.RegistrateDisplayItemsGenerator.ItemOrdering.after(item, AllBlocks.PACKAGER.asItem()));
            }
         });
         return orderings;
      }

      private static Function<Item, ItemStack> makeStackFunc() {
         Map<Item, Function<Item, ItemStack>> factories = new Reference2ReferenceOpenHashMap();
         Map<ItemProviderEntry<?, ?>, Function<Item, ItemStack>> simpleFactories = Map.of(AllItems.COPPER_BACKTANK, item -> {
            ItemStack stack = new ItemStack(item);
            stack.set(AllDataComponents.BACKTANK_AIR, BacktankUtil.maxAirWithoutEnchants());
            return stack;
         }, AllItems.NETHERITE_BACKTANK, item -> {
            ItemStack stack = new ItemStack(item);
            stack.set(AllDataComponents.BACKTANK_AIR, BacktankUtil.maxAirWithoutEnchants());
            return stack;
         });
         simpleFactories.forEach((entry, factory) -> factories.put(entry.asItem(), (Function<Item, ItemStack>)factory));
         return item -> {
            Function<Item, ItemStack> factory = factories.get(item);
            return factory != null ? factory.apply(item) : new ItemStack(item);
         };
      }

      private static Function<Item, TabVisibility> makeVisibilityFunc() {
         Map<Item, TabVisibility> visibilities = new Reference2ObjectOpenHashMap();
         Map<ItemProviderEntry<?, ?>, TabVisibility> simpleVisibilities = Map.of(AllItems.BLAZE_CAKE_BASE, TabVisibility.SEARCH_TAB_ONLY);
         simpleVisibilities.forEach((entryx, factory) -> visibilities.put(entryx.asItem(), factory));

         for (BlockEntry<ValveHandleBlock> entry : AllBlocks.DYED_VALVE_HANDLES) {
            visibilities.put(entry.asItem(), TabVisibility.SEARCH_TAB_ONLY);
         }

         for (BlockEntry<SeatBlock> entry : AllBlocks.SEATS) {
            SeatBlock block = (SeatBlock)entry.get();
            if (block.getColor() != DyeColor.RED) {
               visibilities.put(entry.asItem(), TabVisibility.SEARCH_TAB_ONLY);
            }
         }

         for (BlockEntry<TableClothBlock> entryx : AllBlocks.TABLE_CLOTHS) {
            TableClothBlock block = (TableClothBlock)entryx.get();
            if (block.getColor() != DyeColor.RED) {
               visibilities.put(entryx.asItem(), TabVisibility.SEARCH_TAB_ONLY);
            }
         }

         for (BlockEntry<PostboxBlock> entryxx : AllBlocks.PACKAGE_POSTBOXES) {
            PostboxBlock block = (PostboxBlock)entryxx.get();
            if (block.getColor() != DyeColor.WHITE) {
               visibilities.put(entryxx.asItem(), TabVisibility.SEARCH_TAB_ONLY);
            }
         }

         for (BlockEntry<ToolboxBlock> entryxxx : AllBlocks.TOOLBOXES) {
            ToolboxBlock block = (ToolboxBlock)entryxxx.get();
            if (block.getColor() != DyeColor.BROWN) {
               visibilities.put(entryxxx.asItem(), TabVisibility.SEARCH_TAB_ONLY);
            }
         }

         return item -> {
            TabVisibility visibility = visibilities.get(item);
            return visibility != null ? visibility : TabVisibility.PARENT_AND_SEARCH_TABS;
         };
      }

      public void accept(ItemDisplayParameters parameters, Output output) {
         Predicate<Item> exclusionPredicate = makeExclusionPredicate();
         List<AllCreativeModeTabs.RegistrateDisplayItemsGenerator.ItemOrdering> orderings = makeOrderings();
         Function<Item, ItemStack> stackFunc = makeStackFunc();
         Function<Item, TabVisibility> visibilityFunc = makeVisibilityFunc();
         List<Item> items = new LinkedList<>();
         if (this.addItems) {
            items.addAll(this.collectItems(exclusionPredicate.or(IS_ITEM_3D_PREDICATE.negate())));
         }

         items.addAll(this.collectBlocks(exclusionPredicate));
         if (this.addItems) {
            items.addAll(this.collectItems(exclusionPredicate.or(IS_ITEM_3D_PREDICATE)));
         }

         applyOrderings(items, orderings);
         outputAll(output, items, stackFunc, visibilityFunc);
      }

      private List<Item> collectBlocks(Predicate<Item> exclusionPredicate) {
         List<Item> items = new ReferenceArrayList();

         for (RegistryEntry<Block, Block> entry : Create.registrate().getAll(Registries.BLOCK)) {
            if (CreateRegistrate.isInCreativeTab(entry, this.tabFilter)) {
               Item item = ((Block)entry.get()).asItem();
               if (item != Items.AIR && !exclusionPredicate.test(item)) {
                  items.add(item);
               }
            }
         }

         return new ReferenceArrayList(new ReferenceLinkedOpenHashSet(items));
      }

      private List<Item> collectItems(Predicate<Item> exclusionPredicate) {
         List<Item> items = new ReferenceArrayList();

         for (RegistryEntry<Item, Item> entry : Create.registrate().getAll(Registries.ITEM)) {
            if (CreateRegistrate.isInCreativeTab(entry, this.tabFilter)) {
               Item item = (Item)entry.get();
               if (!(item instanceof BlockItem) && !exclusionPredicate.test(item)) {
                  items.add(item);
               }
            }
         }

         return items;
      }

      private static void applyOrderings(List<Item> items, List<AllCreativeModeTabs.RegistrateDisplayItemsGenerator.ItemOrdering> orderings) {
         for (AllCreativeModeTabs.RegistrateDisplayItemsGenerator.ItemOrdering ordering : orderings) {
            int anchorIndex = items.indexOf(ordering.anchor());
            if (anchorIndex != -1) {
               Item item = ordering.item();
               int itemIndex = items.indexOf(item);
               if (itemIndex != -1) {
                  items.remove(itemIndex);
                  if (itemIndex < anchorIndex) {
                     anchorIndex--;
                  }
               }

               if (ordering.type() == AllCreativeModeTabs.RegistrateDisplayItemsGenerator.ItemOrdering.Type.AFTER) {
                  items.add(anchorIndex + 1, item);
               } else {
                  items.add(anchorIndex, item);
               }
            }
         }
      }

      private static void outputAll(Output output, List<Item> items, Function<Item, ItemStack> stackFunc, Function<Item, TabVisibility> visibilityFunc) {
         for (Item item : items) {
            output.accept(stackFunc.apply(item), visibilityFunc.apply(item));
         }
      }

      static {
         MutableObject<Predicate<Item>> isItem3d = new MutableObject((Predicate<Item>)item -> false);
         if (CatnipServices.PLATFORM.getEnv().isClient()) {
            isItem3d.setValue((Predicate<Item>)item -> {
               ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
               BakedModel model = itemRenderer.getModel(new ItemStack(item), null, null, 0);
               return model.isGui3d();
            });
         }

         IS_ITEM_3D_PREDICATE = (Predicate<Item>)isItem3d.getValue();
      }

      private static record ItemOrdering(Item item, Item anchor, AllCreativeModeTabs.RegistrateDisplayItemsGenerator.ItemOrdering.Type type) {
         public static AllCreativeModeTabs.RegistrateDisplayItemsGenerator.ItemOrdering before(Item item, Item anchor) {
            return new AllCreativeModeTabs.RegistrateDisplayItemsGenerator.ItemOrdering(
               item, anchor, AllCreativeModeTabs.RegistrateDisplayItemsGenerator.ItemOrdering.Type.BEFORE
            );
         }

         public static AllCreativeModeTabs.RegistrateDisplayItemsGenerator.ItemOrdering after(Item item, Item anchor) {
            return new AllCreativeModeTabs.RegistrateDisplayItemsGenerator.ItemOrdering(
               item, anchor, AllCreativeModeTabs.RegistrateDisplayItemsGenerator.ItemOrdering.Type.AFTER
            );
         }

         public static enum Type {
            BEFORE,
            AFTER;
         }
      }
   }
}

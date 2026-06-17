package com.simibubi.create.content.logistics.stockTicker;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import java.util.List;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record PackageOrderWithCrafts(PackageOrder orderedStacks, List<PackageOrderWithCrafts.CraftingEntry> orderedCrafts) {
   public static final Codec<PackageOrderWithCrafts> CODEC = Codec.withAlternative(
      RecordCodecBuilder.create(
         i -> i.group(
                  PackageOrder.CODEC.fieldOf("ordered_stacks").forGetter(PackageOrderWithCrafts::orderedStacks),
                  PackageOrderWithCrafts.CraftingEntry.CODEC.listOf().fieldOf("ordered_crafts").forGetter(PackageOrderWithCrafts::orderedCrafts)
               )
               .apply(i, PackageOrderWithCrafts::new)
      ),
      RecordCodecBuilder.create(
         instance -> instance.group(Codec.list(BigItemStack.CODEC).fieldOf("entries").forGetter(PackageOrderWithCrafts::stacks))
               .apply(instance, PackageOrderWithCrafts::simple)
      )
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, PackageOrderWithCrafts> STREAM_CODEC = StreamCodec.composite(
      PackageOrder.STREAM_CODEC,
      s -> s.orderedStacks,
      CatnipStreamCodecBuilders.list(PackageOrderWithCrafts.CraftingEntry.STREAM_CODEC),
      s -> s.orderedCrafts,
      PackageOrderWithCrafts::new
   );

   public static PackageOrderWithCrafts empty() {
      return new PackageOrderWithCrafts(PackageOrder.empty(), List.of());
   }

   public static PackageOrderWithCrafts simple(List<BigItemStack> orderedStacks) {
      return new PackageOrderWithCrafts(new PackageOrder(orderedStacks), List.of());
   }

   public static PackageOrderWithCrafts singleRecipe(List<BigItemStack> pattern) {
      return new PackageOrderWithCrafts(PackageOrder.empty(), List.of(new PackageOrderWithCrafts.CraftingEntry(new PackageOrder(pattern), 1)));
   }

   public static boolean hasCraftingInformation(PackageOrderWithCrafts context) {
      return context == null ? false : context.orderedCrafts.size() == 1;
   }

   public List<BigItemStack> getCraftingInformation() {
      return this.orderedCrafts.get(0).pattern.stacks();
   }

   public List<BigItemStack> stacks() {
      return this.orderedStacks.stacks();
   }

   public boolean isEmpty() {
      return this.orderedStacks.isEmpty();
   }

   public boolean orderedStacksMatchOrderedRecipes() {
      if (this.orderedCrafts.isEmpty()) {
         return false;
      } else {
         InventorySummary stacks = new InventorySummary();
         InventorySummary crafts = new InventorySummary();
         this.stacks().forEach(stacks::add);
         this.orderedCrafts.forEach(ce -> ce.pattern.stacks().forEach(bisx -> crafts.add(new BigItemStack(bisx.stack, bisx.count * ce.count))));
         List<BigItemStack> stackEntries = stacks.getStacks();
         if (stackEntries.size() != crafts.getStacks().size()) {
            return false;
         } else {
            for (BigItemStack bis : stackEntries) {
               if (crafts.getCountOf(bis.stack) != bis.count) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   public static record CraftingEntry(PackageOrder pattern, int count) {
      public static final Codec<PackageOrderWithCrafts.CraftingEntry> CODEC = RecordCodecBuilder.create(
         i -> i.group(
                  PackageOrder.CODEC.fieldOf("pattern").forGetter(PackageOrderWithCrafts.CraftingEntry::pattern),
                  Codec.INT.fieldOf("count").forGetter(PackageOrderWithCrafts.CraftingEntry::count)
               )
               .apply(i, PackageOrderWithCrafts.CraftingEntry::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, PackageOrderWithCrafts.CraftingEntry> STREAM_CODEC = StreamCodec.composite(
         PackageOrder.STREAM_CODEC, s -> s.pattern, ByteBufCodecs.VAR_INT, s -> s.count, PackageOrderWithCrafts.CraftingEntry::new
      );
   }
}

package com.simibubi.create.content.processing.recipe;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;

public class ProcessingOutput {
   public static final ProcessingOutput EMPTY = new ProcessingOutput(ItemStack.EMPTY, 1.0F);
   public static final StreamCodec<RegistryFriendlyByteBuf, ProcessingOutput> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.registry(Registries.ITEM),
      i -> i.item,
      ByteBufCodecs.INT,
      i -> i.count,
      DataComponentPatch.STREAM_CODEC,
      i -> i.patch,
      ByteBufCodecs.FLOAT,
      i -> i.chance,
      ProcessingOutput::new
   );
   private final Item item;
   private final int count;
   private final DataComponentPatch patch;
   private final float chance;
   private ResourceLocation datagenOutput;
   @Deprecated(
      since = "6.0.3",
      forRemoval = true
   )
   @ScheduledForRemoval(
      inVersion = "1.21.1+ Port"
   )
   private static final Codec<Either<ItemStack, Pair<ResourceLocation, Integer>>> ITEM_CODEC_OLD = Codec.either(
      ItemStack.SINGLE_ITEM_CODEC, ResourceLocation.CODEC.comapFlatMap(loc -> DataResult.error(() -> "Compat cannot be deserialized"), Pair::getFirst)
   );
   @Deprecated(
      since = "6.0.3",
      forRemoval = true
   )
   @ScheduledForRemoval(
      inVersion = "1.21.1+ Port"
   )
   public static final Codec<ProcessingOutput> CODEC_OLD = RecordCodecBuilder.create(
      i -> i.group(
               ITEM_CODEC_OLD.fieldOf("item")
                  .forGetter(s -> s.datagenOutput != null ? Either.right(Pair.of(s.datagenOutput, s.count)) : Either.left(s.item.getDefaultInstance())),
               ExtraCodecs.intRange(1, 99).optionalFieldOf("count", 1).forGetter(s -> s.count),
               ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("chance", 1.0F).forGetter(s -> s.chance)
            )
            .apply(
               i,
               (item, count, chance) -> (ProcessingOutput)item.map(
                     stack -> new ProcessingOutput(stack.getItem(), count, stack.getComponentsPatch(), chance),
                     compat -> new ProcessingOutput((ResourceLocation)compat.getFirst(), (Integer)compat.getSecond(), chance)
                  )
            )
   );
   private static final Codec<Either<Item, ResourceLocation>> ITEM_CODEC = Codec.either(BuiltInRegistries.ITEM.byNameCodec(), ResourceLocation.CODEC);
   public static final Codec<ProcessingOutput> CODEC_NEW = RecordCodecBuilder.create(
      i -> i.group(
               ITEM_CODEC.fieldOf("id").forGetter(s -> s.datagenOutput != null ? Either.right(s.datagenOutput) : Either.left(s.item)),
               ExtraCodecs.intRange(1, 99).optionalFieldOf("count", 1).forGetter(s -> s.count),
               DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(s -> s.patch),
               ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("chance", 1.0F).forGetter(s -> s.chance)
            )
            .apply(
               i,
               (item, count, components, chance) -> (ProcessingOutput)item.map(
                     stack -> new ProcessingOutput(stack, count, components, chance), compat -> new ProcessingOutput(compat, count, chance)
                  )
            )
   );
   @Deprecated(
      since = "6.0.3",
      forRemoval = true
   )
   @ScheduledForRemoval(
      inVersion = "1.21.1+ Port"
   )
   public static final Codec<ProcessingOutput> CODEC = Codec.withAlternative(CODEC_NEW, CODEC_OLD);

   public ProcessingOutput(ItemStack stack, float chance) {
      this(stack.getItem(), stack.getCount(), stack.getComponentsPatch(), chance);
   }

   public ProcessingOutput(Item item, int count, float chance) {
      this(item, count, DataComponentPatch.EMPTY, chance);
   }

   public ProcessingOutput(Item item, int count, DataComponentPatch patch, float chance) {
      this.item = item;
      this.count = count;
      this.patch = patch;
      this.chance = chance;
   }

   public ProcessingOutput(ResourceLocation item, int count, float chance) {
      this(item, count, DataComponentPatch.EMPTY, chance);
   }

   public ProcessingOutput(ResourceLocation item, int count, DataComponentPatch patch, float chance) {
      this.item = Items.AIR;
      this.datagenOutput = item;
      this.count = count;
      this.patch = patch;
      this.chance = chance;
   }

   private ItemStack getStack(int count) {
      ItemStack stack = new ItemStack(this.item, count);
      if (!this.patch.isEmpty()) {
         stack.applyComponents(this.patch);
      }

      return stack;
   }

   public ItemStack getStack() {
      return this.getStack(this.count);
   }

   public float getChance() {
      return this.chance;
   }

   public ItemStack rollOutput(RandomSource randomSource) {
      if (this.chance < 1.0F) {
         int count = this.count;

         for (int roll = 0; roll < this.count; roll++) {
            if (randomSource.nextFloat() > this.chance) {
               count--;
            }
         }

         return count == 0 ? ItemStack.EMPTY : this.getStack(count);
      } else {
         return this.getStack();
      }
   }
}

package dev.eriksonn.aeronautics.index;

import com.simibubi.create.AllItems;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.content.components.Levitating;
import dev.eriksonn.aeronautics.content.items.AviatorsGogglesItem;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.ItemLike;

public class AeroItems {
   private static final SimulatedRegistrate REGISTRATE = Aeronautics.getRegistrate();
   public static final ItemEntry<AviatorsGogglesItem> AVIATORS_GOGGLES = REGISTRATE.item("aviators_goggles", AviatorsGogglesItem::new)
      .lang("Aviator's Goggles")
      .recipe(
         (c, p) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, (ItemLike)c.get(), 1)
               .requires(AeroTags.ItemTags.LEATHERS)
               .requires(AllItems.GOGGLES)
               .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllItems.GOGGLES))
               .save(p)
      )
      .tag(new TagKey[]{AeroTags.ItemTags.ARMORS})
      .tag(new TagKey[]{AeroTags.ItemTags.HEAD_ARMOR})
      .tag(new TagKey[]{ItemTags.FREEZE_IMMUNE_WEARABLES})
      .register();
   public static ItemEntry<Item> MUSIC_DISC_CLOUD_SKIPPER = REGISTRATE.item("music_disc_cloud_skipper", Item::new)
      .properties(
         p -> p.stacksTo(1)
               .rarity(Rarity.RARE)
               .jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG, Aeronautics.path("cloud_skipper")))
               .component(AeroDataComponents.LEVITATING, Levitating.DEFAULT)
      )
      .tag(new TagKey[]{AeroTags.ItemTags.MUSIC_DISCS})
      .lang("Music Disc")
      .register();
   public static ItemEntry<Item> ENDSTONE_POWDER = ingredient("end_stone_powder", p -> p.component(AeroDataComponents.LEVITATING, Levitating.END_STONE));

   private static ItemEntry<Item> ingredient(String name, NonNullUnaryOperator<Properties> poperator) {
      return REGISTRATE.item(name, Item::new).properties(poperator).register();
   }

   public static void init() {
   }
}

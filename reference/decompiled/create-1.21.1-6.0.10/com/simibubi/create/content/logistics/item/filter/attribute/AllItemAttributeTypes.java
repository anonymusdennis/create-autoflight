package com.simibubi.create.content.logistics.item.filter.attribute;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.Create;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.logistics.item.filter.attribute.attributes.AddedByAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.attributes.BookAuthorAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.attributes.BookCopyAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.attributes.ColorAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.attributes.EnchantAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.attributes.FluidContentsAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.attributes.InItemGroupAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.attributes.InTagAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.attributes.ItemNameAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.attributes.ShulkerFillLevelAttribute;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlot.Type;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;

public class AllItemAttributeTypes {
   public static final ItemAttributeType PLACEABLE = singleton("placeable", s -> s.getItem() instanceof BlockItem);
   public static final ItemAttributeType CONSUMABLE = singleton("consumable", s -> s.has(DataComponents.FOOD));
   public static final ItemAttributeType FLUID_CONTAINER = singleton("fluid_container", s -> s.getCapability(FluidHandler.ITEM) != null);
   public static final ItemAttributeType ENCHANTED = singleton("enchanted", ItemStack::isEnchanted);
   public static final ItemAttributeType MAX_ENCHANTED = singleton("max_enchanted", AllItemAttributeTypes::maxEnchanted);
   public static final ItemAttributeType RENAMED = singleton("renamed", s -> s.has(DataComponents.CUSTOM_NAME));
   public static final ItemAttributeType DAMAGED = singleton("damaged", ItemStack::isDamaged);
   public static final ItemAttributeType BADLY_DAMAGED = singleton(
      "badly_damaged", s -> s.isDamaged() && (float)s.getDamageValue() / (float)s.getMaxDamage() > 0.75F
   );
   public static final ItemAttributeType NOT_STACKABLE = singleton("not_stackable", (ItemStack::isStackable).negate());
   public static final ItemAttributeType EQUIPABLE = singleton("equipable", s -> {
      Equipable equipable = Equipable.get(s);
      Type type = equipable != null ? equipable.getEquipmentSlot().getType() : EquipmentSlot.MAINHAND.getType();
      return type != Type.HAND;
   });
   public static final ItemAttributeType FURNACE_FUEL = singleton("furnace_fuel", AbstractFurnaceBlockEntity::isFuel);
   public static final ItemAttributeType WASHABLE = singleton("washable", AllFanProcessingTypes.SPLASHING::canProcess);
   public static final ItemAttributeType HAUNTABLE = singleton("hauntable", AllFanProcessingTypes.HAUNTING::canProcess);
   public static final ItemAttributeType CRUSHABLE = singleton(
      "crushable", (s, w) -> testRecipe(s, w, AllRecipeTypes.CRUSHING.getType()) || testRecipe(s, w, AllRecipeTypes.MILLING.getType())
   );
   public static final ItemAttributeType SMELTABLE = singleton("smeltable", (s, w) -> testRecipe(s, w, RecipeType.SMELTING));
   public static final ItemAttributeType SMOKABLE = singleton("smokable", (s, w) -> testRecipe(s, w, RecipeType.SMOKING));
   public static final ItemAttributeType BLASTABLE = singleton("blastable", (s, w) -> testRecipe(s, w, RecipeType.BLASTING));
   public static final ItemAttributeType COMPOSTABLE = singleton("compostable", s -> ComposterBlock.getValue(s) > 0.0F);
   public static final ItemAttributeType IN_TAG = register("in_tag", new InTagAttribute.Type());
   public static final ItemAttributeType IN_ITEM_GROUP = register("in_item_group", new InItemGroupAttribute.Type());
   public static final ItemAttributeType ADDED_BY = register("added_by", new AddedByAttribute.Type());
   public static final ItemAttributeType HAS_ENCHANT = register("has_enchant", new EnchantAttribute.Type());
   public static final ItemAttributeType SHULKER_FILL_LEVEL = register("shulker_fill_level", new ShulkerFillLevelAttribute.Type());
   public static final ItemAttributeType HAS_COLOR = register("has_color", new ColorAttribute.Type());
   public static final ItemAttributeType HAS_FLUID = register("has_fluid", new FluidContentsAttribute.Type());
   public static final ItemAttributeType HAS_NAME = register("has_name", new ItemNameAttribute.Type());
   public static final ItemAttributeType BOOK_AUTHOR = register("book_author", new BookAuthorAttribute.Type());
   public static final ItemAttributeType BOOK_COPY = register("book_copy", new BookCopyAttribute.Type());

   private static <T extends Recipe<SingleRecipeInput>> boolean testRecipe(ItemStack s, Level w, RecipeType<T> type) {
      return w.getRecipeManager().getRecipeFor(type, new SingleRecipeInput(s.copy()), w).isPresent();
   }

   private static boolean maxEnchanted(ItemStack s) {
      for (Entry<Holder<Enchantment>> entry : s.getTagEnchantments().entrySet()) {
         if (((Enchantment)((Holder)entry.getKey()).value()).getMaxLevel() <= entry.getIntValue()) {
            return true;
         }
      }

      return false;
   }

   private static ItemAttributeType singleton(String id, Predicate<ItemStack> predicate) {
      return register(id, new SingletonItemAttribute.Type(type -> new SingletonItemAttribute(type, (stack, level) -> predicate.test(stack), id)));
   }

   private static ItemAttributeType singleton(String id, BiPredicate<ItemStack, Level> predicate) {
      return register(id, new SingletonItemAttribute.Type(type -> new SingletonItemAttribute(type, predicate, id)));
   }

   private static ItemAttributeType register(String id, ItemAttributeType type) {
      return (ItemAttributeType)Registry.register(CreateBuiltInRegistries.ITEM_ATTRIBUTE_TYPE, Create.asResource(id), type);
   }

   public static void init() {
   }
}

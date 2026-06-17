package com.simibubi.create.content.equipment.armor;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.mixin.accessor.ItemModelGeneratorsAccessor;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.Map;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.ItemModelGenerators.TrimModelData;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.data.models.model.TextureMapping;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;

public class TrimmableArmorModelGenerator {
   public static final VarHandle TEXTURES_HANDLE;

   public static <T extends ArmorItem> void generate(DataGenContext<Item, T> c, RegistrateItemModelProvider p) {
      T item = (T)c.get();
      ItemModelBuilder builder = p.generated(c);

      for (TrimModelData data : ItemModelGeneratorsAccessor.create$getGENERATED_TRIM_MODELS()) {
         ResourceLocation modelLoc = ModelLocationUtils.getModelLocation(item);
         ResourceLocation textureLoc = TextureMapping.getItemTexture(item);
         String trimId = data.name(item.getMaterial());
         ResourceLocation trimModelLoc = modelLoc.withSuffix("_" + trimId + "_trim");
         ResourceLocation trimLoc = ResourceLocation.withDefaultNamespace("trims/items/" + item.getType().getName() + "_trim_" + trimId);
         String parent = "item/generated";
         if (item.getMaterial() == AllArmorMaterials.CARDBOARD) {
            trimLoc = Create.asResource("trims/items/card_" + item.getType().getName() + "_trim_" + trimId);
         }

         ItemModelBuilder itemModel = (ItemModelBuilder)((ItemModelBuilder)p.withExistingParent(trimModelLoc.getPath(), parent)).texture("layer0", textureLoc);
         Map<String, String> textures = (Map)TEXTURES_HANDLE.get((ItemModelBuilder)itemModel);
         textures.put("layer1", trimLoc.toString());
         builder.override().predicate(ItemModelGenerators.TRIM_TYPE_PREDICATE_ID, data.itemModelIndex()).model(itemModel).end();
      }
   }

   static {
      try {
         Lookup lookup = MethodHandles.privateLookupIn(ModelBuilder.class, MethodHandles.lookup());
         TEXTURES_HANDLE = lookup.findVarHandle(ModelBuilder.class, "textures", Map.class);
      } catch (NoSuchFieldException | IllegalAccessException var1) {
         throw new RuntimeException(var1);
      }
   }
}

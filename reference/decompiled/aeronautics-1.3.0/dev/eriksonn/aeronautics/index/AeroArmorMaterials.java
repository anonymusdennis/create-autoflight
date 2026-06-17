package dev.eriksonn.aeronautics.index;

import dev.eriksonn.aeronautics.Aeronautics;
import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.ArmorMaterial.Layer;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

public class AeroArmorMaterials {
   private static final RegistrationProvider<ArmorMaterial> REGISTRY = RegistrationProvider.get(Registries.ARMOR_MATERIAL, "aeronautics");
   public static final RegistryObject<ArmorMaterial> AVIATORS_GOGGLES = REGISTRY.register(
      "aviators_goggles",
      () -> new ArmorMaterial(
            new Object2ObjectOpenHashMap<Type, Integer>() {
               {
                  this.put(Type.HELMET, 1);
               }
            },
            15,
            SoundEvents.ARMOR_EQUIP_LEATHER,
            () -> Ingredient.of(new ItemLike[]{Items.LEATHER}),
            List.of(new Layer(Aeronautics.path("aviators_goggles"))),
            0.0F,
            0.0F
         )
   );

   public static void init() {
   }
}

package dev.ryanhcode.sable.physics.config.block_properties;

import com.mojang.serialization.Codec;
import dev.ryanhcode.sable.Sable;
import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class PhysicsBlockPropertyTypes {
   public static final ResourceKey<Registry<PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(
      Sable.sablePath("physics_block_properties")
   );
   private static final RegistrationProvider<PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<?>> VANILLA_PROVIDER = RegistrationProvider.get(
      REGISTRY_KEY, "sable"
   );
   private static final Registry<PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<?>> REGISTRY = VANILLA_PROVIDER.asVanillaRegistry();
   public static final RegistryObject<PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<Double>> MASS = register(Sable.sablePath("mass"), Codec.DOUBLE, 1.0);
   public static final RegistryObject<PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<Vec3>> INERTIA = register(Sable.sablePath("inertia"), Vec3.CODEC, null);
   public static final RegistryObject<PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<Double>> VOLUME = register(
      Sable.sablePath("volume"), Codec.DOUBLE, 1.0
   );
   public static final RegistryObject<PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<Double>> RESTITUTION = register(
      Sable.sablePath("restitution"), Codec.DOUBLE, 0.0
   );
   public static final RegistryObject<PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<Double>> FRICTION = register(
      Sable.sablePath("friction"), Codec.DOUBLE, 1.0
   );
   public static final RegistryObject<PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<Boolean>> FRAGILE = register(
      Sable.sablePath("fragile"), Codec.BOOL, false
   );
   public static final RegistryObject<PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<ResourceLocation>> FLOATING_MATERIAL = register(
      Sable.sablePath("floating_material"), ResourceLocation.CODEC, null
   );
   public static final RegistryObject<PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<Double>> FLOATING_SCALE = register(
      Sable.sablePath("floating_scale"), Codec.DOUBLE, 1.0
   );

   public static void register() {
   }

   private static <T> RegistryObject<PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<T>> register(ResourceLocation id, Codec<T> codec, T defaultValue) {
      if (REGISTRY.containsKey(id)) {
         throw new IllegalArgumentException("Duplicate physics block property: %s".formatted(id));
      } else {
         return VANILLA_PROVIDER.register(id, () -> new PhysicsBlockPropertyTypes.PhysicsBlockPropertyType(REGISTRY.size(), codec, defaultValue));
      }
   }

   public static int count() {
      return REGISTRY.size();
   }

   public static Codec<Object> getPropertyCodec(ResourceLocation id) {
      PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<?> property = (PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<?>)REGISTRY.get(id);
      if (property != null) {
         return (Codec<Object>)property.codec;
      } else {
         throw new IllegalArgumentException("Unknown physics block property: %s".formatted(id));
      }
   }

   public static PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<?> getPropertyType(ResourceLocation id) {
      PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<?> property = (PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<?>)REGISTRY.get(id);
      if (property != null) {
         return property;
      } else {
         throw new IllegalArgumentException("Unknown physics block property: %s".formatted(id));
      }
   }

   public static record PhysicsBlockPropertyType<T>(int id, Codec<T> codec, T defaultValue) {
   }
}

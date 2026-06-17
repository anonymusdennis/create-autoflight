package com.simibubi.create.content.equipment.potatoCannon;

import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.api.equipment.potatoCannon.PotatoCannonProjectileType;
import com.simibubi.create.api.registry.CreateRegistries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

public class AllPotatoProjectileTypes {
   public static final ResourceKey<PotatoCannonProjectileType> FALLBACK = ResourceKey.create(
      CreateRegistries.POTATO_PROJECTILE_TYPE, Create.asResource("fallback")
   );

   public static void bootstrap(BootstrapContext<PotatoCannonProjectileType> ctx) {
      register(ctx, "fallback", new PotatoCannonProjectileType.Builder().damage(0).build());
      register(
         ctx,
         "potato",
         new PotatoCannonProjectileType.Builder()
            .damage(5)
            .reloadTicks(15)
            .velocity(1.25F)
            .knockback(1.5F)
            .renderTumbling()
            .onBlockHit(new AllPotatoProjectileBlockHitActions.PlantCrop(Blocks.POTATOES))
            .addItems(Items.POTATO)
            .build()
      );
      register(
         ctx,
         "baked_potato",
         new PotatoCannonProjectileType.Builder()
            .damage(5)
            .reloadTicks(15)
            .velocity(1.25F)
            .knockback(0.5F)
            .renderTumbling()
            .preEntityHit(AllPotatoProjectileEntityHitActions.SetOnFire.seconds(3))
            .addItems(Items.BAKED_POTATO)
            .build()
      );
      register(
         ctx,
         "carrot",
         new PotatoCannonProjectileType.Builder()
            .damage(4)
            .reloadTicks(12)
            .velocity(1.45F)
            .knockback(0.3F)
            .renderTowardMotion(140, 1.0F)
            .soundPitch(1.5F)
            .onBlockHit(new AllPotatoProjectileBlockHitActions.PlantCrop(Blocks.CARROTS))
            .addItems(Items.CARROT)
            .build()
      );
      register(
         ctx,
         "golden_carrot",
         new PotatoCannonProjectileType.Builder()
            .damage(12)
            .reloadTicks(15)
            .velocity(1.45F)
            .knockback(0.5F)
            .renderTowardMotion(140, 2.0F)
            .soundPitch(1.5F)
            .addItems(Items.GOLDEN_CARROT)
            .build()
      );
      register(
         ctx,
         "sweet_berry",
         new PotatoCannonProjectileType.Builder()
            .damage(3)
            .reloadTicks(10)
            .knockback(0.1F)
            .velocity(1.05F)
            .renderTumbling()
            .splitInto(3)
            .soundPitch(1.25F)
            .addItems(Items.SWEET_BERRIES)
            .build()
      );
      register(
         ctx,
         "glow_berry",
         new PotatoCannonProjectileType.Builder()
            .damage(2)
            .reloadTicks(10)
            .knockback(0.05F)
            .velocity(1.05F)
            .renderTumbling()
            .splitInto(2)
            .soundPitch(1.2F)
            .onEntityHit(new AllPotatoProjectileEntityHitActions.PotionEffect(MobEffects.GLOWING, 1, 200, false))
            .addItems(Items.GLOW_BERRIES)
            .build()
      );
      register(
         ctx,
         "chocolate_berry",
         new PotatoCannonProjectileType.Builder()
            .damage(4)
            .reloadTicks(10)
            .knockback(0.2F)
            .velocity(1.05F)
            .renderTumbling()
            .splitInto(3)
            .soundPitch(1.25F)
            .addItems((ItemLike)AllItems.CHOCOLATE_BERRIES.get())
            .build()
      );
      register(
         ctx,
         "poison_potato",
         new PotatoCannonProjectileType.Builder()
            .damage(5)
            .reloadTicks(15)
            .knockback(0.05F)
            .velocity(1.25F)
            .renderTumbling()
            .onEntityHit(new AllPotatoProjectileEntityHitActions.PotionEffect(MobEffects.POISON, 1, 160, true))
            .addItems(Items.POISONOUS_POTATO)
            .build()
      );
      register(
         ctx,
         "chorus_fruit",
         new PotatoCannonProjectileType.Builder()
            .damage(3)
            .reloadTicks(15)
            .velocity(1.2F)
            .knockback(0.05F)
            .renderTumbling()
            .onEntityHit(new AllPotatoProjectileEntityHitActions.ChorusTeleport(20.0))
            .addItems(Items.CHORUS_FRUIT)
            .build()
      );
      register(
         ctx,
         "apple",
         new PotatoCannonProjectileType.Builder()
            .damage(5)
            .reloadTicks(10)
            .velocity(1.45F)
            .knockback(0.5F)
            .renderTumbling()
            .soundPitch(1.1F)
            .addItems(Items.APPLE)
            .build()
      );
      register(
         ctx,
         "honeyed_apple",
         new PotatoCannonProjectileType.Builder()
            .damage(6)
            .reloadTicks(15)
            .velocity(1.35F)
            .knockback(0.1F)
            .renderTumbling()
            .soundPitch(1.1F)
            .onEntityHit(new AllPotatoProjectileEntityHitActions.PotionEffect(MobEffects.MOVEMENT_SLOWDOWN, 2, 160, true))
            .addItems((ItemLike)AllItems.HONEYED_APPLE.get())
            .build()
      );
      register(
         ctx,
         "golden_apple",
         new PotatoCannonProjectileType.Builder()
            .damage(1)
            .reloadTicks(100)
            .velocity(1.45F)
            .knockback(0.05F)
            .renderTumbling()
            .soundPitch(1.1F)
            .onEntityHit(AllPotatoProjectileEntityHitActions.CureZombieVillager.INSTANCE)
            .addItems(Items.GOLDEN_APPLE)
            .build()
      );
      register(
         ctx,
         "enchanted_golden_apple",
         new PotatoCannonProjectileType.Builder()
            .damage(1)
            .reloadTicks(100)
            .velocity(1.45F)
            .knockback(0.05F)
            .renderTumbling()
            .soundPitch(1.1F)
            .onEntityHit(new AllPotatoProjectileEntityHitActions.FoodEffects(Foods.ENCHANTED_GOLDEN_APPLE, false))
            .addItems(Items.ENCHANTED_GOLDEN_APPLE)
            .build()
      );
      register(
         ctx,
         "beetroot",
         new PotatoCannonProjectileType.Builder()
            .damage(2)
            .reloadTicks(5)
            .velocity(1.6F)
            .knockback(0.1F)
            .renderTowardMotion(140, 2.0F)
            .soundPitch(1.6F)
            .addItems(Items.BEETROOT)
            .build()
      );
      register(
         ctx,
         "melon_slice",
         new PotatoCannonProjectileType.Builder()
            .damage(3)
            .reloadTicks(8)
            .knockback(0.1F)
            .velocity(1.45F)
            .renderTumbling()
            .soundPitch(1.5F)
            .addItems(Items.MELON_SLICE)
            .build()
      );
      register(
         ctx,
         "glistering_melon",
         new PotatoCannonProjectileType.Builder()
            .damage(5)
            .reloadTicks(8)
            .knockback(0.1F)
            .velocity(1.45F)
            .renderTumbling()
            .soundPitch(1.5F)
            .onEntityHit(new AllPotatoProjectileEntityHitActions.PotionEffect(MobEffects.GLOWING, 1, 100, true))
            .addItems(Items.GLISTERING_MELON_SLICE)
            .build()
      );
      register(
         ctx,
         "melon_block",
         new PotatoCannonProjectileType.Builder()
            .damage(8)
            .reloadTicks(20)
            .knockback(2.0F)
            .velocity(0.95F)
            .renderTumbling()
            .soundPitch(0.9F)
            .onBlockHit(new AllPotatoProjectileBlockHitActions.PlaceBlockOnGround(Blocks.MELON))
            .addItems(Blocks.MELON)
            .build()
      );
      register(
         ctx,
         "pumpkin_block",
         new PotatoCannonProjectileType.Builder()
            .damage(6)
            .reloadTicks(15)
            .knockback(2.0F)
            .velocity(0.95F)
            .renderTumbling()
            .soundPitch(0.9F)
            .onBlockHit(new AllPotatoProjectileBlockHitActions.PlaceBlockOnGround(Blocks.PUMPKIN))
            .addItems(Blocks.PUMPKIN)
            .build()
      );
      register(
         ctx,
         "pumpkin_pie",
         new PotatoCannonProjectileType.Builder()
            .damage(7)
            .reloadTicks(15)
            .knockback(0.05F)
            .velocity(1.1F)
            .renderTumbling()
            .sticky()
            .soundPitch(1.1F)
            .addItems(Items.PUMPKIN_PIE)
            .build()
      );
      register(
         ctx,
         "cake",
         new PotatoCannonProjectileType.Builder()
            .damage(8)
            .reloadTicks(15)
            .knockback(0.1F)
            .velocity(1.1F)
            .renderTumbling()
            .sticky()
            .addItems(Items.CAKE)
            .build()
      );
      register(
         ctx,
         "blaze_cake",
         new PotatoCannonProjectileType.Builder()
            .damage(15)
            .reloadTicks(20)
            .knockback(0.3F)
            .velocity(1.1F)
            .renderTumbling()
            .sticky()
            .preEntityHit(AllPotatoProjectileEntityHitActions.SetOnFire.seconds(12))
            .addItems((ItemLike)AllItems.BLAZE_CAKE.get())
            .build()
      );
      register(
         ctx,
         "fish",
         new PotatoCannonProjectileType.Builder()
            .damage(4)
            .knockback(0.6F)
            .velocity(1.3F)
            .renderTowardMotion(140, 1.0F)
            .sticky()
            .soundPitch(1.3F)
            .addItems(Items.COD, Items.COOKED_COD, Items.SALMON, Items.COOKED_SALMON, Items.TROPICAL_FISH)
            .build()
      );
      register(
         ctx,
         "pufferfish",
         new PotatoCannonProjectileType.Builder()
            .damage(4)
            .knockback(0.4F)
            .velocity(1.1F)
            .renderTowardMotion(140, 1.0F)
            .sticky()
            .onEntityHit(new AllPotatoProjectileEntityHitActions.FoodEffects(Foods.PUFFERFISH, false))
            .soundPitch(1.1F)
            .addItems(Items.PUFFERFISH)
            .build()
      );
      register(
         ctx,
         "suspicious_stew",
         new PotatoCannonProjectileType.Builder()
            .damage(3)
            .reloadTicks(40)
            .knockback(0.2F)
            .velocity(0.8F)
            .renderTowardMotion(140, 1.0F)
            .dropStack(Items.BOWL.getDefaultInstance())
            .onEntityHit(AllPotatoProjectileEntityHitActions.SuspiciousStew.INSTANCE)
            .addItems(Items.SUSPICIOUS_STEW)
            .build()
      );
   }

   private static void register(BootstrapContext<PotatoCannonProjectileType> ctx, String name, PotatoCannonProjectileType type) {
      ctx.register(ResourceKey.create(CreateRegistries.POTATO_PROJECTILE_TYPE, Create.asResource(name)), type);
   }
}

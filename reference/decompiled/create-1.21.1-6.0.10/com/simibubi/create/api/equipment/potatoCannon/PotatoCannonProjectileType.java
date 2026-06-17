package com.simibubi.create.api.equipment.potatoCannon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.equipment.potatoCannon.AllPotatoProjectileRenderModes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public record PotatoCannonProjectileType(
   HolderSet<Item> items,
   int reloadTicks,
   int damage,
   int split,
   float knockback,
   float drag,
   float velocityMultiplier,
   float gravityMultiplier,
   float soundPitch,
   boolean sticky,
   ItemStack dropStack,
   PotatoProjectileRenderMode renderMode,
   Optional<PotatoProjectileEntityHitAction> preEntityHit,
   Optional<PotatoProjectileEntityHitAction> onEntityHit,
   Optional<PotatoProjectileBlockHitAction> onBlockHit
) {
   public static final Codec<PotatoCannonProjectileType> CODEC = RecordCodecBuilder.create(
      i -> i.group(
               RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("items").forGetter(PotatoCannonProjectileType::items),
               Codec.INT.optionalFieldOf("reload_ticks", 10).forGetter(PotatoCannonProjectileType::reloadTicks),
               Codec.INT.optionalFieldOf("damage", 1).forGetter(PotatoCannonProjectileType::damage),
               Codec.INT.optionalFieldOf("split", 1).forGetter(PotatoCannonProjectileType::split),
               Codec.FLOAT.optionalFieldOf("knockback", 1.0F).forGetter(PotatoCannonProjectileType::knockback),
               Codec.FLOAT.optionalFieldOf("drag", 0.99F).forGetter(PotatoCannonProjectileType::drag),
               Codec.FLOAT.optionalFieldOf("velocity_multiplier", 1.0F).forGetter(PotatoCannonProjectileType::velocityMultiplier),
               Codec.FLOAT.optionalFieldOf("gravity_multiplier", 1.0F).forGetter(PotatoCannonProjectileType::gravityMultiplier),
               Codec.FLOAT.optionalFieldOf("sound_pitch", 1.0F).forGetter(PotatoCannonProjectileType::soundPitch),
               Codec.BOOL.optionalFieldOf("sticky", false).forGetter(PotatoCannonProjectileType::sticky),
               ItemStack.CODEC.optionalFieldOf("drop_stack", ItemStack.EMPTY).forGetter(PotatoCannonProjectileType::dropStack),
               PotatoProjectileRenderMode.CODEC
                  .optionalFieldOf("render_mode", AllPotatoProjectileRenderModes.Billboard.INSTANCE)
                  .forGetter(PotatoCannonProjectileType::renderMode),
               PotatoProjectileEntityHitAction.CODEC.optionalFieldOf("pre_entity_hit").forGetter(p -> p.preEntityHit),
               PotatoProjectileEntityHitAction.CODEC.optionalFieldOf("on_entity_hit").forGetter(p -> p.onEntityHit),
               PotatoProjectileBlockHitAction.CODEC.optionalFieldOf("on_block_hit").forGetter(p -> p.onBlockHit)
            )
            .apply(i, PotatoCannonProjectileType::new)
   );

   public static Optional<Reference<PotatoCannonProjectileType>> getTypeForItem(RegistryAccess registryAccess, Item item) {
      return registryAccess.lookupOrThrow(CreateRegistries.POTATO_PROJECTILE_TYPE)
         .listElements()
         .filter(ref -> ((PotatoCannonProjectileType)ref.value()).items.contains(item.builtInRegistryHolder()))
         .findFirst();
   }

   public boolean preEntityHit(ItemStack stack, EntityHitResult ray) {
      return this.preEntityHit.<Boolean>map(i -> i.execute(stack, ray, PotatoProjectileEntityHitAction.Type.PRE_HIT)).orElse(false);
   }

   public boolean onEntityHit(ItemStack stack, EntityHitResult ray) {
      return this.onEntityHit.<Boolean>map(i -> i.execute(stack, ray, PotatoProjectileEntityHitAction.Type.ON_HIT)).orElse(false);
   }

   public boolean onBlockHit(LevelAccessor level, ItemStack stack, BlockHitResult ray) {
      return this.onBlockHit.<Boolean>map(i -> i.execute(level, stack, ray)).orElse(false);
   }

   public ItemStack dropStack() {
      return this.dropStack.copy();
   }

   public static class Builder {
      private final List<Holder<Item>> items = new ArrayList<>();
      private int reloadTicks = 10;
      private int damage = 1;
      private int split = 1;
      private float knockback = 1.0F;
      private float drag = 0.99F;
      private float velocityMultiplier = 1.0F;
      private float gravityMultiplier = 1.0F;
      private float soundPitch = 1.0F;
      private boolean sticky = false;
      private ItemStack dropStack = ItemStack.EMPTY;
      private PotatoProjectileRenderMode renderMode = AllPotatoProjectileRenderModes.Billboard.INSTANCE;
      private PotatoProjectileEntityHitAction preEntityHit = null;
      private PotatoProjectileEntityHitAction onEntityHit = null;
      private PotatoProjectileBlockHitAction onBlockHit = null;

      public PotatoCannonProjectileType.Builder reloadTicks(int reload) {
         this.reloadTicks = reload;
         return this;
      }

      public PotatoCannonProjectileType.Builder damage(int damage) {
         this.damage = damage;
         return this;
      }

      public PotatoCannonProjectileType.Builder splitInto(int split) {
         this.split = split;
         return this;
      }

      public PotatoCannonProjectileType.Builder knockback(float knockback) {
         this.knockback = knockback;
         return this;
      }

      public PotatoCannonProjectileType.Builder drag(float drag) {
         this.drag = drag;
         return this;
      }

      public PotatoCannonProjectileType.Builder velocity(float velocity) {
         this.velocityMultiplier = velocity;
         return this;
      }

      public PotatoCannonProjectileType.Builder gravity(float modifier) {
         this.gravityMultiplier = modifier;
         return this;
      }

      public PotatoCannonProjectileType.Builder soundPitch(float pitch) {
         this.soundPitch = pitch;
         return this;
      }

      public PotatoCannonProjectileType.Builder sticky() {
         this.sticky = true;
         return this;
      }

      public PotatoCannonProjectileType.Builder dropStack(ItemStack stack) {
         this.dropStack = stack;
         return this;
      }

      public PotatoCannonProjectileType.Builder renderMode(PotatoProjectileRenderMode renderMode) {
         this.renderMode = renderMode;
         return this;
      }

      public PotatoCannonProjectileType.Builder renderBillboard() {
         this.renderMode(AllPotatoProjectileRenderModes.Billboard.INSTANCE);
         return this;
      }

      public PotatoCannonProjectileType.Builder renderTumbling() {
         this.renderMode(AllPotatoProjectileRenderModes.Tumble.INSTANCE);
         return this;
      }

      public PotatoCannonProjectileType.Builder renderTowardMotion(int spriteAngle, float spin) {
         this.renderMode(new AllPotatoProjectileRenderModes.TowardMotion(spriteAngle, spin));
         return this;
      }

      public PotatoCannonProjectileType.Builder preEntityHit(PotatoProjectileEntityHitAction entityHitAction) {
         this.preEntityHit = entityHitAction;
         return this;
      }

      public PotatoCannonProjectileType.Builder onEntityHit(PotatoProjectileEntityHitAction entityHitAction) {
         this.onEntityHit = entityHitAction;
         return this;
      }

      public PotatoCannonProjectileType.Builder onBlockHit(PotatoProjectileBlockHitAction blockHitAction) {
         this.onBlockHit = blockHitAction;
         return this;
      }

      public PotatoCannonProjectileType.Builder addItems(ItemLike... items) {
         for (ItemLike provider : items) {
            this.items.add(provider.asItem().builtInRegistryHolder());
         }

         return this;
      }

      public PotatoCannonProjectileType build() {
         return new PotatoCannonProjectileType(
            HolderSet.direct(this.items),
            this.reloadTicks,
            this.damage,
            this.split,
            this.knockback,
            this.drag,
            this.velocityMultiplier,
            this.gravityMultiplier,
            this.soundPitch,
            this.sticky,
            this.dropStack,
            this.renderMode,
            Optional.ofNullable(this.preEntityHit),
            Optional.ofNullable(this.onEntityHit),
            Optional.ofNullable(this.onBlockHit)
         );
      }
   }
}

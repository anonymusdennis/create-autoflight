package com.simibubi.create.api.equipment.potatoCannon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import java.util.function.Function;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

public interface PotatoProjectileEntityHitAction {
   Codec<PotatoProjectileEntityHitAction> CODEC = CreateBuiltInRegistries.POTATO_PROJECTILE_ENTITY_HIT_ACTION
      .byNameCodec()
      .dispatch(PotatoProjectileEntityHitAction::codec, Function.identity());

   boolean execute(ItemStack var1, EntityHitResult var2, PotatoProjectileEntityHitAction.Type var3);

   MapCodec<? extends PotatoProjectileEntityHitAction> codec();

   public static enum Type {
      PRE_HIT,
      ON_HIT;
   }
}

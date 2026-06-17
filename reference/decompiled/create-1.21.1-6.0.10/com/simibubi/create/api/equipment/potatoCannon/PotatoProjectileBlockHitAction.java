package com.simibubi.create.api.equipment.potatoCannon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import java.util.function.Function;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.BlockHitResult;

public interface PotatoProjectileBlockHitAction {
   Codec<PotatoProjectileBlockHitAction> CODEC = CreateBuiltInRegistries.POTATO_PROJECTILE_BLOCK_HIT_ACTION
      .byNameCodec()
      .dispatch(PotatoProjectileBlockHitAction::codec, Function.identity());

   boolean execute(LevelAccessor var1, ItemStack var2, BlockHitResult var3);

   MapCodec<? extends PotatoProjectileBlockHitAction> codec();
}

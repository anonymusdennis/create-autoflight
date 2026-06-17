package com.simibubi.create.api.contraption.storage.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.api.registry.SimpleRegistry;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public abstract class MountedFluidStorageType<T extends MountedFluidStorage> {
   public static final Codec<MountedFluidStorageType<?>> CODEC = CreateBuiltInRegistries.MOUNTED_FLUID_STORAGE_TYPE.byNameCodec();
   public static final SimpleRegistry<Block, MountedFluidStorageType<?>> REGISTRY = SimpleRegistry.create();
   public final MapCodec<? extends T> codec;

   protected MountedFluidStorageType(MapCodec<? extends T> codec) {
      this.codec = codec;
   }

   @Nullable
   public abstract T mount(Level var1, BlockState var2, BlockPos var3, @Nullable BlockEntity var4);

   public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> mountedFluidStorage(
      RegistryEntry<MountedFluidStorageType<?>, ? extends MountedFluidStorageType<?>> type
   ) {
      return builder -> (BlockBuilder)builder.onRegisterAfter(
            CreateRegistries.MOUNTED_FLUID_STORAGE_TYPE, block -> REGISTRY.register(block, (MountedFluidStorageType<?>)type.get())
         );
   }
}

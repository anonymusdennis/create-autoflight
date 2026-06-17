package com.simibubi.create.api.contraption.storage.item.simple;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

public abstract class SimpleMountedStorageType<T extends SimpleMountedStorage> extends MountedItemStorageType<SimpleMountedStorage> {
   protected SimpleMountedStorageType(MapCodec<T> codec) {
      super(codec);
   }

   @Nullable
   public SimpleMountedStorage mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
      return Optional.ofNullable(be).map(b -> this.getHandler(level, b)).map(this::createStorage).orElse(null);
   }

   protected IItemHandler getHandler(Level level, BlockEntity be) {
      IItemHandler handler = (IItemHandler)level.getCapability(ItemHandler.BLOCK, be.getBlockPos(), null);
      return handler instanceof IItemHandlerModifiable modifiable ? modifiable : null;
   }

   protected SimpleMountedStorage createStorage(IItemHandler handler) {
      return new SimpleMountedStorage(this, handler);
   }

   public static final class Impl extends SimpleMountedStorageType<SimpleMountedStorage> {
      public Impl() {
         super(SimpleMountedStorage.CODEC);
      }
   }
}

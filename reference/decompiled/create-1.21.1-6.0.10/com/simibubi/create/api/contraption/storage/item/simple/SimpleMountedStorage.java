package com.simibubi.create.api.contraption.storage.item.simple;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllMountedStorageTypes;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.contraption.storage.item.WrapperMountedItemStorage;
import com.simibubi.create.foundation.codec.CreateCodecs;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class SimpleMountedStorage extends WrapperMountedItemStorage<ItemStackHandler> {
   public static final MapCodec<SimpleMountedStorage> CODEC = codec(SimpleMountedStorage::new);

   public SimpleMountedStorage(MountedItemStorageType<?> type, IItemHandler handler) {
      super(type, copyToItemStackHandler(handler));
   }

   public SimpleMountedStorage(IItemHandler handler) {
      this((MountedItemStorageType<?>)AllMountedStorageTypes.SIMPLE.get(), handler);
   }

   @Override
   public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
      if (be != null) {
         IItemHandler cap = (IItemHandler)level.getCapability(ItemHandler.BLOCK, pos, null);
         if (cap != null) {
            this.validate(cap).ifPresent(handler -> {
               for (int i = 0; i < handler.getSlots(); i++) {
                  handler.setStackInSlot(i, this.getStackInSlot(i));
               }
            });
         }
      }
   }

   protected Optional<IItemHandlerModifiable> validate(IItemHandler handler) {
      return handler.getSlots() == this.getSlots() && handler instanceof IItemHandlerModifiable modifiable ? Optional.of(modifiable) : Optional.empty();
   }

   public static <T extends SimpleMountedStorage> MapCodec<T> codec(Function<IItemHandler, T> factory) {
      return CreateCodecs.ITEM_STACK_HANDLER.xmap(factory, storage -> storage.wrapped).fieldOf("value");
   }
}

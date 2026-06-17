package com.simibubi.create.impl.contraption.storage;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllMountedStorageTypes;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.contraption.storage.item.simple.SimpleMountedStorage;
import java.util.Optional;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

public class FallbackMountedStorage extends SimpleMountedStorage {
   public static final MapCodec<FallbackMountedStorage> CODEC = SimpleMountedStorage.codec(FallbackMountedStorage::new);

   public FallbackMountedStorage(IItemHandler handler) {
      super((MountedItemStorageType<?>)AllMountedStorageTypes.FALLBACK.get(), handler);
   }

   @Override
   protected Optional<IItemHandlerModifiable> validate(IItemHandler handler) {
      return super.validate(handler).filter(FallbackMountedStorage::isValid);
   }

   public static boolean isValid(IItemHandler handler) {
      return handler.getClass() == ItemStackHandler.class;
   }
}

package com.simibubi.create.content.contraptions.behaviour.dispenser.storage;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllMountedStorageTypes;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.contraption.storage.item.menu.MountedStorageMenus;
import com.simibubi.create.api.contraption.storage.item.simple.SimpleMountedStorage;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

public class DispenserMountedStorage extends SimpleMountedStorage {
   public static final MapCodec<DispenserMountedStorage> CODEC = SimpleMountedStorage.codec(DispenserMountedStorage::new);

   protected DispenserMountedStorage(MountedItemStorageType<?> type, IItemHandler handler) {
      super(type, handler);
   }

   public DispenserMountedStorage(IItemHandler handler) {
      this((MountedItemStorageType<?>)AllMountedStorageTypes.DISPENSER.get(), handler);
   }

   @Nullable
   @Override
   protected MenuProvider createMenuProvider(Component name, IItemHandlerModifiable handler, Predicate<Player> stillValid, Consumer<Player> onClose) {
      return MountedStorageMenus.createGeneric9x9(name, handler, stillValid, onClose);
   }

   @Override
   protected void playOpeningSound(ServerLevel level, Vec3 pos) {
   }
}

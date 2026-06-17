package com.simibubi.create.foundation.utility;

import java.util.function.Supplier;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

public final class GlobalRegistryAccess {
   private static Supplier<RegistryAccess> supplier;

   @Nullable
   public static RegistryAccess get() {
      return supplier.get();
   }

   public static RegistryAccess getOrThrow() {
      RegistryAccess registryAccess = get();
      if (registryAccess == null) {
         throw new IllegalStateException("Could not get RegistryAccess");
      } else {
         return registryAccess;
      }
   }

   static {
      CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> supplier = () -> {
               ClientPacketListener packetListener = Minecraft.getInstance().getConnection();
               return packetListener == null ? null : packetListener.registryAccess();
            });
      if (supplier == null) {
         supplier = () -> {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            return server == null ? null : server.registryAccess();
         };
      }
   }
}

package dev.simulated_team.simulated.service;

import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.LinkedTypewriterMenuCommon;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public interface SimMenuService {
   SimMenuService INSTANCE = ServiceUtil.load(SimMenuService.class);

   <T extends LinkedTypewriterMenuCommon> T getLoaderLinkedTypewriter(MenuType<?> var1, int var2, Inventory var3, RegistryFriendlyByteBuf var4);

   <T extends LinkedTypewriterMenuCommon> T getLoaderLinkedTypewriter(MenuType<?> var1, int var2, Inventory var3, LinkedTypewriterBlockEntity var4);

   void openScreen(ServerPlayer var1, MenuProvider var2, Consumer<RegistryFriendlyByteBuf> var3);
}

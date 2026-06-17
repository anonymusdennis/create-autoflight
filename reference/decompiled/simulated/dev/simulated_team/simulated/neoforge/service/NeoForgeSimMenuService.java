package dev.simulated_team.simulated.neoforge.service;

import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.LinkedTypewriterMenuCommon;
import dev.simulated_team.simulated.content.linked_typewriter.LinkedTypewriterMenuImpl;
import dev.simulated_team.simulated.service.SimMenuService;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public class NeoForgeSimMenuService implements SimMenuService {
   @Override
   public <T extends LinkedTypewriterMenuCommon> T getLoaderLinkedTypewriter(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
      return (T)(new LinkedTypewriterMenuImpl(type, id, inv, extraData));
   }

   @Override
   public <T extends LinkedTypewriterMenuCommon> T getLoaderLinkedTypewriter(MenuType<?> type, int id, Inventory inv, LinkedTypewriterBlockEntity be) {
      return (T)(new LinkedTypewriterMenuImpl(type, id, inv, be));
   }

   @Override
   public void openScreen(ServerPlayer player, MenuProvider factory, Consumer<RegistryFriendlyByteBuf> extraDataWriter) {
      player.openMenu(factory, extraDataWriter);
   }
}

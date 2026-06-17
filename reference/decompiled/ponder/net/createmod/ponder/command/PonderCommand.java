package net.createmod.ponder.command;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.Collection;
import net.createmod.catnip.net.packets.ClientboundSimpleActionPacket;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.server.level.ServerPlayer;

public class PonderCommand {
   static ArgumentBuilder<CommandSourceStack, ?> register() {
      return ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                           "ponder"
                        )
                        .requires(cs -> cs.hasPermission(0)))
                     .executes(ctx -> openScene("ponder:tags", ((CommandSourceStack)ctx.getSource()).getPlayerOrException())))
                  .then(Commands.literal("reload").executes(ctx -> reloadPonderIndex(((CommandSourceStack)ctx.getSource()).getPlayerOrException()))))
               .then(Commands.literal("index").executes(ctx -> openScene("ponder:index", ((CommandSourceStack)ctx.getSource()).getPlayerOrException()))))
            .then(Commands.literal("tags").executes(ctx -> openScene("ponder:tags", ((CommandSourceStack)ctx.getSource()).getPlayerOrException()))))
         .then(
            ((RequiredArgumentBuilder)Commands.argument("scene", ResourceLocationArgument.id())
                  .executes(
                     ctx -> openScene(ResourceLocationArgument.getId(ctx, "scene").toString(), ((CommandSourceStack)ctx.getSource()).getPlayerOrException())
                  ))
               .then(
                  ((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.players()).requires(cs -> cs.hasPermission(2)))
                     .executes(ctx -> openScene(ResourceLocationArgument.getId(ctx, "scene").toString(), EntityArgument.getPlayers(ctx, "targets")))
               )
         );
   }

   private static int openScene(String sceneId, ServerPlayer player) {
      return openScene(sceneId, ImmutableList.of(player));
   }

   private static int openScene(String sceneId, Collection<? extends ServerPlayer> players) {
      for (ServerPlayer player : players) {
         if (!CatnipServices.HOOKS.isPlayerFake(player)) {
            CatnipServices.NETWORK.sendToClient(player, new ClientboundSimpleActionPacket("openPonder", sceneId));
         }
      }

      return 1;
   }

   private static int reloadPonderIndex(ServerPlayer player) {
      CatnipServices.NETWORK.simpleActionToClient(player, "reloadPonder", "");
      return 1;
   }
}

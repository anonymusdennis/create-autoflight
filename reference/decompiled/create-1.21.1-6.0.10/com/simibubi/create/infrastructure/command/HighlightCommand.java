package com.simibubi.create.infrastructure.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions;
import java.util.Collection;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class HighlightCommand {
   public static ArgumentBuilder<CommandSourceStack, ?> register() {
      return ((LiteralArgumentBuilder)Commands.literal("highlight")
            .then(
               ((RequiredArgumentBuilder)Commands.argument("pos", BlockPosArgument.blockPos())
                     .then(Commands.argument("players", EntityArgument.players()).executes(ctx -> {
                        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
                        BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                        CatnipServices.NETWORK.sendToClients(players, new HighlightPacket(pos));
                        return players.size();
                     })))
                  .executes(ctx -> {
                     BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                     CatnipServices.NETWORK.sendToClient((ServerPlayer)((CommandSourceStack)ctx.getSource()).getEntity(), new HighlightPacket(pos));
                     return 1;
                  })
            ))
         .executes(ctx -> {
            ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
            return highlightAssemblyExceptionFor(player, (CommandSourceStack)ctx.getSource());
         });
   }

   private static void sendMissMessage(CommandSourceStack source) {
      source.sendSuccess(() -> Component.literal("Try looking at a Block that has failed to assemble a Contraption and try again."), true);
   }

   private static int highlightAssemblyExceptionFor(ServerPlayer player, CommandSourceStack source) {
      double distance = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
      Vec3 start = player.getEyePosition(1.0F);
      Vec3 look = player.getViewVector(1.0F);
      Vec3 end = start.add(look.x * distance, look.y * distance, look.z * distance);
      Level world = player.level();
      BlockHitResult ray = world.clip(new ClipContext(start, end, Block.OUTLINE, Fluid.NONE, player));
      if (ray.getType() == Type.MISS) {
         sendMissMessage(source);
         return 0;
      } else {
         BlockPos pos = ray.getBlockPos();
         if (world.getBlockEntity(pos) instanceof IDisplayAssemblyExceptions display) {
            AssemblyException exception = display.getLastAssemblyException();
            if (exception == null) {
               sendMissMessage(source);
               return 0;
            } else if (!exception.hasPosition()) {
               source.sendSuccess(() -> Component.literal("Can't highlight a specific position for this issue"), true);
               return 1;
            } else {
               BlockPos p = exception.getPosition();
               String command = "/create highlight " + p.getX() + " " + p.getY() + " " + p.getZ();
               player.server.getCommands().performPrefixedCommand(source, command);
               return 1;
            }
         } else {
            sendMissMessage(source);
            return 0;
         }
      }
   }
}

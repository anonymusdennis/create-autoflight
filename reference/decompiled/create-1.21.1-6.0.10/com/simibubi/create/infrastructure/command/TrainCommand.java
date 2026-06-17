package com.simibubi.create.infrastructure.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

public class TrainCommand {
   static ArgumentBuilder<CommandSourceStack, ?> register() {
      return ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("train").requires(cs -> cs.hasPermission(2)))
            .then(
               Commands.literal("remove")
                  .then(
                     Commands.argument("train", UuidArgument.uuid())
                        .executes(ctx -> runDelete((CommandSourceStack)ctx.getSource(), UuidArgument.getUuid(ctx, "train")))
                  )
            ))
         .then(
            Commands.literal("tp")
               .then(
                  ((RequiredArgumentBuilder)Commands.argument("train", UuidArgument.uuid()).requires(CommandSourceStack::isPlayer))
                     .executes(ctx -> runTeleport((CommandSourceStack)ctx.getSource(), UuidArgument.getUuid(ctx, "train")))
               )
         );
   }

   private static int runDelete(CommandSourceStack source, UUID argument) {
      Train train = Create.RAILWAYS.trains.get(argument);
      if (train == null) {
         source.sendFailure(Component.literal("No Train with id " + argument.toString().substring(0, 5) + "[...] was found"));
         return 0;
      } else {
         train.invalid = true;
         source.sendSuccess(() -> Component.literal("Train '").append(train.name).append("' removed successfully"), true);
         return 1;
      }
   }

   private static int runTeleport(CommandSourceStack source, UUID argument) throws CommandSyntaxException {
      ServerPlayer serverPlayer = source.getPlayerOrException();
      GameType gameMode = serverPlayer.gameMode.getGameModeForPlayer();
      if (gameMode != GameType.CREATIVE && gameMode != GameType.SPECTATOR) {
         source.sendFailure(Component.literal("Can only teleport to train when in Creative or Spectator Mode!"));
         return 0;
      } else {
         Train train = Create.RAILWAYS.trains.get(argument);
         if (train == null) {
            source.sendFailure(Component.literal("No Train with id " + argument.toString().substring(0, 5) + "[...] was found"));
            return 0;
         } else {
            List<ResourceKey<Level>> presentDimensions = train.getPresentDimensions();
            if (presentDimensions.isEmpty()) {
               source.sendFailure(Component.literal("Unable to teleport to Train. No valid location found"));
               return 0;
            } else {
               ResourceKey<Level> levelKey = presentDimensions.get(0);
               ServerLevel serverLevel = serverPlayer.getServer().getLevel(levelKey);
               Optional<BlockPos> positionInDimension = train.getPositionInDimension(levelKey);
               if (!positionInDimension.isEmpty() && serverLevel != null) {
                  BlockPos pos = positionInDimension.get();
                  serverPlayer.teleportTo(
                     serverLevel,
                     (double)pos.getX(),
                     (double)(pos.getY() + 5),
                     (double)pos.getZ(),
                     serverPlayer.getViewYRot(0.0F),
                     serverPlayer.getViewXRot(0.0F)
                  );
                  source.sendSuccess(() -> Component.literal("Teleported to Train '").append(train.name).append("' successfully"), true);
                  return 1;
               } else {
                  source.sendFailure(Component.literal("Unable to teleport to Train. No valid location found"));
                  return 0;
               }
            }
         }
      }
   }
}

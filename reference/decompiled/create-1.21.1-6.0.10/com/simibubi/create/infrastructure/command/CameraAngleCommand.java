package com.simibubi.create.infrastructure.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.simibubi.create.foundation.utility.CameraAngleAnimationService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.neoforged.neoforge.server.command.EnumArgument;

public class CameraAngleCommand {
   public static ArgumentBuilder<CommandSourceStack, ?> register() {
      return ((LiteralArgumentBuilder)Commands.literal("angle").requires(cs -> cs.hasPermission(2)))
         .then(
            ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("players", EntityArgument.players())
                     .then(Commands.literal("yaw").then(Commands.argument("degrees", FloatArgumentType.floatArg()).executes(ctx -> {
                        float angleTarget = FloatArgumentType.getFloat(ctx, "degrees");
                        CameraAngleAnimationService.setYawTarget(angleTarget);
                        return 1;
                     }))))
                  .then(Commands.literal("pitch").then(Commands.argument("degrees", FloatArgumentType.floatArg()).executes(ctx -> {
                     float angleTarget = FloatArgumentType.getFloat(ctx, "degrees");
                     CameraAngleAnimationService.setPitchTarget(angleTarget);
                     return 1;
                  }))))
               .then(
                  Commands.literal("mode")
                     .then(
                        ((RequiredArgumentBuilder)Commands.argument("mode", EnumArgument.enumArgument(CameraAngleAnimationService.Mode.class))
                              .executes(ctx -> {
                                 CameraAngleAnimationService.Mode mode = (CameraAngleAnimationService.Mode)ctx.getArgument(
                                    "mode", CameraAngleAnimationService.Mode.class
                                 );
                                 CameraAngleAnimationService.setAnimationMode(mode);
                                 return 1;
                              }))
                           .then(Commands.argument("speed", FloatArgumentType.floatArg(0.0F)).executes(ctx -> {
                              CameraAngleAnimationService.Mode mode = (CameraAngleAnimationService.Mode)ctx.getArgument(
                                 "mode", CameraAngleAnimationService.Mode.class
                              );
                              float speed = FloatArgumentType.getFloat(ctx, "speed");
                              CameraAngleAnimationService.setAnimationMode(mode);
                              CameraAngleAnimationService.setAnimationSpeed(speed);
                              return 1;
                           }))
                     )
               )
         );
   }
}

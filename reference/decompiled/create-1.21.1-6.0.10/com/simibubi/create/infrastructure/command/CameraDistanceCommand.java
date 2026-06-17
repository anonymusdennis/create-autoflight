package com.simibubi.create.infrastructure.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.simibubi.create.content.trains.CameraDistanceModifier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class CameraDistanceCommand {
   public static ArgumentBuilder<CommandSourceStack, ?> register() {
      return ((LiteralArgumentBuilder)Commands.literal("camera").then(Commands.literal("reset").executes(ctx -> {
         CameraDistanceModifier.zoomOut(1.0F);
         return 1;
      }))).then(Commands.argument("multiplier", FloatArgumentType.floatArg(1.0F)).executes(ctx -> {
         float multiplier = FloatArgumentType.getFloat(ctx, "multiplier");
         CameraDistanceModifier.zoomOut(multiplier);
         return 1;
      }));
   }
}

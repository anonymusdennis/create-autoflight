package com.simibubi.create.infrastructure.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.simibubi.create.content.kinetics.KineticDebugger;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class ToggleDebugCommand {
   public static ArgumentBuilder<CommandSourceStack, ?> register() {
      return ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("rainbowDebug").requires(cs -> cs.hasPermission(0)))
            .then(Commands.argument("status", BoolArgumentType.bool()).executes(ctx -> {
               KineticDebugger.rainbowDebug = BoolArgumentType.getBool(ctx, "status");
               Component text = boolToText(KineticDebugger.rainbowDebug).append(Component.literal(" Rainbow Debug Utility").withStyle(ChatFormatting.WHITE));
               ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> text, false);
               return 1;
            })))
         .executes(ctx -> {
            Component text = Component.literal("Rainbow Debug Utility is currently: ").append(boolToText(KineticDebugger.rainbowDebug));
            ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> text, false);
            return 1;
         });
   }

   private static MutableComponent boolToText(boolean b) {
      return b ? Component.literal("enabled").withStyle(ChatFormatting.GREEN) : Component.literal("disabled").withStyle(ChatFormatting.RED);
   }
}

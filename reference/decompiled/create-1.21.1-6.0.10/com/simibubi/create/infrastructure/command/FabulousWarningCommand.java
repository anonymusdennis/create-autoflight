package com.simibubi.create.infrastructure.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class FabulousWarningCommand {
   public static ArgumentBuilder<CommandSourceStack, ?> register() {
      return Commands.literal("dismissFabulousWarning").executes(ctx -> {
         AllConfigs.client().ignoreFabulousWarning.set(true);
         ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Disabled Fabulous graphics warning"), false);
         return 1;
      });
   }
}

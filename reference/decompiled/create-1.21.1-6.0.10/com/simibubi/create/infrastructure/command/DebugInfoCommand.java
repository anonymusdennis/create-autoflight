package com.simibubi.create.infrastructure.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.simibubi.create.Create;
import com.simibubi.create.infrastructure.debugInfo.ServerDebugInfoPacket;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class DebugInfoCommand {
   public static ArgumentBuilder<CommandSourceStack, ?> register() {
      return Commands.literal("debuginfo").executes(ctx -> {
         CommandSourceStack source = (CommandSourceStack)ctx.getSource();
         ServerPlayer player = source.getPlayerOrException();
         Create.lang().translate("command.debuginfo.sending", new Object[0]).sendChat(player);
         CatnipServices.NETWORK.sendToClient(player, new ServerDebugInfoPacket(player));
         return 1;
      });
   }
}

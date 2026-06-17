package net.createmod.catnip.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;

public class FlySpeedCommand {
   public static ArgumentBuilder<CommandSourceStack, ?> register() {
      return ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("flySpeed").requires(cs -> cs.hasPermission(2)))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("speed", FloatArgumentType.floatArg(0.0F))
                     .then(
                        Commands.argument("target", EntityArgument.player())
                           .executes(ctx -> sendFlySpeedUpdate(ctx, EntityArgument.getPlayer(ctx, "target"), FloatArgumentType.getFloat(ctx, "speed")))
                     ))
                  .executes(
                     ctx -> sendFlySpeedUpdate(ctx, ((CommandSourceStack)ctx.getSource()).getPlayerOrException(), FloatArgumentType.getFloat(ctx, "speed"))
                  )
            ))
         .then(
            ((LiteralArgumentBuilder)Commands.literal("reset")
                  .then(
                     Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> sendFlySpeedUpdate(ctx, EntityArgument.getPlayer(ctx, "target"), 0.05F))
                  ))
               .executes(ctx -> sendFlySpeedUpdate(ctx, ((CommandSourceStack)ctx.getSource()).getPlayerOrException(), 0.05F))
         );
   }

   private static int sendFlySpeedUpdate(CommandContext<CommandSourceStack> ctx, ServerPlayer player, float speed) {
      Abilities abilities = player.getAbilities();
      abilities.setFlyingSpeed(speed);
      player.connection.send(new ClientboundPlayerAbilitiesPacket(abilities));
      ((CommandSourceStack)ctx.getSource())
         .sendSuccess(
            () -> Component.literal("[Catnip]: ")
                  .withStyle(ChatFormatting.YELLOW)
                  .append(
                     Component.translatable("catnip.util.fly_speed_set.message", new Object[]{player.getName().copy(), speed}).withStyle(ChatFormatting.WHITE)
                  ),
            true
         );
      return 1;
   }
}

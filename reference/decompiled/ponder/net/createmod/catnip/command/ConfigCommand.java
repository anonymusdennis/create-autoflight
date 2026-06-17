package net.createmod.catnip.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.createmod.catnip.config.ui.ConfigHelper;
import net.createmod.catnip.net.ConfigPathArgument;
import net.createmod.catnip.net.packets.ClientboundConfigPacket;
import net.createmod.catnip.net.packets.ClientboundSimpleActionPacket;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.ponder.Ponder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.config.ModConfig.Type;

public class ConfigCommand {
   public static ArgumentBuilder<CommandSourceStack, ?> register() {
      return ((LiteralArgumentBuilder)Commands.literal("config").executes(ctx -> {
            ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
            CatnipServices.NETWORK.sendToClient(player, new ClientboundSimpleActionPacket("configScreen", ""));
            return 1;
         }))
         .then(
            ((RequiredArgumentBuilder)Commands.argument("path", ConfigPathArgument.path()).executes(ctx -> {
                  ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
                  CatnipServices.NETWORK
                     .sendToClient(player, new ClientboundSimpleActionPacket("configScreen", ConfigPathArgument.getPath(ctx, "path").toString()));
                  return 1;
               }))
               .then(
                  ((LiteralArgumentBuilder)Commands.literal("set").requires(cs -> cs.hasPermission(2)))
                     .then(
                        Commands.argument("value", StringArgumentType.string())
                           .executes(
                              ctx -> {
                                 ConfigHelper.ConfigPath path = ConfigPathArgument.getPath(ctx, "path");
                                 String value = StringArgumentType.getString(ctx, "value");
                                 if (path.getType() == Type.CLIENT) {
                                    ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
                                    CatnipServices.NETWORK.sendToClient(player, new ClientboundConfigPacket(path.toString(), value));
                                    return 1;
                                 } else {
                                    try {
                                       ConfigHelper.setConfigValue(path, value);
                                       ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Great Success!"), false);
                                       return 1;
                                    } catch (ConfigHelper.InvalidValueException var4) {
                                       ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal("Config could not be set the the specified value!"));
                                       return 0;
                                    } catch (Exception var5) {
                                       ((CommandSourceStack)ctx.getSource())
                                          .sendFailure(
                                             Component.literal(
                                                "Something went wrong while trying to set config value. Check the server logs for more information"
                                             )
                                          );
                                       Ponder.LOGGER.warn("Exception during server-side config value set:", var5);
                                       return 0;
                                    }
                                 }
                              }
                           )
                     )
               )
         );
   }
}

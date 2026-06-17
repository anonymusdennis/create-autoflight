package dev.ryanhcode.sable.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ryanhcode.sable.api.command.SableCommandHelper;
import dev.ryanhcode.sable.api.command.SubLevelArgumentType;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundEnterGizmoPacket;
import dev.ryanhcode.sable.network.packets.udp.SableUDPEchoPacket;
import dev.ryanhcode.sable.network.udp.SableUDPServer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import foundry.veil.api.network.VeilPacketManager;
import java.util.Collection;
import java.util.Formatter;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class SableCommand {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
      LiteralArgumentBuilder<CommandSourceStack> sableBuilder = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("sable")
         .requires(commandSourceStack -> commandSourceStack.hasPermission(2));
      SablePhysicsCommands.register(sableBuilder, buildContext);
      SableSpawnCommands.register(sableBuilder, buildContext);
      SableSubLevelCommands.register(sableBuilder, buildContext);
      SableAssembleCommands.register(sableBuilder, buildContext);
      SableStorageCommands.register(sableBuilder, buildContext);
      LiteralArgumentBuilder<CommandSourceStack> debugBuilder = Commands.literal("debug");
      SableJointCommands.register(debugBuilder, buildContext);
      SableConfigCommands.register(debugBuilder, buildContext);
      sableBuilder.then(debugBuilder.then(Commands.literal("udp_test").executes(ctx -> {
         SableUDPServer server = SableUDPServer.getServer(((CommandSourceStack)ctx.getSource()).getServer());
         if (server != null) {
            server.sendUDPPacket(((CommandSourceStack)ctx.getSource()).getPlayerOrException(), new SableUDPEchoPacket("Skibidi Toilet"), true);
         }

         return 1;
      })));
      ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)sableBuilder.then(
                  Commands.literal("engage_gizmo").executes(SableCommand::executeEnableGizmoCommand)
               ))
               .then(
                  ((LiteralArgumentBuilder)Commands.literal("paused").executes(SableCommand::executeTogglePhysicsPausedCommand))
                     .then(Commands.argument("paused", BoolArgumentType.bool()).executes(SableCommand::executeSetPhysicsPausedCommand))
               ))
            .then(
               ((LiteralArgumentBuilder)Commands.literal("forceload")
                     .then(Commands.literal("add").then(Commands.argument("sub_level", SubLevelArgumentType.subLevels()).executes(ctx -> {
                        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
                        ServerSubLevelContainer container = SableCommandHelper.requireSubLevelContainer(source);
                        Collection<ServerSubLevel> subLevels = SubLevelArgumentType.getSubLevels(ctx, "sub_level");
                        int count = 0;

                        for (ServerSubLevel subLevel : subLevels) {
                           if (container.addForceLoadTicket(subLevel, SubLevelLoadingTicketType.COMMAND_FORCED, Unit.INSTANCE)) {
                              count++;
                           }
                        }

                        int finalCount = count;
                        source.sendSuccess(() -> Component.translatable("commands.sable.forceload.add.count", new Object[]{finalCount}), true);
                        return count;
                     }))))
                  .then(Commands.literal("remove").then(Commands.argument("sub_level", SubLevelArgumentType.subLevels()).executes(ctx -> {
                     CommandSourceStack source = (CommandSourceStack)ctx.getSource();
                     ServerSubLevelContainer container = SableCommandHelper.requireSubLevelContainer(source);
                     Collection<ServerSubLevel> subLevels = SubLevelArgumentType.getSubLevels(ctx, "sub_level");
                     int count = 0;

                     for (ServerSubLevel subLevel : subLevels) {
                        if (container.removeForceLoadTicket(subLevel, SubLevelLoadingTicketType.COMMAND_FORCED, Unit.INSTANCE)) {
                           count++;
                        }
                     }

                     int finalCount = count;
                     source.sendSuccess(() -> Component.translatable("commands.sable.forceload.remove.count", new Object[]{finalCount}), true);
                     return count;
                  })))
            ))
         .then(
            Commands.literal("info")
               .then(
                  Commands.argument("sub_level", SubLevelArgumentType.subLevels())
                     .executes(
                        ctx -> {
                           CommandSourceStack source = (CommandSourceStack)ctx.getSource();
                           ServerSubLevelContainer container = SableCommandHelper.requireSubLevelContainer(source);
                           Collection<ServerSubLevel> subLevels = SubLevelArgumentType.getSubLevels(ctx, "sub_level");
                           if (subLevels.isEmpty()) {
                              throw SableCommandHelper.ERROR_NO_SUB_LEVELS_FOUND.create();
                           } else {
                              source.sendSuccess(() -> Component.translatable("commands.sable.info.count", new Object[]{subLevels.size()}), false);

                              for (ServerSubLevel subLevel : subLevels) {
                                 Pose3dc pose = subLevel.logicalPose();
                                 source.sendSuccess(
                                    () -> {
                                       Vector3dc pos = pose.position();
                                       MutableComponent component = Component.translatable(
                                          "commands.sable.info.name",
                                          new Object[]{Component.literal(subLevel.getName() != null ? subLevel.getName() : subLevel.getUniqueId().toString())}
                                       );
                                       ResourceLocation dimension = subLevel.getLevel().dimension().location();
                                       GlobalSavedSubLevelPointer pointer = subLevel.getLastSerializationPointer();
                                       Component fileId = Component.translatable(
                                          "commands.sable.info.name.tooltip", new Object[]{pointer != null ? pointer.toString() : "None yet"}
                                       );
                                       component.setStyle(
                                          Style.EMPTY
                                             .withClickEvent(
                                                new ClickEvent(
                                                   Action.SUGGEST_COMMAND,
                                                   new Formatter()
                                                      .format(Locale.ROOT, "/execute in %s run tp @s %.2f %.2f %.2f", dimension, pos.x(), pos.y(), pos.z())
                                                      .toString()
                                                )
                                             )
                                             .withHoverEvent(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, fileId))
                                             .withColor(ChatFormatting.GRAY)
                                       );
                                       return component;
                                    },
                                    false
                                 );
                                 source.sendSuccess(() -> {
                                    Vector3dc pos = pose.position();
                                    return Component.translatable("commands.sable.info.position", new Object[]{pos.x(), pos.y(), pos.z()});
                                 }, false);
                                 source.sendSuccess(
                                    () -> {
                                       Quaterniondc orientation = pose.orientation();
                                       return Component.translatable(
                                          "commands.sable.info.orientation", new Object[]{orientation.x(), orientation.y(), orientation.z(), orientation.w()}
                                       );
                                    },
                                    false
                                 );
                                 source.sendSuccess(
                                    () -> Component.translatable("commands.sable.info.mass", new Object[]{subLevel.getMassTracker().getMass()}), false
                                 );
                                 SubLevelPhysicsSystem physicsSystem = container.physicsSystem();
                                 RigidBodyHandle handle = physicsSystem.getPhysicsHandle(subLevel);
                                 source.sendSuccess(() -> {
                                    Vector3dc pos = handle.getLinearVelocity(new Vector3d());
                                    return Component.translatable("commands.sable.info.linear_velocity", new Object[]{pos.x(), pos.y(), pos.z()});
                                 }, false);
                                 source.sendSuccess(() -> {
                                    Vector3dc pos = handle.getAngularVelocity(new Vector3d());
                                    return Component.translatable("commands.sable.info.angular_velocity", new Object[]{pos.x(), pos.y(), pos.z()});
                                 }, false);
                              }

                              return subLevels.size();
                           }
                        }
                     )
               )
         );
      dispatcher.register(sableBuilder);
   }

   private static int executeEnableGizmoCommand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      ServerPlayer player = source.getPlayerOrException();
      SableCommandHelper.requireSubLevelPhysicsSystem(ctx).setPaused(true);
      VeilPacketManager.player(player).sendPacket(new CustomPacketPayload[]{new ClientboundEnterGizmoPacket()});
      return 1;
   }

   private static int executeTogglePhysicsPausedCommand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      boolean pause = !SableCommandHelper.requireSubLevelPhysicsSystem(ctx).getPaused();
      SableCommandHelper.requireSubLevelPhysicsSystem(ctx).setPaused(pause);
      ((CommandSourceStack)ctx.getSource())
         .sendSuccess(() -> Component.translatable("commands.sable.physics.paused_toggled.success", new Object[]{Boolean.toString(pause)}), true);
      return 1;
   }

   private static int executeSetPhysicsPausedCommand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      boolean pause = BoolArgumentType.getBool(ctx, "paused");
      SableCommandHelper.requireSubLevelPhysicsSystem(ctx).setPaused(pause);
      ((CommandSourceStack)ctx.getSource())
         .sendSuccess(() -> Component.translatable("commands.sable.physics.paused.success", new Object[]{Boolean.toString(pause)}), true);
      return 1;
   }
}

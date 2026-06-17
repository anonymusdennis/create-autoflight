package dev.simulated_team.simulated.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import dev.ryanhcode.sable.api.command.SubLevelArgumentType;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueEntity;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler;
import java.util.Collection;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class SimCommand {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
      LiteralArgumentBuilder<CommandSourceStack> cmd = Commands.literal("simulated");
      if (CatnipServices.PLATFORM.isDevelopmentEnvironment()) {
         cmd.then(
            ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("debugthing")
                           .requires(command -> command.hasPermission(2)))
                        .then(Commands.literal("start").then(Commands.argument("steps", IntegerArgumentType.integer()).executes(SimDebugThingCommands::start))))
                     .then(Commands.literal("stop").executes(SimDebugThingCommands::stop)))
                  .then(Commands.literal("abort").executes(SimDebugThingCommands::abort)))
               .then(Commands.literal("stop_sublevels").executes(SimDebugThingCommands::stopSublevels))
         );
      }

      cmd.then(
         ((LiteralArgumentBuilder)Commands.literal("lock").requires(command -> command.hasPermission(2)))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("sub_levels", SubLevelArgumentType.subLevels()).executes(ctx -> lockSubLevels(ctx, true)))
                  .then(Commands.argument("locked", BoolArgumentType.bool()).executes(ctx -> lockSubLevels(ctx, false)))
            )
      );
      cmd.then(
         ((LiteralArgumentBuilder)Commands.literal("glue").requires(command -> command.hasPermission(2)))
            .then(
               Commands.argument("from", BlockPosArgument.blockPos()).then(Commands.argument("to", BlockPosArgument.blockPos()).executes(SimCommand::glueArea))
            )
      );
      dispatcher.register(cmd);
   }

   private static int glueArea(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      BlockPos from = BlockPosArgument.getLoadedBlockPos(ctx, "from");
      BlockPos to = BlockPosArgument.getLoadedBlockPos(ctx, "to");
      ServerLevel world = ((CommandSourceStack)ctx.getSource()).getLevel();
      HoneyGlueEntity entity = new HoneyGlueEntity(world, SuperGlueEntity.span(from, to));
      world.addFreshEntity(entity);
      return 1;
   }

   private static int lockSubLevels(CommandContext<CommandSourceStack> ctx, boolean toggle) throws CommandSyntaxException {
      Collection<ServerSubLevel> subLevels = SubLevelArgumentType.getSubLevels(ctx, "sub_levels");
      int updated = 0;
      PhysicsStaffServerHandler handler = PhysicsStaffServerHandler.get(((CommandSourceStack)ctx.getSource()).getLevel());

      for (ServerSubLevel subLevel : subLevels) {
         if (toggle) {
            handler.toggleLock(subLevel.getUniqueId());
            updated++;
         } else {
            boolean isLocked = handler.isLocked(subLevel);
            boolean shouldLock = BoolArgumentType.getBool(ctx, "locked");
            if (shouldLock != isLocked) {
               handler.toggleLock(subLevel.getUniqueId());
               updated++;
            }
         }
      }

      Component message = Component.translatable("commands.simulated.lock.success", new Object[]{updated, updated == 1 ? "" : "s"});
      ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> message, true);
      return updated;
   }
}

package dev.ryanhcode.sable.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ryanhcode.sable.api.command.SableCommandHelper;
import dev.ryanhcode.sable.api.command.SubLevelArgumentType;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import java.util.Collection;
import java.util.Objects;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector2i;
import org.joml.Vector3d;

public class SableSubLevelCommands {
   public static void register(LiteralArgumentBuilder<CommandSourceStack> sableBuilder, CommandBuildContext buildContext) {
      ((LiteralArgumentBuilder)((LiteralArgumentBuilder)sableBuilder.then(
               ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("name")
                        .then(
                           Commands.literal("set")
                              .then(
                                 Commands.argument("sub_level", SubLevelArgumentType.subLevels())
                                    .then(Commands.argument("name", StringArgumentType.string()).executes(SableSubLevelCommands::executeSetSubLevelNameCommand))
                              )
                        ))
                     .then(
                        Commands.literal("clear")
                           .then(
                              Commands.argument("sub_level", SubLevelArgumentType.subLevels()).executes(SableSubLevelCommands::executeClearSubLevelNameCommand)
                           )
                     ))
                  .then(
                     Commands.literal("get")
                        .then(
                           Commands.argument("sub_level", SubLevelArgumentType.singleSubLevel()).executes(SableSubLevelCommands::executeGetSubLevelNameCommand)
                        )
                  )
            ))
            .then(
               Commands.literal("teleport")
                  .then(
                     Commands.argument("targets", SubLevelArgumentType.subLevels())
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("destination", Vec3Argument.vec3(false))
                                 .executes(ctx -> executeTeleportSubLevelCommand(ctx, null)))
                              .then(
                                 Commands.argument("angle", RotationArgument.rotation())
                                    .executes(ctx -> executeTeleportSubLevelCommand(ctx, RotationArgument.getRotation(ctx, "angle")))
                              )
                        )
                  )
            ))
         .then(
            Commands.literal("remove")
               .then(Commands.argument("targets", SubLevelArgumentType.subLevels()).executes(SableSubLevelCommands::executeRemoveSubLevelCommand))
         );
   }

   private static int setSubLevelNames(Collection<ServerSubLevel> subLevels, @Nullable String name) {
      int modifiedCount = 0;

      for (SubLevel target : subLevels) {
         if (!Objects.equals(target.getName(), name)) {
            target.setName(name);
            modifiedCount++;
         }
      }

      return modifiedCount;
   }

   private static int executeSetSubLevelNameCommand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      Collection<ServerSubLevel> subLevels = SubLevelArgumentType.getSubLevels(ctx, "sub_level");
      String name = StringArgumentType.getString(ctx, "name");
      if (subLevels.isEmpty()) {
         throw SableCommandHelper.ERROR_NO_SUB_LEVELS_FOUND.create();
      } else {
         int modifiedCount = setSubLevelNames(subLevels, name);
         if (modifiedCount == 0) {
            throw SableCommandHelper.ERROR_NO_SUB_LEVELS_MODIFIED.create();
         } else {
            if (modifiedCount == 1) {
               ((CommandSourceStack)ctx.getSource())
                  .sendSuccess(() -> Component.translatable("commands.sable.sub_level.set_name.success_singular", new Object[]{name}), true);
            } else {
               ((CommandSourceStack)ctx.getSource())
                  .sendSuccess(() -> Component.translatable("commands.sable.sub_level.set_name.success_multiple", new Object[]{modifiedCount, name}), true);
            }

            return modifiedCount;
         }
      }
   }

   private static int executeClearSubLevelNameCommand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      Collection<ServerSubLevel> subLevels = SubLevelArgumentType.getSubLevels(ctx, "sub_level");
      if (subLevels.isEmpty()) {
         throw SableCommandHelper.ERROR_NO_SUB_LEVELS_FOUND.create();
      } else {
         int modifiedCount = setSubLevelNames(subLevels, null);
         if (modifiedCount == 0) {
            throw SableCommandHelper.ERROR_NO_SUB_LEVELS_MODIFIED.create();
         } else {
            if (modifiedCount == 1) {
               ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable("commands.sable.sub_level.clear_name.success_singular"), true);
            } else {
               ((CommandSourceStack)ctx.getSource())
                  .sendSuccess(() -> Component.translatable("commands.sable.sub_level.clear_name.success_multiple", new Object[]{modifiedCount}), true);
            }

            return modifiedCount;
         }
      }
   }

   private static int executeGetSubLevelNameCommand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      SubLevel subLevel = SubLevelArgumentType.getSingleSubLevel(ctx, "sub_level");
      if (subLevel.getName() == null) {
         throw SableCommandHelper.ERROR_SUB_LEVEL_UNNAMED.create();
      } else {
         ((CommandSourceStack)ctx.getSource())
            .sendSuccess(() -> Component.translatable("commands.sable.sub_level.get_name.success", new Object[]{subLevel.getName()}), true);
         return 1;
      }
   }

   private static int executeTeleportSubLevelCommand(CommandContext<CommandSourceStack> ctx, @Nullable Coordinates angle) throws CommandSyntaxException {
      PhysicsPipeline pipeline = SableCommandHelper.requireSubLevelPhysicsPipeline(ctx);
      Collection<ServerSubLevel> targets = SubLevelArgumentType.getSubLevels(ctx, "targets");
      if (targets.isEmpty()) {
         throw SableCommandHelper.ERROR_NO_SUB_LEVELS_FOUND.create();
      } else {
         Vector3d destination = JOMLConversion.toJOML(Vec3Argument.getVec3(ctx, "destination"));
         Quaterniond orientation = new Quaterniond();
         Vec2 rotation = angle != null ? angle.getRotation((CommandSourceStack)ctx.getSource()) : null;
         if (angle != null) {
            orientation.rotateY(-Math.toRadians((double)rotation.y));
            orientation.rotateX(Math.toRadians((double)rotation.x));
         }

         for (ServerSubLevel target : targets) {
            pipeline.resetVelocity(target);
            pipeline.teleport(target, destination, angle != null ? orientation : target.logicalPose().orientation());
         }

         if (angle != null) {
            SableCommandHelper.sendSuccessDescribingSubLevels(
               "commands.sable.sub_level.teleport_with_orientation.success", ctx, targets, destination.x, destination.y, destination.z, rotation.x, rotation.y
            );
         } else {
            SableCommandHelper.sendSuccessDescribingSubLevels(
               "commands.sable.sub_level.teleport.success", ctx, targets, destination.x, destination.y, destination.z
            );
         }

         return 1;
      }
   }

   private static int executeRemoveSubLevelCommand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      SubLevelContainer container = SableCommandHelper.requireSubLevelContainer(ctx);
      Collection<ServerSubLevel> targets = SubLevelArgumentType.getSubLevels(ctx, "targets");
      if (targets.isEmpty()) {
         throw SableCommandHelper.ERROR_NO_SUB_LEVELS_FOUND.create();
      } else {
         for (SubLevel target : targets) {
            LevelPlot plot = target.getPlot();
            Vector2i origin = container.getOrigin();
            container.removeSubLevel(plot.plotPos.x - origin.x, plot.plotPos.z - origin.y, SubLevelRemovalReason.REMOVED);
         }

         SableCommandHelper.sendSuccessDescribingSubLevels("commands.sable.sub_level.remove.success", ctx, targets);
         return 1;
      }
   }
}

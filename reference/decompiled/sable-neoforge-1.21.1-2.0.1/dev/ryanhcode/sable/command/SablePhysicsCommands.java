package dev.ryanhcode.sable.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ryanhcode.sable.api.command.SableCommandHelper;
import dev.ryanhcode.sable.api.command.SubLevelArgumentType;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import java.util.Collection;
import java.util.function.Function;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public class SablePhysicsCommands {
   public static void register(LiteralArgumentBuilder<CommandSourceStack> sableBuilder, CommandBuildContext buildContext) {
      sableBuilder.then(
         ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("physics")
                  .then(
                     Commands.literal("impulse")
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("sub_level", SubLevelArgumentType.subLevels())
                                 .then(
                                    Commands.literal("linear")
                                       .then(
                                          ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("impulse", Vec3ArgumentAbsolute.vec3())
                                                   .executes(ctx -> executeLinearImpulseCommand(ctx, true)))
                                                .then(Commands.literal("global").executes(ctx -> executeLinearImpulseCommand(ctx, true))))
                                             .then(Commands.literal("local").executes(ctx -> executeLinearImpulseCommand(ctx, false)))
                                       )
                                 ))
                              .then(
                                 Commands.literal("angular")
                                    .then(
                                       ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("impulse", Vec3ArgumentAbsolute.vec3())
                                                .executes(ctx -> executeAngularImpulseCommand(ctx, true)))
                                             .then(Commands.literal("global").executes(ctx -> executeAngularImpulseCommand(ctx, true))))
                                          .then(Commands.literal("local").executes(ctx -> executeAngularImpulseCommand(ctx, false)))
                                    )
                              )
                        )
                  ))
               .then(
                  Commands.literal("rotation")
                     .then(
                        ((RequiredArgumentBuilder)Commands.argument("sub_level", SubLevelArgumentType.subLevels()).then(wrapRotationWithMode(true)))
                           .then(wrapRotationWithMode(false))
                     )
               ))
            .then(
               Commands.literal("translation")
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("sub_level", SubLevelArgumentType.subLevels())
                           .then(
                              Commands.literal("add")
                                 .then(
                                    ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("translation", Vec3ArgumentAbsolute.vec3())
                                             .executes(ctx -> executeAddTranslationCommand(ctx, true)))
                                          .then(Commands.literal("global").executes(ctx -> executeAddTranslationCommand(ctx, true))))
                                       .then(Commands.literal("local").executes(ctx -> executeAddTranslationCommand(ctx, false)))
                                 )
                           ))
                        .then(
                           Commands.literal("set")
                              .then(Commands.argument("translation", Vec3Argument.vec3(false)).executes(SablePhysicsCommands::executeSetTranslationCommand))
                        )
                  )
            )
      );
   }

   private static Component getGlobalComponent(boolean global) {
      return Component.translatable("commands.sable.physics." + (global ? "global" : "local"));
   }

   private static int executeLinearImpulseCommand(CommandContext<CommandSourceStack> ctx, boolean global) throws CommandSyntaxException {
      SubLevelPhysicsSystem system = SableCommandHelper.requireSubLevelPhysicsSystem(ctx);
      Collection<ServerSubLevel> subLevels = SubLevelArgumentType.getSubLevels(ctx, "sub_level");
      Vec3 impulse = (Vec3)ctx.getArgument("impulse", Vec3.class);
      if (subLevels.isEmpty()) {
         throw SableCommandHelper.ERROR_NO_SUB_LEVELS_FOUND.create();
      } else {
         for (ServerSubLevel subLevel : subLevels) {
            Vec3 subLevelImpulse = impulse;
            if (global) {
               subLevelImpulse = subLevel.logicalPose().transformNormalInverse(impulse);
            }

            system.getPhysicsHandle(subLevel).applyLinearImpulse(JOMLConversion.toJOML(subLevelImpulse));
         }

         SableCommandHelper.sendSuccessDescribingSubLevelsAtIndex(
            "commands.sable.physics.impulse.linear.success", ctx, subLevels, 1, getGlobalComponent(global), impulse.x + ", " + impulse.y + ", " + impulse.z
         );
         return 0;
      }
   }

   private static int executeAngularImpulseCommand(CommandContext<CommandSourceStack> ctx, boolean global) throws CommandSyntaxException {
      SubLevelPhysicsSystem system = SableCommandHelper.requireSubLevelPhysicsSystem(ctx);
      Collection<ServerSubLevel> subLevels = SubLevelArgumentType.getSubLevels(ctx, "sub_level");
      Vec3 impulse = (Vec3)ctx.getArgument("impulse", Vec3.class);
      if (subLevels.isEmpty()) {
         throw SableCommandHelper.ERROR_NO_SUB_LEVELS_FOUND.create();
      } else {
         for (ServerSubLevel subLevel : subLevels) {
            Vec3 subLevelImpulse = impulse;
            if (global) {
               subLevelImpulse = subLevel.logicalPose().transformNormalInverse(impulse);
            }

            system.getPhysicsHandle(subLevel).applyAngularImpulse(JOMLConversion.toJOML(subLevelImpulse));
         }

         SableCommandHelper.sendSuccessDescribingSubLevelsAtIndex(
            "commands.sable.physics.impulse.angular.success", ctx, subLevels, 1, getGlobalComponent(global), impulse.x + ", " + impulse.y + ", " + impulse.z
         );
         return 0;
      }
   }

   private static ArgumentBuilder<CommandSourceStack, ?> wrapRotationWithMode(boolean add) {
      return ((LiteralArgumentBuilder)Commands.literal(add ? "add" : "set").then(wrapRotationWithReferenceFrame(add, false)))
         .then(wrapRotationWithReferenceFrame(add, true));
   }

   private static ArgumentBuilder<CommandSourceStack, ?> wrapRotationWithReferenceFrame(boolean add, boolean axis) {
      Command<CommandSourceStack> c = ctx -> executeRotationCommand(ctx, add, axis, true);
      Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> f = bx -> {
         if (add) {
            bx.then(wrapRotationWithGlobality(axis, true)).then(wrapRotationWithGlobality(axis, false));
         }

         return bx;
      };
      ArgumentBuilder<CommandSourceStack, ?> b = axis
         ? Commands.argument("axis", Vec3ArgumentAbsolute.vec3()).then(f.apply(Commands.argument("angle", DoubleArgumentType.doubleArg()).executes(c)))
         : f.apply(Commands.argument("rotation", RotationArgument.rotation()).executes(c));
      return Commands.literal(axis ? "axis" : "entity").then(b);
   }

   private static ArgumentBuilder<CommandSourceStack, ?> wrapRotationWithGlobality(boolean axis, boolean global) {
      return Commands.literal(global ? "global" : "local").executes(ctx -> executeRotationCommand(ctx, true, axis, global));
   }

   private static int executeRotationCommand(CommandContext<CommandSourceStack> ctx, boolean add, boolean axis, boolean global) throws CommandSyntaxException {
      PhysicsPipeline pipeline = SableCommandHelper.requireSubLevelPhysicsPipeline(ctx);
      Quaterniond orientation = new Quaterniond();
      Vec2 rotation2 = new Vec2(0.0F, 0.0F);
      Vec3 rotationAxis = new Vec3(0.0, 0.0, 0.0);
      double rotationAngle = 0.0;
      if (axis) {
         rotationAxis = (Vec3)ctx.getArgument("axis", Vec3.class);
         rotationAngle = (Double)ctx.getArgument("angle", Double.class);
         orientation.fromAxisAngleDeg(rotationAxis.x, rotationAxis.y, rotationAxis.z, rotationAngle);
         if (rotationAxis.lengthSqr() == 0.0) {
            throw SableCommandHelper.ERROR_NO_AXIS_FOR_ROTATION.create();
         }
      } else {
         rotation2 = RotationArgument.getRotation(ctx, "rotation").getRotation((CommandSourceStack)ctx.getSource());
         orientation.rotateY(-Math.toRadians((double)rotation2.y));
         orientation.rotateX(Math.toRadians((double)rotation2.x));
      }

      Collection<ServerSubLevel> subLevels = SubLevelArgumentType.getSubLevels(ctx, "sub_level");
      if (subLevels.isEmpty()) {
         throw SableCommandHelper.ERROR_NO_SUB_LEVELS_FOUND.create();
      } else {
         for (ServerSubLevel subLevel : subLevels) {
            Pose3d pose = subLevel.logicalPose();
            if (add) {
               if (global) {
                  pose.orientation().premul(orientation);
               } else {
                  pose.orientation().mul(orientation);
               }
            } else {
               pose.orientation().set(orientation);
            }

            pipeline.teleport(subLevel, pose.position(), pose.orientation());
         }

         if (axis) {
            SableCommandHelper.sendSuccessDescribingSubLevelsAtIndex(
               add ? "commands.sable.physics.rotation.add.success" : "commands.sable.physics.rotation.set.success",
               ctx,
               subLevels,
               1,
               getGlobalComponent(global),
               rotationAxis.x + ", " + rotationAxis.y + ", " + rotationAxis.z + ", " + rotationAngle
            );
         } else {
            SableCommandHelper.sendSuccessDescribingSubLevelsAtIndex(
               add ? "commands.sable.physics.rotation.add.success" : "commands.sable.physics.rotation.set.success",
               ctx,
               subLevels,
               1,
               getGlobalComponent(global),
               rotation2.x + ", " + rotation2.y
            );
         }

         return 0;
      }
   }

   private static int executeAddTranslationCommand(CommandContext<CommandSourceStack> ctx, boolean global) throws CommandSyntaxException {
      PhysicsPipeline pipeline = SableCommandHelper.requireSubLevelPhysicsPipeline(ctx);
      Collection<ServerSubLevel> subLevels = SubLevelArgumentType.getSubLevels(ctx, "sub_level");
      if (subLevels.isEmpty()) {
         throw SableCommandHelper.ERROR_NO_SUB_LEVELS_FOUND.create();
      } else {
         Vec3 translation = (Vec3)ctx.getArgument("translation", Vec3.class);
         Vector3d sublevelTranslation = new Vector3d();

         for (ServerSubLevel subLevel : subLevels) {
            JOMLConversion.toJOML(translation, sublevelTranslation);
            if (!global) {
               subLevel.logicalPose().transformNormal(sublevelTranslation);
            }

            pipeline.teleport(subLevel, subLevel.logicalPose().position().add(sublevelTranslation), subLevel.logicalPose().orientation());
         }

         SableCommandHelper.sendSuccessDescribingSubLevelsAtIndex(
            "commands.sable.physics.translation.add.success",
            ctx,
            subLevels,
            1,
            getGlobalComponent(global),
            translation.x + ", " + translation.y + ", " + translation.z
         );
         return 0;
      }
   }

   private static int executeSetTranslationCommand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      PhysicsPipeline pipeline = SableCommandHelper.requireSubLevelPhysicsPipeline(ctx);
      Collection<ServerSubLevel> subLevels = SubLevelArgumentType.getSubLevels(ctx, "sub_level");
      if (subLevels.isEmpty()) {
         throw SableCommandHelper.ERROR_NO_SUB_LEVELS_FOUND.create();
      } else {
         Vector3d translation = JOMLConversion.toJOML(Vec3Argument.getVec3(ctx, "translation"));

         for (ServerSubLevel subLevel : subLevels) {
            pipeline.teleport(subLevel, translation, subLevel.logicalPose().orientation());
         }

         SableCommandHelper.sendSuccessDescribingSubLevels(
            "commands.sable.physics.translation.set.success", ctx, subLevels, translation.x + ", " + translation.y + ", " + translation.z
         );
         return 0;
      }
   }
}

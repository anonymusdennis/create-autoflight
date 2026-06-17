package dev.ryanhcode.sable.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.ryanhcode.sable.api.command.SableCommandHelper;
import dev.ryanhcode.sable.api.command.SubLevelArgumentType;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintConfiguration;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.Collection;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

public class SableJointCommands {
   public static final SimpleCommandExceptionType MISSING_JOINT_SUBLEVEL_TARGET = new SimpleCommandExceptionType(
      Component.translatable("commands.sable.joint.missing_sublevel_target")
   );

   public static void register(LiteralArgumentBuilder<CommandSourceStack> sableBuilder, CommandBuildContext buildContext) {
      sableBuilder.then(
         Commands.literal("joint")
            .then(
               Commands.literal("add")
                  .then(
                     Commands.argument("subLevel1", SubLevelArgumentType.subLevels())
                        .then(
                           Commands.argument("subLevel2", SubLevelArgumentType.subLevels())
                              .then(
                                 Commands.literal("rotary")
                                    .then(
                                       Commands.argument("pos1", Vec3Argument.vec3(false))
                                          .then(
                                             Commands.argument("pos2", Vec3Argument.vec3(false))
                                                .then(
                                                   Commands.argument("axis1", Vec3Argument.vec3(false))
                                                      .then(
                                                         Commands.argument("axis2", Vec3Argument.vec3(false))
                                                            .executes(SableJointCommands::executeAddJointCommand)
                                                      )
                                                )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static int executeAddJointCommand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      ServerSubLevelContainer container = SableCommandHelper.requireSubLevelContainer(ctx);
      PhysicsPipeline pipeline = SableCommandHelper.requireSubLevelPhysicsSystem(container).getPipeline();
      addRotaryJoint(
         pipeline,
         SubLevelArgumentType.getSubLevels(ctx, "subLevel1"),
         SubLevelArgumentType.getSubLevels(ctx, "subLevel2"),
         Vec3Argument.getVec3(ctx, "pos1"),
         Vec3Argument.getVec3(ctx, "pos2"),
         Vec3Argument.getVec3(ctx, "axis1"),
         Vec3Argument.getVec3(ctx, "axis2")
      );
      ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable("commands.sable.joint.success"), true);
      return 0;
   }

   private static void addRotaryJoint(
      PhysicsPipeline pipeline, Collection<ServerSubLevel> subLevel1, Collection<ServerSubLevel> subLevel2, Vec3 pos1, Vec3 pos2, Vec3 axis1, Vec3 axis2
   ) throws CommandSyntaxException {
      RotaryConstraintConfiguration constraintConfig = new RotaryConstraintConfiguration(
         JOMLConversion.toJOML(pos1), JOMLConversion.toJOML(pos2), JOMLConversion.toJOML(axis1), JOMLConversion.toJOML(axis2)
      );
      ServerSubLevel jointSubLevel1 = subLevel1.stream().findFirst().orElseThrow(MISSING_JOINT_SUBLEVEL_TARGET::create);
      ServerSubLevel jointSubLevel2 = subLevel2.stream().findFirst().orElseThrow(MISSING_JOINT_SUBLEVEL_TARGET::create);
      pipeline.addConstraint(jointSubLevel1, jointSubLevel2, constraintConfig);
   }
}

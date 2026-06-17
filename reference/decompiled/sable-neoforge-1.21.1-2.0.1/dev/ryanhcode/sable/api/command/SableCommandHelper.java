package dev.ryanhcode.sable.api.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class SableCommandHelper {
   private static final SimpleCommandExceptionType MISSING_SUBLEVEL_CONTAINER = new SimpleCommandExceptionType(
      Component.translatable("commands.sable.helper.missing_sub_level_container")
   );
   private static final SimpleCommandExceptionType MISSING_PHYSICS_SYSTEM = new SimpleCommandExceptionType(
      Component.translatable("commands.sable.helper.missing_physics_system")
   );
   public static final SimpleCommandExceptionType ERROR_NO_SUB_LEVELS_FOUND = new SimpleCommandExceptionType(
      Component.translatable("commands.sable.fail.no_sub_levels")
   );
   public static final SimpleCommandExceptionType ERROR_NOT_INSIDE_SUB_LEVEL = new SimpleCommandExceptionType(
      Component.translatable("commands.sable.fail.not_inside_sub_level")
   );
   public static final SimpleCommandExceptionType ERROR_NO_AXIS_FOR_ROTATION = new SimpleCommandExceptionType(
      Component.translatable("commands.sable.fail.no_axis_for_rotation")
   );
   public static final SimpleCommandExceptionType ERROR_NO_SUB_LEVELS_MODIFIED = new SimpleCommandExceptionType(
      Component.translatable("commands.sable.fail.unmodified")
   );
   public static final SimpleCommandExceptionType ERROR_SUB_LEVEL_UNNAMED = new SimpleCommandExceptionType(
      Component.translatable("commands.sable.sub_level.get_name.failure_unnamed")
   );

   public static Component getResultComponentForSublevelCollection(
      String translationKey, Collection<ServerSubLevel> subLevels, int subLevelsDescriptionIndex, Object... additionalArguments
   ) {
      boolean isPlural = subLevels.size() != 1;
      Object[] translationArguments = new Object[additionalArguments.length + 1];
      System.arraycopy(additionalArguments, 0, translationArguments, 1, additionalArguments.length);
      if (isPlural) {
         translationArguments[0] = Component.translatable("commands.sable.sub_levels", new Object[]{subLevels.size()});
      } else {
         SubLevel subLevel = subLevels.iterator().next();
         Object name = subLevel.getName() == null ? Component.translatable("commands.sable.sub_level") : subLevel.getName();
         translationArguments[0] = name;
      }

      if (subLevelsDescriptionIndex != 0) {
         Object swap = translationArguments[subLevelsDescriptionIndex];
         translationArguments[subLevelsDescriptionIndex] = translationArguments[0];
         translationArguments[0] = swap;
      }

      return Component.translatable(translationKey, translationArguments);
   }

   public static void sendSuccessDescribingSubLevels(
      String translationKey, CommandContext<CommandSourceStack> context, Collection<ServerSubLevel> subLevels, Object... additionalArguments
   ) {
      sendSuccessDescribingSubLevelsAtIndex(translationKey, context, subLevels, 0, additionalArguments);
   }

   public static void sendSuccessDescribingSubLevelsAtIndex(
      String translationKey,
      CommandContext<CommandSourceStack> context,
      Collection<ServerSubLevel> subLevels,
      int subLevelsDescriptionIndex,
      Object... additionalArguments
   ) {
      ((CommandSourceStack)context.getSource())
         .sendSuccess(() -> getResultComponentForSublevelCollection(translationKey, subLevels, subLevelsDescriptionIndex, additionalArguments), true);
   }

   public static ServerSubLevelContainer requireSubLevelContainer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
      return requireSubLevelContainer((CommandSourceStack)context.getSource());
   }

   public static ServerSubLevelContainer requireSubLevelContainer(CommandSourceStack source) throws CommandSyntaxException {
      ServerLevel level = source.getLevel();
      return requireNotNull(SubLevelContainer.getContainer(level), MISSING_SUBLEVEL_CONTAINER);
   }

   public static SubLevelPhysicsSystem requireSubLevelPhysicsSystem(ServerSubLevelContainer subLevelContainer) throws CommandSyntaxException {
      return requireNotNull(subLevelContainer.physicsSystem(), MISSING_PHYSICS_SYSTEM);
   }

   public static SubLevelPhysicsSystem requireSubLevelPhysicsSystem(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
      return requireSubLevelPhysicsSystem(requireSubLevelContainer(context));
   }

   public static PhysicsPipeline requireSubLevelPhysicsPipeline(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
      return requireSubLevelPhysicsSystem(context).getPipeline();
   }

   public static <T> T requireNotNull(T value, SimpleCommandExceptionType message) throws CommandSyntaxException {
      if (value == null) {
         throw message.create();
      } else {
         return value;
      }
   }
}

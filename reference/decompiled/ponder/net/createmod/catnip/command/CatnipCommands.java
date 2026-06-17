package net.createmod.catnip.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collections;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class CatnipCommands {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      LiteralCommandNode<CommandSourceStack> util = buildUtilityCommands();
      LiteralCommandNode<CommandSourceStack> catnipRoot = ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("catnip")
                  .requires(cs -> cs.hasPermission(0)))
               .then(ConfigCommand.register()))
            .then(util))
         .build();
      catnipRoot.addChild(buildRedirect("u", util));
      dispatcher.getRoot().addChild(catnipRoot);
      createOrAddToShortcut(dispatcher, "c", catnipRoot);
   }

   private static LiteralCommandNode<CommandSourceStack> buildUtilityCommands() {
      return ((LiteralArgumentBuilder)Commands.literal("util").then(FlySpeedCommand.register())).build();
   }

   public static LiteralCommandNode<CommandSourceStack> buildRedirect(String alias, LiteralCommandNode<CommandSourceStack> destination) {
      LiteralArgumentBuilder<CommandSourceStack> builder = (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)LiteralArgumentBuilder.literal(
                  alias
               )
               .requires(destination.getRequirement()))
            .forward(destination.getRedirect(), destination.getRedirectModifier(), destination.isFork()))
         .executes(destination.getCommand());

      for (CommandNode<CommandSourceStack> child : destination.getChildren()) {
         builder.then(child);
      }

      return builder.build();
   }

   public static void createOrAddToShortcut(
      CommandDispatcher<CommandSourceStack> dispatcher, String shortcut, LiteralCommandNode<CommandSourceStack> createRoot
   ) {
      CommandNode<CommandSourceStack> node = dispatcher.findNode(Collections.singleton(shortcut));
      if (node == null) {
         dispatcher.getRoot().addChild(buildRedirect(shortcut, createRoot));
      } else {
         for (CommandNode<CommandSourceStack> child : createRoot.getChildren()) {
            node.addChild(child);
         }
      }
   }
}

package dev.engine_room.flywheel.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.engine_room.flywheel.api.backend.Backend;
import dev.engine_room.flywheel.api.backend.BackendManager;
import dev.engine_room.flywheel.backend.BackendDebugFlags;
import dev.engine_room.flywheel.backend.compile.LightSmoothness;
import dev.engine_room.flywheel.backend.compile.PipelineCompiler;
import dev.engine_room.flywheel.backend.engine.uniform.DebugMode;
import dev.engine_room.flywheel.backend.engine.uniform.FrameUniforms;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;

public final class FlwCommands {
   private FlwCommands() {
   }

   public static void registerClientCommands(RegisterClientCommandsEvent event) {
      LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("flywheel");
      ConfigValue<String> backendValue = NeoForgeFlwConfig.INSTANCE.client.backend;
      command.then(
         ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("backend").executes(context -> {
               Backend backend = BackendManager.currentBackend();
               String idStr = Backend.REGISTRY.getIdOrThrow(backend).toString();
               sendMessage((CommandSourceStack)context.getSource(), Component.translatable("command.flywheel.backend.get", new Object[]{idStr}));
               return 1;
            })).then(Commands.literal("DEFAULT").executes(context -> {
               backendValue.set("DEFAULT");
               Minecraft.getInstance().levelRenderer.allChanged();
               Backend actualBackend = BackendManager.currentBackend();
               String actualIdStr = Backend.REGISTRY.getIdOrThrow(actualBackend).toString();
               sendMessage((CommandSourceStack)context.getSource(), Component.translatable("command.flywheel.backend.set", new Object[]{actualIdStr}));
               return 1;
            })))
            .then(
               Commands.argument("id", BackendArgument.INSTANCE)
                  .executes(
                     context -> {
                        Backend requestedBackend = (Backend)context.getArgument("id", Backend.class);
                        String requestedIdStr = Backend.REGISTRY.getIdOrThrow(requestedBackend).toString();
                        backendValue.set(requestedIdStr);
                        Minecraft.getInstance().levelRenderer.allChanged();
                        Backend actualBackend = BackendManager.currentBackend();
                        if (actualBackend != requestedBackend) {
                           sendFailure(
                              (CommandSourceStack)context.getSource(),
                              Component.translatable("command.flywheel.backend.set.unavailable", new Object[]{requestedIdStr})
                           );
                        }

                        String actualIdStr = Backend.REGISTRY.getIdOrThrow(actualBackend).toString();
                        sendMessage((CommandSourceStack)context.getSource(), Component.translatable("command.flywheel.backend.set", new Object[]{actualIdStr}));
                        return 1;
                     }
                  )
            )
      );
      BooleanValue limitUpdatesValue = NeoForgeFlwConfig.INSTANCE.client.limitUpdates;
      command.then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("limitUpdates").executes(context -> {
         if ((Boolean)limitUpdatesValue.get()) {
            sendMessage((CommandSourceStack)context.getSource(), Component.translatable("command.flywheel.limit_updates.get.on"));
         } else {
            sendMessage((CommandSourceStack)context.getSource(), Component.translatable("command.flywheel.limit_updates.get.off"));
         }

         return 1;
      })).then(Commands.literal("on").executes(context -> {
         limitUpdatesValue.set(true);
         sendMessage((CommandSourceStack)context.getSource(), Component.translatable("command.flywheel.limit_updates.set.on"));
         Minecraft.getInstance().levelRenderer.allChanged();
         return 1;
      }))).then(Commands.literal("off").executes(context -> {
         limitUpdatesValue.set(false);
         sendMessage((CommandSourceStack)context.getSource(), Component.translatable("command.flywheel.limit_updates.set.off"));
         Minecraft.getInstance().levelRenderer.allChanged();
         return 1;
      })));
      EnumValue<LightSmoothness> lightSmoothnessValue = NeoForgeFlwConfig.INSTANCE.client.backendConfig.lightSmoothness;
      command.then(Commands.literal("lightSmoothness").then(Commands.argument("mode", LightSmoothnessArgument.INSTANCE).executes(context -> {
         LightSmoothness oldValue = (LightSmoothness)lightSmoothnessValue.get();
         LightSmoothness newValue = (LightSmoothness)context.getArgument("mode", LightSmoothness.class);
         if (oldValue != newValue) {
            lightSmoothnessValue.set(newValue);
            PipelineCompiler.deleteAll();
         }

         return 1;
      })));
      command.then(createDebugCommand());
      event.getDispatcher().register(command);
   }

   private static LiteralArgumentBuilder<CommandSourceStack> createDebugCommand() {
      LiteralArgumentBuilder<CommandSourceStack> debug = Commands.literal("debug");
      debug.then(
         Commands.literal("crumbling")
            .then(
               Commands.argument("pos", BlockPosArgument.blockPos()).then(Commands.argument("stage", IntegerArgumentType.integer(0, 9)).executes(context -> {
                  Entity executor = ((CommandSourceStack)context.getSource()).getEntity();
                  if (executor == null) {
                     return 0;
                  } else {
                     BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
                     int value = IntegerArgumentType.getInteger(context, "stage");
                     executor.level().destroyBlockProgress(executor.getId(), pos, value);
                     return 1;
                  }
               }))
            )
      );
      debug.then(Commands.literal("shader").then(Commands.argument("mode", DebugModeArgument.INSTANCE).executes(context -> {
         DebugMode mode = (DebugMode)context.getArgument("mode", DebugMode.class);
         FrameUniforms.debugMode(mode);
         return 1;
      })));
      debug.then(((LiteralArgumentBuilder)Commands.literal("frustum").then(Commands.literal("capture").executes(context -> {
         FrameUniforms.captureFrustum();
         return 1;
      }))).then(Commands.literal("unpause").executes(context -> {
         FrameUniforms.unpauseFrustum();
         return 1;
      })));
      debug.then(((LiteralArgumentBuilder)Commands.literal("lightSections").then(Commands.literal("on").executes(context -> {
         BackendDebugFlags.LIGHT_STORAGE_VIEW = true;
         return 1;
      }))).then(Commands.literal("off").executes(context -> {
         BackendDebugFlags.LIGHT_STORAGE_VIEW = false;
         return 1;
      })));
      debug.then(((LiteralArgumentBuilder)Commands.literal("pauseUpdates").then(Commands.literal("on").executes(context -> {
         ImplDebugFlags.PAUSE_UPDATES = true;
         return 1;
      }))).then(Commands.literal("off").executes(context -> {
         ImplDebugFlags.PAUSE_UPDATES = false;
         return 1;
      })));
      debug.then(Commands.literal("info").executes(context -> {
         ((CommandSourceStack)context.getSource()).sendSystemMessage(FlwDebugInfo.getDebugCommandInfo());
         return 1;
      }));
      return debug;
   }

   private static void sendMessage(CommandSourceStack source, Component message) {
      source.sendSuccess(() -> message, true);
   }

   private static void sendFailure(CommandSourceStack source, Component message) {
      source.sendFailure(message);
   }
}

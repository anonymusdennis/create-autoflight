package dev.engine_room.flywheel.impl;

import dev.engine_room.flywheel.backend.compile.FlwProgramsReloader;
import dev.engine_room.flywheel.backend.engine.uniform.Uniforms;
import dev.engine_room.flywheel.impl.compat.EmbeddiumCompat;
import dev.engine_room.flywheel.impl.visualization.VisualizationEventHandler;
import dev.engine_room.flywheel.lib.model.baked.PartialModelEventHandler;
import dev.engine_room.flywheel.lib.util.LevelAttached;
import dev.engine_room.flywheel.lib.util.RendererReloadCache;
import dev.engine_room.flywheel.lib.util.ResourceReloadHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.CrashReportCallables;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.jetbrains.annotations.UnknownNullability;

@Mod(
   value = "flywheel",
   dist = {Dist.CLIENT}
)
public final class FlywheelNeoForge {
   @UnknownNullability
   private static ArtifactVersion version;

   public FlywheelNeoForge(IEventBus modEventBus, ModContainer modContainer) {
      version = modContainer.getModInfo().getVersion();
      IEventBus gameEventBus = NeoForge.EVENT_BUS;
      NeoForgeFlwConfig.INSTANCE.registerSpecs(modContainer);
      registerImplEventListeners(gameEventBus, modEventBus);
      registerLibEventListeners(gameEventBus, modEventBus);
      registerBackendEventListeners(gameEventBus, modEventBus);
      CrashReportCallables.registerCrashCallable("Flywheel Backend", BackendManagerImpl::getBackendString);
      FlwImpl.init();
      EmbeddiumCompat.init();
   }

   private static void registerImplEventListeners(IEventBus gameEventBus, IEventBus modEventBus) {
      gameEventBus.addListener(e -> BackendManagerImpl.onReloadLevelRenderer(e.level()));
      gameEventBus.addListener(e -> {
         if (e.getLevel().isClientSide()) {
            VisualizationEventHandler.onClientTick(Minecraft.getInstance(), e.getLevel());
         }
      });
      gameEventBus.addListener(e -> VisualizationEventHandler.onEntityJoinLevel(e.getLevel(), e.getEntity()));
      gameEventBus.addListener(e -> VisualizationEventHandler.onEntityLeaveLevel(e.getLevel(), e.getEntity()));
      gameEventBus.addListener(FlwCommands::registerClientCommands);
      gameEventBus.addListener(e -> {
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft.getDebugOverlay().showDebugScreen()) {
            FlwDebugInfo.addDebugInfo(minecraft, e.getRight());
         }
      });
      modEventBus.addListener(e -> BackendManagerImpl.onEndClientResourceReload(e.error().isPresent()));
      modEventBus.addListener(e -> {
         ArgumentTypeInfos.registerByClass(BackendArgument.class, BackendArgument.INFO);
         ArgumentTypeInfos.registerByClass(DebugModeArgument.class, DebugModeArgument.INFO);
         ArgumentTypeInfos.registerByClass(LightSmoothnessArgument.class, LightSmoothnessArgument.INFO);
      });
   }

   private static void registerLibEventListeners(IEventBus gameEventBus, IEventBus modEventBus) {
      gameEventBus.addListener(e -> LevelAttached.invalidateLevel(e.getLevel()));
      modEventBus.addListener(e -> RendererReloadCache.onReloadLevelRenderer());
      modEventBus.addListener(e -> ResourceReloadHolder.onEndClientResourceReload());
      modEventBus.addListener(PartialModelEventHandler::onRegisterAdditional);
      modEventBus.addListener(PartialModelEventHandler::onBakingCompleted);
   }

   private static void registerBackendEventListeners(IEventBus gameEventBus, IEventBus modEventBus) {
      gameEventBus.addListener(e -> Uniforms.onReloadLevelRenderer());
      modEventBus.addListener(e -> e.registerReloadListener(FlwProgramsReloader.INSTANCE));
   }

   public static ArtifactVersion version() {
      return version;
   }
}

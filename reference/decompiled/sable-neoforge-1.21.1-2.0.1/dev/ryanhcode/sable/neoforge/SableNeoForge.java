package dev.ryanhcode.sable.neoforge;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.SableCommonEvents;
import dev.ryanhcode.sable.SableConfig;
import dev.ryanhcode.sable.command.SableCommand;
import dev.ryanhcode.sable.command.argument.SubLevelSelectorModifiers;
import dev.ryanhcode.sable.index.SableAttributes;
import dev.ryanhcode.sable.physics.config.FloatingBlockMaterialDataHandler;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertiesDefinitionLoader;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.CrashReportCallables;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod("sable")
public final class SableNeoForge {
   public SableNeoForge(ModContainer modContainer, IEventBus modBus) {
      Sable.init();
      IEventBus neoBus = NeoForge.EVENT_BUS;
      neoBus.addListener(this::registerCommand);
      neoBus.addListener(this::registerReloadListeners);
      modBus.addListener(this::serverSetup);
      neoBus.addListener(this::syncDataPack);
      SubLevelSelectorModifiers.registerModifiers();
      DeferredRegister<Attribute> attributes = DeferredRegister.create(BuiltInRegistries.ATTRIBUTE, "sable");
      SableAttributes.PUNCH_STRENGTH = attributes.register("player.sub_level_punch_strength", () -> SableAttributes.PUNCH_STRENGTH_ATTRIBUTE);
      SableAttributes.PUNCH_COOLDOWN = attributes.register("player.sub_level_punch_cooldown", () -> SableAttributes.PUNCH_COOLDOWN_ATTRIBUTE);
      attributes.register(modBus);
      modContainer.registerConfig(Type.COMMON, SableConfig.SPEC);
      CrashReportCallables.registerHeader(Sable::getCrashHeader);
   }

   public void registerReloadListeners(AddReloadListenerEvent event) {
      event.addListener(PhysicsBlockPropertiesDefinitionLoader.INSTANCE);
      event.addListener(DimensionPhysicsData.ReloadListener.INSTANCE);
      event.addListener(FloatingBlockMaterialDataHandler.ReloadListener.INSTANCE);
   }

   private void serverSetup(FMLCommonSetupEvent event) {
      SableAttributes.register();
   }

   private void registerCommand(RegisterCommandsEvent event) {
      SableCommand.register(event.getDispatcher(), event.getBuildContext());
   }

   private void syncDataPack(OnDatapackSyncEvent event) {
      SableCommonEvents.syncDataPacket(packet -> event.getRelevantPlayers().forEach(player -> player.connection.send(packet)));
   }
}

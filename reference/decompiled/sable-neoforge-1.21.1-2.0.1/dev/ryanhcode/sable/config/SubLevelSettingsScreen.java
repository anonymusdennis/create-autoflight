package dev.ryanhcode.sable.config;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.physics.config.PhysicsConfigData;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.OptionInstance.IntRange;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class SubLevelSettingsScreen extends OptionsSubScreen {
   public static final Component TITLE = Component.translatable("options.sable_menu");

   public SubLevelSettingsScreen(Screen optionsScreen, Options options, Component component) {
      super(optionsScreen, options, component);
   }

   protected void addOptions() {
      IntegratedServer singleplayerServer = this.minecraft.getSingleplayerServer();
      this.list
         .addBig(
            new OptionInstance(
               "options.physics_steps",
               OptionInstance.cachedConstantTooltip(Component.translatable("options.physics_steps.tooltip")),
               (component, substeps) -> Options.genericValueLabel(
                     component, Component.translatable("options.physics_steps_template", new Object[]{substeps * 20})
                  ),
               new IntRange(1, 10, false),
               SubLevelContainer.getContainer(singleplayerServer.overworld()).physicsSystem().getConfig().substepsPerTick,
               steps -> {
                  for (ServerLevel level : singleplayerServer.getAllLevels()) {
                     SubLevelPhysicsSystem physicsSystem = SubLevelContainer.getContainer(level).physicsSystem();
                     PhysicsConfigData config = physicsSystem.getConfig();
                     config.substepsPerTick = steps;
                     physicsSystem.getPipeline().updateConfigFrom(config);
                  }
               }
            )
         );
   }
}

package dev.simulated_team.simulated.content.worldgen;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.BooleanValue;
import org.jetbrains.annotations.Nullable;

public class AirshipReadyPreset extends SimulatedWorldPreset {
   public AirshipReadyPreset(ResourceLocation id, @Nullable Component description) {
      super(id, description);
   }

   @Override
   public void modifyGameRules(GameRules gameRules) {
      ((BooleanValue)gameRules.getRule(GameRules.RULE_DOMOBSPAWNING)).set(false, null);
      ((BooleanValue)gameRules.getRule(GameRules.RULE_DO_TRADER_SPAWNING)).set(false, null);
      ((BooleanValue)gameRules.getRule(GameRules.RULE_WEATHER_CYCLE)).set(false, null);
      ((BooleanValue)gameRules.getRule(GameRules.RULE_DAYLIGHT)).set(false, null);
   }
}

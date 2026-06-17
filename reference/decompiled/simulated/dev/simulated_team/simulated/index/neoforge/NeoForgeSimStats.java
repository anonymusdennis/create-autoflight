package dev.simulated_team.simulated.index.neoforge;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.index.SimStats;
import java.util.ArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class NeoForgeSimStats extends SimStats {
   public static final DeferredRegister<ResourceLocation> CUSTOM_STAT = DeferredRegister.create(BuiltInRegistries.CUSTOM_STAT, "simulated");
   private static final ArrayList<SimStats.Stat> STATS_TO_LOAD = new ArrayList<>();

   public static void bootstrap() {
      for (SimStats.Stat stat : STATS_TO_LOAD) {
         Stats.CUSTOM.get(stat.identifier().get(), stat.formatter());
      }

      STATS_TO_LOAD.clear();
   }

   public static void register(IEventBus eventBus) {
      new NeoForgeSimStats().init();
      CUSTOM_STAT.register(eventBus);
   }

   @Override
   protected SimStats.Stat makeCustomStat(String key, StatFormatter formatter) {
      ResourceLocation resourcelocation = Simulated.path(key);
      SimStats.Stat stat = new SimStats.Stat(CUSTOM_STAT.register(key, () -> resourcelocation), formatter);
      STATS_TO_LOAD.add(stat);
      return stat;
   }
}

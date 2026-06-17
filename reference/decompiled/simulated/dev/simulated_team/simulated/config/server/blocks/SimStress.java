package dev.simulated_team.simulated.config.server.blocks;

import com.simibubi.create.infrastructure.config.CStress;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import dev.simulated_team.simulated.Simulated;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;

public class SimStress extends CStress {
   private static final Object2DoubleMap<ResourceLocation> DEFAULT_IMPACTS = new Object2DoubleOpenHashMap();
   private static final Object2DoubleMap<ResourceLocation> DEFAULT_CAPACITIES = new Object2DoubleOpenHashMap();

   public void registerAll(Builder builder) {
      builder.comment(new String[]{".", SimStress.Comments.su, SimStress.Comments.impact}).push("impact");
      DEFAULT_IMPACTS.forEach((id, value) -> this.impacts.put(id, builder.define(id.getPath(), value)));
      builder.pop();
      builder.comment(new String[]{".", SimStress.Comments.su, SimStress.Comments.capacity}).push("capacity");
      DEFAULT_CAPACITIES.forEach((id, value) -> this.capacities.put(id, builder.define(id.getPath(), value)));
      builder.pop();
   }

   public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> setNoImpact() {
      return setImpact(0.0);
   }

   public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> setImpact(double value) {
      return builder -> {
         assertFromSimulated(builder);
         DEFAULT_IMPACTS.put(Simulated.path(builder.getName()), value);
         return builder;
      };
   }

   public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> setCapacity(double value) {
      return builder -> {
         assertFromSimulated(builder);
         DEFAULT_CAPACITIES.put(Simulated.path(builder.getName()), value);
         return builder;
      };
   }

   private static void assertFromSimulated(BlockBuilder<?, ?> builder) {
      if (!builder.getOwner().getModid().equals("simulated")) {
         throw new IllegalStateException("Non-Simulated blocks cannot be added to Simulated's config.");
      }
   }

   private static class Comments {
      static String su = "[in Stress Units]";
      static String impact = "Configure the individual stress impact of mechanical blocks. Note that this cost is doubled for every speed increase it receives.";
      static String capacity = "Configure how much stress a source can accommodate for.";
   }
}

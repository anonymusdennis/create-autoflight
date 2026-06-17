package dev.simulated_team.simulated.index;

import com.tterrag.registrate.builders.EntityBuilder;
import com.tterrag.registrate.util.entry.EntityEntry;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.entities.diagram.DiagramEntity;
import dev.simulated_team.simulated.content.entities.diagram.DiagramEntityRenderer;
import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueEntity;
import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueRenderer;
import dev.simulated_team.simulated.content.entities.launched_plunger.LaunchedPlungerEntity;
import dev.simulated_team.simulated.content.entities.launched_plunger.LaunchedPlungerEntityRenderer;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import dev.simulated_team.simulated.service.SimEntityService;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;

public class SimEntityTypes {
   private static final SimulatedRegistrate REGISTRATE = Simulated.getRegistrate();
   public static final EntityEntry<HoneyGlueEntity> HONEY_GLUE = ((EntityBuilder)REGISTRATE.entity("honey_glue", HoneyGlueEntity::create, MobCategory.MISC)
         .renderer(() -> HoneyGlueRenderer::new)
         .transform(
            builder -> applyLoaderSpecificTransform(builder, new SimEntityTypes.EntityLoaderData(10, Integer.MAX_VALUE, 0.1F, 0.1F, 0.0F, false, true, false))
         ))
      .register();
   public static final EntityEntry<LaunchedPlungerEntity> PLUNGER = ((EntityBuilder)REGISTRATE.entity(
            "launched_plunger", LaunchedPlungerEntity::create, MobCategory.MISC
         )
         .renderer(() -> LaunchedPlungerEntityRenderer::new)
         .transform(builder -> applyLoaderSpecificTransform(builder, new SimEntityTypes.EntityLoaderData(10, 5, 0.5F, 0.5F, 0.25F, true, true, false))))
      .register();
   public static final EntityEntry<DiagramEntity> CONTRAPTION_DIAGRAM = ((EntityBuilder)REGISTRATE.entity(
            "contraption_diagram", DiagramEntity::create, MobCategory.MISC
         )
         .renderer(() -> DiagramEntityRenderer::new)
         .transform(
            builder -> applyLoaderSpecificTransform(builder, new SimEntityTypes.EntityLoaderData(10, Integer.MAX_VALUE, 0.1F, 0.1F, 0.0F, false, true, true))
         ))
      .register();

   public static <T extends Entity, P> EntityBuilder<T, P> applyLoaderSpecificTransform(EntityBuilder<T, P> builder, SimEntityTypes.EntityLoaderData data) {
      return SimEntityService.INSTANCE.loaderEntityTransform(builder, data);
   }

   public static void register() {
   }

   public static record EntityLoaderData(
      int clientTrackingRange, int updateFrequency, float width, float height, float eyeHeight, boolean sendVelocity, boolean immuneToFire, boolean fixed
   ) {
   }
}

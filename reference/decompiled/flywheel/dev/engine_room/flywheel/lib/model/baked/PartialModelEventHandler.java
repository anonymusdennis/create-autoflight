package dev.engine_room.flywheel.lib.model.baked;

import java.util.Map;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent.BakingCompleted;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class PartialModelEventHandler {
   private PartialModelEventHandler() {
   }

   public static void onRegisterAdditional(RegisterAdditional event) {
      for (ResourceLocation modelLocation : PartialModel.ALL.keySet()) {
         event.register(ModelResourceLocation.standalone(modelLocation));
      }
   }

   public static void onBakingCompleted(BakingCompleted event) {
      PartialModel.populateOnInit = true;
      Map<ModelResourceLocation, BakedModel> models = event.getModels();

      for (PartialModel partial : PartialModel.ALL.values()) {
         partial.bakedModel = models.get(ModelResourceLocation.standalone(partial.modelLocation()));
      }
   }
}

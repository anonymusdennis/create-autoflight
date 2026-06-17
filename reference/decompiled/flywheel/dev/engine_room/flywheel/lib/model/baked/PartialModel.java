package dev.engine_room.flywheel.lib.model.baked;

import com.google.common.collect.MapMaker;
import dev.engine_room.flywheel.lib.internal.FlwLibXplat;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.UnknownNullability;

public final class PartialModel {
   static final ConcurrentMap<ResourceLocation, PartialModel> ALL = new MapMaker().weakValues().makeMap();
   static boolean populateOnInit = false;
   private final ResourceLocation modelLocation;
   @UnknownNullability
   BakedModel bakedModel;

   private PartialModel(ResourceLocation modelLocation) {
      this.modelLocation = modelLocation;
      if (populateOnInit) {
         this.bakedModel = FlwLibXplat.INSTANCE.getBakedModel(Minecraft.getInstance().getModelManager(), modelLocation);
      }
   }

   public static PartialModel of(ResourceLocation modelLocation) {
      return ALL.computeIfAbsent(modelLocation, PartialModel::new);
   }

   @UnknownNullability
   public BakedModel get() {
      return this.bakedModel;
   }

   public ResourceLocation modelLocation() {
      return this.modelLocation;
   }
}

package dev.engine_room.flywheel.impl;

import dev.engine_room.flywheel.lib.internal.FlwLibXplat;
import dev.engine_room.flywheel.lib.model.SimpleModel;
import dev.engine_room.flywheel.lib.model.baked.BakedModelBuilder;
import dev.engine_room.flywheel.lib.model.baked.BlockModelBuilder;
import dev.engine_room.flywheel.lib.model.baked.ModelBuilderImpl;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.UnknownNullability;

public class FlwLibXplatImpl implements FlwLibXplat {
   @UnknownNullability
   @Override
   public BakedModel getBakedModel(ModelManager modelManager, ResourceLocation location) {
      return modelManager.getModel(ModelResourceLocation.standalone(location));
   }

   @Override
   public SimpleModel buildBakedModelBuilder(BakedModelBuilder builder) {
      return ModelBuilderImpl.buildBakedModelBuilder(builder);
   }

   @Override
   public SimpleModel buildBlockModelBuilder(BlockModelBuilder builder) {
      return ModelBuilderImpl.buildBlockModelBuilder(builder);
   }
}

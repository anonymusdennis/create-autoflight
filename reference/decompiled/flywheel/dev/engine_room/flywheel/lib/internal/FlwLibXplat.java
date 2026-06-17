package dev.engine_room.flywheel.lib.internal;

import dev.engine_room.flywheel.api.internal.DependencyInjection;
import dev.engine_room.flywheel.lib.model.SimpleModel;
import dev.engine_room.flywheel.lib.model.baked.BakedModelBuilder;
import dev.engine_room.flywheel.lib.model.baked.BlockModelBuilder;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.UnknownNullability;

public interface FlwLibXplat {
   FlwLibXplat INSTANCE = DependencyInjection.load(FlwLibXplat.class, "dev.engine_room.flywheel.impl.FlwLibXplatImpl");

   @UnknownNullability
   BakedModel getBakedModel(ModelManager var1, ResourceLocation var2);

   SimpleModel buildBakedModelBuilder(BakedModelBuilder var1);

   SimpleModel buildBlockModelBuilder(BlockModelBuilder var1);
}

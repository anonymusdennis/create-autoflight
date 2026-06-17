package com.simibubi.create.compat.curios;

import com.simibubi.create.AllItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

@OnlyIn(Dist.CLIENT)
public class CuriosRenderers {
   public static void register() {
      CuriosRendererRegistry.register(
         (Item)AllItems.GOGGLES.get(), () -> new GogglesCurioRenderer(Minecraft.getInstance().getEntityModels().bakeLayer(GogglesCurioRenderer.LAYER))
      );
   }

   public static void onLayerRegister(RegisterLayerDefinitions event) {
      event.registerLayerDefinition(GogglesCurioRenderer.LAYER, () -> LayerDefinition.create(GogglesCurioRenderer.mesh(), 1, 1));
   }
}

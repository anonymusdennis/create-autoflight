package net.createmod.ponder.api.element;

import net.createmod.ponder.api.PonderPalette;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public interface TextElementBuilder {
   TextElementBuilder colored(PonderPalette var1);

   TextElementBuilder pointAt(Vec3 var1);

   TextElementBuilder independent(int var1);

   default TextElementBuilder independent() {
      return this.independent(0);
   }

   TextElementBuilder text(String var1);

   TextElementBuilder text(String var1, Object... var2);

   TextElementBuilder sharedText(ResourceLocation var1);

   TextElementBuilder sharedText(ResourceLocation var1, Object... var2);

   TextElementBuilder sharedText(String var1);

   TextElementBuilder sharedText(String var1, Object... var2);

   TextElementBuilder placeNearTarget();

   TextElementBuilder attachKeyFrame();
}

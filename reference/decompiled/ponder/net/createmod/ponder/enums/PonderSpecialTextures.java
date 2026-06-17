package net.createmod.ponder.enums;

import com.mojang.blaze3d.systems.RenderSystem;
import net.createmod.catnip.render.BindableTexture;
import net.createmod.ponder.Ponder;
import net.minecraft.resources.ResourceLocation;

public enum PonderSpecialTextures implements BindableTexture {
   BLANK("blank.png");

   public static final String ASSET_PATH = "textures/special/";
   private final ResourceLocation location;

   private PonderSpecialTextures(String filename) {
      this.location = Ponder.asResource("textures/special/" + filename);
   }

   @Override
   public void bind() {
      RenderSystem.setShaderTexture(0, this.location);
   }

   @Override
   public ResourceLocation getLocation() {
      return this.location;
   }
}

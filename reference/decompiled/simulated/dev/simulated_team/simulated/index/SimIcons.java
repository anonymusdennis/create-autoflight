package dev.simulated_team.simulated.index;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.gui.AllIcons;
import dev.simulated_team.simulated.Simulated;
import net.createmod.catnip.gui.element.DelegatedStencilElement;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class SimIcons extends AllIcons {
   public static final ResourceLocation ICON_ATLAS = Simulated.path("textures/gui/icons.png");
   public static final int ICON_ATLAS_SIZE = 64;
   private static int x = 0;
   private static int y = -1;
   private final int iconX;
   private final int iconY;
   public static final SimIcons HALF_EXTEND = newRow();
   public static final SimIcons FULL_EXTEND = next();
   public static final SimIcons ADD_OR_EDIT = newRow();
   public static final SimIcons HAMBURGER = next();
   public static final SimIcons CANCEL = next();
   public static final SimIcons CONFIG = next();
   public static final SimIcons KEY_ARROW_UP = newRow();
   public static final SimIcons KEY_ARROW_LEFT = next();
   public static final SimIcons KEY_ARROW_DOWN = next();
   public static final SimIcons KEY_ARROW_RIGHT = next();

   public SimIcons(int x, int y) {
      super(x, y);
      this.iconX = x * 16;
      this.iconY = y * 16;
   }

   private static SimIcons next() {
      return new SimIcons(++x, y);
   }

   private static SimIcons newRow() {
      x = 0;
      return new SimIcons(0, ++y);
   }

   public void bind() {
      RenderSystem.setShaderTexture(0, ICON_ATLAS);
   }

   public void render(GuiGraphics graphics, int x, int y) {
      graphics.blit(ICON_ATLAS, x, y, 0, (float)this.iconX, (float)this.iconY, 16, 16, 64, 64);
   }

   public void render(PoseStack ms, MultiBufferSource buffer, int color) {
      VertexConsumer builder = buffer.getBuffer(RenderType.text(ICON_ATLAS));
      Matrix4f matrix = ms.last().pose();
      Color rgb = new Color(color);
      int light = 15728880;
      Vec3 vec1 = new Vec3(0.0, 0.0, 0.0);
      Vec3 vec2 = new Vec3(0.0, 1.0, 0.0);
      Vec3 vec3 = new Vec3(1.0, 1.0, 0.0);
      Vec3 vec4 = new Vec3(1.0, 0.0, 0.0);
      float u1 = (float)this.iconX * 1.0F / 64.0F;
      float u2 = (float)(this.iconX + 16) * 1.0F / 64.0F;
      float v1 = (float)this.iconY * 1.0F / 64.0F;
      float v2 = (float)(this.iconY + 16) * 1.0F / 64.0F;
      this.vertex(builder, matrix, vec1, rgb, u1, v1, 15728880);
      this.vertex(builder, matrix, vec2, rgb, u1, v2, 15728880);
      this.vertex(builder, matrix, vec3, rgb, u2, v2, 15728880);
      this.vertex(builder, matrix, vec4, rgb, u2, v1, 15728880);
   }

   private void vertex(VertexConsumer builder, Matrix4f matrix, Vec3 vec, Color rgb, float u, float v, int light) {
      builder.addVertex(matrix, (float)vec.x, (float)vec.y, (float)vec.z)
         .setColor(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), 255)
         .setUv(u, v)
         .setLight(light);
   }

   public DelegatedStencilElement asStencil() {
      return (DelegatedStencilElement)new DelegatedStencilElement().withStencilRenderer((ms, w, h, alpha) -> this.render(ms, 0, 0)).withBounds(16, 16);
   }
}

package dev.engine_room.flywheel.backend.engine;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.engine_room.flywheel.api.material.DepthTest;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.backend.Samplers;
import java.util.Comparator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.jetbrains.annotations.Nullable;

public final class MaterialRenderState {
   public static final Comparator<Material> COMPARATOR = MaterialRenderState::compare;

   private MaterialRenderState() {
   }

   public static void setup(Material material) {
      setupTexture(material);
      setupBackfaceCulling(material.backfaceCulling());
      setupPolygonOffset(material.polygonOffset());
      setupDepthTest(material.depthTest());
      setupTransparency(material.transparency());
      setupWriteMask(material.writeMask());
   }

   public static void setupOit(Material material) {
      setupTexture(material);
      setupBackfaceCulling(material.backfaceCulling());
      setupPolygonOffset(material.polygonOffset());
      setupDepthTest(material.depthTest());
      WriteMask mask = material.writeMask();
      boolean writeColor = mask.color();
      RenderSystem.colorMask(writeColor, writeColor, writeColor, writeColor);
   }

   private static void setupTexture(Material material) {
      Samplers.DIFFUSE.makeActive();
      AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(material.texture());
      texture.setFilter(material.blur(), material.mipmap());
      int textureId = texture.getId();
      RenderSystem.setShaderTexture(0, textureId);
      RenderSystem.bindTexture(textureId);
   }

   private static void setupBackfaceCulling(boolean backfaceCulling) {
      if (backfaceCulling) {
         RenderSystem.enableCull();
      } else {
         RenderSystem.disableCull();
      }
   }

   private static void setupPolygonOffset(boolean polygonOffset) {
      if (polygonOffset) {
         RenderSystem.polygonOffset(-1.0F, -10.0F);
         RenderSystem.enablePolygonOffset();
      } else {
         RenderSystem.polygonOffset(0.0F, 0.0F);
         RenderSystem.disablePolygonOffset();
      }
   }

   private static void setupDepthTest(DepthTest depthTest) {
      switch (depthTest) {
         case OFF:
            RenderSystem.disableDepthTest();
            break;
         case NEVER:
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(512);
            break;
         case LESS:
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(513);
            break;
         case EQUAL:
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(514);
            break;
         case LEQUAL:
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(515);
            break;
         case GREATER:
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(516);
            break;
         case NOTEQUAL:
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(517);
            break;
         case GEQUAL:
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(518);
            break;
         case ALWAYS:
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(519);
      }
   }

   private static void setupTransparency(Transparency transparency) {
      switch (transparency) {
         case OPAQUE:
            RenderSystem.disableBlend();
            break;
         case ADDITIVE:
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(SourceFactor.ONE, DestFactor.ONE);
            break;
         case LIGHTNING:
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
            break;
         case GLINT:
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(SourceFactor.SRC_COLOR, DestFactor.ONE, SourceFactor.ZERO, DestFactor.ONE);
            break;
         case CRUMBLING:
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(SourceFactor.DST_COLOR, DestFactor.SRC_COLOR, SourceFactor.ONE, DestFactor.ZERO);
            break;
         case TRANSLUCENT:
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
      }
   }

   private static void setupWriteMask(WriteMask mask) {
      RenderSystem.depthMask(mask.depth());
      boolean writeColor = mask.color();
      RenderSystem.colorMask(writeColor, writeColor, writeColor, writeColor);
   }

   public static void reset() {
      resetTexture();
      resetBackfaceCulling();
      resetPolygonOffset();
      resetDepthTest();
      resetTransparency();
      resetWriteMask();
   }

   private static void resetTexture() {
      Samplers.DIFFUSE.makeActive();
      RenderSystem.setShaderTexture(0, 0);
   }

   private static void resetBackfaceCulling() {
      RenderSystem.enableCull();
   }

   private static void resetPolygonOffset() {
      RenderSystem.polygonOffset(0.0F, 0.0F);
      RenderSystem.disablePolygonOffset();
   }

   private static void resetDepthTest() {
      RenderSystem.disableDepthTest();
      RenderSystem.depthFunc(515);
   }

   private static void resetTransparency() {
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
   }

   private static void resetWriteMask() {
      RenderSystem.depthMask(true);
      RenderSystem.colorMask(true, true, true, true);
   }

   public static boolean materialEquals(Material lhs, Material rhs) {
      return lhs == rhs
         ? true
         : lhs.blur() == rhs.blur()
            && lhs.mipmap() == rhs.mipmap()
            && lhs.backfaceCulling() == rhs.backfaceCulling()
            && lhs.polygonOffset() == rhs.polygonOffset()
            && lhs.depthTest() == rhs.depthTest()
            && lhs.transparency() == rhs.transparency()
            && lhs.writeMask() == rhs.writeMask()
            && lhs.light().source().equals(rhs.light().source())
            && lhs.texture().equals(rhs.texture())
            && lhs.cutout().source().equals(rhs.cutout().source())
            && lhs.shaders().fragmentSource().equals(rhs.shaders().fragmentSource())
            && lhs.shaders().vertexSource().equals(rhs.shaders().vertexSource());
   }

   public static boolean materialIsAllNonNull(@Nullable Material material) {
      return material != null
         && material.shaders() != null
         && material.shaders().fragmentSource() != null
         && material.shaders().vertexSource() != null
         && material.fog() != null
         && material.fog().source() != null
         && material.cutout() != null
         && material.cutout().source() != null
         && material.light() != null
         && material.light().source() != null
         && material.texture() != null
         && material.depthTest() != null
         && material.transparency() != null
         && material.writeMask() != null
         && material.cardinalLightingMode() != null;
   }

   public static int compare(Material lhs, Material rhs) {
      if (lhs == rhs) {
         return 0;
      } else {
         int cmp = lhs.transparency().compareTo(rhs.transparency());
         if (cmp != 0) {
            return cmp;
         } else {
            cmp = lhs.light().source().compareTo(rhs.light().source());
            if (cmp != 0) {
               return cmp;
            } else {
               cmp = lhs.cutout().source().compareTo(rhs.cutout().source());
               if (cmp != 0) {
                  return cmp;
               } else {
                  cmp = lhs.shaders().fragmentSource().compareTo(rhs.shaders().fragmentSource());
                  if (cmp != 0) {
                     return cmp;
                  } else {
                     cmp = lhs.shaders().vertexSource().compareTo(rhs.shaders().vertexSource());
                     if (cmp != 0) {
                        return cmp;
                     } else {
                        cmp = lhs.texture().compareTo(rhs.texture());
                        if (cmp != 0) {
                           return cmp;
                        } else {
                           cmp = Boolean.compare(lhs.blur(), rhs.blur());
                           if (cmp != 0) {
                              return cmp;
                           } else {
                              cmp = Boolean.compare(lhs.mipmap(), rhs.mipmap());
                              if (cmp != 0) {
                                 return cmp;
                              } else {
                                 cmp = Boolean.compare(lhs.backfaceCulling(), rhs.backfaceCulling());
                                 if (cmp != 0) {
                                    return cmp;
                                 } else {
                                    cmp = Boolean.compare(lhs.polygonOffset(), rhs.polygonOffset());
                                    if (cmp != 0) {
                                       return cmp;
                                    } else {
                                       cmp = lhs.depthTest().compareTo(rhs.depthTest());
                                       if (cmp != 0) {
                                          return cmp;
                                       } else {
                                          cmp = lhs.writeMask().compareTo(rhs.writeMask());
                                          return cmp != 0 ? cmp : 0;
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }
}

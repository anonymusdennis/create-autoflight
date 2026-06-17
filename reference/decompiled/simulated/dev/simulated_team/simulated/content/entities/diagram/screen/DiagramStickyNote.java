package dev.simulated_team.simulated.content.entities.diagram.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.simulated_team.simulated.content.entities.diagram.DiagramConfig;
import dev.simulated_team.simulated.index.SimGUITextures;
import foundry.veil.api.client.render.VeilLevelPerspectiveRenderer;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class DiagramStickyNote extends DiagramButton {
   private static final SimGUITextures NOTE_TEXTURE = SimGUITextures.DIAGRAM_STICKY_NOTE;
   private static final int SUBLEVEL_RENDER_WIDTH_PIXELS = 88;
   private static final int SUBLEVEL_RENDER_HEIGHT_PIXELS = 88;
   private static final int SUBLEVEL_RENDER_X_OFFSET = 8;
   private static final int SUBLEVEL_RENDER_Y_OFFSET = 7;
   public static final int MAX_OFFSET = NOTE_TEXTURE.width;
   public static final int MIN_OFFSET = 9;
   private static final Vector3d NOTE_LOCAL_CAM_POS = new Vector3d();
   private static final Vector3d NOTE_CAMERA_POS = new Vector3d();
   private static final Matrix4f NOTE_PROJ_MAT = new Matrix4f();
   private static final Quaternionf NOTE_ORIENTATION = new Quaternionf();
   private DiagramScreen parent;
   private float lastOffset = 9.0F;
   private float currentOffset = 9.0F;
   private AdvancedFbo fbo;
   private AdvancedFbo outlineFbo;
   private AdvancedFbo finalFbo;
   private float renderTime = 0.0F;
   private final int renderXStart;

   public DiagramStickyNote(DiagramScreen parent, int diagramX, int diagramY, Component message, Runnable onClick) {
      super(NOTE_TEXTURE, 0, diagramY + 5, message, onClick);
      this.renderXStart = diagramX + SimGUITextures.DIAGRAM.width - NOTE_TEXTURE.width + 9;
      this.setX(this.renderXStart);
      this.parent = parent;
   }

   public void tick() {
      this.lastOffset = this.currentOffset;
      float target = 9.0F;
      if (this.active) {
         target = (float)(MAX_OFFSET - 8);
      }

      this.currentOffset = Mth.lerp(0.4F, this.currentOffset, target);
      this.setX((int)((float)this.renderXStart + this.currentOffset));
   }

   private float lerpedOffset(float pt) {
      return Mth.lerp(pt, this.lastOffset, this.currentOffset);
   }

   public void activate() {
      if (!this.active) {
         Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
         this.active = true;
      }
   }

   public void deactivate() {
      this.active = false;
   }

   public void create(DiagramConfig.NoteConfigs noteConfigs) {
      this.fbo = AdvancedFbo.withSize(88, 88).addColorTextureBuffer().setDepthTextureBuffer().build(true);
      this.outlineFbo = AdvancedFbo.withSize(88, 88).addColorTextureBuffer().build(true);
      this.finalFbo = AdvancedFbo.withSize(88, 88).addColorTextureBuffer().build(true);
      this.active = noteConfigs.isActive();
      this.updateOrientation();
      if (this.active) {
         this.currentOffset = (float)(MAX_OFFSET - 8);
         this.lastOffset = this.currentOffset;
      }

      this.visible = true;
   }

   public void free() {
      this.deactivate();
      NOTE_ORIENTATION.set(0.0F, 0.0F, 0.0F, 0.0F);
      if (this.fbo != null) {
         this.fbo.free();
         this.fbo = null;
         this.outlineFbo.free();
         this.outlineFbo = null;
         this.finalFbo.free();
         this.finalFbo = null;
      }

      this.parent = null;
   }

   public void updateCurrentScope(Vector2dc start, Vector2dc end, Vector3dc localPosition, Matrix4fc projMatrix) {
      this.updateOrientation();
      int width = DiagramScreen.DIAGRAM_TEXTURE.width;
      int height = DiagramScreen.DIAGRAM_TEXTURE.height;
      Vector3d startPlotSpace = DiagramScreen.getPlotCoords(start, NOTE_ORIENTATION, localPosition, projMatrix, width, height);
      Vector3d endPlotSpace = DiagramScreen.getPlotCoords(end, NOTE_ORIENTATION, localPosition, projMatrix, width, height);
      this.parent
         .config
         .getNoteConfigs()
         .getNoteScope()
         .set(startPlotSpace.x, startPlotSpace.y, startPlotSpace.z, endPlotSpace.x, endPlotSpace.y, endPlotSpace.z);
   }

   public void handleInternalUpdate(Vector2d magnifyingTarget, Vector2d inverseTarget) {
      magnifyingTarget.sub((double)this.getSublevelRenderX(), (double)this.getSublevelRenderY());
      inverseTarget.sub((double)this.getSublevelRenderX(), (double)this.getSublevelRenderY());
      int width = 88;
      int height = 88;
      Vector3d startPlotSpace = DiagramScreen.getPlotCoords(magnifyingTarget, NOTE_ORIENTATION, NOTE_LOCAL_CAM_POS, NOTE_PROJ_MAT, 88, 88);
      Vector3d endPlotSpace = DiagramScreen.getPlotCoords(inverseTarget, NOTE_ORIENTATION, NOTE_LOCAL_CAM_POS, NOTE_PROJ_MAT, 88, 88);
      this.parent
         .config
         .getNoteConfigs()
         .getNoteScope()
         .set(startPlotSpace.x, startPlotSpace.y, startPlotSpace.z, endPlotSpace.x, endPlotSpace.y, endPlotSpace.z);
   }

   private void updateOrientation() {
      this.renderTime = 100.0F;
      NOTE_ORIENTATION.identity()
         .rotateY((float)Math.toRadians(this.parent.config.getNoteConfigs().getNoteYaw()))
         .rotateX((float)Math.toRadians(this.parent.config.getNoteConfigs().getNotePitch()));
   }

   public boolean contains(double x, double y) {
      if (!this.active) {
         return false;
      } else {
         x -= (double)this.getSublevelRenderX();
         y -= (double)this.getSublevelRenderY();
         return x > 0.0 && x < 88.0 && y > 0.0 && y < 88.0;
      }
   }

   public Vector2d clamp(Vector2d dest) {
      float minX = this.getSublevelRenderX();
      float minY = this.getSublevelRenderY();
      dest.max(new Vector2d((double)minX, (double)minY));
      dest.min(new Vector2d((double)(minX + 88.0F), (double)(minY + 88.0F)));
      return dest;
   }

   private float getSublevelRenderX() {
      return (float)this.renderXStart + this.currentOffset + 8.0F;
   }

   private float getSublevelRenderY() {
      return (float)(this.getY() + 7);
   }

   @Override
   protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
      PoseStack ps = guiGraphics.pose();
      ps.pushPose();
      float currentX = (float)this.renderXStart + this.lerpedOffset(partialTicks);
      int currentY = this.getY();
      ps.translate(currentX, (float)currentY, 0.0F);
      SimGUITextures.DIAGRAM_STICKY_NOTE.render(guiGraphics, 0, 0);
      if (this.active) {
         ps.pushPose();
         ps.translate(8.0F, 7.0F, 0.0F);
         if (!VeilLevelPerspectiveRenderer.isRenderingPerspective() && this.fbo != null) {
            this.populateFBO(partialTicks);
            DiagramScreen.renderFBO(guiGraphics, this.finalFbo, 88, 88);
         }

         this.parent.renderArrows(guiGraphics, mouseX, mouseY, (int)currentX + 8, currentY + 7, NOTE_ORIENTATION, NOTE_LOCAL_CAM_POS, NOTE_PROJ_MAT, 88, 88);
         BufferSource bufferSource = guiGraphics.bufferSource();
         bufferSource.endBatch();
         this.renderCustomCOM(guiGraphics, ps);
         ps.popPose();
      }

      ps.popPose();
   }

   public void populateFBO(float partialTicks) {
      if (this.renderTime >= 1.6666666F) {
         this.renderTime = 0.0F;
         float zNear = 0.1F;
         LevelPlot plot = this.parent.subLevel.getPlot();
         BoundingBox3ic plotBounds = plot.getBoundingBox();
         float maxDistance = (float)(
            Math.max(Math.max(plotBounds.maxX() - plotBounds.minX(), plotBounds.maxY() - plotBounds.minY()), plotBounds.maxZ() - plotBounds.minZ()) + 1
         );
         BoundingBox3ic scopeBounds = new BoundingBox3i(this.parent.config.getNoteConfigs().getNoteScope());
         float radius = (float)(
            Math.max(Math.max(scopeBounds.maxX() - scopeBounds.minX(), scopeBounds.maxY() - scopeBounds.minY()), scopeBounds.maxZ() - scopeBounds.minZ()) + 1
         );
         radius *= 0.55F;
         radius = Math.max(radius, 1.0F);
         Vector3d plotBoundsCenter = new Vector3d(
            (double)(scopeBounds.minX() + scopeBounds.maxX() + 1) / 2.0,
            (double)(scopeBounds.minY() + scopeBounds.maxY() + 1) / 2.0,
            (double)(scopeBounds.minZ() + scopeBounds.maxZ() + 1) / 2.0
         );
         float aspect = 1.0F;
         NOTE_PROJ_MAT.identity().ortho(-radius * 1.0F, radius * 1.0F, -radius, radius, 0.1F, maxDistance * 2.0F);
         NOTE_LOCAL_CAM_POS.set(plotBoundsCenter.add(NOTE_ORIENTATION.transform(new Vector3d(0.0, 0.0, (double)maxDistance))));
         Pose3dc renderPose = this.parent.subLevel.renderPose(partialTicks);
         renderPose.transformPosition(NOTE_CAMERA_POS.set(NOTE_LOCAL_CAM_POS));
         DiagramScreen.draw(
            this.parent.subLevel,
            partialTicks,
            NOTE_ORIENTATION,
            NOTE_PROJ_MAT,
            NOTE_CAMERA_POS,
            88.0F,
            88.0F,
            this.fbo,
            this.outlineFbo,
            this.finalFbo,
            0.75F,
            1.15F,
            7235661,
            5854270
         );
      } else {
         this.renderTime = this.renderTime + Minecraft.getInstance().getTimer().getRealtimeDeltaTicks();
      }
   }

   @Override
   public void playDownSound(SoundManager handler) {
   }

   private void renderCustomCOM(GuiGraphics guiGraphics, PoseStack stack) {
      if (this.parent.config.displayCenterOfMass()) {
         stack.pushPose();
         Vector3d centerOfMass = new Vector3d(this.parent.subLevel.logicalPose().rotationPoint());
         Vector2d screenCoords = DiagramScreen.getScreenCoords(centerOfMass, NOTE_ORIENTATION, NOTE_LOCAL_CAM_POS, NOTE_PROJ_MAT, 88, 88);
         SimGUITextures tex = SimGUITextures.DIAGRAM_ICON_COM_TINY;
         double comOffsetX = screenCoords.x - 8.0;
         double comOffsetY = screenCoords.y - 8.0;
         if (comOffsetY > 0.0 && comOffsetX > 0.0 && comOffsetY < 88.0 && comOffsetX < 88.0) {
            stack.translate(comOffsetX, comOffsetY, 0.0);
            guiGraphics.blit(tex.location, 0, 0, 5, (float)tex.startX, (float)tex.startY, tex.width, tex.height, tex.texWidth, tex.texHeight);
         } else {
            float centerX = 44.0F;
            float centerY = 44.0F;
            Vector2d target = new Vector2d(screenCoords.x() - 44.0, screenCoords.y - 44.0).normalize();
            ((PoseTransformStack)TransformStack.of(stack).translate(44.0F, 44.0F, 0.0F).rotate((float)Math.atan2(target.x, -target.y), Axis.Z))
               .translate(-8.0F, -8.0F, 0.0F)
               .translate(0.0F, -40.0F, 0.0F);
            tex = SimGUITextures.DIAGRAM_ICON_COM_ARROW;
            guiGraphics.blit(tex.location, 0, 0, 5, (float)tex.startX, (float)tex.startY, tex.width, tex.height, tex.texWidth, tex.texHeight);
         }

         stack.popPose();
      }
   }
}

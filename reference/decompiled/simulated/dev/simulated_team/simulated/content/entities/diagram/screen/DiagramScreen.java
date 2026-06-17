package dev.simulated_team.simulated.content.entities.diagram.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.simibubi.create.foundation.gui.RemovedGuiUtils;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup.PointForce;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.entities.diagram.DiagramConfig;
import dev.simulated_team.simulated.content.entities.diagram.DiagramEntity;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimGUITextures;
import dev.simulated_team.simulated.index.SimResourceManagers;
import dev.simulated_team.simulated.network.packets.contraption_diagram.DiagramDataPacket;
import dev.simulated_team.simulated.network.packets.contraption_diagram.DiagramSaveConfigPacket;
import dev.simulated_team.simulated.network.packets.contraption_diagram.RequestDiagramDataPacket;
import dev.simulated_team.simulated.util.SimpleSubLevelGroupRenderer;
import foundry.veil.api.client.render.VeilLevelPerspectiveRenderer;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import foundry.veil.api.client.render.post.PostPipeline.Context;
import foundry.veil.api.network.VeilPacketManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

public class DiagramScreen extends AbstractSimiScreen {
   public static int UPDATE_REQUEST_INTERVAL = 10;
   public static final Color TEXT_COLOR = new Color(79, 82, 87);
   public static final Color BUTTON_COLOR = new Color(109, 113, 119);
   public static final Color DULL_BUTTON_COLOR = new Color(181, 177, 168);
   public static final Color BG_COLOR = new Color(247, 240, 221);
   private static final int TOOLTIP_LABEL_COLOR = -4025475;
   private static final int MIN_ARROW_SIZE_PX = 6;
   public static final float PAPER_SLIDE_SPEED = 0.4F;
   public static final float TAB_SLIDE_SPEED = 0.1F;
   public static final int MAX_PAPER_OFFSET = SimGUITextures.DIAGRAM_PAPER.width + 3;
   public static final int MIN_PAPER_OFFSET = 10;
   private static final Vector3d LOCAL_CAMERA_POSITION = new Vector3d();
   private static final Vector3d CAMERA_POSITION = new Vector3d();
   private static final Matrix4f PROJECTION_MAT = new Matrix4f();
   public static final Quaternionf LOCAL_ORIENTATION = new Quaternionf();
   private static final Vector2d MAGNIFYING_CENTER = new Vector2d();
   private static final Vector2d MAGNIFYING_MAX = new Vector2d();
   private static final Vector2d MAGNIFYING_MIN = new Vector2d();
   private static final int MIN_MAGNIFICATION_PIXELS = 3;
   public static final SimGUITextures DIAGRAM_TEXTURE = SimGUITextures.DIAGRAM;
   public static final float FPS = 12.0F;
   private final DiagramEntity diagram;
   public final ClientSubLevel subLevel;
   protected DiagramConfig config;
   private boolean configDirty = false;
   private final List<DiagramForceGroupToggle> forceToggleWidgets = new ObjectArrayList();
   private AdvancedFbo fbo;
   private AdvancedFbo outlineFbo;
   private AdvancedFbo finalFbo;
   private float renderTime = 12.0F;
   private boolean paperVisible = false;
   private float lastPaperOffset = 10.0F;
   private float paperOffset = 10.0F;
   private float lastTabOffset = 0.0F;
   private float tabOffset = 0.0F;
   public final List<FormattedText> tooltipList = new ArrayList<>();
   @Nullable
   private DiagramDataPacket serverData = null;
   private float viewportRadius;
   private int ticksWithoutUpdate = 0;
   private DiagramButton turnUpButton;
   private DiagramButton turnDownButton;
   private DiagramButton mergeButton;
   private boolean magnifying = false;
   private DiagramStickyNote note;

   public DiagramScreen(DiagramEntity diagramEntity, ClientSubLevel subLevel) {
      this.diagram = diagramEntity;
      this.subLevel = subLevel;
   }

   public static void open(DiagramEntity diagramEntity, DiagramConfig config, SubLevel subLevel) {
      Minecraft minecraft = Minecraft.getInstance();
      DiagramScreen screen = new DiagramScreen(diagramEntity, (ClientSubLevel)subLevel);
      screen.config = config;
      screen.updateViewportOrientation();
      minecraft.setScreen(screen);
      minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_WORK_CARTOGRAPHER, 1.0F));
   }

   private void updateViewportOrientation() {
      this.renderTime = Float.MAX_VALUE;
      LOCAL_ORIENTATION.identity().rotateY((float)Math.toRadians(this.config.yaw())).rotateX((float)Math.toRadians(this.config.pitch()));
   }

   private void freeFramebuffers() {
      if (this.note != null) {
         this.note.free();
      }

      if (this.fbo != null) {
         this.fbo.free();
         this.fbo = null;
         this.outlineFbo.free();
         this.outlineFbo = null;
         this.finalFbo.free();
         this.finalFbo = null;
      }
   }

   public void onClose() {
      super.onClose();
      this.freeFramebuffers();
   }

   protected void init() {
      super.init();
      this.freeFramebuffers();
      this.fbo = AdvancedFbo.withSize(DIAGRAM_TEXTURE.width, DIAGRAM_TEXTURE.height).addColorTextureBuffer().setDepthTextureBuffer().build(true);
      this.outlineFbo = AdvancedFbo.withSize(DIAGRAM_TEXTURE.width, DIAGRAM_TEXTURE.height).addColorTextureBuffer().build(true);
      this.finalFbo = AdvancedFbo.withSize(DIAGRAM_TEXTURE.width, DIAGRAM_TEXTURE.height).addColorTextureBuffer().build(true);
      int diagramX = this.width / 2 - DIAGRAM_TEXTURE.width / 2;
      int diagramY = this.height / 2 - DIAGRAM_TEXTURE.height / 2;
      this.note = new DiagramStickyNote(this, diagramX, diagramY, Component.empty(), () -> {
      });
      this.note.create(this.config.getNoteConfigs());
      if (this.subLevel.isRemoved()) {
         this.onClose();
      } else {
         this.renderContents(this.subLevel, 0.0F);

         for (int i = 0; i < 1; i++) {
            this.addGreebles(diagramX, diagramY);
         }

         DiagramButton forceButton = new DiagramButton(SimGUITextures.DIAGRAM_ICON_FORCES, diagramX + 9, diagramY + 9, Component.empty(), () -> {
            this.paperVisible = !this.paperVisible;
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
         }).setDiagramTooltip(() -> SimLang.translate("contraption_diagram.toggle_paper").component());
         this.mergeButton = new DiagramButton(this.getMergeIcon(), diagramX + 9, diagramY + 9 + 20, Component.empty(), () -> {
               this.config.setMergeForces(!this.config.mergeForces());
               this.mergeButton.setTexture(this.getMergeIcon());
               this.setConfigDirty();
            })
            .setDiagramTooltip(
               () -> SimLang.translate("contraption_diagram.merge_forces")
                     .color(-4025475)
                     .add(SimLang.translate(this.config.mergeForces() ? "contraption_diagram.merged" : "contraption_diagram.unmerged").color(-1))
                     .component()
            );
         DiagramButton centerOfMassButton = new DiagramButton(
               SimGUITextures.DIAGRAM_ICON_COM_TOGGLE, diagramX + 9, diagramY + 9 + 40, Component.empty(), () -> {
                  this.config.setDisplayCenterOfMass(!this.config.displayCenterOfMass());
                  this.setConfigDirty();
               }
            )
            .setDiagramTooltip(
               () -> SimLang.translate("contraption_diagram.center_of_mass")
                     .color(-4025475)
                     .add(SimLang.translate(this.config.displayCenterOfMass() ? "contraption_diagram.shown" : "contraption_diagram.hidden").color(-1))
                     .component()
            );
         DiagramButton massButton = new DiagramButton(SimGUITextures.DIAGRAM_ICON_MASS, diagramX + 9, diagramY + 9 + 60, Component.empty(), () -> {
            })
            .setDiagramTooltip(
               () -> {
                  String massString = this.serverData != null ? String.format("%,.2f", this.serverData.mass()) : "---";
                  return SimLang.translate("contraption_diagram.total_mass")
                     .color(-4025475)
                     .add(SimLang.translate("contraption_diagram.mass", massString).color(-1))
                     .component();
               }
            );
         massButton.active = false;
         this.addRenderableWidget(forceButton);
         this.addRenderableWidget(centerOfMassButton);
         this.addRenderableWidget(massButton);
         this.addRenderableWidget(this.mergeButton);
         this.addRotationGizmo(diagramX, diagramY);
         this.addForceToggleWidgets(diagramX, diagramY);
         this.addWidget(this.note);
      }
   }

   private void addRotationGizmo(int diagramX, int diagramY) {
      this.turnUpButton = new DiagramButton(
         SimGUITextures.DIAGRAM_ICON_TURN_UP, diagramX + 236, diagramY + 8, Component.empty(), () -> this.rotateDiagram(0, -1)
      );
      this.turnDownButton = new DiagramButton(
         SimGUITextures.DIAGRAM_ICON_TURN_DOWN, diagramX + 236, diagramY + 8 + 14, Component.empty(), () -> this.rotateDiagram(0, 1)
      );
      DiagramButton turnLeftButton = new DiagramButton(
         SimGUITextures.DIAGRAM_ICON_TURN_LEFT, diagramX + 228, diagramY + 12, Component.empty(), () -> this.rotateDiagram(1, 0)
      );
      DiagramButton turnRightButton = new DiagramButton(
         SimGUITextures.DIAGRAM_ICON_TURN_RIGHT, diagramX + 243, diagramY + 12, Component.empty(), () -> this.rotateDiagram(-1, 0)
      );
      this.addRenderableWidget(this.turnUpButton);
      this.addRenderableWidget(this.turnDownButton);
      this.addRenderableWidget(turnLeftButton);
      this.addRenderableWidget(turnRightButton);
   }

   private void rotateDiagram(int yawSteps, int pitchSteps) {
      if (this.config.pitch() > 45.0) {
         yawSteps = -yawSteps;
      }

      this.config.setYaw(this.config.yaw() + (double)((float)yawSteps * 90.0F));
      this.config.setPitch(Mth.clamp(this.config.pitch() + (double)((float)pitchSteps * 90.0F), -90.0, 90.0));
      this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_WORK_CARTOGRAPHER, 1.0F));
      this.updateViewportOrientation();
      this.setConfigDirty();
   }

   private void addForceToggleWidgets(int diagramX, int diagramY) {
      Iterable<ForceGroup> forceGroups = ForceGroups.REGISTRY;
      this.forceToggleWidgets.clear();
      int i = 0;

      for (ForceGroup forceGroup : forceGroups) {
         int yOffset = 11 * (i + 1) - 1;
         int xOffset = -MAX_PAPER_OFFSET - 6;
         DiagramForceGroupToggle widget = new DiagramForceGroupToggle(this, forceGroup, diagramX + xOffset, diagramY + yOffset);
         this.addWidget(widget);
         this.forceToggleWidgets.add(widget);
         i++;
      }
   }

   private HashMap<ResourceLocation, Tuple<Greeble, ArrayList<Greeble.TextureSlice>>> genGreebleSet(RandomSource random) {
      HashMap<ResourceLocation, Tuple<Greeble, ArrayList<Greeble.TextureSlice>>> greebleSet = new HashMap<>();

      for (Entry<ResourceLocation, Greeble> entry : SimResourceManagers.GREEBLE.entrySet()) {
         greebleSet.put(entry.getKey(), new Tuple(entry.getValue(), entry.getValue().shuffled()));
      }

      return greebleSet;
   }

   private ResourceLocation randomGreeble(RandomSource random) {
      float weightSum = 0.0F;

      for (Greeble greeble : SimResourceManagers.GREEBLE.entries()) {
         weightSum += greeble.weight();
      }

      float weight = random.nextFloat() * weightSum;

      for (Entry<ResourceLocation, Greeble> greeble : SimResourceManagers.GREEBLE.entrySet()) {
         weight -= greeble.getValue().weight();
         if (weight <= 0.0F) {
            return greeble.getKey();
         }
      }

      throw new RuntimeException();
   }

   private void addGreebles(int diagramX, int diagramY) {
      RandomSource random = this.subLevel.getLevel().getRandom();
      HashMap<ResourceLocation, Tuple<Greeble, ArrayList<Greeble.TextureSlice>>> greebleSet = this.genGreebleSet(random);
      List<AABB> placed = new ObjectArrayList();
      placed.add(new AABB(0.0, 0.0, 0.0, 26.0, 66.0, 1.0));
      placed.add(new AABB(227.0, 8.0, 0.0, 250.0, 28.0, 1.0));
      int padding = 10;
      int greebles = 8;
      this.finalFbo.bindRead();

      for (int i = 0; i < 8; i++) {
         ResourceLocation greebleID = this.randomGreeble(random);
         Greeble greeble = SimResourceManagers.GREEBLE.get(greebleID);
         ArrayList<Greeble.TextureSlice> slices = (ArrayList<Greeble.TextureSlice>)greebleSet.get(greebleID).getB();
         if (!slices.isEmpty()) {
            Greeble.TextureSlice slice = slices.removeFirst();
            int x = random.nextInt(10, DIAGRAM_TEXTURE.width - slice.width() - 10);
            int y = random.nextInt(10, DIAGRAM_TEXTURE.height - slice.height() - 10);
            AABB box = new AABB((double)x, (double)y, 0.0, (double)(x + slice.width()), (double)(y + slice.height()), 1.0);
            boolean intersects = false;

            for (AABB aabb : placed) {
               if (box.intersects(aabb)) {
                  intersects = true;
                  break;
               }
            }

            if (!intersects && !this.aabbInFramebuffer(box)) {
               placed.add(box);
               this.addRenderableOnly(
                  new DiagramScreen.GreebleRenderable(x + diagramX, y + diagramY, greeble.width(), greeble.height(), greeble.texture(), slice)
               );
            }
         }
      }

      AdvancedFbo.unbind();
   }

   private boolean aabbInFramebuffer(AABB aabb) {
      int minX = (int)aabb.minX;
      int minY = (int)((double)DIAGRAM_TEXTURE.height - aabb.minY);
      int maxX = (int)aabb.maxX;
      int maxY = (int)((double)DIAGRAM_TEXTURE.height - aabb.maxY);
      int width = Math.abs(maxX - minX);
      int height = Math.abs(maxY - minY);
      int length = width * height;
      int[] buffer = new int[length];
      GL11.glReadPixels(minX, minY - height, width, height, 6408, 5121, buffer);

      for (int i = 0; i < length; i++) {
         int color = buffer[i] >> 24;
         if (color != 0) {
            return true;
         }
      }

      return false;
   }

   private void renderContents(SubLevel subLevel, float partialTicks) {
      if (!VeilLevelPerspectiveRenderer.isRenderingPerspective()) {
         Minecraft minecraft = Minecraft.getInstance();
         if (this.renderTime >= 1.6666666F) {
            this.renderTime = 0.0F;
            if (this.fbo != null) {
               float zNear = 0.1F;
               LevelPlot plot = subLevel.getPlot();
               BoundingBox3ic plotBounds = plot.getBoundingBox();
               float radius = (float)(
                  Math.max(Math.max(plotBounds.maxX() - plotBounds.minX(), plotBounds.maxY() - plotBounds.minY()), plotBounds.maxZ() - plotBounds.minZ()) + 1
               );
               radius *= 0.55F;
               radius = Math.max(radius, 2.0F);
               this.viewportRadius = radius;
               Vector3d plotBoundsCenter = new Vector3d(
                  (double)(plotBounds.minX() + plotBounds.maxX() + 1) / 2.0,
                  (double)(plotBounds.minY() + plotBounds.maxY() + 1) / 2.0,
                  (double)(plotBounds.minZ() + plotBounds.maxZ() + 1) / 2.0
               );
               float aspect = (float)DIAGRAM_TEXTURE.width / (float)DIAGRAM_TEXTURE.height;
               PROJECTION_MAT.identity().ortho(-radius * aspect, radius * aspect, -radius, radius, 0.1F, radius * 2.0F);
               LOCAL_CAMERA_POSITION.set(plotBoundsCenter.add(LOCAL_ORIENTATION.transform(new Vector3d(0.0, 0.0, (double)radius))));
               Pose3dc renderPose = ((ClientSubLevel)subLevel).renderPose(partialTicks);
               renderPose.transformPosition(CAMERA_POSITION.set(LOCAL_CAMERA_POSITION));
               draw(
                  subLevel,
                  partialTicks,
                  LOCAL_ORIENTATION,
                  PROJECTION_MAT,
                  CAMERA_POSITION,
                  (float)DIAGRAM_TEXTURE.width,
                  (float)DIAGRAM_TEXTURE.height,
                  this.fbo,
                  this.outlineFbo,
                  this.finalFbo,
                  0.25F,
                  1.0F,
                  3026994,
                  6908261
               );
            }
         } else {
            this.renderTime = this.renderTime + minecraft.getTimer().getRealtimeDeltaTicks();
         }
      }
   }

   public static void draw(
      SubLevel subLevel,
      float partialTicks,
      Quaternionf localOrientation,
      Matrix4f projMatrix,
      Vector3d cameraPos,
      float inWidth,
      float inHeight,
      AdvancedFbo fbo,
      AdvancedFbo outlineFbo,
      AdvancedFbo finalFbo,
      float paletteOffset,
      float fadeScale,
      int lineColor,
      int lineShadowColor
   ) {
      fbo.bind(true);
      fbo.clear();
      Pose3dc renderPose = ((ClientSubLevel)subLevel).renderPose(partialTicks);
      Quaternionf orientation = new Quaternionf(renderPose.orientation()).conjugate();
      orientation.premul(localOrientation.conjugate(new Quaternionf()));
      SimpleSubLevelGroupRenderer.renderChain(subLevel, fbo, new Matrix4f(), projMatrix, cameraPos, orientation, partialTicks);
      PostProcessingManager manager = VeilRenderSystem.renderer().getPostProcessingManager();
      PostPipeline pipeline = manager.getPipeline(Simulated.path("diagram"));
      if (pipeline != null) {
         Color LINE_SHADOW_COLOR = new Color(lineShadowColor);
         Color LINE_COLOR = new Color(lineColor);
         pipeline.getUniformSafe("LineColor")
            .setVector((float)LINE_COLOR.getRed() / 255.0F, (float)LINE_COLOR.getGreen() / 255.0F, (float)LINE_COLOR.getBlue() / 255.0F, 1.0F);
         pipeline.getUniformSafe("LineShadowColor")
            .setVector(
               (float)LINE_SHADOW_COLOR.getRed() / 255.0F, (float)LINE_SHADOW_COLOR.getGreen() / 255.0F, (float)LINE_SHADOW_COLOR.getBlue() / 255.0F, 1.0F
            );
         pipeline.getUniformSafe("InSize").setVector(inWidth, inHeight);
         pipeline.getUniformSafe("PaletteOffset").setFloat(paletteOffset);
         pipeline.getUniformSafe("FadeScale").setFloat(fadeScale);
      }

      Context context = manager.getPostPipelineContext();
      context.setFramebuffer(Simulated.path("diagram"), fbo);
      context.setFramebuffer(Simulated.path("diagram_outlined"), outlineFbo);
      context.setFramebuffer(Simulated.path("diagram_final"), finalFbo);
      manager.runPipeline(pipeline, false);
   }

   protected void renderWindowBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      graphics.fill(0, 0, this.width, this.height, -10, 1342177279);
   }

   public void tick() {
      super.tick();
      if (!this.subLevel.isRemoved() && !this.diagram.isRemoved()) {
         if (this.configDirty) {
            VeilPacketManager.server().sendPacket(new CustomPacketPayload[]{new DiagramSaveConfigPacket(this.diagram.getId(), this.config)});
            this.configDirty = false;
         }

         if (this.ticksWithoutUpdate++ > UPDATE_REQUEST_INTERVAL) {
            this.ticksWithoutUpdate = 0;
            VeilPacketManager.server().sendPacket(new CustomPacketPayload[]{new RequestDiagramDataPacket(this.subLevel.getUniqueId())});
         }

         this.lastPaperOffset = this.paperOffset;
         this.paperOffset = Mth.lerp(0.4F, this.paperOffset, this.paperVisible ? (float)MAX_PAPER_OFFSET : 10.0F);
         this.lastTabOffset = this.tabOffset;
         this.tabOffset = Mth.lerp(this.paperVisible ? 0.4F : 0.1F, this.tabOffset, this.paperVisible ? 1.0F : 0.0F);
         this.note.tick();
      } else {
         this.onClose();
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      boolean widgetPress = super.mouseClicked(mouseX, mouseY, button);
      boolean withinNote = this.note.contains(mouseX, mouseY);
      if (withinNote || !widgetPress && this.contains(mouseX, mouseY)) {
         MAGNIFYING_CENTER.set(mouseX, mouseY);
      }

      return widgetPress;
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      boolean parent = super.mouseReleased(mouseX, mouseY, button);
      this.updateNote(mouseX, mouseY, parent);
      MAGNIFYING_CENTER.set(0.0, 0.0);
      MAGNIFYING_MAX.set(0.0, 0.0);
      return parent;
   }

   private void updateNote(double mouseX, double mouseY, boolean widgetRelease) {
      if (!(MAGNIFYING_CENTER.distanceSquared(MAGNIFYING_MAX) < 9.0)) {
         this.updateMagnificationBox(mouseX, mouseY);
         if (this.note.contains(MAGNIFYING_CENTER.x, MAGNIFYING_CENTER.y)) {
            if (this.pointsWithinNote(MAGNIFYING_MAX, MAGNIFYING_MIN)) {
               this.note.handleInternalUpdate(MAGNIFYING_MAX, MAGNIFYING_MIN);
               this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_WORK_CARTOGRAPHER, 1.0F));
            }
         } else if (this.pointsWithinDiagram(MAGNIFYING_MAX, MAGNIFYING_MIN) && !widgetRelease) {
            int diagramX = this.width / 2 - DIAGRAM_TEXTURE.width / 2;
            int diagramY = this.height / 2 - DIAGRAM_TEXTURE.height / 2;
            this.config.getNoteConfigs().setNoteYaw(this.config.yaw());
            this.config.getNoteConfigs().setNotePitch(this.config.pitch());
            this.config.getNoteConfigs().setActive(true);
            this.note
               .updateCurrentScope(
                  MAGNIFYING_MAX.sub((double)diagramX, (double)diagramY, new Vector2d()),
                  MAGNIFYING_MIN.sub((double)diagramX, (double)diagramY, new Vector2d()),
                  LOCAL_CAMERA_POSITION,
                  PROJECTION_MAT
               );
            this.note.activate();
            this.setConfigDirty();
            this.magnifying = false;
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_WORK_CARTOGRAPHER, 1.0F));
         }
      }
   }

   private void updateMagnificationBox(double mouseX, double mouseY) {
      MAGNIFYING_MAX.set(mouseX, mouseY);
      MAGNIFYING_MAX.sub(MAGNIFYING_CENTER);
      MAGNIFYING_MAX.absolute();
      double max = Math.max(MAGNIFYING_MAX.x, MAGNIFYING_MAX.y);
      MAGNIFYING_MAX.set(max, max);
      MAGNIFYING_MAX.negate(MAGNIFYING_MIN).add(MAGNIFYING_CENTER);
      MAGNIFYING_MAX.add(MAGNIFYING_CENTER);
   }

   protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      PoseStack ps = graphics.pose();
      if (!this.subLevel.isRemoved() && !this.diagram.isRemoved()) {
         this.note.renderWidget(graphics, mouseX, mouseY, partialTicks);
         this.renderContents(this.subLevel, partialTicks);
         if (this.turnDownButton != null && this.turnUpButton != null) {
            this.turnDownButton.visible = this.turnDownButton.active = this.config.pitch() < 45.0;
            this.turnUpButton.visible = this.turnUpButton.active = this.config.pitch() > -45.0;
         }

         ps.pushPose();

         for (DiagramForceGroupToggle widget : this.forceToggleWidgets) {
            widget.active = this.paperVisible;
            widget.updateForceState(this.serverData);
            widget.renderTab(graphics, mouseX, mouseY, partialTicks);
         }

         int diagramX = this.width / 2 - DIAGRAM_TEXTURE.width / 2;
         int diagramY = this.height / 2 - DIAGRAM_TEXTURE.height / 2;
         ps.pushPose();
         ps.translate((float)diagramX, (float)diagramY, 0.0F);
         ps.translate(-this.getPaperOffset(partialTicks), 0.0F, 0.0F);
         SimGUITextures.DIAGRAM_PAPER.render(graphics, 0, 0);
         ps.popPose();

         for (DiagramForceGroupToggle widget : this.forceToggleWidgets) {
            widget.render(graphics, mouseX, mouseY, partialTicks);
         }

         ps.translate((float)diagramX, (float)diagramY, 0.0F);
         DIAGRAM_TEXTURE.render(graphics, 0, 0);
         renderFBO(graphics, this.finalFbo, DIAGRAM_TEXTURE.width, DIAGRAM_TEXTURE.height);
         String text = this.subLevel.getName();
         ps.pushPose();
         ps.translate(0.0F, 0.0F, 1.0F);
         if (text != null && !text.isEmpty()) {
            int footerW = this.font.width(text);
            graphics.fill(
               DIAGRAM_TEXTURE.width - footerW - 7, DIAGRAM_TEXTURE.height - 5 - 9, DIAGRAM_TEXTURE.width - 4, DIAGRAM_TEXTURE.height - 3, BG_COLOR.getRGB()
            );
            graphics.drawString(this.font, text, DIAGRAM_TEXTURE.width - footerW - 5, DIAGRAM_TEXTURE.height - 3 - 9, TEXT_COLOR.getRGB(), false);
         }

         ps.popPose();
         this.renderArrows(
            graphics,
            mouseX,
            mouseY,
            diagramX,
            diagramY,
            LOCAL_ORIENTATION,
            LOCAL_CAMERA_POSITION,
            PROJECTION_MAT,
            DIAGRAM_TEXTURE.width,
            DIAGRAM_TEXTURE.height
         );
         if (this.config.displayCenterOfMass()) {
            this.renderCenterOfMass(graphics);
         }

         ps.popPose();
      } else {
         this.onClose();
      }
   }

   protected void renderWindowForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      PoseStack ps = graphics.pose();
      this.renderMagnificationHighlight(graphics, mouseX, mouseY, ps);
      if (!this.tooltipList.isEmpty()) {
         renderTooltip(graphics, mouseX, mouseY, this.tooltipList);
      }

      this.tooltipList.clear();
      super.renderWindowForeground(graphics, mouseX, mouseY, partialTicks);
   }

   private void renderMagnificationHighlight(GuiGraphics graphics, int mouseX, int mouseY, PoseStack ps) {
      boolean initiallyWithinNote = this.note.contains(MAGNIFYING_CENTER.x, MAGNIFYING_CENTER.y);
      this.updateMagnificationBox((double)mouseX, (double)mouseY);
      if (!(MAGNIFYING_CENTER.distanceSquared(MAGNIFYING_MAX) < 9.0)) {
         if (initiallyWithinNote || this.contains(MAGNIFYING_CENTER.x, MAGNIFYING_CENTER.y)) {
            ps.pushPose();
            ps.translate(0.0F, 0.0F, 1.0F);
            Vector2d min = new Vector2d(MAGNIFYING_MIN);
            Vector2d max = new Vector2d(MAGNIFYING_MAX);
            boolean valid = initiallyWithinNote
               ? this.note.contains(min.x, min.y) && this.note.contains(max.x, max.y)
               : this.contains(min.x, min.y) && this.contains(max.x, max.y);
            if (initiallyWithinNote) {
               this.note.clamp(min);
               this.note.clamp(max);
            } else {
               this.clamp(min);
               this.clamp(max);
            }

            double startX = min.x;
            double startY = min.y;
            double endX = max.x;
            double endY = max.y;
            int fillColor = valid ? 1090518268 : 1084926634;
            int color = valid ? -1862270977 : -1862292822;
            graphics.fill((int)startX, (int)startY, (int)endX, (int)endY, fillColor);
            graphics.hLine((int)startX, (int)endX, (int)startY, color);
            graphics.hLine((int)startX, (int)endX, (int)endY, color);
            graphics.vLine((int)startX, (int)startY, (int)endY, color);
            graphics.vLine((int)endX, (int)startY, (int)endY, color);
            ps.popPose();
         }
      }
   }

   public boolean pointsWithinNote(Vector2d target, Vector2d inverse) {
      return this.note.contains(target.x, target.y) && this.note.contains(inverse.x, inverse.y);
   }

   public boolean pointsWithinDiagram(Vector2d target, Vector2d inverse) {
      return this.contains(target.x, target.y) && this.contains(inverse.x, inverse.y);
   }

   public boolean contains(double x, double y) {
      x -= (double)((float)this.width / 2.0F - (float)DIAGRAM_TEXTURE.width / 2.0F);
      y -= (double)((float)this.height / 2.0F - (float)DIAGRAM_TEXTURE.height / 2.0F);
      return x > 0.0 && x < (double)DIAGRAM_TEXTURE.width && y > 0.0 && y < (double)DIAGRAM_TEXTURE.height;
   }

   public Vector2d clamp(Vector2d dest) {
      float minX = (float)this.width / 2.0F - (float)DIAGRAM_TEXTURE.width / 2.0F;
      float minY = (float)this.height / 2.0F - (float)DIAGRAM_TEXTURE.height / 2.0F;
      dest.max(new Vector2d((double)minX, (double)minY));
      dest.min(new Vector2d((double)(minX + (float)DIAGRAM_TEXTURE.width - 1.0F), (double)(minY + (float)DIAGRAM_TEXTURE.height - 1.0F)));
      return dest;
   }

   public static void renderFBO(GuiGraphics graphics, AdvancedFbo fbo, int width, int height) {
      int id = fbo.getColorTextureAttachment(0).getId();
      RenderSystem.setShaderTexture(0, id);
      RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
      RenderSystem.enableBlend();
      Matrix4f matrix4f = graphics.pose().last().pose();
      BufferBuilder bufferbuilder = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      float x1 = 0.0F;
      float y1 = 0.0F;
      bufferbuilder.addVertex(matrix4f, 0.0F, 0.0F, 0.0F).setUv(0.0F, 1.0F).setColor(-1);
      bufferbuilder.addVertex(matrix4f, 0.0F, (float)height, 0.0F).setUv(0.0F, 0.0F).setColor(-1);
      bufferbuilder.addVertex(matrix4f, (float)width, (float)height, 0.0F).setUv(1.0F, 0.0F).setColor(-1);
      bufferbuilder.addVertex(matrix4f, (float)width, 0.0F, 0.0F).setUv(1.0F, 1.0F).setColor(-1);
      BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
      RenderSystem.disableBlend();
   }

   public void renderArrows(
      GuiGraphics graphics,
      int mouseX,
      int mouseY,
      int areaOriginX,
      int areaOriginY,
      Quaternionfc orientation,
      Vector3dc cameraPos,
      Matrix4fc projMatrix,
      int areaWidth,
      int areaHeight
   ) {
      if (this.serverData != null) {
         double maxArrowLengthSquared = 0.0;
         Map<ForceGroup, List<ForceClusterFinder.Cluster>> clusters = new HashMap<>();

         for (ResourceLocation groupId : this.config.enabledForceGroups()) {
            ForceGroup group = (ForceGroup)ForceGroups.REGISTRY.get(groupId);

            assert group != null;

            List<PointForce> forces = this.serverData.forces().get(group);
            if (forces != null) {
               List<ForceClusterFinder.Cluster> cluster = this.config.mergeForces()
                  ? ForceClusterFinder.getMergedClusters(forces)
                  : ForceClusterFinder.passThrough(forces);
               clusters.put(group, cluster);

               for (ForceClusterFinder.Cluster force : cluster) {
                  maxArrowLengthSquared = Math.max(maxArrowLengthSquared, force.force().lengthSquared());
               }
            }
         }

         for (ResourceLocation groupId : this.config.enabledForceGroups()) {
            ForceGroup groupx = (ForceGroup)ForceGroups.REGISTRY.get(groupId);

            assert groupx != null;

            List<ForceClusterFinder.Cluster> cluster = clusters.get(groupx);
            if (cluster != null) {
               for (ForceClusterFinder.Cluster force : cluster) {
                  this.renderForceArrow(
                     graphics,
                     groupx,
                     force,
                     Math.sqrt(maxArrowLengthSquared),
                     mouseX - areaOriginX,
                     mouseY - areaOriginY,
                     this.tooltipList,
                     orientation,
                     cameraPos,
                     projMatrix,
                     areaWidth,
                     areaHeight
                  );
               }
            }
         }
      }
   }

   private void renderForceArrow(
      GuiGraphics graphics,
      ForceGroup forceGroup,
      ForceClusterFinder.Cluster pointForce,
      double maxArrowLength,
      int mouseX,
      int mouseY,
      List<FormattedText> tooltipLines,
      Quaternionfc orientation,
      Vector3dc cameraPos,
      Matrix4fc projMatrix,
      int areaWidth,
      int areaHeight
   ) {
      double forceMagnitude = pointForce.force().length();
      if (!(forceMagnitude <= 0.01) && (double)this.viewportRadius != 0.0) {
         Vector3d globalFirstDir = pointForce.force().normalize(new Vector3d());
         Vector3d forceOffset = globalFirstDir.mul(Math.max(0.25, forceMagnitude / maxArrowLength) * (double)this.viewportRadius * 0.5, new Vector3d());
         Vector2d originCoords = getScreenCoords(new Vector3d(pointForce.pos()), orientation, cameraPos, projMatrix, areaWidth, areaHeight);
         if (this.canDrawArrowAt((int)originCoords.x, (int)originCoords.y, areaWidth, areaHeight)) {
            Vector2d mousePos = new Vector2d((double)mouseX, (double)mouseY);
            int color = 0xFF000000 | forceGroup.color();
            int shadowColor = -396578;
            double facingDot = orientation.transformInverse(globalFirstDir, new Vector3d()).dot(OrientedBoundingBox3d.FORWARD);
            if (Math.abs(facingDot) > 0.85) {
               PoseStack ps = graphics.pose();
               ps.pushPose();
               ps.translate(0.0, 0.0, 1.0);
               if (mousePos.sub(originCoords, new Vector2d()).lengthSquared() < 64.0) {
                  addForceArrowTooltip(forceGroup, pointForce.groupSize().getValue(), forceMagnitude, color, tooltipLines);
               }

               if (facingDot < 0.0) {
                  SimGUITextures.DIAGRAM_ICON_ARROW_IN_PAGE_SHADOW.render(graphics, (int)originCoords.x - 8, (int)originCoords.y - 8, new Color(-396578));
                  SimGUITextures.DIAGRAM_ICON_ARROW_IN_PAGE.render(graphics, (int)originCoords.x - 8, (int)originCoords.y - 8, new Color(color));
               } else {
                  SimGUITextures.DIAGRAM_ICON_ARROW_OUT_PAGE_SHADOW.render(graphics, (int)originCoords.x - 8, (int)originCoords.y - 8, new Color(-396578));
                  SimGUITextures.DIAGRAM_ICON_ARROW_OUT_PAGE.render(graphics, (int)originCoords.x - 8, (int)originCoords.y - 8, new Color(color));
               }

               ps.popPose();
            } else {
               Vector2d resultCoords = getScreenCoords(
                  pointForce.pos().add(forceOffset, new Vector3d()), orientation, cameraPos, projMatrix, areaWidth, areaHeight
               );
               Vector2d arrowDir = resultCoords.sub(originCoords, new Vector2d());
               float arrowLength = (float)arrowDir.length();
               arrowDir.div((double)arrowLength);

               while (arrowLength > 0.0F && !this.canDrawArrowAt((int)resultCoords.x, (int)resultCoords.y, areaWidth, areaHeight)) {
                  resultCoords.fma(-3.0, arrowDir);
                  arrowLength -= 3.0F;
               }

               int x1 = (int)originCoords.x();
               int y1 = (int)originCoords.y();
               int x2 = (int)resultCoords.x();
               int y2 = (int)resultCoords.y();
               BufferSource bufferSource = graphics.bufferSource();
               VertexConsumer builder = bufferSource.getBuffer(RenderType.gui());
               Matrix4f pose = graphics.pose().last().pose();
               Vector2d arrowLeft = new Vector2d(-arrowDir.y(), arrowDir.x()).mul(4.0);
               Vector2d arrowRight = new Vector2d(arrowDir.y(), -arrowDir.x()).mul(4.0);
               float headLen = 6.0F;
               boolean drawArrow = originCoords.distanceSquared(resultCoords) > 36.0;
               double distanceAlongLine = mousePos.sub(originCoords, new Vector2d()).dot(arrowDir);
               distanceAlongLine = Mth.clamp(distanceAlongLine, 0.0, (double)arrowLength);
               boolean displayTooltip = new Vector2d(originCoords).fma(distanceAlongLine, arrowDir).distance(mousePos) < 5.0;
               if (displayTooltip) {
                  addForceArrowTooltip(forceGroup, pointForce.groupSize().getValue(), forceMagnitude, color, tooltipLines);
               }

               int z = 1;
               int inflation = 3;
               builder.addVertex(pose, (float)x1 - (float)inflation, (float)y1 - (float)inflation, 1.0F).setColor(-396578);
               builder.addVertex(pose, (float)x1 - (float)inflation, (float)y1 + 1.0F + (float)inflation, 1.0F).setColor(-396578);
               builder.addVertex(pose, (float)x1 + 1.0F + (float)inflation, (float)y1 + 1.0F + (float)inflation, 1.0F).setColor(-396578);
               builder.addVertex(pose, (float)x1 + 1.0F + (float)inflation, (float)y1 - (float)inflation, 1.0F).setColor(-396578);
               if (drawArrow) {
                  drawLine(
                     builder, pose, x2, y2, (int)((double)x2 - arrowDir.x * 6.0 + arrowLeft.x), (int)((double)y2 - arrowDir.y * 6.0 + arrowLeft.y), -396578, 1
                  );
                  drawLine(
                     builder,
                     pose,
                     x2,
                     y2,
                     (int)((double)x2 - arrowDir.x * 6.0 + arrowRight.x),
                     (int)((double)y2 - arrowDir.y * 6.0 + arrowRight.y),
                     -396578,
                     1
                  );
                  drawLine(builder, pose, x1, y1, x2, y2, -396578, 1);
                  drawLine(
                     builder, pose, x2, y2, (int)((double)x2 - arrowDir.x * 6.0 + arrowLeft.x), (int)((double)y2 - arrowDir.y * 6.0 + arrowLeft.y), color, 0
                  );
                  drawLine(
                     builder, pose, x2, y2, (int)((double)x2 - arrowDir.x * 6.0 + arrowRight.x), (int)((double)y2 - arrowDir.y * 6.0 + arrowRight.y), color, 0
                  );
                  drawLine(builder, pose, x1, y1, x2, y2, color, 0);
               }

               int var45 = 2;
               builder.addVertex(pose, (float)x1 - (float)var45, (float)y1 - (float)var45, 1.0F).setColor(color);
               builder.addVertex(pose, (float)x1 - (float)var45, (float)y1 + 1.0F + (float)var45, 1.0F).setColor(color);
               builder.addVertex(pose, (float)x1 + 1.0F + (float)var45, (float)y1 + 1.0F + (float)var45, 1.0F).setColor(color);
               builder.addVertex(pose, (float)x1 + 1.0F + (float)var45, (float)y1 - (float)var45, 1.0F).setColor(color);
            }
         }
      }
   }

   private static void addForceArrowTooltip(ForceGroup forceGroup, int forceCount, double forceMagnitude, int color, List<FormattedText> tooltipLines) {
      LangBuilder forceNameText = SimLang.builder().add(forceGroup.name()).color(color);
      LangBuilder forceMagnitudeText = SimLang.translate("contraption_diagram.force_arrow_magnitude", String.format("%,.2f", forceMagnitude)).color(-1);
      if (forceCount > 1) {
         tooltipLines.add(
            SimLang.translate(
                  "contraption_diagram.merged_force_arrow",
                  SimLang.translate("contraption_diagram.merging_numeral", Integer.toString(forceCount)).color(-1),
                  forceNameText,
                  forceMagnitudeText
               )
               .color(-4025475)
               .component()
         );
      } else {
         tooltipLines.add(SimLang.translate("contraption_diagram.force_arrow", forceNameText, forceMagnitudeText).color(-4025475).component());
      }
   }

   private boolean canDrawArrowAt(int x, int y, int width, int height) {
      int padding = 8;
      return x >= 8 && x < width - 8 && y >= 8 && y < height - 8;
   }

   private static void drawLine(VertexConsumer builder, Matrix4f pose, int x1, int y1, int x2, int y2, int color, int inflation) {
      int z = 1;
      int dx = Math.abs(x2 - x1);
      int dy = Math.abs(y2 - y1);
      int sx = x1 < x2 ? 1 : -1;
      int sy = y1 < y2 ? 1 : -1;
      int err = dx - dy;

      while (true) {
         builder.addVertex(pose, (float)x1 - (float)inflation, (float)y1 - (float)inflation, 1.0F).setColor(color);
         builder.addVertex(pose, (float)x1 - (float)inflation, (float)y1 + 1.0F + (float)inflation, 1.0F).setColor(color);
         builder.addVertex(pose, (float)x1 + 1.0F + (float)inflation, (float)y1 + 1.0F + (float)inflation, 1.0F).setColor(color);
         builder.addVertex(pose, (float)x1 + 1.0F + (float)inflation, (float)y1 - (float)inflation, 1.0F).setColor(color);
         if (x1 == x2 && y1 == y2) {
            return;
         }

         int e2 = 2 * err;
         if (e2 > -dy) {
            err -= dy;
            x1 += sx;
         }

         if (e2 < dx) {
            err += dx;
            y1 += sy;
         }
      }
   }

   private SimGUITextures getMergeIcon() {
      return this.config.mergeForces() ? SimGUITextures.DIAGRAM_ICON_FORCES_MERGED : SimGUITextures.DIAGRAM_ICON_FORCES_SEPARATED;
   }

   public float getPaperOffset(float partialTicks) {
      return Mth.lerp(partialTicks, this.lastPaperOffset, this.paperOffset);
   }

   public float getTabOffset(float partialTicks) {
      return Mth.lerp(partialTicks, this.lastTabOffset, this.tabOffset);
   }

   private void renderCenterOfMass(GuiGraphics graphics) {
      Vector3d centerOfMass = new Vector3d(this.subLevel.logicalPose().rotationPoint());
      Vector2d screenCoords = getScreenCoords(
         centerOfMass, LOCAL_ORIENTATION, LOCAL_CAMERA_POSITION, PROJECTION_MAT, DIAGRAM_TEXTURE.width, DIAGRAM_TEXTURE.height
      );
      SimGUITextures tex = SimGUITextures.DIAGRAM_ICON_COM;
      PoseStack pose = graphics.pose();
      pose.pushPose();
      pose.translate(screenCoords.x - 8.0, screenCoords.y - 8.0, 0.0);
      graphics.blit(tex.location, 0, 0, 5, (float)tex.startX, (float)tex.startY, tex.width, tex.height, tex.texWidth, tex.texHeight);
      pose.popPose();
   }

   public static Vector2d getScreenCoords(
      Vector3d plotSpacePoint, Quaternionfc orientation, Vector3dc localPosition, Matrix4fc projMatrix, int width, int height
   ) {
      plotSpacePoint.sub(localPosition);
      orientation.transformInverse(plotSpacePoint);
      Vector4f clipSpace = new Vector4f((float)plotSpacePoint.x, (float)plotSpacePoint.y, (float)plotSpacePoint.z, 1.0F);
      clipSpace.mul(projMatrix);
      clipSpace.div(clipSpace.w);
      double projectedX = (double)((clipSpace.x() * 0.5F + 0.5F) * (float)width);
      double projectedY = (double)((-clipSpace.y() * 0.5F + 0.5F) * (float)height);
      return new Vector2d(projectedX, projectedY);
   }

   public static Vector3d getPlotCoords(
      Vector2dc diagramSpacePoint, Quaternionfc orientation, Vector3dc localPosition, Matrix4fc projMatrix, int width, int height
   ) {
      Vector3d clipSpace = new Vector3d(2.0 * diagramSpacePoint.x() / (double)width - 1.0, 1.0 - 2.0 * diagramSpacePoint.y() / (double)height, 0.0);
      Vector3d point = clipSpace.sub(projMatrix.getTranslation(new Vector3f()))
         .div((double)projMatrix.m00(), (double)projMatrix.m11(), (double)projMatrix.m22());
      orientation.transform(point);
      point.add(localPosition);
      return point;
   }

   public void updateData(DiagramDataPacket data) {
      this.serverData = data;
   }

   public static void renderTooltip(GuiGraphics guiGraphics, int x, int y, List<FormattedText> lines) {
      Font font = Minecraft.getInstance().font;
      Color colorBackground = new Color(-12766678);
      Color colorBorderTop = new Color(-10663878);
      Color colorBorderBot = new Color(-10663878);
      RemovedGuiUtils.drawHoveringText(
         guiGraphics,
         lines,
         x,
         y,
         guiGraphics.guiWidth(),
         guiGraphics.guiHeight(),
         -1,
         colorBackground.getRGB(),
         colorBorderTop.getRGB(),
         colorBorderBot.getRGB(),
         font
      );
   }

   public void setConfigDirty() {
      this.configDirty = true;
   }

   public static record GreebleRenderable(int x, int y, int width, int height, ResourceLocation texture, Greeble.TextureSlice slice) implements Renderable {
      public void render(GuiGraphics guiGraphics, int i, int i1, float v) {
         guiGraphics.blit(
            this.texture, this.x, this.y, (float)this.slice.x(), (float)this.slice.y(), this.slice.width(), this.slice.height(), this.width, this.height
         );
      }
   }
}

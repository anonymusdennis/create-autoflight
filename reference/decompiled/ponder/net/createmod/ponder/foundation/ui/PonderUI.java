package net.createmod.ponder.foundation.ui;

import com.google.common.graph.ElementOrder;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.mojang.blaze3d.platform.ClipboardManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.gui.NavigatableSimiScreen;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.BoxElement;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.gui.element.RenderElement;
import net.createmod.catnip.gui.widget.BoxWidget;
import net.createmod.catnip.gui.widget.ElementWidget;
import net.createmod.catnip.lang.ClientFontHelper;
import net.createmod.catnip.math.Pointing;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.createmod.catnip.theme.Color;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.registration.StoryBoardEntry;
import net.createmod.ponder.enums.PonderConfig;
import net.createmod.ponder.enums.PonderGuiTextures;
import net.createmod.ponder.foundation.PonderChapter;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.PonderStoryBoardEntry;
import net.createmod.ponder.foundation.PonderTag;
import net.createmod.ponder.foundation.content.DebugScenes;
import net.createmod.ponder.foundation.element.TextWindowElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class PonderUI extends AbstractPonderScreen {
   public static int ponderTicks;
   public static float ponderPartialTicksPaused;
   public static final Color BACKGROUND_TRANSPARENT = new Color(-587202560, true);
   public static final Color BACKGROUND_FLAT = new Color(-16777216, true);
   public static final Color BACKGROUND_IMPORTANT = new Color(-586281440, true);
   public static final Couple<Color> COLOR_IDLE = Couple.create(new Color(1090514653, true), new Color(553643741, true)).map(Color::setImmutable);
   public static final Couple<Color> COLOR_HOVER = Couple.create(new Color(1895825407, true), new Color(822083583, true)).map(Color::setImmutable);
   public static final Couple<Color> COLOR_HIGHLIGHT = Couple.create(new Color(-251662627, true), new Color(1627385565, true)).map(Color::setImmutable);
   public static final Couple<Color> MISSING_VANILLA_ENTRY = Couple.create(new Color(1347420415, true), new Color(1344798847, true)).map(Color::setImmutable);
   public static final Couple<Color> MISSING_MODDED_ENTRY = Couple.create(new Color(1889027328, true), new Color(1885938688, true)).map(Color::setImmutable);
   private static final Vector3f DIFFUSE_LIGHT_0 = new Vector3f(0.4F, -1.0F, 0.7F).normalize();
   private static final Vector3f DIFFUSE_LIGHT_1 = new Vector3f(-0.4F, -0.5F, 0.7F).normalize();
   private final List<PonderScene> scenes;
   private final List<PonderTag> tags;
   private List<PonderButton> tagButtons = new ArrayList<>();
   private List<LerpedFloat> tagFades = new ArrayList<>();
   private final LerpedFloat fadeIn;
   ItemStack stack;
   @Nullable
   PonderChapter chapter = null;
   private boolean userViewMode;
   private boolean identifyMode;
   private ItemStack hoveredTooltipItem = ItemStack.EMPTY;
   @Nullable
   private BlockPos hoveredBlockPos;
   private final ClipboardManager clipboardHelper;
   @Nullable
   private BlockPos copiedBlockPos;
   private final LerpedFloat finishingFlash;
   private final LerpedFloat nextUp;
   private int finishingFlashWarmup = 0;
   private int nextUpWarmup = 0;
   private final LerpedFloat lazyIndex;
   private int index = 0;
   @Nullable
   private PonderTag referredToByTag;
   private PonderButton left;
   private PonderButton right;
   private PonderButton scan;
   private PonderButton chap;
   private PonderButton userMode;
   private PonderButton close;
   private PonderButton replay;
   private PonderButton slowMode;
   private int skipCooling = 0;
   private int extendedTickLength = 0;
   private int extendedTickTimer = 0;

   public static PonderUI of(ResourceLocation id) {
      return new PonderUI(PonderIndex.getSceneAccess().compile(id));
   }

   public static PonderUI of(ItemStack item) {
      return new PonderUI(PonderIndex.getSceneAccess().compile(RegisteredObjectsHelper.getKeyOrThrow(item.getItem())));
   }

   public static PonderUI of(ItemStack item, PonderTag tag) {
      PonderUI ponderUI = new PonderUI(PonderIndex.getSceneAccess().compile(RegisteredObjectsHelper.getKeyOrThrow(item.getItem())));
      ponderUI.referredToByTag = tag;
      return ponderUI;
   }

   protected PonderUI(List<PonderScene> scenes) {
      ResourceLocation location = scenes.get(0).getLocation();
      this.stack = new ItemStack(RegisteredObjectsHelper.getItemOrBlock(location));
      this.tags = new ArrayList<>(PonderIndex.getTagAccess().getTags(location));
      Ponder.LOGGER.debug("Ponder Scenes before ordering: {}", Arrays.toString(scenes.stream().map(PonderScene::getId).toArray()));

      List<PonderScene> orderedScenes;
      try {
         orderedScenes = this.orderScenes(scenes);
         Ponder.LOGGER.debug("Ponder Scenes after ordering: {}", Arrays.toString(orderedScenes.stream().map(PonderScene::getId).toArray()));
      } catch (Exception var5) {
         Ponder.LOGGER.warn("Unable to sort PonderScenes, using unordered List", var5);
         orderedScenes = scenes;
      }

      this.scenes = orderedScenes;
      if (this.scenes.isEmpty()) {
         List<StoryBoardEntry> list = Collections.singletonList(
            new PonderStoryBoardEntry(DebugScenes::empty, "ponder", "debug/scene_1", ResourceLocation.withDefaultNamespace("stick"))
         );
         this.scenes.addAll(PonderIndex.getSceneAccess().compile(list));
      }

      this.lazyIndex = LerpedFloat.linear().startWithValue((double)this.index);
      this.fadeIn = LerpedFloat.linear().startWithValue(0.0).chase(1.0, 0.1F, LerpedFloat.Chaser.EXP);
      this.clipboardHelper = new ClipboardManager();
      this.finishingFlash = LerpedFloat.linear().startWithValue(0.0).chase(0.0, 0.1F, LerpedFloat.Chaser.EXP);
      this.nextUp = LerpedFloat.linear().startWithValue(0.0).chase(0.0, 0.4F, LerpedFloat.Chaser.EXP);
   }

   private List<PonderScene> orderScenes(List<PonderScene> scenes) {
      Map<Boolean, List<PonderScene>> partitioned = scenes.stream().collect(Collectors.partitioningBy(scene -> scene.getOrderingEntries().isEmpty()));
      List<PonderScene> scenesWithOrdering = partitioned.get(false);
      List<PonderScene> scenesWithoutOrdering = partitioned.get(true);
      if (scenesWithOrdering.isEmpty()) {
         return scenes;
      } else {
         List<PonderScene> sceneList = new ArrayList<>(scenes);
         Collections.reverse(sceneList);
         Map<ResourceLocation, PonderScene> sceneLookup = scenes.stream().collect(Collectors.toMap(PonderScene::getId, scene -> (PonderScene)scene));
         MutableGraph<PonderScene> graph = GraphBuilder.directed().nodeOrder(ElementOrder.insertion()).build();
         sceneList.forEach(graph::addNode);
         IntStream.range(1, scenesWithoutOrdering.size()).forEach(i -> graph.putEdge(scenesWithoutOrdering.get(i - 1), scenesWithoutOrdering.get(i)));
         scenesWithOrdering.forEach(
            scene -> {
               List<StoryBoardEntry.SceneOrderingEntry> relevantOrderings = scene.getOrderingEntries()
                  .stream()
                  .filter(entry -> scenes.stream().anyMatch(sc -> sc.getId().equals(entry.sceneId())))
                  .toList();
               if (!relevantOrderings.isEmpty()) {
                  relevantOrderings.forEach(entry -> {
                     PonderScene otherScene = sceneLookup.get(entry.sceneId());
                     if (entry.type() == StoryBoardEntry.SceneOrderingType.BEFORE) {
                        graph.putEdge(scene, otherScene);
                     } else if (entry.type() == StoryBoardEntry.SceneOrderingType.AFTER) {
                        graph.putEdge(otherScene, scene);
                     }
                  });
               }
            }
         );
         return topologicalSort(graph);
      }
   }

   private static List<PonderScene> topologicalSort(MutableGraph<PonderScene> graph) {
      List<PonderScene> result = new ArrayList<>();
      Set<PonderScene> visited = new HashSet<>();
      Set<PonderScene> currentlyVisiting = new HashSet<>();

      for (PonderScene node : graph.nodes()) {
         if (!visited.contains(node) && !dfs(node, graph, visited, currentlyVisiting, result)) {
            throw new IllegalArgumentException("Graph has a cycle!");
         }
      }

      Collections.reverse(result);
      return result;
   }

   private static boolean dfs(
      PonderScene node, MutableGraph<PonderScene> graph, Set<PonderScene> visited, Set<PonderScene> currentlyVisiting, List<PonderScene> result
   ) {
      if (currentlyVisiting.contains(node)) {
         return false;
      } else {
         if (!visited.contains(node)) {
            currentlyVisiting.add(node);

            for (PonderScene neighbor : graph.successors(node)) {
               if (!dfs(neighbor, graph, visited, currentlyVisiting, result)) {
                  return false;
               }
            }

            currentlyVisiting.remove(node);
            visited.add(node);
            result.add(node);
         }

         return true;
      }
   }

   @Override
   protected void init() {
      super.init();
      this.tagButtons = new ArrayList<>();
      this.tagFades = new ArrayList<>();
      this.tags.forEach(t -> {
         int i = this.tagButtons.size();
         int x = 31;
         int y = 81 + i * 30;
         PonderButton b2 = new PonderButton(x, y).<ElementWidget>showing(t).withCallback((mX, mY) -> {
            this.centerScalingOn(mX, mY);
            ScreenOpener.transitionTo(new PonderTagScreen(t));
         });
         this.addRenderableWidget(b2);
         this.tagButtons.add(b2);
         LerpedFloat chase = LerpedFloat.linear().startWithValue(0.0).chase(0.0, 0.05F, LerpedFloat.Chaser.exp(0.1));
         this.tagFades.add(chase);
      });
      Options bindings = this.minecraft.options;
      int spacing = 8;
      int bX = (this.width - 20) / 2 - (70 + 2 * spacing);
      int bY = this.height - 20 - 31;
      int pX = this.width / 2 - 110;
      int pY = bY + 20 + 4;
      int pW = this.width - 2 * pX;
      this.addRenderableWidget(new PonderProgressBar(this, pX, pY, pW, 1));
      this.addRenderableWidget(
         this.scan = new PonderButton(bX, bY)
            .<PonderButton>withShortcut(bindings.keyDrop)
            .<ElementWidget>showing(PonderGuiTextures.ICON_PONDER_IDENTIFY)
            .<ElementWidget>enableFade(0, 5)
            .withCallback(() -> {
               this.identifyMode = !this.identifyMode;
               if (!this.identifyMode) {
                  this.scenes.get(this.index).deselect();
               } else {
                  ponderPartialTicksPaused = AnimationTickHolder.getPartialTicksUI();
               }
            })
      );
      this.scan.atZLevel(600.0F);
      this.addRenderableWidget(
         this.slowMode = new PonderButton(this.width - 20 - 31, bY)
            .<ElementWidget>showing(PonderGuiTextures.ICON_PONDER_SLOW_MODE)
            .<ElementWidget>enableFade(0, 5)
            .withCallback(() -> this.setComfyReadingEnabled(!this.isComfyReadingEnabled()))
      );
      if (PonderIndex.editingModeActive()) {
         this.addRenderableWidget(
            this.userMode = new PonderButton(this.width - 50 - 31, bY)
               .<ElementWidget>showing(PonderGuiTextures.ICON_PONDER_USER_MODE)
               .<ElementWidget>enableFade(0, 5)
               .withCallback(() -> this.userViewMode = !this.userViewMode)
         );
      }

      bX += 50 + spacing;
      this.addRenderableWidget(
         this.left = new PonderButton(bX, bY)
            .<PonderButton>withShortcut(bindings.keyLeft)
            .<ElementWidget>showing(PonderGuiTextures.ICON_PONDER_LEFT)
            .<ElementWidget>enableFade(0, 5)
            .withCallback(() -> this.scroll(false))
      );
      bX += 20 + spacing;
      this.addRenderableWidget(
         this.close = new PonderButton(bX, bY)
            .<PonderButton>withShortcut(bindings.keyInventory)
            .<ElementWidget>showing(PonderGuiTextures.ICON_PONDER_CLOSE)
            .<ElementWidget>enableFade(0, 5)
            .withCallback(this::onClose)
      );
      bX += 20 + spacing;
      this.addRenderableWidget(
         this.right = new PonderButton(bX, bY)
            .<PonderButton>withShortcut(bindings.keyRight)
            .<ElementWidget>showing(PonderGuiTextures.ICON_PONDER_RIGHT)
            .<ElementWidget>enableFade(0, 5)
            .withCallback(() -> this.scroll(true))
      );
      bX += 50 + spacing;
      this.addRenderableWidget(
         this.replay = new PonderButton(bX, bY)
            .<PonderButton>withShortcut(bindings.keyDown)
            .<ElementWidget>showing(PonderGuiTextures.ICON_PONDER_REPLAY)
            .<ElementWidget>enableFade(0, 5)
            .withCallback(this::replay)
      );
   }

   @Override
   protected void initBackTrackIcon(BoxWidget backTrack) {
      backTrack.showingElement(GuiGameElement.of(this.stack).scale(1.5).at(-4.0F, -4.0F));
   }

   @Override
   public void tick() {
      super.tick();
      if (this.skipCooling > 0) {
         this.skipCooling--;
      }

      if (this.referredToByTag != null) {
         for (int i = 0; i < this.scenes.size(); i++) {
            PonderScene ponderScene = this.scenes.get(i);
            if (ponderScene.getTags().contains(this.referredToByTag)) {
               if (i != this.index) {
                  this.scenes.get(this.index).fadeOut();
                  this.index = i;
                  this.scenes.get(this.index).begin();
                  this.lazyIndex.chase((double)this.index, 0.25, LerpedFloat.Chaser.EXP);
                  this.identifyMode = false;
               }
               break;
            }
         }

         this.referredToByTag = null;
      }

      this.lazyIndex.tickChaser();
      this.fadeIn.tickChaser();
      this.finishingFlash.tickChaser();
      this.nextUp.tickChaser();
      PonderScene activeScene = this.scenes.get(this.index);
      this.extendedTickLength = 0;
      if (this.isComfyReadingEnabled()) {
         activeScene.forEachVisible(TextWindowElement.class, twe -> this.extendedTickLength = 2);
      }

      if (this.extendedTickTimer == 0) {
         if (!this.identifyMode) {
            ponderTicks++;
            if (this.skipCooling == 0) {
               activeScene.tick();
            }
         }

         if (!this.identifyMode) {
            float lazyIndexValue = this.lazyIndex.getValue();
            if (Math.abs(lazyIndexValue - (float)this.index) > 0.001953125F) {
               this.scenes.get(lazyIndexValue < (float)this.index ? this.index - 1 : this.index + 1).tick();
            }
         }

         this.extendedTickTimer = this.extendedTickLength;
      } else {
         this.extendedTickTimer--;
      }

      if (activeScene.getCurrentTime() == activeScene.getTotalTime() - 1) {
         this.finishingFlashWarmup = 30;
         this.nextUpWarmup = 50;
      }

      if (this.finishingFlashWarmup > 0) {
         this.finishingFlashWarmup--;
         if (this.finishingFlashWarmup == 0) {
            this.finishingFlash.setValue(1.0);
            this.finishingFlash.setValue(1.0);
         }
      }

      if (this.nextUpWarmup > 0) {
         this.nextUpWarmup--;
         if (this.nextUpWarmup == 0) {
            this.nextUp.updateChaseTarget(1.0F);
         }
      }

      this.updateIdentifiedItem(activeScene);
   }

   public PonderScene getActiveScene() {
      return this.scenes.get(this.index);
   }

   public void seekToTime(int time) {
      if (this.getActiveScene().getCurrentTime() > time) {
         this.replay();
      }

      this.getActiveScene().seekToTime(time);
      if (time != 0) {
         this.coolDownAfterSkip();
      }
   }

   public void updateIdentifiedItem(PonderScene activeScene) {
      this.hoveredTooltipItem = ItemStack.EMPTY;
      this.hoveredBlockPos = null;
      if (this.identifyMode) {
         Window w = this.minecraft.getWindow();
         double mouseX = this.minecraft.mouseHandler.xpos() * (double)w.getGuiScaledWidth() / (double)w.getScreenWidth();
         double mouseY = this.minecraft.mouseHandler.ypos() * (double)w.getGuiScaledHeight() / (double)w.getScreenHeight();
         PonderScene.SceneTransform t = activeScene.getTransform();
         Vec3 vec1 = t.screenToScene(mouseX, mouseY, 1000, 0.0F);
         Vec3 vec2 = t.screenToScene(mouseX, mouseY, -100, 0.0F);
         Pair<ItemStack, BlockPos> pair = activeScene.rayTraceScene(vec1, vec2);
         this.hoveredTooltipItem = pair.getFirst();
         this.hoveredBlockPos = pair.getSecond();
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      return this.scroll(scrollY > 0.0) ? true : super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
   }

   protected void replay() {
      this.identifyMode = false;
      PonderScene scene = this.scenes.get(this.index);
      if (hasShiftDown()) {
         PonderIndex.reload();
         this.scenes.clear();
         this.scenes.addAll(PonderIndex.getSceneAccess().compile(scene.getLocation()));
      }

      scene.begin();
   }

   protected boolean scroll(boolean forward) {
      int prevIndex = this.index;
      this.index = forward ? this.index + 1 : this.index - 1;
      this.index = Mth.clamp(this.index, 0, this.scenes.size() - 1);
      if (prevIndex != this.index) {
         this.scenes.get(prevIndex).fadeOut();
         this.scenes.get(this.index).begin();
         this.lazyIndex.chase((double)this.index, 0.25, LerpedFloat.Chaser.EXP);
         this.identifyMode = false;
         return true;
      } else {
         this.index = prevIndex;
         return false;
      }
   }

   @Override
   protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWindow(graphics, mouseX, mouseY, partialTicks);
      partialTicks = getPartialTicks();
      RenderSystem.enableBlend();
      this.renderVisibleScenes(graphics, mouseX, mouseY, this.skipCooling > 0 ? 0.0F : (this.identifyMode ? ponderPartialTicksPaused : partialTicks));
      this.renderWidgets(graphics, mouseX, mouseY, this.identifyMode ? ponderPartialTicksPaused : partialTicks);
   }

   protected void renderVisibleScenes(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      this.renderScene(graphics, mouseX, mouseY, this.index, partialTicks);
      float lazyIndexValue = this.lazyIndex.getValue(partialTicks);
      if (Math.abs(lazyIndexValue - (float)this.index) > 0.001953125F) {
         this.renderScene(graphics, mouseX, mouseY, lazyIndexValue < (float)this.index ? this.index - 1 : this.index + 1, partialTicks);
      }
   }

   protected void renderScene(GuiGraphics graphics, int mouseX, int mouseY, int i, float partialTicks) {
      SuperRenderTypeBuffer buffer = DefaultSuperRenderTypeBuffer.getInstance();
      PonderScene scene = this.scenes.get(i);
      double value = (double)this.lazyIndex.getValue(AnimationTickHolder.getPartialTicksUI());
      double diff = (double)i - value;
      double slide = Mth.lerp(diff * diff, 200.0, 600.0) * diff;
      RenderSystem.enableBlend();
      RenderSystem.enableDepthTest();
      RenderSystem.backupProjectionMatrix();
      PoseStack poseStack = graphics.pose();
      RenderSystem.setupLevelDiffuseLighting(DIFFUSE_LIGHT_0, DIFFUSE_LIGHT_1);
      Matrix4f matrix4f = new Matrix4f(RenderSystem.getProjectionMatrix());
      matrix4f.translate(0.0F, 0.0F, 800.0F);
      RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.DISTANCE_TO_ORIGIN);
      poseStack.pushPose();
      poseStack.translate(0.0F, 0.0F, -800.0F);
      scene.getTransform().updateScreenParams(this.width, this.height, slide);
      scene.getTransform().apply(poseStack, partialTicks);
      scene.getTransform().updateSceneRVE(partialTicks);
      scene.renderScene(buffer, graphics, partialTicks);
      buffer.draw();
      BoundingBox bounds = scene.getBounds();
      poseStack.pushPose();
      if (!scene.shouldHidePlatformShadow()) {
         RenderSystem.enableCull();
         RenderSystem.enableDepthTest();
         poseStack.pushPose();
         poseStack.translate((float)scene.getBasePlateOffsetX(), 0.0F, (float)scene.getBasePlateOffsetZ());
         UIRenderHelper.flipForGuiRender(poseStack);
         float flash = this.finishingFlash.getValue(partialTicks) * 0.9F;
         float alpha = flash;
         flash *= flash;
         flash = flash * 2.0F - 1.0F;
         flash *= flash;
         flash = 1.0F - flash;

         for (int f = 0; f < 4; f++) {
            poseStack.translate((float)scene.getBasePlateSize(), 0.0F, 0.0F);
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, -9.765625E-4F);
            if (flash > 0.0F) {
               poseStack.pushPose();
               poseStack.scale(1.0F, 0.5F + flash * 0.75F, 1.0F);
               graphics.fillGradient(0, -1, -scene.getBasePlateSize(), 0, 0, new Color(13041609).getRGB(), new Color(-1429798967).scaleAlpha(alpha).getRGB());
               poseStack.popPose();
            }

            poseStack.translate(0.0F, 0.0F, 0.001953125F);
            graphics.fillGradient(0, 0, -scene.getBasePlateSize(), 4, 0, new Color(1711276032).getRGB(), new Color(0).getRGB());
            poseStack.popPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
         }

         poseStack.popPose();
         RenderSystem.disableCull();
         RenderSystem.disableDepthTest();
      }

      if (PonderIndex.editingModeActive() && !this.userViewMode) {
         poseStack.scale(-1.0F, -1.0F, 1.0F);
         poseStack.scale(0.0625F, 0.0625F, 0.0625F);
         poseStack.translate(1.0F, -8.0F, -0.015625F);
         poseStack.pushPose();
         poseStack.translate(4.0F, -3.0F, 0.0F);
         poseStack.translate(0.0F, 0.0F, -0.001953125F);

         for (int x = 0; x <= bounds.getXSpan(); x++) {
            poseStack.translate(-16.0F, 0.0F, 0.0F);
            graphics.drawString(this.font, x == bounds.getXSpan() ? "x" : x + "", 0, 0, -1, false);
         }

         poseStack.popPose();
         poseStack.pushPose();
         poseStack.scale(-1.0F, 1.0F, 1.0F);
         poseStack.translate(0.0F, -3.0F, -4.0F);
         poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
         poseStack.translate(-8.0F, -2.0F, 0.03125F);

         for (int z = 0; z <= bounds.getZSpan(); z++) {
            poseStack.translate(16.0F, 0.0F, 0.0F);
            graphics.drawString(this.font, z == bounds.getZSpan() ? "z" : z + "", 0, 0, -1, false);
         }

         poseStack.popPose();
         poseStack.pushPose();
         poseStack.translate((float)(bounds.getXSpan() * -8), 0.0F, (float)(bounds.getZSpan() * 8));
         poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));

         for (Direction d : Iterate.horizontalDirections) {
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, (float)(bounds.getZSpan() * 16));
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            graphics.drawString(this.font, d.name().substring(0, 1), 0, 0, 1728053247, false);
            graphics.drawString(this.font, "|", 2, 10, 1157627903, false);
            graphics.drawString(this.font, ".", 2, 14, 587202559, false);
            poseStack.popPose();
         }

         poseStack.popPose();
         buffer.draw();
      }

      poseStack.popPose();
      poseStack.popPose();
      RenderSystem.restoreProjectionMatrix();
   }

   protected void renderWidgets(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      RenderSystem.disableDepthTest();
      float fade = this.fadeIn.getValue(partialTicks);
      float lazyIndexValue = this.lazyIndex.getValue(partialTicks);
      float indexDiff = lazyIndexValue - (float)this.index;
      PonderScene activeScene = this.scenes.get(this.index);
      PonderScene nextScene = this.scenes.size() > this.index + 1 ? this.scenes.get(this.index + 1) : null;
      boolean noWidgetsHovered = true;

      for (GuiEventListener child : this.children()) {
         noWidgetsHovered &= !child.isMouseOver((double)mouseX, (double)mouseY);
      }

      int tooltipColor = UIRenderHelper.COLOR_TEXT_DARKER.getFirst().getRGB();
      this.renderSceneInformation(graphics, fade, indexDiff, activeScene, tooltipColor);
      PoseStack ms = graphics.pose();
      if (this.identifyMode) {
         if (noWidgetsHovered && mouseY < this.height - 80) {
            ms.pushPose();
            ms.translate((float)mouseX, (float)mouseY, 100.0F);
            if (this.hoveredTooltipItem.isEmpty()) {
               MutableComponent text = Ponder.lang()
                  .translate("ui.identify_mode", ((MutableComponent)this.minecraft.options.keyDrop.getTranslatedKeyMessage()).withStyle(ChatFormatting.WHITE))
                  .style(ChatFormatting.GRAY)
                  .component();
               graphics.renderComponentTooltip(
                  this.font,
                  this.font.getSplitter().splitLines(text, this.width / 3, Style.EMPTY).stream().map(t -> Component.literal(t.getString())).toList(),
                  0,
                  0
               );
            } else {
               graphics.renderTooltip(this.font, this.hoveredTooltipItem, 0, 0);
            }

            if (this.hoveredBlockPos != null && PonderIndex.editingModeActive() && !this.userViewMode) {
               ms.translate(0.0F, -15.0F, 0.0F);
               boolean copied = this.hoveredBlockPos.equals(this.copiedBlockPos);
               MutableComponent coords = Component.literal(
                     this.hoveredBlockPos.getX() + ", " + this.hoveredBlockPos.getY() + ", " + this.hoveredBlockPos.getZ()
                  )
                  .withStyle(copied ? ChatFormatting.GREEN : ChatFormatting.GOLD);
               graphics.renderTooltip(this.font, coords, 0, 0);
            }

            ms.popPose();
         }

         this.scan.flash();
      } else {
         this.scan.dim();
      }

      if (PonderIndex.editingModeActive()) {
         if (this.userViewMode) {
            this.userMode.flash();
         } else {
            this.userMode.dim();
         }
      }

      if (this.isComfyReadingEnabled()) {
         this.slowMode.flash();
      } else {
         this.slowMode.dim();
      }

      this.renderSceneOverlay(graphics, partialTicks, lazyIndexValue, Math.abs(indexDiff));
      this.renderNextUp(graphics, partialTicks, nextScene);
      this.getRenderables().forEach(w -> {
         if (w instanceof PonderButton button) {
            button.fade().startWithValue((double)fade);
         }
      });
      if (this.index == 0 || this.index == 1 && lazyIndexValue < (float)this.index) {
         this.left.fade().startWithValue((double)lazyIndexValue);
      }

      if (this.index == this.scenes.size() - 1 || this.index == this.scenes.size() - 2 && lazyIndexValue > (float)this.index) {
         this.right.fade().startWithValue((double)((float)this.scenes.size() - lazyIndexValue - 1.0F));
      }

      if (activeScene.isFinished()) {
         this.right.flash();
      } else {
         this.right.dim();
         this.nextUp.updateChaseTarget(0.0F);
      }

      Color c1 = COLOR_NAV_ARROW.getFirst().setAlpha(64);
      Color c2 = COLOR_NAV_ARROW.getFirst().setAlpha(32);
      Color c3 = COLOR_NAV_ARROW.getFirst().setAlpha(16);
      UIRenderHelper.breadcrumbArrow(graphics, this.width / 2 - 20, this.height - 51, 0, 20, 20, 5, c1, c2);
      UIRenderHelper.breadcrumbArrow(graphics, this.width / 2 + 20, this.height - 51, 0, -20, 20, -5, c1, c2);
      UIRenderHelper.breadcrumbArrow(graphics, this.width / 2 - 90, this.height - 51, 0, 70, 20, 5, c1, c3);
      UIRenderHelper.breadcrumbArrow(graphics, this.width / 2 + 90, this.height - 51, 0, -70, 20, -5, c1, c3);
      List<PonderTag> sceneTags = activeScene.getTags();
      boolean highlightAll = sceneTags.stream().anyMatch(tag -> tag.getId() == PonderTag.Highlight.ALL);
      double s = Minecraft.getInstance().getWindow().getGuiScale();
      IntStream.range(0, this.tagButtons.size()).forEach(i -> {
         ms.pushPose();
         PonderTag tag = this.tags.get(i);
         LerpedFloat chase = this.tagFades.get(i);
         PonderButton button = this.tagButtons.get(i);
         if (button.isMouseOver((double)mouseX, (double)mouseY)) {
            chase.updateChaseTarget(1.0F);
         } else {
            chase.updateChaseTarget(0.0F);
         }

         chase.tickChaser();
         if (!highlightAll && !sceneTags.contains(tag)) {
            button.dim();
         } else {
            button.flash();
         }

         int x = button.getX() + button.getWidth() + 4;
         int y = button.getY() - 2;
         ms.translate((float)x, (float)y + 5.0F * (1.0F - fade), 800.0F);
         float fadedWidth = 200.0F * chase.getValue(partialTicks);
         UIRenderHelper.streak(graphics, 0.0F, 0, 12, 26, (int)fadedWidth);
         RenderSystem.enableScissor((int)((double)x * s), 0, (int)((double)fadedWidth * s), (int)((double)this.height * s));
         String tagName = tag.getTitle();
         graphics.drawString(this.font, tagName, 3, 8, UIRenderHelper.COLOR_TEXT_ACCENT.getFirst().getRGB(), false);
         RenderSystem.disableScissor();
         ms.popPose();
      });
      this.renderHoverTooltips(graphics, tooltipColor);
      RenderSystem.enableDepthTest();
   }

   private void renderHoverTooltips(GuiGraphics graphics, int tooltipColor) {
      PoseStack poseStack = graphics.pose();
      poseStack.pushPose();
      poseStack.translate(0.0F, 0.0F, 500.0F);
      int tooltipY = this.height - 16;
      if (this.scan.isHoveredOrFocused()) {
         graphics.drawCenteredString(this.font, Ponder.lang().translate("ui.identify").component(), this.scan.getX() + 10, tooltipY, tooltipColor);
      }

      if (this.index != 0 && this.left.isHoveredOrFocused()) {
         graphics.drawCenteredString(this.font, Ponder.lang().translate("ui.previous").component(), this.left.getX() + 10, tooltipY, tooltipColor);
      }

      if (this.close.isHoveredOrFocused()) {
         graphics.drawCenteredString(this.font, Ponder.lang().translate("ui.close").component(), this.close.getX() + 10, tooltipY, tooltipColor);
      }

      if (this.index != this.scenes.size() - 1 && this.right.isHoveredOrFocused()) {
         graphics.drawCenteredString(this.font, Ponder.lang().translate("ui.next").component(), this.right.getX() + 10, tooltipY, tooltipColor);
      }

      if (this.replay.isHoveredOrFocused()) {
         graphics.drawCenteredString(this.font, Ponder.lang().translate("ui.replay").component(), this.replay.getX() + 10, tooltipY, tooltipColor);
      }

      if (this.slowMode.isHoveredOrFocused()) {
         graphics.drawCenteredString(this.font, Ponder.lang().translate("ui.slow_text").component(), this.slowMode.getX() + 5, tooltipY, tooltipColor);
      }

      if (PonderIndex.editingModeActive() && this.userMode.isHoveredOrFocused()) {
         graphics.drawCenteredString(this.font, "Editor View", this.userMode.getX() + 10, tooltipY, tooltipColor);
      }

      poseStack.popPose();
   }

   private void renderNextUp(GuiGraphics graphics, float partialTicks, @Nullable PonderScene nextScene) {
      if (this.getActiveScene().isFinished()) {
         if (nextScene != null && nextScene.isNextUpEnabled()) {
            if (this.nextUp.getValue() > 0.0625F) {
               PoseStack poseStack = graphics.pose();
               poseStack.pushPose();
               poseStack.translate((float)(this.right.getX() + 10), (float)(this.right.getY() - 6) + this.nextUp.getValue(partialTicks) * 5.0F, 400.0F);
               MutableComponent nextUpComponent = Ponder.lang().translate("ui.next_up").component();
               int boxWidth = Math.max(this.font.width(nextScene.getTitle()), this.font.width(nextUpComponent)) + 5;
               renderSpeechBox(graphics, 0, 0, boxWidth, 20, this.right.isHoveredOrFocused(), Pointing.DOWN, false);
               poseStack.translate(0.0F, -29.0F, 100.0F);
               graphics.drawCenteredString(this.font, nextUpComponent, 0, 0, UIRenderHelper.COLOR_TEXT_DARKER.getFirst().getRGB());
               graphics.drawCenteredString(this.font, nextScene.getTitle(), 0, 10, UIRenderHelper.COLOR_TEXT.getFirst().getRGB());
               poseStack.popPose();
            }
         }
      }
   }

   private void renderSceneOverlay(GuiGraphics graphics, float partialTicks, float lazyIndexValue, float indexDiff) {
      float scenePT = this.skipCooling > 0 ? 0.0F : partialTicks;
      PoseStack poseStack = graphics.pose();
      poseStack.pushPose();
      poseStack.translate(0.0F, 0.0F, 100.0F);
      this.renderOverlay(graphics, this.index, scenePT);
      if (indexDiff > 0.001953125F) {
         this.renderOverlay(graphics, lazyIndexValue < (float)this.index ? this.index - 1 : this.index + 1, scenePT);
      }

      poseStack.popPose();
   }

   private void renderSceneInformation(GuiGraphics graphics, float fade, float indexDiff, PonderScene activeScene, int tooltipColor) {
      float absoluteIndexDiff = Math.abs(indexDiff);
      int otherIndex = this.index;
      if (this.scenes.size() != 1 && (double)absoluteIndexDiff >= 0.01) {
         float indexOffset = Math.signum(indexDiff);
         otherIndex = this.index + (int)indexOffset;
         if (otherIndex < 0 || otherIndex >= this.scenes.size()) {
            return;
         }
      }

      String title = activeScene.getTitle();
      String otherTitle = this.scenes.get(otherIndex).getTitle();
      int maxTitleWidth = 180;
      int titleWidth = this.font.width(title);
      if (titleWidth > maxTitleWidth) {
         titleWidth = maxTitleWidth;
      }

      int otherTitleWidth = this.font.width(otherTitle);
      if (otherTitleWidth > maxTitleWidth) {
         otherTitleWidth = maxTitleWidth;
      }

      int wrappedTitleHeight = this.font.wordWrapHeight(title, maxTitleWidth);
      int otherWrappedTitleHeight = this.font.wordWrapHeight(otherTitle, maxTitleWidth);
      int streakHeight = 26 + (int)Mth.lerp(absoluteIndexDiff, (float)wrappedTitleHeight, (float)otherWrappedTitleHeight);
      int streakWidth = 70 + (int)Mth.lerp(absoluteIndexDiff, (float)titleWidth, (float)otherTitleWidth);
      PoseStack poseStack = graphics.pose();
      poseStack.pushPose();
      poseStack.translate(0.0F, 0.0F, 400.0F);
      poseStack.translate(55.0F, 19.0F, 0.0F);
      UIRenderHelper.streak(graphics, 0.0F, 0, streakHeight / 2, streakHeight, (int)((float)streakWidth * fade));
      UIRenderHelper.streak(graphics, 180.0F, 0, streakHeight / 2, streakHeight, (int)(30.0F * fade));
      new BoxElement()
         .<BoxElement>withBackground(BACKGROUND_FLAT)
         .<BoxElement>gradientBorder(COLOR_IDLE)
         .<RenderElement>at(-34.0F, 2.0F, 100.0F)
         .<RenderElement>withBounds(30, 30)
         .render(graphics);
      GuiGameElement.of(this.stack).scale(2.0).<RenderElement>at(-35.0F, 1.0F).render(graphics);
      poseStack.translate(4.0F, 6.0F, 0.0F);
      graphics.drawString(this.font, Ponder.lang().translate("ui.pondering").component(), 0, 0, tooltipColor, false);
      poseStack.translate(0.0F, 14.0F, 0.0F);
      if (this.scenes.size() != 1 && !((double)absoluteIndexDiff < 0.01)) {
         poseStack.translate(0.0F, 6.0F, 0.0F);
         poseStack.pushPose();
         poseStack.mulPose(Axis.XN.rotationDegrees(indexDiff * -90.0F + Math.signum(indexDiff) * 90.0F));
         poseStack.translate(0.0F, -6.0F, 5.0F);
         ClientFontHelper.drawSplitString(
            graphics, poseStack, this.font, otherTitle, 0, 0, maxTitleWidth, UIRenderHelper.COLOR_TEXT.getFirst().scaleAlphaForText(absoluteIndexDiff).getRGB()
         );
         poseStack.popPose();
         poseStack.mulPose(Axis.XN.rotationDegrees(indexDiff * -90.0F));
         poseStack.translate(0.0F, -6.0F, 5.0F);
         ClientFontHelper.drawSplitString(
            graphics,
            poseStack,
            this.font,
            title,
            0,
            0,
            maxTitleWidth,
            UIRenderHelper.COLOR_TEXT.getFirst().scaleAlphaForText(1.0F - absoluteIndexDiff).getRGB()
         );
         poseStack.popPose();
      } else {
         ClientFontHelper.drawSplitString(
            graphics, poseStack, this.font, title, 0, 0, maxTitleWidth, UIRenderHelper.COLOR_TEXT.getFirst().scaleAlphaForText(fade).getRGB()
         );
         poseStack.popPose();
      }
   }

   private void renderOverlay(GuiGraphics graphics, int i, float partialTicks) {
      if (!this.identifyMode) {
         graphics.pose().pushPose();
         PonderScene story = this.scenes.get(i);
         story.renderOverlay(this, graphics, this.skipCooling > 0 ? 0.0F : (this.identifyMode ? ponderPartialTicksPaused : partialTicks));
         graphics.pose().popPose();
      }
   }

   public boolean mouseClicked(double x, double y, int button) {
      if (this.identifyMode && this.hoveredBlockPos != null && PonderIndex.editingModeActive()) {
         long handle = this.minecraft.getWindow().getWindow();
         if (this.copiedBlockPos != null && button == 1) {
            this.clipboardHelper
               .setClipboard(
                  handle,
                  "util.select().fromTo("
                     + this.copiedBlockPos.getX()
                     + ", "
                     + this.copiedBlockPos.getY()
                     + ", "
                     + this.copiedBlockPos.getZ()
                     + ", "
                     + this.hoveredBlockPos.getX()
                     + ", "
                     + this.hoveredBlockPos.getY()
                     + ", "
                     + this.hoveredBlockPos.getZ()
                     + ")"
               );
            this.copiedBlockPos = this.hoveredBlockPos;
            return true;
         } else {
            if (hasShiftDown()) {
               this.clipboardHelper
                  .setClipboard(
                     handle,
                     "util.select().position(" + this.hoveredBlockPos.getX() + ", " + this.hoveredBlockPos.getY() + ", " + this.hoveredBlockPos.getZ() + ")"
                  );
            } else {
               this.clipboardHelper
                  .setClipboard(
                     handle, "util.grid().at(" + this.hoveredBlockPos.getX() + ", " + this.hoveredBlockPos.getY() + ", " + this.hoveredBlockPos.getZ() + ")"
                  );
            }

            this.copiedBlockPos = this.hoveredBlockPos;
            return true;
         }
      } else {
         return super.mouseClicked(x, y, button);
      }
   }

   @Override
   protected String getBreadcrumbTitle() {
      return this.chapter != null ? this.chapter.getTitle() : this.stack.getItem().getDescription().getString();
   }

   public Font getFontRenderer() {
      return this.font;
   }

   protected boolean isMouseOver(double mouseX, double mouseY, int x, int y, int w, int h) {
      boolean hovered = !(mouseX < (double)x) && !(mouseX > (double)(x + w));
      return hovered & (!(mouseY < (double)y) && !(mouseY > (double)(y + h)));
   }

   public static void renderSpeechBox(
      GuiGraphics graphics, int x, int y, int w, int h, boolean highlighted, Pointing pointing, boolean returnWithLocalTransform
   ) {
      PoseStack poseStack = graphics.pose();
      if (!returnWithLocalTransform) {
         poseStack.pushPose();
      }

      int divotRotation = 0;
      int divotSize = 8;
      int distance = 1;
      int divotRadius = divotSize / 2;
      Couple<Color> borderColors = highlighted ? PonderButton.COLOR_HOVER : COLOR_IDLE;
      int boxX;
      int boxY;
      int divotX;
      int divotY;
      Color c;
      short var19;
      switch (pointing) {
         case DOWN:
         default:
            var19 = 0;
            boxX = x - w / 2;
            boxY = y - (h + divotSize + 1 + distance);
            divotX = x - divotRadius;
            divotY = y - (divotSize + distance);
            c = borderColors.getSecond();
            break;
         case LEFT:
            var19 = 90;
            boxX = x + divotSize + 1 + distance;
            boxY = y - h / 2;
            divotX = x + distance;
            divotY = y - divotRadius;
            c = Color.mixColors(borderColors, 0.5F);
            break;
         case RIGHT:
            var19 = 270;
            boxX = x - (w + divotSize + 1 + distance);
            boxY = y - h / 2;
            divotX = x - (divotSize + distance);
            divotY = y - divotRadius;
            c = Color.mixColors(borderColors, 0.5F);
            break;
         case UP:
            var19 = 180;
            boxX = x - w / 2;
            boxY = y + divotSize + 1 + distance;
            divotX = x - divotRadius;
            divotY = y + distance;
            c = borderColors.getFirst();
      }

      new BoxElement()
         .<BoxElement>withBackground(BACKGROUND_FLAT)
         .<BoxElement>gradientBorder(borderColors)
         .<RenderElement>at((float)boxX, (float)boxY, 100.0F)
         .<RenderElement>withBounds(w, h)
         .render(graphics);
      poseStack.pushPose();
      poseStack.translate((float)(divotX + divotRadius), (float)(divotY + divotRadius), 110.0F);
      poseStack.mulPose(Axis.ZP.rotationDegrees((float)var19));
      poseStack.translate((float)(-divotRadius), (float)(-divotRadius), 0.0F);
      PonderGuiTextures.SPEECH_TOOLTIP_BACKGROUND.render(graphics, 0, 0);
      PonderGuiTextures.SPEECH_TOOLTIP_COLOR.render(graphics, 0, 0, c);
      poseStack.popPose();
      if (returnWithLocalTransform) {
         poseStack.translate((float)boxX, (float)boxY, 0.0F);
      } else {
         poseStack.popPose();
      }
   }

   public ItemStack getHoveredTooltipItem() {
      return this.hoveredTooltipItem;
   }

   public ItemStack getSubject() {
      return this.stack;
   }

   @Override
   public boolean isEquivalentTo(NavigatableSimiScreen other) {
      return !(other instanceof PonderUI otherUI) ? super.isEquivalentTo(other) : !otherUI.stack.isEmpty() && this.stack.is(otherUI.stack.getItem());
   }

   @Override
   public void shareContextWith(NavigatableSimiScreen other) {
      if (other instanceof PonderUI ponderUI) {
         ponderUI.referredToByTag = this.referredToByTag;
      }
   }

   public static float getPartialTicks() {
      float renderPartialTicks = AnimationTickHolder.getPartialTicksUI();
      if (Minecraft.getInstance().screen instanceof PonderUI ui) {
         return ui.identifyMode
            ? ponderPartialTicksPaused
            : (renderPartialTicks + (float)(ui.extendedTickLength - ui.extendedTickTimer)) / (float)(ui.extendedTickLength + 1);
      } else {
         return renderPartialTicks;
      }
   }

   @Override
   public boolean isPauseScreen() {
      return true;
   }

   public void coolDownAfterSkip() {
      this.skipCooling = 15;
   }

   public void removed() {
      super.removed();
      this.hoveredTooltipItem = ItemStack.EMPTY;
   }

   public boolean isComfyReadingEnabled() {
      return PonderConfig.client().comfyReading.get();
   }

   public void setComfyReadingEnabled(boolean slowTextMode) {
      PonderConfig.client().comfyReading.set(Boolean.valueOf(slowTextMode));
   }
}

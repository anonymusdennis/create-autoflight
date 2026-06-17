package net.createmod.ponder.foundation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.PonderElement;
import net.createmod.ponder.api.element.PonderOverlayElement;
import net.createmod.ponder.api.element.PonderSceneElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.registration.StoryBoardEntry;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.foundation.element.WorldSectionElementImpl;
import net.createmod.ponder.foundation.instruction.HideAllInstruction;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.createmod.ponder.foundation.registration.PonderLocalization;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableObject;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class PonderScene {
   public static final String TITLE_KEY = "header";
   final PonderLocalization localization;
   private boolean finished;
   private int textIndex;
   ResourceLocation sceneId;
   private final IntList keyframeTimes;
   List<PonderInstruction> schedule;
   private final List<PonderInstruction> activeSchedule;
   private final Map<UUID, PonderElement> linkedElements;
   private final Set<PonderElement> elements;
   private final List<PonderTag> tags;
   private final List<StoryBoardEntry.SceneOrderingEntry> orderingEntries;
   private final PonderLevel world;
   private final String namespace;
   private final ResourceLocation location;
   private final PonderScene.SceneCamera camera;
   private final Outliner outliner;
   private PonderScene.SceneTransform transform;
   private final WorldSectionElement baseWorldSection;
   private final Entity renderViewEntity;
   private Vec3 pointOfInterest;
   @Nullable
   private Vec3 chasingPointOfInterest;
   int basePlateOffsetX;
   int basePlateOffsetZ;
   int basePlateSize;
   float scaleFactor;
   float yOffset;
   boolean hidePlatformShadow;
   private boolean stoppedCounting;
   private int totalTime;
   private int currentTime;
   private boolean nextUpEnabled = true;

   public PonderScene(
      @Nullable PonderLevel world,
      PonderLocalization localization,
      String namespace,
      ResourceLocation location,
      Collection<ResourceLocation> tags,
      Collection<StoryBoardEntry.SceneOrderingEntry> orderingEntries
   ) {
      if (world != null) {
         world.scene = this;
      }

      this.world = world;
      this.localization = localization;
      this.pointOfInterest = Vec3.ZERO;
      this.textIndex = 1;
      this.hidePlatformShadow = false;
      this.namespace = namespace;
      this.location = location;
      this.sceneId = ResourceLocation.fromNamespaceAndPath(namespace, "missing_title");
      this.outliner = new Outliner();
      this.elements = new HashSet<>();
      this.linkedElements = new HashMap<>();
      this.tags = tags.stream().map(PonderIndex.getTagAccess()::getRegisteredTag).toList();
      this.orderingEntries = new ArrayList<>(orderingEntries);
      this.schedule = new ArrayList<>();
      this.activeSchedule = new ArrayList<>();
      this.transform = new PonderScene.SceneTransform();
      this.basePlateSize = this.getBounds().getXSpan();
      this.camera = new PonderScene.SceneCamera();
      this.baseWorldSection = new WorldSectionElementImpl();
      this.keyframeTimes = new IntArrayList(4);
      this.scaleFactor = 1.0F;
      this.yOffset = 0.0F;
      if (world != null) {
         this.renderViewEntity = new ArmorStand(world, 0.0, 0.0, 0.0);
      } else {
         this.renderViewEntity = null;
      }

      this.setPointOfInterest(new Vec3(0.0, 4.0, 0.0));
   }

   public void deselect() {
      this.forEach(WorldSectionElement.class, WorldSectionElement::resetSelectedBlock);
   }

   public Pair<ItemStack, BlockPos> rayTraceScene(Vec3 from, Vec3 to) {
      MutableObject<Pair<WorldSectionElement, Pair<Vec3, BlockHitResult>>> nearestHit = new MutableObject();
      MutableDouble bestDistance = new MutableDouble(0.0);
      this.forEach(WorldSectionElement.class, wse -> {
         wse.resetSelectedBlock();
         if (wse.isVisible()) {
            Pair<Vec3, BlockHitResult> rayTrace = wse.rayTrace(this.world, from, to);
            if (rayTrace != null) {
               double distanceTo = rayTrace.getFirst().distanceTo(from);
               if (nearestHit.getValue() == null || !(distanceTo >= bestDistance.getValue())) {
                  nearestHit.setValue(Pair.of(wse, rayTrace));
                  bestDistance.setValue(distanceTo);
               }
            }
         }
      });
      if (nearestHit.getValue() == null) {
         return Pair.of(ItemStack.EMPTY, BlockPos.ZERO);
      } else {
         Pair<Vec3, BlockHitResult> selectedHit = (Pair<Vec3, BlockHitResult>)((Pair)nearestHit.getValue()).getSecond();
         BlockPos selectedPos = selectedHit.getSecond().getBlockPos();
         BlockPos origin = new BlockPos(this.basePlateOffsetX, 0, this.basePlateOffsetZ);
         if (!this.world.getBounds().isInside(selectedPos)) {
            return Pair.of(ItemStack.EMPTY, null);
         } else if (BoundingBox.fromCorners(origin, origin.offset(new Vec3i(this.basePlateSize - 1, 0, this.basePlateSize - 1))).isInside(selectedPos)) {
            if (PonderIndex.editingModeActive()) {
               ((WorldSectionElement)((Pair)nearestHit.getValue()).getFirst()).selectBlock(selectedPos);
            }

            return Pair.of(ItemStack.EMPTY, selectedPos);
         } else {
            ((WorldSectionElement)((Pair)nearestHit.getValue()).getFirst()).selectBlock(selectedPos);
            BlockState blockState = this.world.getBlockState(selectedPos);
            Direction direction = selectedHit.getSecond().getDirection();
            Vec3 location = selectedHit.getSecond().getLocation();
            ItemStack pickBlock = CatnipServices.HOOKS
               .getCloneItemFromBlockstate(
                  blockState, new BlockHitResult(location, direction, selectedPos, true), this.world, selectedPos, Minecraft.getInstance().player
               );
            return Pair.of(pickBlock, selectedPos);
         }
      }
   }

   public void reset() {
      this.currentTime = 0;
      this.activeSchedule.clear();
      this.schedule.forEach(mdi -> mdi.reset(this));
   }

   public void begin() {
      this.reset();
      this.forEach(pe -> pe.reset(this));
      this.world.restore();
      this.elements.clear();
      this.linkedElements.clear();
      this.keyframeTimes.clear();
      this.transform = new PonderScene.SceneTransform();
      this.finished = false;
      this.setPointOfInterest(new Vec3(0.0, 4.0, 0.0));
      this.baseWorldSection.setEmpty();
      this.baseWorldSection.forceApplyFade(1.0F);
      this.elements.add(this.baseWorldSection);
      this.totalTime = 0;
      this.stoppedCounting = false;
      this.activeSchedule.addAll(this.schedule);
      this.activeSchedule.forEach(i -> i.onScheduled(this));
   }

   public WorldSectionElement getBaseWorldSection() {
      return this.baseWorldSection;
   }

   public float getSceneProgress() {
      return this.totalTime == 0 ? 0.0F : (float)this.currentTime / (float)this.totalTime;
   }

   public void fadeOut() {
      this.reset();
      this.activeSchedule.add(new HideAllInstruction(10, null));
   }

   public void renderScene(SuperRenderTypeBuffer buffer, GuiGraphics graphics, float pt) {
      PoseStack ms = graphics.pose();
      ms.pushPose();
      Minecraft mc = Minecraft.getInstance();
      Entity prevRVE = mc.cameraEntity;
      mc.cameraEntity = this.renderViewEntity;
      this.forEachVisible(PonderSceneElement.class, e -> e.renderFirst(this.world, buffer, graphics, pt));
      mc.cameraEntity = prevRVE;

      for (RenderType type : RenderType.chunkBufferLayers()) {
         this.forEachVisible(PonderSceneElement.class, e -> e.renderLayer(this.world, buffer, type, graphics, pt));
      }

      this.forEachVisible(PonderSceneElement.class, e -> e.renderLast(this.world, buffer, graphics, pt));
      this.camera.set(-this.transform.xRotation.getValue(pt), this.transform.yRotation.getValue(pt) + 180.0F);
      this.world.renderEntities(ms, buffer, this.camera, pt);
      this.world.renderParticles(ms, buffer, this.camera, pt);
      this.outliner.renderOutlines(ms, buffer, Vec3.ZERO, pt);
      ms.popPose();
   }

   public void renderOverlay(PonderUI screen, GuiGraphics graphics, float partialTicks) {
      graphics.pose().pushPose();
      this.forEachVisible(PonderOverlayElement.class, e -> e.render(this, screen, graphics, partialTicks));
      graphics.pose().popPose();
   }

   public void setPointOfInterest(Vec3 poi) {
      if (this.chasingPointOfInterest == null) {
         this.pointOfInterest = poi;
      }

      this.chasingPointOfInterest = poi;
   }

   public Vec3 getPointOfInterest() {
      return this.pointOfInterest;
   }

   public void tick() {
      if (this.chasingPointOfInterest != null) {
         this.pointOfInterest = VecHelper.lerp(0.25F, this.pointOfInterest, this.chasingPointOfInterest);
      }

      this.outliner.tickOutlines();
      this.world.tick();
      this.transform.tick();
      this.forEach(e -> e.tick(this));
      if (this.currentTime < this.totalTime) {
         this.currentTime++;
      }

      Iterator<PonderInstruction> iterator = this.activeSchedule.iterator();

      while (iterator.hasNext()) {
         PonderInstruction instruction = iterator.next();
         instruction.tick(this);
         if (instruction.isComplete()) {
            iterator.remove();
            if (instruction.isBlocking()) {
               break;
            }
         } else if (instruction.isBlocking()) {
            break;
         }
      }

      if (this.activeSchedule.isEmpty()) {
         this.finished = true;
      }
   }

   public void seekToTime(int time) {
      if (time < this.currentTime) {
         throw new IllegalStateException("Cannot seek backwards. Rewind first.");
      } else {
         while (this.currentTime < time && !this.finished) {
            this.forEach(e -> e.whileSkipping(this));
            this.tick();
         }

         this.forEach(WorldSectionElement.class, WorldSectionElement::queueRedraw);
      }
   }

   public void addToSceneTime(int time) {
      if (!this.stoppedCounting) {
         this.totalTime += time;
      }
   }

   public void stopCounting() {
      this.stoppedCounting = true;
   }

   public void markKeyframe(int offset) {
      if (!this.stoppedCounting) {
         this.keyframeTimes.add(this.totalTime + offset);
      }
   }

   public void addElement(PonderElement e) {
      this.elements.add(e);
   }

   public <E extends PonderElement> void linkElement(E e, ElementLink<E> link) {
      this.linkedElements.put(link.getId(), e);
   }

   @Nullable
   public <E extends PonderElement> E resolve(ElementLink<E> link) {
      return link.cast(this.linkedElements.get(link.getId()));
   }

   public <E extends PonderElement> Optional<E> resolveOptional(ElementLink<E> link) {
      return Optional.ofNullable(this.resolve(link));
   }

   public <E extends PonderElement> void runWith(ElementLink<E> link, Consumer<E> callback) {
      callback.accept(this.resolve(link));
   }

   public <E extends PonderElement, F> F applyTo(ElementLink<E> link, Function<E, F> function) {
      return function.apply(this.resolve(link));
   }

   public void forEach(Consumer<? super PonderElement> function) {
      for (PonderElement elemtent : this.elements) {
         function.accept(elemtent);
      }
   }

   public <T extends PonderElement> void forEach(Class<T> type, Consumer<T> function) {
      for (PonderElement element : this.elements) {
         if (type.isInstance(element)) {
            function.accept(type.cast(element));
         }
      }
   }

   public <T extends PonderElement> void forEachVisible(Class<T> type, Consumer<T> function) {
      for (PonderElement element : this.elements) {
         if (type.isInstance(element) && element.isVisible()) {
            function.accept(type.cast(element));
         }
      }
   }

   public <T extends Entity> void forEachWorldEntity(Class<T> type, Consumer<T> function) {
      for (Entity entity : this.world.getEntityList()) {
         if (type.isInstance(entity)) {
            function.accept(type.cast(entity));
         }
      }
   }

   public Supplier<String> registerText(String defaultText) {
      String key = "text_" + this.textIndex;
      this.localization.registerSpecific(this.sceneId, key, defaultText);
      Supplier<String> supplier = () -> this.localization.getSpecific(this.sceneId, key);
      this.textIndex++;
      return supplier;
   }

   public Supplier<String> registerText(String defaultText, Object... params) {
      String key = "text_" + this.textIndex;
      this.localization.registerSpecific(this.sceneId, key, defaultText);
      Supplier<String> supplier = () -> this.localization.getSpecific(this.sceneId, key, params);
      this.textIndex++;
      return supplier;
   }

   public SceneBuilder builder() {
      return new PonderSceneBuilder(this);
   }

   public SceneBuildingUtil getSceneBuildingUtil() {
      return new PonderSceneBuildingUtil(this.getBounds());
   }

   public String getTitle() {
      return this.getString("header");
   }

   public String getString(String key) {
      return this.localization.getSpecific(this.sceneId, key);
   }

   public PonderLevel getWorld() {
      return this.world;
   }

   public String getNamespace() {
      return this.namespace;
   }

   public int getKeyframeCount() {
      return this.keyframeTimes.size();
   }

   public int getKeyframeTime(int index) {
      return this.keyframeTimes.getInt(index);
   }

   public List<PonderTag> getTags() {
      return this.tags;
   }

   public List<StoryBoardEntry.SceneOrderingEntry> getOrderingEntries() {
      return this.orderingEntries;
   }

   public ResourceLocation getLocation() {
      return this.location;
   }

   public Set<PonderElement> getElements() {
      return this.elements;
   }

   public BoundingBox getBounds() {
      return this.world == null ? new BoundingBox(BlockPos.ZERO) : this.world.getBounds();
   }

   public ResourceLocation getId() {
      return this.sceneId;
   }

   public PonderScene.SceneTransform getTransform() {
      return this.transform;
   }

   public Outliner getOutliner() {
      return this.outliner;
   }

   public boolean isFinished() {
      return this.finished;
   }

   public void setFinished(boolean finished) {
      this.finished = finished;
   }

   public int getBasePlateOffsetX() {
      return this.basePlateOffsetX;
   }

   public int getBasePlateOffsetZ() {
      return this.basePlateOffsetZ;
   }

   public boolean shouldHidePlatformShadow() {
      return this.hidePlatformShadow;
   }

   public int getBasePlateSize() {
      return this.basePlateSize;
   }

   public float getScaleFactor() {
      return this.scaleFactor;
   }

   public float getYOffset() {
      return this.yOffset;
   }

   public int getTotalTime() {
      return this.totalTime;
   }

   public int getCurrentTime() {
      return this.currentTime;
   }

   public void setNextUpEnabled(boolean nextUpEnabled) {
      this.nextUpEnabled = nextUpEnabled;
   }

   public boolean isNextUpEnabled() {
      return this.nextUpEnabled;
   }

   public static class SceneCamera extends Camera {
      public void set(float xRotation, float yRotation) {
         this.setRotation(yRotation, xRotation);
      }
   }

   public class SceneTransform {
      public LerpedFloat xRotation = LerpedFloat.angular().disableSmartAngleChasing().startWithValue(-35.0);
      public LerpedFloat yRotation = LerpedFloat.angular().disableSmartAngleChasing().startWithValue(145.0);
      private int width;
      private int height;
      private double offset;
      private Matrix4f cachedMat;

      public void tick() {
         this.xRotation.tickChaser();
         this.yRotation.tickChaser();
      }

      public void updateScreenParams(int width, int height, double offset) {
         this.width = width;
         this.height = height;
         this.offset = offset;
         this.cachedMat = null;
      }

      public PoseStack apply(PoseStack ms) {
         return this.apply(ms, AnimationTickHolder.getPartialTicks(PonderScene.this.world));
      }

      public PoseStack apply(PoseStack ms, float pt) {
         ms.translate((double)(this.width / 2), (double)(this.height / 2), 200.0 + this.offset);
         ms.mulPose(Axis.XP.rotationDegrees(-35.0F));
         ms.mulPose(Axis.YP.rotationDegrees(55.0F));
         ms.translate(this.offset, 0.0, 0.0);
         ms.mulPose(Axis.YP.rotationDegrees(-55.0F));
         ms.mulPose(Axis.XP.rotationDegrees(35.0F));
         ms.mulPose(Axis.XP.rotationDegrees(this.xRotation.getValue(pt)));
         ms.mulPose(Axis.YP.rotationDegrees(this.yRotation.getValue(pt)));
         UIRenderHelper.flipForGuiRender(ms);
         float f = 30.0F * PonderScene.this.scaleFactor;
         ms.scale(f, f, f);
         ms.translate(
            (float)PonderScene.this.basePlateSize / -2.0F - (float)PonderScene.this.basePlateOffsetX,
            -1.0F + PonderScene.this.yOffset,
            (float)PonderScene.this.basePlateSize / -2.0F - (float)PonderScene.this.basePlateOffsetZ
         );
         return ms;
      }

      public void updateSceneRVE(float pt) {
         Vec3 v = this.screenToScene((double)(this.width / 2), (double)(this.height / 2), 500, pt);
         if (PonderScene.this.renderViewEntity != null) {
            PonderScene.this.renderViewEntity.setPos(v.x, v.y, v.z);
         }
      }

      public Vec3 screenToScene(double x, double y, int depth, float pt) {
         this.refreshMatrix(pt);
         Vec3 vec = new Vec3(x, y, (double)depth);
         vec = vec.subtract((double)(this.width / 2), (double)(this.height / 2), 200.0 + this.offset);
         vec = VecHelper.rotate(vec, 35.0, net.minecraft.core.Direction.Axis.X);
         vec = VecHelper.rotate(vec, -55.0, net.minecraft.core.Direction.Axis.Y);
         vec = vec.subtract(this.offset, 0.0, 0.0);
         vec = VecHelper.rotate(vec, 55.0, net.minecraft.core.Direction.Axis.Y);
         vec = VecHelper.rotate(vec, -35.0, net.minecraft.core.Direction.Axis.X);
         vec = VecHelper.rotate(vec, (double)(-this.xRotation.getValue(pt)), net.minecraft.core.Direction.Axis.X);
         vec = VecHelper.rotate(vec, (double)(-this.yRotation.getValue(pt)), net.minecraft.core.Direction.Axis.Y);
         float f = 1.0F / (30.0F * PonderScene.this.scaleFactor);
         vec = vec.multiply((double)f, (double)(-f), (double)f);
         return vec.subtract(
            (double)((float)PonderScene.this.basePlateSize / -2.0F - (float)PonderScene.this.basePlateOffsetX),
            (double)(-1.0F + PonderScene.this.yOffset),
            (double)((float)PonderScene.this.basePlateSize / -2.0F - (float)PonderScene.this.basePlateOffsetZ)
         );
      }

      public Vec2 sceneToScreen(Vec3 vec, float pt) {
         this.refreshMatrix(pt);
         Vector4f vec4 = new Vector4f((float)vec.x, (float)vec.y, (float)vec.z, 1.0F);
         vec4.mul(this.cachedMat);
         return new Vec2(vec4.x(), vec4.y());
      }

      protected void refreshMatrix(float pt) {
         if (this.cachedMat == null) {
            this.cachedMat = this.apply(new PoseStack(), pt).last().pose();
         }
      }
   }
}

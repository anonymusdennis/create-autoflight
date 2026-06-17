package net.createmod.ponder.foundation.element;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.client.render.model.BakedModelBufferer;
import net.createmod.catnip.client.render.model.ShadeSeparatedResultConsumer;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.outliner.AABBOutline;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.render.SuperByteBufferBuilder;
import net.createmod.catnip.render.SuperByteBufferCache;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WorldSectionElementImpl extends AnimatedSceneElementBase implements WorldSectionElement {
   public static final SuperByteBufferCache.Compartment<Pair<Integer, Integer>> PONDER_WORLD_SECTION = new SuperByteBufferCache.Compartment<>();
   private static final ThreadLocal<WorldSectionElementImpl.ThreadLocalObjects> THREAD_LOCAL_OBJECTS = ThreadLocal.withInitial(
      WorldSectionElementImpl.ThreadLocalObjects::new
   );
   @Nullable
   List<BlockEntity> renderedBlockEntities;
   @Nullable
   List<Pair<BlockEntity, Consumer<Level>>> tickableBlockEntities;
   @Nullable
   Selection section;
   boolean redraw;
   Vec3 prevAnimatedOffset = Vec3.ZERO;
   Vec3 animatedOffset = Vec3.ZERO;
   Vec3 prevAnimatedRotation = Vec3.ZERO;
   Vec3 animatedRotation = Vec3.ZERO;
   Vec3 centerOfRotation = Vec3.ZERO;
   @Nullable
   Vec3 stabilizationAnchor = null;
   @Nullable
   BlockPos selectedBlock;

   public WorldSectionElementImpl() {
   }

   public WorldSectionElementImpl(Selection section) {
      this.section = section.copy();
      this.centerOfRotation = section.getCenter();
   }

   @Override
   public void mergeOnto(WorldSectionElement other) {
      this.setVisible(false);
      if (other.isEmpty()) {
         other.set(this.section);
      } else {
         other.add(this.section);
      }
   }

   @Override
   public void set(Selection selection) {
      this.applyNewSelection(selection.copy());
   }

   @Override
   public void add(Selection toAdd) {
      this.applyNewSelection(this.section.add(toAdd));
   }

   @Override
   public void erase(Selection toErase) {
      this.applyNewSelection(this.section.substract(toErase));
   }

   private void applyNewSelection(Selection selection) {
      this.section = selection;
      this.queueRedraw();
   }

   @Override
   public void setCenterOfRotation(Vec3 center) {
      this.centerOfRotation = center;
   }

   @Override
   public void stabilizeRotation(Vec3 anchor) {
      this.stabilizationAnchor = anchor;
   }

   @Override
   public void reset(PonderScene scene) {
      super.reset(scene);
      this.resetAnimatedTransform();
      this.resetSelectedBlock();
   }

   @Override
   public void selectBlock(BlockPos pos) {
      this.selectedBlock = pos;
   }

   @Override
   public void resetSelectedBlock() {
      this.selectedBlock = null;
   }

   public void resetAnimatedTransform() {
      this.prevAnimatedOffset = Vec3.ZERO;
      this.animatedOffset = Vec3.ZERO;
      this.prevAnimatedRotation = Vec3.ZERO;
      this.animatedRotation = Vec3.ZERO;
   }

   @Override
   public void queueRedraw() {
      this.redraw = true;
   }

   @Override
   public boolean isEmpty() {
      return this.section == null;
   }

   @Override
   public void setEmpty() {
      this.section = null;
   }

   @Override
   public void setAnimatedRotation(Vec3 eulerAngles, boolean force) {
      this.animatedRotation = eulerAngles;
      if (force) {
         this.prevAnimatedRotation = this.animatedRotation;
      }
   }

   @Override
   public Vec3 getAnimatedRotation() {
      return this.animatedRotation;
   }

   @Override
   public void setAnimatedOffset(Vec3 offset, boolean force) {
      this.animatedOffset = offset;
      if (force) {
         this.prevAnimatedOffset = this.animatedOffset;
      }
   }

   @Override
   public Vec3 getAnimatedOffset() {
      return this.animatedOffset;
   }

   @Override
   public boolean isVisible() {
      return super.isVisible() && !this.isEmpty();
   }

   @Override
   public Pair<Vec3, BlockHitResult> rayTrace(PonderLevel world, Vec3 source, Vec3 target) {
      world.setMask(this.section);
      Vec3 transformedTarget = this.reverseTransformVec(target);
      BlockHitResult rayTraceBlocks = world.clip(
         new ClipContext(this.reverseTransformVec(source), transformedTarget, Block.OUTLINE, Fluid.NONE, CollisionContext.empty())
      );
      world.clearMask();
      double t = rayTraceBlocks.getLocation().subtract(transformedTarget).lengthSqr() / source.subtract(target).lengthSqr();
      Vec3 actualHit = VecHelper.lerp((float)t, target, source);
      return Pair.of(actualHit, rayTraceBlocks);
   }

   private Vec3 reverseTransformVec(Vec3 in) {
      float pt = AnimationTickHolder.getPartialTicks();
      in = in.subtract(VecHelper.lerp(pt, this.prevAnimatedOffset, this.animatedOffset));
      if (!this.animatedRotation.equals(Vec3.ZERO) || !this.prevAnimatedRotation.equals(Vec3.ZERO)) {
         double rotX = Mth.lerp((double)pt, this.prevAnimatedRotation.x, this.animatedRotation.x);
         double rotZ = Mth.lerp((double)pt, this.prevAnimatedRotation.z, this.animatedRotation.z);
         double rotY = Mth.lerp((double)pt, this.prevAnimatedRotation.y, this.animatedRotation.y);
         in = in.subtract(this.centerOfRotation);
         in = VecHelper.rotate(in, -rotX, Axis.X);
         in = VecHelper.rotate(in, -rotZ, Axis.Z);
         in = VecHelper.rotate(in, -rotY, Axis.Y);
         in = in.add(this.centerOfRotation);
         if (this.stabilizationAnchor != null) {
            in = in.subtract(this.stabilizationAnchor);
            in = VecHelper.rotate(in, rotX, Axis.X);
            in = VecHelper.rotate(in, rotZ, Axis.Z);
            in = VecHelper.rotate(in, rotY, Axis.Y);
            in = in.add(this.stabilizationAnchor);
         }
      }

      return in;
   }

   public void transformMS(PoseStack ms, float pt) {
      Vec3 vec = VecHelper.lerp(pt, this.prevAnimatedOffset, this.animatedOffset);
      ms.translate(vec.x, vec.y, vec.z);
      if (!this.animatedRotation.equals(Vec3.ZERO) || !this.prevAnimatedRotation.equals(Vec3.ZERO)) {
         double rotX = Mth.lerp((double)pt, this.prevAnimatedRotation.x, this.animatedRotation.x);
         double rotZ = Mth.lerp((double)pt, this.prevAnimatedRotation.z, this.animatedRotation.z);
         double rotY = Mth.lerp((double)pt, this.prevAnimatedRotation.y, this.animatedRotation.y);
         ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)TransformStack.of(ms).translate(this.centerOfRotation))
                     .rotateXDegrees((float)rotX))
                  .rotateYDegrees((float)rotY))
               .rotateZDegrees((float)rotZ))
            .translateBack(this.centerOfRotation);
         if (this.stabilizationAnchor != null) {
            ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)TransformStack.of(ms).translate(this.stabilizationAnchor))
                        .rotateXDegrees((float)(-rotX)))
                     .rotateYDegrees((float)(-rotY)))
                  .rotateZDegrees((float)(-rotZ)))
               .translateBack(this.stabilizationAnchor);
         }
      }
   }

   @Override
   public void tick(PonderScene scene) {
      this.prevAnimatedOffset = this.animatedOffset;
      this.prevAnimatedRotation = this.animatedRotation;
      if (this.isVisible()) {
         this.loadBEsIfMissing(scene.getWorld());
         this.renderedBlockEntities.removeIf(be -> scene.getWorld().getBlockEntity(be.getBlockPos()) != be);
         this.tickableBlockEntities.removeIf(be -> scene.getWorld().getBlockEntity(be.getFirst().getBlockPos()) != be.getFirst());
         this.tickableBlockEntities.forEach(be -> be.getSecond().accept(scene.getWorld()));
      }
   }

   @Override
   public void whileSkipping(PonderScene scene) {
      if (this.redraw) {
         this.renderedBlockEntities = null;
         this.tickableBlockEntities = null;
      }

      this.redraw = false;
   }

   protected void loadBEsIfMissing(PonderLevel world) {
      if (this.renderedBlockEntities == null) {
         this.tickableBlockEntities = new ArrayList<>();
         this.renderedBlockEntities = new ArrayList<>();
         this.section.forEach(pos -> {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            BlockState blockState = world.getBlockState(pos);
            net.minecraft.world.level.block.Block block = blockState.getBlock();
            if (blockEntity != null) {
               if (block instanceof EntityBlock) {
                  blockEntity.setBlockState(world.getBlockState(pos));
                  BlockEntityTicker<?> ticker = ((EntityBlock)block).getTicker(world, blockState, blockEntity.getType());
                  if (ticker != null) {
                     this.addTicker(blockEntity, ticker);
                  }

                  this.renderedBlockEntities.add(blockEntity);
               }
            }
         });
      }
   }

   private <T extends BlockEntity> void addTicker(T blockEntity, BlockEntityTicker<?> ticker) {
      this.tickableBlockEntities.add(Pair.of(blockEntity, w -> ticker.tick(w, blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity)));
   }

   @Override
   public void renderFirst(PonderLevel world, MultiBufferSource buffer, GuiGraphics graphics, float fade, float pt) {
      PoseStack poseStack = graphics.pose();
      int light = -1;
      if (fade != 1.0F) {
         light = (int)Mth.lerp(fade, 5.0F, 15.0F);
      }

      if (this.redraw) {
         this.renderedBlockEntities = null;
         this.tickableBlockEntities = null;
      }

      poseStack.pushPose();
      this.transformMS(poseStack, pt);
      world.pushFakeLight(light);
      this.renderBlockEntities(world, poseStack, buffer, pt);
      world.popLight();
      Map<BlockPos, Integer> blockBreakingProgressions = world.getBlockBreakingProgressions();
      PoseStack overlayMS = null;

      for (Entry<BlockPos, Integer> entry : blockBreakingProgressions.entrySet()) {
         BlockPos pos = entry.getKey();
         if (this.section.test(pos)) {
            if (overlayMS == null) {
               overlayMS = new PoseStack();
               overlayMS.last().pose().set(poseStack.last().pose());
               overlayMS.last().normal().set(poseStack.last().normal());
            }

            VertexConsumer builder = new SheetedDecalTextureGenerator(
               buffer.getBuffer((RenderType)ModelBakery.DESTROY_TYPES.get(entry.getValue())), overlayMS.last(), 1.0F
            );
            poseStack.pushPose();
            poseStack.translate((float)pos.getX(), (float)pos.getY(), (float)pos.getZ());
            Minecraft.getInstance().getBlockRenderer().renderBreakingTexture(world.getBlockState(pos), pos, world, poseStack, builder);
            poseStack.popPose();
         }
      }

      poseStack.popPose();
   }

   @Override
   protected void renderLayer(PonderLevel world, MultiBufferSource buffer, RenderType type, GuiGraphics graphics, float fade, float pt) {
      PoseStack poseStack = graphics.pose();
      SuperByteBufferCache bufferCache = SuperByteBufferCache.getInstance();
      int code = this.hashCode() ^ world.hashCode();
      Pair<Integer, Integer> key = Pair.of(code, RenderType.chunkBufferLayers().indexOf(type));
      if (this.redraw) {
         bufferCache.invalidate(PONDER_WORLD_SECTION, key);
      }

      SuperByteBuffer structureBuffer = bufferCache.get(PONDER_WORLD_SECTION, key, () -> this.buildStructureBuffer(world, type));
      if (!structureBuffer.isEmpty()) {
         this.transformMS(structureBuffer.getTransforms(), pt);
         int light = this.lightCoordsFromFade(fade);
         structureBuffer.<SuperByteBuffer>light(light).renderInto(poseStack, buffer.getBuffer(type));
      }
   }

   @Override
   protected void renderLast(PonderLevel world, MultiBufferSource buffer, GuiGraphics graphics, float fade, float pt) {
      PoseStack poseStack = graphics.pose();
      this.redraw = false;
      if (this.selectedBlock != null) {
         BlockState blockState = world.getBlockState(this.selectedBlock);
         if (!blockState.isAir()) {
            VoxelShape shape = blockState.getShape(world, this.selectedBlock, CollisionContext.of(Minecraft.getInstance().player));
            if (!shape.isEmpty()) {
               poseStack.pushPose();
               this.transformMS(poseStack, pt);
               poseStack.translate((float)this.selectedBlock.getX(), (float)this.selectedBlock.getY(), (float)this.selectedBlock.getZ());
               AABBOutline aabbOutline = new AABBOutline(shape.bounds());
               aabbOutline.getParams().lineWidth(0.015625F).colored(15724527).disableLineNormals();
               aabbOutline.render(poseStack, (SuperRenderTypeBuffer)buffer, Vec3.ZERO, pt);
               poseStack.popPose();
            }
         }
      }
   }

   private void renderBlockEntities(PonderLevel world, PoseStack ms, MultiBufferSource buffer, float pt) {
      this.loadBEsIfMissing(world);
      Iterator<BlockEntity> iterator = this.renderedBlockEntities.iterator();

      while (iterator.hasNext()) {
         BlockEntity tile = iterator.next();
         BlockEntityRenderer<BlockEntity> renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(tile);
         if (renderer == null) {
            iterator.remove();
         } else {
            BlockPos pos = tile.getBlockPos();
            ms.pushPose();
            ms.translate((float)pos.getX(), (float)pos.getY(), (float)pos.getZ());

            try {
               renderer.render(tile, pt, ms, buffer, LevelRenderer.getLightColor(world, pos), OverlayTexture.NO_OVERLAY);
            } catch (Exception var11) {
               iterator.remove();
               String message = "BlockEntity " + RegisteredObjectsHelper.getKeyOrThrow(tile.getType()) + " could not be rendered virtually.";
               Ponder.LOGGER.error(message, var11);
            }

            ms.popPose();
         }
      }
   }

   private SuperByteBuffer buildStructureBuffer(PonderLevel world, RenderType layer) {
      WorldSectionElementImpl.ThreadLocalObjects objects = THREAD_LOCAL_OBJECTS.get();
      WorldSectionElementImpl.SbbBuilder sbbBuilder = objects.sbbBuilder;
      sbbBuilder.prepare(layer);
      world.setMask(this.section);
      world.pushFakeLight(0);
      BakedModelBufferer.bufferBlocks(this.section.iterator(), world, null, true, sbbBuilder);
      world.popLight();
      world.clearMask();
      return sbbBuilder.build();
   }

   private static class SbbBuilder extends SuperByteBufferBuilder implements ShadeSeparatedResultConsumer {
      private RenderType renderType;

      public void prepare(RenderType renderType) {
         this.prepare();
         this.renderType = renderType;
      }

      @Override
      public void accept(RenderType renderType, boolean shaded, MeshData data) {
         if (renderType == this.renderType) {
            this.add(data, shaded);
         }
      }
   }

   private static class ThreadLocalObjects {
      public final WorldSectionElementImpl.SbbBuilder sbbBuilder = new WorldSectionElementImpl.SbbBuilder();
   }
}

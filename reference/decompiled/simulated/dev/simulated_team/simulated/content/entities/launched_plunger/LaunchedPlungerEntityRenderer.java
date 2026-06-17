package dev.simulated_team.simulated.content.entities.launched_plunger;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.items.plunger_launcher.PlungerLauncherItemRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.index.SimRenderTypes;
import dev.simulated_team.simulated.util.CatmulRomSpline;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class LaunchedPlungerEntityRenderer extends EntityRenderer<LaunchedPlungerEntity> {
   private static final Quaternionf POSITIVE_Y = new Quaternionf().setAngleAxis(Math.PI / 2, 1.0, 0.0, 0.0);
   private static final Quaternionf NEGATIVE_Y = new Quaternionf().setAngleAxis(-Math.PI / 2, 1.0, 0.0, 0.0);
   private static final Matrix4f FRUSTUM = new Matrix4f();
   private static final Matrix4f PROJECTION = new Matrix4f();
   private static final Quaternionf ORIENTATION = new Quaternionf();
   private static final Quaternionf NEXT_ORIENTATION = new Quaternionf();
   private static final Vector3f POS = new Vector3f();
   private static final Vector3f NORMAL = new Vector3f();
   private static final Vector3f NEXT_NORMAL = new Vector3f();
   private static final Vector3f FACE_NORMAL = new Vector3f();
   private static final Vector3f TARGET = new Vector3f();
   private static final Vector3f SELF = new Vector3f();
   private static final List<Vec3> CABLE_POINTS = new ArrayList<>();
   private static final List<Vec3> PREV_CABLE_POINTS = new ArrayList<>();
   private static final MutableBlockPos LIGHT_POS = new MutableBlockPos();

   public LaunchedPlungerEntityRenderer(Context context) {
      super(context);
   }

   public static Vec3 getFirstPersonFocusPos(float pt) {
      GameRenderer gameRenderer = Minecraft.getInstance().gameRenderer;
      Camera camera = gameRenderer.getMainCamera();
      Vector3d focusPoint = new Vector3d(PlungerLauncherItemRenderer.focusPos);
      Quaternionf orientation = camera.rotation();
      orientation.transformInverse(focusPoint);
      Vector4f v4 = new Vector4f((float)focusPoint.x, (float)focusPoint.y, (float)focusPoint.z, 1.0F);
      Matrix4f actualProjMat = gameRenderer.getProjectionMatrix(gameRenderer.getFov(camera, AnimationTickHolder.getPartialTicks(), true));
      actualProjMat.invert(new Matrix4f()).transform(v4);
      PlungerLauncherItemRenderer.itemProjMat.transform(v4);
      Vec3 cameraPosition = camera.getPosition();
      focusPoint.set((double)v4.x, (double)v4.y, (double)v4.z);
      orientation.transform(focusPoint);
      double fov = gameRenderer.getFov(camera, pt, true);
      focusPoint.mul(100.0 / fov);
      focusPoint.add(cameraPosition.x, cameraPosition.y, cameraPosition.z);
      return JOMLConversion.toMojang(focusPoint);
   }

   public void render(LaunchedPlungerEntity entity, float f, float pt, PoseStack poseStack, MultiBufferSource multiBufferSource, int light) {
      super.render(entity, f, pt, poseStack, multiBufferSource, light);
      LaunchedPlungerEntity other = entity.getOther();
      Vec3 selfNormal = Vec3.ZERO;
      Vec3 perpendicularNormal = Vec3.ZERO;
      Direction dir = entity.getData(LaunchedPlungerEntity.PLUNGED_DIRECTION);
      if (entity.isPlunged()) {
         selfNormal = Vec3.atLowerCornerOf(dir.getNormal());
         if (dir.getAxis().isHorizontal()) {
            perpendicularNormal = Vec3.atLowerCornerOf(Direction.UP.getNormal());
         } else {
            perpendicularNormal = Vec3.atLowerCornerOf(Direction.NORTH.getNormal());
         }
      } else {
         selfNormal = entity.calculateViewVector(-Mth.lerp(pt, entity.xRotO, entity.getXRot()), -Mth.lerp(pt, entity.yRotO, entity.getYRot())).reverse();
         perpendicularNormal = entity.calculateViewVector(-Mth.lerp(pt, entity.xRotO, entity.getXRot()), -Mth.lerp(pt, entity.yRotO, entity.getYRot()) - 90.0F)
            .reverse();
      }

      poseStack.pushPose();
      Vec3 oldPos = new Vec3(entity.xo, entity.yo, entity.zo);
      Vec3 newPos = entity.position();
      float scalingFactor = 0.6F;
      SubLevel subLevel = Sable.HELPER.getContainingClient(newPos);
      if (subLevel != null) {
         ClientSubLevel clientSubLevel = (ClientSubLevel)subLevel;
         Pose3dc clientPos = clientSubLevel.renderPose(pt);
         newPos = clientPos.transformPosition(newPos);
         selfNormal = clientPos.transformNormal(selfNormal);
         perpendicularNormal = clientPos.transformNormal(perpendicularNormal);
         Quaterniondc quaterniondc = clientPos.orientation();
         poseStack.mulPose(new Quaternionf(quaterniondc.x(), quaterniondc.y(), quaterniondc.z(), quaterniondc.w()).conjugate());
      }

      SubLevel oldSubLevel = Sable.HELPER.getContainingClient(oldPos);
      if (oldSubLevel != null) {
         ClientSubLevel clientSubLevel = (ClientSubLevel)oldSubLevel;
         Pose3dc clientPos = clientSubLevel.renderPose(pt);
         oldPos = clientPos.transformPosition(oldPos);
      }

      Vec3 renderPos = oldPos.lerp(newPos, (double)pt);
      Vec3 pos = renderPos.add(selfNormal.scale(0.6F));
      poseStack.translate(-renderPos.x, -renderPos.y, -renderPos.z);
      Vec3 target;
      if (other != null) {
         Vec3 otherNormal = Vec3.ZERO;
         if (other.isPlunged()) {
            Direction otherDir = other.getData(LaunchedPlungerEntity.PLUNGED_DIRECTION);
            otherNormal = Vec3.atLowerCornerOf(otherDir.getNormal());
         } else {
            otherNormal = other.calculateViewVector(-Mth.lerp(pt, other.xRotO, other.getXRot()), -Mth.lerp(pt, other.yRotO, other.getYRot())).reverse();
         }

         Vec3 targetOldPos = new Vec3(other.xo, other.yo, other.zo);
         Vec3 targetNewPos = other.position();
         if (other.isRemoved()) {
            targetOldPos = (Vec3)entity.getEntityData().get(LaunchedPlungerEntity.TARGET_POS);
            targetNewPos = targetOldPos;
         }

         SubLevel targetSublevel = Sable.HELPER.getContainingClient(targetNewPos);
         if (targetSublevel != null) {
            targetNewPos = ((ClientSubLevel)targetSublevel).renderPose(pt).transformPosition(targetNewPos);
            otherNormal = ((ClientSubLevel)targetSublevel).renderPose(pt).transformNormal(otherNormal);
         }

         SubLevel targetOldSublevel = Sable.HELPER.getContainingClient(other.getPosition(pt));
         if (targetOldSublevel != null) {
            targetOldPos = ((ClientSubLevel)targetOldSublevel).renderPose(pt).transformPosition(targetOldPos);
         }

         target = targetOldPos.lerp(targetNewPos, (double)pt).add(otherNormal.scale(0.6F));
      } else {
         Entity owner = entity.getOwner();
         if (entity.<Optional>getData(LaunchedPlungerEntity.OTHER_PLUNGER).isEmpty()
            && owner == Minecraft.getInstance().player
            && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            target = getFirstPersonFocusPos(pt);
         } else {
            label109: {
               if (owner instanceof AbstractClientPlayer player && entity.<Optional>getData(LaunchedPlungerEntity.OTHER_PLUNGER).isEmpty()) {
                  PlayerRenderer playerrenderer = (PlayerRenderer)Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
                  float headYDirection = Mth.lerp(pt, player.yHeadRotO, player.yHeadRot);
                  float bodyDifference = Math.abs(headYDirection - player.getPreciseBodyRotation(pt)) / 50.0F;
                  float headXDirection = Mth.lerp(pt, player.xRotO, player.getXRot());
                  float lookDelta = Math.abs(Mth.map(headXDirection, 90.0F, 0.0F, 1.0F, 0.0F));
                  headYDirection = Mth.lerp(lookDelta, headYDirection, player.getPreciseBodyRotation(pt));
                  Vec3 viewDirection = player.calculateViewVector(headXDirection, headYDirection);
                  Vec3 handDirection = player.calculateViewVector(0.0F, headYDirection + 90.0F);
                  target = player.getPosition(pt)
                     .add(0.0, 1.28, 0.0)
                     .add(viewDirection.scale(0.875))
                     .add(handDirection.scale((double)(Math.abs(Mth.map(headXDirection, 90.0F, 0.0F, 0.325F, 0.0F)) * 1.0F)));
                  break label109;
               }

               target = Vec3.ZERO;
            }
         }
      }

      Vector3f firstRotation = new Vector3f();
      Vector3f secondRotation = new Vector3f();
      Vector3f finalRotation = new Vector3f();
      poseStack.popPose();
      if ((entity.<Boolean>getData(LaunchedPlungerEntity.IS_FIRST) || other == null || other.isRemoved()) && !target.equals(Vec3.ZERO)) {
         float renderTime = (float)entity.tickCount + pt + entity.getAnimationOffset();
         List<Vec3> points = new ObjectArrayList();
         Vec3 start = pos;
         Vec3 toTarget = target.subtract(pos);
         Vec3 normalizedScalar = toTarget.normalize();
         float length = (float)renderPos.distanceTo(target);
         points.add(pos);
         if ((double)length < 1000.0) {
            for (float j = 0.01F; j < length; j += 0.5F) {
               finalRotation.set(0.0F, 0.0F, 0.0F);
               float delta = j / length;
               Vec3 point = start.add(toTarget.scale((double)delta));
               firstRotation.set(
                  Math.cos((double)(renderTime / 10.0F + j)) * (1.0 - Math.abs(normalizedScalar.x)),
                  Math.cos((double)(renderTime / 10.0F + j)) * (1.0 - Math.abs(normalizedScalar.y)),
                  Math.cos((double)(renderTime / 10.0F + j / 2.0F)) * (1.0 - Math.abs(normalizedScalar.z))
               );
               secondRotation.set(
                  Math.sin((double)(renderTime / 10.0F + j / 4.0F)) * (1.0 - Math.abs(normalizedScalar.x)) * 2.0,
                  Math.sin((double)(renderTime / 10.0F + j / 4.0F)) * (1.0 - Math.abs(normalizedScalar.y)),
                  Math.sin((double)(renderTime / 10.0F + j / 4.0F)) * (1.0 - Math.abs(normalizedScalar.z))
               );
               finalRotation.add(firstRotation);
               finalRotation.add(secondRotation);
               finalRotation.mul(
                  Math.max(
                     0.0F, 1.0F - ((float)entity.tickCount + pt) / 40.0F - ((float)entity.getPlungedTime() + (entity.getPlungedTime() > 0 ? pt : 0.0F)) / 8.0F
                  )
               );
               finalRotation.mul((float)(1.0 - Math.pow((double)(2.0F * delta - 1.0F), 2.0)));
               points.add(point.subtract((double)finalRotation.x, (double)finalRotation.y, (double)finalRotation.z));
            }
         }

         points.add(start.add(toTarget));
         points.add(start.add(toTarget));
         renderRope(points, multiBufferSource, Minecraft.getInstance().level, poseStack);
      }

      double distanceIncludingSublevels = pos.distanceTo(target);
      PoseTransformStack stack = TransformStack.of(poseStack);
      poseStack.pushPose();
      if (entity.isPlunged()) {
         stack.rotate(dir.getRotation());
      } else {
         poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(pt, entity.yRotO, entity.getYRot()) - 90.0F));
         poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(pt, entity.xRotO, entity.getXRot())));
         stack.rotateZDegrees(90.0F);
      }

      stack.rotateXDegrees(-90.0F);
      stack.scale(1.75F, 1.75F, 1.75F);
      stack.translate(0.0F, 0.0F, 0.15625F);
      VertexConsumer vb = multiBufferSource.getBuffer(RenderType.solid());
      SuperByteBuffer body = CachedBuffers.partial(SimPartialModels.LAUNCHED_PLUNGER_BODY, Blocks.AIR.defaultBlockState());
      SuperByteBuffer spool = CachedBuffers.partial(SimPartialModels.LAUNCHED_PLUNGER_SPOOL, Blocks.AIR.defaultBlockState());
      SuperByteBuffer joint = CachedBuffers.partial(SimPartialModels.LAUNCHED_PLUNGER_JOINT, Blocks.AIR.defaultBlockState());
      stack.rotateZDegrees(90.0F);
      body.light(light).renderInto(poseStack, vb);
      FACE_NORMAL.set(selfNormal.x, selfNormal.y, selfNormal.z);
      SELF.set(pos.x, pos.y, pos.z);
      TARGET.set(target.x, target.y, target.z);
      TARGET.add(SELF.mul(-1.0F)).normalize();
      POS.set(perpendicularNormal.x, perpendicularNormal.y, perpendicularNormal.z);
      float angle = 0.0F;
      if (entity.<Boolean>getData(LaunchedPlungerEntity.IS_PLUNGED)) {
         angle = (float)((double)POS.angleSigned(TARGET, FACE_NORMAL) + (Math.PI / 2));
      }

      if (Float.isNaN(angle)) {
         angle = 0.0F;
      }

      poseStack.pushPose();
      stack.rotateZDegrees((float)Math.toDegrees((double)angle));
      joint.light(light).renderInto(poseStack, vb);
      stack.translate(0.0F, 0.0F, 0.1875F);
      stack.rotateXDegrees((float)distanceIncludingSublevels * 90.0F * 2.6F);
      spool.light(light).renderInto(poseStack, vb);
      poseStack.popPose();
      poseStack.popPose();
   }

   public static void renderRope(List<Vec3> positions, MultiBufferSource multiBufferSource, BlockAndTintGetter level, PoseStack poseStack) {
      Vec3 first = positions.getFirst();
      Vector3d origin = new Vector3d();
      Vec3 cameraPosition = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
      RenderType renderType = SimRenderTypes.rope();
      VertexConsumer builder = multiBufferSource.getBuffer(renderType);
      CABLE_POINTS.clear();

      for (Vec3 position : positions) {
         CABLE_POINTS.add(position.subtract(cameraPosition));
      }

      List<Vec3> splinePoints = CatmulRomSpline.generateSpline(CABLE_POINTS, 4);
      int color = -1;
      float constantRadius = 0.0625F;
      float u = 0.125F;
      float v = 0.0F;

      for (int i = 0; i < splinePoints.size() - 1; i++) {
         float delta = (float)i / (float)(splinePoints.size() - 1);
         float nextDelta = (float)(i + 1) / (float)(splinePoints.size() - 1);
         float cableRadius = 0.0625F - 0.001F * delta;
         float nextCableRadius = 0.0625F - 0.001F * nextDelta;
         Vec3 point = splinePoints.get(i);
         Vec3 nextPoint = splinePoints.get(i + 1);
         double x = point.x;
         double y = point.y;
         double z = point.z;
         double nextX = nextPoint.x;
         double nextY = nextPoint.y;
         double nextZ = nextPoint.z;
         if (i < splinePoints.size() - 2) {
            calculateOrientation(NEXT_ORIENTATION, nextX, nextY, nextZ, splinePoints.get(i + 2));
         } else {
            NEXT_ORIENTATION.set(ORIENTATION);
         }

         int lightStart = LevelRenderer.getLightColor(level, LIGHT_POS.set(x + cameraPosition.x, y + cameraPosition.y, z + cameraPosition.z));
         int lightEnd = LevelRenderer.getLightColor(level, LIGHT_POS.set(nextX + cameraPosition.x, nextY + cameraPosition.y, nextZ + cameraPosition.z));
         double length = Math.sqrt((nextX - x) * (nextX - x) + (nextY - y) * (nextY - y) + (nextZ - z) * (nextZ - z));
         float nextV = v + (float)(length * 1.0625);
         ORIENTATION.transform(NORMAL.set(0.0F, -1.0F, 0.0F));
         NEXT_ORIENTATION.transform(NEXT_NORMAL.set(0.0F, -1.0F, 0.0F));
         NEXT_ORIENTATION.transform(POS.set(-nextCableRadius, -nextCableRadius, 0.0F));
         builder.addVertex(
               (float)(nextX - origin.x() + (double)POS.x), (float)(nextY - origin.y() + (double)POS.y), (float)(nextZ - origin.z() + (double)POS.z)
            )
            .setColor(-1)
            .setUv(0.0F, nextV)
            .setLight(lightEnd)
            .setNormal(NEXT_NORMAL.x, NEXT_NORMAL.y, NEXT_NORMAL.z);
         ORIENTATION.transform(POS.set(-cableRadius, -cableRadius, 0.0F));
         builder.addVertex((float)(x - origin.x() + (double)POS.x), (float)(y - origin.y() + (double)POS.y), (float)(z - origin.z() + (double)POS.z))
            .setColor(-1)
            .setUv(0.0F, v)
            .setLight(lightStart)
            .setNormal(NORMAL.x, NORMAL.y, NORMAL.z);
         ORIENTATION.transform(POS.set(cableRadius, -cableRadius, 0.0F));
         builder.addVertex((float)(x - origin.x() + (double)POS.x), (float)(y - origin.y() + (double)POS.y), (float)(z - origin.z() + (double)POS.z))
            .setColor(-1)
            .setUv(0.125F, v)
            .setLight(lightStart)
            .setNormal(NORMAL.x, NORMAL.y, NORMAL.z);
         NEXT_ORIENTATION.transform(POS.set(nextCableRadius, -nextCableRadius, 0.0F));
         builder.addVertex(
               (float)(nextX - origin.x() + (double)POS.x), (float)(nextY - origin.y() + (double)POS.y), (float)(nextZ - origin.z() + (double)POS.z)
            )
            .setColor(-1)
            .setUv(0.125F, nextV)
            .setLight(lightEnd)
            .setNormal(NEXT_NORMAL.x, NEXT_NORMAL.y, NEXT_NORMAL.z);
         ORIENTATION.transform(NORMAL.set(0.0F, 1.0F, 0.0F));
         NEXT_ORIENTATION.transform(NEXT_NORMAL.set(0.0F, 1.0F, 0.0F));
         ORIENTATION.transform(POS.set(-cableRadius, cableRadius, 0.0F));
         builder.addVertex((float)(x - origin.x() + (double)POS.x), (float)(y - origin.y() + (double)POS.y), (float)(z - origin.z() + (double)POS.z))
            .setColor(-1)
            .setUv(0.0F, v)
            .setLight(lightStart)
            .setNormal(NORMAL.x, NORMAL.y, NORMAL.z);
         NEXT_ORIENTATION.transform(POS.set(-nextCableRadius, nextCableRadius, 0.0F));
         builder.addVertex(
               (float)(nextX - origin.x() + (double)POS.x), (float)(nextY - origin.y() + (double)POS.y), (float)(nextZ - origin.z() + (double)POS.z)
            )
            .setColor(-1)
            .setUv(0.0F, nextV)
            .setLight(lightEnd)
            .setNormal(NEXT_NORMAL.x, NEXT_NORMAL.y, NEXT_NORMAL.z);
         NEXT_ORIENTATION.transform(POS.set(nextCableRadius, nextCableRadius, 0.0F));
         builder.addVertex(
               (float)(nextX - origin.x() + (double)POS.x), (float)(nextY - origin.y() + (double)POS.y), (float)(nextZ - origin.z() + (double)POS.z)
            )
            .setColor(-1)
            .setUv(0.125F, nextV)
            .setLight(lightEnd)
            .setNormal(NEXT_NORMAL.x, NEXT_NORMAL.y, NEXT_NORMAL.z);
         ORIENTATION.transform(POS.set(cableRadius, cableRadius, 0.0F));
         builder.addVertex((float)(x - origin.x() + (double)POS.x), (float)(y - origin.y() + (double)POS.y), (float)(z - origin.z() + (double)POS.z))
            .setColor(-1)
            .setUv(0.125F, v)
            .setLight(lightStart)
            .setNormal(NORMAL.x, NORMAL.y, NORMAL.z);
         ORIENTATION.transform(NORMAL.set(-1.0F, 0.0F, 0.0F));
         NEXT_ORIENTATION.transform(NEXT_NORMAL.set(-1.0F, 0.0F, 0.0F));
         NEXT_ORIENTATION.transform(POS.set(-nextCableRadius, -nextCableRadius, 0.0F));
         builder.addVertex(
               (float)(nextX - origin.x() + (double)POS.x), (float)(nextY - origin.y() + (double)POS.y), (float)(nextZ - origin.z() + (double)POS.z)
            )
            .setColor(-1)
            .setUv(0.125F, nextV)
            .setLight(lightEnd)
            .setNormal(NEXT_NORMAL.x, NEXT_NORMAL.y, NEXT_NORMAL.z);
         NEXT_ORIENTATION.transform(POS.set(-nextCableRadius, nextCableRadius, 0.0F));
         builder.addVertex(
               (float)(nextX - origin.x() + (double)POS.x), (float)(nextY - origin.y() + (double)POS.y), (float)(nextZ - origin.z() + (double)POS.z)
            )
            .setColor(-1)
            .setUv(0.0F, nextV)
            .setLight(lightEnd)
            .setNormal(NEXT_NORMAL.x, NEXT_NORMAL.y, NEXT_NORMAL.z);
         ORIENTATION.transform(POS.set(-cableRadius, cableRadius, 0.0F));
         builder.addVertex((float)(x - origin.x() + (double)POS.x), (float)(y - origin.y() + (double)POS.y), (float)(z - origin.z() + (double)POS.z))
            .setColor(-1)
            .setUv(0.0F, v)
            .setLight(lightStart)
            .setNormal(NORMAL.x, NORMAL.y, NORMAL.z);
         ORIENTATION.transform(POS.set(-cableRadius, -cableRadius, 0.0F));
         builder.addVertex((float)(x - origin.x() + (double)POS.x), (float)(y - origin.y() + (double)POS.y), (float)(z - origin.z() + (double)POS.z))
            .setColor(-1)
            .setUv(0.125F, v)
            .setLight(lightStart)
            .setNormal(NORMAL.x, NORMAL.y, NORMAL.z);
         ORIENTATION.transform(NORMAL.set(1.0F, 0.0F, 0.0F));
         NEXT_ORIENTATION.transform(NEXT_NORMAL.set(1.0F, 0.0F, 0.0F));
         ORIENTATION.transform(POS.set(cableRadius, -cableRadius, 0.0F));
         builder.addVertex((float)(x - origin.x() + (double)POS.x), (float)(y - origin.y() + (double)POS.y), (float)(z - origin.z() + (double)POS.z))
            .setColor(-1)
            .setUv(0.125F, v)
            .setLight(lightStart)
            .setNormal(NORMAL.x, NORMAL.y, NORMAL.z);
         ORIENTATION.transform(POS.set(cableRadius, cableRadius, 0.0F));
         builder.addVertex((float)(x - origin.x() + (double)POS.x), (float)(y - origin.y() + (double)POS.y), (float)(z - origin.z() + (double)POS.z))
            .setColor(-1)
            .setUv(0.0F, v)
            .setLight(lightStart)
            .setNormal(NORMAL.x, NORMAL.y, NORMAL.z);
         NEXT_ORIENTATION.transform(POS.set(nextCableRadius, nextCableRadius, 0.0F));
         builder.addVertex(
               (float)(nextX - origin.x() + (double)POS.x), (float)(nextY - origin.y() + (double)POS.y), (float)(nextZ - origin.z() + (double)POS.z)
            )
            .setColor(-1)
            .setUv(0.0F, nextV)
            .setLight(lightEnd)
            .setNormal(NEXT_NORMAL.x, NEXT_NORMAL.y, NEXT_NORMAL.z);
         NEXT_ORIENTATION.transform(POS.set(nextCableRadius, -nextCableRadius, 0.0F));
         builder.addVertex(
               (float)(nextX - origin.x() + (double)POS.x), (float)(nextY - origin.y() + (double)POS.y), (float)(nextZ - origin.z() + (double)POS.z)
            )
            .setColor(-1)
            .setUv(0.125F, nextV)
            .setLight(lightEnd)
            .setNormal(NEXT_NORMAL.x, NEXT_NORMAL.y, NEXT_NORMAL.z);
         ORIENTATION.set(NEXT_ORIENTATION);
         v = nextV;
      }
   }

   private static void calculateOrientation(Quaternionf store, double x, double y, double z, Vec3 nextPoint) {
      double dx = nextPoint.x - x;
      double dy = nextPoint.y - y;
      double dz = nextPoint.z - z;
      float factor = 0.0F;
      store.identity()
         .rotateAxis((float)Math.atan2(dx, dz), 0.0F, 1.0F, 0.0F)
         .rotateAxis((float)(Math.acos(dy / Math.sqrt(dx * dx + dy * dy + dz * dz)) - (Math.PI / 2)), 1.0F, 0.0F, 0.0F)
         .slerp(dy < 0.0 ? POSITIVE_Y : NEGATIVE_Y, 0.0F);
   }

   public ResourceLocation getTextureLocation(LaunchedPlungerEntity entity) {
      return ResourceLocation.withDefaultNamespace("missing");
   }

   public boolean shouldRender(LaunchedPlungerEntity entity, Frustum frustum, double d, double e, double f) {
      return true;
   }
}

package dev.ryanhcode.sable.debug;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import foundry.veil.api.client.render.MatrixStack;
import foundry.veil.api.event.VeilRenderLevelStageEvent.Stage;
import foundry.veil.platform.VeilEventPlatform;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3d;
import org.joml.Vector4f;
import org.joml.Vector4fc;

public class SableClientGizmoHandler {
   private Vec3 mouseDir = Vec3.ZERO;
   private boolean enabled = false;
   @Nullable
   private GizmoSelection selection;

   public void init() {
      VeilEventPlatform.INSTANCE.onVeilRenderLevelStage(this::onRenderStage);
   }

   public static Vec3 getRay(Matrix4fc projectionMatrix, float normalizedMouseX, float normalizedMouseY) {
      Vector4f clipCoords = new Vector4f(-normalizedMouseX, -normalizedMouseY, -1.0F, 0.0F);
      Vector4f eyeSpace = toEyeCoords(projectionMatrix, clipCoords);
      return new Vec3((double)eyeSpace.x, (double)eyeSpace.y, (double)eyeSpace.z).normalize();
   }

   private static Vector4f toEyeCoords(Matrix4fc projectionMatrix, Vector4fc clipCoords) {
      Matrix4f inverse = projectionMatrix.invert(new Matrix4f());
      Vector4f result = new Vector4f(clipCoords.x(), clipCoords.y(), clipCoords.z(), clipCoords.w());
      result.mul(inverse);
      result.set(result.x(), result.y(), 1.0F, 0.0F);
      return result;
   }

   @Nullable
   public GizmoSelection getSelection() {
      return this.selection;
   }

   private void onRenderStage(
      Stage stage,
      LevelRenderer levelRenderer,
      BufferSource bufferSource,
      MatrixStack matrixStack,
      Matrix4fc modelViewMat,
      Matrix4fc projMat,
      int renderTicks,
      DeltaTracker deltaTracker,
      Camera camera,
      Frustum frustum
   ) {
      if (stage == Stage.AFTER_WEATHER) {
         if (this.enabled) {
            float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);
            Minecraft minecraft = Minecraft.getInstance();
            ClientLevel level = minecraft.level;
            Vec3 cameraPos = camera.getPosition();
            SubLevelContainer container = SubLevelContainer.getContainer(level);

            assert container != null;

            this.updateMouseDir(minecraft, partialTicks);
            this.updateSelection();
            PoseStack poseStack = new PoseStack();

            for (SubLevel subLevel : container.getAllSubLevels()) {
               ClientSubLevel clientSubLevel = (ClientSubLevel)subLevel;
               Pose3dc renderPose = clientSubLevel.renderPose();
               Vector3d renderPos = renderPose.position().sub(cameraPos.x, cameraPos.y, cameraPos.z, new Vector3d());
               poseStack.pushPose();
               poseStack.translate(renderPos.x, renderPos.y, renderPos.z);
               DebugRenderer.renderFilledBox(poseStack, bufferSource, new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0).inflate(0.1), 1.0F, 1.0F, 1.0F, 0.4F);

               for (Axis axis : Axis.VALUES) {
                  Direction dir = Direction.get(AxisDirection.POSITIVE, axis);
                  Vec3i normal = dir.getNormal();
                  float r = (float)(Math.max((double)normal.getX(), 0.2) * 0.8);
                  float g = (float)(Math.max((double)normal.getY(), 0.2) * 0.8);
                  float b = (float)(Math.max((double)normal.getZ(), 0.2) * 0.8);
                  Vec3 normalD = new Vec3((double)normal.getX(), (double)normal.getY(), (double)normal.getZ());
                  Vec3 expandDir = normalD.scale(2.0);
                  float inflation = 0.04F;
                  AABB bb = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0).inflate(0.04F).move(normalD.scale(0.125)).expandTowards(expandDir);
                  if (this.selection != null && this.selection.subLevel().equals(clientSubLevel.getUniqueId()) && this.selection.axis() == axis) {
                     r *= 1.2F;
                     g *= 1.2F;
                     b *= 1.2F;
                  }

                  DebugRenderer.renderFilledBox(poseStack, bufferSource, bb, r, g, b, 0.9F);
               }

               poseStack.popPose();
            }
         }
      }
   }

   private void updateSelection() {
      Minecraft minecraft = Minecraft.getInstance();
      ClientLevel level = minecraft.level;
      Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();
      PoseStack poseStack = new PoseStack();
      SubLevelContainer container = SubLevelContainer.getContainer(level);

      assert container != null;

      for (SubLevel subLevel : container.getAllSubLevels()) {
         ClientSubLevel clientSubLevel = (ClientSubLevel)subLevel;
         Pose3dc renderPose = clientSubLevel.renderPose();
         Vector3d renderPos = renderPose.position().sub(cameraPos.x, cameraPos.y, cameraPos.z, new Vector3d());
         poseStack.pushPose();
         poseStack.translate(renderPos.x, renderPos.y, renderPos.z);

         for (Axis axis : Axis.VALUES) {
            Direction dir = Direction.get(AxisDirection.POSITIVE, axis);
            Vec3i normal = dir.getNormal();
            Vec3 normalD = new Vec3((double)normal.getX(), (double)normal.getY(), (double)normal.getZ());
            Vec3 expandDir = normalD.scale(2.0);
            float inflation = 0.04F;
            AABB bb = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0).inflate(0.04F).move(normalD.scale(0.125)).expandTowards(expandDir);
            if (bb.move(renderPos.x, renderPos.y, renderPos.z).inflate(0.1F).clip(Vec3.ZERO, this.mouseDir.scale(100.0)).isPresent()) {
               this.selection = new GizmoSelection(clientSubLevel.getUniqueId(), axis);
               return;
            }
         }
      }

      this.selection = null;
   }

   private void updateMouseDir(Minecraft minecraft, float partialTicks) {
      LocalPlayer player = minecraft.player;
      Window window = minecraft.getWindow();
      MouseHandler mouseHandler = minecraft.mouseHandler;
      double xPos = mouseHandler.xpos() / (double)window.getScreenWidth() * 2.0 - 1.0;
      double yPos = mouseHandler.ypos() / (double)window.getScreenHeight() * 2.0 - 1.0;
      GameRenderer gameRenderer = minecraft.gameRenderer;
      double fov = gameRenderer.getFov(gameRenderer.getMainCamera(), partialTicks, true);
      Matrix4f proj = gameRenderer.getProjectionMatrix(fov);
      float yaw = player.getViewYRot(partialTicks);
      float pitch = player.getViewXRot(partialTicks);
      this.mouseDir = getRay(proj, (float)xPos, (float)yPos).xRot((float)(-Math.toRadians((double)pitch))).yRot((float)(-Math.toRadians((double)yaw)));
   }

   public void start() {
      Minecraft minecraft = Minecraft.getInstance();
      minecraft.setScreen(new GizmoScreen());
      this.enabled = true;
   }

   public void stop() {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.screen instanceof GizmoScreen) {
         minecraft.setScreen(null);
      }

      this.enabled = false;
   }

   public Vec3 getMouseDir() {
      return this.mouseDir;
   }
}

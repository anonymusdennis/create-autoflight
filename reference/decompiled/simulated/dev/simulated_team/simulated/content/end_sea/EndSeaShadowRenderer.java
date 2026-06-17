package dev.simulated_team.simulated.content.end_sea;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.void_anchor.VoidAnchorBlockEntity;
import dev.simulated_team.simulated.util.SimpleSubLevelGroupRenderer;
import foundry.veil.api.client.render.MatrixStack;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import foundry.veil.api.event.VeilRenderLevelStageEvent.Stage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class EndSeaShadowRenderer {
   public static final float SHADOW_VOLUME_RADIUS = 128.0F;
   private static final Matrix4f PROJECTION_MAT = new Matrix4f();
   private static final Vector3d SHADOW_CAMERA_POSITION = new Vector3d();
   private static boolean isRenderingShadowMap = false;
   private static final ObjectArrayList<Vector3dc> voidAnchors = new ObjectArrayList();

   public static boolean isEnabled() {
      return true;
   }

   public static void renderShadowMap(
      Stage stage,
      LevelRenderer levelRenderer,
      BufferSource bufferSource,
      MatrixStack matrixStack,
      Matrix4fc frustumMatrix,
      Matrix4fc projectionMatrix,
      int renderTick,
      DeltaTracker deltaTracker,
      Camera camera,
      Frustum frustum
   ) {
      if (isEnabled() && stage == Stage.AFTER_LEVEL) {
         Minecraft minecraft = Minecraft.getInstance();
         ClientLevel level = minecraft.level;
         EndSeaPhysics physics = EndSeaPhysicsData.of(level);
         if (physics != null) {
            AdvancedFbo fbo = getShadowsFramebuffer();
            if (fbo != null) {
               float zNear = 0.5F;
               Matrix4f modelView = new Matrix4f();
               PROJECTION_MAT.identity().ortho(-128.0F, 128.0F, -128.0F, 128.0F, 0.5F, 128.0F);
               Vec3 cameraPosition = camera.getPosition();
               Vec3 shadowCameraPosition = new Vec3(cameraPosition.x, physics.startY() - 128.0, cameraPosition.z);
               SHADOW_CAMERA_POSITION.set(JOMLConversion.toJOML(shadowCameraPosition));
               SHADOW_CAMERA_POSITION.set(Math.floor(SHADOW_CAMERA_POSITION.x), SHADOW_CAMERA_POSITION.y, Math.floor(SHADOW_CAMERA_POSITION.z));
               isRenderingShadowMap = true;
               Quaternionf orientation = new Quaternionf().rotateX((float) (-Math.PI / 2));
               BoundingBox3dc bounds = new BoundingBox3d(-3.0E7, -10000.0, -3.0E7, 3.0E7, 10000.0, 3.0E7);
               List<ClientSubLevel> clientSubLevelGroup = new ObjectArrayList();

               for (SubLevel subLevel : Sable.HELPER.getAllIntersecting(level, bounds)) {
                  clientSubLevelGroup.add((ClientSubLevel)subLevel);
               }

               fbo.bind(true);
               fbo.clear();
               SimpleSubLevelGroupRenderer.renderGroup(
                  level, clientSubLevelGroup, fbo, modelView, PROJECTION_MAT, SHADOW_CAMERA_POSITION, orientation, 8.0F, false
               );
               isRenderingShadowMap = false;
               PostProcessingManager post = VeilRenderSystem.renderer().getPostProcessingManager();
               PostPipeline pipeline = post.getPipeline(Simulated.path("spread_end_sea"));
               if (pipeline != null) {
                  for (int i = 0; i < 5; i++) {
                     post.runPipeline(pipeline, false);
                  }
               }
            }
         }
      }
   }

   public static void renderVoidAnchors(Camera camera) {
      if (!voidAnchors.isEmpty()) {
         Minecraft minecraft = Minecraft.getInstance();
         RenderSystem.setShaderTexture(0, Simulated.path("textures/effects/cracks.png"));
         RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
         ShaderInstance shader = RenderSystem.getShader();
         if (shader != null) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            shader.setDefaultUniforms(Mode.QUADS, RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), minecraft.getWindow());
            shader.apply();
            BufferBuilder builder = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            Vector3d pos = new Vector3d();
            Vec3 cameraPos = camera.getPosition();
            ObjectListIterator var6 = voidAnchors.iterator();

            while (var6.hasNext()) {
               Vector3dc voidAnchor = (Vector3dc)var6.next();
               float size = 60.0F;
               voidAnchor.sub(cameraPos.x, cameraPos.y, cameraPos.z, pos);
               Matrix4f pose = new Matrix4f().translate((float)pos.x, (float)pos.y, (float)pos.z);
               builder.addVertex(pose, -60.0F, 0.0F, -60.0F).setUv(0.0F, 0.0F).setColor(0.5F, 0.0F, 0.0F, 1.0F);
               builder.addVertex(pose, 60.0F, 0.0F, -60.0F).setUv(1.0F, 0.0F).setColor(0.5F, 0.0F, 0.0F, 1.0F);
               builder.addVertex(pose, 60.0F, 0.0F, 60.0F).setUv(1.0F, 1.0F).setColor(0.5F, 0.0F, 0.0F, 1.0F);
               builder.addVertex(pose, -60.0F, 0.0F, 60.0F).setUv(0.0F, 1.0F).setColor(0.5F, 0.0F, 0.0F, 1.0F);
            }

            BufferUploader.drawWithShader(builder.buildOrThrow());
            RenderSystem.disableDepthTest();
            shader.clear();
            voidAnchors.clear();
         }
      }
   }

   @Nullable
   public static AdvancedFbo getShadowsFramebuffer() {
      return VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(Simulated.path("end_sea_shadows"));
   }

   public static boolean renderingShadowMap() {
      return isRenderingShadowMap;
   }

   public static Vector3dc getLastRenderOrigin() {
      return SHADOW_CAMERA_POSITION;
   }

   public static void addVoidAnchor(VoidAnchorBlockEntity voidAnchor) {
      voidAnchors.add(Sable.HELPER.projectOutOfSubLevel(voidAnchor.getLevel(), JOMLConversion.atCenterOf(voidAnchor.getBlockPos())));
   }
}

package dev.simulated_team.simulated.content.physics_staff;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.simibubi.create.AllSpecialTextures;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.LevelPoseProviderExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.SimulatedClient;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.index.SimRenderTypes;
import foundry.veil.api.client.color.Color;
import foundry.veil.api.client.render.MatrixStack;
import foundry.veil.api.event.VeilRenderLevelStageEvent.Stage;
import java.util.List;
import java.util.UUID;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.joml.Vector3dc;

public class PhysicsStaffRenderHandler {
   @Nullable
   private static BlockPos hoverBlockPos = null;

   public static void renderSelectionBox(
      Stage stage,
      LevelRenderer renderer,
      BufferSource bufferSource,
      MatrixStack ps,
      Matrix4fc frustrumMat,
      Matrix4fc projectionMat,
      int renderTick,
      DeltaTracker tracker,
      Camera camera,
      Frustum frustrum
   ) {
      if (stage == Stage.AFTER_TRANSLUCENT_BLOCKS) {
         if (!Minecraft.getInstance().options.hideGui) {
            ps.matrixPush();
            SimulatedClient.PHYSICS_STAFF_CLIENT_HANDLER.onRender(ps.toPoseStack());
            ps.matrixPop();
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            if (player.getItemInHand(InteractionHand.MAIN_HAND).is(SimItems.PHYSICS_STAFF)
               || player.getItemInHand(InteractionHand.OFF_HAND).is(SimItems.PHYSICS_STAFF)) {
               Vec3 cameraPos = camera.getPosition();
               Level level = player.level();
               renderAllLocks(bufferSource, ps, level, cameraPos);
               updateHoverPos(minecraft, player);
               if (hoverBlockPos != null) {
                  Color color = new Color(0.7490196F, 0.7490196F, 0.7490196F, 1.0F);
                  Outliner.getInstance()
                     .showCluster("physicsStaffSelection", List.of(hoverBlockPos))
                     .colored(color.rgb())
                     .disableLineNormals()
                     .lineWidth(0.03125F)
                     .withFaceTexture(AllSpecialTextures.CHECKERED);
               }
            }
         }
      }
   }

   private static void updateHoverPos(Minecraft minecraft, LocalPlayer player) {
      ClientLevel level = minecraft.level;
      float partialTicks = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
      hoverBlockPos = null;
      PhysicsStaffClientHandler.ClientDragSession dragSession = SimulatedClient.PHYSICS_STAFF_CLIENT_HANDLER.getDragSession();
      if (dragSession != null) {
         Vector3dc localAnchor = dragSession.dragLocalAnchor();
         hoverBlockPos = BlockPos.containing(localAnchor.x(), localAnchor.y(), localAnchor.z());
      } else {
         LevelPoseProviderExtension extension = (LevelPoseProviderExtension)level;
         extension.sable$pushPoseSupplier(x -> ((ClientSubLevel)x).renderPose());
         HitResult hit = player.pick((double)PhysicsStaffItem.RANGE, partialTicks, false);
         extension.sable$popPoseSupplier();
         if (hit instanceof BlockHitResult blockHitResult && blockHitResult.getType() != Type.MISS) {
            Vec3 hitLocation = hit.getLocation();
            SubLevel subLevel = Sable.HELPER.getContaining(level, hitLocation);
            if (subLevel == null) {
               return;
            }

            hoverBlockPos = blockHitResult.getBlockPos();
            return;
         }
      }
   }

   private static void renderAllLocks(BufferSource bufferSource, MatrixStack ps, Level level, Vec3 cameraPos) {
      Minecraft client = Minecraft.getInstance();
      List<UUID> locks = SimulatedClient.PHYSICS_STAFF_CLIENT_HANDLER.getLocks(level);
      SubLevelContainer container = SubLevelContainer.getContainer(level);

      for (UUID lock : locks) {
         if (container.getSubLevel(lock) instanceof ClientSubLevel clientSubLevel) {
            ps.matrixPush();
            Vector3dc renderPos = clientSubLevel.renderPose().position();
            ps.translate(renderPos.x() - cameraPos.x(), renderPos.y() - cameraPos.y(), renderPos.z() - cameraPos.z());
            ps.rotate(client.getEntityRenderDispatcher().cameraOrientation());
            VertexConsumer buffer = bufferSource.getBuffer(SimRenderTypes.lock());
            Pose pose = ps.pose();
            int color = -1;
            buffer.addVertex(pose, -0.5F, -0.5F, 0.0F).setColor(-1).setUv(0.0F, 1.0F).setLight(15728880);
            buffer.addVertex(pose, -0.5F, 0.5F, 0.0F).setColor(-1).setUv(0.0F, 0.0F).setLight(15728880);
            buffer.addVertex(pose, 0.5F, 0.5F, 0.0F).setColor(-1).setUv(1.0F, 0.0F).setLight(15728880);
            buffer.addVertex(pose, 0.5F, -0.5F, 0.0F).setColor(-1).setUv(1.0F, 1.0F).setLight(15728880);
            ps.matrixPop();
         }
      }
   }
}

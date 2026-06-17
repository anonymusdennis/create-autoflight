package dev.ryanhcode.sable.sublevel.render.dispatcher;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderer;
import foundry.veil.api.client.render.CullFrustum;
import java.util.Collection;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Matrix4f;
import org.lwjgl.system.NativeResource;

@Internal
public interface SubLevelRenderDispatcher extends NativeResource, ResourceManagerReloadListener {
   static SubLevelRenderDispatcher get() {
      return SubLevelRenderer.getDispatcher();
   }

   SubLevelRenderData resize(ClientSubLevel var1, SubLevelRenderData var2);

   SubLevelRenderData createRenderData(ClientSubLevel var1);

   default void rebuild(Iterable<ClientSubLevel> sublevels) {
      for (ClientSubLevel sublevel : sublevels) {
         sublevel.getRenderData().rebuild();
      }
   }

   void updateCulling(Iterable<ClientSubLevel> var1, double var2, double var4, double var6, CullFrustum var8, boolean var9);

   void renderSectionLayer(
      Iterable<ClientSubLevel> var1, RenderType var2, ShaderInstance var3, double var4, double var6, double var8, Matrix4f var10, Matrix4f var11, float var12
   );

   void renderAfterSections(Iterable<ClientSubLevel> var1, double var2, double var4, double var6, Matrix4f var8, Matrix4f var9, float var10);

   void renderBlockEntities(Iterable<ClientSubLevel> var1, SubLevelRenderDispatcher.BlockEntityRenderer var2, double var3, double var5, double var7, float var9);

   void addDebugInfo(Consumer<String> var1);

   default void preRenderChunks(Camera camera) {
   }

   public interface BlockEntityRenderer {
      default void renderBlockEntities(
         Collection<BlockEntity> blockEntities, PoseStack poseStack, float partialTick, double cameraX, double cameraY, double cameraZ
      ) {
         for (BlockEntity blockEntity : blockEntities) {
            this.renderSingleBE(blockEntity, poseStack, partialTick, cameraX, cameraY, cameraZ);
         }
      }

      void renderSingleBE(BlockEntity var1, PoseStack var2, float var3, double var4, double var6, double var8);

      BlockEntityRenderDispatcher getBlockEntityRenderDispatcher();
   }
}

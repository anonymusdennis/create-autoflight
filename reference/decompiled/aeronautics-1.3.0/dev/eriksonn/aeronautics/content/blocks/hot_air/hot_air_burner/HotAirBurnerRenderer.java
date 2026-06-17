package dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.util.SimColors;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class HotAirBurnerRenderer extends SmartBlockEntityRenderer<HotAirBurnerBlockEntity> {
   private static final ResourceLocation BURNER_FLAME_SHADER = Aeronautics.path("burner_flame");

   public HotAirBurnerRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(HotAirBurnerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      float signalStrength = Math.max(0.0F, (float)be.getSignalStrength() / 15.0F);
      SuperByteBuffer indicator = CachedBuffers.partial(AeroPartialModels.HOT_AIR_BURNER_INDICATOR, be.getBlockState());
      VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());
      indicator.light(light).color(SimColors.redstone(signalStrength)).renderInto(ms, vb);
      if (!((double)signalStrength <= 0.0)) {
         ms.pushPose();
         ms.translate(-0.5, 0.35, 0.5);
         BlockPos pos = be.getBlockPos();
         Vec3 center = pos.getCenter();
         Minecraft minecraft = Minecraft.getInstance();
         Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
         if (be.getLevel() instanceof PonderLevel) {
            camera = minecraft.getCameraEntity().getPosition(partialTicks);
         }

         SubLevel sublevel = Sable.HELPER.getContaining(be);
         if (sublevel != null) {
            camera = sublevel.logicalPose().transformPositionInverse(camera);
         }

         float angle = (float)Math.atan2(camera.z() - center.z(), camera.x() - center.x());
         HotAirBurnerBlock.Variant variant = (HotAirBurnerBlock.Variant)be.getBlockState().getValue(HotAirBurnerBlock.VARIANT);
         float palette = variant == HotAirBurnerBlock.Variant.FIRE ? 0.25F : 0.75F;
         ShaderProgram shader = VeilRenderSystem.setShader(BURNER_FLAME_SHADER);
         if (shader != null) {
            float flameRenderTime = (float)Mth.lerp((double)partialTicks, be.lastRenderTime, be.renderTime) + be.getTimeOffset();
            shader.getUniformSafe("FlameRenderTime").setFloat(flameRenderTime);
            shader.getUniformSafe("Intensity").setFloat(be.getFlameIntensity(partialTicks));
            shader.getUniformSafe("Palette").setFloat(palette);
            ms.rotateAround(Axis.YP.rotation((float)((double)(-angle) + (Math.PI / 2))), 1.0F, 0.0F, 0.0F);
            renderFlame(ms);
            ms.popPose();
         }

         super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
      }
   }

   private static void renderFlame(PoseStack poseStack) {
      float size = 2.0F;
      BufferBuilder builder = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      RenderSystem.enableDepthTest();
      RenderSystem.disableCull();
      Matrix4f pose = poseStack.last().pose();
      builder.addVertex(pose, 0.0F, 0.0F, 0.0F).setUv(0.0F, 1.0F);
      builder.addVertex(pose, 2.0F, 0.0F, 0.0F).setUv(1.0F, 1.0F);
      builder.addVertex(pose, 2.0F, 2.0F, 0.0F).setUv(1.0F, 0.0F);
      builder.addVertex(pose, 0.0F, 2.0F, 0.0F).setUv(0.0F, 0.0F);
      BufferUploader.drawWithShader(builder.buildOrThrow());
      RenderSystem.disableDepthTest();
      RenderSystem.enableCull();
   }
}

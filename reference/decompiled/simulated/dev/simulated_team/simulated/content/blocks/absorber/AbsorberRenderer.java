package dev.simulated_team.simulated.content.blocks.absorber;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.ryanhcode.sable.util.SableDistUtil;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

public class AbsorberRenderer extends SmartBlockEntityRenderer<AbsorberBlockEntity> {
   public AbsorberRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(AbsorberBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
      Level level = SableDistUtil.getClientLevel();
      VertexConsumer vb = buffer.getBuffer(RenderType.cutout());
      BlockState blockState = be.getBlockState();
      float yRot = (float)Math.toRadians((double)(AngleHelper.horizontalAngle((Direction)blockState.getValue(AbsorberBlock.HORIZONTAL_FACING)) + 180.0F));
      float pos = be.animationTimer.getValue(partialTicks);
      float target = be.animationTimer.getChaseTarget();
      if ((double)target > 0.5) {
         float fallTime = 0.3F;
         if (pos < 0.3F) {
            pos = 1.0F - pos * pos / 0.09F;
         } else {
            pos = (pos - 0.3F) / 0.7F;
            float bounce = (float)(Math.exp((double)(-pos) * 4.0) * Math.sin((double)pos * Math.PI * 3.0));
            float smoothing = 0.05F;
            bounce = (float)Math.sqrt((double)(bounce * bounce + 0.0025000002F)) - 0.05F;
            pos = bounce / 2.0F;
         }
      } else {
         pos = 1.0F - pos;
         float startVelocity = 2.0F;
         pos *= Mth.lerp(pos, 2.0F, 1.0F);
      }

      float movementDistance = 8.0F;
      float totalMovement = (1.0F + (1.0F - pos) * 8.0F) / 16.0F;
      SuperByteBuffer sponge = CachedBuffers.partial(
         blockState.getValue(AbsorberBlock.WET) ? SimPartialModels.ABSORBER_SPONGE_WET : SimPartialModels.ABSORBER_SPONGE_DRY, blockState
      );
      sponge.translate(0.0, 0.25, 0.0);
      sponge.scale(1.0F, 1.0F - pos * 8.0F / 9.0F, 1.0F);
      sponge.light(light).renderInto(ms, vb);
      Matrix4f rotationMatrix = new Matrix4f();
      this.apply(CachedBuffers.partial(SimPartialModels.ABSORBER_HAT, blockState), ms, light, vb, yRot, totalMovement, rotationMatrix);
      totalMovement /= 2.0F;
      this.apply(CachedBuffers.partial(SimPartialModels.ABSORBER_PIVOT, blockState), ms, light, vb, yRot, totalMovement, rotationMatrix);
      float height = totalMovement + 0.03125F;
      float length = 0.43125F;
      float width = (float)Math.sqrt((double)(0.18597656F - height * height));
      width /= 0.43125F;
      height /= 0.43125F;
      rotationMatrix.m22(width);
      rotationMatrix.m21(height);
      rotationMatrix.m11(width);
      rotationMatrix.m12(-height);
      this.apply(CachedBuffers.partial(SimPartialModels.ABSORBER_ARM, blockState), ms, light, vb, yRot, totalMovement, rotationMatrix);
      rotationMatrix.m21(-height);
      rotationMatrix.m12(height);
      rotationMatrix.m00(0.98F);
      this.apply(CachedBuffers.partial(SimPartialModels.ABSORBER_ARM, blockState), ms, light, vb, yRot, totalMovement, rotationMatrix);
   }

   void apply(SuperByteBuffer buffer, PoseStack ms, int light, VertexConsumer vb, float yRot, float offset, Matrix4f rotationMatrix) {
      buffer.translate(0.5, 0.25 + (double)offset, 0.5);
      Matrix4f r = new Matrix4f().rotate(yRot, 0.0F, 1.0F, 0.0F);
      buffer.mulPose(r.mul(rotationMatrix));
      buffer.translate(-0.5, 0.0, -0.5);
      buffer.light(light).renderInto(ms, vb);
   }
}

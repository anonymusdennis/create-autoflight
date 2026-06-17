package dev.createautoflight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.createautoflight.CreateAutoflight;
import dev.createautoflight.content.gyroscope.GyroscopeBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GyroscopeRenderer extends SafeBlockEntityRenderer<GyroscopeBlockEntity> {
    public static final ModelResourceLocation RING_YAW = standalone("gyroscope_ring_yaw");
    public static final ModelResourceLocation RING_ROLL = standalone("gyroscope_ring_roll");
    public static final ModelResourceLocation RING_PITCH = standalone("gyroscope_ring_pitch");
    public static final ModelResourceLocation GLASS = standalone("gyroscope_glass");

    private final BlockRenderDispatcher dispatcher;

    public GyroscopeRenderer(BlockEntityRendererProvider.Context ctx) {
        this.dispatcher = ctx.getBlockRenderDispatcher();
    }

    private static ModelResourceLocation standalone(String path) {
        return ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath(CreateAutoflight.MOD_ID, "block/" + path)
        );
    }

    @Override
    protected void renderSafe(
            GyroscopeBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        if (be.getLevel() == null) {
            return;
        }

        BlockState state = be.getBlockState();
        Direction.Axis axis = state.hasProperty(BlockStateProperties.AXIS)
                ? state.getValue(BlockStateProperties.AXIS)
                : Direction.Axis.Y;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        switch (axis) {
            case X -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(90));
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
            }
            case Z -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
            default -> { }
        }
        poseStack.translate(-0.5, -0.5, -0.5);

        float yaw = be.getDisplayAngleY(partialTick);
        float roll = be.getDisplayAngleZ(partialTick);
        float pitch = be.getDisplayAngleX(partialTick);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.translate(-0.5, -0.5, -0.5);
        renderPart(be, RING_YAW, poseStack, buffers, state, packedLight, packedOverlay, false);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.translate(-0.5, -0.5, -0.5);
        renderPart(be, RING_ROLL, poseStack, buffers, state, packedLight, packedOverlay, false);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.translate(-0.5, -0.5, -0.5);
        renderPart(be, RING_PITCH, poseStack, buffers, state, packedLight, packedOverlay, false);

        poseStack.popPose();
        poseStack.popPose();
        poseStack.popPose();

        renderPart(be, GLASS, poseStack, buffers, state, packedLight, packedOverlay, true);

        poseStack.popPose();
    }

    private void renderPart(
            GyroscopeBlockEntity be,
            ModelResourceLocation modelLocation,
            PoseStack poseStack,
            MultiBufferSource buffers,
            BlockState state,
            int light,
            int overlay,
            boolean translucent
    ) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(modelLocation);
        if (model == null || model == Minecraft.getInstance().getModelManager().getMissingModel()) {
            return;
        }
        RenderType renderType = translucent ? RenderType.translucent() : RenderType.solid();
        VertexConsumer buffer = buffers.getBuffer(renderType);
        dispatcher.getModelRenderer().tesselateBlock(
                be.getLevel(),
                model,
                state,
                be.getBlockPos(),
                poseStack,
                buffer,
                false,
                RandomSource.create(),
                0L,
                light
        );
    }
}

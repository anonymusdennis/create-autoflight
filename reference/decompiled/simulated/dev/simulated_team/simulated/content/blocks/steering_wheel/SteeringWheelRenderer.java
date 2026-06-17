package dev.simulated_team.simulated.content.blocks.steering_wheel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.waterwheel.WaterWheelRenderer;
import com.simibubi.create.foundation.model.BakedModelHelper;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.simulated_team.simulated.index.SimPartialModels;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import java.util.List;
import java.util.Map;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperBufferFactory;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.render.SuperByteBufferCache;
import net.createmod.catnip.render.SuperByteBufferCache.Compartment;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SteeringWheelRenderer extends KineticBlockEntityRenderer<SteeringWheelBlockEntity> {
   public static final Compartment<SteeringWheelRenderer.ModelKey> STEERING_WHEEL = new Compartment();

   public SteeringWheelRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(SteeringWheelBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         boolean floor = (Boolean)be.getBlockState().getValue(SteeringWheelBlock.ON_FLOOR);
         Direction facing = (Direction)be.getBlockState().getValue(SteeringWheelBlock.FACING);
         if (be.shouldRenderShaft()) {
            BlockState state = this.getRenderedBlockState(be);
            RenderType type = this.getRenderType(be, state);
            renderRotatingBuffer(
               be,
               CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(), floor ? Direction.DOWN : Direction.UP),
               ms,
               buffer.getBuffer(type),
               light
            );
         }

         SuperByteBuffer model = this.getWheelModel(be);
         model.rotateCentered(facing.getRotation());
         if (floor) {
            model.translate(0.0, 0.40625, -0.3125);
         } else {
            model.translate(0.0, 0.40625, 0.3125);
         }

         model.rotateCentered(be.getRenderAngle(partialTicks), Direction.UP);
         model.light(light);
         model.color(Color.WHITE);
         model.renderInto(ms, buffer.getBuffer(RenderType.solid()));
      }
   }

   private SuperByteBuffer getWheelModel(SteeringWheelBlockEntity be) {
      SteeringWheelRenderer.ModelKey key = new SteeringWheelRenderer.ModelKey(be.material);
      return SuperByteBufferCache.getInstance().get(STEERING_WHEEL, key, () -> {
         BakedModel model = generateModel(SimPartialModels.STEERING_WHEEL.get(), be.material);
         return SuperBufferFactory.getInstance().createForBlock(model, Blocks.AIR.defaultBlockState(), new PoseStack());
      });
   }

   public static BakedModel generateModel(BakedModel template, BlockState planksBlockState) {
      Block planksBlock = planksBlockState.getBlock();
      ResourceLocation id = RegisteredObjectsHelper.getKeyOrThrow(planksBlock);
      String wood = plankStateToWoodName(planksBlockState);
      if (wood == null) {
         return BakedModelHelper.generateModel(template, sprite -> null);
      } else {
         Map<TextureAtlasSprite, TextureAtlasSprite> map = new Reference2ReferenceOpenHashMap();
         map.put(WaterWheelRenderer.OAK_PLANKS_TEMPLATE.get(), getSpriteOnSide(planksBlockState, Direction.UP));
         return BakedModelHelper.generateModel(template, map::get);
      }
   }

   @Nullable
   private static String plankStateToWoodName(BlockState planksBlockState) {
      Block planksBlock = planksBlockState.getBlock();
      ResourceLocation id = RegisteredObjectsHelper.getKeyOrThrow(planksBlock);
      String path = id.getPath();
      if (path.endsWith("_planks")) {
         return (path.startsWith("archwood") ? "blue_" : "") + path.substring(0, path.length() - 7);
      } else {
         return path.contains("wood/planks/") ? path.substring(12) : null;
      }
   }

   private static TextureAtlasSprite getSpriteOnSide(BlockState state, Direction side) {
      BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
      if (model == null) {
         return null;
      } else {
         RandomSource random = RandomSource.create();
         random.setSeed(42L);
         List<BakedQuad> quads = model.getQuads(state, side, random);
         if (!quads.isEmpty()) {
            return quads.get(0).getSprite();
         } else {
            random.setSeed(42L);
            quads = model.getQuads(state, null, random);
            if (!quads.isEmpty()) {
               for (BakedQuad quad : quads) {
                  if (quad.getDirection() == side) {
                     return quad.getSprite();
                  }
               }
            }

            return model.getParticleIcon();
         }
      }
   }

   public static record ModelKey(BlockState material) {
   }
}

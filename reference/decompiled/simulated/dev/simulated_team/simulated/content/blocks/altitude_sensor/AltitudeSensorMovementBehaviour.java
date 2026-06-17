package dev.simulated_team.simulated.content.blocks.altitude_sensor;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;

public class AltitudeSensorMovementBehaviour implements MovementBehaviour {
   public boolean disableBlockEntityRendering() {
      return true;
   }

   public void tick(MovementContext context) {
      super.tick(context);
      float yPos = (float)Sable.HELPER.projectOutOfSubLevel(context.world, JOMLConversion.toJOML(context.position)).y;
      if (context.temporaryData instanceof Tuple<?, ?> heights) {
         context.temporaryData = new Tuple(heights.getB(), yPos);
      } else {
         context.temporaryData = new Tuple(yPos, yPos);
      }
   }

   public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffer) {
      float lowSignal = context.blockEntityData.getFloat("low_signal");
      float highSignal = context.blockEntityData.getFloat("high_signal");
      float visualHeight;
      if (context.temporaryData instanceof Tuple<?, ?> heights) {
         visualHeight = (Float)heights.getA() * (1.0F - AnimationTickHolder.getPartialTicks()) + (Float)heights.getB() * AnimationTickHolder.getPartialTicks();
      } else {
         Vector3d pos = context.position != null ? JOMLConversion.toJOML(context.position) : new Vector3d();
         visualHeight = (float)Sable.HELPER.projectOutOfSubLevel(context.world, pos).y;
      }

      Level level = context.contraption.entity.level();
      float y = (float)Mth.map(context.position.y, (double)level.getMinBuildHeight(), (double)level.getMaxBuildHeight(), 0.0, 1.0);
      float value = Mth.clampedMap(y, 0.0F, 1.0F, lowSignal, highSignal);
      AltitudeSensorRenderer.render(
         context.state,
         1000,
         value,
         visualHeight,
         matrices.getViewProjection(),
         matrices.getModel(),
         matrices.getWorld(),
         buffer,
         LevelRenderer.getLightColor(renderWorld, context.localPos)
      );
   }
}

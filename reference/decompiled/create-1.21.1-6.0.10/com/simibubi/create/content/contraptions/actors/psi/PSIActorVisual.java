package com.simibubi.create.content.contraptions.actors.psi;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;

public class PSIActorVisual extends ActorVisual {
   private final PIInstance instance;

   public PSIActorVisual(VisualizationContext context, VirtualRenderWorld world, MovementContext movementContext) {
      super(context, world, movementContext);
      this.instance = new PIInstance(context.instancerProvider(), movementContext.state, movementContext.localPos, false);
      this.instance.middle.light(this.localBlockLight(), 0);
      this.instance.top.light(this.localBlockLight(), 0);
   }

   @Override
   public void beginFrame() {
      LerpedFloat lf = PortableStorageInterfaceMovement.getAnimation(this.context);
      this.instance.tick(lf.settled());
      this.instance.beginFrame(lf.getValue(AnimationTickHolder.getPartialTicks()));
   }

   @Override
   protected void _delete() {
      this.instance.remove();
   }
}

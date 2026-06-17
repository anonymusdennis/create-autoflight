package com.simibubi.create.content.contraptions.actors.psi;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import java.util.function.Consumer;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class PIInstance {
   private final InstancerProvider instancerProvider;
   private final BlockState blockState;
   private final BlockPos instancePos;
   private final float angleX;
   private final float angleY;
   private boolean lit;
   TransformedInstance middle;
   TransformedInstance top;

   public PIInstance(InstancerProvider instancerProvider, BlockState blockState, BlockPos instancePos, boolean lit) {
      this.instancerProvider = instancerProvider;
      this.blockState = blockState;
      this.instancePos = instancePos;
      Direction facing = (Direction)blockState.getValue(PortableStorageInterfaceBlock.FACING);
      this.angleX = facing == Direction.UP ? 0.0F : (facing == Direction.DOWN ? 180.0F : 90.0F);
      this.angleY = AngleHelper.horizontalAngle(facing);
      this.lit = lit;
      this.middle = (TransformedInstance)instancerProvider.instancer(
            InstanceTypes.TRANSFORMED, Models.partial(PortableStorageInterfaceRenderer.getMiddleForState(blockState, lit))
         )
         .createInstance();
      this.top = (TransformedInstance)instancerProvider.instancer(
            InstanceTypes.TRANSFORMED, Models.partial(PortableStorageInterfaceRenderer.getTopForState(blockState))
         )
         .createInstance();
   }

   public void beginFrame(float progress) {
      ((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)this.middle.setIdentityTransform().translate(this.instancePos))
                  .center())
               .rotateYDegrees(this.angleY))
            .rotateXDegrees(this.angleX))
         .uncenter();
      ((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)this.top.setIdentityTransform().translate(this.instancePos))
                  .center())
               .rotateYDegrees(this.angleY))
            .rotateXDegrees(this.angleX))
         .uncenter();
      this.middle.translate(0.0F, progress * 0.5F + 0.375F, 0.0F);
      this.top.translate(0.0F, progress, 0.0F);
      this.middle.setChanged();
      this.top.setChanged();
   }

   public void tick(boolean lit) {
      if (this.lit != lit) {
         this.lit = lit;
         this.instancerProvider
            .instancer(InstanceTypes.TRANSFORMED, Models.partial(PortableStorageInterfaceRenderer.getMiddleForState(this.blockState, lit)))
            .stealInstance(this.middle);
      }
   }

   public void remove() {
      this.middle.delete();
      this.top.delete();
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      consumer.accept(this.middle);
      consumer.accept(this.top);
   }
}

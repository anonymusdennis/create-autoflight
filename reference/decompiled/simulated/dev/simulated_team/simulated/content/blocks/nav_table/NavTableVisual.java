package dev.simulated_team.simulated.content.blocks.nav_table;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.util.SimColors;
import dev.simulated_team.simulated.util.SimDirectionUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class NavTableVisual extends AbstractBlockEntityVisual<NavTableBlockEntity> implements SimpleDynamicVisual {
   private final Vector3f tempVec = new Vector3f();
   private final List<NavTableVisual.InstanceDirectionHolder> redstoneInstances = new ArrayList<>();
   private final TransformedInstance pointer;

   public NavTableVisual(VisualizationContext ctx, NavTableBlockEntity navBE, float partialTick) {
      super(ctx, navBE, partialTick);
      Direction facing = (Direction)navBE.getBlockState().getValue(NavTableBlock.FACING);
      Quaternionf facingRot = facing.getRotation();

      for (int i = 0; i < 4; i++) {
         TransformedInstance inst = (TransformedInstance)this.instancerProvider()
            .instancer(InstanceTypes.TRANSFORMED, Models.partial(SimPartialModels.NAV_TABLE_INDICATOR))
            .createInstance();
         Direction dir = SimDirectionUtil.Y_AXIS_PLANE[i];
         ((TransformedInstance)((TransformedInstance)inst.translate(this.getVisualPosition())).center()).rotate(facingRot);
         inst.translate(0.0, -0.5, 0.0);
         inst.rotateToFace(dir);
         inst.translate(0.0, 0.0, 0.5);
         facingRot.transform((float)dir.getStepX(), (float)dir.getStepY(), (float)dir.getStepZ(), this.tempVec);
         Direction logicalDirection = Direction.getNearest(this.tempVec.x, this.tempVec.y, this.tempVec.z);
         inst.colorRgb(SimColors.redstone(navBE.isPowering ? (float)Math.max(navBE.getRedstoneStrength(logicalDirection), 0) / 15.0F : 0.0F));
         this.redstoneInstances.add(new NavTableVisual.InstanceDirectionHolder(inst, logicalDirection));
      }

      this.pointer = (TransformedInstance)this.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(SimPartialModels.NAV_TABLE_POINTER))
         .createInstance();
      this.translatePointer(partialTick);
   }

   private void translatePointer(float partialTick) {
      ((TransformedInstance)((TransformedInstance)((TransformedInstance)this.pointer.translate(this.getVisualPosition())).center())
            .rotate(((Direction)((NavTableBlockEntity)this.blockEntity).getBlockState().getValue(BlockStateProperties.FACING)).getRotation())
            .translate(0.0, 0.3, 0.0))
         .rotateY((float)((double)((NavTableBlockEntity)this.blockEntity).getClientTargetAngle(partialTick) - (Math.PI / 2)));
   }

   public void beginFrame(Context context) {
      for (NavTableVisual.InstanceDirectionHolder holder : this.redstoneInstances) {
         holder.instance()
            .colorRgb(
               SimColors.redstone(
                  ((NavTableBlockEntity)this.blockEntity).isPowering
                     ? (float)Math.max(((NavTableBlockEntity)this.blockEntity).getRedstoneStrength(holder.logicalDirection()), 0) / 15.0F
                     : 0.0F
               )
            )
            .setChanged();
      }

      this.pointer.setIdentityTransform();
      this.translatePointer(context.partialTick());
      this.pointer.setChanged();
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      for (NavTableVisual.InstanceDirectionHolder holder : this.redstoneInstances) {
         consumer.accept(holder.instance());
      }

      consumer.accept(this.pointer);
   }

   public void updateLight(float v) {
      for (NavTableVisual.InstanceDirectionHolder holder : this.redstoneInstances) {
         this.relight(new FlatLit[]{holder.instance()});
      }

      this.relight(new FlatLit[]{this.pointer});
   }

   protected void _delete() {
      for (NavTableVisual.InstanceDirectionHolder holder : this.redstoneInstances) {
         holder.instance().delete();
      }

      this.pointer.delete();
      this.redstoneInstances.clear();
   }

   private static record InstanceDirectionHolder(TransformedInstance instance, Direction logicalDirection) {
   }
}

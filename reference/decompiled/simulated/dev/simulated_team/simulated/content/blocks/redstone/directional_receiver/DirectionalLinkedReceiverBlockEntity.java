package dev.simulated_team.simulated.content.blocks.redstone.directional_receiver;

import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.simulated_team.simulated.content.blocks.redstone.AbstractLinkedReceiverBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class DirectionalLinkedReceiverBlockEntity extends AbstractLinkedReceiverBlockEntity {
   private double angleToClosestLink;

   public DirectionalLinkedReceiverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   public Tuple<Integer, Double> getSignalFromLink(Vec3 relativePosition, int transmittedStrength) {
      Direction dir = (Direction)this.getBlockState().getValue(DirectionalBlock.FACING);
      Vec3 normal = new Vec3((double)dir.getStepX(), (double)dir.getStepY(), (double)dir.getStepZ());
      double length = relativePosition.length();
      if (length > (double)((Integer)AllConfigs.server().logistics.linkRange.get()).intValue()) {
         return new Tuple(0, 0.0);
      } else {
         double dot = relativePosition.dot(normal) / length;
         if (dot < 0.0) {
            return new Tuple(0, 0.0);
         } else {
            double angle = Math.asin(dot);
            this.angleToClosestLink = Math.acos(dot);
            double strengthScalar = Math.clamp(angle / Math.PI * 2.0, 0.0, 1.0);
            return new Tuple((int)Math.ceil(strengthScalar * (double)transmittedStrength), Math.toDegrees(angle));
         }
      }
   }

   public double getAngleToClosestLink() {
      return this.angleToClosestLink;
   }
}

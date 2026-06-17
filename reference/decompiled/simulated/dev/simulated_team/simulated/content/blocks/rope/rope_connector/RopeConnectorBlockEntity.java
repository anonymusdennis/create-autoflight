package dev.simulated_team.simulated.content.blocks.rope.rope_connector;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientRopeStrand;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class RopeConnectorBlockEntity extends SmartBlockEntity implements RopeStrandHolderBlockEntity {
   public static final double RENDER_BOUNDING_BOX_INFLATION = 3.0;
   private RopeStrandHolderBehavior ropeHolder;

   public RopeConnectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public RopeStrandHolderBehavior getRopeHolder() {
      return this.ropeHolder;
   }

   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(this.ropeHolder = new RopeStrandHolderBehavior(this));
   }

   public void tick() {
      super.tick();
      if (this.level.isClientSide) {
         this.invalidateRenderBoundingBox();
      }
   }

   public AABB getRenderBoundingBox() {
      ClientRopeStrand rope = this.ropeHolder.getClientStrand();
      if (rope != null && this.ropeHolder.ownsRope()) {
         AABB bounds = rope.getBounds();
         return bounds == null ? super.getRenderBoundingBox() : bounds.inflate(3.0);
      } else {
         return super.getRenderBoundingBox();
      }
   }

   @Override
   public RopeStrandHolderBehavior getBehavior() {
      return this.ropeHolder;
   }

   @Override
   public Vec3 getAttachmentPoint(BlockPos pos, BlockState state) {
      Direction facing = (Direction)state.getValue(RopeConnectorBlock.FACING);
      double offset = -0.1875;
      return pos.getCenter().add((double)facing.getStepX() * -0.1875, (double)facing.getStepY() * -0.1875, (double)facing.getStepZ() * -0.1875);
   }

   @Override
   public Vec3 getVisualAttachmentPoint(BlockPos pos, BlockState state) {
      Direction facing = (Direction)state.getValue(RopeConnectorBlock.FACING);
      double offset = -0.25;
      return pos.getCenter().add((double)facing.getStepX() * -0.25, (double)facing.getStepY() * -0.25, (double)facing.getStepZ() * -0.25);
   }
}

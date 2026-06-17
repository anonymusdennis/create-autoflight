package dev.simulated_team.simulated.content.blocks.lasers;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractLaserBlockEntity extends SmartBlockEntity {
   public AbstractLaserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public abstract Direction getDirection();

   public Vec3i getNormal() {
      return this.getDirection().getNormal();
   }

   public abstract float getRaycastLength();

   public abstract boolean shouldCast();

   public Couple<Vec3> gatherStartAndEnd() {
      Vec3i normal = this.getNormal();
      Vec3 start = Vec3.atCenterOf(this.worldPosition).add(Vec3.atLowerCornerOf(normal).scale(0.5));
      Vec3 end = start.add(Vec3.atLowerCornerOf(normal).scale((double)this.getRaycastLength()));
      return Couple.create(start, end);
   }

   public AABB getRenderBoundingBox() {
      int range = (int)this.getRaycastLength();
      Vec3i normal = this.getNormal();
      return new AABB(this.getBlockPos()).expandTowards(Vec3.atLowerCornerOf(normal.multiply(range)));
   }
}

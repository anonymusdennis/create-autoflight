package dev.ryanhcode.sable.mixin.block_placement;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({EntityGetter.class})
public interface EntityGetterMixin {
   @Shadow
   List<Entity> getEntities(@Nullable Entity var1, AABB var2);

   @Shadow
   List<? extends Player> players();

   @Overwrite
   default boolean isUnobstructed(@Nullable Entity pEntity, VoxelShape voxelShape) {
      if (voxelShape.isEmpty()) {
         return true;
      } else {
         for (Entity entity : this.getEntities(pEntity, voxelShape.bounds())) {
            AABB entityBounds = entity.getBoundingBox();
            boolean fine = Shapes.joinIsNotEmpty(voxelShape, Shapes.create(entityBounds), BooleanOp.AND);
            BoundingBox3d queryBounds = new BoundingBox3d(entityBounds);
            queryBounds.expand(1.5, queryBounds);

            for (SubLevel subLevel : Sable.HELPER.getAllIntersecting(entity.level(), queryBounds)) {
               if (!fine) {
                  BoundingBox3d bb = new BoundingBox3d(entityBounds);
                  bb.transformInverse(subLevel.logicalPose(), bb);
                  bb.expand(-0.046875, bb);
                  if (Shapes.joinIsNotEmpty(voxelShape, Shapes.create(bb.toMojang()), BooleanOp.AND)) {
                     fine = true;
                  }
               }
            }

            if (!entity.isRemoved() && entity.blocksBuilding && (pEntity == null || !entity.isPassengerOfSameVehicle(pEntity)) && fine) {
               return false;
            }
         }

         return true;
      }
   }
}

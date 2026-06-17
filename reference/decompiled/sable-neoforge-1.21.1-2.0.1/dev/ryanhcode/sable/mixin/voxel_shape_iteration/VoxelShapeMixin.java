package dev.ryanhcode.sable.mixin.voxel_shape_iteration;

import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.mixinhelpers.voxel_shape_iteration.FastVoxelShapeIterator;
import dev.ryanhcode.sable.mixinterface.voxel_shape_iteration.FastVoxelShapeIterable;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.Iterator;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin({VoxelShape.class})
public abstract class VoxelShapeMixin implements FastVoxelShapeIterable {
   @Unique
   private final Long2ObjectMap<FastVoxelShapeIterator> sable$boxIterator = new Long2ObjectArrayMap();
   @Shadow
   @Final
   protected DiscreteVoxelShape shape;

   @Shadow
   public abstract DoubleList getCoords(Axis var1);

   @Override
   public Iterator<BoundingBox3dc> sable$allBoxes() {
      synchronized (this) {
         long id = Thread.currentThread().threadId();
         FastVoxelShapeIterator iterator = (FastVoxelShapeIterator)this.sable$boxIterator.get(id);
         if (iterator == null) {
            iterator = (FastVoxelShapeIterator)this.sable$boxIterator.get(id);
            if (iterator == null) {
               this.sable$boxIterator
                  .put(
                     id,
                     iterator = new FastVoxelShapeIterator(
                        this.shape, this.getCoords(Axis.X).toDoubleArray(), this.getCoords(Axis.Y).toDoubleArray(), this.getCoords(Axis.Z).toDoubleArray()
                     )
                  );
            }
         }

         iterator.reset();
         return iterator;
      }
   }
}

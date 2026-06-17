package dev.ryanhcode.sable.mixinterface.voxel_shape_iteration;

import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import java.util.Iterator;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface FastVoxelShapeIterable {
   Iterator<BoundingBox3dc> sable$allBoxes();
}

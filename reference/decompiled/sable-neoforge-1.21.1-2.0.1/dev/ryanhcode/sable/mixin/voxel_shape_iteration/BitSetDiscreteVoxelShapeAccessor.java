package dev.ryanhcode.sable.mixin.voxel_shape_iteration;

import java.util.BitSet;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({BitSetDiscreteVoxelShape.class})
public interface BitSetDiscreteVoxelShapeAccessor extends DiscreteVoxelShapeAccessor {
   @Accessor
   BitSet getStorage();

   @Invoker
   boolean invokeIsZStripFull(int var1, int var2, int var3, int var4);

   @Invoker
   boolean invokeIsXZRectangleFull(int var1, int var2, int var3, int var4, int var5);

   @Invoker
   void invokeClearZStrip(int var1, int var2, int var3, int var4);
}

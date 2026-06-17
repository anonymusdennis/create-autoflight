package dev.eriksonn.aeronautics.mixin.levitite;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.eriksonn.aeronautics.index.AeroTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({Block.class})
public class BlockMixin {
   @WrapOperation(
      method = {"shouldRenderFace"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/shapes/Shapes;joinIsNotEmpty(Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/shapes/BooleanOp;)Z"
      )}
   )
   private static boolean shouldRenderFace(
      VoxelShape shape1,
      VoxelShape shape2,
      BooleanOp ops,
      Operation<Boolean> original,
      @Local(argsOnly = true,ordinal = 0) BlockState blockstate1,
      @Local(ordinal = 1) BlockState blockstate2
   ) {
      if ((Boolean)original.call(new Object[]{shape1, shape2, ops})) {
         return true;
      } else {
         boolean l1 = blockstate1.is(AeroTags.BlockTags.LEVITITE);
         boolean l2 = blockstate2.is(AeroTags.BlockTags.LEVITITE);
         return l1 ^ l2;
      }
   }
}

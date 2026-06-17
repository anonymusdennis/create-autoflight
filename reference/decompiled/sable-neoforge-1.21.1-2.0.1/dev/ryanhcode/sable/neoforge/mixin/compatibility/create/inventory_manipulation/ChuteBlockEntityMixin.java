package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.inventory_manipulation;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.logistics.chute.ChuteBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({ChuteBlockEntity.class})
public abstract class ChuteBlockEntityMixin extends SmartBlockEntity {
   public ChuteBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @WrapMethod(
      method = {"grabCapability"}
   )
   public IItemHandler sable$grabCap(Direction side, Operation<IItemHandler> original) {
      IItemHandler handler = (IItemHandler)original.call(new Object[]{side});
      if (handler != null) {
         return handler;
      } else {
         Level level = this.getLevel();

         assert level != null;

         BlockPos checkPos = this.worldPosition.relative(side);
         Direction opposite = side.getOpposite();
         Vector3d mut = new Vector3d((double)opposite.getStepX(), (double)opposite.getStepY(), (double)opposite.getStepZ());
         ActiveSableCompanion helper = Sable.HELPER;
         SubLevel parentSublevel = helper.getContaining(level, checkPos);
         if (parentSublevel != null) {
            parentSublevel.logicalPose().transformNormalInverse(mut);
         }

         Vector3d includSublevelDir = new Vector3d(mut);
         return helper.runIncludingSubLevels(
            level,
            checkPos.getCenter(),
            false,
            parentSublevel,
            (sublevel, pos) -> {
               includSublevelDir.set(mut);
               if (sublevel != null) {
                  sublevel.logicalPose().transformNormal(includSublevelDir);
               }

               return (IItemHandler)level.getCapability(
                  ItemHandler.BLOCK, pos, Direction.getNearest(includSublevelDir.x, includSublevelDir.y, includSublevelDir.z)
               );
            }
         );
      }
   }
}

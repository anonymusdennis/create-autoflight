package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.belt;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({BeltBlockEntity.class})
public abstract class BeltBlockEntityMixin extends KineticBlockEntity {
   public BeltBlockEntityMixin(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
      super(typeIn, pos, state);
   }

   public void onSpeedChanged(float previousSpeed) {
      super.onSpeedChanged(previousSpeed);
      if (SubLevelContainer.getContainer(this.level) instanceof ServerSubLevelContainer serverSubLevelContainer) {
         SubLevelPhysicsSystem physicsSystem = serverSubLevelContainer.physicsSystem();
         BlockPos blockPos = this.getBlockPos();
         physicsSystem.wakeUpObjectsAt(blockPos.getX(), blockPos.getY(), blockPos.getZ());
         if (Sable.HELPER.getContaining(this) instanceof ServerSubLevel serverSubLevel) {
            physicsSystem.getPipeline().wakeUp(serverSubLevel);
         }
      }
   }
}

package dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.create.harvester;

import com.simibubi.create.content.contraptions.actors.harvester.HarvesterMovementBehaviour;
import dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.create.behavior_compatibility.harvester_block_entity.DummyMovementContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class HarvesterTicker<T extends BlockEntity & HarvesterLerpedSpeed> implements BlockEntityTicker<T> {
   public static final HarvesterMovementBehaviour blockEntityBehaviour = new HarvesterMovementBehaviour();
   public static final DummyMovementContext dummyMovementContext = new DummyMovementContext();

   public void tick(Level level, BlockPos arg2, BlockState arg3, T be) {
      if (!be.hasLevel()) {
         be.setLevel(level);
      }

      be.sable$clientTick();
   }

   public static void dropItem(Level level, ItemStack itemStack, BlockPos sable$selfPos) {
      if (sable$selfPos != null) {
         Vec3 center = sable$selfPos.getCenter();
         ItemEntity itemEntity = new ItemEntity(level, center.x, center.y, center.z, itemStack);
         level.addFreshEntity(itemEntity);
      }
   }
}

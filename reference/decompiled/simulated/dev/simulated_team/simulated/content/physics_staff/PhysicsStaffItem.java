package dev.simulated_team.simulated.content.physics_staff;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class PhysicsStaffItem extends Item {
   public static float RANGE = 128.0F;

   public PhysicsStaffItem(Properties properties) {
      super(properties);
   }

   public static boolean isHolding(Player player) {
      return player.getMainHandItem().getItem() instanceof PhysicsStaffItem || player.getOffhandItem().getItem() instanceof PhysicsStaffItem;
   }

   public boolean canAttackBlock(BlockState state, Level world, BlockPos pos, Player player) {
      return false;
   }
}

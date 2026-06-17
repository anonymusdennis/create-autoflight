package dev.simulated_team.simulated.content.entities.honey_glue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class HoneyGlueItem extends Item {
   public HoneyGlueItem(Properties properties) {
      super(properties);
   }

   public boolean canAttackBlock(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
      return false;
   }
}

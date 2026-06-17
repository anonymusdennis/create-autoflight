package dev.simulated_team.simulated.content.entities.diagram;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class DiagramItem extends Item {
   public DiagramItem(Properties p_i48487_1_) {
      super(p_i48487_1_);
   }

   public InteractionResult useOn(UseOnContext ctx) {
      Direction face = ctx.getClickedFace();
      Player player = ctx.getPlayer();
      ItemStack stack = ctx.getItemInHand();
      BlockPos pos = ctx.getClickedPos().relative(face);
      if (player != null && !player.mayUseItemAt(pos, face, stack)) {
         return InteractionResult.FAIL;
      } else {
         Level world = ctx.getLevel();
         DiagramEntity diagram = new DiagramEntity(world, pos, face, face.getAxis().isHorizontal() ? Direction.DOWN : ctx.getHorizontalDirection());
         if (!diagram.survives()) {
            return InteractionResult.CONSUME;
         } else {
            if (!world.isClientSide) {
               diagram.playPlacementSound();
               world.addFreshEntity(diagram);
            }

            stack.shrink(1);
            return InteractionResult.sidedSuccess(world.isClientSide);
         }
      }
   }
}

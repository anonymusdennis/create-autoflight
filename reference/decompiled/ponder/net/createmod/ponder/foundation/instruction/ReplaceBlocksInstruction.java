package net.createmod.ponder.foundation.instruction;

import java.util.function.UnaryOperator;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ReplaceBlocksInstruction extends WorldModifyInstruction {
   private final UnaryOperator<BlockState> stateToUse;
   private final boolean replaceAir;
   private final boolean spawnParticles;

   public ReplaceBlocksInstruction(Selection selection, UnaryOperator<BlockState> stateToUse, boolean replaceAir, boolean spawnParticles) {
      super(selection);
      this.stateToUse = stateToUse;
      this.replaceAir = replaceAir;
      this.spawnParticles = spawnParticles;
   }

   @Override
   protected void runModification(Selection selection, PonderScene scene) {
      PonderLevel level = scene.getWorld();
      selection.forEach(pos -> {
         if (level.getBounds().isInside(pos)) {
            BlockState prevState = level.getBlockState(pos);
            if (this.replaceAir || prevState != Blocks.AIR.defaultBlockState()) {
               if (this.spawnParticles) {
                  level.addBlockDestroyEffects(pos, prevState);
               }

               level.setBlockAndUpdate(pos, this.stateToUse.apply(prevState));
            }
         }
      });
   }

   @Override
   protected boolean needsRedraw() {
      return true;
   }
}

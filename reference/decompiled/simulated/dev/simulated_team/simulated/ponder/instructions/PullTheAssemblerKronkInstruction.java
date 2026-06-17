package dev.simulated_team.simulated.ponder.instructions;

import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.minecraft.core.BlockPos;

public class PullTheAssemblerKronkInstruction extends PonderInstruction {
   protected final BlockPos assemblerPos;
   protected final boolean isAssembling;
   protected final boolean instantaneous;

   public PullTheAssemblerKronkInstruction(BlockPos assemblerPos, boolean isAssembling, boolean instantaneous) {
      this.assemblerPos = assemblerPos;
      this.isAssembling = isAssembling;
      this.instantaneous = instantaneous;
   }

   public boolean isComplete() {
      return true;
   }

   public void tick(PonderScene scene) {
      PonderLevel world = scene.getWorld();
      if (world.getBlockEntity(this.assemblerPos) instanceof PhysicsAssemblerBlockEntity be) {
         be.clientFlickLeverTo(this.isAssembling);
         if (this.instantaneous) {
            be.jerkLever();
         }
      }
   }
}

package dev.simulated_team.simulated.ponder.instructions;

import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.minecraft.core.BlockPos;

public class LinkDockingConnectorsInstruction extends PonderInstruction {
   final BlockPos fromPos;
   final BlockPos toPos;

   public LinkDockingConnectorsInstruction(BlockPos fromPos, BlockPos toPos) {
      this.fromPos = fromPos;
      this.toPos = toPos;
   }

   public boolean isComplete() {
      return true;
   }

   public void tick(PonderScene scene) {
      PonderLevel world = scene.getWorld();
      if (world.getBlockEntity(this.fromPos) instanceof DockingConnectorBlockEntity be1
         && world.getBlockEntity(this.toPos) instanceof DockingConnectorBlockEntity be2) {
         be1.tank.connect(this.toPos, be2.tank);
      }
   }
}

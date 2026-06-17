package dev.eriksonn.aeronautics.content.ponder.instructions;

import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlockEntity;
import com.simibubi.create.content.redstone.nixieTube.NixieTubeBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.hot_air.steam_vent.SteamVentBlockEntity;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.WorldModifyInstruction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class RedstoneSignalInstruction extends WorldModifyInstruction {
   protected final int signal;

   public RedstoneSignalInstruction(Selection selection, int signal) {
      super(selection);
      this.signal = signal;
   }

   protected void runModification(Selection selection, PonderScene scene) {
      PonderLevel level = scene.getWorld();
      selection.forEach(pos -> {
         if (level.getBounds().isInside(pos)) {
            BlockEntity BE = level.getBlockEntity(pos);
            if (BE instanceof NixieTubeBlockEntity nixie) {
               nixie.updateRedstoneStrength(this.signal);
               nixie.updateDisplayedStrings();
            }

            if (BE instanceof AnalogLeverBlockEntity lever) {
               CompoundTag tag = new CompoundTag();
               lever.write(tag, level.registryAccess(), false);
               tag.putInt("State", this.signal);
               lever.readClient(tag, level.registryAccess());
            }

            BlockState state = level.getBlockState(pos);
            BlockState newState = null;
            if (state != Blocks.AIR.defaultBlockState()) {
               if (state.hasProperty(BlockStateProperties.POWER)) {
                  newState = (BlockState)state.setValue(BlockStateProperties.POWER, this.signal);
               }

               if (state.hasProperty(BlockStateProperties.POWERED)) {
                  newState = (BlockState)state.setValue(BlockStateProperties.POWERED, this.signal > 0);
               }

               if (state.hasProperty(RedstoneTorchBlock.LIT)) {
                  newState = (BlockState)state.setValue(RedstoneTorchBlock.LIT, this.signal > 0);
               }

               if (BE instanceof HotAirBurnerBlockEntity burner) {
                  burner.setSignalStrength(this.signal);
               }

               if (BE instanceof SteamVentBlockEntity vent) {
                  vent.updateSignal(this.signal);
               }

               if (newState != null) {
                  level.setBlockAndUpdate(pos, newState);
               }
            }
         }
      });
   }

   protected boolean needsRedraw() {
      return true;
   }
}

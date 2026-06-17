package dev.simulated_team.simulated.content.blocks.steering_wheel;

import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchBlock;
import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import dev.simulated_team.simulated.Simulated;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.generators.ModelFile;

public class SteeringWheelGenerator extends SpecialBlockStateGen {
   protected int getXRotation(BlockState state) {
      return 0;
   }

   protected int getYRotation(BlockState state) {
      return this.horizontalAngle((Direction)state.getValue(ThresholdSwitchBlock.FACING)) + 180;
   }

   public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov, BlockState state) {
      return prov.models()
         .getExistingFile(Simulated.path(state.getValue(SteeringWheelBlock.ON_FLOOR) ? "block/steering_wheel/block" : "block/steering_wheel/block_up"));
   }
}

package dev.simulated_team.simulated.service;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import dev.simulated_team.simulated.content.blocks.auger_shaft.AugerShaftBlock;
import dev.simulated_team.simulated.content.blocks.util.AbstractDirectionalAxisBlock;
import dev.simulated_team.simulated.data.SimBlockStateGen;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public interface SimBlockStateService {
   SimBlockStateService INSTANCE = ServiceUtil.load(SimBlockStateService.class);

   <T extends Block> void genericModelBuilder(
      DataGenContext<Block, T> var1, RegistrateBlockstateProvider var2, Function<BlockState, SimBlockStateGen.XYHolder> var3, Function<BlockState, Object> var4
   );

   <P extends AugerShaftBlock> NonNullBiConsumer<DataGenContext<Block, P>, RegistrateBlockstateProvider> augerShaftGenerate(String var1, boolean var2);

   <T extends AbstractDirectionalAxisBlock> void directionalAxisBlock(
      DataGenContext<Block, T> var1, RegistrateBlockstateProvider var2, BiFunction<BlockState, Boolean, Object> var3
   );
}

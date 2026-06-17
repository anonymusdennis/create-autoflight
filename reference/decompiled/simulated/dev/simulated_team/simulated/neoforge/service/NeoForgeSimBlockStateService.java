package dev.simulated_team.simulated.neoforge.service;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import dev.simulated_team.simulated.content.blocks.auger_shaft.AugerShaftBlock;
import dev.simulated_team.simulated.content.blocks.util.AbstractDirectionalAxisBlock;
import dev.simulated_team.simulated.data.SimBlockStateGen;
import dev.simulated_team.simulated.data.neoforge.AugerShaftGen;
import dev.simulated_team.simulated.service.SimBlockStateService;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;

public class NeoForgeSimBlockStateService implements SimBlockStateService {
   @Override
   public <T extends Block> void genericModelBuilder(
      DataGenContext<Block, T> ctx,
      RegistrateBlockstateProvider prov,
      Function<BlockState, SimBlockStateGen.XYHolder> xyGetter,
      Function<BlockState, Object> modelGetter
   ) {
      prov.getVariantBuilder((Block)ctx.getEntry()).forAllStates(state -> {
         if (modelGetter.apply(state) instanceof ModelFile model) {
            SimBlockStateGen.XYHolder rotations = xyGetter.apply(state);
            return ConfiguredModel.builder().modelFile(model).rotationX(rotations.xRot()).rotationY(rotations.yRot()).build();
         } else {
            throw new IllegalArgumentException("ModelGetter must return a ModelFile");
         }
      });
   }

   @Override
   public <P extends AugerShaftBlock> NonNullBiConsumer<DataGenContext<Block, P>, RegistrateBlockstateProvider> augerShaftGenerate(String name, boolean cog) {
      return AugerShaftGen.generate(name, cog);
   }

   @Override
   public <T extends AbstractDirectionalAxisBlock> void directionalAxisBlock(
      DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov, BiFunction<BlockState, Boolean, Object> modelFunc
   ) {
      prov.getVariantBuilder((Block)ctx.getEntry()).forAllStates(state -> {
         boolean alongFirst = (Boolean)state.getValue(AbstractDirectionalAxisBlock.AXIS_ALONG_FIRST_COORDINATE);
         Direction direction = (Direction)state.getValue(AbstractDirectionalAxisBlock.FACING);
         boolean vertical = direction.getAxis().isHorizontal() && direction.getAxis() == Axis.X == alongFirst;
         int xRot = direction == Direction.DOWN ? 270 : (direction == Direction.UP ? 90 : 0);
         int yRot = direction.getAxis().isVertical() ? (alongFirst ? 0 : 90) : (int)direction.toYRot();
         if (modelFunc.apply(state, vertical) instanceof ModelFile m) {
            return ConfiguredModel.builder().modelFile(m).rotationX(xRot).rotationY(yRot).build();
         } else {
            throw new AssertionError("Required Model file!");
         }
      });
   }
}

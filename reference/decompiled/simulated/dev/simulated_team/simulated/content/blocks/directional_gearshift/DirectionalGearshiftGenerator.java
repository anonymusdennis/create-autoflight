package dev.simulated_team.simulated.content.blocks.directional_gearshift;

import com.google.common.collect.UnmodifiableIterator;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import dev.simulated_team.simulated.Simulated;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder.PartBuilder;

public class DirectionalGearshiftGenerator {
   public static <P extends DirectionalGearshiftBlock> void generate(DataGenContext<Block, P> context, RegistrateBlockstateProvider provider) {
      MultiPartBlockStateBuilder builder = provider.getMultipartBuilder((Block)context.get());
      UnmodifiableIterator var3 = ((DirectionalGearshiftBlock)context.get()).getStateDefinition().getPossibleStates().iterator();

      while (var3.hasNext()) {
         BlockState state = (BlockState)var3.next();
         boolean alongFirst = (Boolean)state.getValue(DirectionalGearshiftBlock.AXIS_ALONG_FIRST_COORDINATE);
         Direction direction = (Direction)state.getValue(DirectionalGearshiftBlock.FACING);
         boolean leftOn = (Boolean)state.getValue(DirectionalGearshiftBlock.LEFT_POWERED);
         boolean rightOn = (Boolean)state.getValue(DirectionalGearshiftBlock.RIGHT_POWERED);
         boolean vertical = direction.getAxis().isHorizontal() && direction.getAxis() == Axis.X == alongFirst;
         int xRot = direction == Direction.DOWN ? 270 : (direction == Direction.UP ? 90 : 0);
         int yRot = direction.getAxis().isVertical() ? (alongFirst ? 0 : 90) : (int)direction.toYRot();
         ((PartBuilder)((PartBuilder)((PartBuilder)builder.part()
                     .modelFile(model(provider, "middle", false, vertical))
                     .rotationY(yRot)
                     .rotationX(xRot)
                     .addModel())
                  .condition(DirectionalGearshiftBlock.FACING, new Direction[]{direction})
                  .condition(DirectionalGearshiftBlock.AXIS_ALONG_FIRST_COORDINATE, new Boolean[]{alongFirst})
                  .end()
                  .part()
                  .modelFile(model(provider, "left", leftOn, vertical))
                  .rotationY(yRot)
                  .rotationX(xRot)
                  .addModel())
               .condition(DirectionalGearshiftBlock.LEFT_POWERED, new Boolean[]{leftOn})
               .condition(DirectionalGearshiftBlock.FACING, new Direction[]{direction})
               .condition(DirectionalGearshiftBlock.AXIS_ALONG_FIRST_COORDINATE, new Boolean[]{alongFirst})
               .end()
               .part()
               .modelFile(model(provider, "right", rightOn, vertical))
               .rotationY(yRot)
               .rotationX(xRot)
               .addModel())
            .condition(DirectionalGearshiftBlock.RIGHT_POWERED, new Boolean[]{rightOn})
            .condition(DirectionalGearshiftBlock.FACING, new Direction[]{direction})
            .condition(DirectionalGearshiftBlock.AXIS_ALONG_FIRST_COORDINATE, new Boolean[]{alongFirst})
            .end()
            .part();
      }
   }

   private static ModelFile model(RegistrateBlockstateProvider p, String part, boolean powered, boolean vertical) {
      return p.models()
         .getExistingFile(Simulated.path("block/directional_gearshift/" + (vertical ? "vertical/" : "horizontal/") + part + (powered ? "_powered" : "")));
   }
}

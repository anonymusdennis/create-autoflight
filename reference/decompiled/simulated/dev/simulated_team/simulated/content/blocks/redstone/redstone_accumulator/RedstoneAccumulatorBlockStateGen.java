package dev.simulated_team.simulated.content.blocks.redstone.redstone_accumulator;

import com.google.common.collect.UnmodifiableIterator;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import dev.simulated_team.simulated.Simulated;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel.Builder;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder.PartBuilder;

public class RedstoneAccumulatorBlockStateGen {
   public static Builder<PartBuilder> rotateHorizontal(Direction direction, Builder<PartBuilder> builder) {
      int angleOffset = 0;
      builder.rotationY(((int)direction.toYRot() + 0) % 360);
      return builder;
   }

   public static <P extends RedstoneAccumulatorBlock> NonNullBiConsumer<DataGenContext<Block, P>, RegistrateBlockstateProvider> generate() {
      return (ctx, prov) -> {
         ModelFile backOff = sub(prov, "block_back_off");
         ModelFile backOn = sub(prov, "block_back_on");
         ModelFile front = sub(prov, "block_front");
         ModelFile middleOff = sub(prov, "block_middle_off");
         ModelFile middleOn = sub(prov, "block_middle_on");
         ModelFile torchOff = sub(prov, "torch_off");
         ModelFile torchOn = sub(prov, "torch_on");
         MultiPartBlockStateBuilder builder = prov.getMultipartBuilder((Block)ctx.get());
         UnmodifiableIterator var10 = ((RedstoneAccumulatorBlock)ctx.get()).getStateDefinition().getPossibleStates().iterator();

         while (var10.hasNext()) {
            BlockState state = (BlockState)var10.next();
            Direction facing = (Direction)state.getValue(RedstoneAccumulatorBlock.FACING);
            boolean powered = (Boolean)state.getValue(RedstoneAccumulatorBlock.POWERED);
            boolean sidePowered = (Boolean)state.getValue(RedstoneAccumulatorBlock.SIDE_POWERED);
            boolean powering = (Boolean)state.getValue(RedstoneAccumulatorBlock.POWERING);
            int yRot = (int)facing.getOpposite().toYRot();
            ((PartBuilder)((PartBuilder)((PartBuilder)((PartBuilder)builder.part().modelFile(front).rotationY(yRot).addModel())
                        .condition(RedstoneAccumulatorBlock.FACING, new Direction[]{facing})
                        .end()
                        .part()
                        .modelFile(powered ? backOn : backOff)
                        .rotationY(yRot)
                        .addModel())
                     .condition(RedstoneAccumulatorBlock.POWERED, new Boolean[]{powered})
                     .condition(RedstoneAccumulatorBlock.FACING, new Direction[]{facing})
                     .end()
                     .part()
                     .modelFile(sidePowered ? middleOn : middleOff)
                     .rotationY(yRot)
                     .addModel())
                  .condition(RedstoneAccumulatorBlock.SIDE_POWERED, new Boolean[]{sidePowered})
                  .condition(RedstoneAccumulatorBlock.FACING, new Direction[]{facing})
                  .end()
                  .part()
                  .modelFile(powering ? torchOn : torchOff)
                  .rotationY(yRot)
                  .addModel())
               .condition(RedstoneAccumulatorBlock.POWERING, new Boolean[]{powering})
               .condition(RedstoneAccumulatorBlock.FACING, new Direction[]{facing})
               .end();
         }
      };
   }

   private static ModelFile sub(RegistrateBlockstateProvider p, String suffix) {
      return p.models().getExistingFile(Simulated.path("block/redstone_accumulator/" + suffix));
   }
}

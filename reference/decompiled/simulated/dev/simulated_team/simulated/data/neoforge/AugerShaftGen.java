package dev.simulated_team.simulated.data.neoforge;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.auger_shaft.AugerShaftBlock;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel.Builder;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder.PartBuilder;

public class AugerShaftGen {
   public static Builder<PartBuilder> rotate(Direction direction, Builder<PartBuilder> builder) {
      builder.rotationX(direction.getAxis().isHorizontal() ? -90 : (direction == Direction.UP ? 180 : 0));
      builder.rotationY(direction.getAxis().isVertical() ? 0 : ((int)direction.toYRot() + 180) % 360);
      return builder;
   }

   public static <P extends AugerShaftBlock> NonNullBiConsumer<DataGenContext<Block, P>, RegistrateBlockstateProvider> generate(String name, boolean cog) {
      return (c, p) -> {
         ModelFile axis_y = cog ? sub(p, name, "cog_axis_y") : sub(p, name, "axis_y");
         ModelFile connection_top = sub(p, name, "connection_top");
         MultiPartBlockStateBuilder builder = p.getMultipartBuilder((Block)c.get());

         for (Axis dir : Axis.values()) {
            ((PartBuilder)rotate(Direction.get(AxisDirection.POSITIVE, dir), builder.part().modelFile(axis_y)).addModel())
               .condition(AugerShaftBlock.AXIS, new Axis[]{dir})
               .condition(AugerShaftBlock.ENCASED, new Boolean[]{false})
               .end();
         }

         for (Axis dir : Axis.values()) {
            ((PartBuilder)rotate(Direction.get(AxisDirection.POSITIVE, dir), builder.part().modelFile(sub(p, name, "axis_y_encased"))).addModel())
               .condition(AugerShaftBlock.AXIS, new Axis[]{dir})
               .condition(AugerShaftBlock.ENCASED, new Boolean[]{true})
               .end();
         }

         for (Axis dir : Axis.values()) {
            ((PartBuilder)rotate(Direction.get(AxisDirection.POSITIVE, dir), builder.part().modelFile(connection_top)).addModel())
               .condition(AugerShaftBlock.AXIS, new Axis[]{dir})
               .condition(AugerShaftBlock.SECTION, new AugerShaftBlock.BarrelSection[]{AugerShaftBlock.BarrelSection.END, AugerShaftBlock.BarrelSection.SINGLE})
               .condition(AugerShaftBlock.ENCASED, new Boolean[]{false})
               .end();
            ((PartBuilder)rotate(Direction.get(AxisDirection.NEGATIVE, dir), builder.part().modelFile(connection_top)).addModel())
               .condition(AugerShaftBlock.AXIS, new Axis[]{dir})
               .condition(
                  AugerShaftBlock.SECTION, new AugerShaftBlock.BarrelSection[]{AugerShaftBlock.BarrelSection.FRONT, AugerShaftBlock.BarrelSection.SINGLE}
               )
               .condition(AugerShaftBlock.ENCASED, new Boolean[]{false})
               .end();
         }

         if (!cog) {
            for (Direction dir : Direction.values()) {
               ((PartBuilder)rotate(dir.getOpposite(), builder.part().modelFile(sub(p, name, "bracket_top_" + dir.getAxis().getName()))).addModel())
                  .condition((Property)AugerShaftBlock.PROPERTY_BY_DIRECTION.get(dir), new Boolean[]{true})
                  .condition(AugerShaftBlock.ENCASED, new Boolean[]{false})
                  .end();
            }
         }
      };
   }

   private static ModelFile sub(RegistrateBlockstateProvider p, String name, String suffix) {
      return p.models().getExistingFile(Simulated.path("block/auger_shaft/" + suffix));
   }
}

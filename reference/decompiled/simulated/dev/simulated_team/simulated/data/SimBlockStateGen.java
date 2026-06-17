package dev.simulated_team.simulated.data;

import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.redstone.DirectedDirectionalBlock;
import com.simibubi.create.foundation.data.BlockStateGen;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import dev.simulated_team.simulated.content.blocks.redstone.redstone_inductor.RedstoneInductorBlock;
import dev.simulated_team.simulated.content.blocks.symmetric_sail.SymmetricSailBlock;
import dev.simulated_team.simulated.content.blocks.util.AbstractDirectionalAxisBlock;
import dev.simulated_team.simulated.service.SimBlockStateService;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;

public class SimBlockStateGen {
   public static <T extends DirectionalAxisKineticBlock> void directionalKineticAxisBlockstate(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov) {
      BlockStateGen.directionalAxisBlock(
         ctx,
         prov,
         (blockState, vertical) -> prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName() + "/block_" + (vertical ? "vertical" : "horizontal")))
      );
   }

   public static <T extends Block> void facingPoweredAxisBlockstate(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov) {
      prov.directionalBlock(
         (Block)ctx.getEntry(),
         blockState -> prov.models()
               .getExistingFile(prov.modLoc("block/" + ctx.getName() + "/block" + (blockState.getValue(BlockStateProperties.POWERED) ? "_powered" : "")))
      );
   }

   public static <T extends Block> void facingBlockstate(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov, String modelPath) {
      prov.directionalBlock((Block)ctx.getEntry(), blockState -> prov.models().getExistingFile(prov.modLoc(modelPath)));
   }

   public static <T extends Block> void horizontalFacingLitBlockstate(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov) {
      prov.horizontalBlock(
         (Block)ctx.get(),
         blockState -> prov.models()
               .getExistingFile(prov.modLoc("block/" + ctx.getName() + "/block" + (blockState.getValue(AbstractFurnaceBlock.LIT) ? "_lit" : "")))
      );
   }

   public static <T extends Block> void redstoneInductorBlockstate(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov) {
      prov.horizontalBlock(
         (Block)ctx.getEntry(),
         blockState -> {
            boolean inverted = (Boolean)blockState.getValue(RedstoneInductorBlock.INVERTED);
            return prov.models()
               .getExistingFile(
                  prov.modLoc(
                     "block/"
                        + ctx.getName()
                        + "/block"
                        + (inverted ? "_inverted" : "")
                        + (blockState.getValue(BlockStateProperties.POWERED) ? "_powered" : "")
                  )
               );
         }
      );
   }

   public static <T extends DirectionalAxisKineticBlock> void directionalPoweredAxisBlockstate(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov) {
      BlockStateGen.directionalAxisBlock(
         ctx,
         prov,
         (blockState, vertical) -> prov.models()
               .getExistingFile(
                  prov.modLoc(
                     "block/"
                        + ctx.getName()
                        + "/block_"
                        + (vertical ? "vertical" : "horizontal")
                        + (blockState.getValue(BlockStateProperties.POWERED) ? "_powered" : "")
                  )
               )
      );
   }

   public static <I extends BlockItem> NonNullBiConsumer<DataGenContext<Item, I>, RegistrateItemModelProvider> coloredBlockItemModel(
      String texture, String... folders
   ) {
      return (c, p) -> {
         String path = "block";

         for (String folder : folders) {
            path = path + "/" + ("_".equals(folder) ? c.getName() : folder);
         }

         ((ItemModelBuilder)p.withExistingParent(c.getName(), p.modLoc(path))).texture("0", p.modLoc("block/" + texture));
      };
   }

   public static <T extends AbstractDirectionalAxisBlock> void directionalAxisBlock(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov) {
      SimBlockStateService.INSTANCE
         .directionalAxisBlock(
            ctx,
            prov,
            (blockState, vertical) -> prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName() + "/block_" + (vertical ? "vertical" : "horizontal")))
         );
   }

   public static SimBlockStateGen.XYHolder xySymmetricSail(BlockState state) {
      Axis axis = (Axis)state.getValue(SymmetricSailBlock.AXIS);
      return new SimBlockStateGen.XYHolder(axis == Axis.Y ? 0 : 90, axis == Axis.X ? 90 : (axis == Axis.Z ? 180 : 0));
   }

   public static SimBlockStateGen.XYHolder xyAltitudeSensor(BlockState state) {
      int yRot = ((int)((Direction)state.getValue(BlockStateProperties.HORIZONTAL_FACING)).toYRot() + 180) % 360;
      int xRot = ((AttachFace)state.getValue(BlockStateProperties.ATTACH_FACE)).ordinal() * 90;
      return new SimBlockStateGen.XYHolder(xRot, yRot);
   }

   public static <I extends BlockItem, P> NonNullFunction<ItemBuilder<I, P>, P> customItemModel(ResourceLocation path) {
      return b -> b.model(customBlockItemModel(path)).build();
   }

   public static <I extends BlockItem> NonNullBiConsumer<DataGenContext<Item, I>, RegistrateItemModelProvider> customBlockItemModel(ResourceLocation path) {
      return (c, p) -> p.withExistingParent(c.getName(), path);
   }

   public static SimBlockStateGen.XYHolder xyLaser(BlockState state) {
      Direction dir = (Direction)state.getValue(BlockStateProperties.HORIZONTAL_FACING);
      int yRot = (int)((dir.getAxis().isVertical() ? 0.0F : dir.toYRot()) + 180.0F);

      int xRot = switch ((AttachFace)state.getValue(DirectedDirectionalBlock.TARGET)) {
         case CEILING -> -90;
         case WALL -> 0;
         case FLOOR -> 90;
         default -> throw new MatchException(null, null);
      };
      return new SimBlockStateGen.XYHolder((xRot + 360) % 360, (yRot + 360) % 360);
   }

   public static record XYHolder(int xRot, int yRot) {
   }
}

package dev.simulated_team.simulated.data.neoforge;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.SequencedAssemblyRecipeGen;
import com.simibubi.create.api.data.recipe.BaseRecipeProvider.GeneratedRecipe;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe.Builder;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.kinetics.saw.CuttingRecipe;
import dev.simulated_team.simulated.index.SimItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

public class SimSequencedAssemblyRecipes extends SequencedAssemblyRecipeGen {
   private final GeneratedRecipe GYRO_MECHANISM = this.create(
      "gyroscopic_mechanism",
      b -> b.require(AllItems.IRON_SHEET)
            .transitionTo(SimItems.INCOMPLETE_GYRO_MECHANISM)
            .addStep(DeployerApplicationRecipe::new, rb -> (Builder)rb.require((ItemLike)AllBlocks.COGWHEEL.get()))
            .addStep(DeployerApplicationRecipe::new, rb -> (Builder)rb.require((ItemLike)AllBlocks.SHAFT.get()))
            .addStep(DeployerApplicationRecipe::new, rb -> (Builder)rb.require(AllItems.BRASS_NUGGET))
            .loops(5)
            .addOutput(SimItems.GYRO_MECHANISM, 200.0F)
            .addOutput(AllItems.IRON_SHEET, 8.0F)
            .addOutput(AllItems.ANDESITE_ALLOY, 8.0F)
            .addOutput(AllItems.BRASS_NUGGET, 3.0F)
            .addOutput(AllItems.CRUSHED_IRON, 2.0F)
            .addOutput(Items.COMPASS.asItem(), 1.0F)
   );
   private final GeneratedRecipe ENGINE_ASSEMBLY = this.create(
      "engine_assembly",
      b -> b.require(AllItems.IRON_SHEET)
            .transitionTo(SimItems.INCOMPLETE_ENGINE_ASSEMBLY)
            .addStep(CuttingRecipe::new, rb -> rb)
            .addStep(PressingRecipe::new, rb -> rb)
            .loops(8)
            .addOutput(SimItems.ENGINE_ASSEMBLY, 50.0F)
            .addOutput(AllItems.IRON_SHEET, 16.0F)
            .addOutput(Items.IRON_NUGGET, 15.0F)
            .addOutput(AllBlocks.INDUSTRIAL_IRON_BLOCK, 10.0F)
            .addOutput(Blocks.IRON_BARS, 8.0F)
            .addOutput(Items.IRON_HELMET, 1.0F)
   );

   public SimSequencedAssemblyRecipes(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries, "simulated");
   }

   public String getName() {
      return "Simulated's Splendid Sequenced Assembly Recipes";
   }
}

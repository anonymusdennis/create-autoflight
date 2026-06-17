package com.simibubi.create.infrastructure.data;

import com.google.gson.JsonObject;
import java.util.concurrent.CompletableFuture;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.PathProvider;
import net.minecraft.data.PackOutput.Target;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;

public class CreateWikiBlockInfoProvider implements DataProvider {
   private final PathProvider path;

   public CreateWikiBlockInfoProvider(PackOutput output) {
      this.path = output.createPathProvider(Target.DATA_PACK, ".wiki/block_info/");
   }

   public CompletableFuture<?> run(CachedOutput cachedOutput) {
      return CompletableFuture.allOf(
         BuiltInRegistries.BLOCK.stream().filter(b -> RegisteredObjectsHelper.getKeyOrThrow(b).getNamespace().equals("create")).map(block -> {
            BlockState state = block.defaultBlockState();
            ResourceLocation id = RegisteredObjectsHelper.getKeyOrThrow(block);
            JsonObject element = new JsonObject();
            ItemLike item = RegisteredObjectsHelper.getItemOrBlock(id);
            if (item != null) {
               element.addProperty("stackable", item.asItem().getDefaultInstance().getMaxStackSize());
            }

            element.addProperty("blast_resistance", block.getExplosionResistance());
            element.addProperty("hardness", block.defaultDestroyTime());
            element.addProperty("luminous", state.getLightEmission() > 0);
            element.addProperty("waterloggable", block instanceof SimpleWaterloggedBlock);
            element.addProperty("flammable", ((FireBlock)Blocks.FIRE).getBurnOdds(state) > 0);
            element.addProperty("ignited_by_lava", state.ignitedByLava());
            return DataProvider.saveStable(cachedOutput, element, this.path.json(id));
         }).toArray(CompletableFuture[]::new)
      );
   }

   public String getName() {
      return "Create's Wiki Block Info Provider";
   }
}

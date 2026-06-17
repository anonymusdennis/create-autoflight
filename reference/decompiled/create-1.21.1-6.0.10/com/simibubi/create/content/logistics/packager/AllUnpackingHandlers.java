package com.simibubi.create.content.logistics.packager;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.api.packager.unpacking.VoidingUnpackingHandler;
import com.simibubi.create.impl.unpacking.BasinUnpackingHandler;
import com.simibubi.create.impl.unpacking.CrafterUnpackingHandler;
import net.minecraft.world.level.block.Block;

public class AllUnpackingHandlers {
   public static void registerDefaults() {
      UnpackingHandler.REGISTRY.register((Block)AllBlocks.BASIN.get(), BasinUnpackingHandler.INSTANCE);
      UnpackingHandler.REGISTRY.register((Block)AllBlocks.CREATIVE_CRATE.get(), VoidingUnpackingHandler.INSTANCE);
      UnpackingHandler.REGISTRY.register((Block)AllBlocks.MECHANICAL_CRAFTER.get(), CrafterUnpackingHandler.INSTANCE);
   }
}

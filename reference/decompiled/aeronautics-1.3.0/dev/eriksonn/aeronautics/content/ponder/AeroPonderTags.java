package dev.eriksonn.aeronautics.content.ponder;

import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.index.AeroBlocks;
import dev.eriksonn.aeronautics.service.AeroLevititeService;
import dev.simulated_team.simulated.index.SimPonderTags;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

public class AeroPonderTags {
   public static final ResourceLocation LEVITITE_BREAKABLE = Aeronautics.path("levitite_breakable");

   public static void register(PonderTagRegistrationHelper<ResourceLocation> helper) {
      PonderTagRegistrationHelper<ItemLike> itemHelper = helper.withKeyFunction(RegisteredObjectsHelper::getKeyOrThrow);
      helper.registerTag(LEVITITE_BREAKABLE)
         .item(AeroLevititeService.INSTANCE.getBucket())
         .title("Breaks When Crystallizing")
         .description("Blocks that are broken when nearby Levitite Blend crystallizes into Levitite. Useful for making molds for casting")
         .register();
      itemHelper.addToTag(LEVITITE_BREAKABLE).add(AeroLevititeService.INSTANCE.getBucket());
      itemHelper.addToTag(LEVITITE_BREAKABLE).add(Blocks.CLAY).add(Blocks.MUD).add(Blocks.PACKED_MUD).add(Blocks.COARSE_DIRT);
      itemHelper.addToTag(SimPonderTags.PHYSICS_BEHAVIOR)
         .add(AeroBlocks.PROPELLER_BEARING.asItem())
         .add(AeroBlocks.GYROSCOPIC_PROPELLER_BEARING.asItem())
         .add(AeroBlocks.SMART_PROPELLER.asItem())
         .add(AeroBlocks.ANDESITE_PROPELLER.asItem())
         .add(AeroBlocks.WOODEN_PROPELLER.asItem())
         .add(AeroBlocks.WHITE_ENVELOPE_BLOCK.asItem())
         .add(AeroBlocks.HOT_AIR_BURNER.asItem())
         .add(AeroBlocks.STEAM_VENT.asItem())
         .add(AeroBlocks.LEVITITE.asItem())
         .add(AeroBlocks.PEARLESCENT_LEVITITE.asItem());
      itemHelper.addToTag(SimPonderTags.THRUST_PRODUCING_BLOCKS)
         .add(AeroBlocks.PROPELLER_BEARING.asItem())
         .add(AeroBlocks.GYROSCOPIC_PROPELLER_BEARING.asItem())
         .add(AeroBlocks.SMART_PROPELLER.asItem())
         .add(AeroBlocks.ANDESITE_PROPELLER.asItem())
         .add(AeroBlocks.WOODEN_PROPELLER.asItem());
      itemHelper.addToTag(AllCreatePonderTags.KINETIC_APPLIANCES)
         .add(AeroBlocks.PROPELLER_BEARING.asItem())
         .add(AeroBlocks.GYROSCOPIC_PROPELLER_BEARING.asItem())
         .add(AeroBlocks.SMART_PROPELLER.asItem())
         .add(AeroBlocks.ANDESITE_PROPELLER.asItem())
         .add(AeroBlocks.WOODEN_PROPELLER.asItem())
         .add(AeroBlocks.MOUNTED_POTATO_CANNON.asItem());
      itemHelper.addToTag(AllCreatePonderTags.ARM_TARGETS).add(AeroBlocks.MOUNTED_POTATO_CANNON.asItem());
      itemHelper.addToTag(AllCreatePonderTags.THRESHOLD_SWITCH_TARGETS).add(AeroBlocks.HOT_AIR_BURNER.asItem()).add(AeroBlocks.STEAM_VENT.asItem());
      itemHelper.addToTag(AllCreatePonderTags.DISPLAY_SOURCES)
         .add(AeroBlocks.HOT_AIR_BURNER.asItem())
         .add(AeroBlocks.STEAM_VENT.asItem())
         .add(AeroBlocks.PROPELLER_BEARING.asItem())
         .add(AeroBlocks.GYROSCOPIC_PROPELLER_BEARING.asItem())
         .add(AeroBlocks.SMART_PROPELLER.asItem())
         .add(AeroBlocks.ANDESITE_PROPELLER.asItem())
         .add(AeroBlocks.WOODEN_PROPELLER.asItem());
   }
}

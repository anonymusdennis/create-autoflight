package com.simibubi.create.infrastructure;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.content.decoration.palettes.AllPaletteBlocks;
import com.simibubi.create.foundation.data.recipe.CommonMetal;
import java.util.HashMap;
import java.util.Map;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber
public class RemapHelper {
   private static final Map<String, ResourceLocation> reMap = new HashMap<>();

   private static void remapPaletteBlock(String type, String newType, boolean vanilla) {
      reMap.put("%s_cobblestone_stairs".formatted(type), Create.asResource("cut_%s_stairs".formatted(newType)));
      reMap.put("%s_cobblestone_slab".formatted(type), Create.asResource("cut_%s_slab".formatted(newType)));
      reMap.put("%s_cobblestone_wall".formatted(type), Create.asResource("cut_%s_wall".formatted(newType)));
      if (!vanilla) {
         if (type != "gabbro") {
            reMap.put("%s_cobblestone".formatted(type), Create.asResource("%s".formatted(newType)));
         }

         reMap.put("polished_%s".formatted(type), Create.asResource("polished_cut_%s".formatted(newType)));
         reMap.put("polished_%s_stairs".formatted(type), Create.asResource("polished_cut_%s_stairs".formatted(newType)));
         reMap.put("polished_%s_slab".formatted(type), Create.asResource("polished_cut_%s_slab".formatted(newType)));
         reMap.put("polished_%s_wall".formatted(type), Create.asResource("polished_cut_%s_wall".formatted(newType)));
      }

      reMap.put("%s_bricks".formatted(type), Create.asResource("cut_%s_bricks".formatted(newType)));
      reMap.put("%s_bricks_stairs".formatted(type), Create.asResource("cut_%s_brick_stairs".formatted(newType)));
      reMap.put("%s_bricks_slab".formatted(type), Create.asResource("cut_%s_brick_slab".formatted(newType)));
      reMap.put("%s_bricks_wall".formatted(type), Create.asResource("cut_%s_brick_wall".formatted(newType)));
      reMap.put("fancy_%s_bricks".formatted(type), Create.asResource("small_%s_bricks".formatted(newType)));
      reMap.put("fancy_%s_bricks_stairs".formatted(type), Create.asResource("small_%s_brick_stairs".formatted(newType)));
      reMap.put("fancy_%s_bricks_slab".formatted(type), Create.asResource("small_%s_brick_slab".formatted(newType)));
      reMap.put("fancy_%s_bricks_wall".formatted(type), Create.asResource("small_%s_brick_wall".formatted(newType)));
      reMap.put("paved_%s".formatted(type), Create.asResource("small_%s_bricks".formatted(newType)));
      reMap.put("paved_%s_stairs".formatted(type), Create.asResource("small_%s_brick_stairs".formatted(newType)));
      reMap.put("paved_%s_slab".formatted(type), Create.asResource("small_%s_brick_slab".formatted(newType)));
      reMap.put("paved_%s_wall".formatted(type), Create.asResource("small_%s_brick_wall".formatted(newType)));
      if (!vanilla) {
         reMap.put("chiseled_%s".formatted(type), Create.asResource("polished_cut_%s".formatted(newType)));
      }

      reMap.put("mossy_%s".formatted(type), Create.asResource("cut_%s_bricks".formatted(newType)));
      reMap.put("overgrown_%s".formatted(type), Create.asResource("cut_%s_bricks".formatted(newType)));
      if (!type.equals(newType)) {
         reMap.put("layered_%s".formatted(type), Create.asResource("layered_%s".formatted(newType)));
         reMap.put("%s_pillar".formatted(type), Create.asResource("%s_pillar".formatted(newType)));
      }
   }

   @SubscribeEvent
   public static void remap(RegisterEvent event) {
      Registry<?> registry = event.getRegistry();
      if (registry == Registries.BLOCK || registry == Registries.ITEM) {
         reMap.forEach((string, resourceLocation) -> registry.addAlias(Create.asResource(string), resourceLocation));
      }

      if (registry == Registries.FLUID) {
         registry.addAlias(Create.asResource("milk"), NeoForgeMod.MILK.getId());
         registry.addAlias(Create.asResource("flowing_milk"), NeoForgeMod.FLOWING_MILK.getId());
      }

      if (registry == Registries.BLOCK_ENTITY_TYPE) {
         registry.addAlias(Create.asResource("copper_backtank"), AllBlockEntityTypes.BACKTANK.getId());
         registry.addAlias(Create.asResource("adjustable_pulley"), AllBlockEntityTypes.ADJUSTABLE_CHAIN_GEARSHIFT.getId());
      }
   }

   static {
      reMap.put("toggle_latch", AllBlocks.POWERED_TOGGLE_LATCH.getId());
      reMap.put("encased_shaft", AllBlocks.ANDESITE_ENCASED_SHAFT.getId());
      reMap.put("encased_belt", AllBlocks.ENCASED_CHAIN_DRIVE.getId());
      reMap.put("adjustable_pulley", AllBlocks.ADJUSTABLE_CHAIN_GEARSHIFT.getId());
      reMap.put("stockswitch", AllBlocks.THRESHOLD_SWITCH.getId());
      reMap.put("redstone_latch", AllBlocks.POWERED_LATCH.getId());
      reMap.put("contact", AllBlocks.REDSTONE_CONTACT.getId());
      reMap.put("belt_funnel", AllBlocks.BRASS_BELT_FUNNEL.getId());
      reMap.put("entity_detector", AllBlocks.SMART_OBSERVER.getId());
      reMap.put("saw", AllBlocks.MECHANICAL_SAW.getId());
      reMap.put("flexpulsepeater", AllBlocks.PULSE_REPEATER.getId());
      reMap.put("stress_gauge", AllBlocks.STRESSOMETER.getId());
      reMap.put("harvester", AllBlocks.MECHANICAL_HARVESTER.getId());
      reMap.put("plough", AllBlocks.MECHANICAL_PLOUGH.getId());
      reMap.put("drill", AllBlocks.MECHANICAL_DRILL.getId());
      reMap.put("flexpeater", AllBlocks.PULSE_EXTENDER.getId());
      reMap.put("rotation_chassis", AllBlocks.RADIAL_CHASSIS.getId());
      reMap.put("belt_tunnel", AllBlocks.BRASS_TUNNEL.getId());
      reMap.put("redstone_bridge", AllBlocks.REDSTONE_LINK.getId());
      reMap.put("speed_gauge", AllBlocks.SPEEDOMETER.getId());
      reMap.put("translation_chassis", AllBlocks.LINEAR_CHASSIS.getId());
      reMap.put("translation_chassis_secondary", AllBlocks.SECONDARY_LINEAR_CHASSIS.getId());
      reMap.put("piston_pole", AllBlocks.PISTON_EXTENSION_POLE.getId());
      reMap.put("adjustable_pulse_repeater", AllBlocks.PULSE_REPEATER.getId());
      reMap.put("adjustable_repeater", AllBlocks.PULSE_REPEATER.getId());
      reMap.put("copper_block", RegisteredObjectsHelper.getKeyOrThrow(Blocks.COPPER_BLOCK));
      reMap.put("copper_ore", RegisteredObjectsHelper.getKeyOrThrow(Blocks.COPPER_ORE));
      reMap.put("acacia_glass", AllPaletteBlocks.ACACIA_WINDOW.getId());
      reMap.put("acacia_glass_pane", AllPaletteBlocks.ACACIA_WINDOW_PANE.getId());
      reMap.put("birch_glass", AllPaletteBlocks.BIRCH_WINDOW.getId());
      reMap.put("birch_glass_pane", AllPaletteBlocks.BIRCH_WINDOW_PANE.getId());
      reMap.put("dark_oak_glass", AllPaletteBlocks.DARK_OAK_WINDOW.getId());
      reMap.put("dark_oak_glass_pane", AllPaletteBlocks.DARK_OAK_WINDOW_PANE.getId());
      reMap.put("jungle_glass", AllPaletteBlocks.JUNGLE_WINDOW.getId());
      reMap.put("jungle_glass_pane", AllPaletteBlocks.JUNGLE_WINDOW_PANE.getId());
      reMap.put("oak_glass", AllPaletteBlocks.OAK_WINDOW.getId());
      reMap.put("oak_glass_pane", AllPaletteBlocks.OAK_WINDOW_PANE.getId());
      reMap.put("iron_glass", AllPaletteBlocks.ORNATE_IRON_WINDOW.getId());
      reMap.put("iron_glass_pane", AllPaletteBlocks.ORNATE_IRON_WINDOW_PANE.getId());
      reMap.put("spruce_glass", AllPaletteBlocks.SPRUCE_WINDOW.getId());
      reMap.put("spruce_glass_pane", AllPaletteBlocks.SPRUCE_WINDOW_PANE.getId());
      reMap.put("limestone_stairs", Create.asResource("cut_limestone_stairs"));
      reMap.put("weathered_limestone_layers", Create.asResource("layered_tuff"));
      reMap.put("indented_gabbro_slab", Create.asResource("polished_cut_dripstone_slab"));
      reMap.put("andesite_layers", Create.asResource("layered_andesite"));
      reMap.put("scoria_layers", Create.asResource("layered_scoria"));
      reMap.put("dark_scoria_tiles_stairs", Create.asResource("cut_scorchia_brick_stairs"));
      reMap.put("dolomite_stairs", Create.asResource("polished_cut_calcite_stairs"));
      reMap.put("paved_gabbro_bricks", Create.asResource("cut_dripstone_bricks"));
      reMap.put("slightly_mossy_gabbro_bricks", Create.asResource("cut_dripstone_bricks"));
      reMap.put("limestone_wall", Create.asResource("polished_cut_limestone_wall"));
      reMap.put("dark_scoria_tiles", Create.asResource("cut_scorchia_bricks"));
      reMap.put("dark_scoria_tiles_slab", Create.asResource("cut_scorchia_brick_slab"));
      reMap.put("weathered_limestone_stairs", Create.asResource("polished_cut_tuff_stairs"));
      reMap.put("limestone_slab", Create.asResource("polished_cut_limestone_slab"));
      reMap.put("scoria_slab", Create.asResource("polished_cut_scoria_slab"));
      reMap.put("dolomite_wall", Create.asResource("polished_cut_calcite_wall"));
      reMap.put("gabbro_layers", Create.asResource("layered_dripstone"));
      reMap.put("scoria_wall", Create.asResource("polished_cut_scoria_wall"));
      reMap.put("gabbro_slab", Create.asResource("polished_cut_dripstone_slab"));
      reMap.put("dolomite_slab", Create.asResource("polished_cut_calcite_slab"));
      reMap.put("mossy_gabbro_bricks", Create.asResource("cut_dripstone_bricks"));
      reMap.put("paved_gabbro_bricks_slab", Create.asResource("cut_dripstone_brick_slab"));
      reMap.put("gabbro_wall", Create.asResource("cut_gabbro_wall"));
      reMap.put("granite_layers", Create.asResource("layered_granite"));
      reMap.put("indented_gabbro", Create.asResource("polished_cut_dripstone"));
      reMap.put("scoria_stairs", Create.asResource("polished_cut_scoria_stairs"));
      reMap.put("weathered_limestone_wall", Create.asResource("polished_cut_tuff_wall"));
      reMap.put("diorite_layers", Create.asResource("layered_diorite"));
      reMap.put("weathered_limestone_slab", Create.asResource("polished_cut_tuff_slab"));
      reMap.put("gabbro_stairs", Create.asResource("polished_cut_dripstone_stairs"));
      reMap.put("limestone_layers", Create.asResource("layered_limestone"));
      reMap.put("gabbro", ResourceLocation.withDefaultNamespace("dripstone_block"));
      reMap.put("dolomite", ResourceLocation.withDefaultNamespace("calcite"));
      reMap.put("weathered_limestone", ResourceLocation.withDefaultNamespace("tuff"));
      reMap.put("gabbro_cobblestone", ResourceLocation.withDefaultNamespace("dripstone_block"));
      reMap.put("andesite_cobblestone", ResourceLocation.withDefaultNamespace("andesite"));
      reMap.put("diorite_cobblestone", ResourceLocation.withDefaultNamespace("diorite"));
      reMap.put("granite_cobblestone", ResourceLocation.withDefaultNamespace("granite"));
      reMap.put("dark_scoria", Create.asResource("scorchia"));
      remapPaletteBlock("andesite", "andesite", true);
      remapPaletteBlock("diorite", "diorite", true);
      remapPaletteBlock("granite", "granite", true);
      remapPaletteBlock("limestone", "limestone", false);
      remapPaletteBlock("gabbro", "dripstone", false);
      remapPaletteBlock("scoria", "scoria", false);
      remapPaletteBlock("dark_scoria", "scorchia", false);
      remapPaletteBlock("dolomite", "calcite", false);
      remapPaletteBlock("weathered_limestone", "tuff", false);
      reMap.put("natural_scoria", Create.asResource("scoria"));
      reMap.put("empty_blueprint", AllItems.SCHEMATIC.getId());
      reMap.put("gold_sheet", AllItems.GOLDEN_SHEET.getId());
      reMap.put("flour", AllItems.WHEAT_FLOUR.getId());
      reMap.put("blueprint_and_quill", AllItems.SCHEMATIC_AND_QUILL.getId());
      reMap.put("slot_cover", AllItems.CRAFTER_SLOT_COVER.getId());
      reMap.put("blueprint", AllItems.SCHEMATIC.getId());
      reMap.put("symmetry_wand", AllItems.WAND_OF_SYMMETRY.getId());
      reMap.put("terrain_zapper", AllItems.WORLDSHAPER.getId());
      reMap.put("property_filter", AllItems.ATTRIBUTE_FILTER.getId());
      reMap.put("obsidian_dust", AllItems.POWDERED_OBSIDIAN.getId());
      reMap.put("diving_helmet", AllItems.COPPER_DIVING_HELMET.getId());
      reMap.put("diving_boots", AllItems.COPPER_DIVING_BOOTS.getId());

      for (String metal : new String[]{"iron", "gold", "copper", "zinc"}) {
         reMap.put("crushed_" + metal + "_ore", Create.asResource("crushed_raw_" + metal));
      }

      for (CommonMetal compatMetal : CommonMetal.values()) {
         reMap.put("crushed_" + compatMetal.name + "_ore", Create.asResource("crushed_raw_" + compatMetal.name));
      }
   }
}

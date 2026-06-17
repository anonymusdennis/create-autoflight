package com.simibubi.create.foundation.utility;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.createmod.catnip.data.Couple;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

public class DyeHelper {
   private static final Map<DyeColor, Supplier<ItemLike>> WOOL_TABLE = new HashMap<>();
   private static final Map<DyeColor, Couple<Integer>> DYE_TABLE = new HashMap<>();

   public static ItemLike getWoolOfDye(DyeColor color) {
      return WOOL_TABLE.getOrDefault(color, () -> Blocks.WHITE_WOOL).get();
   }

   public static Couple<Integer> getDyeColors(DyeColor color) {
      return DYE_TABLE.getOrDefault(color, DYE_TABLE.get(DyeColor.WHITE));
   }

   public static void addDye(DyeColor color, Integer brightColor, Integer darkColor, Supplier<ItemLike> wool) {
      DYE_TABLE.put(color, Couple.create(brightColor, darkColor));
      WOOL_TABLE.put(color, wool);
   }

   private static void addDye(DyeColor color, Integer brightColor, Integer darkColor, ItemLike wool) {
      addDye(color, brightColor, darkColor, (Supplier<ItemLike>)(() -> wool));
   }

   static {
      addDye(DyeColor.BLACK, 4538427, 2170911, Blocks.BLACK_WOOL);
      addDye(DyeColor.RED, 11614519, 6498103, Blocks.RED_WOOL);
      addDye(DyeColor.GREEN, 2132550, 1925189, Blocks.GREEN_WOOL);
      addDye(DyeColor.BROWN, 11306332, 6837054, Blocks.BROWN_WOOL);
      addDye(DyeColor.BLUE, 5476833, 5262224, Blocks.BLUE_WOOL);
      addDye(DyeColor.GRAY, 6121071, 3224888, Blocks.GRAY_WOOL);
      addDye(DyeColor.LIGHT_GRAY, 9803419, 7368816, Blocks.LIGHT_GRAY_WOOL);
      addDye(DyeColor.PURPLE, 10441902, 6501996, Blocks.PURPLE_WOOL);
      addDye(DyeColor.CYAN, 4107188, 3962994, Blocks.CYAN_WOOL);
      addDye(DyeColor.PINK, 14002379, 12086165, Blocks.PINK_WOOL);
      addDye(DyeColor.LIME, 10739541, 5222767, Blocks.LIME_WOOL);
      addDye(DyeColor.YELLOW, 15128406, 15313961, Blocks.YELLOW_WOOL);
      addDye(DyeColor.LIGHT_BLUE, 6934226, 5278373, Blocks.LIGHT_BLUE_WOOL);
      addDye(DyeColor.ORANGE, 15635014, 14240039, Blocks.ORANGE_WOOL);
      addDye(DyeColor.MAGENTA, 15753904, 12600456, Blocks.MAGENTA_WOOL);
      addDye(DyeColor.WHITE, 15592165, 12302000, Blocks.WHITE_WOOL);
   }
}

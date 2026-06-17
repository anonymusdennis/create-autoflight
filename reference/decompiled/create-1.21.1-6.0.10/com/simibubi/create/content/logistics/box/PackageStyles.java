package com.simibubi.create.content.logistics.box;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.Create;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.ApiStatus.Internal;

public class PackageStyles {
   @Internal
   @Unmodifiable
   public static final List<PackageStyles.PackageStyle> STYLES = ImmutableList.of(
      new PackageStyles.PackageStyle("cardboard", 12, 12, 23.0F, false),
      new PackageStyles.PackageStyle("cardboard", 10, 12, 22.0F, false),
      new PackageStyles.PackageStyle("cardboard", 10, 8, 18.0F, false),
      new PackageStyles.PackageStyle("cardboard", 12, 10, 21.0F, false),
      rare("creeper"),
      rare("darcy"),
      rare("evan"),
      rare("jinx"),
      rare("kryppers"),
      rare("simi"),
      rare("starlotte"),
      rare("thunder"),
      new PackageStyles.PackageStyle[]{rare("up"), rare("vector")}
   );
   public static final List<PackageItem> ALL_BOXES = new ArrayList<>();
   public static final List<PackageItem> STANDARD_BOXES = new ArrayList<>();
   public static final List<PackageItem> RARE_BOXES = new ArrayList<>();
   private static final Random STYLE_PICKER = new Random();
   private static final int RARE_CHANCE = 7500;

   public static ItemStack getRandomBox() {
      List<PackageItem> pool = STYLE_PICKER.nextInt(7500) == 0 ? RARE_BOXES : STANDARD_BOXES;
      return new ItemStack((ItemLike)pool.get(STYLE_PICKER.nextInt(pool.size())));
   }

   public static ItemStack getDefaultBox() {
      return new ItemStack((ItemLike)ALL_BOXES.get(0));
   }

   private static PackageStyles.PackageStyle rare(String name) {
      return new PackageStyles.PackageStyle("rare_" + name, 12, 10, 21.0F, true);
   }

   public static record PackageStyle(String type, int width, int height, float riggingOffset, boolean rare) {
      public ResourceLocation getItemId() {
         String size = "_" + this.width + "x" + this.height;
         String id = this.type + "_package" + (this.rare ? "" : size);
         return Create.asResource(id);
      }

      public ResourceLocation getRiggingModel() {
         String size = this.width + "x" + this.height;
         return Create.asResource("item/package/rigging_" + size);
      }
   }
}

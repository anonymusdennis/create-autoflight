package com.simibubi.create.content.trains.bogey;

import com.simibubi.create.Create;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.UnmodifiableView;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class BogeySizes {
   private static final Map<ResourceLocation, BogeySizes.BogeySize> BOGEY_SIZES = new HashMap<>();
   private static final List<BogeySizes.BogeySize> SORTED_INCREASING = new ArrayList<>();
   private static final List<BogeySizes.BogeySize> SORTED_DECREASING = new ArrayList<>();
   @UnmodifiableView
   private static final Map<ResourceLocation, BogeySizes.BogeySize> BOGEY_SIZES_VIEW = Collections.unmodifiableMap(BOGEY_SIZES);
   @UnmodifiableView
   private static final List<BogeySizes.BogeySize> SORTED_INCREASING_VIEW = Collections.unmodifiableList(SORTED_INCREASING);
   @UnmodifiableView
   private static final List<BogeySizes.BogeySize> SORTED_DECREASING_VIEW = Collections.unmodifiableList(SORTED_DECREASING);
   public static final BogeySizes.BogeySize SMALL = new BogeySizes.BogeySize(Create.asResource("small"), 0.40625F);
   public static final BogeySizes.BogeySize LARGE = new BogeySizes.BogeySize(Create.asResource("large"), 0.78125F);

   private BogeySizes() {
   }

   public static void register(BogeySizes.BogeySize size) {
      ResourceLocation id = size.id();
      if (BOGEY_SIZES.containsKey(id)) {
         throw new IllegalArgumentException();
      } else {
         BOGEY_SIZES.put(id, size);
         SORTED_INCREASING.add(size);
         SORTED_DECREASING.add(size);
         SORTED_INCREASING.sort(Comparator.comparing(BogeySizes.BogeySize::wheelRadius));
         SORTED_DECREASING.sort(Comparator.comparing(BogeySizes.BogeySize::wheelRadius).reversed());
      }
   }

   @UnmodifiableView
   public static Map<ResourceLocation, BogeySizes.BogeySize> all() {
      return BOGEY_SIZES_VIEW;
   }

   @UnmodifiableView
   public static List<BogeySizes.BogeySize> allSortedIncreasing() {
      return SORTED_INCREASING_VIEW;
   }

   @UnmodifiableView
   public static List<BogeySizes.BogeySize> allSortedDecreasing() {
      return SORTED_DECREASING_VIEW;
   }

   @Internal
   public static void init() {
   }

   static {
      register(SMALL);
      register(LARGE);
   }

   public static record BogeySize(ResourceLocation id, float wheelRadius) {
      public BogeySizes.BogeySize nextBySize() {
         List<BogeySizes.BogeySize> values = BogeySizes.allSortedIncreasing();
         int ordinal = values.indexOf(this);
         return values.get((ordinal + 1) % values.size());
      }
   }
}

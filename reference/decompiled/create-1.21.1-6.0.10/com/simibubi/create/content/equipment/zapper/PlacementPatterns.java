package com.simibubi.create.content.equipment.zapper;

import com.google.common.base.Predicates;
import com.mojang.serialization.Codec;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.foundation.gui.AllIcons;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Predicate;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.lang.Lang;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public enum PlacementPatterns implements StringRepresentable {
   Solid(AllIcons.I_PATTERN_SOLID),
   Checkered(AllIcons.I_PATTERN_CHECKERED),
   InverseCheckered(AllIcons.I_PATTERN_CHECKERED_INVERSED),
   Chance25(AllIcons.I_PATTERN_CHANCE_25),
   Chance50(AllIcons.I_PATTERN_CHANCE_50),
   Chance75(AllIcons.I_PATTERN_CHANCE_75);

   public static final Codec<PlacementPatterns> CODEC = StringRepresentable.fromValues(PlacementPatterns::values);
   public static final StreamCodec<ByteBuf, PlacementPatterns> STREAM_CODEC = CatnipStreamCodecBuilders.ofEnum(PlacementPatterns.class);
   public final String translationKey = Lang.asId(this.name());
   public final AllIcons icon;

   private PlacementPatterns(AllIcons icon) {
      this.icon = icon;
   }

   public static void applyPattern(List<BlockPos> blocksIn, ItemStack stack, RandomSource random) {
      PlacementPatterns pattern = (PlacementPatterns)stack.getOrDefault(AllDataComponents.PLACEMENT_PATTERN, Solid);
      Predicate<BlockPos> filter = Predicates.alwaysFalse();

      blocksIn.removeIf((Predicate<? super BlockPos>)(switch (pattern) {
         case Checkered -> pos -> (pos.getX() + pos.getY() + pos.getZ()) % 2 == 0;
         case InverseCheckered -> pos -> (pos.getX() + pos.getY() + pos.getZ()) % 2 != 0;
         case Chance25 -> pos -> random.nextBoolean() || random.nextBoolean();
         case Chance50 -> pos -> random.nextBoolean();
         case Chance75 -> pos -> random.nextBoolean() && random.nextBoolean();
      }));
   }

   @NotNull
   public String getSerializedName() {
      return Lang.asId(this.name());
   }
}

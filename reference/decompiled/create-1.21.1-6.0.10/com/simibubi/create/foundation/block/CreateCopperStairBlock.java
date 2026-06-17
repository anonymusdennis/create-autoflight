package com.simibubi.create.foundation.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import org.jetbrains.annotations.NotNull;

public class CreateCopperStairBlock extends StairBlock {
   public static final MapCodec<StairBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(propertiesCodec()).apply(i, CreateCopperStairBlock::new));

   public CreateCopperStairBlock(Properties properties) {
      super(Blocks.AIR.defaultBlockState(), properties);
   }

   public float getExplosionResistance() {
      return this.explosionResistance;
   }

   @NotNull
   public MapCodec<? extends StairBlock> codec() {
      return CODEC;
   }
}

package com.simibubi.create.foundation.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.level.block.WeatheringCopperStairBlock;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import org.jetbrains.annotations.NotNull;

public class CreateWeatheringCopperStairBlock extends WeatheringCopperStairBlock {
   public static final MapCodec<WeatheringCopperStairBlock> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(WeatherState.CODEC.fieldOf("weathering_state").forGetter(ChangeOverTimeBlock::getAge), propertiesCodec())
            .apply(i, CreateWeatheringCopperStairBlock::new)
   );

   public CreateWeatheringCopperStairBlock(WeatherState weatherState, Properties properties) {
      super(weatherState, Blocks.AIR.defaultBlockState(), properties);
   }

   public float getExplosionResistance() {
      return this.explosionResistance;
   }

   @NotNull
   public MapCodec<WeatheringCopperStairBlock> codec() {
      return CODEC;
   }
}

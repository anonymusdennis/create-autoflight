package com.simibubi.create.content.processing.recipe;

import com.mojang.serialization.Codec;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.lang.Lang;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum HeatCondition implements StringRepresentable {
   NONE(16777215),
   HEATED(15237888),
   SUPERHEATED(6067176);

   private int color;
   public static final Codec<HeatCondition> CODEC = StringRepresentable.fromEnum(HeatCondition::values);
   public static final StreamCodec<ByteBuf, HeatCondition> STREAM_CODEC = CatnipStreamCodecBuilders.ofEnum(HeatCondition.class);

   private HeatCondition(int color) {
      this.color = color;
   }

   public boolean testBlazeBurner(BlazeBurnerBlock.HeatLevel level) {
      if (this == SUPERHEATED) {
         return level == BlazeBurnerBlock.HeatLevel.SEETHING;
      } else {
         return this != HEATED ? true : level != BlazeBurnerBlock.HeatLevel.NONE && level != BlazeBurnerBlock.HeatLevel.SMOULDERING;
      }
   }

   public BlazeBurnerBlock.HeatLevel visualizeAsBlazeBurner() {
      if (this == SUPERHEATED) {
         return BlazeBurnerBlock.HeatLevel.SEETHING;
      } else {
         return this == HEATED ? BlazeBurnerBlock.HeatLevel.KINDLED : BlazeBurnerBlock.HeatLevel.NONE;
      }
   }

   @NotNull
   public String getSerializedName() {
      return Lang.asId(this.name());
   }

   public String getTranslationKey() {
      return "recipe.heat_requirement." + this.getSerializedName();
   }

   public int getColor() {
      return this.color;
   }
}

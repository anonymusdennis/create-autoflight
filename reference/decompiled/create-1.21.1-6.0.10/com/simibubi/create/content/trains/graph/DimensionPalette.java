package com.simibubi.create.content.trains.graph;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class DimensionPalette {
   public static final StreamCodec<ByteBuf, DimensionPalette> STREAM_CODEC = CatnipStreamCodecBuilders.list(ResourceKey.streamCodec(Registries.DIMENSION))
      .map(DimensionPalette::new, i -> i.gatheredDims);
   private final List<ResourceKey<Level>> gatheredDims;

   public DimensionPalette() {
      this.gatheredDims = new ArrayList<>();
   }

   public DimensionPalette(List<ResourceKey<Level>> gatheredDims) {
      this.gatheredDims = gatheredDims;
   }

   public int encode(ResourceKey<Level> dimension) {
      int indexOf = this.gatheredDims.indexOf(dimension);
      if (indexOf == -1) {
         indexOf = this.gatheredDims.size();
         this.gatheredDims.add(dimension);
      }

      return indexOf;
   }

   public ResourceKey<Level> decode(int index) {
      return this.gatheredDims.size() > index && index >= 0 ? this.gatheredDims.get(index) : Level.OVERWORLD;
   }

   public void send(FriendlyByteBuf buffer) {
      buffer.writeInt(this.gatheredDims.size());
      this.gatheredDims.forEach(rk -> buffer.writeResourceLocation(rk.location()));
   }

   public static DimensionPalette receive(FriendlyByteBuf buffer) {
      DimensionPalette palette = new DimensionPalette();
      int length = buffer.readInt();

      for (int i = 0; i < length; i++) {
         palette.gatheredDims.add(ResourceKey.create(Registries.DIMENSION, buffer.readResourceLocation()));
      }

      return palette;
   }

   public void write(CompoundTag tag) {
      tag.put("DimensionPalette", NBTHelper.writeCompoundList(this.gatheredDims, rk -> {
         CompoundTag c = new CompoundTag();
         c.putString("Id", rk.location().toString());
         return c;
      }));
   }

   public static DimensionPalette read(CompoundTag tag) {
      DimensionPalette palette = new DimensionPalette();
      NBTHelper.iterateCompoundList(
         tag.getList("DimensionPalette", 10),
         c -> palette.gatheredDims.add(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(c.getString("Id"))))
      );
      return palette;
   }
}

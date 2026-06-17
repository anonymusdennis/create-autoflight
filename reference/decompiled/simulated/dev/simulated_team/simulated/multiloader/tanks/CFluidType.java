package dev.simulated_team.simulated.multiloader.tanks;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DataResult.Error;
import dev.simulated_team.simulated.Simulated;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

public class CFluidType {
   public final Fluid fluid;
   DataComponentMap data;
   public static final CFluidType BLANK = new CFluidType(Fluids.EMPTY, null);

   public CFluidType(ResourceLocation type, @Nullable DataComponentMap data) {
      this.fluid = (Fluid)BuiltInRegistries.FLUID.get(type);
      this.data = data;
   }

   public CFluidType(Fluid type, @Nullable DataComponentMap data) {
      this.fluid = type;
      this.data = data;
   }

   public boolean isBlank() {
      return this.equals(BLANK);
   }

   public CompoundTag write() {
      CompoundTag tag = new CompoundTag();
      tag.putString("Fluid", BuiltInRegistries.FLUID.getKey(this.fluid).toString());
      if (this.data != null) {
         Codec<DataComponentMap> codec = DataComponentMap.CODEC;
         DataResult<Tag> result = codec.encodeStart(NbtOps.INSTANCE, this.data);
         if (result.isError()) {
            Simulated.LOGGER.warn(((Error)result.error().get()).message());
         } else {
            tag.put("data", (Tag)result.result().get());
         }
      }

      return tag;
   }

   public static CFluidType read(CompoundTag tag) {
      DataComponentMap map = null;
      if (tag.contains("data")) {
         DataResult<Pair<DataComponentMap, Tag>> result = DataComponentMap.CODEC.decode(NbtOps.INSTANCE, tag.getCompound("data"));
         if (result.isError()) {
            Simulated.LOGGER.warn(((Error)result.error().get()).message());
         } else {
            map = (DataComponentMap)((Pair)result.result().get()).getFirst();
         }
      }

      return new CFluidType(ResourceLocation.parse(tag.getString("Fluid")), map);
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else {
         if (obj instanceof CFluidType other && this.data == null == (other.data == null)) {
            return this.fluid.isSame(other.fluid) && (this.data == null || this.data.equals(other.data));
         }

         return false;
      }
   }
}

package com.simibubi.create.content.fluids.potion;

import com.mojang.serialization.Codec;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllFluids;
import com.simibubi.create.content.fluids.VirtualFluid;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.lang.Lang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.BaseFlowingFluid.Properties;
import org.jetbrains.annotations.NotNull;

public class PotionFluid extends VirtualFluid {
   public static PotionFluid createSource(Properties properties) {
      return new PotionFluid(properties, true);
   }

   public static PotionFluid createFlowing(Properties properties) {
      return new PotionFluid(properties, false);
   }

   public PotionFluid(Properties properties, boolean source) {
      super(properties, source);
   }

   public static FluidStack of(int amount, PotionContents potionContents, PotionFluid.BottleType bottleType) {
      FluidStack fluidStack = new FluidStack(((PotionFluid)AllFluids.POTION.get()).getSource(), amount);
      addPotionToFluidStack(fluidStack, potionContents);
      fluidStack.set(AllDataComponents.POTION_FLUID_BOTTLE_TYPE, bottleType);
      return fluidStack;
   }

   public static FluidStack addPotionToFluidStack(FluidStack fs, PotionContents potionContents) {
      if (potionContents == PotionContents.EMPTY) {
         fs.remove(DataComponents.POTION_CONTENTS);
         return fs;
      } else {
         fs.set(DataComponents.POTION_CONTENTS, potionContents);
         return fs;
      }
   }

   public static enum BottleType implements StringRepresentable {
      REGULAR,
      SPLASH,
      LINGERING;

      public static final Codec<PotionFluid.BottleType> CODEC = StringRepresentable.fromEnum(PotionFluid.BottleType::values);
      public static final StreamCodec<ByteBuf, PotionFluid.BottleType> STREAM_CODEC = CatnipStreamCodecBuilders.ofEnum(PotionFluid.BottleType.class);

      @NotNull
      public String getSerializedName() {
         return Lang.asId(this.name());
      }
   }

   public static class PotionFluidType extends AllFluids.TintedFluidType {
      public PotionFluidType(net.neoforged.neoforge.fluids.FluidType.Properties properties, ResourceLocation stillTexture, ResourceLocation flowingTexture) {
         super(properties, stillTexture, flowingTexture);
      }

      @Override
      public int getTintColor(FluidStack stack) {
         return ((PotionContents)stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)).getColor() | 0xFF000000;
      }

      public String getDescriptionId(FluidStack stack) {
         PotionContents contents = (PotionContents)stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
         ItemLike itemFromBottleType = PotionFluidHandler.itemFromBottleType(
            (PotionFluid.BottleType)stack.getOrDefault(AllDataComponents.POTION_FLUID_BOTTLE_TYPE, PotionFluid.BottleType.REGULAR)
         );
         return Potion.getName(contents.potion(), itemFromBottleType.asItem().getDescriptionId() + ".effect.");
      }

      @Override
      protected int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
         return -1;
      }
   }
}

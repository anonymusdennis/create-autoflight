package net.createmod.catnip.platform;

import net.createmod.catnip.platform.services.ModFluidHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public class NeoForgeFluidHelper implements ModFluidHelper<FluidStack> {
   @OnlyIn(Dist.CLIENT)
   public int getColor(FluidStack stack, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos) {
      Fluid fluid = stack.getFluid();
      IClientFluidTypeExtensions extension = IClientFluidTypeExtensions.of(fluid);
      return level != null && pos != null ? extension.getTintColor(fluid.defaultFluidState(), level, pos) : extension.getTintColor(stack);
   }

   public int getLuminosity(FluidStack fluid) {
      return fluid.getFluid().getFluidType().getLightLevel();
   }

   @OnlyIn(Dist.CLIENT)
   @Nullable
   public TextureAtlasSprite getStillTexture(FluidStack fluid) {
      ResourceLocation id = IClientFluidTypeExtensions.of(fluid.getFluid()).getStillTexture(fluid);
      return id == null ? null : (TextureAtlasSprite)Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(id);
   }

   public boolean isLighterThanAir(FluidStack fluid) {
      return fluid.getFluid().getFluidType().isLighterThanAir();
   }

   public FluidStack toStack(FluidState state) {
      return new FluidStack(state.getType(), 1000);
   }
}

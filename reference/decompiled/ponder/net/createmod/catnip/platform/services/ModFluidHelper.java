package net.createmod.catnip.platform.services;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public interface ModFluidHelper<R> {
   @OnlyIn(Dist.CLIENT)
   int getColor(R var1, @Nullable BlockAndTintGetter var2, @Nullable BlockPos var3);

   int getLuminosity(R var1);

   @OnlyIn(Dist.CLIENT)
   @Nullable
   TextureAtlasSprite getStillTexture(R var1);

   @OnlyIn(Dist.CLIENT)
   default TextureAtlasSprite getStillTextureOrMissing(R fluid) {
      TextureAtlasSprite texture = this.getStillTexture(fluid);
      return texture != null
         ? texture
         : (TextureAtlasSprite)Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(MissingTextureAtlasSprite.getLocation());
   }

   boolean isLighterThanAir(R var1);

   R toStack(FluidState var1);
}

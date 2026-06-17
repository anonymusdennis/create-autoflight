package dev.ryanhcode.sable.sublevel.render.vanilla;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SingleBlockSubLevelWrapper implements BlockAndTintGetter {
   private ClientLevel level;
   private final MutableBlockPos globalPos = new MutableBlockPos();
   private final MutableBlockPos localPos = new MutableBlockPos();
   private BlockState state;

   public void setup(ClientLevel level, double x, double y, double z, BlockPos localPos, BlockState state) {
      this.level = level;
      this.globalPos.set(x, y, z);
      this.localPos.set(localPos);
      this.state = state;
   }

   public void clear() {
      this.level = null;
   }

   public float getShade(Direction direction, boolean bl) {
      return this.level.getShade(direction, bl);
   }

   @NotNull
   public LevelLightEngine getLightEngine() {
      return this.level.getLightEngine();
   }

   public int getBrightness(LightLayer lightLayer, BlockPos pos) {
      return this.getLightEngine().getLayerListener(lightLayer).getLightValue(this.globalPos);
   }

   public int getRawBrightness(BlockPos pos, int i) {
      return this.getLightEngine().getRawBrightness(this.globalPos, i);
   }

   public boolean canSeeSky(BlockPos pos) {
      return this.getBrightness(LightLayer.SKY, this.globalPos) >= this.getMaxLightLevel();
   }

   public int getBlockTint(BlockPos pos, ColorResolver colorResolver) {
      return this.level.getBlockTint(pos, colorResolver);
   }

   @Nullable
   public BlockEntity getBlockEntity(BlockPos pos) {
      return this.level.getBlockEntity(pos);
   }

   @NotNull
   public BlockState getBlockState(BlockPos pos) {
      return pos.equals(this.localPos) ? this.state : Blocks.AIR.defaultBlockState();
   }

   @NotNull
   public FluidState getFluidState(BlockPos pos) {
      return pos.equals(this.localPos) ? this.state.getFluidState() : Fluids.EMPTY.defaultFluidState();
   }

   public int getHeight() {
      return this.level.getHeight();
   }

   public int getMinBuildHeight() {
      return this.level.getMinBuildHeight();
   }

   public ClientLevel getLevel() {
      return this.level;
   }
}

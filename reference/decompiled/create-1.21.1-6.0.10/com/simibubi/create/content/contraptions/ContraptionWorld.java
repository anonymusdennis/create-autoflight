package com.simibubi.create.content.contraptions;

import net.createmod.catnip.levelWrappers.WrappedLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public class ContraptionWorld extends WrappedLevel {
   final Contraption contraption;
   private final int minY;
   private final int height;

   public ContraptionWorld(Level world, Contraption contraption) {
      super(world);
      this.contraption = contraption;
      this.minY = nextMultipleOf16(contraption.bounds.minY - 1.0);
      this.height = nextMultipleOf16(contraption.bounds.maxY + 1.0) - this.minY;
   }

   private static int nextMultipleOf16(double a) {
      return ((Math.abs((int)a) - 1 | 15) + 1) * Mth.sign(a);
   }

   public BlockState getBlockState(BlockPos pos) {
      StructureBlockInfo blockInfo = this.contraption.getBlocks().get(pos);
      return blockInfo != null ? blockInfo.state() : Blocks.AIR.defaultBlockState();
   }

   public void playLocalSound(double x, double y, double z, SoundEvent sound, SoundSource category, float volume, float pitch, boolean distanceDelay) {
      this.level.playLocalSound(x, y, z, sound, category, volume, pitch, distanceDelay);
   }

   public int getHeight() {
      return this.height;
   }

   public int getMinBuildHeight() {
      return this.minY;
   }
}

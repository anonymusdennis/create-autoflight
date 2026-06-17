package com.simibubi.create.content.fluids.tank;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongListIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.Level;

public class SoundPool {
   private final int maxConcurrent;
   private final int mergeTicks;
   private final SoundPool.Sound sound;
   private final LongList queuedPositions = new LongArrayList();
   private final MutableBlockPos pos = new MutableBlockPos();
   private int ticks = 0;

   public SoundPool(int maxConcurrent, int mergeTicks, SoundPool.Sound sound) {
      this.maxConcurrent = maxConcurrent;
      this.sound = sound;
      this.mergeTicks = mergeTicks;
   }

   public void queueAt(BlockPos pos) {
      this.queueAt(pos.asLong());
   }

   public void queueAt(long pos) {
      this.queuedPositions.add(pos);
   }

   public void play(Level level) {
      if (!this.queuedPositions.isEmpty()) {
         this.ticks++;
         if (this.ticks >= this.mergeTicks) {
            this.ticks = 0;
            int numberOfPositions = this.queuedPositions.size();
            if (numberOfPositions <= this.maxConcurrent) {
               LongListIterator var3 = this.queuedPositions.iterator();

               while (var3.hasNext()) {
                  long pos = (Long)var3.next();
                  this.playAt(level, pos);
               }
            } else {
               while (!this.queuedPositions.isEmpty() && this.queuedPositions.size() > numberOfPositions - this.maxConcurrent) {
                  this.rollNextPosition(level);
               }
            }

            this.queuedPositions.clear();
         }
      }
   }

   private void rollNextPosition(Level level) {
      int index = level.random.nextInt(this.queuedPositions.size());
      long pos = this.queuedPositions.removeLong(index);
      this.playAt(level, pos);
   }

   private void playAt(Level level, long pos) {
      this.sound.playAt(level, this.pos.set(pos));
   }

   public interface Sound {
      void playAt(Level var1, Vec3i var2);
   }
}

package dev.ryanhcode.sable.neoforge.compatibility.flywheel;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.core.SectionPos;
import org.jetbrains.annotations.Nullable;

public final class SableLightLut {
   public final SableLightLut.Layer<SableLightLut.Layer<SableLightLut.Layer<SableLightLut.IntLayer>>> indices = new SableLightLut.Layer<>();

   public void add(int scene, long position, int index) {
      int x = SectionPos.x(position);
      int y = SectionPos.y(position);
      int z = SectionPos.z(position);
      this.indices
         .computeIfAbsent(scene, SableLightLut.Layer::new)
         .computeIfAbsent(y, SableLightLut.Layer::new)
         .computeIfAbsent(x, SableLightLut.IntLayer::new)
         .set(z, index + 1);
   }

   public void prune() {
      this.indices.prune(scene -> scene.prune(middle -> middle.prune(SableLightLut.IntLayer::prune)));
   }

   public void remove(int scene, long section) {
      int x = SectionPos.x(section);
      int y = SectionPos.y(section);
      int z = SectionPos.z(section);
      SableLightLut.Layer<SableLightLut.Layer<SableLightLut.IntLayer>> first = this.indices.get(scene);
      if (first != null) {
         SableLightLut.Layer<SableLightLut.IntLayer> second = first.get(y);
         if (second != null) {
            SableLightLut.IntLayer third = second.get(x);
            if (third != null) {
               third.clear(z);
            }
         }
      }
   }

   public IntArrayList flatten() {
      IntArrayList out = new IntArrayList();
      this.indices
         .fillLut(out, (sceneIndices, lut1) -> sceneIndices.fillLut(lut1, (yIndices, lut2) -> yIndices.fillLut(lut2, SableLightLut.IntLayer::fillLut)));
      return out;
   }

   public static final class IntLayer {
      private boolean hasBase = false;
      private int base = 0;
      private int[] indices = new int[0];

      public void fillLut(IntArrayList lut) {
         lut.add(this.base);
         lut.add(this.indices.length);

         for (int index : this.indices) {
            lut.add(index);
         }
      }

      public int base() {
         return this.base;
      }

      public int size() {
         return this.indices.length;
      }

      public int getRaw(int i) {
         if (i < 0) {
            return 0;
         } else {
            return i >= this.indices.length ? 0 : this.indices[i];
         }
      }

      public int get(int i) {
         return !this.hasBase ? 0 : this.getRaw(i - this.base);
      }

      public void set(int i, int index) {
         if (!this.hasBase) {
            this.base = i;
            this.hasBase = true;
         }

         if (i < this.base) {
            this.rebase(i);
         }

         int offset = i - this.base;
         if (offset >= this.indices.length) {
            this.resize(offset + 1);
         }

         this.indices[offset] = index;
      }

      public boolean prune() {
         if (!this.hasBase) {
            return true;
         } else {
            int leadingZeros = this.getLeadingZeros();
            if (leadingZeros == this.indices.length) {
               return true;
            } else {
               int trailingZeros = this.getTrailingZeros();
               if (leadingZeros == 0 && trailingZeros == 0) {
                  return false;
               } else {
                  int[] newIndices = new int[this.indices.length - leadingZeros - trailingZeros];
                  System.arraycopy(this.indices, leadingZeros, newIndices, 0, newIndices.length);
                  this.indices = newIndices;
                  this.base += leadingZeros;
                  return false;
               }
            }
         }
      }

      private int getTrailingZeros() {
         int out = 0;

         for (int i = this.indices.length - 1; i >= 0 && this.indices[i] == 0; i--) {
            out++;
         }

         return out;
      }

      private int getLeadingZeros() {
         int out = 0;

         for (int index : this.indices) {
            if (index != 0) {
               break;
            }

            out++;
         }

         return out;
      }

      public void clear(int i) {
         if (this.hasBase) {
            if (i >= this.base) {
               int offset = i - this.base;
               if (offset < this.indices.length) {
                  this.indices[offset] = 0;
               }
            }
         }
      }

      private void resize(int length) {
         int[] newIndices = new int[length];
         System.arraycopy(this.indices, 0, newIndices, 0, this.indices.length);
         this.indices = newIndices;
      }

      private void rebase(int newBase) {
         int growth = this.base - newBase;
         int[] newIndices = new int[this.indices.length + growth];
         System.arraycopy(this.indices, 0, newIndices, growth, this.indices.length);
         this.indices = newIndices;
         this.base = newBase;
      }
   }

   public static final class Layer<T> {
      private boolean hasBase = false;
      private int base = 0;
      private Object[] nextLayer = new Object[0];

      public void fillLut(IntArrayList lut, BiConsumer<T, IntArrayList> inner) {
         lut.add(this.base);
         lut.add(this.nextLayer.length);
         int innerIndexBase = lut.size();
         lut.size(innerIndexBase + this.nextLayer.length);

         for (int i = 0; i < this.nextLayer.length; i++) {
            T innerIndices = (T)this.nextLayer[i];
            if (innerIndices != null) {
               int layerPosition = lut.size();
               lut.set(innerIndexBase + i, layerPosition);
               inner.accept(innerIndices, lut);
            }
         }
      }

      public int base() {
         return this.base;
      }

      public int size() {
         return this.nextLayer.length;
      }

      @Nullable
      public T getRaw(int i) {
         if (i < 0) {
            return null;
         } else {
            return (T)(i >= this.nextLayer.length ? null : this.nextLayer[i]);
         }
      }

      @Nullable
      public T get(int i) {
         return !this.hasBase ? null : this.getRaw(i - this.base);
      }

      public T computeIfAbsent(int i, Supplier<T> ifAbsent) {
         if (!this.hasBase) {
            this.base = i;
            this.hasBase = true;
         }

         if (i < this.base) {
            this.rebase(i);
         }

         int offset = i - this.base;
         if (offset >= this.nextLayer.length) {
            this.resize(offset + 1);
         }

         Object out = this.nextLayer[offset];
         if (out == null) {
            out = ifAbsent.get();
            this.nextLayer[offset] = out;
         }

         return (T)out;
      }

      public boolean prune(SableLightLut.Prune<T> inner) {
         if (!this.hasBase) {
            return true;
         } else {
            for (int i = 0; i < this.nextLayer.length; i++) {
               Object o = this.nextLayer[i];
               if (o != null && inner.prune((T)o)) {
                  this.nextLayer[i] = null;
               }
            }

            int leadingZeros = this.getLeadingZeros();
            if (leadingZeros == this.nextLayer.length) {
               return true;
            } else {
               int trailingZeros = this.getTrailingZeros();
               if (leadingZeros == 0 && trailingZeros == 0) {
                  return false;
               } else {
                  Object[] newIndices = new Object[this.nextLayer.length - leadingZeros - trailingZeros];
                  System.arraycopy(this.nextLayer, leadingZeros, newIndices, 0, newIndices.length);
                  this.nextLayer = newIndices;
                  this.base += leadingZeros;
                  return false;
               }
            }
         }
      }

      private int getLeadingZeros() {
         int out = 0;

         for (Object index : this.nextLayer) {
            if (index != null) {
               break;
            }

            out++;
         }

         return out;
      }

      private int getTrailingZeros() {
         int out = 0;

         for (int i = this.nextLayer.length - 1; i >= 0 && this.nextLayer[i] == null; i--) {
            out++;
         }

         return out;
      }

      private void resize(int length) {
         Object[] newIndices = new Object[length];
         System.arraycopy(this.nextLayer, 0, newIndices, 0, this.nextLayer.length);
         this.nextLayer = newIndices;
      }

      private void rebase(int newBase) {
         int growth = this.base - newBase;
         Object[] newIndices = new Object[this.nextLayer.length + growth];
         System.arraycopy(this.nextLayer, 0, newIndices, growth, this.nextLayer.length);
         this.nextLayer = newIndices;
         this.base = newBase;
      }
   }

   @FunctionalInterface
   public interface Prune<T> {
      boolean prune(T var1);
   }
}

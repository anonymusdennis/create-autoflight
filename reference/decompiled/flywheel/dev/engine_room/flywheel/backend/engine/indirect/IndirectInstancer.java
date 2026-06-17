package dev.engine_room.flywheel.backend.engine.indirect;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceWriter;
import dev.engine_room.flywheel.backend.engine.AbstractInstancer;
import dev.engine_room.flywheel.backend.engine.InstanceHandleImpl;
import dev.engine_room.flywheel.backend.engine.InstancerKey;
import dev.engine_room.flywheel.backend.util.AtomicBitSet;
import dev.engine_room.flywheel.lib.math.MoreMath;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4fc;
import org.lwjgl.system.MemoryUtil;

public class IndirectInstancer<I extends Instance> extends AbstractInstancer<I> {
   private final long instanceStride;
   private final InstanceWriter<I> writer;
   private final List<IndirectDraw> associatedDraws = new ArrayList<>();
   private final Vector4fc boundingSphere;
   private final AtomicReference<IndirectInstancer.InstancePage<I>[]> pages = new AtomicReference<>(pageArray(0));
   private final AtomicInteger instanceCount = new AtomicInteger(0);
   private final AtomicBitSet validityChanged = new AtomicBitSet();
   private final AtomicBitSet contentsChanged = new AtomicBitSet();
   private final AtomicBitSet fullPages = new AtomicBitSet();
   private final AtomicBitSet mergeablePages = new AtomicBitSet();
   public ObjectStorage.Mapping mapping;
   private int modelIndex = -1;
   private int baseInstance = -1;

   public IndirectInstancer(InstancerKey<I> key, AbstractInstancer.Recreate<I> recreate) {
      super(key, recreate);
      this.instanceStride = (long)MoreMath.align4(this.type.layout().byteSize());
      this.writer = this.type.writer();
      this.boundingSphere = key.model().boundingSphere();
   }

   private static <I extends Instance> IndirectInstancer.InstancePage<I>[] pageArray(int length) {
      return new IndirectInstancer.InstancePage[length];
   }

   private static <I extends Instance> I[] instanceArray() {
      return (I[])(new Instance[32]);
   }

   private static <I extends Instance> InstanceHandleImpl<I>[] handleArray() {
      return new InstanceHandleImpl[32];
   }

   @Nullable
   public static IndirectInstancer<?> fromState(InstanceHandleImpl.State<?> handle) {
      return handle instanceof IndirectInstancer.InstancePage<?> instancer ? instancer.parent : null;
   }

   public void addDraw(IndirectDraw draw) {
      this.associatedDraws.add(draw);
   }

   public List<IndirectDraw> draws() {
      return this.associatedDraws;
   }

   public void update(int modelIndex, int baseInstance) {
      this.baseInstance = baseInstance;
      boolean sameModelIndex = this.modelIndex == modelIndex;
      if (!sameModelIndex || !this.validityChanged.isEmpty()) {
         this.modelIndex = modelIndex;
         IndirectInstancer.InstancePage<I>[] pages = this.pages.get();
         this.mapping.updateCount(pages.length);
         if (sameModelIndex) {
            for (int page = this.validityChanged.nextSetBit(0); page >= 0 && page < pages.length; page = this.validityChanged.nextSetBit(page + 1)) {
               this.mapping.updatePage(page, modelIndex, pages[page].valid.get());
            }
         } else {
            for (int i = 0; i < pages.length; i++) {
               this.mapping.updatePage(i, modelIndex, pages[i].valid.get());
            }
         }

         this.validityChanged.clear();
      }
   }

   public void writeModel(long ptr) {
      MemoryUtil.memPutInt(ptr, 0);
      MemoryUtil.memPutInt(ptr + 4L, this.baseInstance);
      MemoryUtil.memPutInt(ptr + 8L, this.environment.matrixIndex());
      MemoryUtil.memPutFloat(ptr + 12L, this.boundingSphere.x());
      MemoryUtil.memPutFloat(ptr + 16L, this.boundingSphere.y());
      MemoryUtil.memPutFloat(ptr + 20L, this.boundingSphere.z());
      MemoryUtil.memPutFloat(ptr + 24L, this.boundingSphere.w());
   }

   public void uploadInstances(StagingBuffer stagingBuffer, int instanceVbo) {
      if (!this.contentsChanged.isEmpty()) {
         IndirectInstancer.InstancePage<I>[] pages = this.pages.get();

         for (int page = this.contentsChanged.nextSetBit(0); page >= 0 && page < pages.length; page = this.contentsChanged.nextSetBit(page + 1)) {
            I[] instances = pages[page].instances;
            long baseByte = this.mapping.page2ByteOffset(page);
            if (baseByte >= 0L) {
               long size = 32L * this.instanceStride;
               long direct = stagingBuffer.reserveForCopy(size, instanceVbo, baseByte);
               if (direct != 0L) {
                  for (I instance : instances) {
                     if (instance != null) {
                        this.writer.write(direct, instance);
                     }

                     direct += this.instanceStride;
                  }
               } else {
                  MemoryBlock block = stagingBuffer.getScratch(size);
                  long ptr = block.ptr();

                  for (I instance : instances) {
                     if (instance != null) {
                        this.writer.write(ptr, instance);
                     }

                     ptr += this.instanceStride;
                  }

                  stagingBuffer.enqueueCopy(block.ptr(), size, instanceVbo, baseByte);
               }
            }
         }

         this.contentsChanged.clear();
      }
   }

   @Override
   public void parallelUpdate() {
      IndirectInstancer.InstancePage<I>[] pages = this.pages.get();
      this.mergeablePages.clear(pages.length, this.mergeablePages.currentCapacity());
      int page = 0;

      while (this.mergeablePages.cardinality() > 1) {
         page = this.mergeablePages.nextSetBit(page);
         if (page < 0) {
            break;
         }

         int next = this.mergeablePages.nextSetBit(page + 1);
         if (next < 0) {
            break;
         }

         pages[page].takeFrom(pages[next]);
      }
   }

   private static boolean isFull(int valid) {
      return valid == -1;
   }

   private static boolean isEmpty(int valid) {
      return valid == 0;
   }

   private static boolean isMergeable(int valid) {
      return !isEmpty(valid) && Integer.bitCount(valid) <= 16;
   }

   @Override
   public void delete() {
      for (IndirectDraw draw : this.draws()) {
         draw.delete();
      }

      this.mapping.delete();
   }

   public int modelIndex() {
      return this.modelIndex;
   }

   public int baseInstance() {
      return this.baseInstance;
   }

   public int local2GlobalInstanceIndex(int instanceIndex) {
      return this.mapping.objectIndex2GlobalIndex(instanceIndex);
   }

   @Override
   public I createInstance() {
      InstanceHandleImpl<I> handle = new InstanceHandleImpl<>(null);
      I instance = this.type.create(handle);
      this.addInner(instance, handle);
      return instance;
   }

   @Override
   public InstanceHandleImpl.State<I> revealInstance(InstanceHandleImpl<I> handle, I instance) {
      this.addInner(instance, handle);
      return handle.state;
   }

   @Override
   public void stealInstance(@Nullable I instance) {
      if (instance != null) {
         if (instance.handle() instanceof InstanceHandleImpl<I> handle) {
            if (!(handle.state instanceof InstanceHandleImpl.Deleted)) {
               if (handle.state instanceof InstanceHandleImpl.Hidden<I> hidden && this.recreate.equals(hidden.recreate())) {
                  return;
               }

               if (handle.state instanceof IndirectInstancer.InstancePage<?> other) {
                  if (other.parent == this) {
                     return;
                  }

                  other.setDeleted(handle.index);
                  this.addInner(instance, handle);
               } else if (handle.state instanceof InstanceHandleImpl.Hidden) {
                  handle.state = new InstanceHandleImpl.Hidden<>(this.recreate, instance);
               }
            }
         }
      }
   }

   private void addInner(I instance, InstanceHandleImpl<I> handle) {
      IndirectInstancer.InstancePage<I>[] pages;
      do {
         pages = this.pages.get();

         for (int i = this.fullPages.nextClearBit(0); i < pages.length; i = this.fullPages.nextClearBit(i + 1)) {
            if (pages[i].add(instance, handle)) {
               return;
            }
         }

         int desiredLength = pages.length + 1;

         while (pages.length < desiredLength) {
            IndirectInstancer.InstancePage<I>[] newPages = pageArray(desiredLength);
            System.arraycopy(pages, 0, newPages, 0, pages.length);
            newPages[pages.length] = new IndirectInstancer.InstancePage<>(this, pages.length);
            if (this.pages.compareAndSet(pages, newPages)) {
               pages = newPages;
            } else {
               pages = this.pages.get();
            }
         }
      } while (!pages[pages.length - 1].add(instance, handle));
   }

   @Override
   public int instanceCount() {
      return this.instanceCount.get();
   }

   @Override
   public void clear() {
      this.pages.set(pageArray(0));
      this.contentsChanged.clear();
      this.validityChanged.clear();
      this.fullPages.clear();
      this.mergeablePages.clear();
   }

   private static final class InstancePage<I extends Instance> implements InstanceHandleImpl.State<I> {
      private final IndirectInstancer<I> parent;
      private final int pageNo;
      private final I[] instances;
      private final InstanceHandleImpl<I>[] handles;
      private final AtomicInteger valid;

      private InstancePage(IndirectInstancer<I> parent, int pageNo) {
         this.parent = parent;
         this.pageNo = pageNo;
         this.instances = (I[])IndirectInstancer.instanceArray();
         this.handles = IndirectInstancer.handleArray();
         this.valid = new AtomicInteger(0);
      }

      public boolean add(I instance, InstanceHandleImpl<I> handle) {
         int currentValue;
         int index;
         int newValue;
         do {
            currentValue = this.valid.get();
            if (IndirectInstancer.isFull(currentValue)) {
               return false;
            }

            index = Integer.numberOfTrailingZeros(~currentValue);
            newValue = currentValue | 1 << index;
         } while (!this.valid.compareAndSet(currentValue, newValue));

         this.instances[index] = instance;
         this.handles[index] = handle;
         handle.state = this;
         handle.index = this.local2HandleIndex(index);
         this.parent.contentsChanged.set(this.pageNo);
         this.parent.validityChanged.set(this.pageNo);
         if (IndirectInstancer.isFull(newValue)) {
            this.parent.fullPages.set(this.pageNo);
         }

         if (IndirectInstancer.isMergeable(newValue)) {
            this.parent.mergeablePages.set(this.pageNo);
         }

         this.parent.instanceCount.incrementAndGet();
         return true;
      }

      private int local2HandleIndex(int index) {
         return (this.pageNo << 5) + index;
      }

      @Override
      public InstanceHandleImpl.State<I> setChanged(int index) {
         this.parent.contentsChanged.set(this.pageNo);
         return this;
      }

      @Override
      public InstanceHandleImpl.State<I> setDeleted(int index) {
         int localIndex = index % 32;
         this.clear(localIndex);
         return InstanceHandleImpl.Deleted.instance();
      }

      @Override
      public InstanceHandleImpl.State<I> setVisible(InstanceHandleImpl<I> handle, int index, boolean visible) {
         if (visible) {
            return this;
         } else {
            int localIndex = index % 32;
            I out = this.instances[localIndex];
            this.clear(localIndex);
            return new InstanceHandleImpl.Hidden<>(this.parent.recreate, out);
         }
      }

      private void clear(int localIndex) {
         this.instances[localIndex] = null;
         this.handles[localIndex] = null;

         int currentValue;
         int newValue;
         do {
            currentValue = this.valid.get();
            newValue = currentValue & ~(1 << localIndex);
         } while (!this.valid.compareAndSet(currentValue, newValue));

         this.parent.validityChanged.set(this.pageNo);
         if (IndirectInstancer.isMergeable(newValue)) {
            this.parent.mergeablePages.set(this.pageNo);
         }

         this.parent.fullPages.clear(this.pageNo);
         this.parent.instanceCount.decrementAndGet();
      }

      private void takeFrom(IndirectInstancer.InstancePage<I> other) {
         int valid = this.valid.get();
         if (IndirectInstancer.isFull(valid)) {
            this.parent.mergeablePages.clear(this.pageNo);
         } else {
            int otherValid = other.valid.get();

            for (int i = 0; i < 32; i++) {
               int mask = 1 << i;
               if ((otherValid & mask) != 0) {
                  int writePos = Integer.numberOfTrailingZeros(~valid);
                  this.instances[writePos] = other.instances[i];
                  this.handles[writePos] = other.handles[i];
                  this.handles[writePos].state = this;
                  this.handles[writePos].index = this.local2HandleIndex(writePos);
                  otherValid &= ~mask;
                  other.handles[i] = null;
                  other.instances[i] = null;
                  valid |= 1 << writePos;
                  if (IndirectInstancer.isFull(valid)) {
                     break;
                  }
               }
            }

            this.valid.set(valid);
            other.valid.set(otherValid);
            this.parent.mergeablePages.set(this.pageNo, IndirectInstancer.isMergeable(valid));
            this.parent.contentsChanged.set(this.pageNo);
            this.parent.validityChanged.set(this.pageNo);
            this.parent.contentsChanged.clear(other.pageNo);
            this.parent.validityChanged.set(other.pageNo);
            this.parent.mergeablePages.clear(other.pageNo);
            if (IndirectInstancer.isFull(valid)) {
               this.parent.fullPages.set(this.pageNo);
            }
         }
      }
   }
}

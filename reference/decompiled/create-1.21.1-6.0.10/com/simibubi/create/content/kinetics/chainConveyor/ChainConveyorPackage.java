package com.simibubi.create.content.kinetics.chainConveyor;

import com.google.common.cache.Cache;
import com.simibubi.create.foundation.utility.TickBasedCache;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.WorldAttached;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

public class ChainConveyorPackage {
   public static final AtomicInteger netIdGenerator = new AtomicInteger();
   private static final int ticksUntilExpired = 30;
   public static final WorldAttached<Cache<Integer, ChainConveyorPackage.ChainConveyorPackagePhysicsData>> physicsDataCache = new WorldAttached(
      $ -> new TickBasedCache(30, true)
   );
   public float chainPosition;
   public ItemStack item;
   public int netId;
   public boolean justFlipped;
   public Vec3 worldPosition;
   public float yaw;
   private ChainConveyorPackage.ChainConveyorPackagePhysicsData physicsData;

   public ChainConveyorPackage(float chainPosition, ItemStack item) {
      this(chainPosition, item, netIdGenerator.incrementAndGet());
   }

   public ChainConveyorPackage(float chainPosition, ItemStack item, int netId) {
      this.chainPosition = chainPosition;
      this.item = item;
      this.netId = netId;
      this.physicsData = null;
   }

   public CompoundTag writeToClient(Provider registries) {
      CompoundTag tag = this.write(registries);
      tag.putInt("NetID", this.netId);
      return tag;
   }

   public CompoundTag write(Provider registries) {
      CompoundTag compoundTag = new CompoundTag();
      compoundTag.putFloat("Position", this.chainPosition);
      compoundTag.put("Item", this.item.saveOptional(registries));
      return compoundTag;
   }

   public static ChainConveyorPackage read(CompoundTag compoundTag, Provider registries) {
      float pos = compoundTag.getFloat("Position");
      ItemStack item = ItemStack.parseOptional(registries, compoundTag.getCompound("Item"));
      return compoundTag.contains("NetID") ? new ChainConveyorPackage(pos, item, compoundTag.getInt("NetID")) : new ChainConveyorPackage(pos, item);
   }

   public ChainConveyorPackage.ChainConveyorPackagePhysicsData physicsData(LevelAccessor level) {
      if (this.physicsData == null) {
         try {
            return this.physicsData = (ChainConveyorPackage.ChainConveyorPackagePhysicsData)((Cache)physicsDataCache.get(level))
               .get(this.netId, () -> new ChainConveyorPackage.ChainConveyorPackagePhysicsData(this.worldPosition));
         } catch (ExecutionException var3) {
            var3.printStackTrace();
         }
      }

      ((Cache)physicsDataCache.get(level)).getIfPresent(this.netId);
      return this.physicsData;
   }

   public class ChainConveyorPackagePhysicsData {
      public Vec3 targetPos = null;
      public Vec3 prevTargetPos = null;
      public Vec3 prevPos;
      public Vec3 pos = null;
      public Vec3 motion;
      public int lastTick;
      public float yaw;
      public float prevYaw;
      public boolean flipped;
      public ResourceLocation modelKey;
      public WeakReference<ChainConveyorBlockEntity> beReference;

      public ChainConveyorPackagePhysicsData(Vec3 serverPosition) {
         this.prevPos = null;
         this.motion = Vec3.ZERO;
         this.lastTick = AnimationTickHolder.getTicks();
      }

      public boolean shouldTick() {
         if (this.lastTick == AnimationTickHolder.getTicks()) {
            return false;
         } else {
            this.lastTick = AnimationTickHolder.getTicks();
            return true;
         }
      }

      public void setBE(ChainConveyorBlockEntity ccbe) {
         if (this.beReference == null || this.beReference.get() != ccbe) {
            this.beReference = new WeakReference<>(ccbe);
         }
      }
   }
}

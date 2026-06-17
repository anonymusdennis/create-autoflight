package dev.ryanhcode.sable.sublevel.water_occlusion;

import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinterface.water_occlusion.WaterOcclusionContainerHolder;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.util.BoundedBitVolume3i;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public abstract class WaterOcclusionContainer<T extends WaterOcclusionRegion> {
   protected final Set<T> regions = new ObjectOpenHashSet();
   private final Level level;

   public WaterOcclusionContainer(Level level) {
      this.level = level;
   }

   @Nullable
   public static WaterOcclusionContainer<?> getContainer(Level level) {
      return level instanceof WaterOcclusionContainerHolder holder ? holder.sable$getWaterOcclusionContainer() : null;
   }

   public boolean isOccluded(Vec3 location) {
      ActiveSableCompanion helper = Sable.HELPER;

      for (T region : this.regions) {
         BoundedBitVolume3i bitSet = region.getVolume();
         SubLevel subLevel = helper.getContaining(this.level, bitSet.getMinBlockPos());
         Vec3 localLocation = subLevel != null ? subLevel.logicalPose().transformPositionInverse(location) : location;
         if (bitSet.getOccupied(Mth.floor(localLocation.x), Mth.floor(localLocation.y), Mth.floor(localLocation.z))) {
            return true;
         }
      }

      return false;
   }

   @Nullable
   public T getOccludingRegion(Vec3 location) {
      ActiveSableCompanion helper = Sable.HELPER;

      for (T region : this.regions) {
         BoundedBitVolume3i bitSet = region.getVolume();
         SubLevel subLevel = helper.getContaining(this.level, bitSet.getMinBlockPos());
         Vec3 localLocation = subLevel != null ? subLevel.logicalPose().transformPositionInverse(location) : location;
         if (bitSet.getOccupied(Mth.floor(localLocation.x), Mth.floor(localLocation.y), Mth.floor(localLocation.z))) {
            return region;
         }
      }

      return null;
   }

   public void markDirty(BlockPos pos) {
      for (T region : this.regions) {
         BoundedBitVolume3i bitSet = region.getVolume();

         for (Direction direction : Direction.values()) {
            BlockPos rel = pos.relative(direction);
            if (bitSet.getOccupied(rel.getX(), rel.getY(), rel.getZ())) {
               region.markDirty();
               break;
            }
         }
      }
   }

   public abstract void removeRegion(WaterOcclusionRegion var1);

   public abstract WaterOcclusionRegion addRegion(BoundedBitVolume3i var1);

   public Set<T> getRegions() {
      return this.regions;
   }
}

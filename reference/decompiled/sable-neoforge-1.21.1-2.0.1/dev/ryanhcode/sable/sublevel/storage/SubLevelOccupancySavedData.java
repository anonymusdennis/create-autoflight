package dev.ryanhcode.sable.sublevel.storage;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import java.util.BitSet;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

public class SubLevelOccupancySavedData extends SavedData {
   public static final String FILE_ID = "sable_sub_level_occupancy";
   private final ServerLevel level;

   private SubLevelOccupancySavedData(ServerLevel level) {
      this.level = level;
   }

   public static SubLevelOccupancySavedData getOrLoad(ServerLevel level) {
      return (SubLevelOccupancySavedData)level.getChunkSource()
         .getDataStorage()
         .computeIfAbsent(
            new Factory(() -> new SubLevelOccupancySavedData(level), (tag, provider) -> load(level, tag), DataFixTypes.LEVEL), "sable_sub_level_occupancy"
         );
   }

   private static SubLevelOccupancySavedData load(ServerLevel level, CompoundTag tag) {
      SubLevelOccupancySavedData data = new SubLevelOccupancySavedData(level);
      long[] longArray = tag.getLongArray("sub_level_occupancy");
      if (longArray.length > 0) {
         BitSet occupancyData = BitSet.valueOf(longArray);
         SubLevelContainer container = SubLevelContainer.getContainer(level);

         assert container != null : "Sub-level container is null";

         BitSet occupancy = container.getOccupancy();
         occupancy.clear();
         occupancy.or(occupancyData);
      }

      return data;
   }

   public CompoundTag save(CompoundTag compoundTag, Provider provider) {
      SubLevelContainer container = SubLevelContainer.getContainer(this.level);

      assert container != null : "Sub-level container is null";

      BitSet occupancy = container.getOccupancy();
      long[] longArray = occupancy.toLongArray();
      compoundTag.putLongArray("sub_level_occupancy", longArray);
      return compoundTag;
   }
}

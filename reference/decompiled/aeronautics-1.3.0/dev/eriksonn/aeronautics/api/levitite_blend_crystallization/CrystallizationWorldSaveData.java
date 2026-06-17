package dev.eriksonn.aeronautics.api.levitite_blend_crystallization;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

public class CrystallizationWorldSaveData extends SavedData {
   public static final String ID = "aeronautics_levitite_data";
   Level level;

   public CompoundTag save(CompoundTag tag, Provider provider) {
      ListTag list = new ListTag();
      LevititeCrystallizerManager.saveData(list, this.level);
      tag.put("Levitite Manager Data", list);
      return tag;
   }

   public static CrystallizationWorldSaveData load(ServerLevel level, CompoundTag tag, Provider registries) {
      CrystallizationWorldSaveData data = new CrystallizationWorldSaveData();
      data.level = level;
      LevititeCrystallizerManager.loadData(tag, level);
      return data;
   }

   public static CrystallizationWorldSaveData get(ServerLevel level) {
      CrystallizationWorldSaveData data = (CrystallizationWorldSaveData)level.getChunkSource()
         .getDataStorage()
         .computeIfAbsent(new Factory(CrystallizationWorldSaveData::new, (nbt, lookup) -> load(level, nbt, lookup), null), "aeronautics_levitite_data");
      data.level = level;
      return data;
   }
}

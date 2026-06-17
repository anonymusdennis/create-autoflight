package dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.map;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.ServerBalloon;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;
import org.jetbrains.annotations.NotNull;

public class BalloonLevelSavedData extends SavedData {
   public static final String ID = "aeronautics_unloaded_balloons";
   public static Codec<List<SavedBalloon>> CODEC = Codec.list(SavedBalloon.CODEC);
   private Level level;

   private static BalloonLevelSavedData create(ServerLevel level, CompoundTag tag, Provider registries) {
      BalloonLevelSavedData sd = new BalloonLevelSavedData();
      if (tag.contains("aeronautics_unloaded_balloons")) {
         DataResult<Pair<List<SavedBalloon>, Tag>> result = CODEC.decode(NbtOps.INSTANCE, tag.getList("aeronautics_unloaded_balloons", 10));
         BalloonMap map = (BalloonMap)BalloonMap.MAP.get(level);
         result.ifSuccess(x -> map.getUnloadedBalloons().addAll((Collection<? extends SavedBalloon>)x.getFirst()));
      }

      return sd;
   }

   public static BalloonLevelSavedData get(ServerLevel level) {
      BalloonLevelSavedData data = (BalloonLevelSavedData)level.getChunkSource()
         .getDataStorage()
         .computeIfAbsent(new Factory(BalloonLevelSavedData::new, (nbt, lookup) -> create(level, nbt, lookup), null), "aeronautics_unloaded_balloons");
      data.level = level;
      return data;
   }

   @NotNull
   public CompoundTag save(CompoundTag tag, @NotNull Provider provider) {
      BalloonMap map = (BalloonMap)BalloonMap.MAP.get(this.level);
      ObjectArrayList<SavedBalloon> list = new ObjectArrayList(map.getUnloadedBalloons());

      for (Balloon balloon : map.getBalloons()) {
         list.add(BalloonMap.saveBalloon((ServerBalloon)balloon));
      }

      DataResult<Tag> result = CODEC.encodeStart(NbtOps.INSTANCE, list);
      result.ifSuccess(data -> tag.put("aeronautics_unloaded_balloons", data));
      return tag;
   }
}

package dev.eriksonn.aeronautics.api.levitite_blend_crystallization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class LevititeCrystallizerManager {
   private static final Map<LevelAccessor, List<LevititeBlendTicker>> tickers = new HashMap<>();
   private static final List<LevititeBlendTicker> queuedTickers = new ArrayList<>();

   public static void tick(Level level) {
      if (tickers.containsKey(level)) {
         tickers.get(level).removeIf(LevititeBlendTicker::tick);
      }

      addQueued(level);
   }

   private static void addQueued(Level level) {
      CrystallizationWorldSaveData data = CrystallizationWorldSaveData.get((ServerLevel)level);
      Set<BlockPos> tickedPositions = getTickedPositions(level);
      List<LevititeBlendTicker> levelTickers = tickers.get(level);

      for (LevititeBlendTicker queuedTicker : queuedTickers) {
         if (!tickedPositions.contains(queuedTicker.getPos())) {
            levelTickers.add(queuedTicker);
            queuedTicker.getContext().onCrystallizationInitialize(level, queuedTicker.getPos(), queuedTicker.isDormant);
            data.setDirty();
         }
      }

      queuedTickers.clear();
   }

   public static void addTicker(Level level, BlockPos pos, int delay, boolean requiresCatalyst, boolean skipDormant, CrystalPropagationContext context) {
      queuedTickers.add(new LevititeBlendTicker(delay, pos, level, requiresCatalyst, skipDormant, context));
   }

   public static Set<BlockPos> getTickedPositions(Level level) {
      Set<BlockPos> tickedPositions = new HashSet<>();
      tickers.putIfAbsent(level, new ArrayList<>());
      tickers.get(level).forEach(t -> tickedPositions.add(t.getPos()));
      return tickedPositions;
   }

   public static void saveData(ListTag list, Level level) {
      if (tickers.containsKey(level)) {
         for (LevititeBlendTicker ticker : tickers.get(level)) {
            list.add(ticker.serialize());
         }
      }
   }

   public static void loadData(CompoundTag tag, Level level) {
      tickers.putIfAbsent(level, new ArrayList<>());
      ListTag data = tag.getList("Levitite Manager Data", 10);
      List<LevititeBlendTicker> newTickers = new ArrayList<>();

      for (int i = 0; i < data.size(); i++) {
         newTickers.add(new LevititeBlendTicker(data.getCompound(i), level));
      }

      tickers.put(level, newTickers);
   }

   public static void clearLevel(LevelAccessor level) {
      tickers.remove(level);
   }
}

package com.simibubi.create.content.kinetics.drill;

import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.mixin.accessor.FluidInteractionRegistryAccessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry.FluidInteraction;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry.HasFluidInteraction;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry.InteractionInformation;
import org.jetbrains.annotations.Nullable;

public class CobbleGenOptimisation {
   static CobbleGenLevel cachedLevel;

   @Nullable
   public static CobbleGenOptimisation.CobbleGenBlockConfiguration getConfig(LevelAccessor level, BlockPos drillPos, Direction drillDirection) {
      List<BlockState> list = new ArrayList<>();

      for (Direction side : Iterate.directions) {
         BlockPos relative = drillPos.relative(drillDirection).relative(side);
         if (level instanceof Level l && !l.isLoaded(relative)) {
            return null;
         }

         list.add(level.getBlockState(relative));
      }

      return new CobbleGenOptimisation.CobbleGenBlockConfiguration(list);
   }

   public static BlockState determineOutput(ServerLevel level, BlockPos pos, CobbleGenOptimisation.CobbleGenBlockConfiguration config) {
      Map<FluidType, List<InteractionInformation>> interactions = FluidInteractionRegistryAccessor.getInteractions();
      Map<FluidType, Pair<Direction, FluidState>> presentFluidTypes = new HashMap<>();

      for (int i = 0; i < Iterate.directions.length && config.statesAroundDrill.size() > i; i++) {
         FluidState fluidState = config.statesAroundDrill.get(i).getFluidState();
         FluidType fluidType = fluidState.getFluidType();
         if (!fluidType.isAir() && interactions.get(fluidType) != null) {
            presentFluidTypes.put(fluidType, Pair.of(Iterate.directions[i], fluidState));
         }
      }

      FluidInteraction interaction = null;
      Pair<Direction, FluidState> affected = null;

      label64:
      for (Entry<FluidType, Pair<Direction, FluidState>> type : presentFluidTypes.entrySet()) {
         List<InteractionInformation> list = interactions.get(type.getKey());
         FluidState state = FluidHelper.convertToFlowing(((FluidState)type.getValue().getSecond()).getType()).defaultFluidState();
         if (list != null) {
            for (Direction d : Iterate.horizontalDirections) {
               for (InteractionInformation information : list) {
                  if (d != type.getValue().getFirst()) {
                     BlockPos relative = pos.relative(d);
                     HasFluidInteraction predicate = information.predicate();
                     if (predicate.test(level, pos, relative, state)) {
                        interaction = information.interaction();
                        affected = Pair.of(d, state);
                        break label64;
                     }
                  }
               }
            }
         }
      }

      ServerLevel owLevel = level.getServer().getLevel(Level.OVERWORLD);
      if (owLevel == null) {
         owLevel = level;
      }

      if (cachedLevel == null || cachedLevel.getLevel() != owLevel) {
         cachedLevel = new CobbleGenLevel(level);
      }

      BlockState result = Blocks.AIR.defaultBlockState();
      if (interaction == null) {
         return result;
      } else {
         interaction.interact(cachedLevel, pos, pos.relative((Direction)affected.getFirst()), (FluidState)affected.getSecond());
         BlockState output = cachedLevel.blocksAdded.getOrDefault(pos, result);
         cachedLevel.clear();
         return output;
      }
   }

   public static void invalidateWorld(LevelAccessor world) {
      if (cachedLevel != null && cachedLevel.getLevel() == world) {
         cachedLevel = null;
      }
   }

   public static record CobbleGenBlockConfiguration(List<BlockState> statesAroundDrill) {
   }
}

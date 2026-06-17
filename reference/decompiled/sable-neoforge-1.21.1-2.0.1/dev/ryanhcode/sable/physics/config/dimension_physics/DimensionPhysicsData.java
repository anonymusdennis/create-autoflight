package dev.ryanhcode.sable.physics.config.dimension_physics;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3fc;

public class DimensionPhysicsData {
   static final Map<ResourceKey<Level>, DimensionPhysics> DIMENSION_PHYSICS_DATA = new HashMap<>();
   static final Map<ResourceKey<Level>, DimensionPhysics> DEFAULT_DIMENSION_PHYSICS_DATA = new HashMap<>();

   public static DimensionPhysics of(Level level) {
      DimensionPhysics dimensionPhysics = DIMENSION_PHYSICS_DATA.get(level.dimension());
      return dimensionPhysics == null ? getDefault(level) : dimensionPhysics;
   }

   public static DimensionPhysics getDefault(Level level) {
      DimensionPhysics dimensionPhysics = DEFAULT_DIMENSION_PHYSICS_DATA.get(level.dimension());
      if (dimensionPhysics == null) {
         dimensionPhysics = DimensionPhysics.createDefault(level);
         DEFAULT_DIMENSION_PHYSICS_DATA.put(level.dimension(), dimensionPhysics);
      }

      return dimensionPhysics;
   }

   public static Vector3d getGravity(Level level) {
      return getGravity(level, JOMLConversion.ZERO);
   }

   public static Vector3d getGravity(Level level, Vector3dc pos) {
      return getGravity(level, pos, new Vector3d());
   }

   public static Vector3d getGravity(Level level, Vector3dc pos, Vector3d dest) {
      DimensionPhysics physics = of(level);
      DimensionPhysics defaultPhysics = getDefault(level);
      Vector3fc gravity = (Vector3fc)physics.baseGravity().orElseGet(defaultPhysics.baseGravity()::orElseThrow);
      return dest.set(gravity);
   }

   public static double getAirPressure(Level level, Vector3dc pos) {
      DimensionPhysics physics = of(level);
      DimensionPhysics defaultPhysics = getDefault(level);
      double pressure = physics.basePressure().orElseGet(defaultPhysics.basePressure()::orElseThrow);
      BezierResourceFunction curve = physics.pressureFunction().orElseGet(defaultPhysics.pressureFunction()::orElseThrow);
      return pressure * curve.evaluateFunction(pos.y());
   }

   public static Vector3fc getMagneticNorth(Level level) {
      DimensionPhysics physics = of(level);
      DimensionPhysics defaultPhysics = getDefault(level);
      return (Vector3fc)physics.magneticNorth().orElseGet(defaultPhysics.magneticNorth()::orElseThrow);
   }

   public static double getUniversalDrag(ServerLevel level) {
      DimensionPhysics physics = of(level);
      DimensionPhysics defaultPhysics = getDefault(level);
      return (double)physics.universalDrag().orElseGet(defaultPhysics.universalDrag()::orElseThrow).floatValue();
   }

   public static class ReloadListener extends SimpleJsonResourceReloadListener {
      private static final Gson GSON = new Gson();
      public static final DimensionPhysicsData.ReloadListener INSTANCE = new DimensionPhysicsData.ReloadListener();
      public static final String NAME = "dimension_physics";
      public static final ResourceLocation ID = Sable.sablePath("dimension_physics");

      public ReloadListener() {
         super(GSON, "dimension_physics");
      }

      public static void addKeyWithPriority(Map<ResourceKey<Level>, DimensionPhysics> data, ResourceKey<Level> key, DimensionPhysics newProperties) {
         DimensionPhysics existing = data.get(key);
         if (existing != null) {
            if (newProperties.priority() > existing.priority()) {
               data.put(key, newProperties);
            }
         } else {
            data.put(key, newProperties);
         }
      }

      protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
         DimensionPhysicsData.DIMENSION_PHYSICS_DATA.clear();

         for (Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            try {
               DataResult<DimensionPhysics> dataResult = DimensionPhysics.CODEC.parse(JsonOps.INSTANCE, entry.getValue());
               if (dataResult.error().isPresent()) {
                  Sable.LOGGER.error(String.valueOf(dataResult.error().get()));
               }

               DimensionPhysics dimensionPhysics = (DimensionPhysics)dataResult.getOrThrow();
               ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionPhysics.dimension());
               addKeyWithPriority(DimensionPhysicsData.DIMENSION_PHYSICS_DATA, dimension, dimensionPhysics);
            } catch (Exception var9) {
               Sable.LOGGER.error("Error while loading dimension data \"{}\" : {} ", entry.getKey(), var9.getMessage());
            }
         }
      }
   }
}

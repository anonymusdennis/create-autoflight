package dev.simulated_team.simulated.content.end_sea;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.network.packets.end_sea.ClientboundEndSeaPacket;
import foundry.veil.api.network.VeilPacketManager.PacketSink;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class EndSeaPhysicsData {
   private static final HashMap<ResourceKey<Level>, EndSeaPhysics> END_SEA_PHYSICS_DATA = new HashMap<>();

   @Nullable
   public static EndSeaPhysics of(Level level) {
      return END_SEA_PHYSICS_DATA.get(level.dimension());
   }

   public static void physicsTick(double substepTimeStep, ServerLevel level) {
      EndSeaPhysics physics = of(level);
      if (physics != null) {
         physics.physicsTick(substepTimeStep, level);
      }
   }

   public static void addKeyWithPriority(ResourceKey<Level> dimension, EndSeaPhysics newPhysics) {
      EndSeaPhysics existing = END_SEA_PHYSICS_DATA.get(dimension);
      if (existing != null) {
         if (existing.priority().isEmpty()) {
            END_SEA_PHYSICS_DATA.put(dimension, newPhysics);
         } else if (!newPhysics.priority().isEmpty() && newPhysics.priority().get() > existing.priority().get()) {
            END_SEA_PHYSICS_DATA.put(dimension, newPhysics);
         }
      } else {
         END_SEA_PHYSICS_DATA.put(dimension, newPhysics);
      }
   }

   public static void syncDataPacket(PacketSink sink) {
      sink.sendPacket(new CustomPacketPayload[]{new ClientboundEndSeaPacket(END_SEA_PHYSICS_DATA.entrySet().stream().map(Entry::getValue).toList())});
   }

   public static void handleDataPacket(ClientboundEndSeaPacket packet) {
      END_SEA_PHYSICS_DATA.clear();

      for (EndSeaPhysics physics : packet.physics()) {
         addKeyWithPriority(ResourceKey.create(Registries.DIMENSION, physics.dimension()), physics);
      }
   }

   public static class ReloadListener extends SimpleJsonResourceReloadListener {
      private static final Gson GSON = new Gson();
      public static final EndSeaPhysicsData.ReloadListener INSTANCE = new EndSeaPhysicsData.ReloadListener();
      public static final String NAME = "end_sea";
      public static final ResourceLocation ID = Simulated.path("end_sea");

      public ReloadListener() {
         super(GSON, "end_sea");
      }

      protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
         EndSeaPhysicsData.END_SEA_PHYSICS_DATA.clear();

         for (Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            try {
               DataResult<EndSeaPhysics> dataResult = EndSeaPhysics.CODEC.parse(JsonOps.INSTANCE, entry.getValue());
               if (dataResult.isError()) {
                  Simulated.LOGGER.error(String.valueOf(dataResult.error().get()));
               }

               EndSeaPhysics physics = (EndSeaPhysics)dataResult.getOrThrow();
               ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, physics.dimension());
               EndSeaPhysicsData.addKeyWithPriority(dimension, physics);
            } catch (Exception var9) {
               Simulated.LOGGER.error("Error while parsing EndSeaPhysics \"{}\" : {}", entry.getKey(), var9.getMessage());
            }
         }
      }

      public String getName() {
         return "end_sea";
      }
   }
}

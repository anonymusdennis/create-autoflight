package dev.simulated_team.simulated.content.blocks.rope.strand.client;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.UUID;
import net.createmod.catnip.data.WorldAttached;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;

public class ClientLevelRopeManager {
   private static final WorldAttached<ClientLevelRopeManager> worldAttached = new WorldAttached(ClientLevelRopeManager::create);
   private final Level level;
   private final Map<UUID, ClientRopeStrand> ropeStrands = new Object2ObjectOpenHashMap();

   public ClientLevelRopeManager(Level level) {
      this.level = level;
   }

   public static ClientLevelRopeManager getOrCreate(Level level) {
      return (ClientLevelRopeManager)worldAttached.get(level);
   }

   private static ClientLevelRopeManager create(LevelAccessor level) {
      return level instanceof ClientLevel clientLevel ? new ClientLevelRopeManager(clientLevel) : null;
   }

   public void addStrand(ClientRopeStrand strand) {
      this.ropeStrands.put(strand.getUuid(), strand);
   }

   @Nullable
   public ClientRopeStrand getStrand(UUID uuid) {
      return this.ropeStrands.get(uuid);
   }

   public void removeStrand(UUID uuid) {
      this.ropeStrands.remove(uuid);
   }

   public Iterable<ClientRopeStrand> getAllStrands() {
      return this.ropeStrands.values();
   }

   public void tickInterpolation(double interpolationTick) {
      for (ClientRopeStrand strand : this.ropeStrands.values()) {
         strand.tickInterpolation(interpolationTick);
      }
   }
}

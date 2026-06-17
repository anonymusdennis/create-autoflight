package dev.ryanhcode.sable.api.sublevel;

import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.network.client.ClientSableInterpolationState;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.BitSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus.Internal;

public class ClientSubLevelContainer extends SubLevelContainer {
   private final ClientSableInterpolationState interpolation = new ClientSableInterpolationState();
   private final BitSet lightingSceneIds = new BitSet(this.subLevels.length);

   public ClientSubLevelContainer(Level level, int logSideLength, int logPlotSize, int originX, int originZ) {
      super(level, logSideLength, logPlotSize, originX, originZ);
   }

   @Override
   protected SubLevel createSubLevel(int globalPlotX, int globalPlotZ, Pose3d pose, UUID uuid) {
      ClientSubLevel subLevel = new ClientSubLevel(this.getLevel(), globalPlotX, globalPlotZ, pose);
      subLevel.setUniqueId(uuid);
      return subLevel;
   }

   @Override
   public void tick() {
      this.interpolation.tick();
      super.tick();
   }

   @Internal
   public void addDebugInfo(Consumer<String> consumer) {
      consumer.accept("Sub-Levels: " + this.getAllSubLevels().size());
      this.interpolation.addDebugInfo(consumer);
   }

   @Override
   public List<ClientSubLevel> getAllSubLevels() {
      return super.getAllSubLevels();
   }

   public ClientLevel getLevel() {
      return (ClientLevel)super.getLevel();
   }

   public ClientSableInterpolationState getInterpolation() {
      return this.interpolation;
   }

   public int getLightingSceneId(ClientSubLevel subLevel) {
      synchronized (this.lightingSceneIds) {
         if (subLevel.getLightingSceneId() >= 0) {
            return subLevel.getLightingSceneId();
         } else {
            for (int i = 0; i < this.lightingSceneIds.size(); i++) {
               if (!this.lightingSceneIds.get(i)) {
                  this.lightingSceneIds.set(i);
                  subLevel.setLightingSceneId(i + 1);
                  return subLevel.getLightingSceneId();
               }
            }

            throw new IllegalStateException("Out of lighting scene ids, uh oh!");
         }
      }
   }

   @Internal
   public void freeLightingScene(int lightingSceneId) {
      this.lightingSceneIds.clear(lightingSceneId - 1);
   }
}

package dev.ryanhcode.sable.sublevel.water_occlusion;

import dev.ryanhcode.sable.util.BoundedBitVolume3i;
import net.minecraft.world.level.Level;

public class ServerWaterOcclusionContainer extends WaterOcclusionContainer<WaterOcclusionRegion> {
   public static ServerWaterOcclusionContainer create(Level level) {
      return new ServerWaterOcclusionContainer(level);
   }

   public ServerWaterOcclusionContainer(Level level) {
      super(level);
   }

   @Override
   public void removeRegion(WaterOcclusionRegion region) {
      this.regions.remove(region);
   }

   @Override
   public WaterOcclusionRegion addRegion(BoundedBitVolume3i bitSet) {
      WaterOcclusionRegion region = new WaterOcclusionRegion(bitSet);
      this.regions.add(region);
      return region;
   }
}

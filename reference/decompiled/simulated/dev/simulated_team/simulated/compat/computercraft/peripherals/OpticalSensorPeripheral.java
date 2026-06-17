package dev.simulated_team.simulated.compat.computercraft.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.lasers.optical_sensor.OpticalSensorBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;

public class OpticalSensorPeripheral extends SimPeripheral<OpticalSensorBlockEntity> {
   public OpticalSensorPeripheral(OpticalSensorBlockEntity blockEntity) {
      super(blockEntity);
   }

   public String getType() {
      return "optical_sensor";
   }

   @LuaFunction
   public boolean hasHit() {
      return this.blockEntity.hasHit();
   }

   @LuaFunction
   public float getDistance() {
      return this.blockEntity.getHitBlockDistance();
   }

   @LuaFunction
   public String getBlock() {
      return BuiltInRegistries.BLOCK.getKey(this.blockEntity.getHitBlock()).toString();
   }

   @LuaFunction
   public float getRange() {
      return (float)this.blockEntity.getRange();
   }

   @LuaFunction(
      mainThread = true
   )
   public final void setRange(int blocks) {
      this.blockEntity.setRange(blocks);
   }
}

package dev.simulated_team.simulated.compat.computercraft.peripherals;

import dan200.computercraft.api.peripheral.IPeripheral;
import java.util.List;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3dc;

public abstract class SimPeripheral<T extends BlockEntity> implements IPeripheral {
   protected final T blockEntity;

   public SimPeripheral(T blockEntity) {
      this.blockEntity = blockEntity;
   }

   public boolean equals(IPeripheral iPeripheral) {
      return iPeripheral == this;
   }

   static List<Float> vecList(Vec3 vec3) {
      return List.of((float)vec3.x(), (float)vec3.y(), (float)vec3.z());
   }

   static List<Float> vecList(Vector3dc vec3) {
      return List.of((float)vec3.x(), (float)vec3.y(), (float)vec3.z());
   }
}

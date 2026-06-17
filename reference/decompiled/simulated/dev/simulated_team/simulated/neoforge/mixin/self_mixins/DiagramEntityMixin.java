package dev.simulated_team.simulated.neoforge.mixin.self_mixins;

import dev.simulated_team.simulated.content.entities.diagram.DiagramEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({DiagramEntity.class})
public abstract class DiagramEntityMixin implements IEntityWithComplexSpawn {
   @Shadow
   public abstract void addAdditionalSaveData(CompoundTag var1);

   @Shadow
   public abstract void readAdditionalSaveData(CompoundTag var1);

   public void writeSpawnData(RegistryFriendlyByteBuf registryFriendlyByteBuf) {
      CompoundTag compound = new CompoundTag();
      this.addAdditionalSaveData(compound);
      registryFriendlyByteBuf.writeNbt(compound);
   }

   public void readSpawnData(RegistryFriendlyByteBuf registryFriendlyByteBuf) {
      this.readAdditionalSaveData(registryFriendlyByteBuf.readNbt());
   }
}

package dev.simulated_team.simulated.api.sound;

import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record SimSoundEntry(ResourceLocation id, RegistryObject<SoundEvent> registryObject, SoundSource category) {
   public SoundEvent event() {
      return (SoundEvent)this.registryObject().get();
   }

   public void playFrom(Entity entity) {
      this.playFrom(entity, 1.0F, 1.0F);
   }

   public void playFrom(Entity entity, float volume, float pitch) {
      if (!entity.isSilent()) {
         this.play(entity.level(), null, entity.blockPosition(), volume, pitch);
      }
   }

   public void play(Level world, Player entity, Vec3i pos, float volume, float pitch) {
      this.play(world, entity, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, volume, pitch);
   }

   public void play(Level world, Player entity, Vec3 pos, float volume, float pitch) {
      this.play(world, entity, pos.x(), pos.y(), pos.z(), volume, pitch);
   }

   public void playAt(Level world, Vec3i pos, float volume, float pitch, boolean fade) {
      this.playAt(world, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, volume, pitch, fade);
   }

   public void playAt(Level world, Vec3 pos, float volume, float pitch, boolean fade) {
      this.playAt(world, pos.x(), pos.y(), pos.z(), volume, pitch, fade);
   }

   public void play(Level world, Player entity, double x, double y, double z, float volume, float pitch) {
      world.playSound(entity, x, y, z, this.event(), this.category, volume, pitch);
   }

   public void playAt(Level world, double x, double y, double z, float volume, float pitch, boolean fade) {
      world.playLocalSound(x, y, z, this.event(), this.category, volume, pitch, fade);
   }
}

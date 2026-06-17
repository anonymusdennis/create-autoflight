package dev.ryanhcode.sable.sound;

import com.mojang.blaze3d.audio.Channel;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.mixin.sublevel_sounds.ChannelAccessor;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.lwjgl.openal.AL11;

public class MovingSoundInstanceDelegate implements SoundInstance, TickableSoundInstance {
   private SubLevel subLevel;
   private double latestX;
   private double latestY;
   private double latestZ;
   public SoundInstance instance;

   public MovingSoundInstanceDelegate(SoundInstance instance, SubLevel subLevel) {
      this.instance = instance;
      this.subLevel = subLevel;
      if (this.instance instanceof SoundInstanceDelegated) {
         ((SoundInstanceDelegated)this.instance).setDelegate(this);
      }
   }

   public void tickWithChannel(Channel channel) {
      int source = ((ChannelAccessor)channel).getSource();
      if (this.subLevel != null && this.subLevel.isRemoved()) {
         this.subLevel = null;
      }

      if (this.subLevel == null) {
         AL11.alSource3f(source, 4102, 0.0F, 0.0F, 0.0F);
      } else {
         Vector3d instancePos = new Vector3d(this.instance.getX(), this.instance.getY(), this.instance.getZ());
         Vector3d motion = Sable.HELPER.getVelocity(Minecraft.getInstance().level, instancePos);
         Entity player = Minecraft.getInstance().getCameraEntity();
         if (player == null) {
            AL11.alSource3f(source, 4102, 0.0F, 0.0F, 0.0F);
         } else {
            Vector3d playerPosition = JOMLConversion.toJOML(player.position());
            Vector3d playerMotion = playerPosition.sub(player.xo, player.yo, player.zo).mul(20.0);
            AL11.alSpeedOfSound(1800.0F);
            AL11.alDopplerFactor(0.4F);
            AL11.alSource3f(source, 4102, (float)(motion.x - playerMotion.x), (float)(motion.y - playerMotion.y), (float)(motion.z - playerMotion.z));
         }
      }
   }

   public void unload(Channel channel) {
      AL11.alSource3f(((ChannelAccessor)channel).getSource(), 4102, 0.0F, 0.0F, 0.0F);
   }

   @NotNull
   public ResourceLocation getLocation() {
      return this.instance.getLocation();
   }

   public WeighedSoundEvents resolve(SoundManager pManager) {
      return this.instance.resolve(pManager);
   }

   @NotNull
   public Sound getSound() {
      return this.instance.getSound();
   }

   @NotNull
   public SoundSource getSource() {
      return this.instance.getSource();
   }

   public boolean isLooping() {
      return this.instance.isLooping();
   }

   public boolean isRelative() {
      return this.instance.isRelative();
   }

   public int getDelay() {
      return this.instance.getDelay();
   }

   public float getVolume() {
      return this.instance.getVolume();
   }

   public float getPitch() {
      return this.instance.getPitch();
   }

   public double getX() {
      return this.subLevel == null
         ? this.latestX
         : (this.latestX = this.subLevel.logicalPose().transformPosition(new Vec3(this.instance.getX(), this.instance.getY(), this.instance.getZ())).x);
   }

   public double getY() {
      return this.subLevel == null
         ? this.latestY
         : (this.latestY = this.subLevel.logicalPose().transformPosition(new Vec3(this.instance.getX(), this.instance.getY(), this.instance.getZ())).y);
   }

   public double getZ() {
      return this.subLevel == null
         ? this.latestZ
         : (this.latestZ = this.subLevel.logicalPose().transformPosition(new Vec3(this.instance.getX(), this.instance.getY(), this.instance.getZ())).z);
   }

   public boolean canStartSilent() {
      return this.instance.canStartSilent();
   }

   public boolean canPlaySound() {
      return this.instance.canPlaySound();
   }

   public Attenuation getAttenuation() {
      return this.instance.getAttenuation();
   }

   public boolean isStopped() {
      return this.instance instanceof TickableSoundInstance tickable ? tickable.isStopped() : !this.instance.canPlaySound();
   }

   public void tick() {
      if (this.instance instanceof TickableSoundInstance tickable) {
         tickable.tick();
      }
   }
}

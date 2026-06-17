package dev.eriksonn.aeronautics.content.blocks.hot_air.sound;

import dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.hot_air.steam_vent.SteamVentBlockEntity;
import dev.eriksonn.aeronautics.index.AeroSoundEvents;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class BalloonBurnerSoundInstance extends AbstractTickableSoundInstance {
   public static final BalloonBurnerSoundInstance GLOBAL_HOT_AIR_BURNER_SOUND = new BalloonBurnerSoundInstance(AeroSoundEvents.HOT_AIR_BURNER_HEAT.event());
   public static final BalloonBurnerSoundInstance GLOBAL_STEAM_VENT_AIR_BURNER_SOUND = new BalloonBurnerSoundInstance(AeroSoundEvents.STEAM_VENT_HEAT.event());
   private static final int MAX_DISTANCE = 10;
   private static final float VOLUME_SCALE = 0.325F;
   private final Set<BlockPos> NEARBY_BLOCKS = new HashSet<>();
   private final Vector3d meanPos = new Vector3d();
   private float meanPitch = 0.0F;
   private float meanVolume = 0.0F;

   public BalloonBurnerSoundInstance(SoundEvent sound) {
      super(sound, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
      this.looping = true;
      this.delay = 0;
      this.volume = 0.0F;
      this.pitch = 0.0F;
   }

   public void addPos(BlockPos pos) {
      Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
      if (distSquared(camera, pos) < 100.0 && this.NEARBY_BLOCKS.add(pos.immutable())) {
         this.updateMeanPos();
      }
   }

   public void removePos(BlockPos pos) {
      this.NEARBY_BLOCKS.remove(pos);
      this.updateMeanPos();
   }

   private void updateMeanPos() {
      this.meanPos.zero();
      Vector3d v = new Vector3d();
      if (!this.NEARBY_BLOCKS.isEmpty()) {
         for (BlockPos nearby : this.NEARBY_BLOCKS) {
            v.set((double)nearby.getX() + 0.5, (double)nearby.getY() + 0.5, (double)nearby.getZ() + 0.5);
            ClientSubLevel subLevel = Sable.HELPER.getContainingClient(v);
            if (subLevel != null) {
               subLevel.logicalPose().transformPosition(v);
            }

            this.meanPos.add(v);
         }

         this.meanPos.div((double)this.NEARBY_BLOCKS.size());
      }
   }

   private void updateInformation() {
      if (!this.NEARBY_BLOCKS.isEmpty()) {
         ClientLevel level = Minecraft.getInstance().level;
         Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
         this.meanVolume = 0.001F;
         int volumeChangers = 0;
         Iterator<BlockPos> iter = this.NEARBY_BLOCKS.iterator();

         while (iter.hasNext()) {
            BlockPos next = iter.next();
            if (next != null) {
               if (distSquared(camera, next) > 100.0) {
                  iter.remove();
                  this.updateMeanPos();
               } else {
                  BlockEntity be = level.getBlockEntity(next);
                  float intensityScaling;
                  if (be instanceof HotAirBurnerBlockEntity hbe) {
                     intensityScaling = Mth.clamp(hbe.getClientIntensity().getValue(), 0.0F, 1.0F);
                  } else {
                     if (!(be instanceof SteamVentBlockEntity sbe)) {
                        iter.remove();
                        this.updateMeanPos();
                        continue;
                     }

                     intensityScaling = Mth.clamp(sbe.getClientIntensity().getValue(), 0.0F, 1.0F);
                  }

                  this.meanVolume = this.meanVolume + Math.clamp(intensityScaling * 4.0F, 0.0F, 2.0F);
                  volumeChangers++;
               }
            }
         }

         if (!this.NEARBY_BLOCKS.isEmpty()) {
            this.meanPitch = 1.0F;
            this.meanVolume /= (float)volumeChangers;
            this.meanVolume = this.meanVolume * (float)(1.0 - Math.sqrt(distSquared(camera, this.meanPos)) / 10.0);
         }
      }
   }

   private static double distSquared(Camera camera, Vector3dc pos) {
      ClientLevel level = Minecraft.getInstance().level;
      return Sable.HELPER.distanceSquaredWithSubLevels(level, camera.getPosition(), pos.x(), pos.y(), pos.z());
   }

   private static double distSquared(Camera camera, Vec3i pos) {
      ClientLevel level = Minecraft.getInstance().level;
      return Sable.HELPER
         .distanceSquaredWithSubLevels(level, camera.getPosition(), (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5);
   }

   public void tick() {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         this.updateInformation();
         if (!this.NEARBY_BLOCKS.isEmpty()) {
            this.x = this.meanPos.x;
            this.y = this.meanPos.y;
            this.z = this.meanPos.z;
            this.volume = this.meanVolume * 0.325F;
            this.pitch = this.meanPitch;
         }
      }
   }

   public boolean canStartSilent() {
      return true;
   }

   public boolean canPlaySound() {
      ClientLevel level = Minecraft.getInstance().level;
      return level != null && !this.NEARBY_BLOCKS.isEmpty();
   }

   public boolean isStopped() {
      ClientLevel level = Minecraft.getInstance().level;
      return level == null || this.NEARBY_BLOCKS.isEmpty();
   }
}

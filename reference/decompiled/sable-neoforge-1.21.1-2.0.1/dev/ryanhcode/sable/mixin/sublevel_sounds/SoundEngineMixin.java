package dev.ryanhcode.sable.mixin.sublevel_sounds;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sound.MovingSoundInstanceDelegate;
import dev.ryanhcode.sable.sound.SoundInstanceDelegated;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.Iterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.ChannelAccess.ChannelHandle;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin({SoundEngine.class})
public class SoundEngineMixin {
   @ModifyVariable(
      method = {"play"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private SoundInstance sable$play(SoundInstance instance) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) {
         return instance;
      } else {
         SubLevel subLevel = Sable.HELPER.getContaining(level, instance.getX(), instance.getZ());
         return (SoundInstance)(subLevel != null ? new MovingSoundInstanceDelegate(instance, subLevel) : instance);
      }
   }

   @ModifyVariable(
      method = {"stop(Lnet/minecraft/client/resources/sounds/SoundInstance;)V"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private SoundInstance sable$stop(SoundInstance instance) {
      if (instance instanceof SoundInstanceDelegated delegated && delegated.getDelegate() != null) {
         return delegated.getDelegate();
      }

      return instance;
   }

   @Inject(
      method = {"tickNonPaused"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/sounds/ChannelAccess$ChannelHandle;execute(Ljava/util/function/Consumer;)V",
         shift = Shift.AFTER,
         ordinal = 0
      )},
      locals = LocalCapture.CAPTURE_FAILEXCEPTION
   )
   private void sable$tick(
      CallbackInfo ci, Iterator<TickableSoundInstance> sounds, TickableSoundInstance sound, float volume, float pitch, Vec3 pos, ChannelHandle access
   ) {
      if (sound instanceof MovingSoundInstanceDelegate delegated) {
         access.execute(delegated::tickWithChannel);
      }
   }

   @Inject(
      method = {"stop(Lnet/minecraft/client/resources/sounds/SoundInstance;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/sounds/ChannelAccess$ChannelHandle;execute(Ljava/util/function/Consumer;)V",
         shift = Shift.AFTER
      )},
      locals = LocalCapture.CAPTURE_FAILEXCEPTION
   )
   private void sable$clear(SoundInstance sound, CallbackInfo ci, ChannelHandle access) {
      if (sound instanceof MovingSoundInstanceDelegate delegated) {
         access.execute(delegated::unload);
      }
   }
}

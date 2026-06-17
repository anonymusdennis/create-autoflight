package dev.engine_room.flywheel.backend.engine.uniform;

import net.minecraft.client.Options;

public final class OptionsUniforms extends UniformWriter {
   private static final int SIZE = 56;
   static final UniformBuffer BUFFER = new UniformBuffer(2, 56);

   public static void update(Options options) {
      long ptr = BUFFER.ptr();
      ptr = writeFloat(ptr, ((Double)options.gamma().get()).floatValue());
      ptr = writeInt(ptr, (Integer)options.fov().get());
      ptr = writeFloat(ptr, ((Double)options.screenEffectScale().get()).floatValue());
      ptr = writeFloat(ptr, ((Double)options.glintSpeed().get()).floatValue());
      ptr = writeFloat(ptr, ((Double)options.glintStrength().get()).floatValue());
      ptr = writeInt(ptr, (Integer)options.biomeBlendRadius().get());
      ptr = writeInt(ptr, options.ambientOcclusion().get() ? 1 : 0);
      ptr = writeInt(ptr, options.bobView().get() ? 1 : 0);
      ptr = writeInt(ptr, options.highContrast().get() ? 1 : 0);
      ptr = writeFloat(ptr, ((Double)options.textBackgroundOpacity().get()).floatValue());
      ptr = writeInt(ptr, options.backgroundForChatOnly().get() ? 1 : 0);
      ptr = writeFloat(ptr, ((Double)options.darknessEffectScale().get()).floatValue());
      ptr = writeFloat(ptr, ((Double)options.damageTiltStrength().get()).floatValue());
      ptr = writeInt(ptr, options.hideLightningFlash().get() ? 1 : 0);
      BUFFER.markDirty();
   }
}

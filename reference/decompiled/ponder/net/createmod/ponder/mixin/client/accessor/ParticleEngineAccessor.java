package net.createmod.ponder.mixin.client.accessor;

import java.util.Map;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ParticleEngine.class})
public interface ParticleEngineAccessor {
   @Accessor("providers")
   Map<ResourceLocation, ParticleProvider<?>> ponder$getProviders();
}

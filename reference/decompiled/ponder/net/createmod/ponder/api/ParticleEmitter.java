package net.createmod.ponder.api;

import net.createmod.ponder.api.level.PonderLevel;

@FunctionalInterface
public interface ParticleEmitter {
   void create(PonderLevel var1, double var2, double var4, double var6);
}

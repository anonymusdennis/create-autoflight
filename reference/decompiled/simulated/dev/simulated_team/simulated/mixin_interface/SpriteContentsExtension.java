package dev.simulated_team.simulated.mixin_interface;

import net.minecraft.client.renderer.texture.SpriteContents.Ticker;

public interface SpriteContentsExtension {
   Ticker simulated$getTicker();

   void simulated$setTicker(Ticker var1);
}

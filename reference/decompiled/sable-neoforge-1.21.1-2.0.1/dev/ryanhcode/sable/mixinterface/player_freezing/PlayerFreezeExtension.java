package dev.ryanhcode.sable.mixinterface.player_freezing;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3dc;

public interface PlayerFreezeExtension {
   @Nullable
   UUID sable$getFrozenToSubLevel();

   @Nullable
   Vector3dc sable$getFrozenToSubLevelAnchor();

   void sable$tickStopFreezing();

   void sable$freezeTo(UUID var1, Vector3dc var2);

   void sable$teleport();
}

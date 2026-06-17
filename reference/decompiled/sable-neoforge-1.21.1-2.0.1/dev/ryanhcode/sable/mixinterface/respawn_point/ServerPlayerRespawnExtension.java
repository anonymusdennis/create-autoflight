package dev.ryanhcode.sable.mixinterface.respawn_point;

import it.unimi.dsi.fastutil.Pair;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public interface ServerPlayerRespawnExtension {
   @Nullable
   UUID sable$getRespawnPoint();

   void sable$takeQueuedFreezeFrom(ServerPlayer var1);

   @Nullable
   Pair<UUID, Vector3d> sable$getQueuedFreeze();
}

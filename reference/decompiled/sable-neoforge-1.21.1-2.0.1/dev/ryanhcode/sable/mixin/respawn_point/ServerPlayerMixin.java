package dev.ryanhcode.sable.mixin.respawn_point;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.mixinterface.player_freezing.PlayerFreezeExtension;
import dev.ryanhcode.sable.mixinterface.respawn_point.ServerPlayerRespawnExtension;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundFreezePlayerPacket;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.tracking_points.SubLevelTrackingPointSavedData;
import it.unimi.dsi.fastutil.Pair;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayer.RespawnPosAngle;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerPlayer.class})
public abstract class ServerPlayerMixin implements ServerPlayerRespawnExtension {
   @Shadow
   @Final
   public MinecraftServer server;
   @Shadow
   public ServerGamePacketListenerImpl connection;
   @Shadow
   @Nullable
   private BlockPos respawnPosition;
   @Shadow
   private ResourceKey<Level> respawnDimension;
   @Shadow
   private float respawnAngle;
   @Shadow
   private boolean respawnForced;
   @Unique
   @Nullable
   private UUID sable$respawnPoint = null;
   @Unique
   private Pair<UUID, Vector3d> sable$queuedFreeze = null;

   @Shadow
   public static Optional<RespawnPosAngle> findRespawnAndUseSpawnBlock(ServerLevel serverLevel, BlockPos blockPos, float f, boolean bl, boolean bl2) {
      return null;
   }

   @Shadow
   public abstract ServerLevel serverLevel();

   @Shadow
   public abstract void sendSystemMessage(Component var1);

   @Nullable
   @Override
   public UUID sable$getRespawnPoint() {
      return this.sable$respawnPoint;
   }

   @Inject(
      method = {"setRespawnPosition"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$setRespawnPosition(ResourceKey<Level> resourceKey, @Nullable BlockPos blockPos, float f, boolean bl, boolean sendMessage, CallbackInfo ci) {
      ServerLevel level = this.serverLevel();
      SubLevelTrackingPointSavedData data = SubLevelTrackingPointSavedData.getOrLoad(level);
      if (this.sable$respawnPoint != null) {
         data.removeTrackingPoint(this.sable$respawnPoint);
         this.sable$respawnPoint = null;
      }

      if (blockPos != null && Sable.HELPER.getContaining(level, blockPos) instanceof ServerSubLevel serverSubLevel) {
         this.sable$respawnPoint = data.generateTrackingPoint(Vec3.atCenterOf(blockPos), serverSubLevel);
         if (this.sable$respawnPoint != null) {
            boolean theSame = blockPos.equals(this.respawnPosition) && resourceKey.equals(this.respawnDimension);
            if (sendMessage && !theSame) {
               this.sendSystemMessage(Component.translatable("block.minecraft.set_spawn"));
            }

            this.respawnPosition = blockPos;
            this.respawnDimension = resourceKey;
            this.respawnAngle = f;
            this.respawnForced = bl;
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"addAdditionalSaveData"},
      at = {@At("TAIL")}
   )
   private void sable$addRespawnPoint(CompoundTag compoundTag, CallbackInfo ci) {
      if (this.sable$respawnPoint != null) {
         compoundTag.putUUID("RespawnPoint", this.sable$respawnPoint);
      }
   }

   @Inject(
      method = {"readAdditionalSaveData"},
      at = {@At("TAIL")}
   )
   private void sable$readRespawnPoint(CompoundTag compoundTag, CallbackInfo ci) {
      if (compoundTag.hasUUID("RespawnPoint")) {
         this.sable$respawnPoint = compoundTag.getUUID("RespawnPoint");
      }
   }

   @Overwrite
   public void copyRespawnPosition(ServerPlayer serverPlayer) {
      if (serverPlayer.getRespawnPosition() != null) {
         this.sable$respawnPoint = ((ServerPlayerRespawnExtension)serverPlayer).sable$getRespawnPoint();
         this.respawnPosition = serverPlayer.getRespawnPosition();
         this.respawnDimension = serverPlayer.getRespawnDimension();
         this.respawnAngle = serverPlayer.getRespawnAngle();
         this.respawnForced = serverPlayer.isRespawnForced();
      } else {
         this.sable$respawnPoint = null;
         this.respawnPosition = null;
         this.respawnDimension = Level.OVERWORLD;
         this.respawnAngle = 0.0F;
         this.respawnForced = false;
      }
   }

   @Override
   public void sable$takeQueuedFreezeFrom(ServerPlayer oldPlayer) {
      ServerPlayerRespawnExtension extension = (ServerPlayerRespawnExtension)oldPlayer;
      Pair<UUID, Vector3d> queuedFreeze = extension.sable$getQueuedFreeze();
      if (queuedFreeze != null) {
         ((PlayerFreezeExtension)this).sable$freezeTo((UUID)queuedFreeze.first(), (Vector3dc)queuedFreeze.second());
         this.connection
            .send(new ClientboundCustomPayloadPacket(new ClientboundFreezePlayerPacket((UUID)queuedFreeze.first(), (Vector3dc)queuedFreeze.second())));
      }
   }

   @Nullable
   @Override
   public Pair<UUID, Vector3d> sable$getQueuedFreeze() {
      return this.sable$queuedFreeze;
   }

   @Redirect(
      method = {"findRespawnPositionAndUseSpawnBlock"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/server/level/ServerPlayer;findRespawnAndUseSpawnBlock(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;FZZ)Ljava/util/Optional;"
      )
   )
   private Optional<RespawnPosAngle> sable$findRespawnPosition(ServerLevel level, BlockPos blockPos, float f1, boolean b1, boolean b2) {
      SubLevelTrackingPointSavedData data = SubLevelTrackingPointSavedData.getOrLoad(level);
      if (this.sable$respawnPoint != null) {
         SubLevelTrackingPointSavedData.TakenLoginPoint point = data.take(this.sable$respawnPoint, false);
         if (point == null) {
            this.sable$respawnPoint = null;
            return Optional.empty();
         } else {
            if (point.subLevelId() != null) {
               this.sable$queuedFreeze = Pair.of(point.subLevelId(), point.localAnchor());
            }

            return Optional.of(new RespawnPosAngle(JOMLConversion.toMojang(point.position()), f1));
         }
      } else {
         return findRespawnAndUseSpawnBlock(level, blockPos, f1, b1, b2);
      }
   }
}

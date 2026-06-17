package com.simibubi.create.content.equipment.bell;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.simibubi.create.AllBlocks;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import net.createmod.catnip.data.IntAttached;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;

@EventBusSubscriber
public class HauntedBellPulser {
   public static final int DISTANCE = 3;
   public static final int RECHARGE_TICKS = 8;
   public static final int WARMUP_TICKS = 10;
   public static final Cache<UUID, IntAttached<Entity>> WARMUP = CacheBuilder.newBuilder().expireAfterAccess(250L, TimeUnit.MILLISECONDS).build();

   @SubscribeEvent
   public static void hauntedBellCreatesPulse(Post event) {
      Player player = event.getEntity();
      if (!player.level().isClientSide()) {
         if (!player.isSpectator()) {
            if (player.isHolding(AllBlocks.HAUNTED_BELL::isIn)) {
               boolean firstPulse = false;

               try {
                  IntAttached<Entity> ticker = (IntAttached<Entity>)WARMUP.get(player.getUUID(), () -> IntAttached.with(10, player));
                  firstPulse = (Integer)ticker.getFirst() == 1;
                  ticker.decrement();
                  if (!ticker.isOrBelowZero()) {
                     return;
                  }
               } catch (ExecutionException var7) {
               }

               long gameTime = player.level().getGameTime();
               if ((firstPulse || gameTime % 8L != 0L) && player.level() instanceof ServerLevel serverLevel) {
                  sendPulse(serverLevel, player.blockPosition(), 3, false);
               }
            }
         }
      }
   }

   public static void sendPulse(ServerLevel world, BlockPos pos, int distance, boolean canOverlap) {
      ChunkPos chunk = world.getChunkAt(pos).getPos();
      CatnipServices.NETWORK.sendToClientsTrackingChunk(world, chunk, new SoulPulseEffectPacket(pos, distance, canOverlap));
   }
}

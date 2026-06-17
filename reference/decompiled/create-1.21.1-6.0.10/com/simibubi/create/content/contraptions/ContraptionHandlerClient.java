package com.simibubi.create.content.contraptions;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.sync.ContraptionInteractionPacket;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.TrainRelocator;
import com.simibubi.create.foundation.utility.RaycastHelper;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Map;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber({Dist.CLIENT})
public class ContraptionHandlerClient {
   @SubscribeEvent
   @OnlyIn(Dist.CLIENT)
   public static void preventRemotePlayersWalkingAnimations(Post event) {
      if (event.getEntity() instanceof RemotePlayer remotePlayer) {
         CompoundTag data = remotePlayer.getPersistentData();
         if (data.contains("LastOverrideLimbSwingUpdate")) {
            int lastOverride = data.getInt("LastOverrideLimbSwingUpdate");
            data.putInt("LastOverrideLimbSwingUpdate", lastOverride + 1);
            if (lastOverride > 5) {
               data.remove("LastOverrideLimbSwingUpdate");
               data.remove("OverrideLimbSwing");
            } else {
               float limbSwing = data.getFloat("OverrideLimbSwing");
               remotePlayer.xo = remotePlayer.getX() - (double)(limbSwing / 4.0F);
               remotePlayer.zo = remotePlayer.getZ();
            }
         }
      }
   }

   @SubscribeEvent
   @OnlyIn(Dist.CLIENT)
   public static void rightClickingOnContraptionsGetsHandledLocally(InteractionKeyMappingTriggered event) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null) {
         if (!player.isSpectator()) {
            if (mc.level != null) {
               if (event.isUseItem()) {
                  Couple<Vec3> rayInputs = getRayInputs(player);
                  Vec3 origin = (Vec3)rayInputs.getFirst();
                  Vec3 target = (Vec3)rayInputs.getSecond();
                  AABB aabb = new AABB(origin, target).inflate(16.0);
                  Collection<WeakReference<AbstractContraptionEntity>> contraptions = ((Map)ContraptionHandler.loadedContraptions.get(mc.level)).values();
                  double bestDistance = Double.MAX_VALUE;
                  BlockHitResult bestResult = null;
                  AbstractContraptionEntity bestEntity = null;

                  for (WeakReference<AbstractContraptionEntity> ref : contraptions) {
                     AbstractContraptionEntity contraptionEntity = ref.get();
                     if (contraptionEntity != null && contraptionEntity.getBoundingBox().intersects(aabb)) {
                        BlockHitResult rayTraceResult = rayTraceContraption(origin, target, contraptionEntity);
                        if (rayTraceResult != null) {
                           double distance = contraptionEntity.toGlobalVector(rayTraceResult.getLocation(), 1.0F).distanceTo(origin);
                           if (!(distance > bestDistance)) {
                              bestResult = rayTraceResult;
                              bestDistance = distance;
                              bestEntity = contraptionEntity;
                           }
                        }
                     }
                  }

                  if (bestResult != null) {
                     InteractionHand hand = event.getHand();
                     Direction face = bestResult.getDirection();
                     BlockPos pos = bestResult.getBlockPos();
                     if (bestEntity.handlePlayerInteraction(player, pos, face, hand)) {
                        CatnipServices.NETWORK.sendToServer(new ContraptionInteractionPacket(bestEntity, hand, pos, face));
                     } else {
                        handleSpecialInteractions(bestEntity, player, pos, face, hand);
                     }

                     event.setCanceled(true);
                     event.setSwingHand(false);
                  }
               }
            }
         }
      }
   }

   private static boolean handleSpecialInteractions(
      AbstractContraptionEntity contraptionEntity, Player player, BlockPos localPos, Direction side, InteractionHand interactionHand
   ) {
      return AllItems.WRENCH.isIn(player.getItemInHand(interactionHand)) && contraptionEntity instanceof CarriageContraptionEntity car
         ? TrainRelocator.carriageWrenched(car.toGlobalVector(VecHelper.getCenterOf(localPos), 1.0F), car)
         : false;
   }

   @OnlyIn(Dist.CLIENT)
   public static Couple<Vec3> getRayInputs(LocalPlayer player) {
      Minecraft mc = Minecraft.getInstance();
      Vec3 origin = player.getEyePosition();
      double reach = player.blockInteractionRange();
      if (mc.hitResult != null && mc.hitResult.getLocation() != null) {
         reach = Math.min(mc.hitResult.getLocation().distanceTo(origin), reach);
      }

      Vec3 target = RaycastHelper.getTraceTarget(player, reach, origin);
      return Couple.create(origin, target);
   }

   @Nullable
   public static BlockHitResult rayTraceContraption(Vec3 origin, Vec3 target, AbstractContraptionEntity contraptionEntity) {
      Vec3 localOrigin = contraptionEntity.toLocalVector(origin, 1.0F);
      Vec3 localTarget = contraptionEntity.toLocalVector(target, 1.0F);
      Contraption contraption = contraptionEntity.getContraption();
      MutableObject<BlockHitResult> mutableResult = new MutableObject();
      RaycastHelper.PredicateTraceResult predicateResult = RaycastHelper.rayTraceUntil(localOrigin, localTarget, p -> {
         for (Direction d : Iterate.directions) {
            if (d != Direction.UP) {
               BlockPos pos = d == Direction.DOWN ? p : p.relative(d);
               StructureBlockInfo blockInfo = contraption.getBlocks().get(pos);
               if (blockInfo != null) {
                  BlockState state = blockInfo.state();
                  VoxelShape raytraceShape = state.getShape(contraption.getContraptionWorld(), BlockPos.ZERO.below());
                  if (!raytraceShape.isEmpty() && !contraption.isHiddenInPortal(pos)) {
                     BlockHitResult rayTrace = raytraceShape.clip(localOrigin, localTarget, pos);
                     if (rayTrace != null) {
                        mutableResult.setValue(rayTrace);
                        return true;
                     }
                  }
               }
            }
         }

         return false;
      });
      return predicateResult != null && !predicateResult.missed() ? (BlockHitResult)mutableResult.getValue() : null;
   }
}

package dev.simulated_team.simulated.content.blocks.rope.strand.client;

import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.foundation.utility.RaycastHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimTags;
import dev.simulated_team.simulated.network.packets.RopeBreakPacket;
import dev.simulated_team.simulated.network.packets.RopeRidingPacket;
import dev.simulated_team.simulated.service.SimConfigService;
import dev.simulated_team.simulated.util.SimColors;
import dev.simulated_team.simulated.util.SimMathUtils;
import dev.simulated_team.simulated.util.click_interactions.InteractCallback;
import foundry.veil.api.network.VeilPacketManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Optional;
import java.util.UUID;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class ZiplineClientManager implements InteractCallback {
   private static final double CONTINUOUS_STEP_SIZE = 0.25;
   private static final double HALF_THICKNESS = 0.25;
   public static UUID ridingRope = null;
   public static UUID hoveringRope = null;
   private static int groundedTimer = 0;

   public static void tick() {
      if (ridingRope != null) {
         ridingTick();
      } else {
         groundedTimer = 0;
      }

      Minecraft mc = Minecraft.getInstance();
      if (!isRopeInteractable(mc.player.getMainHandItem())) {
         hoveringRope = null;
      } else {
         double maxRange = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 1.0;
         HitResult hitResult = mc.hitResult;
         ClientLevelRopeManager ropeManager = ClientLevelRopeManager.getOrCreate(mc.level);
         Vector3d from = JOMLConversion.toJOML(mc.player.getEyePosition());
         Vector3d to = JOMLConversion.toJOML(RaycastHelper.getTraceTarget(mc.player, maxRange, JOMLConversion.toMojang(from)));
         double bestDiffSqr = hitResult == null
            ? Float.MAX_VALUE
            : Sable.HELPER.projectOutOfSubLevel(mc.level, hitResult.getLocation()).distanceToSqr(from.x, from.y, from.z);
         hoveringRope = raycastRope(ropeManager, from, to, bestDiffSqr, 0.25);
         if (ridingRope == null && hoveringRope == null && mc.options.keyUse.isDown() && !mc.player.isShiftKeyDown()) {
            holdUseSearch(ropeManager, mc.player, from, to, bestDiffSqr);
         }
      }
   }

   @Nullable
   public static UUID raycastRope(ClientLevelRopeManager ropeManager, Vector3dc from, Vector3dc to, double bestDiffSqr, double halfThickness) {
      Vector3d localFrom = new Vector3d();
      Vector3d localTo = new Vector3d();
      Vector3d normal = new Vector3d();
      UUID bestHovering = null;

      for (ClientRopeStrand strand : ropeManager.getAllStrands()) {
         ObjectArrayList<ClientRopePoint> points = strand.getPoints();

         for (int i = 0; i < points.size() - 1; i++) {
            ClientRopePoint point0 = (ClientRopePoint)points.get(i);
            ClientRopePoint point1 = (ClientRopePoint)points.get(i + 1);
            point1.position().sub(point0.position(), normal).normalize();
            AABB bounds = new AABB(-halfThickness, 0.0, -halfThickness, halfThickness, point0.position().distance(point1.position()), halfThickness);
            Quaternionf rot = SimMathUtils.getQuaternionfFromVectorRotation(OrientedBoundingBox3d.UP, normal);
            rot.transformInverse(localFrom.set(from).sub(point0.position()));
            rot.transformInverse(localTo.set(to).sub(point0.position()));
            Optional<Vec3> clip = bounds.clip(JOMLConversion.toMojang(localFrom), JOMLConversion.toMojang(localTo));
            if (!clip.isEmpty()) {
               double distanceToSqr = clip.get().distanceToSqr(localFrom.x, localFrom.y, localFrom.z);
               if (!(distanceToSqr > bestDiffSqr)) {
                  bestDiffSqr = distanceToSqr;
                  bestHovering = strand.getUuid();
               }
            }
         }
      }

      return bestHovering;
   }

   private static void holdUseSearch(ClientLevelRopeManager ropeManager, Player player, Vector3d from, Vector3d to, double bestDiffSqr) {
      Vec3 oldPlayerPosition = new Vec3(player.xo, player.yo, player.zo);
      Vec3 playerMovement = player.position().subtract(oldPlayerPosition);
      double length = Math.min(playerMovement.length(), 15.0);
      if (length > 1.0E-4) {
         playerMovement = playerMovement.normalize();
      }

      Vector3d offsetFrom = new Vector3d();
      Vector3d offsetTo = new Vector3d();
      Vector3d offset = new Vector3d();

      for (double i = 0.0; i < Math.max(length, 0.01); i += 0.25) {
         JOMLConversion.toJOML(playerMovement, offset).mul(-i);
         offsetFrom.set(from).add(offset);
         offsetTo.set(to).add(offset);
         UUID foundStrand = raycastRope(ropeManager, offsetFrom, offsetTo, bestDiffSqr, 0.25);
         if (foundStrand != null) {
            ClientRopeStrand strand = ropeManager.getStrand(foundStrand);
            if (strand != null) {
               ZiplineClientManager.ClosestQuery query = getClosestPointOnStrand(strand, player);
               if (canStartRiding(query, player, true)) {
                  embark(foundStrand);
                  player.swing(InteractionHand.MAIN_HAND);
                  return;
               }
            }
         }
      }
   }

   private static void ridingTick() {
      Minecraft mc = Minecraft.getInstance();
      if (!mc.isPaused()) {
         if (!AllItemTags.CHAIN_RIDEABLE.matches(mc.player.getMainHandItem())) {
            disembark();
         } else {
            ClientLevelRopeManager ropeHandler = ClientLevelRopeManager.getOrCreate(mc.level);
            ClientRopeStrand strand = ropeHandler.getStrand(ridingRope);
            if (mc.player.onGround()) {
               groundedTimer++;
            } else {
               groundedTimer = 0;
            }

            if (groundedTimer <= 5 && !mc.player.isShiftKeyDown() && !mc.player.getAbilities().flying && strand != null) {
               float chainYOffset = 0.5F * mc.player.getScale();
               Vec3 playerPosition = mc.player.position().add(0.0, mc.player.getBoundingBox().getYsize() + (double)chainYOffset, 0.0);
               ZiplineClientManager.ClosestQuery query = getClosestPointOnStrand(strand, playerPosition);
               boolean isEnd = query.position().distanceSquared(((ClientRopePoint)strand.getPoints().getLast()).position()) < 0.25;
               boolean isStart = query.position().distanceSquared(((ClientRopePoint)strand.getPoints().getFirst()).position()) < 0.25;
               Vec3 mojNormal = new Vec3(query.normal.x, query.normal.y, query.normal.z);
               double exitThreshold = 0.6;
               Vec3 exitingMovement = mc.player.getDeltaMovement();
               if (!(exitingMovement.lengthSqr() > 1.0E-8)
                  || (!isEnd || !(exitingMovement.normalize().dot(mojNormal) > 0.6)) && (!isStart || !(exitingMovement.normalize().dot(mojNormal) < -0.6))) {
                  Vec3 target = JOMLConversion.toMojang(query.position());
                  Vec3 diff = target.subtract(playerPosition);
                  Vec3 normal = JOMLConversion.toMojang(query.normal());
                  Vec3 assistanceForce = normal.scale(mc.player.getDeltaMovement().dot(normal)).scale(0.04);
                  double reach = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 1.0;
                  if (diff.lengthSqr() > reach * reach) {
                     disembark();
                  } else {
                     Vec3 dampingForce = mc.player.getDeltaMovement().scale(-0.6);
                     dampingForce = dampingForce.subtract(normal.scale(normal.dot(dampingForce)));
                     float diffLength = diff.lengthSqr() > 0.0 ? Mth.sqrt((float)diff.length()) : 0.0F;
                     mc.player.setDeltaMovement(mc.player.getDeltaMovement().add(dampingForce).add(assistanceForce).add(diff.scale((double)diffLength * 0.3)));
                     mc.player.fallDistance = 0.0F;
                     if (AnimationTickHolder.getTicks() % 10 == 0) {
                        VeilPacketManager.server().sendPacket(new CustomPacketPayload[]{new RopeRidingPacket(ridingRope, false)});
                     }
                  }
               } else {
                  disembark();
               }
            } else {
               disembark();
            }
         }
      }
   }

   public static boolean canStartRidingDistance(ZiplineClientManager.ClosestQuery query, Player player) {
      double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 1.0;
      return query.position.distanceSquared(JOMLConversion.toJOML(player.position())) <= reach * reach;
   }

   public static boolean canStartRidingSteepness(ZiplineClientManager.ClosestQuery query, Player player) {
      double verticalDot = query.normal.dot(new Vector3d(0.0, 1.0, 0.0));
      return Math.abs(verticalDot) <= Math.sin(Math.toRadians((double)SimConfigService.INSTANCE.server().blocks.maxRopeZiplineAngle.getF()));
   }

   public static boolean canStartRiding(ZiplineClientManager.ClosestQuery query, Player player, boolean sendMessage) {
      if (!canStartRidingDistance(query, player)) {
         if (sendMessage) {
            player.displayClientMessage(SimLang.translate("zipline.too_far").color(SimColors.NUH_UH_RED).component(), true);
         }

         return false;
      } else if (!canStartRidingSteepness(query, player)) {
         if (sendMessage) {
            player.displayClientMessage(SimLang.translate("zipline.too_steep").color(SimColors.NUH_UH_RED).component(), true);
         }

         return false;
      } else {
         return true;
      }
   }

   public static ZiplineClientManager.ClosestQuery getClosestPointOnStrand(ClientRopeStrand strand, Vec3 playerPosition) {
      ObjectArrayList<ClientRopePoint> points = strand.getPoints();
      double minDistanceSquared = Double.MAX_VALUE;
      Vector3d minPoint = new Vector3d();
      Vector3d minNormal = new Vector3d();
      Vector3d point = new Vector3d();
      Vector3d diff = new Vector3d();
      Vector3d normalizedDiff = new Vector3d();

      for (int i = 0; i < points.size() - 1; i++) {
         Vector3dc pointA = ((ClientRopePoint)points.get(i)).position();
         Vector3dc pointB = ((ClientRopePoint)points.get(i + 1)).position();
         pointB.sub(pointA, diff);
         diff.normalize(normalizedDiff);
         point.set(playerPosition.x, playerPosition.y, playerPosition.z).sub(pointA);
         double along = point.dot(normalizedDiff);
         along = Mth.clamp(along, 0.0, diff.length());
         point.set(pointA).fma(along, normalizedDiff);
         double distance = point.distanceSquared(playerPosition.x, playerPosition.y, playerPosition.z);
         if (distance < minDistanceSquared) {
            minPoint.set(point);
            minNormal.set(normalizedDiff);
            minDistanceSquared = distance;
         }
      }

      return new ZiplineClientManager.ClosestQuery(minPoint, minNormal);
   }

   public static ZiplineClientManager.ClosestQuery getClosestPointOnStrand(ClientRopeStrand strand, Player player) {
      float chainYOffset = 0.5F * player.getScale();
      Vec3 playerPosition = player.position().add(0.0, player.getBoundingBox().getYsize() + (double)chainYOffset, 0.0);
      return getClosestPointOnStrand(strand, playerPosition);
   }

   public static boolean isRopeInteractable(ItemStack stack) {
      return AllItemTags.CHAIN_RIDEABLE.matches(stack) || stack.is(SimTags.Items.DESTROYS_ROPE);
   }

   public static void embark(UUID rope) {
      Minecraft mc = Minecraft.getInstance();
      Component component = Component.translatable("mount.onboard", new Object[]{mc.options.keyShift.getTranslatedKeyMessage()});
      mc.gui.setOverlayMessage(component, false);
      mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.WOOL_HIT, 1.0F, 0.5F));
      ridingRope = rope;
      mc.player.getAbilities().flying = false;
      mc.player.stopFallFlying();
      VeilPacketManager.server().sendPacket(new CustomPacketPayload[]{new RopeRidingPacket(ridingRope, false)});
   }

   public static void disembark() {
      if (ridingRope != null) {
         VeilPacketManager.server().sendPacket(new CustomPacketPayload[]{new RopeRidingPacket(ridingRope, true)});
      }

      ridingRope = null;
      Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.WOOL_HIT, 0.75F, 0.35F));
   }

   @Override
   public InteractCallback.Result onUse(int modifiers, int action, KeyMapping rightKey) {
      if (action != 0 && hoveringRope != null && ridingRope != hoveringRope) {
         Minecraft mc = Minecraft.getInstance();
         ItemStack mainHandItem = mc.player.getMainHandItem();
         boolean isWrench = AllItemTags.CHAIN_RIDEABLE.matches(mainHandItem);
         boolean isDestroyer = mainHandItem.is(SimTags.Items.DESTROYS_ROPE);
         if (isWrench && !mc.player.isShiftKeyDown()) {
            ClientLevelRopeManager ropeManager = ClientLevelRopeManager.getOrCreate(mc.player.level());
            ClientRopeStrand strand = ropeManager.getStrand(hoveringRope);
            if (strand == null) {
               return InteractCallback.Result.empty();
            } else {
               ZiplineClientManager.ClosestQuery query = getClosestPointOnStrand(strand, mc.player);
               if (!canStartRiding(query, mc.player, true)) {
                  return InteractCallback.Result.empty();
               } else {
                  embark(hoveringRope);
                  mc.player.swing(InteractionHand.MAIN_HAND);
                  return new InteractCallback.Result(true);
               }
            }
         } else if (!isDestroyer && (!isWrench || !mc.player.isShiftKeyDown())) {
            return InteractCallback.Result.empty();
         } else {
            VeilPacketManager.server().sendPacket(new CustomPacketPayload[]{new RopeBreakPacket(hoveringRope)});
            mc.player.swing(InteractionHand.MAIN_HAND);
            return new InteractCallback.Result(true);
         }
      } else {
         return InteractCallback.Result.empty();
      }
   }

   public static record ClosestQuery(Vector3d position, Vector3d normal) {
   }
}

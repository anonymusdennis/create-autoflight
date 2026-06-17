package com.simibubi.create.content.logistics.packagePort;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.neoforge.common.Tags.Items;

public class PackagePortTargetSelectionHandler {
   public static PackagePortTarget activePackageTarget;
   public static Vec3 exactPositionOfTarget;
   public static boolean isPostbox;

   public static void flushSettings(BlockPos pos) {
      if (activePackageTarget == null) {
         CreateLang.translate("gui.package_port.not_targeting_anything").sendStatus(Minecraft.getInstance().player);
      } else {
         if (validateDiff(exactPositionOfTarget, pos) == null) {
            activePackageTarget.relativePos = activePackageTarget.relativePos.subtract(pos);
            CatnipServices.NETWORK.sendToServer(new PackagePortPlacementPacket(activePackageTarget, pos));
         }

         activePackageTarget = null;
         isPostbox = false;
      }
   }

   public static boolean onUse() {
      Minecraft mc = Minecraft.getInstance();
      HitResult hitResult = mc.hitResult;
      ItemStack mainHandItem = mc.player.getMainHandItem();
      if (hitResult == null || hitResult.getType() == Type.MISS) {
         return false;
      } else if (hitResult instanceof BlockHitResult bhr) {
         BlockPos pos = bhr.getBlockPos();
         if (mc.level.getBlockEntity(pos) instanceof StationBlockEntity sbe) {
            if (sbe.edgePoint == null) {
               return false;
            } else if (!AllTags.AllItemTags.POSTBOXES.matches(mainHandItem)) {
               return false;
            } else {
               exactPositionOfTarget = Vec3.atCenterOf(pos);
               activePackageTarget = new PackagePortTarget.TrainStationFrogportTarget(pos);
               isPostbox = true;
               return true;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static void tick() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      boolean isPostbox = AllTags.AllItemTags.POSTBOXES.matches(player.getMainHandItem());
      boolean isWrench = player.getMainHandItem().is(Items.TOOLS_WRENCH);
      if (!isWrench) {
         if (activePackageTarget == null) {
            return;
         }

         if (!AllBlocks.PACKAGE_FROGPORT.isIn(player.getMainHandItem()) && !isPostbox) {
            return;
         }
      }

      if (mc.hitResult instanceof BlockHitResult blockRayTraceResult) {
         if (isWrench) {
            if (blockRayTraceResult.getType() != Type.MISS) {
               BlockPos pos = blockRayTraceResult.getBlockPos();
               if (mc.level.getBlockEntity(pos) instanceof PackagePortBlockEntity ppbe) {
                  if (ppbe.target != null) {
                     Vec3 source = Vec3.atBottomCenterOf(pos);
                     Vec3 target = ppbe.target.getExactTargetLocation(ppbe, mc.level, pos);
                     if (target != Vec3.ZERO) {
                        Color color = new Color(10411635);
                        animateConnection(mc, source, target, color);
                        Outliner.getInstance().chaseAABB("ChainPointSelected", new AABB(target, target)).colored(color).lineWidth(0.2F).disableLineNormals();
                     }
                  }
               }
            }
         } else {
            Vec3 target = exactPositionOfTarget;
            if (blockRayTraceResult.getType() == Type.MISS) {
               Outliner.getInstance().chaseAABB("ChainPointSelected", new AABB(target, target)).colored(10411635).lineWidth(0.2F).disableLineNormals();
            } else {
               BlockPos pos = blockRayTraceResult.getBlockPos();
               if (!mc.level.getBlockState(pos).canBeReplaced()) {
                  pos = pos.relative(blockRayTraceResult.getDirection());
               }

               String validateDiff = validateDiff(target, pos);
               boolean valid = validateDiff == null;
               Color color = new Color(valid ? 10411635 : 16740721);
               Vec3 source = Vec3.atBottomCenterOf(pos);
               CreateLang.translate(validateDiff != null ? validateDiff : "package_port.valid").color(color.getRGB()).sendStatus(player);
               Outliner.getInstance().chaseAABB("ChainPointSelected", new AABB(target, target)).colored(color).lineWidth(0.2F).disableLineNormals();
               if (mc.level.getBlockState(pos).canBeReplaced()) {
                  Outliner.getInstance()
                     .chaseAABB("TargetedFrogPos", new AABB(pos).contract(0.0, 1.0, 0.0).deflate(0.125, 0.0, 0.125))
                     .colored(color)
                     .lineWidth(0.0625F)
                     .disableLineNormals();
                  animateConnection(mc, source, target, color);
               }
            }
         }
      }
   }

   public static void animateConnection(Minecraft mc, Vec3 source, Vec3 target, Color color) {
      DustParticleOptions data = new DustParticleOptions(color.asVectorF(), 1.0F);
      ClientLevel world = mc.level;
      double totalFlyingTicks = 10.0;
      int segments = (int)totalFlyingTicks / 3 + 1;
      double tickOffset = totalFlyingTicks / (double)segments;

      for (int i = 0; i < segments; i++) {
         double ticks = (double)(AnimationTickHolder.getRenderTime() / 3.0F) % tickOffset + (double)i * tickOffset;
         Vec3 vec = source.lerp(target, ticks / totalFlyingTicks);
         world.addParticle(data, vec.x, vec.y, vec.z, 0.0, 0.0, 0.0);
      }
   }

   public static String validateDiff(Vec3 target, BlockPos placedPos) {
      Vec3 source = Vec3.atBottomCenterOf(placedPos);
      Vec3 diff = target.subtract(source);
      if (diff.y < 0.0 && !isPostbox) {
         return "package_port.cannot_reach_down";
      } else {
         return diff.length() > (double)((Integer)AllConfigs.server().logistics.packagePortRange.get()).intValue() ? "package_port.too_far" : null;
      }
   }
}

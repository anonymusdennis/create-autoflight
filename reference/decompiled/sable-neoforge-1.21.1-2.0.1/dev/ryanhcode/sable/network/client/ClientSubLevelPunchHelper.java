package dev.ryanhcode.sable.network.client;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.index.SableAttributes;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.network.packets.tcp.ServerboundPunchSubLevelPacket;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Vector3d;

public class ClientSubLevelPunchHelper {
   public static void clientTryPunch(BlockHitResult hitResult, Level level, boolean testCreativeBreaking) {
      Minecraft minecraft = Minecraft.getInstance();
      LocalPlayer player = minecraft.player;
      if (!player.blockActionRestricted(level, hitResult.getBlockPos(), minecraft.gameMode.getPlayerMode())
         && !player.getCooldowns().isOnCooldown(player.getMainHandItem().getItem())) {
         if (player.isCreative() && testCreativeBreaking) {
            BlockState blockState = minecraft.level.getBlockState(hitResult.getBlockPos());
            if (player.getMainHandItem().getItem().canAttackBlock(blockState, minecraft.level, hitResult.getBlockPos(), player)) {
               return;
            }
         }

         Vector3d hitPosition = JOMLConversion.toJOML(hitResult.getLocation());
         Vector3d hitDirection = JOMLConversion.toJOML(player.getLookAngle());
         SubLevel targetSubLevel = Sable.HELPER.getContaining(level, hitPosition);
         SubLevel trackingSubLevel = ((EntityMovementExtension)player).sable$getTrackingSubLevel();
         if (targetSubLevel != trackingSubLevel) {
            if (targetSubLevel != null) {
               targetSubLevel.lastPose().transformPosition(hitPosition);
               hitPosition.sub(targetSubLevel.lastPose().position());
            }

            if (trackingSubLevel != null) {
               trackingSubLevel.lastPose().transformNormalInverse(hitDirection);
            }

            int customCooldown = SableAttributes.getPushCooldownTicks(player);
            if (customCooldown > 0) {
               player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), customCooldown);
            }

            minecraft.getConnection()
               .send(new ServerboundCustomPayloadPacket(new ServerboundPunchSubLevelPacket(hitResult.getBlockPos(), hitPosition, hitDirection)));
         }
      }
   }
}

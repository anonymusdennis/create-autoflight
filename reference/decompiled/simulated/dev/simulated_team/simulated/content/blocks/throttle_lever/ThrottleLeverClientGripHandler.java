package dev.simulated_team.simulated.content.blocks.throttle_lever;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.simulated_team.simulated.index.SimBlocks;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class ThrottleLeverClientGripHandler {
   private static final PoseStack stack = new PoseStack();
   private static final Set<ThrottleLeverBlockEntity> nearbyThrottleLevers = new ObjectOpenHashSet();

   public static void tickGrip(ThrottleLeverBlockEntity blockEntity) {
      if (!isInvalid(blockEntity)) {
         nearbyThrottleLevers.add(blockEntity);
      }
   }

   private static boolean isInvalid(ThrottleLeverBlockEntity blockEntity) {
      if (blockEntity.isRemoved()) {
         return true;
      } else {
         Minecraft minecraft = Minecraft.getInstance();
         LocalPlayer player = minecraft.player;
         if (player == null) {
            return true;
         } else {
            double reach = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).getValue() + 2.0;
            BlockPos blockPos = blockEntity.getBlockPos();
            return player.distanceToSqr(blockPos.getCenter()) > reach * reach;
         }
      }
   }

   public static void clearNearbyThrottleLevers() {
      nearbyThrottleLevers.removeIf(ThrottleLeverClientGripHandler::isInvalid);
   }

   public static Collection<ThrottleLeverBlockEntity> getNearbyThrottleLevers() {
      return nearbyThrottleLevers;
   }

   public static Double raycastLever(Vec3 eyePosMoj, Vec3 viewVectorMoj, ThrottleLeverBlockEntity lever, float partialTicks) {
      LocalPlayer player = Minecraft.getInstance().player;

      assert player != null;

      BlockPos leverPos = lever.getBlockPos();
      Vector3d eyePos = JOMLConversion.toJOML(eyePosMoj);
      Vector3d viewVector = JOMLConversion.toJOML(viewVectorMoj);
      ClientSubLevel subLevel = Sable.HELPER.getContainingClient(lever);
      if (subLevel != null) {
         Pose3dc pose = subLevel.renderPose(partialTicks);
         pose.transformPositionInverse(eyePos);
         pose.transformNormalInverse(viewVector);
      }

      stack.pushPose();
      stack.translate((double)leverPos.getX() - eyePos.x, (double)leverPos.getY() - eyePos.y, (double)leverPos.getZ() - eyePos.z);
      ThrottleLeverRenderer.transformHandleExternal(lever, partialTicks, stack);
      Matrix4f pose = stack.last().pose();
      pose.invert();
      stack.popPose();
      Vector3f localViewPosition = pose.transformPosition(new Vector3f());
      Vector3f localViewDirection = pose.transformDirection(new Vector3f((float)viewVector.x, (float)viewVector.y, (float)viewVector.z));
      VoxelShape leverShape = ((ThrottleLeverBlock)SimBlocks.THROTTLE_LEVER.get()).getHandleShape(SimBlocks.THROTTLE_LEVER.getDefaultState());
      eyePos.set(localViewPosition);
      viewVector.set(localViewDirection).mul(player.blockInteractionRange()).add(eyePos);
      BlockHitResult hitResult = leverShape.clip(JOMLConversion.toMojang(eyePos), JOMLConversion.toMojang(viewVector), BlockPos.ZERO);
      if (hitResult != null && hitResult.getType() != Type.MISS) {
         Vec3 location = hitResult.getLocation();
         return eyePos.distanceSquared(location.x, location.y, location.z);
      } else {
         return null;
      }
   }
}

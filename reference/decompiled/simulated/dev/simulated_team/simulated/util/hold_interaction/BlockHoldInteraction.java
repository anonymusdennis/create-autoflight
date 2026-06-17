package dev.simulated_team.simulated.util.hold_interaction;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.mixin.hold_interaction.KeyMappingInvoker;
import dev.simulated_team.simulated.util.click_interactions.InteractCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public abstract class BlockHoldInteraction implements InteractCallback {
   private BlockPos interactionPos = null;

   @OverrideOnly
   public void start() {
      ((KeyMappingInvoker)Minecraft.getInstance().options.keyAttack).invokeRelease();
   }

   @OverrideOnly
   public void stop() {
      this.interactionPos = null;
   }

   @OverrideOnly
   public void release() {
   }

   public boolean isActive() {
      return HoldInteractionManager.isActive(this);
   }

   public boolean isBlockActive(BlockPos pos) {
      return this.isActive() && pos.equals(this.interactionPos);
   }

   public void renderOverlay(GuiGraphics graphics, int width1, int height1, boolean hideGui) {
   }

   public static double getInteractionRange(Player player) {
      return player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).getValue();
   }

   public static boolean inInteractionRange(Player player, Position target, double reachBuffer) {
      double distance = getInteractionRange(player) + reachBuffer;
      Vec3 eyePosition = player.getEyePosition();
      return Sable.HELPER.projectOutOfSubLevel(player.level(), JOMLConversion.toJOML(target)).distanceSquared(eyePosition.x, eyePosition.y, eyePosition.z)
         < distance * distance;
   }

   public static boolean inInteractionRange(Player player, Vector3dc target, double reachBuffer) {
      double distance = getInteractionRange(player) + reachBuffer;
      Vec3 eyePosition = player.getEyePosition();
      return Sable.HELPER.projectOutOfSubLevel(player.level(), target, new Vector3d()).distanceSquared(eyePosition.x, eyePosition.y, eyePosition.z)
         < distance * distance;
   }

   public static boolean inInteractionRange(Player player, Position target) {
      return inInteractionRange(player, target, 0.0);
   }

   public int getCrouchBlockingTicks() {
      return 0;
   }

   public BlockPos getInteractionPos() {
      return this.interactionPos;
   }

   public void startHold(Level level, Player player, BlockPos blockPos) {
      HoldInteractionManager.start(this);
      this.interactionPos = blockPos;
   }

   @Override
   public InteractCallback.Result onAttack(int modifiers, int action, KeyMapping leftKey) {
      return this.isActive() && action != 0 ? new InteractCallback.Result(true) : InteractCallback.super.onAttack(modifiers, action, leftKey);
   }

   @Override
   public InteractCallback.Result onUse(int modifiers, int action, KeyMapping rightKey) {
      if (action == 0 && this.isActive()) {
         this.release();
         HoldInteractionManager.stop();
         ((KeyMappingInvoker)Minecraft.getInstance().options.keyUse).invokeRelease();
      }

      return InteractCallback.super.onUse(modifiers, action, rightKey);
   }

   public boolean activeTick(Level level, LocalPlayer player) {
      return false;
   }

   @Override
   public InteractCallback.Result onMouseMove(double yaw, double pitch) {
      return this.isActive() && this.activeOnMouseMove(yaw, pitch) ? new InteractCallback.Result(true) : InteractCallback.super.onMouseMove(yaw, pitch);
   }

   public boolean activeOnMouseMove(double yaw, double pitch) {
      return false;
   }
}

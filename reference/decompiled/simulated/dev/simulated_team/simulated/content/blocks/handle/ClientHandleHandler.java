package dev.simulated_team.simulated.content.blocks.handle;

import com.simibubi.create.AllItems;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.index.SimTags;
import dev.simulated_team.simulated.network.packets.UpdatePlayerUsingHandlePacket;
import dev.simulated_team.simulated.util.SimDistUtil;
import dev.simulated_team.simulated.util.click_interactions.InteractCallback;
import dev.simulated_team.simulated.util.hold_interaction.BlockHoldInteraction;
import foundry.veil.api.network.VeilPacketManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public class ClientHandleHandler extends BlockHoldInteraction {
   private float desiredRange = -1.0F;
   public int actuallyUsedBlockCountdown = 0;
   public boolean movingSubLevel = false;

   @Override
   public void startHold(Level level, Player player, BlockPos blockPos) {
      InteractionHand hand = this.getHandOrNull(player);
      if (hand != null) {
         if (level.getBlockEntity(blockPos) instanceof HandleBlockEntity handleBE) {
            Vector3d var9 = handleBE.getGrabCenter();
            Vector3d projected = Sable.HELPER.projectOutOfSubLevel(player.level(), var9);
            Vec3 eyePosition = player.getEyePosition();
            this.desiredRange = (float)Math.min(
               projected.distance(eyePosition.x, eyePosition.y, eyePosition.z),
               Math.min(5.0, player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).getValue())
            );
            this.movingSubLevel = player.isShiftKeyDown();
            player.swing(hand);
            super.startHold(level, player, blockPos);
            this.sendUpdate(false);
         }
      }
   }

   @Override
   public boolean activeTick(Level level, LocalPlayer player) {
      BlockPos interactionPos = this.getInteractionPos();
      ChunkPos chunk = new ChunkPos(interactionPos);
      SubLevelContainer container = SubLevelContainer.getContainer(level);

      assert container != null;

      if (container.inBounds(chunk) && Sable.HELPER.getContaining(level, chunk) == null) {
         return true;
      } else {
         InteractionHand hand = this.getHandOrNull(player);
         if (hand != null && !player.isDeadOrDying() && !player.isSpectator()) {
            if (!(level.getBlockEntity(interactionPos) instanceof HandleBlockEntity handleBE)) {
               return true;
            } else {
               Vector3d var18 = Sable.HELPER.projectOutOfSubLevel(level, handleBE.getGrabCenter());
               if (!inInteractionRange(player, var18, 4.0)) {
                  return true;
               } else if (!HandleBlock.canInteractWithHandle(player)) {
                  return true;
               } else {
                  Minecraft minecraft = Minecraft.getInstance();
                  if (!minecraft.gameRenderer.getMainCamera().isDetached()) {
                     player.swingTime = 0;
                     player.swinging = true;
                     player.swingingArm = InteractionHand.MAIN_HAND;
                  }

                  if (player.isFallFlying()) {
                     player.stopFallFlying();
                  }

                  boolean crouchingOrFlying = this.movingSubLevel || player.getAbilities().flying;
                  if (!crouchingOrFlying) {
                     if (player.input.up) {
                        this.deltaRange(player, -0.5F);
                     }

                     if (player.input.down) {
                        this.deltaRange(player, 0.5F);
                     }

                     VeilPacketManager.server().sendPacket(new CustomPacketPayload[]{new UpdatePlayerUsingHandlePacket(-1.0F, false, interactionPos)});
                     Vec3 eyePos = player.getEyePosition();
                     Vec3 goalEyePos = JOMLConversion.toMojang(var18)
                        .add(player.getLookAngle().scale((double)(-(this.desiredRange * 0.4F)) - Math.max(-player.getLookAngle().y, 0.0)));
                     Vec3 difference = goalEyePos.subtract(eyePos);
                     double differenceLength = difference.length();
                     double maxLength = 2.0;
                     if (differenceLength > 2.0) {
                        difference = difference.scale(2.0 / differenceLength);
                     }

                     player.setDeltaMovement(player.getDeltaMovement().scale(0.25).add(difference.scale(0.3)));
                     player.resetFallDistance();
                  } else {
                     this.sendUpdate(false);
                  }

                  return false;
               }
            }
         } else {
            return true;
         }
      }
   }

   @Override
   public void clientTick(Level level, LocalPlayer player) {
      if (this.actuallyUsedBlockCountdown > 0) {
         this.actuallyUsedBlockCountdown--;
      }

      if (!this.isActive()) {
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft.options.keyUse.isDown() && !minecraft.options.keyShift.isDown()) {
            if (!player.isUsingItem()) {
               if (!player.getMainHandItem().is(AllItems.WRENCH) && !player.getOffhandItem().is(AllItems.WRENCH)) {
                  if (HandleBlock.canInteractWithHandle(player)) {
                     if (this.actuallyUsedBlockCountdown <= 0) {
                        double length = player.getDeltaMovement().length();
                        Vec3 moveNorm = player.getDeltaMovement().normalize();

                        for (double i = -0.2; i < length; i += 0.2) {
                           Vec3 castOrigin = player.getEyePosition().add(moveNorm.scale(i));
                           Vec3 castDir = player.getLookAngle().scale(BlockHoldInteraction.getInteractionRange(player));
                           BlockHitResult clip = level.clip(new ClipContext(castOrigin, castOrigin.add(castDir), Block.OUTLINE, Fluid.NONE, player));
                           BlockState state = level.getBlockState(clip.getBlockPos());
                           if (state.is(SimTags.Blocks.HANDLES)) {
                              this.startHold(level, player, clip.getBlockPos());
                              return;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public InteractCallback.Result onScroll(double deltaX, double deltaY) {
      Player player = SimDistUtil.getClientPlayer();
      if (this.isActive() && player != null) {
         this.deltaRange(player, (float)deltaY);
         return new InteractCallback.Result(true);
      } else {
         return InteractCallback.Result.empty();
      }
   }

   public void deltaRange(Player player, float delta) {
      this.desiredRange = (float)Math.clamp(
         (double)(this.desiredRange + delta), 1.0, Math.min(player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).getValue(), 5.0)
      );
   }

   @Override
   public void stop() {
      this.sendUpdate(true);
      this.desiredRange = -1.0F;
      super.stop();
   }

   @Nullable
   public InteractionHand getHandOrNull(Player player) {
      ItemStack mainItem = player.getItemInHand(InteractionHand.MAIN_HAND);
      ItemStack offHandItem = player.getItemInHand(InteractionHand.OFF_HAND);
      return this.isEmptyOrExtendoGrip(mainItem) ? InteractionHand.MAIN_HAND : (this.isEmptyOrExtendoGrip(offHandItem) ? InteractionHand.OFF_HAND : null);
   }

   public boolean isEmptyOrExtendoGrip(ItemStack stack) {
      return stack.isEmpty() || AllItems.EXTENDO_GRIP.is(stack.getItem());
   }

   private void sendUpdate(boolean remove) {
      VeilPacketManager.server()
         .sendPacket(new CustomPacketPayload[]{new UpdatePlayerUsingHandlePacket(remove ? -1.0F : this.desiredRange, remove, this.getInteractionPos())});
   }
}

package dev.simulated_team.simulated.content.items.spring;

import dev.ryanhcode.sable.Sable;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.network.packets.PlaceSpringPacket;
import dev.simulated_team.simulated.util.SimColors;
import dev.simulated_team.simulated.util.SimDistUtil;
import dev.simulated_team.simulated.util.click_interactions.InteractCallback;
import foundry.veil.api.network.VeilPacketManager;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.Nullable;

public class SpringItemHandler implements InteractCallback {
   public static final double MAX_LENGTH = 9.0;
   public BlockPos linkPos;
   public Direction linkDirection;

   public boolean tryStartPlacement(UseOnContext context) {
      LocalPlayer player = (LocalPlayer)SimDistUtil.getClientPlayer();
      Level level = player.level();
      Direction dir = context.getClickedFace();
      BlockPos pos = context.getClickedPos();
      BlockPos relative = pos.relative(dir);
      if (this.linkPos != null) {
         return false;
      } else if (!this.testPlacementAndSendError(level, relative, pos, dir)) {
         return false;
      } else {
         this.linkPos = pos;
         this.linkDirection = dir;
         return true;
      }
   }

   @Override
   public InteractCallback.Result onUse(int modifiers, int action, KeyMapping rightKey) {
      LocalPlayer player = (LocalPlayer)SimDistUtil.getClientPlayer();
      Level level = player.level();
      if (action == 1) {
         InteractionHand hand = this.getHandOrNull(player);
         if (hand == null) {
            this.reset(true);
            return InteractCallback.Result.empty();
         }

         if (this.linkPos != null && player.isShiftKeyDown()) {
            player.swing(hand);
            this.reset(true);
            return new InteractCallback.Result(true);
         }

         if (Minecraft.getInstance().hitResult instanceof BlockHitResult hit && hit.getType() != Type.MISS && this.linkPos != null) {
            Direction dir = hit.getDirection();
            BlockPos pos = hit.getBlockPos();
            BlockPos childCenter = pos.relative(dir);
            BlockPos parentCenter = this.linkPos.relative(this.linkDirection);
            if (this.testExceedsRange(level, childCenter, parentCenter)) {
               this.sendMessage("out_of_range", SimColors.NUH_UH_RED);
               return InteractCallback.Result.empty();
            }

            if (parentCenter.equals(childCenter)) {
               this.sendMessage("same_block", SimColors.NUH_UH_RED);
               return InteractCallback.Result.empty();
            }

            if (!this.testPlacementAndSendError(level, childCenter, pos, dir)) {
               return InteractCallback.Result.empty();
            }

            player.swing(hand);
            VeilPacketManager.server().sendPacket(new CustomPacketPayload[]{new PlaceSpringPacket(this.linkPos, pos, this.linkDirection, dir, hand)});
            this.reset(false);
            return new InteractCallback.Result(true);
         }
      }

      return InteractCallback.Result.empty();
   }

   private boolean testExceedsRange(Level level, BlockPos childPos, BlockPos parentPos) {
      return Sable.HELPER
            .distanceSquaredWithSubLevels(
               level,
               (double)childPos.getX() + 0.5,
               (double)childPos.getY() + 0.5,
               (double)childPos.getZ() + 0.5,
               (double)parentPos.getX() + 0.5,
               (double)parentPos.getY() + 0.5,
               (double)parentPos.getZ() + 0.5
            )
         > 81.0;
   }

   private boolean testPlacementAndSendError(Level level, BlockPos relative, BlockPos pos, Direction dir) {
      if (!level.getBlockState(relative).canBeReplaced()) {
         this.sendMessage("block_exists", SimColors.NUH_UH_RED);
         return false;
      } else if (!Block.canSupportCenter(level, pos, dir)) {
         this.sendMessage("not_enough_support", SimColors.NUH_UH_RED);
         return false;
      } else {
         return true;
      }
   }

   @Nullable
   public InteractionHand getHandOrNull(LocalPlayer player) {
      ItemStack mainItem = player.getItemInHand(InteractionHand.MAIN_HAND);
      ItemStack offHandItem = player.getItemInHand(InteractionHand.OFF_HAND);
      InteractionHand hand = null;
      if (mainItem.getItem() instanceof SpringItem) {
         hand = InteractionHand.MAIN_HAND;
      } else if (offHandItem.getItem() instanceof SpringItem) {
         hand = InteractionHand.OFF_HAND;
      }

      return hand;
   }

   public void reset(boolean sayMessage) {
      if (sayMessage && this.linkPos != null) {
         this.sendMessage("connection_terminated", SimColors.NUH_UH_RED);
      }

      this.linkPos = null;
      this.linkDirection = null;
   }

   public void sendMessage(String message, int color) {
      SimLang.translate("spring." + message).color(color).sendStatus(SimDistUtil.getClientPlayer());
   }

   @Override
   public void clientTick(Level level, LocalPlayer player) {
      if (!player.getMainHandItem().is(SimItems.SPRING) && !player.getOffhandItem().is(SimItems.SPRING)) {
         this.reset(true);
      } else {
         if (this.linkPos != null) {
            Vec3 linkVec = new Vec3((double)this.linkDirection.getStepX(), (double)this.linkDirection.getStepY(), (double)this.linkDirection.getStepZ());
            AABB linkAABB = new AABB(this.linkPos).inflate(-0.3).move(linkVec.scale(0.65));
            Outliner.getInstance().showAABB(this.linkPos + "Spring", linkAABB).colored(SimColors.SUCCESS_LIME).lineWidth(0.0625F);
            HitResult clientHit = Minecraft.getInstance().hitResult;
            if (clientHit != null && clientHit.getType() != Type.MISS && clientHit instanceof BlockHitResult hit) {
               BlockPos pos = hit.getBlockPos();
               Direction dir = hit.getDirection();
               BlockPos childCenter = pos.relative(dir);
               BlockPos parentCenter = this.linkPos.relative(this.linkDirection);
               int color = SimColors.SUCCESS_LIME;
               if (!level.getBlockState(pos.relative(dir)).canBeReplaced()
                  || !Block.canSupportCenter(level, pos, dir)
                  || this.linkPos.relative(this.linkDirection).equals(pos.relative(dir))
                  || this.testExceedsRange(level, childCenter, parentCenter)) {
                  color = SimColors.NUH_UH_RED;
               }

               AABB hitAABB = new AABB(pos).inflate(-0.3).move(new Vec3((double)dir.getStepX(), (double)dir.getStepY(), (double)dir.getStepZ()).scale(0.65));
               Vec3 globalFirstPoint = Sable.HELPER.projectOutOfSubLevel(level, linkAABB.getCenter());
               Vec3 globalTarget = Sable.HELPER.projectOutOfSubLevel(level, hitAABB.getCenter());
               DustParticleOptions data = new DustParticleOptions(new Color(color).asVectorF(), 1.0F);
               double totalFlyingTicks = 10.0;
               int segments = 4;

               for (int i = 0; i < 4; i++) {
                  Vec3 vec = globalFirstPoint.lerp(globalTarget, (double)level.getRandom().nextFloat());
                  level.addParticle(data, vec.x, vec.y, vec.z, 0.0, 0.0, 0.0);
               }

               Outliner.getInstance().showAABB(this.linkPos + " Spring Selection", hitAABB).colored(color).lineWidth(0.0625F);
            }
         }
      }
   }
}

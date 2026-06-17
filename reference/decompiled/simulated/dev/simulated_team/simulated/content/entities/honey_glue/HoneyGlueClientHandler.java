package dev.simulated_team.simulated.content.entities.honey_glue;

import com.simibubi.create.AllKeys;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.utility.RaycastHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimEntityTypes;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.index.SimSoundEvents;
import dev.simulated_team.simulated.index.SimSpecialTextures;
import dev.simulated_team.simulated.mixin.aabb.AABBMixin;
import dev.simulated_team.simulated.network.packets.honey_glue.HoneyGlueChangeBoundsPacket;
import dev.simulated_team.simulated.network.packets.honey_glue.HoneyGlueSpawnPacket;
import dev.simulated_team.simulated.service.SimConfigService;
import dev.simulated_team.simulated.util.SimColors;
import dev.simulated_team.simulated.util.SimDistUtil;
import dev.simulated_team.simulated.util.click_interactions.InteractCallback;
import foundry.veil.api.network.VeilPacketManager;
import java.util.List;
import java.util.Optional;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HoneyGlueClientHandler implements InteractCallback {
   private HoneyGlueClientHandler.State currentState = HoneyGlueClientHandler.State.UNBOUND;
   private BlockPos selectedPos;
   private HoneyGlueEntity hoveredGlue;
   private Direction hoveredFace;

   public boolean selectPos(BlockPos pos, Player player, ItemStack honeyGlueStack) {
      if (this.currentState == HoneyGlueClientHandler.State.UNBOUND) {
         this.selectedPos = pos;
         this.currentState = HoneyGlueClientHandler.State.BINDING;
         SimSoundEvents.HONEY_ADDED.playAt(player.level(), this.selectedPos, 0.5F, 0.95F, false);
         player.level().playSound(player, this.selectedPos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.75F, 1.0F);
      } else {
         if (!this.checkBBValidity(player, honeyGlueStack, pos, false)) {
            return false;
         }

         VeilPacketManager.server().sendPacket(new CustomPacketPayload[]{new HoneyGlueSpawnPacket(this.selectedPos, pos)});
         SimSoundEvents.HONEY_ADDED.playAt(player.level(), pos, 0.5F, 0.95F, false);
         player.level().playSound(player, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.75F, 1.0F);
         this.selectedPos = null;
         this.currentState = HoneyGlueClientHandler.State.UNBOUND;
      }

      return true;
   }

   @Override
   public InteractCallback.Result onUse(int modifiers, int action, KeyMapping rightKey) {
      if (action == 1) {
         Player player = SimDistUtil.getClientPlayer();
         InteractionHand hand = this.getHoneyGlueHand(player);
         if (hand == null) {
            return InteractCallback.Result.empty();
         }

         if (player.isShiftKeyDown()) {
            this.clearAndSwing(player);
            return new InteractCallback.Result(true);
         }

         BlockHitResult bhr = this.getHitResult();
         if (AllKeys.altDown() && bhr.getType() == Type.MISS) {
            boolean success = this.selectPos(bhr.getBlockPos(), player, player.getItemInHand(hand));
            if (success) {
               player.swing(InteractionHand.MAIN_HAND);
               return new InteractCallback.Result(true);
            }

            return InteractCallback.Result.empty();
         }
      }

      return InteractCallback.super.onUse(modifiers, action, rightKey);
   }

   @Override
   public InteractCallback.Result onAttack(int modifiers, int action, KeyMapping leftKey) {
      if (action == 1) {
         Player player = SimDistUtil.getClientPlayer();
         if (this.getHoneyGlueHand(player) == null) {
            return InteractCallback.Result.empty();
         }

         if (this.hoveredGlue != null) {
            VeilPacketManager.server()
               .sendPacket(new CustomPacketPayload[]{new HoneyGlueChangeBoundsPacket(new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0), this.hoveredGlue.getUUID())});
            this.clearAndSwing(player);
            return new InteractCallback.Result(true);
         }
      }

      return InteractCallback.super.onAttack(modifiers, action, leftKey);
   }

   @Override
   public InteractCallback.Result onScroll(double deltaX, double deltaY) {
      Player player = SimDistUtil.getClientPlayer();
      if (player == null) {
         return InteractCallback.Result.empty();
      } else {
         InteractionHand hand = this.getHoneyGlueHand(player);
         if (hand != null && AllKeys.ctrlDown()) {
            if (this.currentState == HoneyGlueClientHandler.State.UNBOUND && this.hoveredGlue != null) {
               this.hoveredGlue.updateClientBounds();
               AABB bb = this.hoveredGlue.getBoundingBox();
               ClientSubLevel clientSublevel = Sable.HELPER.getContainingClient(bb.getCenter());
               Vec3 eyePos = player.getEyePosition();
               if (clientSublevel != null) {
                  eyePos = clientSublevel.renderPose().transformPositionInverse(eyePos);
               }

               if (bb.contains(eyePos)) {
                  deltaY *= -1.0;
               }

               AABB newBounds = this.extendHoneyBB(bb, (int)deltaY);
               Pair<Boolean, String> pair = HoneyGlueMaxSizing.checkBounds(newBounds);
               if ((Boolean)pair.getFirst()) {
                  this.hoveredGlue.setBounds(newBounds);
                  VeilPacketManager.server().sendPacket(new CustomPacketPayload[]{new HoneyGlueChangeBoundsPacket(newBounds, this.hoveredGlue.getUUID())});
               } else {
                  SimLang.text((String)pair.getSecond() + getDimensionalText(bb)).color(SimColors.NUH_UH_RED).sendStatus(player);
               }
            }

            return new InteractCallback.Result(true);
         } else {
            return InteractCallback.Result.empty();
         }
      }
   }

   @Override
   public void clientTick(Level level, LocalPlayer player) {
      InteractionHand hand = this.getHoneyGlueHand(player);
      if (hand == null) {
         this.hoveredGlue = null;
         this.selectedPos = null;
         this.currentState = HoneyGlueClientHandler.State.UNBOUND;
      } else {
         BlockHitResult bhr = this.getHitResult();
         this.renderSelection(bhr, player.isShiftKeyDown() ? SimColors.DISCARDABLE_ORANGE : SimColors.ACTIVE_YELLOW);
         this.updateHovered();
         this.renderHoneyGlue();
         this.checkBBValidity(player, player.getItemInHand(hand), bhr.getBlockPos(), AllKeys.altDown() || bhr.getType() != Type.MISS);
      }
   }

   private void renderSelection(BlockHitResult bhr, int color) {
      if (AllKeys.altDown() && !AllKeys.shiftDown() && this.currentState == HoneyGlueClientHandler.State.UNBOUND) {
         Outliner.getInstance()
            .showAABB("HoneyGlue", new AABB(bhr.getBlockPos()))
            .colored(color)
            .withFaceTexture(SimSpecialTextures.HONEY_GLUE)
            .disableLineNormals()
            .lineWidth(0.0625F);
      } else if ((AllKeys.altDown() || bhr.getType() != Type.MISS) && this.currentState == HoneyGlueClientHandler.State.BINDING) {
         if (Sable.HELPER.getContainingClient(bhr.getBlockPos()) != Sable.HELPER.getContainingClient(this.selectedPos)) {
            return;
         }

         Outliner.getInstance()
            .showAABB("HoneyGlue", AABB.encapsulatingFullBlocks(bhr.getBlockPos(), this.selectedPos))
            .colored(color)
            .withFaceTexture(SimSpecialTextures.HONEY_GLUE)
            .disableLineNormals()
            .lineWidth(0.0625F);
      }
   }

   public void updateHovered() {
      Player player = SimDistUtil.getClientPlayer();
      Vec3 baseOrigin = player.getEyePosition();
      Vec3 baseTarget = RaycastHelper.getTraceTarget(player, player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) * 5.0, baseOrigin);
      HoneyGlueEntity closestGlue = null;
      double distance = Double.MAX_VALUE;
      Direction closestDir = null;

      for (HoneyGlueEntity entity : getHoneyGlue(player)) {
         AABB toClip = entity.getBoundingBox();
         SubLevel subLevel = Sable.HELPER.getContainingClient(toClip.getCenter());
         Vec3 subLevelOrigin = baseOrigin;
         Vec3 subLevelTarget = baseTarget;
         if (subLevel != null) {
            subLevelOrigin = subLevel.logicalPose().transformPositionInverse(baseOrigin);
            subLevelTarget = subLevel.logicalPose().transformPositionInverse(baseTarget);
         }

         boolean contains = toClip.contains(subLevelOrigin);
         Optional<Vec3> clip;
         if (contains) {
            clip = toClip.clip(subLevelTarget, subLevelOrigin);
         } else {
            clip = toClip.clip(subLevelOrigin, subLevelTarget);
         }

         if (clip.isPresent()) {
            double hitDist = clip.get().distanceToSqr(subLevelOrigin);
            if (hitDist < distance) {
               distance = hitDist;
               closestGlue = entity;
               closestDir = this.getDirectionFromAABBClip(toClip, subLevelOrigin, subLevelTarget, contains);
            }
         }
      }

      this.hoveredFace = closestDir;
      this.hoveredGlue = closestGlue;
   }

   private void renderHoneyGlue() {
      if (this.currentState == HoneyGlueClientHandler.State.UNBOUND) {
         for (HoneyGlueEntity entity : getHoneyGlue(SimDistUtil.getClientPlayer())) {
            if (entity != this.hoveredGlue) {
               Outliner.getInstance()
                  .showAABB("HoneyGluePassive" + entity, entity.getBoundingBox())
                  .colored(SimColors.PERCHANCE_ORANGE)
                  .disableLineNormals()
                  .lineWidth(0.015625F);
            }
         }

         if (this.hoveredGlue != null) {
            Outliner.getInstance()
               .chaseAABB("HoneyGlueActive" + this.hoveredGlue.getId(), this.hoveredGlue.getBoundingBox())
               .colored(SimColors.ACTIVE_YELLOW)
               .withFaceTexture(SimSpecialTextures.HONEY_GLUE)
               .highlightFace(this.hoveredFace)
               .disableLineNormals()
               .lineWidth(0.0625F);
         }
      }
   }

   private boolean checkBBValidity(Player player, ItemStack honeyGlueStack, BlockPos hoveredPos, boolean showText) {
      int color = SimColors.ACTIVE_YELLOW;
      if (this.currentState == HoneyGlueClientHandler.State.BINDING) {
         String key = "super_glue.click_to_confirm";
         DataComponentMap components = honeyGlueStack.getComponents();
         AABB bb = AABB.encapsulatingFullBlocks(this.selectedPos, hoveredPos);
         boolean showDimensions = true;
         if (HoneyGlueMaxSizing.checkBBMax(bb)) {
            key = "super_glue.too_far";
            color = SimColors.DISCARDABLE_ORANGE;
         } else if (!components.has(DataComponents.MAX_DAMAGE)) {
            key = "super_glue.not_enough";
            showDimensions = false;
            color = SimColors.DISCARDABLE_ORANGE;
         } else if (player.isShiftKeyDown()) {
            key = "super_glue.click_to_discard";
            showDimensions = false;
            color = SimColors.DISCARDABLE_ORANGE;
         }

         if (showText) {
            String dimensions = getDimensionalText(bb);
            CreateLang.translate(key, new Object[0]).text(showDimensions ? dimensions : ".").color(color).sendStatus(player);
         }
      }

      return color == SimColors.ACTIVE_YELLOW;
   }

   public BlockHitResult getHitResult() {
      Player player = SimDistUtil.getClientPlayer();
      Level level = player.level();
      ClipContext clipContext = new ClipContext(
         player.getEyePosition(),
         player.getEyePosition().add(player.getViewVector(SimDistUtil.getPartialTick()).scale(player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE))),
         Block.COLLIDER,
         Fluid.NONE,
         CollisionContext.empty()
      );
      return level.clip(clipContext);
   }

   public Direction getDirectionFromAABBClip(AABB aabb, Vec3 origin, Vec3 end, boolean inside) {
      if (inside) {
         Vec3 temp = origin;
         origin = end;
         end = temp;
      }

      double d = end.x - origin.x;
      double e = end.y - origin.y;
      double f = end.z - origin.z;
      return AABBMixin.invokeGetDirection(aabb, origin, new double[]{1.0}, null, d, e, f);
   }

   public AABB extendHoneyBB(AABB bb, int delta) {
      if (this.hoveredFace == null) {
         return bb;
      } else {
         int x = this.hoveredFace.getStepX() * delta;
         int y = this.hoveredFace.getStepY() * delta;
         int z = this.hoveredFace.getStepZ() * delta;
         AxisDirection axisDirection = this.hoveredFace.getAxisDirection();
         if (axisDirection == AxisDirection.NEGATIVE) {
            bb = bb.move((double)(-x), (double)(-y), (double)(-z));
         }

         double maxX = Math.max(bb.maxX - (double)(x * axisDirection.getStep()), bb.minX);
         double maxY = Math.max(bb.maxY - (double)(y * axisDirection.getStep()), bb.minY);
         double maxZ = Math.max(bb.maxZ - (double)(z * axisDirection.getStep()), bb.minZ);
         return new AABB(bb.minX, bb.minY, bb.minZ, maxX, maxY, maxZ);
      }
   }

   @NotNull
   private static String getDimensionalText(AABB bb) {
      return "";
   }

   @NotNull
   private static List<HoneyGlueEntity> getHoneyGlue(Player player) {
      return player.level()
         .getEntities(
            (EntityTypeTest)SimEntityTypes.HONEY_GLUE.get(),
            player.getBoundingBox().inflate((double)((Integer)SimConfigService.INSTANCE.server().assembly.honeyGlueRange.get()).intValue()),
            e -> true
         );
   }

   @Nullable
   public InteractionHand getHoneyGlueHand(Player player) {
      return player.getItemInHand(InteractionHand.MAIN_HAND).is(SimItems.HONEY_GLUE)
         ? InteractionHand.MAIN_HAND
         : (player.getItemInHand(InteractionHand.OFF_HAND).is(SimItems.HONEY_GLUE) ? InteractionHand.OFF_HAND : null);
   }

   private void clearAndSwing(Player player) {
      this.selectedPos = null;
      this.currentState = HoneyGlueClientHandler.State.UNBOUND;
      player.swing(InteractionHand.MAIN_HAND);
   }

   public static enum State {
      UNBOUND,
      BINDING;
   }
}

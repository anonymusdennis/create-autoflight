package com.simibubi.create.content.contraptions.glue;

import com.google.common.base.Objects;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.content.contraptions.chassis.AbstractChassisBlock;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.utility.RaycastHelper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class SuperGlueSelectionHandler {
   private static final int PASSIVE = 5083490;
   private static final int HIGHLIGHT = 6866310;
   private static final int FAIL = 12957000;
   private Object clusterOutlineSlot = new Object();
   private Object bbOutlineSlot = new Object();
   private int clusterCooldown;
   private BlockPos firstPos;
   private BlockPos hoveredPos;
   private Set<BlockPos> currentCluster;
   private int glueRequired;
   private SuperGlueEntity selected;
   private BlockPos soundSourceForRemoval;

   public void tick() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      BlockPos hovered = null;
      ItemStack stack = player.getMainHandItem();
      if (!this.isGlue(stack)) {
         if (this.firstPos != null) {
            this.discard();
         }
      } else {
         if (this.clusterCooldown > 0) {
            if (this.clusterCooldown == 25) {
               player.displayClientMessage(CommonComponents.EMPTY, true);
            }

            Outliner.getInstance().keep(this.clusterOutlineSlot);
            this.clusterCooldown--;
         }

         AABB scanArea = player.getBoundingBox().inflate(32.0, 16.0, 32.0);
         List<SuperGlueEntity> glueNearby = mc.level.getEntitiesOfClass(SuperGlueEntity.class, scanArea);
         this.selected = null;
         if (this.firstPos == null) {
            double range = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 1.0;
            Vec3 traceOrigin = player.getEyePosition();
            Vec3 traceTarget = RaycastHelper.getTraceTarget(player, range, traceOrigin);
            double bestDistance = Double.MAX_VALUE;

            for (SuperGlueEntity glueEntity : glueNearby) {
               Optional<Vec3> clip = glueEntity.getBoundingBox().clip(traceOrigin, traceTarget);
               if (!clip.isEmpty()) {
                  Vec3 vec3 = clip.get();
                  double distanceToSqr = vec3.distanceToSqr(traceOrigin);
                  if (!(distanceToSqr > bestDistance)) {
                     this.selected = glueEntity;
                     this.soundSourceForRemoval = BlockPos.containing(vec3);
                     bestDistance = distanceToSqr;
                  }
               }
            }

            for (SuperGlueEntity glueEntityx : glueNearby) {
               boolean h = this.clusterCooldown == 0 && glueEntityx == this.selected;
               AllSpecialTextures faceTex = h ? AllSpecialTextures.GLUE : null;
               Outliner.getInstance()
                  .showAABB(glueEntityx, glueEntityx.getBoundingBox())
                  .colored(h ? 6866310 : 5083490)
                  .withFaceTextures(faceTex, faceTex)
                  .disableLineNormals()
                  .lineWidth(h ? 0.0625F : 0.015625F);
            }
         }

         HitResult hitResult = mc.hitResult;
         if (hitResult != null && hitResult.getType() == Type.BLOCK) {
            hovered = ((BlockHitResult)hitResult).getBlockPos();
         }

         if (hovered == null) {
            this.hoveredPos = null;
         } else if (this.firstPos != null && !this.firstPos.closerThan(hovered, 24.0)) {
            CreateLang.translate("super_glue.too_far").color(12957000).sendStatus(player);
         } else {
            boolean cancel = player.isShiftKeyDown();
            if (!cancel || this.firstPos != null) {
               AABB currentSelectionBox = this.getCurrentSelectionBox();
               boolean unchanged = Objects.equal(hovered, this.hoveredPos);
               if (!unchanged) {
                  this.hoveredPos = hovered;
                  Set<BlockPos> cluster = SuperGlueSelectionHelper.searchGlueGroup(mc.level, this.firstPos, this.hoveredPos, true);
                  this.currentCluster = cluster;
                  this.glueRequired = 1;
               } else {
                  if (this.currentCluster != null) {
                     boolean canReach = this.currentCluster.contains(hovered);
                     boolean canAfford = SuperGlueSelectionHelper.collectGlueFromInventory(player, this.glueRequired, true);
                     int color = 6866310;
                     String key = "super_glue.click_to_confirm";
                     if (!canReach) {
                        color = 12957000;
                        key = "super_glue.cannot_reach";
                     } else if (!canAfford) {
                        color = 12957000;
                        key = "super_glue.not_enough";
                     } else if (cancel) {
                        color = 12957000;
                        key = "super_glue.click_to_discard";
                     }

                     CreateLang.translate(key).color(color).sendStatus(player);
                     if (currentSelectionBox != null) {
                        Outliner.getInstance()
                           .showAABB(this.bbOutlineSlot, currentSelectionBox)
                           .colored(canReach && canAfford && !cancel ? 6866310 : 12957000)
                           .withFaceTextures(AllSpecialTextures.GLUE, AllSpecialTextures.GLUE)
                           .disableLineNormals()
                           .lineWidth(0.0625F);
                     }

                     Outliner.getInstance()
                        .showCluster(this.clusterOutlineSlot, this.currentCluster)
                        .colored(5083490)
                        .disableLineNormals()
                        .lineWidth(0.015625F);
                  }
               }
            }
         }
      }
   }

   private boolean isGlue(ItemStack stack) {
      return stack.getItem() instanceof SuperGlueItem;
   }

   private AABB getCurrentSelectionBox() {
      return this.firstPos != null && this.hoveredPos != null
         ? new AABB(Vec3.atLowerCornerOf(this.firstPos), Vec3.atLowerCornerOf(this.hoveredPos)).expandTowards(1.0, 1.0, 1.0)
         : null;
   }

   public boolean onMouseInput(boolean attack) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      ClientLevel level = mc.level;
      if (!this.isGlue(player.getMainHandItem())) {
         return false;
      } else if (!player.mayBuild()) {
         return false;
      } else if (attack) {
         if (this.selected == null) {
            return false;
         } else {
            CatnipServices.NETWORK.sendToServer(new SuperGlueRemovalPacket(this.selected.getId(), this.soundSourceForRemoval));
            this.selected = null;
            this.clusterCooldown = 0;
            return true;
         }
      } else if (player.isShiftKeyDown()) {
         if (this.firstPos != null) {
            this.discard();
            return true;
         } else {
            return false;
         }
      } else if (this.hoveredPos == null) {
         return false;
      } else {
         Direction face = null;
         if (mc.hitResult instanceof BlockHitResult bhr) {
            face = bhr.getDirection();
            BlockState blockState = level.getBlockState(this.hoveredPos);
            if (blockState.getBlock() instanceof AbstractChassisBlock cb && cb.getGlueableSide(blockState, bhr.getDirection()) != null) {
               return false;
            }
         }

         if (this.firstPos != null && this.currentCluster != null) {
            boolean canReach = this.currentCluster.contains(this.hoveredPos);
            boolean canAfford = SuperGlueSelectionHelper.collectGlueFromInventory(player, this.glueRequired, true);
            if (canReach && canAfford) {
               this.confirm();
               return true;
            } else {
               return true;
            }
         } else {
            this.firstPos = this.hoveredPos;
            if (face != null) {
               SuperGlueItem.spawnParticles(level, this.firstPos, face, true);
            }

            CreateLang.translate("super_glue.first_pos").sendStatus(player);
            AllSoundEvents.SLIME_ADDED.playAt(level, this.firstPos, 0.5F, 0.85F, false);
            level.playSound(player, this.firstPos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.75F, 1.0F);
            return true;
         }
      }
   }

   public void discard() {
      LocalPlayer player = Minecraft.getInstance().player;
      this.currentCluster = null;
      this.firstPos = null;
      CreateLang.translate("super_glue.abort").sendStatus(player);
      this.clusterCooldown = 0;
   }

   public void confirm() {
      LocalPlayer player = Minecraft.getInstance().player;
      CatnipServices.NETWORK.sendToServer(new SuperGlueSelectionPacket(this.firstPos, this.hoveredPos));
      AllSoundEvents.SLIME_ADDED.playAt(player.level(), this.hoveredPos, 0.5F, 0.95F, false);
      player.level().playSound(player, this.hoveredPos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.75F, 1.0F);
      if (this.currentCluster != null) {
         Outliner.getInstance()
            .showCluster(this.clusterOutlineSlot, this.currentCluster)
            .colored(11924166)
            .withFaceTextures(AllSpecialTextures.GLUE, AllSpecialTextures.HIGHLIGHT_CHECKERED)
            .disableLineNormals()
            .lineWidth(0.041666668F);
      }

      this.discard();
      CreateLang.translate("super_glue.success").sendStatus(player);
      this.clusterCooldown = 40;
   }
}

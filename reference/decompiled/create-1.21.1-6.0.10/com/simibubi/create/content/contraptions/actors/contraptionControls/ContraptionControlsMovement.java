package com.simibubi.create.content.contraptions.actors.contraptionControls;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.elevator.ElevatorContraption;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.IntAttached;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ContraptionControlsMovement implements MovementBehaviour {
   @Override
   public ItemStack canBeDisabledVia(MovementContext context) {
      return null;
   }

   @Override
   public void startMoving(MovementContext context) {
      if (context.contraption instanceof ElevatorContraption && context.blockEntityData != null) {
         context.blockEntityData.remove("Filter");
      }
   }

   @Override
   public void stopMoving(MovementContext context) {
      ItemStack filter = getFilter(context);
      if (filter != null) {
         context.blockEntityData
            .putBoolean("Disabled", context.contraption.isActorTypeDisabled(filter) || context.contraption.isActorTypeDisabled(ItemStack.EMPTY));
      }
   }

   public static boolean isSameFilter(ItemStack stack1, ItemStack stack2) {
      return stack1.isEmpty() && stack2.isEmpty() ? true : ItemStack.isSameItemSameComponents(stack1, stack2);
   }

   public static ItemStack getFilter(MovementContext ctx) {
      CompoundTag blockEntityData = ctx.blockEntityData;
      return blockEntityData == null ? null : ItemStack.parseOptional(ctx.world.registryAccess(), blockEntityData.getCompound("Filter"));
   }

   public static boolean isDisabledInitially(MovementContext ctx) {
      return ctx.blockEntityData != null && ctx.blockEntityData.getBoolean("Disabled");
   }

   @Override
   public void tick(MovementContext ctx) {
      if (ctx.world.isClientSide()) {
         Contraption contraption = ctx.contraption;
         BlockEntity blockEntity = contraption.getBlockEntityClientSide(ctx.localPos);
         if (contraption instanceof ElevatorContraption ec) {
            if (!(ctx.temporaryData instanceof ContraptionControlsMovement.ElevatorFloorSelection)) {
               ctx.temporaryData = new ContraptionControlsMovement.ElevatorFloorSelection();
            }

            ContraptionControlsMovement.ElevatorFloorSelection efs = (ContraptionControlsMovement.ElevatorFloorSelection)ctx.temporaryData;
            tickFloorSelection(efs, ec);
            if (blockEntity instanceof ContraptionControlsBlockEntity cbe) {
               cbe.tickAnimations();
               int var16 = (int)Math.round(contraption.entity.getY() + (double)ec.getContactYOffset());
               boolean atTargetY = ec.clientYTarget == var16;
               LerpedFloat indicator = cbe.indicator;
               float currentIndicator = indicator.getChaseTarget();
               boolean below = atTargetY ? currentIndicator > 0.0F : ec.clientYTarget <= var16;
               if (currentIndicator == 0.0F && !atTargetY) {
                  int startingPoint = below ? 181 : -181;
                  indicator.setValue((double)startingPoint);
                  indicator.updateChaseTarget((float)startingPoint);
                  cbe.tickAnimations();
               } else {
                  int currentStage = Mth.floor((currentIndicator % 360.0F + 360.0F) % 360.0F);
                  if (atTargetY && currentStage / 45 == 0) {
                     indicator.setValue(0.0);
                     indicator.updateChaseTarget(0.0F);
                  } else {
                     float increment = currentStage / 45 == (below ? 4 : 3) ? 2.25F : 33.75F;
                     indicator.chase((double)(currentIndicator + (below ? increment : -increment)), 45.0, Chaser.LINEAR);
                  }
               }
            }
         } else if (blockEntity instanceof ContraptionControlsBlockEntity cbex) {
            ItemStack cbexx = getFilter(ctx);
            int value = !contraption.isActorTypeDisabled(cbexx) && !contraption.isActorTypeDisabled(ItemStack.EMPTY) ? 0 : 180;
            cbex.indicator.setValue((double)value);
            cbex.indicator.updateChaseTarget((float)value);
            cbex.tickAnimations();
         }
      }
   }

   public static void tickFloorSelection(ContraptionControlsMovement.ElevatorFloorSelection efs, ElevatorContraption ec) {
      if (ec.namesList.isEmpty()) {
         efs.currentShortName = "X";
         efs.currentLongName = "No Floors";
         efs.currentIndex = 0;
         efs.targetYEqualsSelection = true;
      } else {
         efs.currentIndex = Mth.clamp(efs.currentIndex, 0, ec.namesList.size() - 1);
         IntAttached<Couple<String>> entry = ec.namesList.get(efs.currentIndex);
         efs.currentTargetY = (Integer)entry.getFirst();
         efs.currentShortName = (String)((Couple)entry.getSecond()).getFirst();
         efs.currentLongName = (String)((Couple)entry.getSecond()).getSecond();
         efs.targetYEqualsSelection = efs.currentTargetY == ec.clientYTarget;
         if (ec.isTargetUnreachable(efs.currentTargetY)) {
            efs.currentLongName = CreateLang.translate("contraption.controls.floor_unreachable").string();
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void renderInContraption(MovementContext ctx, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffer) {
      ContraptionControlsRenderer.renderInContraption(ctx, renderWorld, matrices, buffer);
   }

   public static class ElevatorFloorSelection {
      public int currentIndex = 0;
      public int currentTargetY = 0;
      public boolean targetYEqualsSelection = true;
      public String currentShortName = "";
      public String currentLongName = "";
   }
}

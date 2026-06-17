package com.simibubi.create.foundation.blockEntity.behaviour;

import com.simibubi.create.AllBlocks;
import java.util.List;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;

public class ValueSettingsClient implements Layer {
   private Minecraft mc;
   public int interactHeldTicks = -1;
   public BlockPos interactHeldPos = null;
   public BehaviourType<?> interactHeldBehaviour = null;
   public InteractionHand interactHeldHand = null;
   public Direction interactHeldFace = null;
   public List<MutableComponent> lastHoverTip;
   public int hoverTicks;
   public int hoverWarmup;

   public ValueSettingsClient() {
      this.mc = Minecraft.getInstance();
   }

   public void cancelIfWarmupAlreadyStarted(RightClickBlock event) {
      if (this.interactHeldTicks != -1 && event.getPos().equals(this.interactHeldPos)) {
         event.setCanceled(true);
         event.setCancellationResult(InteractionResult.FAIL);
      }
   }

   public void startInteractionWith(BlockPos pos, BehaviourType<?> behaviourType, InteractionHand hand, Direction side) {
      this.interactHeldTicks = 0;
      this.interactHeldPos = pos;
      this.interactHeldBehaviour = behaviourType;
      this.interactHeldHand = hand;
      this.interactHeldFace = side;
   }

   public void cancelInteraction() {
      this.interactHeldTicks = -1;
   }

   public void tick() {
      if (this.hoverWarmup > 0) {
         this.hoverWarmup--;
      }

      if (this.hoverTicks > 0) {
         this.hoverTicks--;
      }

      if (this.interactHeldTicks != -1) {
         Player player = this.mc.player;
         if (ValueSettingsInputHandler.canInteract(player) && !AllBlocks.CLIPBOARD.isIn(player.getMainHandItem())) {
            if (!(this.mc.hitResult instanceof BlockHitResult blockHitResult) || !blockHitResult.getBlockPos().equals(this.interactHeldPos)) {
               this.cancelInteraction();
               return;
            }

            if (!(
                  BlockEntityBehaviour.get(this.mc.level, this.interactHeldPos, this.interactHeldBehaviour) instanceof ValueSettingsBehaviour valueSettingBehaviour
               )
               || valueSettingBehaviour.bypassesInput(player.getMainHandItem())
               || !valueSettingBehaviour.testHit(blockHitResult.getLocation())) {
               this.cancelInteraction();
               return;
            }

            if (!this.mc.options.keyUse.isDown()) {
               CatnipServices.NETWORK
                  .sendToServer(
                     new ValueSettingsPacket(
                        this.interactHeldPos, 0, 0, this.interactHeldHand, blockHitResult, this.interactHeldFace, false, valueSettingBehaviour.netId()
                     )
                  );
               valueSettingBehaviour.onShortInteract(player, this.interactHeldHand, this.interactHeldFace, blockHitResult);
               this.cancelInteraction();
            } else {
               if (this.interactHeldTicks > 3) {
                  player.swinging = false;
               }

               if (this.interactHeldTicks++ >= 5) {
                  ScreenOpener.open(
                     new ValueSettingsScreen(
                        this.interactHeldPos,
                        valueSettingBehaviour.createBoard(player, blockHitResult),
                        valueSettingBehaviour.getValueSettings(),
                        valueSettingBehaviour::newSettingHovered,
                        valueSettingBehaviour.netId()
                     )
                  );
                  this.interactHeldTicks = -1;
               }
            }
         } else {
            this.cancelInteraction();
         }
      }
   }

   public void showHoverTip(List<MutableComponent> tip) {
      if (this.mc.screen == null) {
         if (this.hoverWarmup < 6) {
            this.hoverWarmup += 2;
         } else {
            this.hoverWarmup++;
            this.hoverTicks = this.hoverTicks == 0 ? 11 : Math.max(this.hoverTicks, 6);
            this.lastHoverTip = tip;
         }
      }
   }

   public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
      Minecraft mc = Minecraft.getInstance();
      if (!mc.options.hideGui && ValueSettingsInputHandler.canInteract(mc.player)) {
         if (this.hoverTicks != 0 && this.lastHoverTip != null) {
            int x = guiGraphics.guiWidth() / 2;
            int y = guiGraphics.guiHeight() - 75 - this.lastHoverTip.size() * 12;
            float alpha = this.hoverTicks > 5 ? (float)(11 - this.hoverTicks) / 5.0F : Math.min(1.0F, (float)this.hoverTicks / 5.0F);
            Color color = new Color(16777215);
            Color titleColor = new Color(16505981);
            color.setAlpha(alpha);
            titleColor.setAlpha(alpha);

            for (int i = 0; i < this.lastHoverTip.size(); i++) {
               MutableComponent mutableComponent = this.lastHoverTip.get(i);
               guiGraphics.drawString(mc.font, mutableComponent, x - mc.font.width(mutableComponent) / 2, y, (i == 0 ? titleColor : color).getRGB());
               y += 12;
            }
         }
      }
   }
}

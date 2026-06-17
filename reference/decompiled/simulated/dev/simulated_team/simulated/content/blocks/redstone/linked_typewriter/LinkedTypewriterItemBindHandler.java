package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.simulated_team.simulated.content.blocks.redstone.AbstractLinkedReceiverBlockEntity;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.mixin.accessor.RedstoneLinkBlockEntityAccessor;
import dev.simulated_team.simulated.network.packets.linked_typewriter.TypewriterSaveKeyToItemPacket;
import dev.simulated_team.simulated.util.SimColors;
import foundry.veil.api.network.VeilPacketManager;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LinkedTypewriterItemBindHandler {
   public static final Layer OVERLAY = LinkedTypewriterItemBindHandler::renderOverlay;
   private static BlockPos clickedPos;
   private static final List<AABB> outlines = new ArrayList<>();
   private static boolean firstTick = false;

   public static void setClickedPos(BlockPos pos) {
      clickedPos = pos;
      if (pos != null) {
         firstTick = true;
      } else {
         reset();
      }
   }

   public static void tick() {
      ClientLevel level = Minecraft.getInstance().level;
      LocalPlayer player = Minecraft.getInstance().player;
      ItemStack mainHandItem = player.getMainHandItem();
      ItemStack offhandItem = player.getOffhandItem();
      if (Minecraft.getInstance().screen == null
         && (mainHandItem.getItem() instanceof LinkedTypewriterItem || offhandItem.getItem() instanceof LinkedTypewriterItem)) {
         if (firstTick || level.getGameTime() % 5L == 0L) {
            firstTick = false;
            outlines.clear();
            Couple<Frequency> frequencies = isPosValid(level);
            if (frequencies == null) {
               reset();
            } else {
               BlockState state = level.getBlockState(clickedPos);
               VoxelShape collisionShape = state.getShape(level, clickedPos);
               if (!collisionShape.isEmpty()) {
                  outlines.addAll(collisionShape.toAabbs());
               }
            }
         }

         if (clickedPos != null) {
            for (AABB outline : outlines) {
               Outliner.getInstance()
                  .showAABB("linked_typewriter_outliner" + clickedPos + outline, outline.move(clickedPos))
                  .colored(SimColors.GROSS_BINDING_BROWN)
                  .lineWidth(0.0625F);
            }
         }
      } else {
         reset();
      }
   }

   public static void keyPress(int key, int scanCode, int action, int modifiers) {
      ClientLevel level = Minecraft.getInstance().level;
      Couple<Frequency> frequency = isPosValid(level);
      if (frequency == null) {
         reset();
      } else {
         if (key != 256) {
            InteractionHand hand = getHand();
            if (hand != null) {
               VeilPacketManager.server()
                  .sendPacket(
                     new CustomPacketPayload[]{
                        new TypewriterSaveKeyToItemPacket(
                           hand,
                           new LinkedTypewriterEntries.KeyboardEntry((Frequency)frequency.getFirst(), (Frequency)frequency.getSecond(), key, BlockPos.ZERO)
                        )
                     }
                  );
               LinkedTypewriterInteractionHandler.preventPress(key, scanCode);
               SimLang.builder()
                  .translate("linked_typewriter.bind_success", new Object[]{InputConstants.getKey(key, scanCode).getDisplayName().getString()})
                  .sendStatus(Minecraft.getInstance().player);
            }
         }

         reset();
      }
   }

   public static void renderOverlay(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
      if (LinkedTypewriterInteractionHandler.getMode() == LinkedTypewriterInteractionHandler.Mode.BINDING_FROM_ITEM) {
         Minecraft mc = Minecraft.getInstance();
         if (!mc.options.hideGui) {
            guiGraphics.pose().pushPose();
            List<Component> list = new ArrayList<>();
            list.add(CreateLang.translateDirect("linked_controller.bind_mode", new Object[0]).withStyle(ChatFormatting.GOLD));
            MutableComponent component = SimLang.translate("linked_typewriter.bind_item").component();
            list.addAll(TooltipHelper.cutTextComponent(component, Palette.ALL_GRAY));
            int width = 0;
            int height = list.size() * 9;

            for (Component iTextComponent : list) {
               width = Math.max(width, mc.font.width(iTextComponent));
            }

            int x = guiGraphics.guiWidth() / 3 - width / 2;
            int y = guiGraphics.guiHeight() - height - 24;
            guiGraphics.renderComponentTooltip(Minecraft.getInstance().font, list, x, y);
            guiGraphics.pose().popPose();
         }
      }
   }

   private static Couple<Frequency> isPosValid(Level level) {
      Couple<Frequency> frequency = null;
      if (clickedPos != null) {
         BlockEntity be = level.getBlockEntity(clickedPos);
         if (be instanceof AbstractLinkedReceiverBlockEntity abe) {
            frequency = abe.getFrequency();
         }

         if (be instanceof RedstoneLinkBlockEntity lbe) {
            frequency = ((RedstoneLinkBlockEntityAccessor)lbe).getLink().getNetworkKey();
         }
      }

      return frequency;
   }

   private static InteractionHand getHand() {
      LocalPlayer player = Minecraft.getInstance().player;
      Item item = SimBlocks.LINKED_TYPEWRITER.asItem();
      if (player.getMainHandItem().is(item)) {
         return InteractionHand.MAIN_HAND;
      } else {
         return player.getOffhandItem().is(item) ? InteractionHand.OFF_HAND : null;
      }
   }

   public static void reset() {
      outlines.clear();
      LinkedTypewriterInteractionHandler.setMode(LinkedTypewriterInteractionHandler.Mode.IDLE);
      clickedPos = null;
   }
}

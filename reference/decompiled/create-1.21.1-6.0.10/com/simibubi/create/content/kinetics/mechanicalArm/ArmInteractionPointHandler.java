package com.simibubi.create.content.kinetics.mechanicalArm;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;

@EventBusSubscriber({Dist.CLIENT})
public class ArmInteractionPointHandler {
   static List<ArmInteractionPoint> currentSelection = new ArrayList<>();
   static ItemStack currentItem;
   static long lastBlockPos = -1L;

   @SubscribeEvent
   public static void rightClickingBlocksSelectsThem(RightClickBlock event) {
      if (currentItem != null) {
         BlockPos pos = event.getPos();
         Level world = event.getLevel();
         if (world.isClientSide) {
            Player player = event.getEntity();
            if (player == null || !player.isSpectator()) {
               ArmInteractionPoint selected = getSelected(pos);
               BlockState state = world.getBlockState(pos);
               if (selected == null) {
                  ArmInteractionPoint point = ArmInteractionPoint.create(world, pos, state);
                  if (point == null) {
                     return;
                  }

                  selected = point;
                  put(point);
               }

               selected.cycleMode();
               if (player != null) {
                  ArmInteractionPoint.Mode mode = selected.getMode();
                  CreateLang.builder()
                     .translate(mode.getTranslationKey(), new Object[]{CreateLang.blockName(state).style(ChatFormatting.WHITE)})
                     .color(mode.getColor())
                     .sendStatus(player);
               }

               event.setCanceled(true);
               event.setCancellationResult(InteractionResult.SUCCESS);
            }
         }
      }
   }

   @SubscribeEvent
   public static void leftClickingBlocksDeselectsThem(LeftClickBlock event) {
      if (currentItem != null) {
         if (event.getLevel().isClientSide) {
            BlockPos pos = event.getPos();
            if (remove(pos) != null) {
               event.setCanceled(true);
            }
         }
      }
   }

   public static void flushSettings(BlockPos pos) {
      if (currentSelection != null) {
         int removed = 0;
         Iterator<ArmInteractionPoint> iterator = currentSelection.iterator();

         while (iterator.hasNext()) {
            ArmInteractionPoint point = iterator.next();
            if (!point.getPos().closerThan(pos, (double)ArmBlockEntity.getRange())) {
               iterator.remove();
               removed++;
            }
         }

         LocalPlayer player = Minecraft.getInstance().player;
         if (removed > 0) {
            CreateLang.builder().translate("mechanical_arm.points_outside_range", new Object[]{removed}).style(ChatFormatting.RED).sendStatus(player);
         } else {
            int inputs = 0;
            int outputs = 0;

            for (ArmInteractionPoint armInteractionPoint : currentSelection) {
               if (armInteractionPoint.getMode() == ArmInteractionPoint.Mode.DEPOSIT) {
                  outputs++;
               } else {
                  inputs++;
               }
            }

            if (inputs + outputs > 0) {
               CreateLang.builder().translate("mechanical_arm.summary", new Object[]{inputs, outputs}).style(ChatFormatting.WHITE).sendStatus(player);
            }
         }

         CatnipServices.NETWORK.sendToServer(new ArmPlacementPacket(currentSelection, pos));
         currentSelection.clear();
         currentItem = null;
      }
   }

   public static void tick() {
      Player player = Minecraft.getInstance().player;
      if (player != null) {
         ItemStack heldItemMainhand = player.getMainHandItem();
         if (!AllBlocks.MECHANICAL_ARM.isIn(heldItemMainhand)) {
            currentItem = null;
         } else {
            if (heldItemMainhand != currentItem) {
               currentSelection.clear();
               currentItem = heldItemMainhand;
            }

            drawOutlines(currentSelection);
         }

         checkForWrench(heldItemMainhand);
      }
   }

   private static void checkForWrench(ItemStack heldItem) {
      if (AllItems.WRENCH.isIn(heldItem)) {
         if (Minecraft.getInstance().hitResult instanceof BlockHitResult result) {
            BlockPos pos = result.getBlockPos();
            BlockEntity be = Minecraft.getInstance().level.getBlockEntity(pos);
            if (!(be instanceof ArmBlockEntity)) {
               lastBlockPos = -1L;
               currentSelection.clear();
            } else {
               if (lastBlockPos == -1L || lastBlockPos != pos.asLong()) {
                  currentSelection.clear();
                  ArmBlockEntity arm = (ArmBlockEntity)be;
                  arm.inputs.forEach(ArmInteractionPointHandler::put);
                  arm.outputs.forEach(ArmInteractionPointHandler::put);
                  lastBlockPos = pos.asLong();
               }

               if (lastBlockPos != -1L) {
                  drawOutlines(currentSelection);
               }
            }
         }
      }
   }

   private static void drawOutlines(Collection<ArmInteractionPoint> selection) {
      Iterator<ArmInteractionPoint> iterator = selection.iterator();

      while (iterator.hasNext()) {
         ArmInteractionPoint point = iterator.next();
         if (!point.isValid()) {
            iterator.remove();
         } else {
            Level level = point.getLevel();
            BlockPos pos = point.getPos();
            BlockState state = level.getBlockState(pos);
            VoxelShape shape = state.getShape(level, pos);
            if (!shape.isEmpty()) {
               int color = point.getMode().getColor();
               Outliner.getInstance().showAABB(point, shape.bounds().move(pos)).colored(color).lineWidth(0.0625F);
            }
         }
      }
   }

   private static void put(ArmInteractionPoint point) {
      currentSelection.add(point);
   }

   private static ArmInteractionPoint remove(BlockPos pos) {
      ArmInteractionPoint result = getSelected(pos);
      if (result != null) {
         currentSelection.remove(result);
      }

      return result;
   }

   private static ArmInteractionPoint getSelected(BlockPos pos) {
      for (ArmInteractionPoint point : currentSelection) {
         if (point.getPos().equals(pos)) {
            return point;
         }
      }

      return null;
   }
}

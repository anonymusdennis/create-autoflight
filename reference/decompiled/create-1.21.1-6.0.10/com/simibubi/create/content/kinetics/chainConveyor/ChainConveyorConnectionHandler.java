package com.simibubi.create.content.kinetics.chainConveyor;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.blueprint.BlueprintOverlayRenderer;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;

@EventBusSubscriber({Dist.CLIENT})
public class ChainConveyorConnectionHandler {
   private static BlockPos firstPos;
   private static ResourceKey<Level> firstDim;

   public static boolean onRightClick() {
      Minecraft mc = Minecraft.getInstance();
      if (!isChain(mc.player.getMainHandItem())) {
         return false;
      } else if (firstPos == null) {
         return false;
      } else {
         boolean missed = false;
         if (mc.hitResult instanceof BlockHitResult bhr
            && bhr.getType() != Type.MISS
            && !(mc.level.getBlockEntity(bhr.getBlockPos()) instanceof ChainConveyorBlockEntity)) {
            missed = true;
         }

         if (!mc.player.isShiftKeyDown() && !missed) {
            return false;
         } else {
            firstPos = null;
            CreateLang.translate("chain_conveyor.selection_cleared").sendStatus(mc.player);
            return true;
         }
      }
   }

   @SubscribeEvent
   public static void onItemUsedOnBlock(RightClickBlock event) {
      ItemStack itemStack = event.getItemStack();
      BlockPos pos = event.getPos();
      Level level = event.getLevel();
      Player player = event.getEntity();
      BlockState blockState = level.getBlockState(pos);
      if (AllBlocks.CHAIN_CONVEYOR.has(blockState)) {
         if (isChain(itemStack)) {
            if (player.mayBuild() && !(player instanceof FakePlayer)) {
               event.setCanceled(true);
               event.setCancellationResult(InteractionResult.CONSUME);
               if (level.isClientSide()) {
                  if (level.getBlockEntity(pos) instanceof ChainConveyorBlockEntity ccbe
                     && ccbe.connections.size() >= (Integer)AllConfigs.server().kinetics.maxChainConveyorConnections.get()) {
                     CreateLang.translate("chain_conveyor.cannot_add_more_connections").style(ChatFormatting.RED).sendStatus(player);
                     return;
                  }

                  if (firstPos != null && firstDim == level.dimension()) {
                     boolean success = validateAndConnect(level, pos, player, itemStack, false);
                     firstPos = null;
                     if (!success) {
                        AllSoundEvents.DENY.play(level, player, pos);
                     } else {
                        SoundType soundtype = Blocks.CHAIN.defaultBlockState().getSoundType();
                        if (soundtype != null) {
                           level.playSound(
                              player, pos, soundtype.getPlaceSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F
                           );
                        }
                     }
                  } else {
                     firstPos = pos;
                     firstDim = level.dimension();
                     player.swing(event.getHand());
                  }
               }
            }
         }
      }
   }

   private static boolean isChain(ItemStack itemStack) {
      return itemStack.is(Items.CHAIN);
   }

   public static void clientTick() {
      if (firstPos != null) {
         LocalPlayer player = Minecraft.getInstance().player;
         BlockEntity sourceLift = player.level().getBlockEntity(firstPos);
         if (firstDim == player.level().dimension() && sourceLift instanceof ChainConveyorBlockEntity) {
            ItemStack stack = player.getMainHandItem();
            HitResult hitResult = Minecraft.getInstance().hitResult;
            if (!isChain(stack)) {
               stack = player.getOffhandItem();
               if (!isChain(stack)) {
                  return;
               }
            }

            if (hitResult != null && hitResult.getType() == Type.BLOCK) {
               Level level = player.level();
               BlockHitResult bhr = (BlockHitResult)hitResult;
               BlockPos pos = bhr.getBlockPos();
               BlockState hitState = level.getBlockState(pos);
               if (pos.equals(firstPos)) {
                  highlightConveyor(firstPos, 16777215, "chain_connect");
                  CreateLang.translate("chain_conveyor.select_second").sendStatus(player);
               } else if (!(hitState.getBlock() instanceof ChainConveyorBlock)) {
                  highlightConveyor(firstPos, 16777215, "chain_connect");
               } else {
                  boolean success = validateAndConnect(level, pos, player, stack, true);
                  if (success) {
                     CreateLang.translate("chain_conveyor.valid_connection").style(ChatFormatting.GREEN).sendStatus(player);
                  }

                  int color = success ? 9817409 : 15359019;
                  highlightConveyor(firstPos, color, "chain_connect");
                  highlightConveyor(pos, color, "chain_connect_to");
                  Vec3 from = Vec3.atCenterOf(pos);
                  Vec3 to = Vec3.atCenterOf(firstPos);
                  Vec3 diff = from.subtract(to);
                  if (!(diff.length() < 1.0)) {
                     from = from.subtract(diff.normalize().scale(0.5));
                     to = to.add(diff.normalize().scale(0.5));
                     Vec3 normal = diff.cross(new Vec3(0.0, 1.0, 0.0)).normalize().scale(0.875);
                     Outliner.getInstance().showLine("chain_connect_line", from.add(normal), to.add(normal)).lineWidth(0.0625F).colored(color);
                     Outliner.getInstance().showLine("chain_connect_line_1", from.subtract(normal), to.subtract(normal)).lineWidth(0.0625F).colored(color);
                  }
               }
            } else {
               highlightConveyor(firstPos, 16777215, "chain_connect");
            }
         } else {
            firstPos = null;
            CreateLang.translate("chain_conveyor.selection_cleared").sendStatus(player);
         }
      }
   }

   private static void highlightConveyor(BlockPos pos, int color, String key) {
      for (int y : Iterate.zeroAndOne) {
         Vec3 prevV = VecHelper.rotate(new Vec3(0.0, 0.125 + (double)y * 0.75, 1.25), -22.5, Axis.Y).add(Vec3.atBottomCenterOf(pos));

         for (int i = 0; i < 8; i++) {
            Vec3 v = VecHelper.rotate(new Vec3(0.0, 0.125 + (double)y * 0.75, 1.25), 22.5 + (double)(i * 45), Axis.Y).add(Vec3.atBottomCenterOf(pos));
            Outliner.getInstance().showLine(key + y + i, prevV, v).lineWidth(0.0625F).colored(color);
            prevV = v;
         }
      }
   }

   public static boolean validateAndConnect(LevelAccessor level, BlockPos pos, Player player, ItemStack chain, boolean simulate) {
      if (!simulate && player.isShiftKeyDown()) {
         CreateLang.translate("chain_conveyor.selection_cleared").sendStatus(player);
         return false;
      } else if (pos.equals(firstPos)) {
         return false;
      } else if (!pos.closerThan(firstPos, (double)((Integer)AllConfigs.server().kinetics.maxChainConveyorLength.get()).intValue())) {
         return fail("chain_conveyor.too_far");
      } else if (pos.closerThan(firstPos, 2.5)) {
         return fail("chain_conveyor.too_close");
      } else {
         Vec3 diff = Vec3.atLowerCornerOf(pos.subtract(firstPos));
         double horizontalDistance = diff.multiply(1.0, 0.0, 1.0).length() - 1.5;
         if (horizontalDistance <= 0.0) {
            return fail("chain_conveyor.cannot_connect_vertically");
         } else if (Math.abs(diff.y) / horizontalDistance > 1.0) {
            return fail("chain_conveyor.too_steep");
         } else {
            ChainConveyorBlock chainConveyorBlock = (ChainConveyorBlock)AllBlocks.CHAIN_CONVEYOR.get();
            ChainConveyorBlockEntity sourceLift = chainConveyorBlock.getBlockEntity(level, firstPos);
            ChainConveyorBlockEntity targetLift = chainConveyorBlock.getBlockEntity(level, pos);
            if (targetLift.connections.size() >= (Integer)AllConfigs.server().kinetics.maxChainConveyorConnections.get()) {
               return fail("chain_conveyor.cannot_add_more_connections");
            } else if (targetLift.connections.contains(firstPos.subtract(pos))) {
               return fail("chain_conveyor.already_connected");
            } else if (sourceLift != null && targetLift != null) {
               if (!player.isCreative()) {
                  int chainCost = ChainConveyorBlockEntity.getChainCost(pos.subtract(firstPos));
                  boolean hasEnough = ChainConveyorBlockEntity.getChainsFromInventory(player, chain, chainCost, true);
                  if (simulate) {
                     BlueprintOverlayRenderer.displayChainRequirements(chain.getItem(), chainCost, hasEnough);
                  }

                  if (!hasEnough) {
                     return fail("chain_conveyor.not_enough_chains");
                  }
               }

               if (simulate) {
                  return true;
               } else {
                  CatnipServices.NETWORK.sendToServer(new ChainConveyorConnectionPacket(firstPos, pos, chain, true));
                  CreateLang.text("").sendStatus(player);
                  firstPos = null;
                  firstDim = null;
                  return true;
               }
            } else {
               return fail("chain_conveyor.blocks_invalid");
            }
         }
      }
   }

   private static boolean fail(String message) {
      CreateLang.translate(message).style(ChatFormatting.RED).sendStatus(Minecraft.getInstance().player);
      return false;
   }
}

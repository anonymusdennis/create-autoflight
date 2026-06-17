package com.simibubi.create.content.equipment.symmetryWand;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlock;
import com.simibubi.create.content.equipment.symmetryWand.mirror.CrossPlaneMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.EmptyMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.PlaneMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.SymmetryMirror;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;

public class SymmetryWandItem extends Item {
   public SymmetryWandItem(Properties properties) {
      super(properties);
   }

   @NotNull
   public InteractionResult useOn(UseOnContext context) {
      Player player = context.getPlayer();
      BlockPos pos = context.getClickedPos();
      if (player == null) {
         return InteractionResult.PASS;
      } else {
         player.getCooldowns().addCooldown(this, 5);
         ItemStack wand = player.getItemInHand(context.getHand());
         checkComponents(wand);
         if (player.isShiftKeyDown()) {
            if (player.level().isClientSide) {
               CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> this.openWandGUI(wand, context.getHand()));
               player.getCooldowns().addCooldown(this, 5);
            }

            return InteractionResult.SUCCESS;
         } else if (!context.getLevel().isClientSide && context.getHand() == InteractionHand.MAIN_HAND) {
            pos = pos.relative(context.getClickedFace());
            SymmetryMirror previousElement = (SymmetryMirror)wand.get(AllDataComponents.SYMMETRY_WAND);
            wand.set(AllDataComponents.SYMMETRY_WAND_ENABLE, true);
            Vec3 pos3d = new Vec3((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
            SymmetryMirror newElement = new PlaneMirror(pos3d);
            if (previousElement instanceof EmptyMirror) {
               newElement.setOrientation(
                  player.getDirection() != Direction.NORTH && player.getDirection() != Direction.SOUTH
                     ? PlaneMirror.Align.YZ.ordinal()
                     : PlaneMirror.Align.XY.ordinal()
               );
               newElement.enable = true;
               wand.set(AllDataComponents.SYMMETRY_WAND_ENABLE, true);
            } else {
               previousElement.setPosition(pos3d);
               if (previousElement instanceof PlaneMirror) {
                  previousElement.setOrientation(
                     player.getDirection() != Direction.NORTH && player.getDirection() != Direction.SOUTH
                        ? PlaneMirror.Align.YZ.ordinal()
                        : PlaneMirror.Align.XY.ordinal()
                  );
               }

               if (previousElement instanceof CrossPlaneMirror) {
                  float rotation = player.getYHeadRot();
                  float abs = Math.abs(rotation % 90.0F);
                  boolean diagonal = abs > 22.0F && abs < 67.0F;
                  previousElement.setOrientation(diagonal ? CrossPlaneMirror.Align.D.ordinal() : CrossPlaneMirror.Align.Y.ordinal());
               }

               newElement = previousElement;
            }

            wand.set(AllDataComponents.SYMMETRY_WAND, newElement);
            player.setItemInHand(context.getHand(), wand);
            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.SUCCESS;
         }
      }
   }

   public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
      ItemStack wand = playerIn.getItemInHand(handIn);
      checkComponents(wand);
      if (playerIn.isShiftKeyDown()) {
         if (worldIn.isClientSide) {
            CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> this.openWandGUI(playerIn.getItemInHand(handIn), handIn));
            playerIn.getCooldowns().addCooldown(this, 5);
         }

         return new InteractionResultHolder(InteractionResult.SUCCESS, wand);
      } else {
         wand.set(AllDataComponents.SYMMETRY_WAND_ENABLE, false);
         return new InteractionResultHolder(InteractionResult.SUCCESS, wand);
      }
   }

   @OnlyIn(Dist.CLIENT)
   private void openWandGUI(ItemStack wand, InteractionHand hand) {
      ScreenOpener.open(new SymmetryWandScreen(wand, hand));
   }

   private static void checkComponents(ItemStack wand) {
      if (!wand.has(AllDataComponents.SYMMETRY_WAND)) {
         wand.set(AllDataComponents.SYMMETRY_WAND, new EmptyMirror(new Vec3(0.0, 0.0, 0.0)));
         wand.set(AllDataComponents.SYMMETRY_WAND_ENABLE, false);
      }
   }

   public static boolean isEnabled(ItemStack stack) {
      checkComponents(stack);
      return (Boolean)stack.getOrDefault(AllDataComponents.SYMMETRY_WAND_ENABLE, false)
         && !(Boolean)stack.getOrDefault(AllDataComponents.SYMMETRY_WAND_SIMULATE, false);
   }

   public static SymmetryMirror getMirror(ItemStack stack) {
      checkComponents(stack);
      return (SymmetryMirror)stack.get(AllDataComponents.SYMMETRY_WAND);
   }

   public static void configureSettings(ItemStack stack, SymmetryMirror mirror) {
      checkComponents(stack);
      stack.set(AllDataComponents.SYMMETRY_WAND, mirror);
   }

   public static void apply(Level world, ItemStack wand, Player player, BlockPos pos, BlockState block) {
      checkComponents(wand);
      if (isEnabled(wand)) {
         if (BlockItem.BY_BLOCK.containsKey(block.getBlock())) {
            Map<BlockPos, BlockState> blockSet = new HashMap<>();
            blockSet.put(pos, block);
            SymmetryMirror symmetry = (SymmetryMirror)wand.get(AllDataComponents.SYMMETRY_WAND);
            Vec3 mirrorPos = symmetry.getPosition();
            if (!(mirrorPos.distanceTo(Vec3.atLowerCornerOf(pos)) > (double)((Integer)AllConfigs.server().equipment.maxSymmetryWandRange.get()).intValue())) {
               if (player.isCreative() || !isHoldingBlock(player, block) || BlockHelper.findAndRemoveInInventory(block, player, 1) != 0) {
                  symmetry.process(blockSet);
                  BlockPos to = BlockPos.containing(mirrorPos);
                  List<BlockPos> targets = new ArrayList<>();
                  targets.add(pos);

                  for (BlockPos position : blockSet.keySet()) {
                     if (!position.equals(pos) && world.isUnobstructed(block, position, CollisionContext.of(player))) {
                        BlockState blockState = blockSet.get(position);

                        for (Direction face : Iterate.directions) {
                           blockState = blockState.updateShape(face, world.getBlockState(position.relative(face)), world, position, position.relative(face));
                        }

                        if (player.isCreative()) {
                           world.setBlockAndUpdate(position, blockState);
                           targets.add(position);
                        } else {
                           BlockState toReplace = world.getBlockState(position);
                           if (toReplace.canBeReplaced() && toReplace.getDestroySpeed(world, position) != -1.0F) {
                              if (AllBlocks.CART_ASSEMBLER.has(blockState)) {
                                 BlockState railBlock = CartAssemblerBlock.getRailBlock(blockState);
                                 if (BlockHelper.findAndRemoveInInventory(railBlock, player, 1) == 0) {
                                    continue;
                                 }

                                 if (BlockHelper.findAndRemoveInInventory(blockState, player, 1) == 0) {
                                    blockState = railBlock;
                                 }
                              } else if (BlockHelper.findAndRemoveInInventory(blockState, player, 1) == 0) {
                                 continue;
                              }

                              BlockSnapshot blocksnapshot = BlockSnapshot.create(world.dimension(), world, position);
                              FluidState ifluidstate = world.getFluidState(position);
                              world.setBlock(position, ifluidstate.createLegacyBlock(), 16);
                              world.setBlockAndUpdate(position, blockState);
                              wand.set(AllDataComponents.SYMMETRY_WAND_SIMULATE, true);
                              boolean placeInterrupted = EventHooks.onBlockPlace(player, blocksnapshot, Direction.UP);
                              wand.set(AllDataComponents.SYMMETRY_WAND_SIMULATE, false);
                              if (placeInterrupted) {
                                 blocksnapshot.restore(2);
                              } else {
                                 targets.add(position);
                              }
                           }
                        }
                     }
                  }

                  CatnipServices.NETWORK.sendToClientsTrackingAndSelf(player, new SymmetryEffectPacket(to, targets));
               }
            }
         }
      }
   }

   private static boolean isHoldingBlock(Player player, BlockState block) {
      ItemStack itemBlock = BlockHelper.getRequiredItem(block);
      return player.isHolding(itemBlock.getItem());
   }

   public static void remove(Level world, ItemStack wand, Player player, BlockPos pos) {
      BlockState air = Blocks.AIR.defaultBlockState();
      BlockState ogBlock = world.getBlockState(pos);
      checkComponents(wand);
      if (isEnabled(wand)) {
         Map<BlockPos, BlockState> blockSet = new HashMap<>();
         blockSet.put(pos, air);
         SymmetryMirror symmetry = (SymmetryMirror)wand.get(AllDataComponents.SYMMETRY_WAND);
         Vec3 mirrorPos = symmetry.getPosition();
         if (!(mirrorPos.distanceTo(Vec3.atLowerCornerOf(pos)) > (double)((Integer)AllConfigs.server().equipment.maxSymmetryWandRange.get()).intValue())) {
            symmetry.process(blockSet);
            BlockPos to = BlockPos.containing(mirrorPos);
            List<BlockPos> targets = new ArrayList<>();
            targets.add(pos);

            for (BlockPos position : blockSet.keySet()) {
               if ((player.isCreative() || ogBlock.getBlock() == world.getBlockState(position).getBlock()) && !position.equals(pos)) {
                  BlockState blockstate = world.getBlockState(position);
                  if (!blockstate.isAir()) {
                     targets.add(position);
                     world.levelEvent(2001, position, Block.getId(blockstate));
                     world.setBlock(position, air, 3);
                     if (!player.isCreative()) {
                        if (!player.getMainHandItem().isEmpty()) {
                           player.getMainHandItem().mineBlock(world, blockstate, position, player);
                        }

                        BlockEntity blockEntity = blockstate.hasBlockEntity() ? world.getBlockEntity(position) : null;
                        Block.dropResources(blockstate, world, pos, blockEntity, player, player.getMainHandItem());
                     }
                  }
               }
            }

            CatnipServices.NETWORK.sendToClientsTrackingAndSelf(player, new SymmetryEffectPacket(to, targets));
         }
      }
   }

   public static boolean presentInHotbar(Player player) {
      Inventory inv = player.getInventory();

      for (int i = 0; i < Inventory.getSelectionSize(); i++) {
         if (AllItems.WAND_OF_SYMMETRY.isIn(inv.getItem(i))) {
            return true;
         }
      }

      return false;
   }

   @OnlyIn(Dist.CLIENT)
   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(SimpleCustomRenderer.create(this, new SymmetryWandItemRenderer()));
   }
}

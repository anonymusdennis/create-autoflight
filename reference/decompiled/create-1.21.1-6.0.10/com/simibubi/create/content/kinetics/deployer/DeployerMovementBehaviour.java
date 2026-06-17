package com.simibubi.create.content.kinetics.deployer;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.mounted.MountedContraption;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.schematics.SchematicInstances;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.extensions.IBaseRailBlockExtension;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.items.IItemHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

public class DeployerMovementBehaviour implements MovementBehaviour {
   @Override
   public Vec3 getActiveAreaOffset(MovementContext context) {
      return Vec3.atLowerCornerOf(((Direction)context.state.getValue(DeployerBlock.FACING)).getNormal()).scale(2.0);
   }

   @Override
   public void visitNewPosition(MovementContext context, BlockPos pos) {
      if (!context.world.isClientSide) {
         this.tryGrabbingItem(context);
         DeployerFakePlayer player = this.getPlayer(context);
         DeployerBlockEntity.Mode mode = this.getMode(context);
         if (mode != DeployerBlockEntity.Mode.USE || DeployerHandler.shouldActivate(player.getMainHandItem(), context.world, pos, null)) {
            this.activate(context, pos, player, mode);
            this.checkForTrackPlacementAdvancement(context, player);
            this.tryDisposeOfExcess(context);
            context.stall = player.blockBreakingProgress != null;
         }
      }
   }

   public void activate(MovementContext context, BlockPos pos, DeployerFakePlayer player, DeployerBlockEntity.Mode mode) {
      Level world = context.world;
      player.placedTracks = false;
      FilterItemStack filter = context.getFilterFromBE();
      if (AllItems.SCHEMATIC.isIn(filter.item())) {
         this.activateAsSchematicPrinter(context, pos, player, world, filter.item());
      } else {
         Vec3 facingVec = Vec3.atLowerCornerOf(((Direction)context.state.getValue(DeployerBlock.FACING)).getNormal());
         facingVec = context.rotation.apply(facingVec);
         Vec3 vec = context.position.subtract(facingVec.scale(2.0));
         float xRot = AbstractContraptionEntity.pitchFromVector(facingVec) - 90.0F;
         if (Math.abs(xRot) > 89.0F) {
            Vec3 initial = new Vec3(0.0, 0.0, 1.0);
            if (context.contraption.entity instanceof OrientedContraptionEntity oce) {
               initial = VecHelper.rotate(initial, (double)oce.getInitialYaw(), Axis.Y);
            }

            if (context.contraption.entity instanceof CarriageContraptionEntity cce) {
               initial = VecHelper.rotate(initial, 90.0, Axis.Y);
            }

            facingVec = context.rotation.apply(initial);
         }

         player.setYRot(AbstractContraptionEntity.yawFromVector(facingVec));
         player.setXRot(xRot);
         DeployerHandler.activate(player, vec, pos, facingVec, mode);
      }
   }

   protected void checkForTrackPlacementAdvancement(MovementContext context, DeployerFakePlayer player) {
      if ((context.contraption instanceof MountedContraption || context.contraption instanceof CarriageContraption)
         && player.placedTracks
         && context.blockEntityData != null
         && context.blockEntityData.contains("Owner")) {
         AllAdvancements.SELF_DEPLOYING.awardTo(context.world.getPlayerByUUID(context.blockEntityData.getUUID("Owner")));
      }
   }

   protected void activateAsSchematicPrinter(MovementContext context, BlockPos pos, DeployerFakePlayer player, Level level, ItemStack filter) {
      if (filter.has(AllDataComponents.SCHEMATIC_ANCHOR)) {
         if (level.getBlockState(pos).canBeReplaced()) {
            if ((Boolean)filter.getOrDefault(AllDataComponents.SCHEMATIC_DEPLOYED, false)) {
               SchematicLevel schematicWorld = SchematicInstances.get(level, filter);
               if (schematicWorld != null) {
                  if (schematicWorld.getBounds().isInside(pos.subtract(schematicWorld.anchor))) {
                     BlockState blockState = schematicWorld.getBlockState(pos);
                     ItemRequirement requirement = ItemRequirement.of(blockState, schematicWorld.getBlockEntity(pos));
                     if (!requirement.isInvalid() && !requirement.isEmpty()) {
                        if (!AllBlocks.BELT.has(blockState)) {
                           List<ItemRequirement.StackRequirement> requiredItems = requirement.getRequiredItems();
                           ItemStack contextStack = requiredItems.isEmpty() ? ItemStack.EMPTY : requiredItems.get(0).stack;
                           if (!context.contraption.hasUniversalCreativeCrate) {
                              IItemHandler itemHandler = context.contraption.getStorage().getAllItems();

                              for (ItemRequirement.StackRequirement required : requiredItems) {
                                 ItemStack stack = ItemHelper.extract(
                                    itemHandler, required::matches, ItemHelper.ExtractionCountMode.EXACTLY, required.stack.getCount(), true
                                 );
                                 if (stack.isEmpty()) {
                                    return;
                                 }
                              }

                              for (ItemRequirement.StackRequirement requiredx : requiredItems) {
                                 contextStack = ItemHelper.extract(
                                    itemHandler, requiredx::matches, ItemHelper.ExtractionCountMode.EXACTLY, requiredx.stack.getCount(), false
                                 );
                              }
                           }

                           CompoundTag data = BlockHelper.prepareBlockEntityData(level, blockState, schematicWorld.getBlockEntity(pos));
                           BlockSnapshot blocksnapshot = BlockSnapshot.create(level.dimension(), level, pos);
                           BlockHelper.placeSchematicBlock(level, blockState, pos, contextStack, data);
                           if (EventHooks.onBlockPlace(player, blocksnapshot, Direction.UP)) {
                              blocksnapshot.restore(2);
                           } else if (blockState.getBlock() instanceof IBaseRailBlockExtension || blockState.getBlock() instanceof ITrackBlock) {
                              player.placedTracks = true;
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
   public void tick(MovementContext context) {
      if (!context.world.isClientSide) {
         if (context.stall) {
            DeployerFakePlayer player = this.getPlayer(context);
            DeployerBlockEntity.Mode mode = this.getMode(context);
            Pair<BlockPos, Float> blockBreakingProgress = player.blockBreakingProgress;
            if (blockBreakingProgress != null) {
               int timer = context.data.getInt("Timer");
               if (timer < 20) {
                  context.data.putInt("Timer", ++timer);
                  return;
               }

               context.data.remove("Timer");
               this.activate(context, (BlockPos)blockBreakingProgress.getKey(), player, mode);
               this.tryDisposeOfExcess(context);
            }

            context.stall = player.blockBreakingProgress != null;
         }
      }
   }

   @Override
   public void cancelStall(MovementContext context) {
      if (!context.world.isClientSide) {
         MovementBehaviour.super.cancelStall(context);
         DeployerFakePlayer player = this.getPlayer(context);
         if (player != null) {
            if (player.blockBreakingProgress != null) {
               context.world.destroyBlockProgress(player.getId(), (BlockPos)player.blockBreakingProgress.getKey(), -1);
               player.blockBreakingProgress = null;
            }
         }
      }
   }

   @Override
   public void stopMoving(MovementContext context) {
      if (!context.world.isClientSide) {
         DeployerFakePlayer player = this.getPlayer(context);
         if (player != null) {
            this.cancelStall(context);
            context.blockEntityData.put("Inventory", player.getInventory().save(new ListTag()));
            player.discard();
         }
      }
   }

   private void tryGrabbingItem(MovementContext context) {
      DeployerFakePlayer player = this.getPlayer(context);
      if (player != null) {
         if (player.getMainHandItem().isEmpty()) {
            FilterItemStack filter = context.getFilterFromBE();
            if (AllItems.SCHEMATIC.isIn(filter.item())) {
               return;
            }

            ItemStack held = ItemHelper.extract(context.contraption.getStorage().getAllItems(), stack -> filter.test(context.world, stack), 1, false);
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
         }
      }
   }

   private void tryDisposeOfExcess(MovementContext context) {
      DeployerFakePlayer player = this.getPlayer(context);
      if (player != null) {
         Inventory inv = player.getInventory();
         FilterItemStack filter = context.getFilterFromBE();

         for (List<ItemStack> list : Arrays.asList(inv.armor, inv.offhand, inv.items)) {
            for (int i = 0; i < list.size(); i++) {
               ItemStack itemstack = list.get(i);
               if (!itemstack.isEmpty() && (list != inv.items || i != inv.selected || !filter.test(context.world, itemstack))) {
                  this.collectOrDropItem(context, itemstack);
                  list.set(i, ItemStack.EMPTY);
               }
            }
         }
      }
   }

   @Override
   public void writeExtraData(MovementContext context) {
      DeployerFakePlayer player = this.getPlayer(context);
      if (player != null) {
         context.data.put("HeldItem", player.getMainHandItem().saveOptional(context.world.registryAccess()));
      }
   }

   private DeployerFakePlayer getPlayer(MovementContext context) {
      if (!(context.temporaryData instanceof DeployerFakePlayer) && context.world instanceof ServerLevel) {
         UUID owner = context.blockEntityData.contains("Owner") ? context.blockEntityData.getUUID("Owner") : null;
         DeployerFakePlayer deployerFakePlayer = new DeployerFakePlayer((ServerLevel)context.world, owner);
         deployerFakePlayer.onMinecartContraption = context.contraption instanceof MountedContraption;
         deployerFakePlayer.getInventory().load(context.blockEntityData.getList("Inventory", 10));
         if (context.data.contains("HeldItem")) {
            deployerFakePlayer.setItemInHand(
               InteractionHand.MAIN_HAND, ItemStack.parseOptional(context.world.registryAccess(), context.data.getCompound("HeldItem"))
            );
         }

         context.blockEntityData.remove("Inventory");
         context.temporaryData = deployerFakePlayer;
      }

      return (DeployerFakePlayer)context.temporaryData;
   }

   private DeployerBlockEntity.Mode getMode(MovementContext context) {
      return (DeployerBlockEntity.Mode)NBTHelper.readEnum(context.blockEntityData, "Mode", DeployerBlockEntity.Mode.class);
   }

   @Override
   public boolean disableBlockEntityRendering() {
      return true;
   }

   @Override
   public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffers) {
      if (!VisualizationManager.supportsVisualization(context.world)) {
         DeployerRenderer.renderInContraption(context, renderWorld, matrices, buffers);
      }
   }

   @Nullable
   @Override
   public ActorVisual createVisual(VisualizationContext visualizationContext, VirtualRenderWorld simulationWorld, MovementContext movementContext) {
      return new DeployerActorVisual(visualizationContext, simulationWorld, movementContext);
   }
}

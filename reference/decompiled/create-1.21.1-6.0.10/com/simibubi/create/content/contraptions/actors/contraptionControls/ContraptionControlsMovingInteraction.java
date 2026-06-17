package com.simibubi.create.content.contraptions.actors.contraptionControls;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.elevator.ElevatorContraption;
import com.simibubi.create.content.contraptions.elevator.ElevatorTargetFloorPacket;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import java.util.Iterator;
import java.util.List;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.MutablePair;

public class ContraptionControlsMovingInteraction extends MovingInteractionBehaviour {
   @Override
   public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos, AbstractContraptionEntity contraptionEntity) {
      Contraption contraption = contraptionEntity.getContraption();
      MutablePair<StructureBlockInfo, MovementContext> actor = contraption.getActorAt(localPos);
      if (actor == null) {
         return false;
      } else {
         MovementContext ctx = (MovementContext)actor.right;
         if (ctx == null) {
            return false;
         } else if (contraption instanceof ElevatorContraption ec) {
            return this.elevatorInteraction(localPos, contraptionEntity, ec, ctx);
         } else if (contraptionEntity.level().isClientSide()) {
            if (contraption.getBlockEntityClientSide(ctx.localPos) instanceof ContraptionControlsBlockEntity cbe) {
               cbe.pressButton();
            }

            return true;
         } else {
            ItemStack filter = ContraptionControlsMovement.getFilter(ctx);
            boolean disable = true;
            boolean invert = false;
            List<ItemStack> disabledActors = contraption.getDisabledActors();
            Iterator<ItemStack> iterator = disabledActors.iterator();

            while (iterator.hasNext()) {
               ItemStack presentFilter = iterator.next();
               boolean sameFilter = ContraptionControlsMovement.isSameFilter(presentFilter, filter);
               if (presentFilter.isEmpty()) {
                  iterator.remove();
                  disable = false;
                  if (!sameFilter) {
                     invert = true;
                  }
               } else if (sameFilter) {
                  iterator.remove();
                  disable = false;
                  break;
               }
            }

            if (invert) {
               for (MutablePair<StructureBlockInfo, MovementContext> pair : contraption.getActors()) {
                  MovementBehaviour behaviour = MovementBehaviour.REGISTRY.get(((StructureBlockInfo)pair.left).state());
                  if (behaviour != null) {
                     ItemStack behaviourStack = behaviour.canBeDisabledVia((MovementContext)pair.right);
                     if (behaviourStack != null
                        && !ContraptionControlsMovement.isSameFilter(behaviourStack, filter)
                        && !contraption.isActorTypeDisabled(behaviourStack)) {
                        disabledActors.add(behaviourStack);
                        this.send(contraptionEntity, behaviourStack, true);
                     }
                  }
               }
            }

            if (filter.isEmpty()) {
               disabledActors.clear();
            }

            if (disable) {
               disabledActors.add(filter);
            }

            contraption.setActorsActive(filter, !disable);
            ContraptionControlsBlockEntity.sendStatus(player, filter, !disable);
            this.send(contraptionEntity, filter, disable);
            AllSoundEvents.CONTROLLER_CLICK
               .play(player.level(), null, BlockPos.containing(contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0F)), 1.0F, disable ? 0.8F : 1.5F);
            if (contraptionEntity instanceof CarriageContraptionEntity cce) {
               if (!filter.is(ItemTags.DOORS)) {
                  return true;
               } else {
                  Carriage carriage = cce.getCarriage();
                  Train train = carriage.train;

                  for (Carriage c : train.carriages) {
                     CarriageContraptionEntity anyAvailableEntity = c.anyAvailableEntity();
                     if (anyAvailableEntity != null) {
                        Contraption cpt = anyAvailableEntity.getContraption();
                        cpt.setActorsActive(filter, !disable);
                        ContraptionControlsBlockEntity.sendStatus(player, filter, !disable);
                        this.send(anyAvailableEntity, filter, disable);
                     }
                  }

                  return true;
               }
            } else {
               return true;
            }
         }
      }
   }

   private void send(AbstractContraptionEntity contraptionEntity, ItemStack filter, boolean disable) {
      CatnipServices.NETWORK.sendToClientsTrackingEntity(contraptionEntity, new ContraptionDisableActorPacket(contraptionEntity.getId(), filter, !disable));
   }

   private boolean elevatorInteraction(BlockPos localPos, AbstractContraptionEntity contraptionEntity, ElevatorContraption contraption, MovementContext ctx) {
      Level level = contraptionEntity.level();
      if (!level.isClientSide()) {
         BlockPos pos = BlockPos.containing(contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0F));
         AllSoundEvents.CONTROLLER_CLICK.play(level, null, pos, 1.0F, 1.5F);
         AllSoundEvents.CONTRAPTION_ASSEMBLE.play(level, null, pos, 0.75F, 0.8F);
         return true;
      } else if (ctx.temporaryData instanceof ContraptionControlsMovement.ElevatorFloorSelection efs) {
         if (efs.currentTargetY == contraption.clientYTarget) {
            return true;
         } else {
            CatnipServices.NETWORK.sendToServer(new ElevatorTargetFloorPacket(contraptionEntity, efs.currentTargetY));
            if (contraption.getBlockEntityClientSide(ctx.localPos) instanceof ContraptionControlsBlockEntity cbe) {
               cbe.pressButton();
            }

            return true;
         }
      } else {
         return false;
      }
   }
}

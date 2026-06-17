package com.simibubi.create.foundation.blockEntity.behaviour;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.SidedFilteringBehaviour;
import com.simibubi.create.foundation.utility.AdventureUtil;
import java.util.Iterator;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;

@EventBusSubscriber
public class ValueSettingsInputHandler {
   @SubscribeEvent
   public static void onBlockActivated(RightClickBlock event) {
      Level world = event.getLevel();
      BlockPos pos = event.getPos();
      Player player = event.getEntity();
      InteractionHand hand = event.getHand();
      if (canInteract(player)) {
         if (!AllBlocks.CLIPBOARD.isIn(player.getMainHandItem())) {
            if (world.getBlockEntity(pos) instanceof SmartBlockEntity sbe) {
               if (event.getSide() == LogicalSide.CLIENT) {
                  CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> CreateClient.VALUE_SETTINGS_HANDLER.cancelIfWarmupAlreadyStarted(event));
               }

               if (!event.isCanceled()) {
                  Iterator var12 = sbe.getAllBehaviours().iterator();

                  while (true) {
                     BlockEntityBehaviour behaviour;
                     BlockHitResult ray;
                     while (true) {
                        while (true) {
                           if (!var12.hasNext()) {
                              return;
                           }

                           behaviour = (BlockEntityBehaviour)var12.next();
                           if (behaviour instanceof ValueSettingsBehaviour valueSettingsBehaviour
                              && !valueSettingsBehaviour.bypassesInput(player.getMainHandItem())
                              && valueSettingsBehaviour.mayInteract(player)) {
                              ray = event.getHitVec();
                              if (ray == null) {
                                 return;
                              }

                              if (!(behaviour instanceof SidedFilteringBehaviour)) {
                                 break;
                              }

                              behaviour = ((SidedFilteringBehaviour)behaviour).get(ray.getDirection());
                              if (behaviour != null) {
                                 break;
                              }
                           }
                        }

                        if (valueSettingsBehaviour.isActive()
                           && (!valueSettingsBehaviour.onlyVisibleWithWrench() || player.getItemInHand(hand).is(Items.TOOLS_WRENCH))) {
                           if (!(valueSettingsBehaviour.getSlotPositioning() instanceof ValueBoxTransform.Sided sidedSlot)) {
                              break;
                           }

                           if (sidedSlot.isSideActive(sbe.getBlockState(), ray.getDirection())) {
                              sidedSlot.fromSide(ray.getDirection());
                              break;
                           }
                        }
                     }

                     boolean fakePlayer = player instanceof FakePlayer;
                     if (valueSettingsBehaviour.testHit(ray.getLocation()) || fakePlayer) {
                        event.setCanceled(true);
                        event.setCancellationResult(InteractionResult.SUCCESS);
                        if (valueSettingsBehaviour.acceptsValueSettings() && !fakePlayer) {
                           if (event.getSide() == LogicalSide.CLIENT) {
                              BehaviourType<?> type = behaviour.getType();
                              CatnipServices.PLATFORM
                                 .executeOnClientOnly(() -> () -> CreateClient.VALUE_SETTINGS_HANDLER.startInteractionWith(pos, type, hand, ray.getDirection()));
                           }

                           return;
                        } else {
                           valueSettingsBehaviour.onShortInteract(player, hand, ray.getDirection(), ray);
                           return;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static boolean canInteract(Player player) {
      return player != null && !player.isSpectator() && !player.isShiftKeyDown() && !AdventureUtil.isAdventure(player);
   }
}

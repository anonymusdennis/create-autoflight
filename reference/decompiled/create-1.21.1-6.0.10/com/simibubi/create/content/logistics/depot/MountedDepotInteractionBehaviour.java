package com.simibubi.create.content.logistics.depot;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.content.logistics.depot.storage.DepotMountedStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class MountedDepotInteractionBehaviour extends MovingInteractionBehaviour {
   @Override
   public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos, AbstractContraptionEntity contraptionEntity) {
      ItemStack itemInHand = player.getItemInHand(activeHand);
      if (activeHand == InteractionHand.OFF_HAND) {
         return false;
      } else if (player.level().isClientSide) {
         return true;
      } else {
         MountedStorageManager manager = contraptionEntity.getContraption().getStorage();
         MountedItemStorage storage = (MountedItemStorage)manager.getAllItemStorages().get(localPos);
         if (storage instanceof DepotMountedStorage depot) {
            ItemStack itemOnDepot = depot.getItem();
            if (itemOnDepot.isEmpty() && itemInHand.isEmpty()) {
               return true;
            } else {
               depot.setItem(itemInHand.copy());
               player.setItemInHand(activeHand, itemOnDepot.copy());
               AllSoundEvents.DEPOT_PLOP.playOnServer(player.level(), BlockPos.containing(contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 0.0F)));
               return true;
            }
         } else {
            return false;
         }
      }
   }
}

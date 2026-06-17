package dev.eriksonn.aeronautics.content.blocks.mounted_potato_cannon;

import com.simibubi.create.api.equipment.potatoCannon.PotatoCannonProjectileType;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.equipment.potatoCannon.PotatoCannonItem.Ammo;
import dev.simulated_team.simulated.multiloader.inventory.ContainerSlot;
import dev.simulated_team.simulated.multiloader.inventory.ItemInfoWrapper;
import dev.simulated_team.simulated.multiloader.inventory.SingleSlotContainer;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class MountedPotatoCannonInventory extends SingleSlotContainer {
   private final MountedPotatoCannonBlockEntity be;
   private PotatoCannonProjectileType cachedProjectileType;

   public MountedPotatoCannonInventory(MountedPotatoCannonBlockEntity be) {
      super(16);
      this.be = be;
   }

   public void setChanged() {
      this.be.notifyUpdate();
   }

   public void onStackItemChange(ContainerSlot slot, ItemStack oldSlotStack, ItemStack newSlotStack) {
      super.onStackItemChange(slot, oldSlotStack, newSlotStack);
      if (oldSlotStack.getItem() != newSlotStack.getItem()) {
         this.updateCachedType(this.be.getLevel().registryAccess(), newSlotStack);
         this.be.resetAndUpdate();
      }
   }

   public void updateCachedType(Provider registries, ItemStack itemStack) {
      this.cachedProjectileType = registries.lookupOrThrow(CreateRegistries.POTATO_PROJECTILE_TYPE)
         .listElements()
         .filter(ref -> ((PotatoCannonProjectileType)ref.value()).items().contains(itemStack.getItem().builtInRegistryHolder()))
         .findFirst()
         .<PotatoCannonProjectileType>map(Reference::value)
         .orElse(null);
   }

   @Nullable
   public Ammo getAmmo() {
      ItemStack currentStack = this.getItem(0);
      return this.cachedProjectileType != null ? new Ammo(currentStack, this.cachedProjectileType) : null;
   }

   public boolean canInsertItem(ItemInfoWrapper info) {
      return PotatoCannonProjectileType.getTypeForItem(this.be.getLevel().registryAccess(), info.type()).isPresent();
   }
}

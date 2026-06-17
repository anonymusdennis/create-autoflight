package com.simibubi.create.content.equipment.toolbox;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.animatedContainer.AnimatedContainerBehaviour;
import com.simibubi.create.foundation.utility.ResetableLazy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentMap.Builder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity.DataComponentInput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;

public class ToolboxBlockEntity extends SmartBlockEntity implements MenuProvider, Nameable {
   public LerpedFloat lid = LerpedFloat.linear().startWithValue(0.0);
   public LerpedFloat drawers = LerpedFloat.linear().startWithValue(0.0);
   UUID uniqueId;
   ToolboxInventory inventory;
   ResetableLazy<DyeColor> colorProvider;
   Map<Integer, WeakHashMap<Player, Integer>> connectedPlayers = new HashMap<>();
   private Component customName;
   private AnimatedContainerBehaviour<ToolboxMenu> openTracker;

   public ToolboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.inventory = new ToolboxInventory(this);
      this.colorProvider = ResetableLazy.of(() -> {
         BlockState blockState = this.getBlockState();
         return blockState != null && blockState.getBlock() instanceof ToolboxBlock ? ((ToolboxBlock)blockState.getBlock()).getColor() : DyeColor.BROWN;
      });
      this.setLazyTickRate(10);
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      event.registerBlockEntity(ItemHandler.BLOCK, (BlockEntityType)AllBlockEntityTypes.TOOLBOX.get(), (be, context) -> be.inventory);
   }

   public DyeColor getColor() {
      return this.colorProvider.get();
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(this.openTracker = new AnimatedContainerBehaviour(this, ToolboxMenu.class));
   }

   @Override
   public void initialize() {
      super.initialize();
      ToolboxHandler.onLoad(this);
   }

   @Override
   public void invalidate() {
      super.invalidate();
      ToolboxHandler.onUnload(this);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level.isClientSide) {
         this.tickAudio();
      }

      if (!this.level.isClientSide) {
         this.tickPlayers();
      }

      this.lid.chase(this.openTracker.openCount > 0 ? 1.0 : 0.0, 0.2F, Chaser.LINEAR);
      this.drawers.chase(this.openTracker.openCount > 0 ? 1.0 : 0.0, 0.2F, Chaser.EXP);
      this.lid.tickChaser();
      this.drawers.tickChaser();
   }

   private void tickPlayers() {
      boolean update = false;
      Iterator<Entry<Integer, WeakHashMap<Player, Integer>>> toolboxSlots = this.connectedPlayers.entrySet().iterator();

      while (toolboxSlots.hasNext()) {
         Entry<Integer, WeakHashMap<Player, Integer>> toolboxSlotEntry = toolboxSlots.next();
         WeakHashMap<Player, Integer> set = toolboxSlotEntry.getValue();
         int slot = toolboxSlotEntry.getKey();
         ItemStack referenceItem = this.inventory.filters.get(slot);
         boolean clear = referenceItem.isEmpty();
         Iterator<Entry<Player, Integer>> playerEntries = set.entrySet().iterator();

         while (playerEntries.hasNext()) {
            Entry<Player, Integer> playerEntry = playerEntries.next();
            Player player = playerEntry.getKey();
            int hotbarSlot = playerEntry.getValue();
            if (clear || ToolboxHandler.withinRange(player, this)) {
               Inventory playerInv = player.getInventory();
               ItemStack playerStack = playerInv.getItem(hotbarSlot);
               if (!clear && (playerStack.isEmpty() || ToolboxInventory.canItemsShareCompartment(playerStack, referenceItem))) {
                  int count = playerStack.getCount();
                  int targetAmount = (referenceItem.getMaxStackSize() + 1) / 2;
                  if (count < targetAmount) {
                     int amountToReplenish = targetAmount - count;
                     if (this.isOpenInContainer(player)) {
                        ItemStack extracted = this.inventory.takeFromCompartment(amountToReplenish, slot, true);
                        if (!extracted.isEmpty()) {
                           ToolboxHandler.unequip(player, hotbarSlot, false);
                           ToolboxHandler.syncData(player);
                           continue;
                        }
                     }

                     ItemStack extracted = this.inventory.takeFromCompartment(amountToReplenish, slot, false);
                     if (!extracted.isEmpty()) {
                        update = true;
                        ItemStack template = playerStack.isEmpty() ? extracted : playerStack;
                        playerInv.setItem(hotbarSlot, template.copyWithCount(count + extracted.getCount()));
                     }
                  }

                  if (count > targetAmount) {
                     int amountToDeposit = count - targetAmount;
                     ItemStack toDistribute = playerStack.copyWithCount(amountToDeposit);
                     if (this.isOpenInContainer(player)) {
                        int deposited = amountToDeposit - this.inventory.distributeToCompartment(toDistribute, slot, true).getCount();
                        if (deposited > 0) {
                           ToolboxHandler.unequip(player, hotbarSlot, true);
                           ToolboxHandler.syncData(player);
                           continue;
                        }
                     }

                     int deposited = amountToDeposit - this.inventory.distributeToCompartment(toDistribute, slot, false).getCount();
                     if (deposited > 0) {
                        update = true;
                        playerInv.setItem(hotbarSlot, playerStack.copyWithCount(count - deposited));
                     }
                  }
               } else {
                  player.getPersistentData().getCompound("CreateToolboxData").remove(String.valueOf(hotbarSlot));
                  playerEntries.remove();
                  if (player instanceof ServerPlayer) {
                     ToolboxHandler.syncData(player);
                  }
               }
            }
         }

         if (clear) {
            toolboxSlots.remove();
         }
      }

      if (update) {
         this.sendData();
      }
   }

   private boolean isOpenInContainer(Player player) {
      return player.containerMenu instanceof ToolboxMenu && ((ToolboxMenu)player.containerMenu).contentHolder == this;
   }

   public void unequipTracked() {
      if (!this.level.isClientSide) {
         Set<ServerPlayer> affected = new HashSet<>();

         for (Entry<Integer, WeakHashMap<Player, Integer>> toolboxSlotEntry : this.connectedPlayers.entrySet()) {
            WeakHashMap<Player, Integer> set = toolboxSlotEntry.getValue();

            for (Entry<Player, Integer> playerEntry : set.entrySet()) {
               Player player = playerEntry.getKey();
               int hotbarSlot = playerEntry.getValue();
               ToolboxHandler.unequip(player, hotbarSlot, false);
               if (player instanceof ServerPlayer) {
                  affected.add((ServerPlayer)player);
               }
            }
         }

         for (ServerPlayer player : affected) {
            ToolboxHandler.syncData(player);
         }

         this.connectedPlayers.clear();
      }
   }

   public void unequip(int slot, Player player, int hotbarSlot, boolean keepItems) {
      if (this.connectedPlayers.containsKey(slot)) {
         this.connectedPlayers.get(slot).remove(player);
         if (!keepItems) {
            Inventory playerInv = player.getInventory();
            ItemStack playerStack = playerInv.getItem(hotbarSlot);
            ItemStack toInsert = ToolboxInventory.cleanItemNBT(playerStack.copy());
            ItemStack remainder = this.inventory.distributeToCompartment(toInsert, slot, false);
            if (remainder.getCount() != toInsert.getCount()) {
               playerInv.setItem(hotbarSlot, remainder);
            }
         }
      }
   }

   private void tickAudio() {
      Vec3 vec = VecHelper.getCenterOf(this.worldPosition);
      if (this.lid.settled()) {
         if (this.openTracker.openCount > 0 && this.lid.getChaseTarget() == 0.0F) {
            this.level
               .playLocalSound(vec.x, vec.y, vec.z, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 0.25F, this.level.random.nextFloat() * 0.1F + 1.2F, true);
            this.level.playLocalSound(vec.x, vec.y, vec.z, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.1F, this.level.random.nextFloat() * 0.1F + 1.1F, true);
         }

         if (this.openTracker.openCount == 0 && this.lid.getChaseTarget() == 1.0F) {
            this.level
               .playLocalSound(vec.x, vec.y, vec.z, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.1F, this.level.random.nextFloat() * 0.1F + 1.1F, true);
         }
      } else if (this.openTracker.openCount == 0 && this.lid.getChaseTarget() == 0.0F && this.lid.getValue(0.0F) > 0.0625F && this.lid.getValue(1.0F) < 0.0625F
         )
       {
         this.level
            .playLocalSound(vec.x, vec.y, vec.z, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.25F, this.level.random.nextFloat() * 0.1F + 1.2F, true);
      }
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      this.inventory.deserializeNBT(registries, compound.getCompound("Inventory"));
      super.read(compound, registries, clientPacket);
      if (compound.contains("UniqueId", 11)) {
         this.uniqueId = compound.getUUID("UniqueId");
      }

      if (compound.contains("CustomName", 8)) {
         this.customName = Serializer.fromJson(compound.getString("CustomName"), registries);
      }
   }

   @Override
   protected void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      if (this.uniqueId == null) {
         this.uniqueId = UUID.randomUUID();
      }

      compound.put("Inventory", this.inventory.serializeNBT(registries));
      compound.putUUID("UniqueId", this.uniqueId);
      if (this.customName != null) {
         compound.putString("CustomName", Serializer.toJson(this.customName, registries));
      }

      super.write(compound, registries, clientPacket);
   }

   public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
      return ToolboxMenu.create(id, inv, this);
   }

   @Override
   public void lazyTick() {
      ToolboxHandler.onLoad(this);
      super.lazyTick();
   }

   public void connectPlayer(int slot, Player player, int hotbarSlot) {
      if (!this.level.isClientSide) {
         WeakHashMap<Player, Integer> map = this.connectedPlayers.computeIfAbsent(slot, WeakHashMap::new);
         Integer previous = map.get(player);
         if (previous != null) {
            if (previous == hotbarSlot) {
               return;
            }

            ToolboxHandler.unequip(player, previous, false);
         }

         map.put(player, hotbarSlot);
      }
   }

   public void readInventory(ToolboxInventory inv) {
      if (inv != null) {
         this.inventory.filters = new ArrayList<>(inv.filters);

         for (int i = 0; i < inv.getSlots(); i++) {
            this.inventory.setStackInSlot(i, inv.getStackInSlot(i));
         }
      }
   }

   public void setUniqueId(UUID uniqueId) {
      this.uniqueId = uniqueId;
   }

   public UUID getUniqueId() {
      return this.uniqueId;
   }

   public boolean isFullyInitialized() {
      return this.uniqueId != null;
   }

   public void setCustomName(Component customName) {
      this.customName = customName;
   }

   public Component getDisplayName() {
      return (Component)(this.customName != null ? this.customName : ((ToolboxBlock)AllBlocks.TOOLBOXES.get(this.getColor()).get()).getName());
   }

   public Component getCustomName() {
      return this.customName;
   }

   public boolean hasCustomName() {
      return this.customName != null;
   }

   public Component getName() {
      return this.customName;
   }

   public void setBlockState(BlockState state) {
      super.setBlockState(state);
      this.colorProvider.reset();
   }

   protected void applyImplicitComponents(DataComponentInput componentInput) {
      this.setUniqueId((UUID)componentInput.get(AllDataComponents.TOOLBOX_UUID));
      this.readInventory((ToolboxInventory)componentInput.get(AllDataComponents.TOOLBOX_INVENTORY));
   }

   protected void collectImplicitComponents(Builder components) {
      components.set(AllDataComponents.TOOLBOX_UUID, this.uniqueId);
      components.set(AllDataComponents.TOOLBOX_INVENTORY, this.inventory);
   }
}

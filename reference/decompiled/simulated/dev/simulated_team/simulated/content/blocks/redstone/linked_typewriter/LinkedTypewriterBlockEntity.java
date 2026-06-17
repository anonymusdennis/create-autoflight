package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.simulated_team.simulated.compat.computercraft.AttachedComputerHandler;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.LinkedTypewriterMenuCommon;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimSoundEvents;
import dev.simulated_team.simulated.mixin_interface.PlayerTypewriterExtension;
import dev.simulated_team.simulated.service.SimPlatformService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LinkedTypewriterBlockEntity extends SmartBlockEntity implements MenuProvider, ClipboardCloneable {
   private static final boolean CC_LOADED = SimPlatformService.INSTANCE.isLoaded("computercraft");
   private LinkedTypewriterEntries entryMap;
   private final List<Integer> pressedKeys = new ArrayList<>();
   private UUID currentUser;
   private String typedEntry = "";
   public boolean powered;
   public final AttachedComputerHandler computerHandler;

   public LinkedTypewriterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.entryMap = new LinkedTypewriterEntries();
      if (CC_LOADED) {
         this.computerHandler = new AttachedComputerHandler();
      } else {
         this.computerHandler = null;
      }
   }

   public void addBehaviours(List<BlockEntityBehaviour> list) {
   }

   public void tick() {
      super.tick();

      assert this.level != null;

      this.entryMap.updateNetworks(this.level);
      if (!this.level.isClientSide) {
         if ((Boolean)this.getBlockState().getValue(LinkedTypewriterBlock.POWERED) != this.powered) {
            this.level.setBlockAndUpdate(this.getBlockPos(), (BlockState)this.getBlockState().setValue(LinkedTypewriterBlock.POWERED, this.powered));
         }

         if (this.currentUser != null) {
            Player currentPlayer = this.level.getPlayerByUUID(this.currentUser);
            if (currentPlayer == null || !playerInRange(currentPlayer, this.level, this.getBlockPos())) {
               this.disconnectUser();
            }
         }
      }
   }

   public static boolean playerInRange(Player player, Level world, BlockPos pos) {
      double range = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).getValue();
      return Sable.HELPER
            .distanceSquaredWithSubLevels(world, player.getEyePosition(), (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5)
         < range * range;
   }

   public LinkedTypewriterEntries getTypewriterEntries() {
      return this.entryMap;
   }

   public boolean checkAndStartUsing(UUID userID) {
      if (this.currentUser == null) {
         Player player = this.level.getPlayerByUUID(userID);
         if (player != null) {
            PlayerTypewriterExtension playerEx = (PlayerTypewriterExtension)player;
            this.currentUser = userID;
            BlockPos previousTypewriter = playerEx.simulated$getCurrentTypewriter();
            if (previousTypewriter != null && this.level.getBlockEntity(previousTypewriter) instanceof LinkedTypewriterBlockEntity nbe) {
               nbe.disconnectUser();
            }

            this.powered = true;
            playerEx.simulated$setCurrentTypewriter(this.getBlockPos());
            if (this.level.isClientSide) {
               LinkedTypewriterInteractionHandler.associateTypewriter(this);
            } else {
               this.level
                  .playSound(
                     null,
                     this.worldPosition,
                     AllSoundEvents.CONTROLLER_PUT.getMainEvent(),
                     SoundSource.BLOCKS,
                     1.0F,
                     0.95F + 0.1F * this.level.getRandom().nextFloat()
                  );
               this.sendConnectMessage(player);
            }

            return true;
         }
      }

      return false;
   }

   public void sendConnectMessage(Player player) {
      Component customName = (Component)this.components().getOrDefault(DataComponents.CUSTOM_NAME, SimLang.translate("linked_typewriter.title").component());
      player.displayClientMessage(SimLang.translate("linked_typewriter.start_controlling", customName.getString()).component(), true);
   }

   public void sendDisconnectMessage(Player player) {
      Component customName = (Component)this.components().getOrDefault(DataComponents.CUSTOM_NAME, SimLang.translate("linked_typewriter.title").component());
      player.displayClientMessage(SimLang.translate("linked_typewriter.stop_controlling", customName.getString()).component(), true);
   }

   public boolean checkUser(UUID user) {
      return user.equals(this.currentUser);
   }

   public boolean isInUse() {
      return this.currentUser != null;
   }

   public void disconnectUser() {
      if (!this.level.isClientSide) {
         this.pressedKeys.clear();
         this.entryMap.deactivateAll();
         this.setChanged();
         this.sendData();
         this.level
            .playSound(
               null,
               this.worldPosition,
               SimSoundEvents.LINKED_TYPEWRITER_DING.event(),
               SoundSource.BLOCKS,
               1.0F,
               0.95F + 0.1F * this.level.getRandom().nextFloat()
            );
         Player player = this.level.getPlayerByUUID(this.currentUser);
         if (player != null) {
            this.sendDisconnectMessage(player);
            ((PlayerTypewriterExtension)player).simulated$setCurrentTypewriter(null);
         }
      } else {
         LinkedTypewriterInteractionHandler.associateTypewriter(null);
      }

      this.powered = false;
      this.currentUser = null;
   }

   public List<Integer> getPressedKeys() {
      return this.pressedKeys;
   }

   public void onKeyInteraction(UUID user, @Nullable LinkedTypewriterEntries.KeyboardEntry toBind, int key, boolean press) {
      if (this.checkUser(user)) {
         if (press && toBind != null) {
            this.entryMap.setKey(key, toBind);
         } else {
            if (press) {
               this.pressKey(key);
            } else {
               this.releaseKey(key);
            }
         }
      }
   }

   public void pressKey(int key) {
      this.pressedKeys.add(key);
      if (key == 259) {
         if (!this.typedEntry.isEmpty()) {
            this.typedEntry = this.typedEntry.substring(0, this.typedEntry.length() - 1);
         }
      } else {
         this.typedEntry = this.typedEntry + (char)key;
      }

      if (this.typedEntry.length() >= 25) {
         this.typedEntry = this.typedEntry.substring(1);
      }

      if (this.computerHandler != null) {
         this.computerHandler.queueEvent("key", key, this.entryMap.getEntry(key).isAlive());
      }

      this.entryMap.activateKey(key, this);
   }

   public void releaseKey(int key) {
      this.pressedKeys.remove(Integer.valueOf(key));
      this.entryMap.deactivateKey(key);
      if (this.computerHandler != null) {
         this.computerHandler.queueEvent("key_up", key);
      }
   }

   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.write(tag, registries, clientPacket);
      tag.putString("typedEntry", this.typedEntry);
      tag.put("Keys", this.entryMap.saveKeys(registries));
      if (this.currentUser != null) {
         tag.putUUID("CurrentUser", this.currentUser);
      }
   }

   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.read(tag, registries, clientPacket);
      this.typedEntry = tag.getString("typedEntry");
      this.entryMap = LinkedTypewriterEntries.readKeys(registries, tag.getList("Keys", 10), this.getBlockPos());
      if (tag.contains("CurrentUser")) {
         this.currentUser = tag.getUUID("CurrentUser");
      } else {
         this.currentUser = null;
      }
   }

   public String getTypedEntry() {
      return this.typedEntry;
   }

   public void invalidate() {
      this.pressedKeys.clear();
      this.entryMap.deactivateAll();
      this.entryMap.updateNetworks(this.level);
      super.invalidate();
   }

   public void destroy() {
      this.pressedKeys.clear();
      this.entryMap.deactivateAll();
      this.entryMap.updateNetworks(this.level);
      super.destroy();
   }

   @Nullable
   public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
      return LinkedTypewriterMenuCommon.create(id, inventory, this);
   }

   public Component getDisplayName() {
      return ((LinkedTypewriterBlock)SimBlocks.LINKED_TYPEWRITER.get()).getName();
   }

   public String getClipboardKey() {
      return "TypewriterKeys";
   }

   public boolean writeToClipboard(@NotNull Provider registries, CompoundTag tag, Direction side) {
      tag.put("Keys", this.entryMap.saveKeys(registries));
      return true;
   }

   public boolean readFromClipboard(@NotNull Provider registries, CompoundTag tag, Player player, Direction side, boolean simulate) {
      if (simulate) {
         return true;
      } else {
         this.entryMap = LinkedTypewriterEntries.readKeys(registries, tag.getList("Keys", 10), this.getBlockPos());
         return true;
      }
   }
}

package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.foundation.utility.ControlsUtil;
import dev.simulated_team.simulated.index.SimSoundEvents;
import dev.simulated_team.simulated.mixin.accessor.KeyMappingsAccessor;
import dev.simulated_team.simulated.network.packets.linked_typewriter.TypewriterDisconnectUser;
import dev.simulated_team.simulated.network.packets.linked_typewriter.TypewriterKeyInteractionPacket;
import dev.simulated_team.simulated.network.packets.linked_typewriter.TypewriterKeySavePacket;
import foundry.veil.api.network.VeilPacketManager;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Vector;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class LinkedTypewriterInteractionHandler {
   private static final Vector<Integer> renderPressed = new Vector<>();
   private static final Int2IntMap presetKeys = new Int2IntOpenHashMap();
   private static WeakReference<LinkedTypewriterBlockEntity> TYPEWRITER = new WeakReference<>(null);
   private static LinkedTypewriterInteractionHandler.Mode MODE = LinkedTypewriterInteractionHandler.Mode.IDLE;

   public static void associateTypewriter(LinkedTypewriterBlockEntity be) {
      if (be == null) {
         MODE = LinkedTypewriterInteractionHandler.Mode.IDLE;
         stopInteraction();
      } else {
         MODE = LinkedTypewriterInteractionHandler.Mode.ACTIVE;
      }

      TYPEWRITER = new WeakReference<>(be);
   }

   public static void tick() {
      LinkedTypewriterRenderer.tick();
      if (getMode() == LinkedTypewriterInteractionHandler.Mode.BINDING_FROM_ITEM) {
         LinkedTypewriterItemBindHandler.tick();
      }

      LinkedTypewriterBlockEntity be = TYPEWRITER.get();
      if (be != null) {
         if (getMode() == LinkedTypewriterInteractionHandler.Mode.ACTIVE
            && !LinkedTypewriterBlockEntity.playerInRange(Minecraft.getInstance().player, be.getLevel(), be.getBlockPos())) {
            VeilPacketManager.server().sendPacket(new CustomPacketPayload[]{new TypewriterDisconnectUser(be.getBlockPos())});
            associateTypewriter(null);
         }

         if (getMode() != LinkedTypewriterInteractionHandler.Mode.SCREEN_BINDING && Minecraft.getInstance().screen != null && !be.isRemoved()) {
            VeilPacketManager.server().sendPacket(new CustomPacketPayload[]{new TypewriterDisconnectUser(be.getBlockPos())});
            associateTypewriter(null);
         }
      }
   }

   private static void stopInteraction() {
      LinkedTypewriterRenderer.resetKeys();
      renderPressed.clear();
   }

   public static void onKeyPress(int key, int scanCode, int action, int modifiers) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player != null) {
         LinkedTypewriterBlockEntity be = TYPEWRITER.get();
         if (getMode() == LinkedTypewriterInteractionHandler.Mode.BINDING_FROM_ITEM) {
            LinkedTypewriterItemBindHandler.keyPress(key, scanCode, action, modifiers);
         } else {
            if (getMode() != LinkedTypewriterInteractionHandler.Mode.SCREEN_BINDING) {
               if (be != null && !be.isRemoved()) {
                  LinkedTypewriterEntries.KeyboardEntry frequency = be.getTypewriterEntries().getEntry(key);
                  if (key == 256) {
                     be.disconnectUser();
                     VeilPacketManager.server().sendPacket(new CustomPacketPayload[]{new TypewriterDisconnectUser(be.getBlockPos())});
                     minecraft.setScreen(null);
                  }

                  if (frequency != null) {
                     preventPress(key, scanCode);
                     if (action != 2) {
                        VeilPacketManager.server()
                           .sendPacket(new CustomPacketPayload[]{new TypewriterKeyInteractionPacket(be.getBlockPos(), key, scanCode, action)});
                     }

                     LocalPlayer player = minecraft.player;
                     if (action == 1) {
                        SimSoundEvents.LINKED_TYPEWRITER_TAP.playAt(player.level(), player.blockPosition(), 1.0F, 1.0F, true);
                        checkKeyCodeAndSetPressed(key, true);
                     } else if (action == 0) {
                        SimSoundEvents.LINKED_TYPEWRITER_UNTAP.playAt(player.level(), player.blockPosition(), 1.0F, 1.0F, true);
                        checkKeyCodeAndSetPressed(key, false);
                     }
                  }

                  for (KeyMapping control : ControlsUtil.getControls()) {
                     if (control.matches(key, scanCode)) {
                        control.consumeClick();
                        control.setDown(false);
                        break;
                     }
                  }
               } else {
                  MODE = LinkedTypewriterInteractionHandler.Mode.IDLE;
                  stopInteraction();
               }
            }
         }
      }
   }

   public static void preventPress(int key, int scanCode) {
      for (KeyMapping mapping : Minecraft.getInstance().options.keyMappings) {
         if (mapping.matches(key, scanCode)) {
            mapping.consumeClick();
            mapping.setDown(false);
            break;
         }
      }
   }

   private static void checkKeyCodeAndSetPressed(int keycode, boolean pressed) {
      int indexPressed;
      if (presetKeys.containsKey(keycode)) {
         indexPressed = presetKeys.get(keycode);
      } else {
         RandomSource random = RandomSource.create((long)keycode);
         indexPressed = random.nextInt(13);
      }

      if (pressed) {
         renderPressed.addElement(indexPressed);
      } else {
         renderPressed.removeElement(indexPressed);
      }
   }

   public static LinkedTypewriterInteractionHandler.Mode getMode() {
      return MODE;
   }

   public static void setMode(LinkedTypewriterInteractionHandler.Mode newMode) {
      MODE = newMode;
   }

   public static Vector<Integer> getPressedKeys() {
      return renderPressed;
   }

   public static void sendLinkedControllerData(Level level, BlockPos blockPos, ItemStack item) {
      BlockEntity blockEntity = level.getBlockEntity(blockPos);
      if (blockEntity instanceof LinkedTypewriterBlockEntity) {
         ItemContainerContents linkedControllerData = (ItemContainerContents)item.get(AllDataComponents.LINKED_CONTROLLER_ITEMS);
         List<ItemStack> linkedControllerItems;
         if (linkedControllerData == null) {
            int size = 12;
            ObjectArrayList<ItemStack> emptyData = new ObjectArrayList(12);

            for (int i = 0; i < 12; i++) {
               emptyData.add(ItemStack.EMPTY);
            }

            linkedControllerItems = emptyData;
         } else {
            linkedControllerItems = new ObjectArrayList(linkedControllerData.stream().toList());

            while (linkedControllerItems.size() < 12) {
               linkedControllerItems.add(ItemStack.EMPTY);
            }
         }

         Int2ObjectMap<LinkedTypewriterEntries.KeyboardEntry> newKeyBindings = new Int2ObjectOpenHashMap();
         int controlIndex = 0;

         for (KeyMapping mapping : ControlsUtil.getControls()) {
            int control = ((KeyMappingsAccessor)mapping).getKey().getValue();
            ItemStack first = linkedControllerItems.get(2 * controlIndex);
            ItemStack second = linkedControllerItems.get(2 * controlIndex + 1);
            newKeyBindings.put(control, new LinkedTypewriterEntries.KeyboardEntry(Frequency.of(first), Frequency.of(second), control, blockPos));
            controlIndex++;
         }

         VeilPacketManager.server().sendPacket(new CustomPacketPayload[]{new TypewriterKeySavePacket(newKeyBindings, blockPos, false)});
      }
   }

   static {
      presetKeys.put(81, 0);
      presetKeys.put(87, 1);
      presetKeys.put(69, 2);
      presetKeys.put(65, 6);
      presetKeys.put(83, 7);
      presetKeys.put(68, 8);
      presetKeys.put(265, 4);
      presetKeys.put(263, 10);
      presetKeys.put(264, 11);
      presetKeys.put(262, 12);
      presetKeys.put(32, 13);
      presetKeys.put(48, 12);
      presetKeys.put(320, 12);

      for (int i = 0; i < 9; i++) {
         presetKeys.put(49 + i, i);
         presetKeys.put(321 + i, i);
      }
   }

   public static enum Mode {
      IDLE,
      ACTIVE,
      BIND,
      SCREEN_BINDING,
      BINDING_FROM_ITEM;
   }
}

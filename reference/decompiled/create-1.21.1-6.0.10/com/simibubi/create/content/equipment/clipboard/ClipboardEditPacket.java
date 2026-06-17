package com.simibubi.create.content.equipment.clipboard;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllPackets;
import java.util.List;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.nbt.NBTProcessors;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public record ClipboardEditPacket(int hotbarSlot, @Nullable ClipboardContent clipboardContent, @Nullable BlockPos targetedBlock)
   implements ServerboundPacketPayload {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClipboardEditPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      ClipboardEditPacket::hotbarSlot,
      CatnipStreamCodecBuilders.nullable(ClipboardContent.STREAM_CODEC),
      ClipboardEditPacket::clipboardContent,
      CatnipStreamCodecBuilders.nullable(BlockPos.STREAM_CODEC),
      ClipboardEditPacket::targetedBlock,
      ClipboardEditPacket::new
   );

   public void handle(ServerPlayer sender) {
      ClipboardContent processedContent = clipboardProcessor(this.clipboardContent);
      if (this.targetedBlock != null) {
         Level world = sender.level();
         if (world.isLoaded(this.targetedBlock)) {
            if (sender.canInteractWithBlock(this.targetedBlock, 20.0)) {
               if (world.getBlockEntity(this.targetedBlock) instanceof ClipboardBlockEntity cbe) {
                  PatchedDataComponentMap map = new PatchedDataComponentMap(cbe.components());
                  if (processedContent == null) {
                     map.remove(AllDataComponents.CLIPBOARD_CONTENT);
                  } else {
                     map.set(AllDataComponents.CLIPBOARD_CONTENT, processedContent);
                  }

                  cbe.setComponents(map);
                  cbe.onEditedBy(sender);
               }
            }
         }
      } else {
         ItemStack itemStack = sender.getInventory().getItem(this.hotbarSlot);
         if (AllBlocks.CLIPBOARD.isIn(itemStack)) {
            if (processedContent == null) {
               itemStack.remove(AllDataComponents.CLIPBOARD_CONTENT);
            } else {
               itemStack.set(AllDataComponents.CLIPBOARD_CONTENT, processedContent);
            }
         }
      }
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.CLIPBOARD_EDIT;
   }

   public static ClipboardContent clipboardProcessor(@Nullable ClipboardContent content) {
      if (content == null) {
         return null;
      } else {
         for (List<ClipboardEntry> page : content.pages()) {
            for (ClipboardEntry entry : page) {
               if (NBTProcessors.textComponentHasClickEvent(entry.text)) {
                  return null;
               }
            }
         }

         return content;
      }
   }
}

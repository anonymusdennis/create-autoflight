package com.simibubi.create.content.equipment.toolbox;

import com.simibubi.create.AllPackets;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public record ToolboxEquipPacket(BlockPos toolboxPos, int slot, int hotbarSlot) implements ServerboundPacketPayload {
   public static final StreamCodec<ByteBuf, ToolboxEquipPacket> STREAM_CODEC = StreamCodec.composite(
      CatnipStreamCodecBuilders.nullable(BlockPos.STREAM_CODEC),
      ToolboxEquipPacket::toolboxPos,
      ByteBufCodecs.VAR_INT,
      ToolboxEquipPacket::slot,
      ByteBufCodecs.VAR_INT,
      ToolboxEquipPacket::hotbarSlot,
      ToolboxEquipPacket::new
   );

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.TOOLBOX_EQUIP;
   }

   public void handle(ServerPlayer player) {
      Level world = player.level();
      if (this.toolboxPos == null) {
         ToolboxHandler.unequip(player, this.hotbarSlot, false);
         ToolboxHandler.syncData(player);
      } else {
         BlockEntity blockEntity = world.getBlockEntity(this.toolboxPos);
         double maxRange = ToolboxHandler.getMaxRange(player);
         if (!(
            player.distanceToSqr((double)this.toolboxPos.getX() + 0.5, (double)this.toolboxPos.getY(), (double)this.toolboxPos.getZ() + 0.5)
               > maxRange * maxRange
         )) {
            if (blockEntity instanceof ToolboxBlockEntity toolboxBlockEntity) {
               ToolboxHandler.unequip(player, this.hotbarSlot, false);
               if (this.slot >= 0 && this.slot < 8) {
                  ItemStack playerStack = player.getInventory().getItem(this.hotbarSlot);
                  if (!playerStack.isEmpty() && !ToolboxInventory.canItemsShareCompartment(playerStack, toolboxBlockEntity.inventory.filters.get(this.slot))) {
                     toolboxBlockEntity.inventory.inLimitedMode(inventory -> {
                        ItemStack remainder = ItemHandlerHelper.insertItemStacked(inventory, playerStack, false);
                        if (!remainder.isEmpty()) {
                           remainder = ItemHandlerHelper.insertItemStacked(new ItemReturnInvWrapper(player.getInventory()), remainder, false);
                        }

                        if (remainder.getCount() != playerStack.getCount()) {
                           player.getInventory().setItem(this.hotbarSlot, remainder);
                        }
                     });
                  }

                  CompoundTag compound = player.getPersistentData().getCompound("CreateToolboxData");
                  String key = String.valueOf(this.hotbarSlot);
                  CompoundTag data = new CompoundTag();
                  data.putInt("Slot", this.slot);
                  data.put("Pos", NbtUtils.writeBlockPos(this.toolboxPos));
                  compound.put(key, data);
                  player.getPersistentData().put("CreateToolboxData", compound);
                  toolboxBlockEntity.connectPlayer(this.slot, player, this.hotbarSlot);
                  ToolboxHandler.syncData(player);
               } else {
                  ToolboxHandler.syncData(player);
               }
            }
         }
      }
   }
}

package dev.simulated_team.simulated.network.packets.linked_typewriter;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DataResult.Error;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterEntries;
import dev.simulated_team.simulated.index.SimBlocks;
import foundry.veil.api.network.handler.ServerPacketContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public record TypewriterSaveKeyToItemPacket(InteractionHand hand, LinkedTypewriterEntries.KeyboardEntry entry) implements CustomPacketPayload {
   public static Type<TypewriterSaveKeyToItemPacket> TYPE = new Type(Simulated.path("linked_typewriter_bind_item"));
   public static StreamCodec<RegistryFriendlyByteBuf, TypewriterSaveKeyToItemPacket> CODEC = StreamCodec.composite(
      ByteBufCodecs.INT,
      packet -> packet.hand.ordinal(),
      LinkedTypewriterEntries.KeyboardEntry.STREAM_CODEC,
      TypewriterSaveKeyToItemPacket::entry,
      (h, e) -> new TypewriterSaveKeyToItemPacket(InteractionHand.values()[h], e)
   );

   public void handle(ServerPacketContext context) {
      ServerPlayer player = context.player();
      ItemStack item = player.getItemInHand(this.hand);
      CompoundTag currentTag = new CompoundTag();
      if (item.has(DataComponents.BLOCK_ENTITY_DATA)) {
         currentTag = ((CustomData)item.get(DataComponents.BLOCK_ENTITY_DATA)).copyTag();
      } else {
         currentTag.putString("id", item.getItem().toString());
      }

      RegistryOps<Tag> ops = context.level().registryAccess().createSerializationContext(NbtOps.INSTANCE);
      DataResult<Tag> result = LinkedTypewriterEntries.KeyboardEntry.CODEC.encodeStart(ops, this.entry);
      if (result.isError()) {
         Simulated.LOGGER.warn("Unable to process entry for item saving!: {}", ((Error)result.error().get()).message());
      } else {
         CompoundTag entryTag = (CompoundTag)result.getOrThrow();
         if (!currentTag.contains("Keys")) {
            currentTag.put("Keys", new ListTag());
         }

         ListTag keys = currentTag.getList("Keys", 10);
         boolean alreadyPresent = false;

         for (int i = 0; i < keys.size(); i++) {
            Tag key = keys.get(i);
            int glfwKey = ((CompoundTag)key).getInt("GLFWKey");
            if (glfwKey == this.entry.glfwKeyCode) {
               alreadyPresent = true;
               keys.set(i, entryTag);
               break;
            }
         }

         if (!alreadyPresent) {
            keys.add(entryTag);
         }

         currentTag.put("Keys", keys);
         if (item.is(SimBlocks.LINKED_TYPEWRITER.asItem())) {
            CustomData.set(DataComponents.BLOCK_ENTITY_DATA, item, currentTag);
         }
      }
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}

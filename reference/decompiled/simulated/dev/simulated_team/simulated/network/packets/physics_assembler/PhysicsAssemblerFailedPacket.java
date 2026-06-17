package dev.simulated_team.simulated.network.packets.physics_assembler;

import dev.ryanhcode.sable.Sable;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import dev.simulated_team.simulated.index.SimSoundEvents;
import foundry.veil.api.network.handler.ClientPacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public record PhysicsAssemblerFailedPacket(BlockPos pos) implements CustomPacketPayload {
   public static Type<PhysicsAssemblerFailedPacket> TYPE = new Type(Simulated.path("assembler_failed"));
   public static StreamCodec<ByteBuf, PhysicsAssemblerFailedPacket> CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC, PhysicsAssemblerFailedPacket::pos, PhysicsAssemblerFailedPacket::new
   );

   @NotNull
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public void handle(ClientPacketContext context) {
      Level level = context.level();

      assert level != null;

      if (level.getBlockEntity(this.pos) instanceof PhysicsAssemblerBlockEntity blockEntity) {
         blockEntity.clientFlickLeverTo(Sable.HELPER.getContaining(level, this.pos) != null);
         blockEntity.setClientHoldLeverInPlace(false);
         SimSoundEvents.ASSEMBLER_FAIL.playAt(level, this.pos, 1.0F, 1.0F, false);
      }
   }
}

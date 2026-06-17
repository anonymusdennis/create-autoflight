package dev.simulated_team.simulated.content.physics_staff;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.minecraft.network.codec.StreamCodec;

public enum PhysicsStaffAction {
   STOP_DRAG,
   LOCK,
   START_DRAG;

   public static final StreamCodec<ByteBuf, PhysicsStaffAction> STREAM_CODEC = CatnipStreamCodecBuilders.ofEnum(PhysicsStaffAction.class);
}

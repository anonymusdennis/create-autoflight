package com.simibubi.create.content.equipment.zapper;

import com.simibubi.create.AllPackets;
import com.simibubi.create.CreateClient;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecs;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ZapperBeamPacket extends ShootGadgetPacket {
   public static final StreamCodec<ByteBuf, ZapperBeamPacket> STREAM_CODEC = StreamCodec.composite(
      CatnipStreamCodecs.VEC3,
      packet -> packet.location,
      CatnipStreamCodecs.HAND,
      packet -> packet.hand,
      ByteBufCodecs.BOOL,
      packet -> packet.self,
      CatnipStreamCodecs.VEC3,
      packet -> packet.target,
      ZapperBeamPacket::new
   );
   private final Vec3 target;

   public ZapperBeamPacket(Vec3 start, InteractionHand hand, boolean self, Vec3 target) {
      super(start, hand, self);
      this.target = target;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   protected ShootableGadgetRenderHandler getHandler() {
      return CreateClient.ZAPPER_RENDER_HANDLER;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   protected void handleAdditional() {
      CreateClient.ZAPPER_RENDER_HANDLER.addBeam(new ZapperRenderHandler.LaserBeam(this.location, this.target));
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.BEAM_EFFECT;
   }
}

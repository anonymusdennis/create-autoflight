package com.simibubi.create.content.fluids.transfer;

import com.simibubi.create.AllPackets;
import com.simibubi.create.content.fluids.FluidFX;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

public record FluidSplashPacket(BlockPos pos, FluidStack fluid) implements ClientboundPacketPayload {
   public static final StreamCodec<RegistryFriendlyByteBuf, FluidSplashPacket> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC, FluidSplashPacket::pos, FluidStack.OPTIONAL_STREAM_CODEC, FluidSplashPacket::fluid, FluidSplashPacket::new
   );

   @OnlyIn(Dist.CLIENT)
   public void handle(LocalPlayer player) {
      if (!(player.position().distanceTo(new Vec3((double)this.pos.getX(), (double)this.pos.getY(), (double)this.pos.getZ())) > 100.0)) {
         FluidFX.splash(this.pos, this.fluid);
      }
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.FLUID_SPLASH;
   }
}

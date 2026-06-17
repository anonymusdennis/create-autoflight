package com.simibubi.create.api.contraption.train;

import com.simibubi.create.api.registry.SimpleRegistry;
import com.simibubi.create.content.trains.track.AllPortalTracks;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface PortalTrackProvider {
   SimpleRegistry<Block, PortalTrackProvider> REGISTRY = SimpleRegistry.create();

   PortalTrackProvider.Exit findExit(ServerLevel var1, BlockFace var2);

   static boolean isSupportedPortal(BlockState state) {
      return REGISTRY.get(state) != null;
   }

   @Nullable
   static PortalTrackProvider.Exit getOtherSide(ServerLevel level, BlockFace inboundTrack) {
      BlockPos portalPos = inboundTrack.getConnectedPos();
      BlockState portalState = level.getBlockState(portalPos);
      PortalTrackProvider provider = REGISTRY.get(portalState);
      return provider == null ? null : provider.findExit(level, inboundTrack);
   }

   static PortalTrackProvider.Exit fromPortal(
      ServerLevel level, BlockFace face, ResourceKey<Level> firstDimension, ResourceKey<Level> secondDimension, Portal portal
   ) {
      return AllPortalTracks.fromPortal(level, face, firstDimension, secondDimension, portal);
   }

   public static record Exit(ServerLevel level, BlockFace face) {
   }
}

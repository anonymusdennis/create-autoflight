package com.simibubi.create.content.trains.track;

import com.simibubi.create.Create;
import com.simibubi.create.api.contraption.train.PortalTrackProvider;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;

public class AllPortalTracks {
   public static void tryRegisterIntegration(ResourceLocation id, PortalTrackProvider provider) {
      if (BuiltInRegistries.BLOCK.containsKey(id)) {
         Block block = (Block)BuiltInRegistries.BLOCK.get(id);
         PortalTrackProvider.REGISTRY.register(block, provider);
      } else {
         Create.LOGGER.warn("Portal for integration wasn't found: {}. Compat outdated?", id);
      }
   }

   private static void tryRegisterSimpleInteraction(ResourceLocation portalBlockId, ResourceLocation dimensionId) {
      ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, dimensionId);
      tryRegisterSimpleInteraction(portalBlockId, levelKey);
   }

   private static void tryRegisterSimpleInteraction(ResourceLocation portalBlockId, ResourceKey<Level> levelKey) {
      tryRegisterSimpleInteraction((Block)BuiltInRegistries.BLOCK.get(portalBlockId), levelKey);
   }

   private static void tryRegisterSimpleInteraction(Block portalBlock, ResourceKey<Level> levelKey) {
      PortalTrackProvider p = (level, face) -> PortalTrackProvider.fromPortal(level, face, Level.OVERWORLD, levelKey, (Portal)portalBlock);
      PortalTrackProvider.REGISTRY.register(portalBlock, p);
   }

   public static void registerDefaults() {
      tryRegisterSimpleInteraction(Blocks.NETHER_PORTAL, Level.NETHER);
      if (Mods.AETHER.isLoaded()) {
         tryRegisterSimpleInteraction(Mods.AETHER.rl("aether_portal"), Mods.AETHER.rl("the_aether"));
      }

      if (Mods.AETHER_II.isLoaded()) {
         tryRegisterSimpleInteraction(Mods.AETHER_II.rl("aether_portal"), Mods.AETHER_II.rl("aether_highlands"));
      }

      if (Mods.BETTEREND.isLoaded()) {
         tryRegisterSimpleInteraction(Mods.BETTEREND.rl("end_portal_block"), Level.END);
      }
   }

   public static PortalTrackProvider.Exit fromPortal(
      ServerLevel level, BlockFace inboundTrack, ResourceKey<Level> firstDimension, ResourceKey<Level> secondDimension, Portal portal
   ) {
      ResourceKey<Level> resourceKey = level.dimension() == secondDimension ? firstDimension : secondDimension;
      MinecraftServer minecraftServer = level.getServer();
      ServerLevel otherLevel = minecraftServer.getLevel(resourceKey);
      if (otherLevel == null) {
         return null;
      } else {
         BlockPos portalPos = inboundTrack.getConnectedPos();
         BlockState portalState = level.getBlockState(portalPos);
         SuperGlueEntity probe = new SuperGlueEntity(level, new AABB(portalPos));
         probe.setYRot(inboundTrack.getFace().toYRot());
         DimensionTransition dimensiontransition = portal.getPortalDestination(level, probe, probe.blockPosition());
         if (dimensiontransition == null) {
            return null;
         } else if (!minecraftServer.isLevelEnabled(dimensiontransition.newLevel())) {
            return null;
         } else {
            BlockPos otherPortalPos = BlockPos.containing(dimensiontransition.pos());
            BlockState otherPortalState = otherLevel.getBlockState(otherPortalPos);
            if (!otherPortalState.is(portalState.getBlock())) {
               return null;
            } else {
               Direction targetDirection = inboundTrack.getFace();
               if (targetDirection.getAxis() == otherPortalState.getValue(BlockStateProperties.HORIZONTAL_AXIS)) {
                  targetDirection = targetDirection.getClockWise();
               }

               BlockPos otherPos = otherPortalPos.relative(targetDirection);
               return new PortalTrackProvider.Exit(otherLevel, new BlockFace(otherPos, targetDirection.getOpposite()));
            }
         }
      }
   }
}

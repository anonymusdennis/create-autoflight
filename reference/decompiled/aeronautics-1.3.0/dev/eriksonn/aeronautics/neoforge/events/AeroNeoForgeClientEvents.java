package dev.eriksonn.aeronautics.neoforge.events;

import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.events.AeronauticsClientEvents;
import dev.eriksonn.aeronautics.index.AeroBlocks;
import dev.eriksonn.aeronautics.index.client.AeroRenderTypes;
import dev.eriksonn.aeronautics.mixin.levitite.ChunkRenderTypeSetAccessor;
import dev.eriksonn.aeronautics.neoforge.content.fluids.AeroFluidType;
import dev.eriksonn.aeronautics.neoforge.index.AeroFluidsNeoForge;
import java.util.List;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.ClientTickEvent.Pre;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.RegisterStageEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.fluids.FluidType;

@EventBusSubscriber(
   modid = "aeronautics",
   value = {Dist.CLIENT}
)
public class AeroNeoForgeClientEvents {
   @SubscribeEvent
   public static void preClientTick(Pre event) {
      AeronauticsClientEvents.clientLevelTick(false);
   }

   @SubscribeEvent
   public static void postClientTick(Post event) {
      AeronauticsClientEvents.clientLevelTick(true);
   }

   @EventBusSubscriber(
      modid = "aeronautics",
      value = {Dist.CLIENT}
   )
   public static class ModBusEvents {
      @SubscribeEvent
      public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
         AeroFluidType type = (AeroFluidType)AeroFluidsNeoForge.LEVITITE_BLEND.getType();
         event.registerFluidType(type, new FluidType[]{type});
      }

      @SubscribeEvent
      public static void clientSetup(FMLClientSetupEvent event) {
         ChunkRenderTypeSet set = ChunkRenderTypeSet.of(new RenderType[]{RenderType.SOLID, AeroRenderTypes.levitite(), AeroRenderTypes.levititeGhosts()});
         ItemBlockRenderTypes.setRenderLayer((Block)AeroBlocks.LEVITITE.get(), set);
         ItemBlockRenderTypes.setRenderLayer((Block)AeroBlocks.PEARLESCENT_LEVITITE.get(), set);
         fixChunkRenderTypeSet();
      }

      private static void fixChunkRenderTypeSet() {
         List<RenderType> list = RenderType.chunkBufferLayers();
         ChunkRenderTypeSetAccessor.setChunkRenderTypesList(list);
         ChunkRenderTypeSetAccessor.setChunkRenderTypes(list.toArray(new RenderType[0]));
         ((ChunkRenderTypeSetAccessor)ChunkRenderTypeSet.all()).getBits().set(0, list.size());
      }

      @SubscribeEvent
      public static void registerRegisterStageEvent(RegisterStageEvent event) {
         event.register(Aeronautics.path("levitite"), AeroRenderTypes.levitite());
         event.register(Aeronautics.path("levitite_ghosts"), AeroRenderTypes.levititeGhosts());
      }
   }
}

package dev.eriksonn.aeronautics.mixin.levitite;

import java.util.BitSet;
import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ChunkRenderTypeSet.class})
public interface ChunkRenderTypeSetAccessor {
   @Mutable
   @Accessor("CHUNK_RENDER_TYPES_LIST")
   static void setChunkRenderTypesList(List<RenderType> data) {
      throw new AssertionError("Something has gone terribly wrong.");
   }

   @Accessor("CHUNK_RENDER_TYPES")
   @Mutable
   static void setChunkRenderTypes(RenderType[] data) {
      throw new AssertionError("Something has gone terribly wrong.");
   }

   @Accessor("bits")
   BitSet getBits();
}

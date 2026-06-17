package dev.ryanhcode.sable.sublevel.render.dispatcher;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.block.DynamicShaderBlock;
import foundry.veil.api.client.render.shader.block.ShaderBlock;
import foundry.veil.api.client.render.shader.block.ShaderBlock.BufferBinding;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Arrays;
import java.util.Objects;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.lwjgl.system.NativeResource;

public class SubLevelTextureCache implements NativeResource {
   private static final int SPRITE_SIZE = 32;
   private static final int DEFAULT_SPRITE_COUNT = 32;
   private final Object2IntMap<SubLevelTextureCache.PackedTexture> textures = Object2IntMaps.synchronize(new Object2IntOpenHashMap());
   private final Object2IntMap<SubLevelTextureCache.PackedTexture> newTextures = new Object2IntArrayMap();
   private DynamicShaderBlock<SubLevelTextureCache.PackedTexture[]> textureBlock = null;

   public SubLevelTextureCache() {
      VeilRenderSystem.renderer().getShaderDefinitions().set("SABLE_TEXTURE_CACHE_SIZE", Integer.toString(32));
   }

   public int getTextureId(BakedQuad quad) {
      int[] vertices = quad.getVertices();
      float u0 = Float.intBitsToFloat(vertices[4]);
      float v0 = Float.intBitsToFloat(vertices[5]);
      float u1 = Float.intBitsToFloat(vertices[12]);
      float v1 = Float.intBitsToFloat(vertices[13]);
      float u2 = Float.intBitsToFloat(vertices[20]);
      float v2 = Float.intBitsToFloat(vertices[21]);
      float u3 = Float.intBitsToFloat(vertices[28]);
      float v3 = Float.intBitsToFloat(vertices[29]);
      return this.textures.computeIfAbsent(new SubLevelTextureCache.PackedTexture(u0, v0, u1, v1, u2, v2, u3, v3), texture -> {
         int textureId = this.textures.size();
         this.newTextures.put((SubLevelTextureCache.PackedTexture)texture, textureId);
         return textureId;
      });
   }

   public void flush() {
      if (!this.newTextures.isEmpty()) {
         if (this.textureBlock == null) {
            this.textureBlock = ShaderBlock.dynamic(BufferBinding.UNIFORM, 1024, (packedTexturesx, byteBuffer) -> {
               for (SubLevelTextureCache.PackedTexture texture : packedTexturesx) {
                  if (texture == null) {
                     break;
                  }

                  byteBuffer.putFloat(texture.u0);
                  byteBuffer.putFloat(texture.u1);
                  byteBuffer.putFloat(texture.u2);
                  byteBuffer.putFloat(texture.u3);
                  byteBuffer.putFloat(texture.v0);
                  byteBuffer.putFloat(texture.v1);
                  byteBuffer.putFloat(texture.v2);
                  byteBuffer.putFloat(texture.v3);
               }
            });
            this.textureBlock.set(new SubLevelTextureCache.PackedTexture[32]);
         }

         int expectedSize = this.textures.size() + this.newTextures.size();
         if (expectedSize * 32 > this.textureBlock.getSize()) {
            int newSize = (int)((double)expectedSize * 1.5);
            this.textureBlock.setSize(newSize * 32);
            SubLevelTextureCache.PackedTexture[] packedTextures = Objects.requireNonNull((SubLevelTextureCache.PackedTexture[])this.textureBlock.getValue());
            this.textureBlock.set(Arrays.copyOf(packedTextures, newSize));
            VeilRenderSystem.renderer().getShaderDefinitions().set("SABLE_TEXTURE_CACHE_SIZE", Long.toString((long)newSize));
         }

         SubLevelTextureCache.PackedTexture[] packedTextures = Objects.requireNonNull((SubLevelTextureCache.PackedTexture[])this.textureBlock.getValue());
         ObjectIterator var6 = this.newTextures.object2IntEntrySet().iterator();

         while (var6.hasNext()) {
            Entry<SubLevelTextureCache.PackedTexture> entry = (Entry<SubLevelTextureCache.PackedTexture>)var6.next();
            packedTextures[entry.getIntValue()] = (SubLevelTextureCache.PackedTexture)entry.getKey();
         }

         this.newTextures.clear();
         this.textureBlock.set(packedTextures);
      }
   }

   public void bind() {
      this.flush();
      if (this.textureBlock != null) {
         VeilRenderSystem.bind("SableSprites", this.textureBlock);
      }
   }

   public void free() {
      if (this.textureBlock != null) {
         VeilRenderSystem.unbind(this.textureBlock);
         this.textureBlock.free();
         this.textureBlock = null;
      }
   }

   private static record PackedTexture(float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3) {
   }
}

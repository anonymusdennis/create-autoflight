package dev.engine_room.flywheel.lib.model.baked;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.blaze3d.vertex.BufferBuilder;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.model.SimpleModel;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import java.util.function.BiFunction;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

class MeshEmitterManager<T extends MeshEmitter> {
   private static final RenderType[] CHUNK_LAYERS = RenderType.chunkBufferLayers().toArray(RenderType[]::new);
   private final Reference2ReferenceMap<RenderType, T> emitterMap = new Reference2ReferenceArrayMap();
   private final ByteBufferBuilderStack byteBufferBuilderStack = new ByteBufferBuilderStack();
   @UnknownNullability
   private BlockMaterialFunction blockMaterialFunction;

   MeshEmitterManager(BiFunction<ByteBufferBuilderStack, RenderType, T> meshEmitterFactory) {
      for (RenderType renderType : CHUNK_LAYERS) {
         this.emitterMap.put(renderType, meshEmitterFactory.apply(this.byteBufferBuilderStack, renderType));
      }
   }

   public T getEmitter(RenderType renderType) {
      return (T)this.emitterMap.get(renderType);
   }

   public void prepare(BlockMaterialFunction blockMaterialFunction) {
      this.blockMaterialFunction = blockMaterialFunction;
      this.byteBufferBuilderStack.reset();
      ObjectIterator var2 = this.emitterMap.values().iterator();

      while (var2.hasNext()) {
         MeshEmitter emitter = (MeshEmitter)var2.next();
         emitter.prepare(blockMaterialFunction);
      }
   }

   public void prepareForBlock() {
      ObjectIterator var1 = this.emitterMap.values().iterator();

      while (var1.hasNext()) {
         MeshEmitter emitter = (MeshEmitter)var1.next();
         emitter.prepareForBlock();
      }
   }

   public SimpleModel end() {
      this.blockMaterialFunction = null;
      Builder<Model.ConfiguredMesh> meshes = ImmutableList.builder();
      ObjectIterator var2 = this.emitterMap.values().iterator();

      while (var2.hasNext()) {
         MeshEmitter emitter = (MeshEmitter)var2.next();
         emitter.end(meshes);
      }

      return new SimpleModel(meshes.build());
   }

   @Nullable
   public BufferBuilder getBuffer(RenderType renderType, boolean shade, boolean ao) {
      Material key = this.blockMaterialFunction.apply(renderType, shade, ao);
      return key != null ? ((MeshEmitter)this.emitterMap.get(renderType)).getBuffer(key) : null;
   }
}

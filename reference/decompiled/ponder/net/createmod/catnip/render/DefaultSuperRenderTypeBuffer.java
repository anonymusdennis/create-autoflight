package net.createmod.catnip.render;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import java.util.SortedMap;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.resources.model.ModelBakery;

public class DefaultSuperRenderTypeBuffer implements SuperRenderTypeBuffer {
   private static final DefaultSuperRenderTypeBuffer INSTANCE = new DefaultSuperRenderTypeBuffer();
   protected DefaultSuperRenderTypeBuffer.SuperRenderTypeBufferPhase earlyBuffer = new DefaultSuperRenderTypeBuffer.SuperRenderTypeBufferPhase();
   protected DefaultSuperRenderTypeBuffer.SuperRenderTypeBufferPhase defaultBuffer = new DefaultSuperRenderTypeBuffer.SuperRenderTypeBufferPhase();
   protected DefaultSuperRenderTypeBuffer.SuperRenderTypeBufferPhase lateBuffer = new DefaultSuperRenderTypeBuffer.SuperRenderTypeBufferPhase();

   public static DefaultSuperRenderTypeBuffer getInstance() {
      return INSTANCE;
   }

   @Override
   public VertexConsumer getEarlyBuffer(RenderType type) {
      return this.earlyBuffer.bufferSource.getBuffer(type);
   }

   @Override
   public VertexConsumer getBuffer(RenderType type) {
      return this.defaultBuffer.bufferSource.getBuffer(type);
   }

   @Override
   public VertexConsumer getLateBuffer(RenderType type) {
      return this.lateBuffer.bufferSource.getBuffer(type);
   }

   @Override
   public void draw() {
      this.earlyBuffer.bufferSource.endBatch();
      this.defaultBuffer.bufferSource.endBatch();
      this.lateBuffer.bufferSource.endBatch();
   }

   @Override
   public void draw(RenderType type) {
      this.earlyBuffer.bufferSource.endBatch(type);
      this.defaultBuffer.bufferSource.endBatch(type);
      this.lateBuffer.bufferSource.endBatch(type);
   }

   public static class SuperRenderTypeBufferPhase {
      private final SectionBufferBuilderPack fixedBufferPack = new SectionBufferBuilderPack();
      private final SortedMap<RenderType, ByteBufferBuilder> fixedBuffers = (SortedMap<RenderType, ByteBufferBuilder>)Util.make(
         new Object2ObjectLinkedOpenHashMap(), map -> {
            map.put(Sheets.solidBlockSheet(), this.fixedBufferPack.buffer(RenderType.solid()));
            map.put(Sheets.cutoutBlockSheet(), this.fixedBufferPack.buffer(RenderType.cutout()));
            map.put(Sheets.bannerSheet(), this.fixedBufferPack.buffer(RenderType.cutoutMipped()));
            map.put(Sheets.translucentCullBlockSheet(), this.fixedBufferPack.buffer(RenderType.translucent()));
            put(map, Sheets.shieldSheet());
            put(map, Sheets.bedSheet());
            put(map, Sheets.shulkerBoxSheet());
            put(map, Sheets.signSheet());
            put(map, Sheets.hangingSignSheet());
            map.put(Sheets.chestSheet(), new ByteBufferBuilder(786432));
            put(map, RenderType.armorEntityGlint());
            put(map, RenderType.glint());
            put(map, RenderType.glintTranslucent());
            put(map, RenderType.entityGlint());
            put(map, RenderType.entityGlintDirect());
            put(map, RenderType.waterMask());
            ModelBakery.DESTROY_TYPES.forEach(renderType -> put(map, renderType));
            put(map, PonderRenderTypes.outlineSolid());
         }
      );
      private final BufferSource bufferSource = MultiBufferSource.immediateWithBuffers(this.fixedBuffers, new ByteBufferBuilder(256));

      private static void put(Object2ObjectLinkedOpenHashMap<RenderType, ByteBufferBuilder> map, RenderType type) {
         map.put(type, new ByteBufferBuilder(type.bufferSize()));
      }
   }
}

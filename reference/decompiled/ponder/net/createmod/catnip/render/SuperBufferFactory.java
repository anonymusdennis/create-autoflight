package net.createmod.catnip.render;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.lib.model.baked.EmptyVirtualBlockGetter;
import javax.annotation.Nullable;
import net.createmod.catnip.client.render.model.BakedModelBufferer;
import net.createmod.catnip.client.render.model.ShadeSeparatedResultConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class SuperBufferFactory {
   private static final ThreadLocal<SuperBufferFactory.ThreadLocalObjects> THREAD_LOCAL_OBJECTS = ThreadLocal.withInitial(
      SuperBufferFactory.ThreadLocalObjects::new
   );
   private static SuperBufferFactory instance = new SuperBufferFactory();

   public static SuperBufferFactory getInstance() {
      return instance;
   }

   static void setInstance(SuperBufferFactory factory) {
      instance = factory;
   }

   public SuperByteBuffer create(MeshData data) {
      return new ShadeSeparatingSuperByteBuffer(new MutableTemplateMesh(data).toImmutable());
   }

   public SuperByteBuffer createForBlock(BlockState renderedState) {
      return this.createForBlock(Minecraft.getInstance().getBlockRenderer().getBlockModel(renderedState), renderedState);
   }

   public SuperByteBuffer createForBlock(BakedModel model, BlockState referenceState) {
      return this.createForBlock(model, referenceState, new PoseStack());
   }

   public SuperByteBuffer createForBlock(BakedModel model, BlockState state, @Nullable PoseStack poseStack) {
      SuperBufferFactory.ThreadLocalObjects objects = THREAD_LOCAL_OBJECTS.get();
      SuperBufferFactory.SbbBuilder sbbBuilder = objects.sbbBuilder;
      sbbBuilder.prepare();
      BakedModelBufferer.bufferModel(model, BlockPos.ZERO, EmptyVirtualBlockGetter.FULL_DARK, state, poseStack, sbbBuilder);
      return sbbBuilder.build();
   }

   private static class SbbBuilder extends SuperByteBufferBuilder implements ShadeSeparatedResultConsumer {
      @Override
      public void accept(RenderType renderType, boolean shaded, MeshData data) {
         this.add(data, shaded);
      }
   }

   private static class ThreadLocalObjects {
      public final SuperBufferFactory.SbbBuilder sbbBuilder = new SuperBufferFactory.SbbBuilder();
   }
}

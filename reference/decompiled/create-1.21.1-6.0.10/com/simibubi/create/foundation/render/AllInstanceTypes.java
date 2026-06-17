package com.simibubi.create.foundation.render;

import com.simibubi.create.Create;
import com.simibubi.create.content.fluids.FluidInstance;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.processing.burner.ScrollInstance;
import com.simibubi.create.content.processing.burner.ScrollTransformedInstance;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.layout.FloatRepr;
import dev.engine_room.flywheel.api.layout.IntegerRepr;
import dev.engine_room.flywheel.api.layout.LayoutBuilder;
import dev.engine_room.flywheel.lib.instance.SimpleInstanceType;
import dev.engine_room.flywheel.lib.util.ExtraMemoryOps;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.system.MemoryUtil;

@OnlyIn(Dist.CLIENT)
public class AllInstanceTypes {
   public static final InstanceType<RotatingInstance> ROTATING = SimpleInstanceType.builder(RotatingInstance::new)
      .cullShader(Create.asResource("instance/cull/rotating.glsl"))
      .vertexShader(Create.asResource("instance/rotating.vert"))
      .layout(
         LayoutBuilder.create()
            .vector("color", FloatRepr.NORMALIZED_UNSIGNED_BYTE, 4)
            .vector("light", IntegerRepr.SHORT, 2)
            .vector("overlay", IntegerRepr.SHORT, 2)
            .vector("rotation", FloatRepr.FLOAT, 4)
            .vector("pos", FloatRepr.FLOAT, 3)
            .scalar("speed", FloatRepr.FLOAT)
            .scalar("offset", FloatRepr.FLOAT)
            .vector("axis", FloatRepr.NORMALIZED_BYTE, 3)
            .build()
      )
      .writer((ptr, instance) -> {
         MemoryUtil.memPutByte(ptr, instance.red);
         MemoryUtil.memPutByte(ptr + 1L, instance.green);
         MemoryUtil.memPutByte(ptr + 2L, instance.blue);
         MemoryUtil.memPutByte(ptr + 3L, instance.alpha);
         ExtraMemoryOps.put2x16(ptr + 4L, instance.light);
         ExtraMemoryOps.put2x16(ptr + 8L, instance.overlay);
         ExtraMemoryOps.putQuaternionf(ptr + 12L, instance.rotation);
         MemoryUtil.memPutFloat(ptr + 28L, instance.x);
         MemoryUtil.memPutFloat(ptr + 32L, instance.y);
         MemoryUtil.memPutFloat(ptr + 36L, instance.z);
         MemoryUtil.memPutFloat(ptr + 40L, instance.rotationalSpeed);
         MemoryUtil.memPutFloat(ptr + 44L, instance.rotationOffset);
         MemoryUtil.memPutByte(ptr + 48L, instance.rotationAxisX);
         MemoryUtil.memPutByte(ptr + 49L, instance.rotationAxisY);
         MemoryUtil.memPutByte(ptr + 50L, instance.rotationAxisZ);
      })
      .build();
   public static final InstanceType<ScrollInstance> SCROLLING = SimpleInstanceType.builder(ScrollInstance::new)
      .cullShader(Create.asResource("instance/cull/scrolling.glsl"))
      .vertexShader(Create.asResource("instance/scrolling.vert"))
      .layout(
         LayoutBuilder.create()
            .vector("color", FloatRepr.NORMALIZED_UNSIGNED_BYTE, 4)
            .vector("light", IntegerRepr.SHORT, 2)
            .vector("overlay", IntegerRepr.SHORT, 2)
            .vector("pos", FloatRepr.FLOAT, 3)
            .vector("rotation", FloatRepr.FLOAT, 4)
            .vector("speed", FloatRepr.FLOAT, 2)
            .vector("diff", FloatRepr.FLOAT, 2)
            .vector("scale", FloatRepr.FLOAT, 2)
            .vector("offset", FloatRepr.FLOAT, 2)
            .build()
      )
      .writer((ptr, instance) -> {
         MemoryUtil.memPutByte(ptr, instance.red);
         MemoryUtil.memPutByte(ptr + 1L, instance.green);
         MemoryUtil.memPutByte(ptr + 2L, instance.blue);
         MemoryUtil.memPutByte(ptr + 3L, instance.alpha);
         ExtraMemoryOps.put2x16(ptr + 4L, instance.light);
         ExtraMemoryOps.put2x16(ptr + 8L, instance.overlay);
         MemoryUtil.memPutFloat(ptr + 12L, instance.x);
         MemoryUtil.memPutFloat(ptr + 16L, instance.y);
         MemoryUtil.memPutFloat(ptr + 20L, instance.z);
         ExtraMemoryOps.putQuaternionf(ptr + 24L, instance.rotation);
         MemoryUtil.memPutFloat(ptr + 40L, instance.speedU);
         MemoryUtil.memPutFloat(ptr + 44L, instance.speedV);
         MemoryUtil.memPutFloat(ptr + 48L, instance.diffU);
         MemoryUtil.memPutFloat(ptr + 52L, instance.diffV);
         MemoryUtil.memPutFloat(ptr + 56L, instance.scaleU);
         MemoryUtil.memPutFloat(ptr + 60L, instance.scaleV);
         MemoryUtil.memPutFloat(ptr + 64L, instance.offsetU);
         MemoryUtil.memPutFloat(ptr + 68L, instance.offsetV);
      })
      .build();
   public static final InstanceType<ScrollTransformedInstance> SCROLLING_TRANSFORMED = SimpleInstanceType.builder(ScrollTransformedInstance::new)
      .cullShader(Create.asResource("instance/cull/scrolling_transformed.glsl"))
      .vertexShader(Create.asResource("instance/scrolling_transformed.vert"))
      .layout(
         LayoutBuilder.create()
            .matrix("pose", FloatRepr.FLOAT, 4)
            .vector("color", FloatRepr.NORMALIZED_UNSIGNED_BYTE, 4)
            .vector("light", IntegerRepr.SHORT, 2)
            .vector("overlay", IntegerRepr.SHORT, 2)
            .vector("speed", FloatRepr.FLOAT, 2)
            .vector("diff", FloatRepr.FLOAT, 2)
            .vector("scale", FloatRepr.FLOAT, 2)
            .vector("offset", FloatRepr.FLOAT, 2)
            .build()
      )
      .writer((ptr, instance) -> {
         ExtraMemoryOps.putMatrix4f(ptr, instance.pose);
         MemoryUtil.memPutByte(ptr + 64L, instance.red);
         MemoryUtil.memPutByte(ptr + 65L, instance.green);
         MemoryUtil.memPutByte(ptr + 66L, instance.blue);
         MemoryUtil.memPutByte(ptr + 67L, instance.alpha);
         ExtraMemoryOps.put2x16(ptr + 68L, instance.light);
         ExtraMemoryOps.put2x16(ptr + 72L, instance.overlay);
         MemoryUtil.memPutFloat(ptr + 76L, instance.speedU);
         MemoryUtil.memPutFloat(ptr + 80L, instance.speedV);
         MemoryUtil.memPutFloat(ptr + 84L, instance.diffU);
         MemoryUtil.memPutFloat(ptr + 88L, instance.diffV);
         MemoryUtil.memPutFloat(ptr + 92L, instance.scaleU);
         MemoryUtil.memPutFloat(ptr + 96L, instance.scaleV);
         MemoryUtil.memPutFloat(ptr + 100L, instance.offsetU);
         MemoryUtil.memPutFloat(ptr + 104L, instance.offsetV);
      })
      .build();
   public static final InstanceType<FluidInstance> FLUID = SimpleInstanceType.builder(FluidInstance::new)
      .cullShader(Create.asResource("instance/cull/fluid.glsl"))
      .vertexShader(Create.asResource("instance/fluid.vert"))
      .layout(
         LayoutBuilder.create()
            .matrix("pose", FloatRepr.FLOAT, 4)
            .vector("color", FloatRepr.NORMALIZED_UNSIGNED_BYTE, 4)
            .vector("light", IntegerRepr.SHORT, 2)
            .vector("overlay", IntegerRepr.SHORT, 2)
            .scalar("progress", FloatRepr.FLOAT)
            .scalar("vScale", FloatRepr.FLOAT)
            .scalar("v0", FloatRepr.FLOAT)
            .build()
      )
      .writer((ptr, instance) -> {
         ExtraMemoryOps.putMatrix4f(ptr, instance.pose);
         MemoryUtil.memPutByte(ptr + 64L, instance.red);
         MemoryUtil.memPutByte(ptr + 65L, instance.green);
         MemoryUtil.memPutByte(ptr + 66L, instance.blue);
         MemoryUtil.memPutByte(ptr + 67L, instance.alpha);
         ExtraMemoryOps.put2x16(ptr + 68L, instance.light);
         ExtraMemoryOps.put2x16(ptr + 72L, instance.overlay);
         MemoryUtil.memPutFloat(ptr + 76L, instance.progress);
         MemoryUtil.memPutFloat(ptr + 80L, instance.vScale);
         MemoryUtil.memPutFloat(ptr + 84L, instance.v0);
      })
      .build();

   public static void init() {
   }
}

package dev.engine_room.flywheel.lib.instance;

import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.layout.FloatRepr;
import dev.engine_room.flywheel.api.layout.IntegerRepr;
import dev.engine_room.flywheel.api.layout.LayoutBuilder;
import dev.engine_room.flywheel.lib.util.ExtraMemoryOps;
import dev.engine_room.flywheel.lib.util.ResourceUtil;
import org.lwjgl.system.MemoryUtil;

public final class InstanceTypes {
   public static final InstanceType<TransformedInstance> TRANSFORMED = SimpleInstanceType.builder(TransformedInstance::new)
      .layout(
         LayoutBuilder.create()
            .vector("color", FloatRepr.NORMALIZED_UNSIGNED_BYTE, 4)
            .vector("overlay", IntegerRepr.SHORT, 2)
            .vector("light", FloatRepr.UNSIGNED_SHORT, 2)
            .matrix("pose", FloatRepr.FLOAT, 4)
            .build()
      )
      .writer((ptr, instance) -> {
         MemoryUtil.memPutByte(ptr, instance.red);
         MemoryUtil.memPutByte(ptr + 1L, instance.green);
         MemoryUtil.memPutByte(ptr + 2L, instance.blue);
         MemoryUtil.memPutByte(ptr + 3L, instance.alpha);
         ExtraMemoryOps.put2x16(ptr + 4L, instance.overlay);
         ExtraMemoryOps.put2x16(ptr + 8L, instance.light);
         ExtraMemoryOps.putMatrix4f(ptr + 12L, instance.pose);
      })
      .vertexShader(ResourceUtil.rl("instance/transformed.vert"))
      .cullShader(ResourceUtil.rl("instance/cull/transformed.glsl"))
      .build();
   public static final InstanceType<PosedInstance> POSED = SimpleInstanceType.builder(PosedInstance::new)
      .layout(
         LayoutBuilder.create()
            .vector("color", FloatRepr.NORMALIZED_UNSIGNED_BYTE, 4)
            .vector("overlay", IntegerRepr.SHORT, 2)
            .vector("light", FloatRepr.UNSIGNED_SHORT, 2)
            .matrix("pose", FloatRepr.FLOAT, 4)
            .matrix("normal", FloatRepr.FLOAT, 3)
            .build()
      )
      .writer((ptr, instance) -> {
         MemoryUtil.memPutByte(ptr, instance.red);
         MemoryUtil.memPutByte(ptr + 1L, instance.green);
         MemoryUtil.memPutByte(ptr + 2L, instance.blue);
         MemoryUtil.memPutByte(ptr + 3L, instance.alpha);
         ExtraMemoryOps.put2x16(ptr + 4L, instance.overlay);
         ExtraMemoryOps.put2x16(ptr + 8L, instance.light);
         ExtraMemoryOps.putMatrix4f(ptr + 12L, instance.pose);
         ExtraMemoryOps.putMatrix3f(ptr + 76L, instance.normal);
      })
      .vertexShader(ResourceUtil.rl("instance/posed.vert"))
      .cullShader(ResourceUtil.rl("instance/cull/posed.glsl"))
      .build();
   public static final InstanceType<OrientedInstance> ORIENTED = SimpleInstanceType.builder(OrientedInstance::new)
      .layout(
         LayoutBuilder.create()
            .vector("color", FloatRepr.NORMALIZED_UNSIGNED_BYTE, 4)
            .vector("overlay", IntegerRepr.SHORT, 2)
            .vector("light", FloatRepr.UNSIGNED_SHORT, 2)
            .vector("position", FloatRepr.FLOAT, 3)
            .vector("pivot", FloatRepr.FLOAT, 3)
            .vector("rotation", FloatRepr.FLOAT, 4)
            .build()
      )
      .writer((ptr, instance) -> {
         MemoryUtil.memPutByte(ptr, instance.red);
         MemoryUtil.memPutByte(ptr + 1L, instance.green);
         MemoryUtil.memPutByte(ptr + 2L, instance.blue);
         MemoryUtil.memPutByte(ptr + 3L, instance.alpha);
         ExtraMemoryOps.put2x16(ptr + 4L, instance.overlay);
         ExtraMemoryOps.put2x16(ptr + 8L, instance.light);
         MemoryUtil.memPutFloat(ptr + 12L, instance.posX);
         MemoryUtil.memPutFloat(ptr + 16L, instance.posY);
         MemoryUtil.memPutFloat(ptr + 20L, instance.posZ);
         MemoryUtil.memPutFloat(ptr + 24L, instance.pivotX);
         MemoryUtil.memPutFloat(ptr + 28L, instance.pivotY);
         MemoryUtil.memPutFloat(ptr + 32L, instance.pivotZ);
         ExtraMemoryOps.putQuaternionf(ptr + 36L, instance.rotation);
      })
      .vertexShader(ResourceUtil.rl("instance/oriented.vert"))
      .cullShader(ResourceUtil.rl("instance/cull/oriented.glsl"))
      .build();
   public static final InstanceType<ShadowInstance> SHADOW = SimpleInstanceType.builder(ShadowInstance::new)
      .layout(
         LayoutBuilder.create()
            .vector("pos", FloatRepr.FLOAT, 3)
            .vector("entityPosXZ", FloatRepr.FLOAT, 2)
            .vector("size", FloatRepr.FLOAT, 2)
            .scalar("alpha", FloatRepr.FLOAT)
            .scalar("radius", FloatRepr.FLOAT)
            .build()
      )
      .writer((ptr, instance) -> {
         MemoryUtil.memPutFloat(ptr, instance.x);
         MemoryUtil.memPutFloat(ptr + 4L, instance.y);
         MemoryUtil.memPutFloat(ptr + 8L, instance.z);
         MemoryUtil.memPutFloat(ptr + 12L, instance.entityX);
         MemoryUtil.memPutFloat(ptr + 16L, instance.entityZ);
         MemoryUtil.memPutFloat(ptr + 20L, instance.sizeX);
         MemoryUtil.memPutFloat(ptr + 24L, instance.sizeZ);
         MemoryUtil.memPutFloat(ptr + 28L, instance.alpha);
         MemoryUtil.memPutFloat(ptr + 32L, instance.radius);
      })
      .vertexShader(ResourceUtil.rl("instance/shadow.vert"))
      .cullShader(ResourceUtil.rl("instance/cull/shadow.glsl"))
      .build();

   private InstanceTypes() {
   }
}

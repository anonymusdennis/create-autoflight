package com.simibubi.create.content.equipment.potatoCannon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.Create;
import com.simibubi.create.api.equipment.potatoCannon.PotatoProjectileRenderMode;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class AllPotatoProjectileRenderModes {
   public static void init() {
   }

   private static void register(String name, MapCodec<? extends PotatoProjectileRenderMode> codec) {
      Registry.register(CreateBuiltInRegistries.POTATO_PROJECTILE_RENDER_MODE, Create.asResource(name), codec);
   }

   private static int entityRandom(Entity entity, int maxValue) {
      return System.identityHashCode(entity) * 31 % maxValue;
   }

   static {
      register("billboard", AllPotatoProjectileRenderModes.Billboard.CODEC);
      register("tumble", AllPotatoProjectileRenderModes.Tumble.CODEC);
      register("toward_motion", AllPotatoProjectileRenderModes.TowardMotion.CODEC);
      register("stuck_to_entity", AllPotatoProjectileRenderModes.StuckToEntity.CODEC);
   }

   public static enum Billboard implements PotatoProjectileRenderMode {
      INSTANCE;

      public static final MapCodec<AllPotatoProjectileRenderModes.Billboard> CODEC = MapCodec.unit(INSTANCE);

      @OnlyIn(Dist.CLIENT)
      @Override
      public void transform(PoseStack ms, PotatoProjectileEntity entity, float pt) {
         Minecraft mc = Minecraft.getInstance();
         Vec3 p1 = mc.getCameraEntity().getEyePosition(pt);
         Vec3 diff = entity.getBoundingBox().getCenter().subtract(p1);
         ((PoseTransformStack)TransformStack.of(ms).rotateYDegrees(AngleHelper.deg(Mth.atan2(diff.x, diff.z)) + 180.0F))
            .rotateXDegrees(AngleHelper.deg(Mth.atan2(diff.y, (double)Mth.sqrt((float)(diff.x * diff.x + diff.z * diff.z)))));
      }

      @Override
      public MapCodec<? extends PotatoProjectileRenderMode> codec() {
         return CODEC;
      }
   }

   public static record StuckToEntity(Vec3 offset) implements PotatoProjectileRenderMode {
      public static final MapCodec<AllPotatoProjectileRenderModes.StuckToEntity> CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(Vec3.CODEC.fieldOf("offset").forGetter(i -> i.offset)).apply(instance, AllPotatoProjectileRenderModes.StuckToEntity::new)
      );

      @OnlyIn(Dist.CLIENT)
      @Override
      public void transform(PoseStack ms, PotatoProjectileEntity entity, float pt) {
         TransformStack.of(ms).rotateYDegrees(AngleHelper.deg(Mth.atan2(this.offset.x, this.offset.z)));
      }

      @Override
      public MapCodec<? extends PotatoProjectileRenderMode> codec() {
         return CODEC;
      }
   }

   public static record TowardMotion(int spriteAngleOffset, float spin) implements PotatoProjectileRenderMode {
      public static final MapCodec<AllPotatoProjectileRenderModes.TowardMotion> CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(
                  Codec.INT.fieldOf("sprite_angle_offset").forGetter(i -> i.spriteAngleOffset), Codec.FLOAT.fieldOf("spin").forGetter(i -> i.spin)
               )
               .apply(instance, AllPotatoProjectileRenderModes.TowardMotion::new)
      );

      @OnlyIn(Dist.CLIENT)
      @Override
      public void transform(PoseStack ms, PotatoProjectileEntity entity, float pt) {
         Vec3 diff = entity.getDeltaMovement();
         ((PoseTransformStack)TransformStack.of(ms).rotateYDegrees(AngleHelper.deg(Mth.atan2(diff.x, diff.z))))
            .rotateXDegrees(270.0F + AngleHelper.deg(Mth.atan2(diff.y, (double)(-Mth.sqrt((float)(diff.x * diff.x + diff.z * diff.z))))));
         ((PoseTransformStack)TransformStack.of(ms)
               .rotateYDegrees(((float)entity.tickCount + pt) * 20.0F * this.spin + (float)AllPotatoProjectileRenderModes.entityRandom(entity, 360)))
            .rotateZDegrees((float)(-this.spriteAngleOffset));
      }

      @Override
      public MapCodec<? extends PotatoProjectileRenderMode> codec() {
         return CODEC;
      }
   }

   public static enum Tumble implements PotatoProjectileRenderMode {
      INSTANCE;

      public static final MapCodec<AllPotatoProjectileRenderModes.Tumble> CODEC = MapCodec.unit(INSTANCE);

      @OnlyIn(Dist.CLIENT)
      @Override
      public void transform(PoseStack ms, PotatoProjectileEntity entity, float pt) {
         AllPotatoProjectileRenderModes.Billboard.INSTANCE.transform(ms, entity, pt);
         ((PoseTransformStack)TransformStack.of(ms)
               .rotateZDegrees(((float)entity.tickCount + pt) * 2.0F * (float)AllPotatoProjectileRenderModes.entityRandom(entity, 16)))
            .rotateXDegrees(((float)entity.tickCount + pt) * (float)AllPotatoProjectileRenderModes.entityRandom(entity, 32));
      }

      @Override
      public MapCodec<? extends PotatoProjectileRenderMode> codec() {
         return CODEC;
      }
   }
}

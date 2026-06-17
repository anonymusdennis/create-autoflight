package com.simibubi.create.content.logistics.box;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.AbstractEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class PackageVisual extends AbstractEntityVisual<PackageEntity> implements SimpleDynamicVisual {
   public final TransformedInstance instance;

   public PackageVisual(VisualizationContext ctx, PackageEntity entity, float partialTick) {
      super(ctx, entity, partialTick);
      ItemStack box = entity.box;
      if (box.isEmpty() || !PackageItem.isPackage(box)) {
         box = AllBlocks.CARDBOARD_BLOCK.asStack();
      }

      PartialModel model = AllPartialModels.PACKAGES.get(BuiltInRegistries.ITEM.getKey(box.getItem()));
      this.instance = (TransformedInstance)this.instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(model)).createInstance();
      this.animate(partialTick);
   }

   public void beginFrame(Context ctx) {
      this.animate(ctx.partialTick());
   }

   private void animate(float partialTick) {
      float yaw = Mth.lerp(partialTick, ((PackageEntity)this.entity).yRotO, ((PackageEntity)this.entity).getYRot());
      Vec3 pos = ((PackageEntity)this.entity).position();
      Vec3i renderOrigin = this.renderOrigin();
      float x = (float)(Mth.lerp((double)partialTick, ((PackageEntity)this.entity).xo, pos.x) - (double)renderOrigin.getX());
      float y = (float)(Mth.lerp((double)partialTick, ((PackageEntity)this.entity).yo, pos.y) - (double)renderOrigin.getY());
      float z = (float)(Mth.lerp((double)partialTick, ((PackageEntity)this.entity).zo, pos.z) - (double)renderOrigin.getZ());
      long randomBits = (long)((PackageEntity)this.entity).getId() * 31L * 493286711L;
      randomBits = randomBits * randomBits * 4392167121L + randomBits * 98761L;
      float xNudge = (((float)(randomBits >> 16 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
      float yNudge = (((float)(randomBits >> 20 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
      float zNudge = (((float)(randomBits >> 24 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
      ((TransformedInstance)((TransformedInstance)this.instance
               .setIdentityTransform()
               .translate((double)x - 0.5 + (double)xNudge, (double)(y + yNudge), (double)z - 0.5 + (double)zNudge))
            .rotateYCenteredDegrees(-yaw - 90.0F))
         .light(this.computePackedLight(partialTick))
         .setChanged();
   }

   protected void _delete() {
      this.instance.delete();
   }
}

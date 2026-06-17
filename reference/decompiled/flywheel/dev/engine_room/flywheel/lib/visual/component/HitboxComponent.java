package dev.engine_room.flywheel.lib.visual.component;

import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.LineModelBuilder;
import dev.engine_room.flywheel.lib.visual.util.SmartRecycler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public final class HitboxComponent implements EntityComponent {
   public static final Model BOX_MODEL = new LineModelBuilder(12)
      .line(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F)
      .line(0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F)
      .line(0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F)
      .line(0.0F, 1.0F, 1.0F, 0.0F, 1.0F, 0.0F)
      .line(0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F)
      .line(0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F)
      .line(1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F)
      .line(1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F)
      .line(1.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F)
      .line(1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F)
      .line(1.0F, 1.0F, 0.0F, 1.0F, 0.0F, 0.0F)
      .line(1.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F)
      .build();
   public static final Model LINE_MODEL = new LineModelBuilder(1).line(0.0F, 0.0F, 0.0F, 0.0F, 2.0F, 0.0F).build();
   private final VisualizationContext context;
   private final Entity entity;
   private final SmartRecycler<Model, TransformedInstance> recycler;
   private boolean showEyeBox;

   public HitboxComponent(VisualizationContext context, Entity entity) {
      this.context = context;
      this.entity = entity;
      this.showEyeBox = entity instanceof LivingEntity;
      this.recycler = new SmartRecycler<>(this::createInstance);
   }

   private TransformedInstance createInstance(Model model) {
      TransformedInstance instance = this.context.instancerProvider().instancer(InstanceTypes.TRANSFORMED, model).createInstance();
      instance.light(240);
      instance.setChanged();
      return instance;
   }

   public boolean doesShowEyeBox() {
      return this.showEyeBox;
   }

   public HitboxComponent showEyeBox(boolean showEyeBox) {
      this.showEyeBox = showEyeBox;
      return this;
   }

   @Override
   public void beginFrame(DynamicVisual.Context context) {
      this.recycler.resetCount();
      boolean shouldRenderHitBoxes = Minecraft.getInstance().getEntityRenderDispatcher().shouldRenderHitBoxes();
      if (shouldRenderHitBoxes && !this.entity.isInvisible() && !Minecraft.getInstance().showOnlyReducedInfo()) {
         float partialTick = context.partialTick();
         double entityX = Mth.lerp((double)partialTick, this.entity.xOld, this.entity.getX());
         double entityY = Mth.lerp((double)partialTick, this.entity.yOld, this.entity.getY());
         double entityZ = Mth.lerp((double)partialTick, this.entity.zOld, this.entity.getZ());
         AABB bb = this.entity.getBoundingBox();
         double boxX = entityX + bb.minX - this.entity.getX();
         double boxY = entityY + bb.minY - this.entity.getY();
         double boxZ = entityZ + bb.minZ - this.entity.getZ();
         float widthX = (float)(bb.maxX - bb.minX);
         float widthY = (float)(bb.maxY - bb.minY);
         float widthZ = (float)(bb.maxZ - bb.minZ);
         this.recycler.get(BOX_MODEL).setIdentityTransform().translate(boxX, boxY, boxZ).scale(widthX, widthY, widthZ).setChanged();
         if (this.showEyeBox) {
            this.recycler
               .get(BOX_MODEL)
               .setIdentityTransform()
               .translate(boxX, entityY + (double)this.entity.getEyeHeight() - 0.01, boxZ)
               .scale(widthX, 0.02F, widthZ)
               .color(255, 0, 0)
               .setChanged();
         }

         Vec3 viewVector = this.entity.getViewVector(partialTick);
         this.recycler
            .get(LINE_MODEL)
            .setIdentityTransform()
            .translate(entityX, entityY + (double)this.entity.getEyeHeight(), entityZ)
            .rotate(new Quaternionf().rotateTo(0.0F, 1.0F, 0.0F, (float)viewVector.x, (float)viewVector.y, (float)viewVector.z))
            .color(0, 0, 255)
            .setChanged();
      }

      this.recycler.discardExtra();
   }

   @Override
   public void delete() {
      this.recycler.delete();
   }
}

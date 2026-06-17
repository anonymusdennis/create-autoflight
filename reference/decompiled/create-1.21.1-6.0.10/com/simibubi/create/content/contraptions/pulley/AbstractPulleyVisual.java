package com.simibubi.create.content.contraptions.pulley;

import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.ShaftVisual;
import com.simibubi.create.content.processing.burner.ScrollInstance;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visual.SectionTrackedVisual.SectionCollector;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.math.MoreMath;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.engine_room.flywheel.lib.visual.util.SmartRecycler;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.function.Consumer;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

public abstract class AbstractPulleyVisual<T extends KineticBlockEntity> extends ShaftVisual<T> implements SimpleDynamicVisual {
   private final ScrollInstance coil;
   private final TransformedInstance magnet;
   private final SmartRecycler<Boolean, TransformedInstance> rope;
   protected final Direction rotatingAbout;
   protected final Axis rotationAxis;
   private final AbstractPulleyVisual<T>.LightCache lightCache = new AbstractPulleyVisual.LightCache();
   private float offset;

   public AbstractPulleyVisual(VisualizationContext dispatcher, T blockEntity, float partialTick) {
      super(dispatcher, blockEntity, partialTick);
      this.rotatingAbout = Direction.get(AxisDirection.POSITIVE, this.rotationAxis());
      this.rotationAxis = Axis.of(this.rotatingAbout.step());
      float blockStateAngle = AngleHelper.horizontalAngle(this.rotatingAbout);
      Quaternionfc rotation = new Quaternionf().rotationY((float) (Math.PI / 180.0) * blockStateAngle);
      this.coil = ((ScrollInstance)this.getCoilModel().createInstance())
         .rotation(rotation)
         .position(this.getVisualPosition())
         .setSpriteShift(this.getCoilAnimation());
      this.coil.setChanged();
      this.magnet = (TransformedInstance)this.magnetInstancer().createInstance();
      this.rope = new SmartRecycler(
         b -> b ? (TransformedInstance)this.getHalfRopeModel().createInstance() : (TransformedInstance)this.getRopeModel().createInstance()
      );
      this.updateOffset(partialTick);
      this.updateLight(partialTick);
      this.animate();
   }

   public void setSectionCollector(SectionCollector sectionCollector) {
      super.setSectionCollector(sectionCollector);
      this.lightCache.updateSections();
   }

   protected abstract Instancer<TransformedInstance> getRopeModel();

   protected abstract Instancer<TransformedInstance> getMagnetModel();

   protected abstract Instancer<TransformedInstance> getHalfMagnetModel();

   protected abstract Instancer<ScrollInstance> getCoilModel();

   protected abstract Instancer<TransformedInstance> getHalfRopeModel();

   protected abstract float getOffset(float var1);

   protected abstract boolean isRunning();

   protected abstract SpriteShiftEntry getCoilAnimation();

   private Instancer<TransformedInstance> magnetInstancer() {
      return this.offset > 0.25F ? this.getMagnetModel() : this.getHalfMagnetModel();
   }

   public void beginFrame(Context ctx) {
      this.updateOffset(ctx.partialTick());
      this.animate();
   }

   private void animate() {
      this.coil.offsetV = -this.offset;
      this.coil.setChanged();
      this.magnet.setVisible(this.isRunning() || this.offset == 0.0F);
      this.magnetInstancer().stealInstance(this.magnet);
      ((TransformedInstance)this.magnet.setIdentityTransform().translate(this.getVisualPosition()))
         .translate(0.0F, -this.offset, 0.0F)
         .light(this.lightCache.getPackedLight(Math.max(0, Mth.floor(this.offset))))
         .setChanged();
      this.rope.resetCount();
      if (this.shouldRenderHalfRope()) {
         float f = this.offset % 1.0F;
         float halfRopeNudge = f > 0.75F ? f - 1.0F : f;
         ((TransformedInstance)((TransformedInstance)this.rope.get(true)).setIdentityTransform().translate(this.getVisualPosition()))
            .translate(0.0F, -halfRopeNudge, 0.0F)
            .light(this.lightCache.getPackedLight(0))
            .setChanged();
      }

      if (this.isRunning()) {
         int neededRopeCount = this.getNeededRopeCount();

         for (int i = 0; i < neededRopeCount; i++) {
            ((TransformedInstance)((TransformedInstance)this.rope.get(false)).setIdentityTransform().translate(this.getVisualPosition()))
               .translate(0.0F, -this.offset + (float)i + 1.0F, 0.0F)
               .light(this.lightCache.getPackedLight(neededRopeCount - 1 - i))
               .setChanged();
         }
      }

      this.rope.discardExtra();
   }

   @Override
   public void updateLight(float partialTick) {
      super.updateLight(partialTick);
      this.relight(new FlatLit[]{this.coil});
      this.lightCache.update();
   }

   private void updateOffset(float pt) {
      this.offset = this.getOffset(pt);
      this.lightCache.setSize(Mth.ceil(this.offset) + 2);
   }

   private int getNeededRopeCount() {
      return Math.max(0, Mth.ceil(this.offset - 1.25F));
   }

   private boolean shouldRenderHalfRope() {
      float f = this.offset % 1.0F;
      return this.offset > 0.75F && (f < 0.25F || f > 0.75F);
   }

   @Override
   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      super.collectCrumblingInstances(consumer);
      consumer.accept(this.coil);
      consumer.accept(this.magnet);
   }

   @Override
   protected void _delete() {
      super._delete();
      this.coil.delete();
      this.magnet.delete();
      this.rope.delete();
   }

   private class LightCache {
      private final ByteList data = new ByteArrayList();
      private final LongSet sections = new LongOpenHashSet();
      private final MutableBlockPos mutablePos = new MutableBlockPos();
      private int sectionCount;

      public void setSize(int size) {
         if (size != this.data.size()) {
            this.data.size(size);
            this.update();
            int sectionCount = MoreMath.ceilingDiv(size + 15 - AbstractPulleyVisual.this.pos.getY() + AbstractPulleyVisual.this.pos.getY() / 4 * 4, 16);
            if (sectionCount != this.sectionCount) {
               this.sectionCount = sectionCount;
               this.sections.clear();
               int sectionX = SectionPos.blockToSectionCoord(AbstractPulleyVisual.this.pos.getX());
               int sectionY = SectionPos.blockToSectionCoord(AbstractPulleyVisual.this.pos.getY());
               int sectionZ = SectionPos.blockToSectionCoord(AbstractPulleyVisual.this.pos.getZ());

               for (int i = 0; i < sectionCount; i++) {
                  this.sections.add(SectionPos.asLong(sectionX, sectionY - i, sectionZ));
               }

               if (AbstractPulleyVisual.this.lightSections != null) {
                  this.updateSections();
               }
            }
         }
      }

      public void updateSections() {
         AbstractPulleyVisual.this.lightSections.sections(this.sections);
      }

      public void update() {
         this.mutablePos.set(AbstractPulleyVisual.this.pos);

         for (int i = 0; i < this.data.size(); i++) {
            int blockLight = AbstractPulleyVisual.this.level.getBrightness(LightLayer.BLOCK, this.mutablePos);
            int skyLight = AbstractPulleyVisual.this.level.getBrightness(LightLayer.SKY, this.mutablePos);
            int light = (skyLight & 15) << 4 | blockLight & 15;
            this.data.set(i, (byte)light);
            this.mutablePos.move(Direction.DOWN);
         }
      }

      public int getPackedLight(int offset) {
         if (offset >= 0 && offset < this.data.size()) {
            int light = Byte.toUnsignedInt(this.data.getByte(offset));
            int blockLight = light & 15;
            int skyLight = light >>> 4 & 15;
            return LightTexture.pack(blockLight, skyLight);
         } else {
            return 0;
         }
      }
   }
}

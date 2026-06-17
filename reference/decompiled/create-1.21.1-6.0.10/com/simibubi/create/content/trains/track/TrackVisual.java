package com.simibubi.create.content.trains.track;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.render.SpecialModels;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visual.SectionTrackedVisual.SectionCollector;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.engine_room.flywheel.lib.visual.AbstractVisual;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public class TrackVisual extends AbstractVisual implements BlockEntityVisual<TrackBlockEntity>, ShaderLightVisual {
   private final List<TrackVisual.BezierTrackVisual> visuals = new ArrayList<>();
   protected final TrackBlockEntity blockEntity;
   protected final BlockPos pos;
   protected final BlockPos visualPos;
   @UnknownNullability
   protected SectionCollector lightSections;

   public TrackVisual(VisualizationContext context, TrackBlockEntity track, float partialTick) {
      super(context, track.getLevel(), partialTick);
      this.blockEntity = track;
      this.pos = this.blockEntity.getBlockPos();
      this.visualPos = this.pos.subtract(context.renderOrigin());
      this.collectConnections();
   }

   public void setSectionCollector(SectionCollector sectionCollector) {
      this.lightSections = sectionCollector;
      this.lightSections.sections(this.collectLightSections());
   }

   public void update(float pt) {
      if (!this.blockEntity.connections.isEmpty()) {
         this._delete();
         this.collectConnections();
         this.lightSections.sections(this.collectLightSections());
      }
   }

   private void collectConnections() {
      this.blockEntity.connections.values().stream().map(this::createInstance).filter(Objects::nonNull).forEach(this.visuals::add);
   }

   @Nullable
   private TrackVisual.BezierTrackVisual createInstance(BezierConnection bc) {
      return !bc.isPrimary() ? null : new TrackVisual.BezierTrackVisual(bc);
   }

   public void _delete() {
      this.visuals.forEach(TrackVisual.BezierTrackVisual::delete);
      this.visuals.clear();
   }

   public LongSet collectLightSections() {
      if (this.blockEntity.connections.isEmpty()) {
         return LongSet.of();
      } else {
         int minX = Integer.MAX_VALUE;
         int minY = Integer.MAX_VALUE;
         int minZ = Integer.MAX_VALUE;
         int maxX = Integer.MIN_VALUE;
         int maxY = Integer.MIN_VALUE;
         int maxZ = Integer.MIN_VALUE;

         for (BezierConnection connection : this.blockEntity.connections.values()) {
            AABB bounds = connection.getBounds();
            minX = Math.min(minX, Mth.floor(bounds.minX) - 1);
            minY = Math.min(minY, Mth.floor(bounds.minY) - 1);
            minZ = Math.min(minZ, Mth.floor(bounds.minZ) - 1);
            maxX = Math.max(maxX, Mth.ceil(bounds.maxX) + 1);
            maxY = Math.max(maxY, Mth.ceil(bounds.maxY) + 1);
            maxZ = Math.max(maxZ, Mth.ceil(bounds.maxZ) + 1);
         }

         int minSectionX = SectionPos.blockToSectionCoord(minX);
         int minSectionY = SectionPos.blockToSectionCoord(minY);
         int minSectionZ = SectionPos.blockToSectionCoord(minZ);
         int maxSectionX = SectionPos.blockToSectionCoord(maxX);
         int maxSectionY = SectionPos.blockToSectionCoord(maxY);
         int maxSectionZ = SectionPos.blockToSectionCoord(maxZ);
         LongSet out = new LongArraySet();

         for (int x = minSectionX; x <= maxSectionX; x++) {
            for (int y = minSectionY; y <= maxSectionY; y++) {
               for (int z = minSectionZ; z <= maxSectionZ; z++) {
                  out.add(SectionPos.asLong(x, y, z));
               }
            }
         }

         return out;
      }
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      for (TrackVisual.BezierTrackVisual instance : this.visuals) {
         instance.collectCrumblingInstances(consumer);
      }
   }

   private class BezierTrackVisual {
      private final TransformedInstance[] ties;
      private final TransformedInstance[] left;
      private final TransformedInstance[] right;
      @Nullable
      private TrackVisual.BezierTrackVisual.GirderVisual girder;

      private BezierTrackVisual(BezierConnection bc) {
         this.girder = bc.hasGirder ? new TrackVisual.BezierTrackVisual.GirderVisual(bc) : null;
         PoseStack pose = new PoseStack();
         TransformStack.of(pose).translate(TrackVisual.this.visualPos);
         int segCount = bc.getSegmentCount();
         this.ties = new TransformedInstance[segCount];
         this.left = new TransformedInstance[segCount];
         this.right = new TransformedInstance[segCount];
         TrackMaterial.TrackModelHolder modelHolder = bc.getMaterial().getModelHolder();
         TrackVisual.this.instancerProvider().instancer(InstanceTypes.TRANSFORMED, SpecialModels.flatChunk(modelHolder.tie())).createInstances(this.ties);
         TrackVisual.this.instancerProvider()
            .instancer(InstanceTypes.TRANSFORMED, SpecialModels.flatChunk(modelHolder.leftSegment()))
            .createInstances(this.left);
         TrackVisual.this.instancerProvider()
            .instancer(InstanceTypes.TRANSFORMED, SpecialModels.flatChunk(modelHolder.rightSegment()))
            .createInstances(this.right);
         BezierConnection.SegmentAngles segment = bc.getBakedSegments();

         for (int i = 1; i < segment.length; i++) {
            int modelIndex = i - 1;
            this.ties[modelIndex].setTransform(pose).mul(segment.tieTransform[i]).setChanged();

            for (boolean first : Iterate.trueAndFalse) {
               Pose transform = (Pose)segment.railTransforms[i].get(first);
               (first ? this.left : this.right)[modelIndex].setTransform(pose).mul(transform).setChanged();
            }
         }
      }

      void delete() {
         for (TransformedInstance d : this.ties) {
            d.delete();
         }

         for (TransformedInstance d : this.left) {
            d.delete();
         }

         for (TransformedInstance d : this.right) {
            d.delete();
         }

         if (this.girder != null) {
            this.girder.delete();
         }
      }

      public void collectCrumblingInstances(Consumer<Instance> consumer) {
         for (TransformedInstance d : this.ties) {
            consumer.accept(d);
         }

         for (TransformedInstance d : this.left) {
            consumer.accept(d);
         }

         for (TransformedInstance d : this.right) {
            consumer.accept(d);
         }

         if (this.girder != null) {
            this.girder.collectCrumblingInstances(consumer);
         }
      }

      private class GirderVisual {
         private final Couple<TransformedInstance[]> beams;
         private final Couple<Couple<TransformedInstance[]>> beamCaps;

         private GirderVisual(BezierConnection bc) {
            PoseStack pose = new PoseStack();
            ((PoseTransformStack)TransformStack.of(pose).translate(TrackVisual.this.visualPos)).nudge((int)((BlockPos)bc.bePositions.getFirst()).asLong());
            int segCount = bc.getSegmentCount();
            this.beams = Couple.create(() -> new TransformedInstance[segCount]);
            this.beamCaps = Couple.create(() -> Couple.create(() -> new TransformedInstance[segCount]));
            this.beams
               .forEach(
                  TrackVisual.this.instancerProvider().instancer(InstanceTypes.TRANSFORMED, SpecialModels.flatChunk(AllPartialModels.GIRDER_SEGMENT_MIDDLE))::createInstances
               );
            this.beamCaps.forEachWithContext((c, topx) -> {
               Model partialModel = SpecialModels.flatChunk(topx ? AllPartialModels.GIRDER_SEGMENT_TOP : AllPartialModels.GIRDER_SEGMENT_BOTTOM);
               c.forEach(TrackVisual.this.instancerProvider().instancer(InstanceTypes.TRANSFORMED, partialModel)::createInstances);
            });
            BezierConnection.GirderAngles segment = bc.getBakedGirders();

            for (int i = 1; i < segment.length; i++) {
               int modelIndex = i - 1;

               for (boolean first : Iterate.trueAndFalse) {
                  Pose beamTransform = (Pose)segment.beams[i].get(first);
                  ((TransformedInstance[])this.beams.get(first))[modelIndex].setTransform(pose).mul(beamTransform).setChanged();

                  for (boolean top : Iterate.trueAndFalse) {
                     Pose beamCapTransform = (Pose)((Couple)segment.beamCaps[i].get(top)).get(first);
                     ((TransformedInstance[])((Couple)this.beamCaps.get(top)).get(first))[modelIndex].setTransform(pose).mul(beamCapTransform).setChanged();
                  }
               }
            }
         }

         void delete() {
            this.beams.forEach(arr -> {
               for (TransformedInstance d : arr) {
                  d.delete();
               }
            });
            this.beamCaps.forEach(c -> c.forEach(arr -> {
                  for (TransformedInstance d : arr) {
                     d.delete();
                  }
               }));
         }

         public void collectCrumblingInstances(Consumer<Instance> consumer) {
            this.beams.forEach(arr -> {
               for (TransformedInstance d : arr) {
                  consumer.accept(d);
               }
            });
            this.beamCaps.forEach(c -> c.forEach(arr -> {
                  for (TransformedInstance d : arr) {
                     consumer.accept(d);
                  }
               }));
         }
      }
   }
}

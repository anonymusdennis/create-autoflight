package com.simibubi.create.content.trains.track;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.simibubi.create.AllBlocks;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BezierConnection implements Iterable<BezierConnection.Segment> {
   public final Couple<BlockPos> bePositions;
   public final Couple<Vec3> starts;
   public final Couple<Vec3> axes;
   public final Couple<Vec3> normals;
   @Nullable
   public Couple<Integer> smoothing;
   public final boolean primary;
   public final boolean hasGirder;
   protected TrackMaterial trackMaterial;
   private final AtomicReference<BezierConnection.Runtime> lazyRuntime = new AtomicReference<>(null);
   private final AtomicReference<BezierConnection.SegmentAngles> bakedSegments = new AtomicReference<>(null);
   private final AtomicReference<BezierConnection.GirderAngles> bakedGirders = new AtomicReference<>(null);

   public BezierConnection(
      Couple<BlockPos> positions, Couple<Vec3> starts, Couple<Vec3> axes, Couple<Vec3> normals, boolean primary, boolean girder, TrackMaterial material
   ) {
      this.bePositions = positions;
      this.starts = starts;
      this.axes = axes;
      this.normals = normals;
      this.primary = primary;
      this.hasGirder = girder;
      this.trackMaterial = material;
   }

   public BezierConnection secondary() {
      BezierConnection bezierConnection = new BezierConnection(
         this.bePositions.swap(), this.starts.swap(), this.axes.swap(), this.normals.swap(), !this.primary, this.hasGirder, this.trackMaterial
      );
      if (this.smoothing != null) {
         bezierConnection.smoothing = this.smoothing.swap();
      }

      return bezierConnection;
   }

   public BezierConnection clone() {
      BezierConnection out = new BezierConnection(
         this.bePositions.copy(), this.starts.copy(), this.axes.copy(), this.normals.copy(), this.primary, this.hasGirder, this.trackMaterial
      );
      if (this.smoothing != null) {
         out.smoothing = this.smoothing.copy();
      }

      return out;
   }

   private static boolean coupleEquals(Couple<?> a, Couple<?> b) {
      if (a.getFirst().equals(b.getFirst()) && a.getSecond().equals(b.getSecond())) {
         return true;
      } else {
         if (a.getFirst() instanceof Vec3 aFirst
            && a.getSecond() instanceof Vec3 aSecond
            && b.getFirst() instanceof Vec3 bFirst
            && b.getSecond() instanceof Vec3 bSecond
            && aFirst.closerThan(bFirst, 1.0E-6)
            && aSecond.closerThan(bSecond, 1.0E-6)) {
            return true;
         }

         return false;
      }
   }

   public boolean equalsSansMaterial(BezierConnection other) {
      return this.equalsSansMaterialInner(other) || this.equalsSansMaterialInner(other.secondary());
   }

   private boolean equalsSansMaterialInner(BezierConnection other) {
      return this == other
         || other != null
            && coupleEquals(this.bePositions, other.bePositions)
            && coupleEquals(this.starts, other.starts)
            && coupleEquals(this.axes, other.axes)
            && coupleEquals(this.normals, other.normals)
            && this.hasGirder == other.hasGirder;
   }

   public BezierConnection(CompoundTag compound, BlockPos localTo) {
      this(
         Couple.deserializeEach(compound.getList("Positions", 10), t -> NBTHelper.readBlockPos(t, "Pos")).map(b -> b.offset(localTo)),
         Couple.deserializeEach(compound.getList("Starts", 10), VecHelper::readNBTCompound).map(v -> v.add(Vec3.atLowerCornerOf(localTo))),
         Couple.deserializeEach(compound.getList("Axes", 10), VecHelper::readNBTCompound),
         Couple.deserializeEach(compound.getList("Normals", 10), VecHelper::readNBTCompound),
         compound.getBoolean("Primary"),
         compound.getBoolean("Girder"),
         TrackMaterial.deserialize(compound.getString("Material"))
      );
      if (compound.contains("Smoothing")) {
         this.smoothing = Couple.deserializeEach(compound.getList("Smoothing", 10), NBTHelper::intFromCompound);
      }
   }

   public CompoundTag write(BlockPos localTo) {
      Couple<BlockPos> tePositions = this.bePositions.map(b -> b.subtract(localTo));
      Couple<Vec3> starts = this.starts.map(v -> v.subtract(Vec3.atLowerCornerOf(localTo)));
      CompoundTag compound = new CompoundTag();
      compound.putBoolean("Girder", this.hasGirder);
      compound.putBoolean("Primary", this.primary);
      compound.put("Positions", tePositions.serializeEach(t -> {
         CompoundTag tag = new CompoundTag();
         tag.put("Pos", NbtUtils.writeBlockPos(t));
         return tag;
      }));
      compound.put("Starts", starts.serializeEach(VecHelper::writeNBTCompound));
      compound.put("Axes", this.axes.serializeEach(VecHelper::writeNBTCompound));
      compound.put("Normals", this.normals.serializeEach(VecHelper::writeNBTCompound));
      compound.putString("Material", this.getMaterial().id.toString());
      if (this.smoothing != null) {
         compound.put("Smoothing", this.smoothing.serializeEach(NBTHelper::intToCompound));
      }

      return compound;
   }

   public BezierConnection(FriendlyByteBuf buffer) {
      this(
         Couple.create(buffer::readBlockPos),
         Couple.create(() -> VecHelper.read(buffer)),
         Couple.create(() -> VecHelper.read(buffer)),
         Couple.create(() -> VecHelper.read(buffer)),
         buffer.readBoolean(),
         buffer.readBoolean(),
         TrackMaterial.deserialize(buffer.readUtf())
      );
      if (buffer.readBoolean()) {
         this.smoothing = Couple.create(buffer::readVarInt);
      }
   }

   public void write(FriendlyByteBuf buffer) {
      this.bePositions.forEach(buffer::writeBlockPos);
      this.starts.forEach(v -> VecHelper.write(v, buffer));
      this.axes.forEach(v -> VecHelper.write(v, buffer));
      this.normals.forEach(v -> VecHelper.write(v, buffer));
      buffer.writeBoolean(this.primary);
      buffer.writeBoolean(this.hasGirder);
      buffer.writeUtf(this.getMaterial().id.toString());
      buffer.writeBoolean(this.smoothing != null);
      if (this.smoothing != null) {
         this.smoothing.forEach(buffer::writeVarInt);
      }
   }

   public BlockPos getKey() {
      return (BlockPos)this.bePositions.getSecond();
   }

   public boolean isPrimary() {
      return this.primary;
   }

   public int yOffsetAt(Vec3 end) {
      if (this.smoothing == null) {
         return 0;
      } else if (TrackBlockEntityTilt.compareHandles((Vec3)this.starts.getFirst(), end)) {
         return (Integer)this.smoothing.getFirst();
      } else {
         return TrackBlockEntityTilt.compareHandles((Vec3)this.starts.getSecond(), end) ? (Integer)this.smoothing.getSecond() : 0;
      }
   }

   public double getLength() {
      return this.resolve().length;
   }

   public float[] getStepLUT() {
      return this.resolve().stepLUT;
   }

   public int getSegmentCount() {
      return this.resolve().segments;
   }

   public Vec3 getPosition(double t) {
      BezierConnection.Runtime runtime = this.resolve();
      return VecHelper.bezier((Vec3)this.starts.getFirst(), (Vec3)this.starts.getSecond(), runtime.finish1, runtime.finish2, (float)t);
   }

   public double getRadius() {
      return this.resolve().radius;
   }

   public double getHandleLength() {
      return this.resolve().handleLength;
   }

   public float getSegmentT(int index) {
      return this.resolve().getSegmentT(index);
   }

   public double incrementT(double currentT, double distance) {
      BezierConnection.Runtime runtime = this.resolve();
      double dx = VecHelper.bezierDerivative((Vec3)this.starts.getFirst(), (Vec3)this.starts.getSecond(), runtime.finish1, runtime.finish2, (float)currentT)
            .length()
         / this.getLength();
      return currentT + distance / dx;
   }

   public AABB getBounds() {
      return this.resolve().bounds;
   }

   public Vec3 getNormal(double t) {
      BezierConnection.Runtime runtime = this.resolve();
      Vec3 end1 = (Vec3)this.starts.getFirst();
      Vec3 end2 = (Vec3)this.starts.getSecond();
      Vec3 fn1 = (Vec3)this.normals.getFirst();
      Vec3 fn2 = (Vec3)this.normals.getSecond();
      Vec3 derivative = VecHelper.bezierDerivative(end1, end2, runtime.finish1, runtime.finish2, (float)t).normalize();
      Vec3 faceNormal = fn1.equals(fn2) ? fn1 : VecHelper.slerp((float)t, fn1, fn2);
      Vec3 normal = faceNormal.cross(derivative).normalize();
      return derivative.cross(normal);
   }

   @NotNull
   private BezierConnection.Runtime resolve() {
      BezierConnection.Runtime out = this.lazyRuntime.get();
      if (out == null) {
         out = new BezierConnection.Runtime(this.starts, this.axes);
         this.lazyRuntime.set(out);
      }

      return out;
   }

   @Override
   public Iterator<BezierConnection.Segment> iterator() {
      Vec3 offset = Vec3.atLowerCornerOf((Vec3i)this.bePositions.getFirst()).scale(-1.0).add(0.0, 0.1875, 0.0);
      return new BezierConnection.Bezierator(this, offset);
   }

   public void addItemsToPlayer(Player player) {
      Inventory inv = player.getInventory();

      for (int tracks = this.getTrackItemCost(); tracks > 0; tracks -= 64) {
         inv.placeItemBackInInventory(new ItemStack(this.getMaterial().getBlock(), Math.min(64, tracks)));
      }

      for (int girders = this.getGirderItemCost(); girders > 0; girders -= 64) {
         inv.placeItemBackInInventory(AllBlocks.METAL_GIRDER.asStack(Math.min(64, girders)));
      }
   }

   public int getGirderItemCost() {
      return this.hasGirder ? this.getTrackItemCost() * 2 : 0;
   }

   public int getTrackItemCost() {
      return (this.getSegmentCount() + 1) / 2;
   }

   public void spawnItems(Level level) {
      if (level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
         Vec3 origin = Vec3.atLowerCornerOf((Vec3i)this.bePositions.getFirst());

         for (BezierConnection.Segment segment : this) {
            if (segment.index % 2 == 0 && segment.index != this.getSegmentCount()) {
               Vec3 v = VecHelper.offsetRandomly(segment.position, level.random, 0.125F).add(origin);
               ItemEntity entity = new ItemEntity(level, v.x, v.y, v.z, this.getMaterial().asStack());
               entity.setDefaultPickUpDelay();
               level.addFreshEntity(entity);
               if (this.hasGirder) {
                  for (int i = 0; i < 2; i++) {
                     entity = new ItemEntity(level, v.x, v.y, v.z, AllBlocks.METAL_GIRDER.asStack());
                     entity.setDefaultPickUpDelay();
                     level.addFreshEntity(entity);
                  }
               }
            }
         }
      }
   }

   public void spawnDestroyParticles(Level level) {
      BlockParticleOption data = new BlockParticleOption(ParticleTypes.BLOCK, this.getMaterial().getBlock().defaultBlockState());
      BlockParticleOption girderData = new BlockParticleOption(ParticleTypes.BLOCK, AllBlocks.METAL_GIRDER.getDefaultState());
      if (level instanceof ServerLevel slevel) {
         Vec3 origin = Vec3.atLowerCornerOf((Vec3i)this.bePositions.getFirst());

         for (BezierConnection.Segment segment : this) {
            for (int offset : Iterate.positiveAndNegative) {
               Vec3 v = segment.position.add(segment.normal.scale((double)(0.875F * (float)offset))).add(origin);
               slevel.sendParticles(data, v.x, v.y, v.z, 1, 0.0, 0.0, 0.0, 0.0);
               if (this.hasGirder) {
                  slevel.sendParticles(girderData, v.x, v.y - 0.5, v.z, 1, 0.0, 0.0, 0.0, 0.0);
               }
            }
         }
      }
   }

   public TrackMaterial getMaterial() {
      return this.trackMaterial;
   }

   public void setMaterial(TrackMaterial material) {
      this.trackMaterial = material;
   }

   public BezierConnection.SegmentAngles getBakedSegments() {
      BezierConnection.SegmentAngles out = this.bakedSegments.get();
      if (out == null) {
         out = new BezierConnection.SegmentAngles(this);
         this.bakedSegments.set(out);
      }

      return out;
   }

   public BezierConnection.GirderAngles getBakedGirders() {
      BezierConnection.GirderAngles out = this.bakedGirders.get();
      if (out == null) {
         out = new BezierConnection.GirderAngles(this);
         this.bakedGirders.set(out);
      }

      return out;
   }

   public Map<Pair<Integer, Integer>, Double> rasterise() {
      Map<Pair<Integer, Integer>, Double> yLevels = new HashMap<>();
      BlockPos tePosition = (BlockPos)this.bePositions.getFirst();
      Vec3 end1 = ((Vec3)this.starts.getFirst()).subtract(Vec3.atLowerCornerOf(tePosition)).add(0.0, 0.1875, 0.0);
      Vec3 end2 = ((Vec3)this.starts.getSecond()).subtract(Vec3.atLowerCornerOf(tePosition)).add(0.0, 0.1875, 0.0);
      Vec3 axis1 = (Vec3)this.axes.getFirst();
      Vec3 axis2 = (Vec3)this.axes.getSecond();
      double handleLength = this.getHandleLength();
      Vec3 finish1 = axis1.scale(handleLength).add(end1);
      Vec3 finish2 = axis2.scale(handleLength).add(end2);
      Vec3 faceNormal1 = (Vec3)this.normals.getFirst();
      Vec3 faceNormal2 = (Vec3)this.normals.getSecond();
      int segCount = this.getSegmentCount();
      float[] lut = this.getStepLUT();
      Vec3[] samples = new Vec3[segCount];

      for (int i = 0; i < segCount; i++) {
         float t = Mth.clamp(((float)i + 0.5F) * lut[i] / (float)segCount, 0.0F, 1.0F);
         Vec3 result = VecHelper.bezier(end1, end2, finish1, finish2, t);
         Vec3 derivative = VecHelper.bezierDerivative(end1, end2, finish1, finish2, t).normalize();
         Vec3 faceNormal = faceNormal1.equals(faceNormal2) ? faceNormal1 : VecHelper.slerp(t, faceNormal1, faceNormal2);
         Vec3 normal = faceNormal.cross(derivative).normalize();
         Vec3 below = result.add(faceNormal.scale(-0.25));
         Vec3 rail1 = below.add(normal.scale(0.05F));
         Vec3 rail2 = below.subtract(normal.scale(0.05F));
         Vec3 railMiddle = rail1.add(rail2).scale(0.5);
         samples[i] = railMiddle;
      }

      Vec3 center = end1.add(end2).scale(0.5);
      Pair<Integer, Integer> prev = null;
      Pair<Integer, Integer> prev2 = null;
      Pair<Integer, Integer> prev3 = null;

      for (int i = 0; i < segCount; i++) {
         Vec3 railMiddle = samples[i];
         BlockPos pos = BlockPos.containing(railMiddle);
         Pair<Integer, Integer> key = Pair.of(pos.getX(), pos.getZ());
         boolean alreadyPresent = yLevels.containsKey(key);
         if (!alreadyPresent || !(yLevels.get(key) <= railMiddle.y)) {
            yLevels.put(key, railMiddle.y);
            if (!alreadyPresent) {
               if (prev3 != null) {
                  boolean doubledViaPrev = this.isLineDoubled(prev2, prev, key);
                  boolean doubledViaPrev2 = this.isLineDoubled(prev3, prev2, prev);
                  boolean prevCloser = this.diff(prev, center) > this.diff(prev2, center);
                  if (doubledViaPrev2 && (!doubledViaPrev || !prevCloser)) {
                     yLevels.remove(prev2);
                     prev2 = prev;
                     prev = key;
                     continue;
                  }

                  if (doubledViaPrev && doubledViaPrev2 && prevCloser) {
                     yLevels.remove(prev);
                     prev = key;
                     continue;
                  }
               }

               prev3 = prev2;
               prev2 = prev;
               prev = key;
            }
         }
      }

      return yLevels;
   }

   private double diff(Pair<Integer, Integer> pFrom, Vec3 to) {
      return to.distanceToSqr((double)((Integer)pFrom.getFirst()).intValue() + 0.5, to.y, (double)((Integer)pFrom.getSecond()).intValue() + 0.5);
   }

   private boolean isLineDoubled(Pair<Integer, Integer> pFrom, Pair<Integer, Integer> pVia, Pair<Integer, Integer> pTo) {
      int diff1x = (Integer)pVia.getFirst() - (Integer)pFrom.getFirst();
      int diff1z = (Integer)pVia.getSecond() - (Integer)pFrom.getSecond();
      int diff2x = (Integer)pTo.getFirst() - (Integer)pVia.getFirst();
      int diff2z = (Integer)pTo.getSecond() - (Integer)pVia.getSecond();
      return Math.abs(diff1x) + Math.abs(diff1z) == 1 && Math.abs(diff2x) + Math.abs(diff2z) == 1 && diff1x != diff2x && diff1z != diff2z;
   }

   private static class Bezierator implements Iterator<BezierConnection.Segment> {
      private final BezierConnection.Segment segment;
      private final Vec3 end1;
      private final Vec3 end2;
      private final Vec3 finish1;
      private final Vec3 finish2;
      private final Vec3 faceNormal1;
      private final Vec3 faceNormal2;
      private final BezierConnection.Runtime runtime;

      private Bezierator(BezierConnection bc, Vec3 offset) {
         this.runtime = bc.resolve();
         this.end1 = ((Vec3)bc.starts.getFirst()).add(offset);
         this.end2 = ((Vec3)bc.starts.getSecond()).add(offset);
         this.finish1 = ((Vec3)bc.axes.getFirst()).scale(this.runtime.handleLength).add(this.end1);
         this.finish2 = ((Vec3)bc.axes.getSecond()).scale(this.runtime.handleLength).add(this.end2);
         this.faceNormal1 = (Vec3)bc.normals.getFirst();
         this.faceNormal2 = (Vec3)bc.normals.getSecond();
         this.segment = new BezierConnection.Segment();
         this.segment.index = -1;
      }

      @Override
      public boolean hasNext() {
         return this.segment.index + 1 <= this.runtime.segments;
      }

      public BezierConnection.Segment next() {
         this.segment.index++;
         float t = this.runtime.getSegmentT(this.segment.index);
         this.segment.position = VecHelper.bezier(this.end1, this.end2, this.finish1, this.finish2, t);
         this.segment.derivative = VecHelper.bezierDerivative(this.end1, this.end2, this.finish1, this.finish2, t).normalize();
         this.segment.faceNormal = this.faceNormal1.equals(this.faceNormal2) ? this.faceNormal1 : VecHelper.slerp(t, this.faceNormal1, this.faceNormal2);
         this.segment.normal = this.segment.faceNormal.cross(this.segment.derivative).normalize();
         return this.segment;
      }
   }

   public static class GirderAngles {
      public final int length;
      public final Couple<Pose>[] beams;
      public final Couple<Couple<Pose>>[] beamCaps;
      public final BlockPos[] lightPosition;

      private GirderAngles(BezierConnection bc) {
         int segmentCount = bc.getSegmentCount();
         this.length = segmentCount + 1;
         this.beams = new Couple[this.length];
         this.beamCaps = new Couple[this.length];
         this.lightPosition = new BlockPos[this.length];
         Couple<Couple<Vec3>> previousOffsets = null;

         for (BezierConnection.Segment segment : bc) {
            int i = segment.index;
            boolean end = i == 0 || i == segmentCount;
            Vec3 leftGirder = segment.position.add(segment.normal.scale(0.965F));
            Vec3 rightGirder = segment.position.subtract(segment.normal.scale(0.965F));
            Vec3 upNormal = segment.derivative.normalize().cross(segment.normal);
            Vec3 firstGirderOffset = upNormal.scale(-0.5);
            Vec3 secondGirderOffset = upNormal.scale(-0.625);
            Vec3 leftTop = segment.position.add(segment.normal.scale(1.0)).add(firstGirderOffset);
            Vec3 rightTop = segment.position.subtract(segment.normal.scale(1.0)).add(firstGirderOffset);
            Vec3 leftBottom = leftTop.add(secondGirderOffset);
            Vec3 rightBottom = rightTop.add(secondGirderOffset);
            this.lightPosition[i] = BlockPos.containing(leftGirder.add(rightGirder).scale(0.5));
            Couple<Couple<Vec3>> offsets = Couple.create(Couple.create(leftTop, rightTop), Couple.create(leftBottom, rightBottom));
            if (previousOffsets == null) {
               previousOffsets = offsets;
            } else {
               this.beams[i] = Couple.create(null, null);
               this.beamCaps[i] = Couple.create(Couple.create(null, null), Couple.create(null, null));
               float scale = end ? 2.3F : 2.2F;

               for (boolean first : Iterate.trueAndFalse) {
                  Vec3 currentBeam = ((Vec3)((Couple)offsets.getFirst()).get(first)).add((Vec3)((Couple)offsets.getSecond()).get(first)).scale(0.5);
                  Vec3 previousBeam = ((Vec3)((Couple)previousOffsets.getFirst()).get(first))
                     .add((Vec3)((Couple)previousOffsets.getSecond()).get(first))
                     .scale(0.5);
                  Vec3 beamDiff = currentBeam.subtract(previousBeam);
                  Vec3 beamAngles = TrackRenderer.getModelAngles(segment.normal, beamDiff);
                  PoseStack poseStack = new PoseStack();
                  ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)TransformStack.of(poseStack).translate(previousBeam))
                              .rotateY((float)beamAngles.y))
                           .rotateX((float)beamAngles.x))
                        .rotateZ((float)beamAngles.z))
                     .translate(0.0F, 0.125F + (float)(segment.index % 2 == 0 ? 1 : -1) / 2048.0F - 9.765625E-4F, -0.03125F)
                     .scale(1.0F, 1.0F, (float)beamDiff.length() * scale);
                  this.beams[i].set(first, poseStack.last());

                  for (boolean top : Iterate.trueAndFalse) {
                     Vec3 current = (Vec3)((Couple)offsets.get(top)).get(first);
                     Vec3 previous = (Vec3)((Couple)previousOffsets.get(top)).get(first);
                     Vec3 diff = current.subtract(previous);
                     Vec3 capAngles = TrackRenderer.getModelAngles(segment.normal, diff);
                     poseStack = new PoseStack();
                     ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)TransformStack.of(poseStack)
                                       .translate(previous))
                                    .rotateY((float)capAngles.y))
                                 .rotateX((float)capAngles.x))
                              .rotateZ((float)capAngles.z))
                           .translate(0.0F, 0.125F + (float)(segment.index % 2 == 0 ? 1 : -1) / 2048.0F - 9.765625E-4F, -0.03125F)
                           .rotateZ(top ? 0.0F : 0.0F))
                        .scale(1.0F, 1.0F, (float)diff.length() * scale);
                     ((Couple)this.beamCaps[i].get(top)).set(first, poseStack.last());
                  }
               }

               previousOffsets = offsets;
            }
         }
      }
   }

   private static class Runtime {
      private final Vec3 finish1;
      private final Vec3 finish2;
      private final double length;
      private final float[] stepLUT;
      private final int segments;
      private double radius;
      private double handleLength;
      private final AABB bounds;

      private Runtime(Couple<Vec3> starts, Couple<Vec3> axes) {
         Vec3 end1 = (Vec3)starts.getFirst();
         Vec3 end2 = (Vec3)starts.getSecond();
         Vec3 axis1 = ((Vec3)axes.getFirst()).normalize();
         Vec3 axis2 = ((Vec3)axes.getSecond()).normalize();
         this.determineHandles(end1, end2, axis1, axis2);
         this.finish1 = axis1.scale(this.handleLength).add(end1);
         this.finish2 = axis2.scale(this.handleLength).add(end2);
         int scanCount = 16;
         this.length = computeLength(this.finish1, this.finish2, end1, end2, scanCount);
         this.segments = (int)(this.length * 2.0);
         this.stepLUT = new float[this.segments + 1];
         this.stepLUT[0] = 1.0F;
         float combinedDistance = 0.0F;
         AABB bounds = new AABB(end1, end2);
         Vec3 previous = end1;

         for (int i = 0; i <= this.segments; i++) {
            float t = (float)i / (float)this.segments;
            Vec3 result = VecHelper.bezier(end1, end2, this.finish1, this.finish2, t);
            bounds = bounds.minmax(new AABB(result, result));
            if (i > 0) {
               combinedDistance = (float)((double)combinedDistance + result.distanceTo(previous) / this.length);
               this.stepLUT[i] = t / combinedDistance;
            }

            previous = result;
         }

         this.bounds = bounds.inflate(1.375);
      }

      private static double computeLength(Vec3 finish1, Vec3 finish2, Vec3 end1, Vec3 end2, int scanCount) {
         double length = 0.0;
         Vec3 previous = end1;

         for (int i = 0; i <= scanCount; i++) {
            float t = (float)i / (float)scanCount;
            Vec3 result = VecHelper.bezier(end1, end2, finish1, finish2, t);
            if (previous != null) {
               length += result.distanceTo(previous);
            }

            previous = result;
         }

         return length;
      }

      public float getSegmentT(int index) {
         return index == this.segments ? 1.0F : (float)index * this.stepLUT[index] / (float)this.segments;
      }

      private void determineHandles(Vec3 end1, Vec3 end2, Vec3 axis1, Vec3 axis2) {
         Vec3 cross1 = axis1.cross(new Vec3(0.0, 1.0, 0.0));
         Vec3 cross2 = axis2.cross(new Vec3(0.0, 1.0, 0.0));
         this.radius = 0.0;
         double a1 = Mth.atan2(-axis2.z, -axis2.x);
         double a2 = Mth.atan2(axis1.z, axis1.x);
         double angle = a1 - a2;
         float circle = (float) (Math.PI * 2);
         angle = (angle + (double)circle) % (double)circle;
         if (Math.abs((double)circle - angle) < Math.abs(angle)) {
            angle = (double)circle - angle;
         }

         if (Mth.equal(angle, 0.0)) {
            double[] intersect = VecHelper.intersect(end1, end2, axis1, cross2, Axis.Y);
            if (intersect != null) {
               double t = Math.abs(intersect[0]);
               double u = Math.abs(intersect[1]);
               double min = Math.min(t, u);
               double max = Math.max(t, u);
               if (min > 1.2 && max / min > 1.0 && max / min < 3.0) {
                  this.handleLength = max - min;
                  return;
               }
            }

            this.handleLength = end2.distanceTo(end1) / 3.0;
         } else {
            double n = (double)circle / angle;
            double factor = 1.3333333333333333 * Math.tan(Math.PI / (2.0 * n));
            double[] intersect = VecHelper.intersect(end1, end2, cross1, cross2, Axis.Y);
            if (intersect == null) {
               this.handleLength = end2.distanceTo(end1) / 3.0;
            } else {
               this.radius = Math.abs(intersect[1]);
               this.handleLength = this.radius * factor;
               if (Mth.equal(this.handleLength, 0.0)) {
                  this.handleLength = 1.0;
               }
            }
         }
      }
   }

   public static class Segment {
      public int index;
      public Vec3 position;
      public Vec3 derivative;
      public Vec3 faceNormal;
      public Vec3 normal;
   }

   public static class SegmentAngles {
      public final int length;
      @NotNull
      public final Pose[] tieTransform;
      @NotNull
      public final Couple<Pose>[] railTransforms;
      @NotNull
      public final BlockPos[] lightPosition;

      private SegmentAngles(BezierConnection bc) {
         int segmentCount = bc.getSegmentCount();
         this.length = segmentCount + 1;
         this.tieTransform = new Pose[segmentCount + 1];
         this.railTransforms = new Couple[segmentCount + 1];
         this.lightPosition = new BlockPos[segmentCount + 1];
         Couple<Vec3> previousOffsets = null;

         for (BezierConnection.Segment segment : bc) {
            int i = segment.index;
            boolean end = i == 0 || i == segmentCount;
            Couple<Vec3> railOffsets = Couple.create(
               segment.position.add(segment.normal.scale(0.965F)), segment.position.subtract(segment.normal.scale(0.965F))
            );
            Vec3 railMiddle = ((Vec3)railOffsets.getFirst()).add((Vec3)railOffsets.getSecond()).scale(0.5);
            if (previousOffsets == null) {
               previousOffsets = railOffsets;
            } else {
               Vec3 prevMiddle = ((Vec3)previousOffsets.getFirst()).add((Vec3)previousOffsets.getSecond()).scale(0.5);
               Vec3 tieAngles = TrackRenderer.getModelAngles(segment.normal, railMiddle.subtract(prevMiddle));
               this.lightPosition[i] = BlockPos.containing(railMiddle);
               this.railTransforms[i] = Couple.create(null, null);
               PoseStack poseStack = new PoseStack();
               ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)TransformStack.of(poseStack).translate(prevMiddle))
                           .rotateY((float)tieAngles.y))
                        .rotateX((float)tieAngles.x))
                     .rotateZ((float)tieAngles.z))
                  .translate(-0.5F, -0.12890625F, 0.0F);
               this.tieTransform[i] = poseStack.last();
               float scale = end ? 2.2F : 2.1F;

               for (boolean first : Iterate.trueAndFalse) {
                  Vec3 railI = (Vec3)railOffsets.get(first);
                  Vec3 prevI = (Vec3)previousOffsets.get(first);
                  Vec3 diff = railI.subtract(prevI);
                  Vec3 anglesI = TrackRenderer.getModelAngles(segment.normal, diff);
                  poseStack = new PoseStack();
                  ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)TransformStack.of(poseStack).translate(prevI))
                              .rotateY((float)anglesI.y))
                           .rotateX((float)anglesI.x))
                        .rotateZ((float)anglesI.z))
                     .translate(0.0F, -0.12890625F, -0.03125F)
                     .scale(1.0F, 1.0F, (float)diff.length() * scale);
                  this.railTransforms[i].set(first, poseStack.last());
               }

               previousOffsets = railOffsets;
            }
         }
      }
   }
}

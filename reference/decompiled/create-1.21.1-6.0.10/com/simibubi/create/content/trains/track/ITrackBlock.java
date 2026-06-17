package com.simibubi.create.content.trains.track;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.Affine;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public interface ITrackBlock {
   Vec3 getUpNormal(BlockGetter var1, BlockPos var2, BlockState var3);

   List<Vec3> getTrackAxes(BlockGetter var1, BlockPos var2, BlockState var3);

   Vec3 getCurveStart(BlockGetter var1, BlockPos var2, BlockState var3, Vec3 var4);

   default int getYOffsetAt(BlockGetter world, BlockPos pos, BlockState state, Vec3 end) {
      return 0;
   }

   BlockState getBogeyAnchor(BlockGetter var1, BlockPos var2, BlockState var3);

   boolean trackEquals(BlockState var1, BlockState var2);

   default BlockState overlay(BlockGetter world, BlockPos pos, BlockState existing, BlockState placed) {
      return existing;
   }

   default double getElevationAtCenter(BlockGetter world, BlockPos pos, BlockState state) {
      return this.isSlope(world, pos, state) ? 0.5 : 0.0;
   }

   static Collection<TrackNodeLocation.DiscoveredLocation> walkConnectedTracks(BlockGetter worldIn, TrackNodeLocation location, boolean linear) {
      BlockGetter world = (BlockGetter)(location != null && worldIn instanceof ServerLevel sl ? sl.getServer().getLevel(location.dimension) : worldIn);
      List<TrackNodeLocation.DiscoveredLocation> list = new ArrayList<>();

      for (BlockPos blockPos : location.allAdjacent()) {
         BlockState blockState = world.getBlockState(blockPos);
         if (blockState.getBlock() instanceof ITrackBlock track) {
            list.addAll(track.getConnected(world, blockPos, blockState, linear, location));
         }
      }

      return list;
   }

   default Collection<TrackNodeLocation.DiscoveredLocation> getConnected(
      BlockGetter worldIn, BlockPos pos, BlockState state, boolean linear, @Nullable TrackNodeLocation connectedTo
   ) {
      BlockGetter world = (BlockGetter)(connectedTo != null && worldIn instanceof ServerLevel sl ? sl.getServer().getLevel(connectedTo.dimension) : worldIn);
      Vec3 center = Vec3.atBottomCenterOf(pos).add(0.0, this.getElevationAtCenter(world, pos, state), 0.0);
      List<TrackNodeLocation.DiscoveredLocation> list = new ArrayList<>();
      TrackShape shape = (TrackShape)state.getValue(TrackBlock.SHAPE);
      List<Vec3> trackAxes = this.getTrackAxes(world, pos, state);
      trackAxes.forEach(
         axis -> {
            BiFunction<Double, Boolean, Vec3> offsetFactory = (d, b) -> axis.scale(b ? d : -d).add(center);
            Function<Boolean, ResourceKey<Level>> dimensionFactory = b -> world instanceof Level l ? l.dimension() : Level.OVERWORLD;
            Function<Vec3, Integer> yOffsetFactory = v -> this.getYOffsetAt(world, pos, state, v);
            addToListIfConnected(
               connectedTo, list, offsetFactory, b -> shape.getNormal(), dimensionFactory, yOffsetFactory, axis, null, (b, v) -> getMaterialSimple(world, v)
            );
         }
      );
      return list;
   }

   static TrackMaterial getMaterialSimple(BlockGetter world, Vec3 pos) {
      return getMaterialSimple(world, pos, TrackMaterial.ANDESITE);
   }

   static TrackMaterial getMaterialSimple(BlockGetter world, Vec3 pos, TrackMaterial defaultMaterial) {
      if (defaultMaterial == null) {
         defaultMaterial = TrackMaterial.ANDESITE;
      }

      return world != null && world.getBlockState(BlockPos.containing(pos)).getBlock() instanceof ITrackBlock track ? track.getMaterial() : defaultMaterial;
   }

   static void addToListIfConnected(
      @Nullable TrackNodeLocation fromEnd,
      Collection<TrackNodeLocation.DiscoveredLocation> list,
      BiFunction<Double, Boolean, Vec3> offsetFactory,
      Function<Boolean, Vec3> normalFactory,
      Function<Boolean, ResourceKey<Level>> dimensionFactory,
      Function<Vec3, Integer> yOffsetFactory,
      Vec3 axis,
      BezierConnection viaTurn,
      BiFunction<Boolean, Vec3, TrackMaterial> materialFactory
   ) {
      Vec3 firstOffset = offsetFactory.apply(0.5, true);
      TrackNodeLocation.DiscoveredLocation firstLocation = new TrackNodeLocation.DiscoveredLocation(dimensionFactory.apply(true), firstOffset)
         .viaTurn(viaTurn)
         .materialA(materialFactory.apply(true, offsetFactory.apply(0.0, true)))
         .materialB(materialFactory.apply(true, offsetFactory.apply(1.0, true)))
         .withNormal(normalFactory.apply(true))
         .withDirection(axis)
         .withYOffset(yOffsetFactory.apply(firstOffset));
      Vec3 secondOffset = offsetFactory.apply(0.5, false);
      TrackNodeLocation.DiscoveredLocation secondLocation = new TrackNodeLocation.DiscoveredLocation(dimensionFactory.apply(false), secondOffset)
         .viaTurn(viaTurn)
         .materialA(materialFactory.apply(false, offsetFactory.apply(0.0, false)))
         .materialB(materialFactory.apply(false, offsetFactory.apply(1.0, false)))
         .withNormal(normalFactory.apply(false))
         .withDirection(axis)
         .withYOffset(yOffsetFactory.apply(secondOffset));
      if (!firstLocation.dimension.equals(secondLocation.dimension)) {
         firstLocation.forceNode();
         secondLocation.forceNode();
      }

      boolean skipFirst = false;
      boolean skipSecond = false;
      if (fromEnd != null) {
         boolean equalsFirst = firstLocation.equals(fromEnd);
         boolean equalsSecond = secondLocation.equals(fromEnd);
         if (!equalsFirst && !equalsSecond) {
            return;
         }

         if (equalsFirst) {
            skipFirst = true;
         }

         if (equalsSecond) {
            skipSecond = true;
         }
      }

      if (!skipFirst) {
         list.add(firstLocation);
      }

      if (!skipSecond) {
         list.add(secondLocation);
      }
   }

   @OnlyIn(Dist.CLIENT)
   <Self extends Affine<Self>> PartialModel prepareTrackOverlay(
      Affine<Self> var1,
      BlockGetter var2,
      BlockPos var3,
      BlockState var4,
      BezierTrackPointLocation var5,
      AxisDirection var6,
      TrackTargetingBehaviour.RenderedTrackOverlayType var7
   );

   @OnlyIn(Dist.CLIENT)
   PartialModel prepareAssemblyOverlay(BlockGetter var1, BlockPos var2, BlockState var3, Direction var4, PoseStack var5);

   default boolean isSlope(BlockGetter world, BlockPos pos, BlockState state) {
      return this.getTrackAxes(world, pos, state).get(0).y != 0.0;
   }

   default Pair<Vec3, AxisDirection> getNearestTrackAxis(BlockGetter world, BlockPos pos, BlockState state, Vec3 lookVec) {
      Vec3 best = null;
      double bestDiff = Double.MAX_VALUE;

      for (Vec3 vec3 : this.getTrackAxes(world, pos, state)) {
         for (int opposite : Iterate.positiveAndNegative) {
            double distanceTo = vec3.normalize().distanceTo(lookVec.scale((double)opposite));
            if (!(distanceTo > bestDiff)) {
               bestDiff = distanceTo;
               best = vec3;
            }
         }
      }

      return Pair.of(best, lookVec.dot(best.multiply(1.0, 0.0, 1.0).normalize()) < 0.0 ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE);
   }

   TrackMaterial getMaterial();
}

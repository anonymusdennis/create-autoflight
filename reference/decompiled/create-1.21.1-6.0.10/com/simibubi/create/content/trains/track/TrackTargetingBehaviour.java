package com.simibubi.create.content.trains.track;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.EdgeData;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackGraphHelper;
import com.simibubi.create.content.trains.graph.TrackGraphLocation;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.signal.SingleBlockEntityEdgePoint;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.List;
import java.util.UUID;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public class TrackTargetingBehaviour<T extends TrackEdgePoint> extends BlockEntityBehaviour {
   public static final BehaviourType<TrackTargetingBehaviour<?>> TYPE = new BehaviourType<>();
   private BlockPos targetTrack;
   private BezierTrackPointLocation targetBezier;
   private AxisDirection targetDirection;
   private UUID id;
   private Vec3 prevDirection;
   private Vec3 rotatedDirection;
   private CompoundTag migrationData;
   private EdgePointType<T> edgePointType;
   private T edgePoint;
   private boolean orthogonal;

   public TrackTargetingBehaviour(SmartBlockEntity be, EdgePointType<T> edgePointType) {
      super(be);
      this.edgePointType = edgePointType;
      this.targetDirection = AxisDirection.POSITIVE;
      this.targetTrack = BlockPos.ZERO;
      this.id = UUID.randomUUID();
      this.migrationData = null;
      this.orthogonal = false;
   }

   @Override
   public boolean isSafeNBT() {
      return true;
   }

   @Override
   public void write(CompoundTag nbt, Provider registries, boolean clientPacket) {
      nbt.putUUID("Id", this.id);
      nbt.put("TargetTrack", NbtUtils.writeBlockPos(this.targetTrack));
      nbt.putBoolean("Ortho", this.orthogonal);
      nbt.putBoolean("TargetDirection", this.targetDirection == AxisDirection.POSITIVE);
      if (this.rotatedDirection != null) {
         nbt.put("RotatedAxis", VecHelper.writeNBT(this.rotatedDirection));
      }

      if (this.prevDirection != null) {
         nbt.put("PrevAxis", VecHelper.writeNBT(this.prevDirection));
      }

      if (this.migrationData != null && !clientPacket) {
         nbt.put("Migrate", this.migrationData);
      }

      if (this.targetBezier != null) {
         CompoundTag bezierNbt = new CompoundTag();
         bezierNbt.putInt("Segment", this.targetBezier.segment());
         bezierNbt.put("Key", NbtUtils.writeBlockPos(this.targetBezier.curveTarget().subtract(this.getPos())));
         nbt.put("Bezier", bezierNbt);
      }

      super.write(nbt, registries, clientPacket);
   }

   @Override
   public void read(CompoundTag nbt, Provider registries, boolean clientPacket) {
      this.id = nbt.contains("Id") ? nbt.getUUID("Id") : UUID.randomUUID();
      this.targetTrack = NBTHelper.readBlockPos(nbt, "TargetTrack");
      this.targetDirection = nbt.getBoolean("TargetDirection") ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE;
      this.orthogonal = nbt.getBoolean("Ortho");
      if (nbt.contains("PrevAxis")) {
         this.prevDirection = VecHelper.readNBT(nbt.getList("PrevAxis", 6));
      }

      if (nbt.contains("RotatedAxis")) {
         this.rotatedDirection = VecHelper.readNBT(nbt.getList("RotatedAxis", 6));
      }

      if (nbt.contains("Migrate")) {
         this.migrationData = nbt.getCompound("Migrate");
      }

      if (clientPacket) {
         this.edgePoint = null;
      }

      if (nbt.contains("Bezier")) {
         CompoundTag bezierNbt = nbt.getCompound("Bezier");
         BlockPos key = NBTHelper.readBlockPos(bezierNbt, "Key");
         this.targetBezier = new BezierTrackPointLocation(key.offset(this.getPos()), bezierNbt.getInt("Segment"));
      }

      super.read(nbt, registries, clientPacket);
   }

   @Nullable
   public T getEdgePoint() {
      return this.edgePoint;
   }

   public void invalidateEdgePoint(CompoundTag migrationData) {
      this.migrationData = migrationData;
      this.edgePoint = null;
      this.blockEntity.sendData();
   }

   @Override
   public void tick() {
      super.tick();
      if (this.edgePoint == null) {
         this.edgePoint = this.createEdgePoint();
      }
   }

   public T createEdgePoint() {
      Level level = this.getWorld();
      boolean isClientSide = level.isClientSide;
      if (this.migrationData == null || isClientSide) {
         for (TrackGraph trackGraph : Create.RAILWAYS.sided(level).trackNetworks.values()) {
            T point = trackGraph.getPoint(this.edgePointType, this.id);
            if (point != null) {
               return point;
            }
         }
      }

      if (isClientSide) {
         return null;
      } else if (!this.hasValidTrack()) {
         return null;
      } else {
         TrackGraphLocation loc = this.determineGraphLocation();
         if (loc == null) {
            return null;
         } else {
            TrackGraph graph = loc.graph;
            TrackNode node1 = graph.locateNode((TrackNodeLocation)loc.edge.getFirst());
            TrackNode node2 = graph.locateNode((TrackNodeLocation)loc.edge.getSecond());
            TrackEdge edge = graph.getConnectionsFrom(node1).get(node2);
            if (edge == null) {
               return null;
            } else {
               T point = this.edgePointType.create();
               boolean front = this.getTargetDirection() == AxisDirection.POSITIVE;
               this.prevDirection = edge.getDirectionAt(loc.position).scale(front ? -1.0 : 1.0);
               if (this.rotatedDirection != null) {
                  double dot = this.prevDirection.dot(this.rotatedDirection);
                  if (dot < -0.85F) {
                     this.rotatedDirection = null;
                     this.targetDirection = this.targetDirection.opposite();
                     return null;
                  }

                  this.rotatedDirection = null;
               }

               double length = edge.getLength();
               CompoundTag data = this.migrationData;
               this.migrationData = null;
               this.orthogonal = this.targetBezier == null;
               Vec3 direction = edge.getDirection(true);
               int nonZeroComponents = 0;

               for (Axis axis : Iterate.axes) {
                  nonZeroComponents += direction.get(axis) != 0.0 ? 1 : 0;
               }

               this.orthogonal &= nonZeroComponents <= 1;
               EdgeData signalData = edge.getEdgeData();
               if (signalData.hasPoints()) {
                  for (EdgePointType<?> otherType : EdgePointType.TYPES.values()) {
                     TrackEdgePoint otherPoint = signalData.get((EdgePointType<TrackEdgePoint>)otherType, loc.position);
                     if (otherPoint != null) {
                        if (otherType == this.edgePointType) {
                           if (!otherPoint.canMerge()) {
                              return null;
                           }

                           otherPoint.blockEntityAdded(this.blockEntity, front);
                           this.id = otherPoint.getId();
                           this.blockEntity.notifyUpdate();
                           return (T)otherPoint;
                        }

                        if (!otherPoint.canCoexistWith(this.edgePointType, front)) {
                           return null;
                        }
                     }
                  }
               }

               if (data != null) {
                  point.read(data, level.registryAccess(), true, DimensionPalette.read(data));
               }

               point.setId(this.id);
               boolean reverseEdge = front || point instanceof SingleBlockEntityEdgePoint;
               point.setLocation(reverseEdge ? loc.edge : loc.edge.swap(), reverseEdge ? loc.position : length - loc.position);
               point.blockEntityAdded(this.blockEntity, front);
               loc.graph.addPoint(this.edgePointType, point);
               this.blockEntity.sendData();
               return point;
            }
         }
      }
   }

   @Override
   public void destroy() {
      super.destroy();
      if (this.edgePoint != null && !this.getWorld().isClientSide) {
         this.edgePoint.blockEntityRemoved(this.getPos(), this.getTargetDirection() == AxisDirection.POSITIVE);
      }
   }

   @Override
   public BehaviourType<?> getType() {
      return TYPE;
   }

   public boolean isOnCurve() {
      return this.targetBezier != null;
   }

   public boolean isOrthogonal() {
      return this.orthogonal;
   }

   public boolean hasValidTrack() {
      return this.getTrackBlockState().getBlock() instanceof ITrackBlock;
   }

   public ITrackBlock getTrack() {
      return (ITrackBlock)this.getTrackBlockState().getBlock();
   }

   public BlockState getTrackBlockState() {
      return this.getWorld().getBlockState(this.getGlobalPosition());
   }

   public BlockPos getGlobalPosition() {
      return this.targetTrack.offset(this.blockEntity.getBlockPos());
   }

   public BlockPos getPositionForMapMarker() {
      BlockPos target = this.targetTrack.offset(this.blockEntity.getBlockPos());
      if (this.targetBezier != null && this.getWorld().getBlockEntity(target) instanceof TrackBlockEntity tbe) {
         BezierConnection bc = tbe.getConnections().get(this.targetBezier.curveTarget());
         if (bc == null) {
            return target;
         } else {
            double length = (double)Mth.floor(bc.getLength() * 2.0);
            int seg = this.targetBezier.segment() + 1;
            double t = (double)seg / length;
            return BlockPos.containing(bc.getPosition(t));
         }
      } else {
         return target;
      }
   }

   public AxisDirection getTargetDirection() {
      return this.targetDirection;
   }

   public BezierTrackPointLocation getTargetBezier() {
      return this.targetBezier;
   }

   public TrackGraphLocation determineGraphLocation() {
      Level level = this.getWorld();
      BlockPos pos = this.getGlobalPosition();
      BlockState state = this.getTrackBlockState();
      ITrackBlock track = this.getTrack();
      List<Vec3> trackAxes = track.getTrackAxes(level, pos, state);
      AxisDirection targetDirection = this.getTargetDirection();
      return this.targetBezier != null
         ? TrackGraphHelper.getBezierGraphLocationAt(level, pos, targetDirection, this.targetBezier)
         : TrackGraphHelper.getGraphLocationAt(level, pos, targetDirection, trackAxes.get(0));
   }

   @OnlyIn(Dist.CLIENT)
   public static void render(
      LevelAccessor level,
      BlockPos pos,
      AxisDirection direction,
      BezierTrackPointLocation bezier,
      PoseStack ms,
      MultiBufferSource buffer,
      int light,
      int overlay,
      TrackTargetingBehaviour.RenderedTrackOverlayType type,
      float scale
   ) {
      if (!(level instanceof SchematicLevel) || level instanceof PonderLevel) {
         BlockState trackState = level.getBlockState(pos);
         if (trackState.getBlock() instanceof ITrackBlock track) {
            ms.pushPose();
            PoseTransformStack msr = TransformStack.of(ms);
            PartialModel partial = track.prepareTrackOverlay(msr, level, pos, trackState, bezier, direction, type);
            if (partial != null) {
               ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial(partial, trackState).translate(0.5, 0.0, 0.5)).scale(scale))
                     .translate(-0.5, 0.0, -0.5))
                  .light(LevelRenderer.getLightColor(level, pos))
                  .renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
            }

            ms.popPose();
         }
      }
   }

   public void transform(BlockEntity be, StructureTransform transform) {
      this.id = UUID.randomUUID();
      this.targetTrack = transform.applyWithoutOffset(this.targetTrack);
      if (this.prevDirection != null) {
         this.rotatedDirection = transform.applyWithoutOffsetUncentered(this.prevDirection);
      }

      if (this.targetBezier != null) {
         this.targetBezier = new BezierTrackPointLocation(
            transform.applyWithoutOffset(this.targetBezier.curveTarget().subtract(this.getPos())).offset(this.getPos()), this.targetBezier.segment()
         );
      }

      this.blockEntity.notifyUpdate();
   }

   public static enum RenderedTrackOverlayType {
      STATION,
      SIGNAL,
      DUAL_SIGNAL,
      OBSERVER;
   }
}

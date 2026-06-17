package com.simibubi.create.content.logistics.factoryBoard;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class FactoryPanelConnection {
   public static final Codec<FactoryPanelConnection> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
               FactoryPanelPosition.CODEC.fieldOf("position").forGetter(i -> i.from),
               Codec.INT.fieldOf("amount").forGetter(i -> i.amount),
               Codec.INT.fieldOf("arrow_bending").forGetter(i -> i.arrowBendMode)
            )
            .apply(instance, FactoryPanelConnection::new)
   );
   public FactoryPanelPosition from;
   public int amount;
   public List<Direction> path;
   public int arrowBendMode;
   public boolean success;
   public WeakReference<Object> cachedSource;
   private int arrowBendModeCurrentPathUses;

   public FactoryPanelConnection(FactoryPanelPosition from, int amount) {
      this(from, amount, -1);
   }

   public FactoryPanelConnection(FactoryPanelPosition from, int amount, int arrowBendMode) {
      this.from = from;
      this.amount = amount;
      this.arrowBendMode = arrowBendMode;
      this.path = new ArrayList<>();
      this.success = true;
      this.arrowBendModeCurrentPathUses = 0;
      this.cachedSource = new WeakReference<>(null);
   }

   public List<Direction> getPath(Level level, BlockState state, FactoryPanelPosition to) {
      if (!this.path.isEmpty() && this.arrowBendModeCurrentPathUses == this.arrowBendMode) {
         return this.path;
      } else {
         boolean findSuitable = this.arrowBendMode == -1;
         this.arrowBendModeCurrentPathUses = this.arrowBendMode;
         FactoryPanelBehaviour fromBehaviour = FactoryPanelBehaviour.at(level, to);
         Vec3 diff = this.calculatePathDiff(state, to);
         Vec3 start = fromBehaviour != null
            ? fromBehaviour.getSlotPositioning().getLocalOffset(level, to.pos(), state).add(Vec3.atLowerCornerOf(to.pos()))
            : Vec3.ZERO;
         float xRot = (180.0F / (float)Math.PI) * FactoryPanelBlock.getXRot(state);
         float yRot = (180.0F / (float)Math.PI) * FactoryPanelBlock.getYRot(state);

         label164:
         for (int actualMode = 0; actualMode <= 4; actualMode++) {
            this.path.clear();
            if (findSuitable || actualMode == this.arrowBendMode) {
               boolean desperateOption = actualMode == 4;
               BlockPos toTravelFirst = BlockPos.ZERO;
               BlockPos toTravelLast = BlockPos.containing(diff.scale(2.0).add(0.1, 0.1, 0.1));
               if (actualMode > 1) {
                  boolean flipX = diff.x > 0.0 ^ actualMode % 2 == 1;
                  boolean flipZ = diff.z > 0.0 ^ actualMode % 2 == 0;
                  int ceilX = Mth.positiveCeilDiv(toTravelLast.getX(), 2);
                  int ceilZ = Mth.positiveCeilDiv(toTravelLast.getZ(), 2);
                  int floorZ = Mth.floorDiv(toTravelLast.getZ(), 2);
                  int floorX = Mth.floorDiv(toTravelLast.getX(), 2);
                  toTravelFirst = new BlockPos(flipX ? floorX : ceilX, 0, flipZ ? floorZ : ceilZ);
                  toTravelLast = new BlockPos(!flipX ? floorX : ceilX, 0, !flipZ ? floorZ : ceilZ);
               }

               Direction lastDirection = null;
               Direction currentDirection = null;

               for (BlockPos toTravel : List.of(toTravelFirst, toTravelLast)) {
                  boolean zIsFarther = Math.abs(toTravel.getZ()) > Math.abs(toTravel.getX());
                  boolean zIsPreferred = desperateOption ? zIsFarther : actualMode % 2 == 1;
                  List<Direction> directionOrder = zIsPreferred
                     ? List.of(Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST)
                     : List.of(Direction.WEST, Direction.EAST, Direction.SOUTH, Direction.NORTH);

                  for (int i = 0; i < 100 && !toTravel.equals(BlockPos.ZERO); i++) {
                     for (Direction d : directionOrder) {
                        if ((lastDirection == null || d != lastDirection.getOpposite())
                           && (
                              currentDirection == null
                                 || toTravel.relative(d).distManhattan(BlockPos.ZERO) < toTravel.relative(currentDirection).distManhattan(BlockPos.ZERO)
                           )) {
                           currentDirection = d;
                        }
                     }

                     lastDirection = currentDirection;
                     toTravel = toTravel.relative(currentDirection);
                     this.path.add(currentDirection);
                  }
               }

               if (!findSuitable || desperateOption) {
                  break;
               }

               BlockPos travelled = BlockPos.ZERO;

               for (int i = 0; i < this.path.size() - 1; i++) {
                  Direction dx = this.path.get(i);
                  travelled = travelled.relative(dx);
                  Vec3 testOffset = Vec3.atLowerCornerOf(travelled).scale(0.5);
                  testOffset = VecHelper.rotate(testOffset, 180.0, Axis.Y);
                  testOffset = VecHelper.rotate(testOffset, (double)(xRot + 90.0F), Axis.X);
                  testOffset = VecHelper.rotate(testOffset, (double)yRot, Axis.Y);
                  Vec3 v = start.add(testOffset);
                  if (!level.noCollision(new AABB(v, v).inflate(0.0078125))) {
                     continue label164;
                  }
               }
               break;
            }
         }

         return this.path;
      }
   }

   public Vec3 calculatePathDiff(BlockState state, FactoryPanelPosition to) {
      float xRot = (180.0F / (float)Math.PI) * FactoryPanelBlock.getXRot(state);
      float yRot = (180.0F / (float)Math.PI) * FactoryPanelBlock.getYRot(state);
      int slotDiffx = to.slot().xOffset - this.from.slot().xOffset;
      int slotDiffY = to.slot().yOffset - this.from.slot().yOffset;
      Vec3 diff = Vec3.atLowerCornerOf(to.pos().subtract(this.from.pos()));
      diff = VecHelper.rotate(diff, (double)(-yRot), Axis.Y);
      diff = VecHelper.rotate(diff, (double)(-xRot - 90.0F), Axis.X);
      diff = VecHelper.rotate(diff, -180.0, Axis.Y);
      diff = diff.add((double)slotDiffx * 0.5, 0.0, (double)slotDiffY * 0.5);
      return diff.multiply(1.0, 0.0, 1.0);
   }
}

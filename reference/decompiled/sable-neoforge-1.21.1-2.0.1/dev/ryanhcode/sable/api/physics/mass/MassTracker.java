package dev.ryanhcode.sable.api.physics.mass;

import dev.ryanhcode.sable.api.block.BlockSubLevelCustomCenterOfMass;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.physics.chunk.VoxelNeighborhoodState;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.util.SableMathUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Matrix3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class MassTracker implements MassData {
   private static final AABB UNIT_BOUNDS = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
   public static BiFunction<BlockGetter, BlockState, Vector3dc> BLOCK_CENTER_OF_MASS = new BiFunction<BlockGetter, BlockState, Vector3dc>() {
      private final Int2ObjectOpenHashMap<Vector3dc> cache = new Int2ObjectOpenHashMap();

      public Vector3dc apply(BlockGetter blockGetter, BlockState state) {
         return (Vector3dc)this.cache.computeIfAbsent(state.hashCode(), x -> {
            if (state.isAir()) {
               return JOMLConversion.HALF;
            } else if (state.getBlock() instanceof BlockSubLevelCustomCenterOfMass customCenterOfMass) {
               return customCenterOfMass.getCenterOfMass(blockGetter, state);
            } else {
               VoxelShape shape = state.getCollisionShape(blockGetter, BlockPos.ZERO);
               if (shape.isEmpty()) {
                  return JOMLConversion.HALF;
               } else if (state.isCollisionShapeFullBlock(blockGetter, BlockPos.ZERO)) {
                  return JOMLConversion.HALF;
               } else {
                  AABB bounds = shape.bounds().intersect(MassTracker.UNIT_BOUNDS);
                  return JOMLConversion.toJOML(bounds.getCenter());
               }
            }
         });
      }
   };
   private static final Matrix3d BLOCK_INERTIA = new Matrix3d();
   private double mass = 0.0;
   private Matrix3d inertiaTensor;
   private double inverseMass;
   private Matrix3d inverseInertiaTensor;
   @Nullable
   private Vector3d centerOfMass = null;

   public MassTracker() {
      this.inertiaTensor = new Matrix3d().zero();
      this.inverseInertiaTensor = new Matrix3d().zero();
   }

   public static MassTracker build(BlockGetter blockGetter, BoundingBox3ic bounds) {
      double mass = 0.0;
      Vector3d centerOfMass = new Vector3d();
      Matrix3d inertiaTensor = new Matrix3d().zero();
      MutableBlockPos blockPos = new MutableBlockPos();
      Vector3d blockCenter = new Vector3d();
      int blockCount = 0;

      for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
         for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
               BlockState state = blockGetter.getBlockState(blockPos.set(x, y, z));
               if (VoxelNeighborhoodState.isSolid(blockGetter, blockPos, state)) {
                  double blockMass = PhysicsBlockPropertyHelper.getMass(blockGetter, blockPos, state);
                  blockCenter.set((double)x, (double)y, (double)z).add(BLOCK_CENTER_OF_MASS.apply(blockGetter, state));
                  mass += blockMass;
                  centerOfMass.fma(blockMass, blockCenter);
                  blockCount++;
               }
            }
         }
      }

      if (blockCount == 0) {
         MassTracker tracker = new MassTracker();
         tracker.mass = 0.0;
         tracker.centerOfMass = null;
         tracker.inertiaTensor = new Matrix3d().zero();
         tracker.inverseInertiaTensor = new Matrix3d().zero();
         return tracker;
      } else {
         centerOfMass.div(mass);

         for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
               for (int zx = bounds.minZ(); zx <= bounds.maxZ(); zx++) {
                  BlockState state = blockGetter.getBlockState(blockPos.set(x, y, zx));
                  if (VoxelNeighborhoodState.isSolid(blockGetter, blockPos, state)) {
                     blockCenter.set((double)x, (double)y, (double)zx).add(BLOCK_CENTER_OF_MASS.apply(blockGetter, state));
                     double blockMass = PhysicsBlockPropertyHelper.getMass(blockGetter, blockPos, state);
                     Vec3 blockInertia = PhysicsBlockPropertyHelper.getInertia(blockGetter, blockPos, state);
                     Vector3d r = blockCenter.sub(centerOfMass);
                     addBlockInertia(r, blockMass, inertiaTensor, blockInertia);
                  }
               }
            }
         }

         Matrix3d inverseInertiaTensor = new Matrix3d(inertiaTensor).invert();
         double inverseMass = 1.0 / mass;
         MassTracker tracker = new MassTracker();
         tracker.centerOfMass = centerOfMass;
         tracker.mass = mass;
         tracker.inverseMass = inverseMass;
         tracker.inertiaTensor = inertiaTensor;
         tracker.inverseInertiaTensor = inverseInertiaTensor;
         return tracker;
      }
   }

   private static Matrix3d addBlockInertia(Vector3d blockPos, double blockMass, Matrix3d dest, @Nullable Vec3 blockInertia) {
      if (blockInertia == null) {
         BLOCK_INERTIA.identity().scale(blockMass / 6.0);
      } else {
         BLOCK_INERTIA.identity();
         BLOCK_INERTIA.m00 = blockInertia.x * blockMass;
         BLOCK_INERTIA.m11 = blockInertia.y * blockMass;
         BLOCK_INERTIA.m22 = blockInertia.z * blockMass;
      }

      dest.add(BLOCK_INERTIA);
      SableMathUtils.fmaInertiaTensor(blockPos, blockMass, dest);
      return dest;
   }

   public void addBlockMass(BlockGetter blockGetter, BlockState state, BlockPos blockPos, double blockMass, @Nullable Vec3 blockInertia) {
      double oldMass = this.mass;
      double newMass = oldMass + blockMass;
      Vector3d blockCenter = new Vector3d((double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ())
         .add(BLOCK_CENTER_OF_MASS.apply(blockGetter, state));
      if (this.centerOfMass == null) {
         this.centerOfMass = new Vector3d(blockCenter);
      }

      Vector3d blockCenterFromCOM = new Vector3d(blockCenter).sub(this.centerOfMass);
      addBlockInertia(blockCenterFromCOM, blockMass, this.inertiaTensor, blockInertia);
      this.mass = newMass;
      this.inverseMass = 1.0 / newMass;
      this.moveCenterOfMass(new Vector3d(this.centerOfMass).mul(oldMass).add(blockCenter.mul(blockMass)).div(newMass));
   }

   public void moveCenterOfMass(Vector3d newCenterOfMass) {
      Vector3d diff = new Vector3d(newCenterOfMass).sub(this.centerOfMass);
      Matrix3d outerProduct = new Matrix3d(
         diff.x * diff.x,
         diff.y * diff.x,
         diff.z * diff.x,
         diff.x * diff.y,
         diff.y * diff.y,
         diff.z * diff.y,
         diff.x * diff.z,
         diff.y * diff.z,
         diff.z * diff.z
      );
      Matrix3d inertia = new Matrix3d().scale(diff.lengthSquared()).sub(outerProduct).scale(this.mass);
      this.inertiaTensor.sub(inertia);
      this.inverseInertiaTensor = new Matrix3d(this.inertiaTensor).invert();
      this.centerOfMass.set(newCenterOfMass);
   }

   @Override
   public double getInverseMass() {
      return this.inverseMass;
   }

   @Override
   public Matrix3dc getInverseInertiaTensor() {
      return this.inverseInertiaTensor;
   }

   @Override
   public Matrix3dc getInertiaTensor() {
      return this.inertiaTensor;
   }

   @Override
   public double getMass() {
      return this.mass;
   }

   @Override
   public Vector3dc getCenterOfMass() {
      return this.centerOfMass;
   }
}

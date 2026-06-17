package dev.eriksonn.aeronautics.content.blocks.hot_air.balloon;

import dev.eriksonn.aeronautics.content.blocks.hot_air.BlockEntityLiftingGasProvider;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonLayerData;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonLayerGraph;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.map.SavedBalloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas.LiftingGasData;
import dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas.LiftingGasHolder;
import dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas.LiftingGasType;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper.AssemblyTransform;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.util.LevelAccelerator;
import dev.ryanhcode.sable.util.SableMathUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Matrix3d;
import org.joml.Vector3d;

public class ServerBalloon extends Balloon {
   private final Map<LiftingGasType, LiftingGasData> gasAmounts = new Object2ObjectOpenHashMap();
   private final Matrix3d outerProduct = new Matrix3d();
   private final Matrix3d translatedOuterProduct = new Matrix3d();
   private final Vector3d averagePosition = new Vector3d();
   private final Vector3d translatedAveragePosition = new Vector3d();
   private final Vector3d physicsOrigin;
   private double totalLift;
   private double totalFilledVolume;
   private double totalTargetVolume;
   private double totalVolumeChange;
   private boolean leaking = false;
   static final Vector3d force = new Vector3d();
   static final Vector3d torque = new Vector3d();
   static final Vector3d localAveragePosition = new Vector3d();
   static final Vector3d worldCenter = new Vector3d();
   static final Vector3d gradient = new Vector3d();
   static final Vector3d gravity = new Vector3d();
   static final Vector3d POSITION_TEMP = new Vector3d();

   @Internal
   public ServerBalloon(
      Level level, LevelAccelerator accelerator, BlockPos controllerPos, BalloonLayerGraph graph, ObjectArrayList<BlockEntityLiftingGasProvider> heaters
   ) {
      super(level, accelerator, controllerPos, graph, heaters);
      this.physicsOrigin = new Vector3d((double)controllerPos.getX(), (double)controllerPos.getY(), (double)controllerPos.getZ());
      this.onRebuilt();
   }

   public void translateMatrices() {
      this.translatedOuterProduct.set(this.outerProduct);
      SableMathUtils.fmaOuterProduct(this.averagePosition, this.averagePosition, (double)(-this.getCapacity()), this.translatedOuterProduct);
   }

   @Override
   protected void checkHeaters() {
      super.checkHeaters();

      for (LiftingGasData data : this.gasAmounts.values()) {
         data.target = 0.0;
      }

      if (!this.leaking) {
         for (BlockEntityLiftingGasProvider heater : this.heaters) {
            this.gasAmounts.compute(heater.getLiftingGasType(), (k, v) -> {
               if (v == null) {
                  v = new LiftingGasData();
               }

               v.target = v.target + heater.getGasOutput();
               return (LiftingGasData)v;
            });
         }
      }
   }

   public void applyForces(double timeStep) {
      int capacity = this.getCapacity();
      if (capacity > 0) {
         ServerSubLevel subLevel = (ServerSubLevel)Sable.HELPER.getContaining(this.level, this.controllerPos);
         if (subLevel != null && this.totalFilledVolume != 0.0) {
            this.translateMatrices();
            Level level = subLevel.getLevel();
            Pose3d pose = subLevel.logicalPose();
            this.translatedAveragePosition.set(this.averagePosition).add(this.physicsOrigin);
            localAveragePosition.set(this.translatedAveragePosition).sub(pose.rotationPoint());
            worldCenter.set(localAveragePosition);
            pose.orientation().transform(worldCenter).add(pose.position());
            DimensionPhysicsData.getGravity(level, worldCenter, gravity);
            pose.orientation().transformInverse(gravity);
            double pressure = DimensionPhysicsData.getAirPressure(level, worldCenter);
            if (!(pressure < 1.0E-5) && gravity.lengthSquared() != 0.0) {
               double diff = 0.1;
               double pressureX = DimensionPhysicsData.getAirPressure(level, gradient.set(0.1, 0.0, 0.0).add(worldCenter)) - pressure;
               double pressureY = DimensionPhysicsData.getAirPressure(level, gradient.set(0.0, 0.1, 0.0).add(worldCenter)) - pressure;
               double pressureZ = DimensionPhysicsData.getAirPressure(level, gradient.set(0.0, 0.0, 0.1).add(worldCenter)) - pressure;
               gradient.set(pressureX, pressureY, pressureZ).div(0.1);
               pose.orientation().transformInverse(gradient);
               force.set(gravity).mul(-this.totalLift);
               torque.set(localAveragePosition).mul(pressure);
               torque.fma(1.0 / (double)capacity, this.translatedOuterProduct.transform(gradient));
               torque.cross(force);
               force.mul(pressure);
               force.mul(timeStep);
               torque.mul(timeStep);
               QueuedForceGroup forceGroup = subLevel.getOrCreateQueuedForceGroup((ForceGroup)ForceGroups.BALLOON_LIFT.get());
               forceGroup.getForceTotal().applyLinearAndAngularImpulse(force, torque);
               if (subLevel.isTrackingIndividualQueuedForces()) {
                  forceGroup.recordPointForce(
                     new Vector3d(this.translatedAveragePosition).fma(1.0 / (pressure * (double)capacity), gradient), new Vector3d(force)
                  );
               }
            }
         }
      }
   }

   @Override
   protected void onRebuilt() {
      this.outerProduct.zero();
      this.averagePosition.zero();
      List[] var1 = this.graph.getAllLayers();
      int var2 = var1.length;

      for (int var3 = 0; var3 < var2; var3++) {
         for (BalloonLayerData layer : var1[var3]) {
            Iterator<BlockPos> iter = layer.nonSolidBlockIterator();

            while (iter.hasNext()) {
               BlockPos pos = iter.next();
               POSITION_TEMP.set((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5).sub(this.physicsOrigin);
               this.averagePosition.add(POSITION_TEMP);
            }
         }
      }

      this.averagePosition.div((double)this.getCapacity());
      var1 = this.graph.getAllLayers();
      var2 = var1.length;

      for (int var14 = 0; var14 < var2; var14++) {
         for (BalloonLayerData layer : var1[var14]) {
            Iterator<BlockPos> iter = layer.nonSolidBlockIterator();

            while (iter.hasNext()) {
               BlockPos pos = iter.next();
               int x = pos.getX();
               int y = pos.getY();
               int z = pos.getZ();
               POSITION_TEMP.set((double)x + 0.5, (double)y + 0.5, (double)z + 0.5).sub(this.physicsOrigin);
               SableMathUtils.fmaOuterProduct(POSITION_TEMP, POSITION_TEMP, 1.0, this.outerProduct);
            }
         }
      }

      this.leaking = false;
   }

   @Override
   protected void onHotAirAdded(BlockPos pos) {
      POSITION_TEMP.set((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5).sub(this.physicsOrigin);
      this.averagePosition.mul((double)(this.getCapacity() - 1)).add(POSITION_TEMP).div((double)this.getCapacity());
      SableMathUtils.fmaOuterProduct(POSITION_TEMP, POSITION_TEMP, 1.0, this.outerProduct);
   }

   @Override
   protected void onHotAirRemoved(BlockPos pos) {
      POSITION_TEMP.set((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5).sub(this.physicsOrigin);
      this.averagePosition.mul((double)(this.getCapacity() + 1)).sub(POSITION_TEMP).div((double)this.getCapacity());
      SableMathUtils.fmaOuterProduct(POSITION_TEMP, POSITION_TEMP, -1.0, this.outerProduct);
   }

   @Override
   protected void onHotAirRemoved(Iterable<BlockPos> iterable) {
      super.onHotAirRemoved(iterable);

      for (BlockPos blockPos : iterable) {
         int y = blockPos.getY();
         int x = blockPos.getX();
         int z = blockPos.getZ();
         POSITION_TEMP.set((double)x + 0.5, (double)y + 0.5, (double)z + 0.5).sub(this.physicsOrigin);
         this.averagePosition.mul((double)this.capacity).sub(POSITION_TEMP).div((double)(this.capacity - 1));
         SableMathUtils.fmaOuterProduct(POSITION_TEMP, POSITION_TEMP, -1.0, this.outerProduct);
         this.capacity--;
      }
   }

   @Override
   public boolean isValid() {
      return this.totalTargetVolume > 0.05 || this.totalFilledVolume > 0.05;
   }

   @Override
   public void tick() {
      super.tick();
      this.updateGasAmounts();
   }

   public void updateGasAmounts() {
      int capacity = this.getCapacity();
      this.totalTargetVolume = 0.0;

      for (LiftingGasData data : this.gasAmounts.values()) {
         this.totalTargetVolume = this.totalTargetVolume + data.target;
      }

      double scale = Math.min((double)capacity / this.totalTargetVolume, 1.0);
      double totalDesiredVolume = 0.0;

      for (Entry<LiftingGasType, LiftingGasData> entry : this.gasAmounts.entrySet()) {
         LiftingGasData data = entry.getValue();
         LiftingGasType type = entry.getKey();
         double diff = data.target * scale - data.amount;
         data.nudge = diff > 0.0 ? diff / type.getFillingTime() : (diff < 0.0 ? diff / type.getEmptyingTime() : 0.0);
         if (type.getResponsivenessAdjustmentFactor() > 0.0 && type.getResponsivenessAdjustmentRange() > 0.0) {
            double x = diff / ((double)capacity * type.getResponsivenessAdjustmentRange());
            data.nudge = data.nudge * (1.0 + type.getResponsivenessAdjustmentFactor() / (1.0 + 3.0 * x * x));
         }

         totalDesiredVolume += data.amount + data.nudge;
      }

      this.totalLift = 0.0;
      this.totalFilledVolume = 0.0;
      this.totalVolumeChange = 0.0;

      for (Entry<LiftingGasType, LiftingGasData> entry : this.gasAmounts.entrySet()) {
         LiftingGasData data = entry.getValue();
         data.amount = data.amount + data.nudge;
         this.totalLift = this.totalLift + data.amount * entry.getKey().getLiftStrength();
         this.totalFilledVolume = this.totalFilledVolume + data.amount;
         this.totalVolumeChange = this.totalVolumeChange + data.nudge;
      }

      this.totalTargetVolume = Math.min(this.totalTargetVolume, (double)capacity);
   }

   @Override
   public void merge(Balloon other) {
      super.merge(other);
      if (other instanceof ServerBalloon otherServerBalloon) {
         for (Entry<LiftingGasType, LiftingGasData> entry : otherServerBalloon.gasAmounts.entrySet()) {
            LiftingGasType type = entry.getKey();
            LiftingGasData data = entry.getValue();
            LiftingGasData var10000 = this.gasAmounts.computeIfAbsent(type, x -> new LiftingGasData());
            var10000.amount = var10000.amount + data.amount;
         }
      }
   }

   @Override
   public void setLeaking() {
      this.leaking = true;
   }

   public Vec3 getCenter() {
      return JOMLConversion.toMojang(this.averagePosition).add(this.physicsOrigin.x(), this.physicsOrigin.y(), this.physicsOrigin.z());
   }

   public double getTotalLift() {
      return this.totalLift;
   }

   public double getTotalFilledVolume() {
      return this.totalFilledVolume;
   }

   public double getTotalTargetVolume() {
      return this.totalTargetVolume;
   }

   public double getTotalVolumeChange() {
      return this.totalVolumeChange;
   }

   @Override
   public boolean shouldSpawnGust(BlockPos pos) {
      float percentHeight = ((float)pos.getY() + 0.5F - (float)this.bounds.minY) / this.getHeight();
      return (double)percentHeight > 1.0 - Math.clamp(this.totalFilledVolume / (double)this.getCapacity(), 0.0, 1.0);
   }

   @Override
   public void spawnGust(Level level, BlockPos pos, Direction dir) {
      int contributingGases = 0;

      for (LiftingGasHolder liftingGasHolder : this.getLiftingGasHolders()) {
         if (liftingGasHolder.data().amount > 0.0) {
            contributingGases++;
         }
      }

      if (contributingGases != 0) {
         boolean canSpawnGust = true;
         double nudge = 1.0 / (double)contributingGases;

         for (LiftingGasHolder liftingGasHolderx : this.getLiftingGasHolders()) {
            if (liftingGasHolderx.data().amount < nudge) {
               canSpawnGust = false;
               liftingGasHolderx.data().amount = 0.0;
            } else {
               liftingGasHolderx.data().amount -= nudge;
            }
         }

         if (canSpawnGust) {
            super.spawnGust(level, pos, dir);
         }
      }
   }

   @Override
   public void setAssembling(AssemblyTransform transform) {
      super.setAssembling(transform);
      this.physicsOrigin.set((double)this.controllerPos.getX(), (double)this.controllerPos.getY(), (double)this.controllerPos.getZ());
   }

   public void loadFrom(SavedBalloon unloaded) {
      for (LiftingGasHolder entry : unloaded.gasData()) {
         LiftingGasType type = entry.type();
         LiftingGasData data = entry.data();
         this.gasAmounts.put(type, data);
      }
   }

   public List<LiftingGasHolder> getLiftingGasHolders() {
      List<LiftingGasHolder> holders = new ObjectArrayList();

      for (Entry<LiftingGasType, LiftingGasData> entry : this.gasAmounts.entrySet()) {
         holders.add(new LiftingGasHolder(entry.getKey(), entry.getValue()));
      }

      return holders;
   }
}

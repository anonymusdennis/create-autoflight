package dev.eriksonn.aeronautics.content.blocks.propeller.behaviour;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.equipment.armor.DivingBootsItem;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.eriksonn.aeronautics.content.particle.PropellerAirParticleData;
import dev.eriksonn.aeronautics.data.AeroLang;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.propeller.BlockEntityPropeller;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class PropellerActorBehaviour extends BlockEntityBehaviour implements IHaveGoggleInformation {
   public static final BehaviourType<PropellerActorBehaviour> TYPE = new BehaviourType("prop_behaviour");
   private static final int MAX_ACCELERATION = 5;
   private static final Vector3d STORED_MUT_POS = new Vector3d();
   private static final Vector3d STORED_TRANSFORMED_THRUST = new Vector3d();
   private static final Vector3d TEMP_CLIP_START = new Vector3d();
   private static final Vector3d TEMP_CLIP_END = new Vector3d();
   private static final Vector3d globalThrust = new Vector3d();
   private static final Vector3d relativeDiff = new Vector3d();
   private static final Vector3d normal = new Vector3d();
   private static final Vector3d particleVelocity = new Vector3d();
   protected final BlockEntityPropeller propeller;
   public double updatedParticleAmount;
   public int maxParticleAmount = 20;
   public double particleSmoothing = 10.0;
   public double radius = 0.0;
   protected Supplier<Double> particleAmountUpdater;
   protected BiConsumer<Vector3d, RandomSource> particlePositionUpdater;
   protected List<PropellerActorBehaviour.PropellerLayer> propellerLayers = new ObjectArrayList();
   private Vector3dc thrustDirection;

   public PropellerActorBehaviour(SmartBlockEntity be, BlockEntityPropeller propeller) {
      super(be);
      this.propeller = propeller;
   }

   public void addPropellerLayer(PropellerActorBehaviour.PropellerLayer layer) {
      this.radius = Math.max(this.radius, layer.outerRadius);
      this.propellerLayers.add(layer);
      this.propellerLayers.sort(Comparator.comparingDouble(propellerLayer -> propellerLayer.offset));
   }

   public void addSimpleLayer(double offset, double radius) {
      this.addPropellerLayer(new PropellerActorBehaviour.PropellerLayer(offset, 0.0, radius));
   }

   public List<PropellerActorBehaviour.PropellerLayer> getLayers() {
      return this.propellerLayers;
   }

   public void tick() {
      this.updatedParticleAmount = this.particleAmountUpdater.get();
      super.tick();
   }

   public void pushEntities() {
      if (!this.propellerLayers.isEmpty()) {
         double thrust = this.propeller.getThrust();
         Direction direction = this.propeller.getBlockDirection();
         double thrustFlowMult = Math.signum(thrust);
         Quaternionf quat = direction.getRotation();
         double dist = this.getParticleRange();
         double radius = 0.0;
         double offsetMax = Double.MIN_VALUE;
         double offsetMin = Double.MAX_VALUE;
         double d1 = Math.max(dist, 0.0);
         double d0 = Math.min(dist, 0.0);

         for (PropellerActorBehaviour.PropellerLayer layer : this.propellerLayers) {
            radius = Math.max(radius, layer.outerRadius);
            offsetMax = Math.max(offsetMax, layer.offset + d1);
            offsetMin = Math.min(offsetMin, layer.offset + d0);
         }

         Vector3d max = new Vector3d(radius, offsetMax, radius);
         Vector3d min = new Vector3d(-radius, offsetMin, -radius);
         quat.transform(max);
         quat.transform(min);
         min.add(JOMLConversion.toJOML(this.getPos().getCenter()));
         max.add(JOMLConversion.toJOML(this.getPos().getCenter()));
         BoundingBox3d aabb = new BoundingBox3d(min.x, min.y, min.z, max.x, max.y, max.z);
         STORED_TRANSFORMED_THRUST.set(this.thrustDirection);
         SubLevel subLevel = Sable.HELPER.getContaining(this.getWorld(), this.getPos());
         if (subLevel != null) {
            aabb.transform(subLevel.logicalPose(), aabb);
            subLevel.logicalPose().transformNormal(STORED_TRANSFORMED_THRUST);
         }

         List<Entity> entities = this.getWorld().getEntities(null, aabb.toMojang());
         if (!entities.isEmpty()) {
            for (Entity entity : entities) {
               if (!(entity instanceof AbstractContraptionEntity) && !AirCurrent.isPlayerCreativeFlying(entity) && !DivingBootsItem.isWornBy(entity)) {
                  Vec3 qc = entity.getBoundingBox().getCenter();
                  STORED_MUT_POS.set(qc.x, qc.y, qc.z);
                  Vector3d temp = new Vector3d().set(JOMLConversion.toJOML(this.getPos().getCenter()));
                  if (subLevel != null) {
                     subLevel.logicalPose().transformPosition(temp);
                  }

                  STORED_MUT_POS.sub(temp);
                  double entityDistance = STORED_TRANSFORMED_THRUST.dot(STORED_MUT_POS);
                  STORED_MUT_POS.fma(-entityDistance, STORED_TRANSFORMED_THRUST);
                  double radialDistanceSq = STORED_MUT_POS.lengthSquared();
                  double layerForceScale = 0.0;
                  double minLayerDistance = 100.0;

                  for (PropellerActorBehaviour.PropellerLayer layer : this.propellerLayers) {
                     double layerDistance = entityDistance - layer.offset;
                     layerDistance *= thrustFlowMult;
                     if (layerDistance > 0.0 && radialDistanceSq < layer.outerRadiusSquared()) {
                        double distanceScale = layerDistance * 0.2;
                        double innerRadiusScale = 0.0;
                        if (layer.innerRadius > 0.0 && radialDistanceSq < layer.innerRadiusSquared()) {
                           innerRadiusScale = (layer.innerRadiusSquared() - radialDistanceSq) / (layer.innerRadius * layer.outerRadius);
                           innerRadiusScale *= innerRadiusScale * 12.0;
                        }

                        minLayerDistance = layerDistance;
                        layerForceScale = Math.max(layerForceScale, Math.exp(-distanceScale - innerRadiusScale));
                     }
                  }

                  if (layerForceScale > 0.0) {
                     TEMP_CLIP_START.set(qc.x, qc.y, qc.z).fma(thrustFlowMult * -minLayerDistance, STORED_TRANSFORMED_THRUST);
                     TEMP_CLIP_END.set(qc.x, qc.y, qc.z);
                     Vec3 mojStart = JOMLConversion.toMojang(TEMP_CLIP_START);
                     Vec3 mojEnd = JOMLConversion.toMojang(TEMP_CLIP_END);
                     ClipContext ctx = new ClipContext(mojStart, mojEnd, Block.COLLIDER, Fluid.ANY, CollisionContext.empty());
                     if (this.getWorld().clip(ctx).getType() == Type.MISS) {
                        float modifier = entity.isShiftKeyDown() ? 0.125F : 1.0F;
                        double forceScale = 2.2;
                        double acceleration = 2.2
                           * (double)this.getAirflowTickSpeed()
                           * (double)modifier
                           * layerForceScale
                           * Math.min(this.getAirPressure(), 1.0);
                        Vec3 previousMotion = entity.getDeltaMovement();
                        entity.setDeltaMovement(
                           previousMotion.add(
                              Math.min(Math.max(STORED_TRANSFORMED_THRUST.x() * acceleration - previousMotion.x, -5.0), 5.0) * 0.125,
                              Math.min(Math.max(STORED_TRANSFORMED_THRUST.y() * acceleration - previousMotion.y, -5.0), 5.0) * 0.125,
                              Math.min(Math.max(STORED_TRANSFORMED_THRUST.z() * acceleration - previousMotion.z, -5.0), 5.0) * 0.125
                           )
                        );
                        entity.fallDistance = 0.0F;
                     }
                  }
               }
            }
         }
      }
   }

   public double getParticleRange() {
      return (double)Math.signum(this.getAirflowTickSpeed()) * Math.log((double)Math.abs(this.getAirflowTickSpeed()) * 0.2 * 20.0 + 1.0) / 0.2;
   }

   public float getAirflowTickSpeed() {
      double airflow = this.propeller.getAirflow();
      return (float)(airflow / 20.0);
   }

   public float getParticleSpeed() {
      float speed = this.getAirflowTickSpeed();
      return Math.clamp(speed, -5.0F, 5.0F);
   }

   public void spawnParticles() {
      if (this.getWorld().isClientSide) {
         if (!this.propellerLayers.isEmpty()) {
            double speed = (double)this.getParticleSpeed();
            Vector3d mutSpeed = new Vector3d();
            RandomSource random = this.getWorld().getRandom();
            int particleCount = this.getParticleCount();
            SubLevel subLevel = Sable.HELPER.getContaining(this.getWorld(), this.getPos());
            Vector3d origin = new Vector3d((double)this.getPos().getX() + 0.5, (double)this.getPos().getY() + 0.5, (double)this.getPos().getZ() + 0.5);

            for (int i = 0; i < particleCount; i++) {
               this.particlePositionUpdater.accept(STORED_MUT_POS, random);
               STORED_MUT_POS.add(origin);
               double positionNudge = speed * (double)random.nextFloat();
               STORED_MUT_POS.fma(positionNudge, this.thrustDirection);
               this.thrustDirection.mul(speed * Math.exp(-0.2 * positionNudge), mutSpeed);
               this.getWorld()
                  .addParticle(
                     new PropellerAirParticleData(true, false), STORED_MUT_POS.x, STORED_MUT_POS.y, STORED_MUT_POS.z, mutSpeed.x, mutSpeed.y, mutSpeed.z
                  );
            }

            particleCount = particleCount / 4 + 1;
            Vector3d endVector = new Vector3d();

            for (int i = 0; i < particleCount; i++) {
               this.particlePositionUpdater.accept(STORED_MUT_POS, random);
               STORED_MUT_POS.add(origin);
               STORED_MUT_POS.fma(1.3 * this.getParticleRange() * Math.sqrt((double)random.nextFloat()), this.thrustDirection, endVector);
               if (subLevel != null) {
                  subLevel.logicalPose().transformPosition(STORED_MUT_POS);
                  subLevel.logicalPose().transformPosition(endVector);
               }

               this.createHitParticle(subLevel, origin, STORED_MUT_POS, endVector);
            }
         }
      }
   }

   private void createHitParticle(SubLevel subLevel, Vector3d origin, Vector3d start, Vector3d end) {
      BlockHitResult clip = this.getWorld()
         .clip(new ClipContext(JOMLConversion.toMojang(start), JOMLConversion.toMojang(end), Block.COLLIDER, Fluid.ANY, (Entity)null));
      Vec3 hitPos = clip.getLocation();
      if (clip.getType() != Type.MISS && start.distanceSquared(hitPos.x, hitPos.y, hitPos.z) > 1.0) {
         BlockState hitState = this.getWorld().getBlockState(clip.getBlockPos());
         net.minecraft.world.level.material.Fluid fluid = this.getWorld().getFluidState(clip.getBlockPos()).getType();
         globalThrust.set(this.thrustDirection);
         relativeDiff.set(origin);
         if (subLevel != null) {
            subLevel.logicalPose().orientation().transform(globalThrust);
            subLevel.logicalPose().transformPosition(relativeDiff);
         }

         normal.set((double)clip.getDirection().getStepX(), (double)clip.getDirection().getStepY(), (double)clip.getDirection().getStepZ());
         SubLevel other = Sable.HELPER.getContaining(this.getWorld(), clip.getBlockPos());
         if (other != null) {
            other.logicalPose().orientation().transform(normal);
         }

         start.sub(relativeDiff, relativeDiff).div(this.radius);
         this.projectVector(relativeDiff, globalThrust, 1.0);
         this.projectVector(globalThrust, normal, 1.2);
         this.projectVector(relativeDiff, normal, 1.0);
         relativeDiff.mul((double)Math.signum(-this.getAirflowTickSpeed()));
         globalThrust.sub(relativeDiff, particleVelocity).mul((double)this.getAirflowTickSpeed() * 0.8);
         if (other != null) {
            other.logicalPose().orientation().transformInverse(particleVelocity);
         }

         this.getWorld().addParticle(ParticleTypes.DUST_PLUME, hitPos.x, hitPos.y, hitPos.z, particleVelocity.x, particleVelocity.y, particleVelocity.z);
         if (hitState.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            this.getWorld()
               .addParticle(
                  new BlockParticleOption(ParticleTypes.BLOCK, hitState),
                  hitPos.x,
                  hitPos.y,
                  hitPos.z,
                  particleVelocity.x,
                  particleVelocity.y,
                  particleVelocity.z
               );
         } else if (fluid.isSame(Fluids.WATER)) {
            this.getWorld().addParticle(ParticleTypes.SPLASH, hitPos.x, hitPos.y, hitPos.z, 0.0, 0.0, 0.0);
            if (this.getWorld().getRandom().nextDouble() < 0.2) {
               this.getWorld().addParticle(ParticleTypes.BUBBLE, hitPos.x, hitPos.y, hitPos.z, 0.0, 0.0, 0.0);
            }
         } else if (fluid.isSame(Fluids.LAVA)) {
            this.getWorld().addParticle(ParticleTypes.SMOKE, hitPos.x, hitPos.y, hitPos.z, 0.0, 0.0, 0.0);
            if (this.getWorld().getRandom().nextDouble() < 0.2) {
               this.getWorld().addParticle(ParticleTypes.LAVA, hitPos.x, hitPos.y, hitPos.z, 0.0, 0.0, 0.0);
            }
         }
      }
   }

   private Vector3d projectVector(Vector3d x, Vector3d axis, double scale) {
      return x.fma(-scale * x.dot(axis), axis);
   }

   public int getParticleCount() {
      double count = this.updatedParticleAmount * this.getAirPressure();
      if (this.particleSmoothing > 0.0) {
         count = Math.log(count / this.particleSmoothing + 1.0) * this.particleSmoothing;
      }

      return Math.min((int)(count + (double)this.getWorld().random.nextFloat()), this.maxParticleAmount);
   }

   public void setThrustDirection(Vector3dc thrustDirection) {
      this.thrustDirection = thrustDirection;
   }

   private double getAirPressure() {
      return DimensionPhysicsData.getAirPressure(this.getWorld(), Sable.HELPER.projectOutOfSubLevel(this.getWorld(), JOMLConversion.atCenterOf(this.getPos())));
   }

   public void setParticleAmountUpdater(Supplier<Double> supp) {
      this.particleAmountUpdater = supp;
   }

   public void setParticlePositionUpdater(BiConsumer<Vector3d, RandomSource> cons) {
      this.particlePositionUpdater = cons;
   }

   public void setParticleCountProperties(int maxParticleAmount, double particleSmoothing) {
      this.maxParticleAmount = maxParticleAmount;
      this.particleSmoothing = particleSmoothing;
   }

   public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      if (this.propeller.isActive()) {
         AeroLang.emptyLine(tooltip);
         AeroLang.blockName(this.blockEntity.getBlockState()).text(":").forGoggles(tooltip);
         MutableComponent thrustComponent = AeroLang.pixelNewton(Math.abs(this.propeller.getScaledThrust())).style(ChatFormatting.AQUA).component();
         AeroLang.translate("propeller.thrust", thrustComponent).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
         MutableComponent airflowComponent = AeroLang.translate("unit.meters_per_second", String.format("%.2f", Math.abs(this.propeller.getAirflow())))
            .style(ChatFormatting.AQUA)
            .component();
         AeroLang.translate("propeller.airflow", airflowComponent).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
         this.additionalTooltipInfo(tooltip, isPlayerSneaking);
         return true;
      } else {
         return false;
      }
   }

   public void additionalTooltipInfo(List<Component> tooltip, boolean isPlayerSneaking) {
   }

   public BehaviourType<?> getType() {
      return TYPE;
   }

   public static record PropellerLayer(double offset, double innerRadius, double outerRadius) {
      public double innerRadiusSquared() {
         return this.innerRadius * this.innerRadius;
      }

      public double outerRadiusSquared() {
         return this.outerRadius * this.outerRadius;
      }
   }
}

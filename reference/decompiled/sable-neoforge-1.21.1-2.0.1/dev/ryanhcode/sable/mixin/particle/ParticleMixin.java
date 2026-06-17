package dev.ryanhcode.sable.mixin.particle;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.api.particle.ParticleSubLevelKickable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.mixinterface.particle.ParticleExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Particle.class})
public abstract class ParticleMixin implements ParticleExtension {
   @Unique
   private static final double LIGHT_QUERY_AREA = 8.0;
   @Unique
   private static final BoundingBox3d TEMP_BOX = new BoundingBox3d();
   @Unique
   private final Vector3d sable$inheritedVelocity = new Vector3d();
   @Shadow
   public double x;
   @Shadow
   public double y;
   @Shadow
   public double z;
   @Shadow
   protected double xd;
   @Shadow
   protected double zd;
   @Shadow
   protected double yd;
   @Shadow
   protected double xo;
   @Shadow
   protected double zo;
   @Shadow
   protected double yo;
   @Shadow
   @Final
   protected ClientLevel level;
   @Shadow
   protected boolean onGround;
   @Unique
   private boolean sable$checkedInitialKick = false;
   @Unique
   @Nullable
   private ClientSubLevel sable$trackingSubLevel = null;
   @Unique
   @Nullable
   private Vector3d sable$localTrackingAnchor = null;
   @Unique
   private List<ClientSubLevel> sable$nearbySubLevels;
   @Shadow
   private boolean stoppedByCollision;

   @Shadow
   public abstract void setPos(double var1, double var3, double var5);

   @Shadow
   public abstract void move(double var1, double var3, double var5);

   @Shadow
   protected abstract void setLocationFromBoundingbox();

   @Shadow
   public abstract AABB getBoundingBox();

   @Shadow
   public abstract void setBoundingBox(AABB var1);

   @Shadow
   public abstract void tick();

   @ModifyConstant(
      method = {"Lnet/minecraft/client/particle/Particle;<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDD)V"},
      constant = {@Constant(
         ordinal = 13
      )}
   )
   private double sable$removeUpwardsVelocity(double originalBlockDamageDistanceConstant) {
      return 0.0;
   }

   @Inject(
      method = {"Lnet/minecraft/client/particle/Particle;<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDD)V"},
      at = {@At("TAIL")}
   )
   private void sable$addUpwardsVelocity(ClientLevel clientLevel, double d, double e, double f, double g, double h, double i, CallbackInfo ci) {
      Vec3 particlePosition = new Vec3(this.x, this.y, this.z);
      ClientSubLevel subLevel = Sable.HELPER.getContainingClient(particlePosition);
      if (subLevel != null) {
         Vec3 stupidVanillaVelocity = subLevel.logicalPose().transformNormalInverse(new Vec3(0.0, 0.1, 0.0));
         this.xd = this.xd + stupidVanillaVelocity.x;
         this.yd = this.yd + stupidVanillaVelocity.y;
         this.zd = this.zd + stupidVanillaVelocity.z;
         this.sable$setTrackingSubLevel(subLevel, particlePosition);
      }
   }

   @Override
   public void sable$initialKickOut() {
      Vec3 particlePosition = new Vec3(this.x, this.y, this.z);
      if (!this.sable$checkedInitialKick) {
         ClientSubLevel subLevel = Sable.HELPER.getContainingClient(particlePosition);
         if (subLevel != null) {
            Pose3d pose = subLevel.logicalPose();
            Vec3 particlePositionOld = new Vec3(this.xo, this.yo, this.zo);
            Vec3 globalPosition = pose.transformPosition(particlePosition);
            Vec3 globalPositionOld = pose.transformPosition(particlePositionOld);
            Vec3 globalVelocity = pose.transformNormal(new Vec3(this.xd, this.yd, this.zd));
            this.x = globalPosition.x;
            this.y = globalPosition.y;
            this.z = globalPosition.z;
            this.xo = globalPositionOld.x;
            this.yo = globalPositionOld.y;
            this.zo = globalPositionOld.z;
            this.xd = globalVelocity.x;
            this.yd = globalVelocity.y;
            this.zd = globalVelocity.z;
            this.setPos(this.x, this.y, this.z);
            this.sable$setTrackingSubLevel(subLevel, particlePosition);
         }

         this.sable$checkedInitialKick = true;
      }
   }

   private void sable$kickFromTracking() {
      Vector3d currentLocalPos = this.sable$trackingSubLevel.logicalPose().transformPositionInverse(new Vector3d(this.x, this.y, this.z));
      Sable.HELPER.getVelocity(this.level, currentLocalPos, this.sable$inheritedVelocity);
      this.sable$inheritedVelocity.mul(0.05);
      this.sable$localTrackingAnchor = null;
      this.sable$trackingSubLevel = null;
   }

   @Override
   public void sable$moveWithInheritedVelocity() {
   }

   @Override
   public void sable$setTrackingSubLevel(ClientSubLevel subLevel, Vec3 particlePosition) {
      this.sable$trackingSubLevel = subLevel;
      this.sable$localTrackingAnchor = new Vector3d();
      this.sable$localTrackingAnchor.set(particlePosition.x, particlePosition.y, particlePosition.z);
      this.sable$inheritedVelocity.zero();
   }

   @Override
   public SubLevel sable$getTrackingSubLevel() {
      return this.sable$trackingSubLevel;
   }

   @WrapMethod(
      method = {"move"}
   )
   private void sable$moveWithSubLevels(double motionX, double motionY, double motionZ, Operation<Void> original) {
      AABB bounds;
      BoundingBox3d globalBounds;
      ObjectSet<SubLevel> intersecting;
      boolean var10000;
      label112: {
         bounds = this.getBoundingBox();
         globalBounds = new BoundingBox3d(bounds).expand(0.5);
         intersecting = new ObjectOpenHashSet();
         if (this instanceof ParticleSubLevelKickable kickable && !kickable.sable$shouldCareAboutIntersectingSubLevels()) {
            var10000 = true;
            break label112;
         }

         var10000 = false;
      }

      boolean ignoreIntersecting = var10000;
      if (!ignoreIntersecting) {
         for (SubLevel subLevel : Sable.HELPER.getAllIntersecting(this.level, globalBounds)) {
            intersecting.add(subLevel);
         }
      }

      if (this.sable$trackingSubLevel != null) {
         intersecting.add(this.sable$trackingSubLevel);
      }

      if (this.sable$trackingSubLevel != null && this.sable$trackingSubLevel.isRemoved()) {
         this.sable$trackingSubLevel = null;
         this.sable$localTrackingAnchor = null;
      }

      Vector3d movementFromPushing = new Vector3d();
      Vector3d localPosition = new Vector3d();
      Vector3d globalBoundsCenter = new Vector3d();
      Vector3d localRayStart = new Vector3d();
      Vector3d localRayEnd = new Vector3d();
      Vector3d movement = new Vector3d(motionX, motionY, motionZ);
      movement.add(this.sable$inheritedVelocity);
      boolean isGrounded = false;
      ObjectIterator kickable = intersecting.iterator();

      while (kickable.hasNext()) {
         SubLevel subLevel = (SubLevel)kickable.next();
         Pose3dc pose = subLevel.logicalPose();
         Pose3dc last = subLevel.lastPose();
         movementFromPushing.zero();
         if (this.sable$trackingSubLevel == subLevel) {
            JOMLConversion.getAABBCenter(bounds, globalBoundsCenter);
            last.transformPositionInverse(globalBoundsCenter, localPosition);
            Vector3d newGlobalPosition = pose.transformPosition(localPosition);
            movementFromPushing.add(newGlobalPosition).sub(globalBoundsCenter);
         } else {
            JOMLConversion.getAABBCenter(bounds, globalBoundsCenter);
            last.transformPositionInverse(globalBoundsCenter, localRayStart);
            pose.transformPositionInverse(globalBoundsCenter, localRayEnd);
            ClipContext clipContext = new ClipContext(
               JOMLConversion.toMojang(localRayStart), JOMLConversion.toMojang(localRayEnd), Block.COLLIDER, Fluid.NONE, CollisionContext.empty()
            );
            ((ClipContextExtension)clipContext).sable$setDoNotProject(true);
            BlockHitResult result = this.level.clip(clipContext);
            if (result.getType() == Type.BLOCK) {
               pose.transformPosition(JOMLConversion.toJOML(result.getLocation(), movementFromPushing)).sub(globalBoundsCenter);
               if (this.sable$trackingSubLevel == null) {
                  this.sable$setTrackingSubLevel((ClientSubLevel)subLevel, result.getLocation());
               }
            }
         }

         label96: {
            if (this.sable$trackingSubLevel == subLevel
               && this instanceof ParticleSubLevelKickable kickablex
               && !kickablex.sable$shouldCollideWithTrackingSubLevel()) {
               var10000 = true;
               break label96;
            }

            var10000 = false;
         }

         boolean shouldCollide = !var10000;
         if (shouldCollide) {
            Vector3dc pushedPosition = JOMLConversion.getAABBCenter(bounds, globalBoundsCenter).add(movementFromPushing);
            pose.transformPositionInverse(pushedPosition, localRayStart);
            pose.transformPositionInverse(pushedPosition.add(movement, localRayEnd));
            ClipContext clipContextx = new ClipContext(
               JOMLConversion.toMojang(localRayStart), JOMLConversion.toMojang(localRayEnd), Block.COLLIDER, Fluid.NONE, CollisionContext.empty()
            );
            ((ClipContextExtension)clipContextx).sable$setDoNotProject(true);
            BlockHitResult resultx = this.level.clip(clipContextx);
            if (resultx != null && resultx.getType() == Type.BLOCK) {
               Vec3 diff = pose.transformPosition(resultx.getLocation()).subtract(pushedPosition.x(), pushedPosition.y(), pushedPosition.z());
               movement.set(diff.x, diff.y, diff.z);
            }
         }

         movement.add(movementFromPushing);
         if (shouldCollide) {
            Vec3 collisionBoxCenter = pose.transformPositionInverse(bounds.getCenter().add(movement.x, movement.y, movement.z));
            double radius = Math.max(bounds.getXsize(), Math.max(bounds.getYsize(), bounds.getZsize())) / 2.0;
            BoundingBox3d collisionBounds = new BoundingBox3d();
            collisionBounds.set(
               collisionBoxCenter.x - radius,
               collisionBoxCenter.y - radius,
               collisionBoxCenter.z - radius,
               collisionBoxCenter.x + radius,
               collisionBoxCenter.y + radius,
               collisionBoxCenter.z + radius
            );
            Vector3d mtv = this.resolveAABBCollision(collisionBounds);
            if (mtv.lengthSquared() > 0.0) {
               subLevel.logicalPose().transformNormal(mtv);
               Vector3d nmtv = mtv.normalize(new Vector3d());
               Vector3dc upDirection = OrientedBoundingBox3d.UP;
               if (this instanceof ParticleSubLevelKickable kickablex) {
                  upDirection = kickablex.sable$getUpDirection();
               }

               double verticalDot = nmtv.dot(upDirection);
               if (verticalDot > 0.6) {
                  isGrounded = true;
               }

               double dot = nmtv.dot(this.xd, this.yd, this.zd);
               this.xd = this.xd - dot * nmtv.x;
               this.yd = this.yd - dot * nmtv.y;
               this.zd = this.zd - dot * nmtv.z;
               if (verticalDot > 0.6 || verticalDot < 0.6) {
                  this.xd = upDirection.x() * this.xd;
                  this.yd = upDirection.y() * this.yd;
                  this.zd = upDirection.z() * this.zd;
               }

               movement.add(mtv);
               if (this.sable$trackingSubLevel == null) {
                  this.sable$setTrackingSubLevel((ClientSubLevel)subLevel, collisionBoxCenter);
               }
            }
         }
      }

      original.call(new Object[]{movement.x, movement.y, movement.z});
      this.onGround |= isGrounded;
      if (this.sable$trackingSubLevel != null
         && (!(this instanceof ParticleSubLevelKickable kickablex) || kickablex.sable$shouldKickFromTracking())
         && this.sable$trackingSubLevel.logicalPose().transformPosition(this.sable$localTrackingAnchor, new Vector3d()).distanceSquared(this.x, this.y, this.z)
            > 0.25) {
         this.sable$kickFromTracking();
      }
   }

   private Vector3d resolveAABBCollision(BoundingBox3d box) {
      Vector3d totalMTV = new Vector3d();
      Vector3d mtv = new Vector3d();
      double[] maxMTVLengthSq = new double[]{0.0};
      int minX = (int)Math.floor(box.minX());
      int minY = (int)Math.floor(box.minY());
      int minZ = (int)Math.floor(box.minZ());
      int maxX = (int)Math.floor(box.maxX());
      int maxY = (int)Math.floor(box.maxY());
      int maxZ = (int)Math.floor(box.maxZ());
      MutableBlockPos mpos = new MutableBlockPos();

      for (int x = minX; x <= maxX; x++) {
         for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
               BlockPos blockPos = mpos.set(x, y, z);
               BlockState state = this.level.getBlockState(blockPos);
               if (!state.isAir()) {
                  VoxelShape shape = state.getCollisionShape(this.level, blockPos);
                  if (!shape.isEmpty()) {
                     int finalX = x;
                     int finalY = y;
                     int finalZ = z;
                     if (state.isCollisionShapeFullBlock(this.level, blockPos)) {
                        TEMP_BOX.setUnchecked(
                           0.0 + (double)finalX, 0.0 + (double)finalY, 0.0 + (double)finalZ, 1.0 + (double)finalX, 1.0 + (double)finalY, 1.0 + (double)finalZ
                        );
                        mtv.zero();
                        this.resolveAABBAABBCollision(box, TEMP_BOX, mtv);
                        double lenSq = mtv.lengthSquared();
                        if (lenSq > maxMTVLengthSq[0]) {
                           maxMTVLengthSq[0] = lenSq;
                           totalMTV.set(mtv);
                        }
                     } else {
                        shape.forAllBoxes(
                           (minXb, minYb, minZb, maxXb, maxYb, maxZb) -> {
                              TEMP_BOX.setUnchecked(
                                 minXb + (double)finalX,
                                 minYb + (double)finalY,
                                 minZb + (double)finalZ,
                                 maxXb + (double)finalX,
                                 maxYb + (double)finalY,
                                 maxZb + (double)finalZ
                              );
                              mtv.zero();
                              this.resolveAABBAABBCollision(box, TEMP_BOX, mtv);
                              double lenSqx = mtv.lengthSquared();
                              if (lenSqx > maxMTVLengthSq[0]) {
                                 maxMTVLengthSq[0] = lenSqx;
                                 totalMTV.set(mtv);
                              }
                           }
                        );
                     }
                  }
               }
            }
         }
      }

      return totalMTV;
   }

   private void resolveAABBAABBCollision(BoundingBox3d a, BoundingBox3dc b, Vector3d mtv) {
      double dx1 = b.maxX() - a.minX();
      double dx2 = a.maxX() - b.minX();
      if (!(dx1 <= 0.0) && !(dx2 <= 0.0)) {
         double dy1 = b.maxY() - a.minY();
         double dy2 = a.maxY() - b.minY();
         if (!(dy1 <= 0.0) && !(dy2 <= 0.0)) {
            double dz1 = b.maxZ() - a.minZ();
            double dz2 = a.maxZ() - b.minZ();
            if (!(dz1 <= 0.0) && !(dz2 <= 0.0)) {
               double minOverlap = dx1;
               mtv.set(dx1, 0.0, 0.0);
               if (dx2 < dx1) {
                  minOverlap = dx2;
                  mtv.set(-dx2, 0.0, 0.0);
               }

               if (dy1 < minOverlap) {
                  minOverlap = dy1;
                  mtv.set(0.0, dy1, 0.0);
               }

               if (dy2 < minOverlap) {
                  minOverlap = dy2;
                  mtv.set(0.0, -dy2, 0.0);
               }

               if (dz1 < minOverlap) {
                  minOverlap = dz1;
                  mtv.set(0.0, 0.0, dz1);
               }

               if (dz2 < minOverlap) {
                  mtv.set(0.0, 0.0, -dz2);
               }
            }
         }
      }
   }

   @Inject(
      method = {"getLightColor"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$checkSubLevelLightColor(float f, CallbackInfoReturnable<Integer> cir) {
      BlockPos pos = BlockPos.containing(this.x, this.y, this.z);
      boolean hasChunk = this.level.hasChunkAt(pos);
      if (hasChunk) {
         BlockState state = this.level.getBlockState(pos);
         if (state.emissiveRendering(this.level, pos)) {
            cir.setReturnValue(15728880);
         } else {
            int skyLight;
            int blockLight;
            if (this.sable$trackingSubLevel != null) {
               blockLight = this.level.getBrightness(LightLayer.BLOCK, pos);
               skyLight = this.level.getBrightness(LightLayer.SKY, pos);
               Vector3d particlePos = new Vector3d();
               MutableBlockPos localBlockPos = new MutableBlockPos();
               MutableBlockPos heightmapPos = new MutableBlockPos();
               Pose3d pose = this.sable$trackingSubLevel.logicalPose();
               pose.transformPositionInverse(particlePos.set(this.x, this.y, this.z));
               localBlockPos.set(particlePos.x, particlePos.y, particlePos.z);
               blockLight = Math.max(blockLight, this.sable$trackingSubLevel.getLevel().getBrightness(LightLayer.BLOCK, localBlockPos));
               heightmapPos.setWithOffset(localBlockPos, 0, 1, 0);
               LevelPlot plot = this.sable$trackingSubLevel.getPlot();
               boolean isAboveGround = false;

               while (heightmapPos.getY() >= plot.getBoundingBox().minY()) {
                  if (!this.level.getBlockState(heightmapPos).isAir()) {
                     isAboveGround = true;
                     break;
                  }

                  heightmapPos.move(0, -1, 0);
               }

               if (isAboveGround) {
                  skyLight = Math.min(skyLight, this.sable$trackingSubLevel.scaleSkyLight(this.level.getBrightness(LightLayer.SKY, localBlockPos)));
               }
            } else {
               if (this.sable$nearbySubLevels == null) {
                  this.sable$nearbySubLevels = new ObjectArrayList(6);

                  for (SubLevel subLevel : Sable.HELPER.getAllIntersecting(this.level, new BoundingBox3d(pos).expand(8.0))) {
                     this.sable$nearbySubLevels.add((ClientSubLevel)subLevel);
                  }
               }

               if (this.sable$nearbySubLevels.isEmpty()) {
                  return;
               }

               blockLight = this.level.getBrightness(LightLayer.BLOCK, pos);
               skyLight = this.level.getBrightness(LightLayer.SKY, pos);
               Vector3d particlePos = new Vector3d();
               MutableBlockPos localBlockPos = new MutableBlockPos();
               MutableBlockPos heightmapPos = new MutableBlockPos();
               BoundingBox3d box = new BoundingBox3d(pos).expand(0.5);

               for (ClientSubLevel subLevel : this.sable$nearbySubLevels) {
                  if (subLevel.boundingBox().intersects(box)) {
                     Pose3d pose = subLevel.logicalPose();
                     pose.transformPositionInverse(particlePos.set(this.x, this.y, this.z));
                     localBlockPos.set(particlePos.x, particlePos.y, particlePos.z);
                     blockLight = Math.max(blockLight, subLevel.getLevel().getBrightness(LightLayer.BLOCK, localBlockPos));
                     heightmapPos.setWithOffset(localBlockPos, 0, 1, 0);
                     LevelPlot plot = subLevel.getPlot();
                     boolean isAboveGround = false;

                     while (heightmapPos.getY() >= plot.getBoundingBox().minY()) {
                        if (!this.level.getBlockState(heightmapPos).isAir()) {
                           isAboveGround = true;
                           break;
                        }

                        heightmapPos.move(0, -1, 0);
                     }

                     if (isAboveGround) {
                        skyLight = Math.min(skyLight, subLevel.scaleSkyLight(this.level.getBrightness(LightLayer.SKY, localBlockPos)));
                     }
                  }
               }
            }

            int k = state.getLightEmission();
            if (blockLight < k) {
               blockLight = k;
            }

            cir.setReturnValue(LightTexture.pack(blockLight, skyLight));
         }
      }
   }
}

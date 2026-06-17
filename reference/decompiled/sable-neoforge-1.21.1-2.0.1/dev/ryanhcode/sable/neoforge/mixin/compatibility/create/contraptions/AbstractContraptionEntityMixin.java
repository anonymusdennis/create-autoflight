package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.contraptions;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import dev.ryanhcode.sable.api.physics.mass.MassTracker;
import dev.ryanhcode.sable.api.sublevel.KinematicContraption;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.physics.floating_block.FloatingBlockCluster;
import dev.ryanhcode.sable.physics.floating_block.FloatingClusterContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AbstractContraptionEntity.class})
public abstract class AbstractContraptionEntityMixin extends Entity implements KinematicContraption {
   @Unique
   private final Vector3d sable$cachedGlobalPosition = new Vector3d();
   @Unique
   private final Object2ObjectMap<BlockPos, BlockSubLevelLiftProvider.LiftProviderContext> sable$liftProviderContexts = new Object2ObjectOpenHashMap();
   @Unique
   private final FloatingClusterContainer sable$floatingClusterContainer = new FloatingClusterContainer();
   @Shadow
   protected Contraption contraption;
   @Unique
   private BoundingBox3i sable$localBounds;
   @Unique
   private MassTracker sable$massTracker;
   @Unique
   private boolean sable$initialized = false;

   public AbstractContraptionEntityMixin(EntityType<?> arg, Level arg2) {
      super(arg, arg2);
   }

   @Shadow
   public abstract Vec3 applyRotation(Vec3 var1, float var2);

   @Shadow
   public abstract Vec3 getPrevAnchorVec();

   @Shadow
   public abstract Vec3 getAnchorVec();

   @Redirect(
      method = {"moveCollidedEntitiesOnDisassembly"},
      at = @At(
         value = "INVOKE",
         target = "Lcom/simibubi/create/content/contraptions/AbstractContraptionEntity;toLocalVector(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;"
      )
   )
   private Vec3 sable$applyTransform(AbstractContraptionEntity instance, Vec3 localVec, float partialTicks) {
      SubLevel subLevel = Sable.HELPER.getContaining(instance);
      return instance.toLocalVector(subLevel != null ? subLevel.logicalPose().transformPositionInverse(localVec) : localVec, partialTicks);
   }

   @WrapOperation(
      method = {"moveCollidedEntitiesOnDisassembly"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;setPos(DDD)V"
      ), @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;teleportTo(DDD)V"
      )}
   )
   private void sable$applyTransform(Entity instance, double x, double y, double z, Operation<Void> original) {
      Vector3d pos = Sable.HELPER.projectOutOfSubLevel(instance.level(), new Vector3d(x, y, z));
      original.call(new Object[]{instance, pos.x, pos.y, pos.z});
   }

   @Inject(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/util/Map;entrySet()Ljava/util/Set;"
      )},
      remap = false
   )
   private void sable$contraptionInitialize(CallbackInfo ci) {
      if (!this.sable$initialized && this.level() instanceof ServerLevel serverLevel) {
         this.sable$buildProperties();
         if (this.sable$massTracker.getCenterOfMass() == null) {
            this.sable$initialized = true;
            return;
         }

         this.sable$addToPlot();
         this.sable$addToPipeline(serverLevel);
         this.sable$initialized = true;
      }
   }

   @Override
   public Map<BlockPos, BlockSubLevelLiftProvider.LiftProviderContext> sable$liftProviders() {
      return this.sable$liftProviderContexts;
   }

   @Overwrite
   public CompoundTag saveWithoutId(CompoundTag nbt) {
      Vec3 vec = this.position();

      for (Entity entity : this.getPassengers()) {
         if (!(entity instanceof Player)) {
            entity.removalReason = RemovalReason.UNLOADED_TO_CHUNK;
            Vec3 prevVec = entity.position();
            entity.setPosRaw(vec.x, prevVec.y, vec.z);
            entity.removalReason = null;
         }
      }

      return super.saveWithoutId(nbt);
   }

   @Unique
   private void sable$buildProperties() {
      for (Entry<BlockPos, StructureBlockInfo> entry : this.contraption.getBlocks().entrySet()) {
         BlockPos blockPos = entry.getKey();
         StructureBlockInfo info = entry.getValue();
         BlockState state = info.state();
         if (!state.isAir()) {
            if (this.sable$localBounds == null) {
               this.sable$localBounds = new BoundingBox3i(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockPos.getX(), blockPos.getY(), blockPos.getZ());
            }

            this.sable$localBounds.expandTo(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            if (state.getBlock() instanceof BlockSubLevelLiftProvider prov) {
               BlockSubLevelLiftProvider.LiftProviderContext context = new BlockSubLevelLiftProvider.LiftProviderContext(
                  blockPos, state, Vec3.atLowerCornerOf(prov.sable$getNormal(state).getNormal())
               );
               this.sable$liftProviderContexts.put(blockPos, context);
            }

            if (PhysicsBlockPropertyHelper.getFloatingMaterial(state) != null) {
               this.sable$floatingClusterContainer
                  .addFloatingBlock(state, new Vector3d((double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ()));
            }
         }
      }

      assert this.sable$localBounds != null;

      this.sable$massTracker = MassTracker.build(this.sable$blockGetter(), this.sable$localBounds);
      Vector3d temp = this.sable$massTracker.getCenterOfMass().negate(new Vector3d()).add(0.5, 0.5, 0.5);

      for (FloatingBlockCluster cluster : this.sable$floatingClusterContainer.clusters) {
         cluster.getBlockData().translateOrigin(temp);
      }
   }

   @Unique
   private void sable$addToPlot() {
      SubLevel subLevel = Sable.HELPER.getContaining(this);
      if (subLevel != null) {
         ServerSubLevel serverSubLevel = (ServerSubLevel)subLevel;
         serverSubLevel.getPlot().addContraption(this);
      }
   }

   @Unique
   private void sable$removeFromPlot() {
      SubLevel subLevel = Sable.HELPER.getContaining(this);
      if (subLevel != null) {
         ServerSubLevel serverSubLevel = (ServerSubLevel)subLevel;
         serverSubLevel.getPlot().removeContraption(this);
      }
   }

   public void setRemoved(RemovalReason removalReason) {
      if (this.level() instanceof ServerLevel serverLevel) {
         this.sable$removeFromPlot();
         this.sable$removeFromPipeline(serverLevel);
      }

      super.setRemoved(removalReason);
   }

   @Unique
   private void sable$addToPipeline(ServerLevel serverLevel) {
      SubLevelPhysicsSystem physics = SubLevelPhysicsSystem.require(serverLevel);
      physics.getPipeline().add(this);
   }

   @Unique
   private void sable$removeFromPipeline(ServerLevel serverLevel) {
      SubLevelPhysicsSystem physics = SubLevelPhysicsSystem.require(serverLevel);
      physics.getPipeline().remove(this);
   }

   @Override
   public void sable$getLocalBounds(BoundingBox3i bounds) {
      bounds.set(this.sable$localBounds);
   }

   @Override
   public BlockGetter sable$blockGetter() {
      return this.contraption.getContraptionWorld();
   }

   @Override
   public MassTracker sable$getMassTracker() {
      return this.sable$massTracker;
   }

   @Override
   public Vector3dc sable$getPosition(double partialTick) {
      Vec3 localVec = JOMLConversion.toMojang(this.sable$massTracker.getCenterOfMass());
      Vec3 anchor = this.getPrevAnchorVec().lerp(this.getAnchorVec(), partialTick);
      Vec3 rotationOffset = VecHelper.getCenterOf(BlockPos.ZERO);
      localVec = localVec.subtract(rotationOffset);
      localVec = this.applyRotation(localVec, (float)partialTick);
      localVec = localVec.add(rotationOffset).add(anchor);
      return JOMLConversion.toJOML(localVec, this.sable$cachedGlobalPosition);
   }

   @Override
   public Quaterniond sable$getOrientation(double partialTick) {
      Matrix3d matrix = new Matrix3d();
      Vector3d tempColumn = new Vector3d();

      for (int i = 0; i < 3; i++) {
         matrix.getColumn(i, tempColumn);
         Vec3 transformed = this.applyRotation(JOMLConversion.toMojang(tempColumn), (float)partialTick);
         matrix.setColumn(i, transformed.x, transformed.y, transformed.z);
      }

      return matrix.getNormalizedRotation(new Quaterniond());
   }

   @Override
   public boolean sable$isValid() {
      return !this.isRemoved();
   }

   @Override
   public boolean sable$shouldCollide() {
      return true;
   }

   @Override
   public FloatingClusterContainer sable$getFloatingClusterContainer() {
      return this.sable$floatingClusterContainer;
   }
}

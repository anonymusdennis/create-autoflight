package dev.eriksonn.aeronautics.content.blocks.hot_air.gust;

import dev.eriksonn.aeronautics.content.particle.AirPoofParticleData;
import dev.eriksonn.aeronautics.content.particle.GustParticleData;
import dev.eriksonn.aeronautics.index.AeroEntityTypes;
import dev.eriksonn.aeronautics.index.AeroSoundEvents;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.util.SableBufferUtils;
import dev.ryanhcode.sable.util.SableNBTUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector3d;

public class GustEntity extends Entity implements IEntityWithComplexSpawn {
   private final Quaterniond orientation = new Quaterniond();
   private boolean spawnedInitialBurst = false;

   public static void addGust(Level level, BlockPos pos, Direction direction) {
      Quaterniond orientation = new Quaterniond(direction.getRotation());
      GustEntity gust = new GustEntity((EntityType<?>)AeroEntityTypes.GUST.get(), level, orientation);
      gust.setPos(pos.getCenter());
      level.addFreshEntity(gust);
   }

   public GustEntity(EntityType<?> entityType, Level level) {
      this(entityType, level, JOMLConversion.QUAT_IDENTITY);
   }

   public GustEntity(EntityType<?> entityType, Level level, Quaterniondc orientation) {
      super(entityType, level);
      this.orientation.set(orientation);
   }

   public void tick() {
      super.tick();
      if (this.level().isClientSide) {
         this.spawnClientEffects();
      } else {
         if (Sable.HELPER.getContaining(this) instanceof ServerSubLevel serverSubLevel) {
            Vector3d forceDir = this.orientation.transform(new Vector3d(0.0, 1.0, 0.0)).mul(-3.0);
            RigidBodyHandle.of(serverSubLevel).applyImpulseAtPoint(this.position(), JOMLConversion.toMojang(forceDir));
         }

         if (this.tickCount > 5) {
            this.remove(RemovalReason.DISCARDED);
         }
      }
   }

   private void spawnClientEffects() {
      Level level = this.level();
      if (!this.spawnedInitialBurst) {
         Vec3 soundPos = this.position();
         level.playLocalSound(soundPos.x, soundPos.y, soundPos.z, AeroSoundEvents.GUST.event(), SoundSource.BLOCKS, 0.65F, 0.35F, false);
         int poofParticleCount = 30;

         for (int i = 0; i < 3; i++) {
            if (!((double)level.random.nextFloat() < 0.1)) {
               Quaternionf particleOrientation = new Quaternionf(this.orientation);
               particleOrientation.rotateY((float)((Math.PI * 2.0 / 3.0) * (double)i));
               particleOrientation.rotateZ((float)Math.toRadians(-10.0));
               float randomRot = (float)Math.toRadians(12.0);
               particleOrientation.rotateX(this.random.nextFloat() * randomRot - randomRot / 2.0F);
               particleOrientation.rotateZ(this.random.nextFloat() * randomRot - randomRot / 2.0F);
               particleOrientation.rotateY(this.random.nextFloat() * randomRot - randomRot / 2.0F);
               Vector3d particlePos = JOMLConversion.toJOML(this.position());
               particlePos.add(particleOrientation.transform(new Vector3d(0.5, 0.5, 0.0)));
               level.addParticle(new GustParticleData(particleOrientation), particlePos.x, particlePos.y, particlePos.z, 0.0, 0.0, 0.0);
            }
         }

         for (int ix = 0; ix < 30; ix++) {
            Vector3d outDir = this.orientation.transform(new Vector3d(0.0, 1.0, 0.0));
            float velocity = 0.07F + this.random.nextFloat() * 0.1F;
            double vx = outDir.x * (double)velocity + this.random.nextGaussian() * 0.01;
            double vy = outDir.y * (double)velocity + this.random.nextGaussian() * 0.01;
            double vz = outDir.z * (double)velocity + this.random.nextGaussian() * 0.01;
            float positionalRandomness = 0.7F;
            Vec3 particlePos = this.position()
               .subtract(outDir.x, outDir.y, outDir.z)
               .add(
                  0.7F * ((double)this.random.nextFloat() - 0.5),
                  0.7F * ((double)this.random.nextFloat() - 0.5),
                  0.7F * ((double)this.random.nextFloat() - 0.5)
               );
            level.addParticle(new AirPoofParticleData(), particlePos.x, particlePos.y, particlePos.z, vx, vy, vz);
         }

         this.spawnedInitialBurst = true;
      }
   }

   @NotNull
   public PushReaction getPistonPushReaction() {
      return PushReaction.IGNORE;
   }

   @NotNull
   protected AABB makeBoundingBox() {
      AABB boundingBox = this.getDimensions(this.getPose()).makeBoundingBox(this.position());
      return boundingBox.move(0.0, -boundingBox.getYsize() / 2.0, 0.0);
   }

   protected void defineSynchedData(@NotNull Builder builder) {
   }

   protected void readAdditionalSaveData(CompoundTag compoundTag) {
      compoundTag.put("GustOrientation", SableNBTUtils.writeQuaternion(this.orientation));
   }

   protected void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
      this.orientation.set(SableNBTUtils.readQuaternion(compoundTag));
   }

   public void writeSpawnData(@NotNull RegistryFriendlyByteBuf buffer) {
      SableBufferUtils.write(buffer, this.orientation);
   }

   public void readSpawnData(@NotNull RegistryFriendlyByteBuf buffer) {
      SableBufferUtils.read(buffer, this.orientation);
   }
}

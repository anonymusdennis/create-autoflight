package com.simibubi.create.content.contraptions.actors.seat;

import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.content.logistics.box.PackageEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Entity.MoveFunction;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

public class SeatEntity extends Entity implements IEntityWithComplexSpawn {
   public SeatEntity(EntityType<?> entityType, Level level) {
      super(entityType, level);
   }

   public SeatEntity(Level level) {
      this((EntityType<?>)AllEntityTypes.SEAT.get(), level);
      this.noPhysics = true;
   }

   public static Builder<?> build(Builder<?> builder) {
      return builder.sized(0.25F, 0.35F);
   }

   public void setPos(double x, double y, double z) {
      super.setPos(x, y, z);
      AABB bb = this.getBoundingBox();
      Vec3 diff = new Vec3(x, y, z).subtract(bb.getCenter());
      this.setBoundingBox(bb.move(diff));
   }

   protected void positionRider(Entity pEntity, MoveFunction pCallback) {
      if (this.hasPassenger(pEntity)) {
         double heightOffset = this.getPassengerRidingPosition(pEntity).y - pEntity.getVehicleAttachmentPoint(this).y;
         pCallback.accept(pEntity, this.getX(), 0.0625 + heightOffset + getCustomEntitySeatOffset(pEntity), this.getZ());
      }
   }

   public void onPassengerTurned(Entity entity) {
      entity.setYHeadRot(entity.getYRot());
   }

   public static double getCustomEntitySeatOffset(Entity entity) {
      if (entity instanceof Slime) {
         return 0.0;
      } else if (entity instanceof Parrot) {
         return 0.083333336F;
      } else if (entity instanceof Skeleton) {
         return 0.125;
      } else if (entity instanceof Cat) {
         return 0.083333336F;
      } else if (entity instanceof Wolf) {
         return 0.0625;
      } else if (entity instanceof Frog) {
         return 0.09375;
      } else if (entity instanceof Spider) {
         return 0.125;
      } else {
         return entity instanceof PackageEntity ? 0.09375 : 0.0;
      }
   }

   public void setDeltaMovement(Vec3 vec) {
   }

   public void tick() {
      if (!this.level().isClientSide) {
         boolean blockPresent = this.level().getBlockState(this.blockPosition()).getBlock() instanceof SeatBlock;
         if (!this.isVehicle() || !blockPresent) {
            this.discard();
         }
      }
   }

   protected boolean canRide(Entity entity) {
      return !(entity instanceof FakePlayer);
   }

   protected void removePassenger(Entity entity) {
      super.removePassenger(entity);
      if (entity instanceof TamableAnimal ta) {
         ta.setInSittingPose(false);
      }
   }

   public Vec3 getDismountLocationForPassenger(LivingEntity pLivingEntity) {
      return super.getDismountLocationForPassenger(pLivingEntity).add(0.0, 0.5, 0.0);
   }

   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
   }

   public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
   }

   public void readSpawnData(RegistryFriendlyByteBuf additionalData) {
   }

   public static class Render extends EntityRenderer<SeatEntity> {
      public Render(Context context) {
         super(context);
      }

      public boolean shouldRender(SeatEntity seatEntity, Frustum frustum, double p_225626_3_, double p_225626_5_, double p_225626_7_) {
         return false;
      }

      public ResourceLocation getTextureLocation(SeatEntity seatEntity) {
         return null;
      }
   }
}

package dev.eriksonn.aeronautics.content.blocks.mounted_potato_cannon;

import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.equipment.potatoCannon.PotatoCannonProjectileType;
import com.simibubi.create.content.equipment.potatoCannon.PotatoProjectileEntity;
import com.simibubi.create.content.equipment.potatoCannon.PotatoCannonItem.Ammo;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.particle.AirParticleData;
import dev.eriksonn.aeronautics.config.AeroConfig;
import dev.eriksonn.aeronautics.data.AeroLang;
import dev.eriksonn.aeronautics.index.AeroAdvancements;
import dev.eriksonn.aeronautics.mixinterface.PotatoProjectileEntityExtension;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.List;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class MountedPotatoCannonBlockEntity extends KineticBlockEntity implements BlockEntitySubLevelActor, IHaveGoggleInformation, Clearable {
   private static final Vector3d RECOIL_DIR = new Vector3d();
   private static final Vector3d RECOIL_CENTER = new Vector3d();
   private MountedPotatoCannonBlockEntity.State currentState = MountedPotatoCannonBlockEntity.State.CHARGING;
   private final MountedPotatoCannonInventory inventory;
   private float chargeTimer;
   private int initialAmmoReloadTicks = -1;
   private float recoilMagnitude;
   boolean needsClientUpdate;
   private boolean blocked;
   private double blockedLength;
   private int itemRotationId;
   private int barrelTimer;
   private int itemTimer;
   private float animationSpeed;
   private float angle;
   private float previousAngle;

   public MountedPotatoCannonBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
      super(typeIn, pos, state);
      this.chargeTimer = 0.0F;
      this.recoilMagnitude = 0.0F;
      this.inventory = new MountedPotatoCannonInventory(this);
      this.barrelTimer = 100;
      this.needsClientUpdate = false;
      this.itemTimer = 20;
   }

   public void initialize() {
      super.initialize();
      this.inventory.updateCachedType(this.level.registryAccess(), this.inventory.slot.getStack());
      this.resetAndUpdate();
   }

   public void tick() {
      super.tick();
      if (this.recoilMagnitude > 0.0F) {
         this.recoilMagnitude *= 0.5F;
      }

      if (this.level.isClientSide()) {
         this.previousAngle = this.angle;
         float targetSpeed = Math.abs(this.speed);
         float maxTarget = 32.0F;
         targetSpeed = (float)(1.0 - Math.exp((double)(-targetSpeed / maxTarget))) * maxTarget;
         targetSpeed *= 0.3F;
         this.animationSpeed = this.animationSpeed + (targetSpeed - this.animationSpeed) * 0.3F;
         this.angle = this.angle + this.animationSpeed;
         if (this.angle > 360.0F) {
            this.angle -= 360.0F;
            this.previousAngle -= 360.0F;
         }
      }

      if (this.itemTimer < 20) {
         this.itemTimer++;
      }

      if (this.barrelTimer < 100) {
         this.barrelTimer++;
      }

      this.updateBlockedState();
      switch (this.currentState) {
         case CHARGED:
            boolean internalBlocked = this.blocked;
            BlockState state = this.level.getBlockState(this.getBlockPos().relative((Direction)this.getBlockState().getValue(DirectionalKineticBlock.FACING)));
            if (Blocks.MANGROVE_TRAPDOOR == state.getBlock()) {
               internalBlocked = false;
            }

            if (!internalBlocked && (Boolean)this.getBlockState().getValue(MountedPotatoCannonBlock.POWERED)) {
               this.currentState = MountedPotatoCannonBlockEntity.State.FIRING;
               this.barrelTimer = 0;
            }
            break;
         case FIRING:
            this.animationSpeed = (float)(this.barrelTimer * 75);
            if (this.barrelTimer > 2) {
               if (this.level.isClientSide) {
                  this.speed = 0.2F;
               }

               Ammo ammo = this.getInventory().getAmmo();
               if (ammo != null) {
                  this.getInventory().extractSlot(0, 1, false);
                  Vec3 barrelPos = this.getBarrelPos();
                  BlockState statex = this.level
                     .getBlockState(this.getBlockPos().relative((Direction)this.getBlockState().getValue(DirectionalKineticBlock.FACING)));
                  if (this.blocked && Blocks.MANGROVE_TRAPDOOR == statex.getBlock()) {
                     barrelPos = new Vec3(
                           (double)this.getBlockPos().getX() + 0.5, (double)this.getBlockPos().getY() + 0.5, (double)this.getBlockPos().getZ() + 0.5
                        )
                        .add(this.getAimingVector().scale(this.blockedLength / 1.725F));
                  }

                  if (this.level.isClientSide) {
                     for (int i = 0; i < 8; i++) {
                        Vec3 vel = this.getAimingVector();
                        RandomSource rnd = this.level.getRandom();
                        vel = vel.add(new Vec3(rnd.nextDouble() - 0.5, rnd.nextDouble() - 0.5, rnd.nextDouble() - 0.5).scale(1.0));
                        vel = vel.scale(1.5);
                        this.level.addParticle(new AirParticleData(0.5F, 0.1F), barrelPos.x, barrelPos.y, barrelPos.z, vel.x, vel.y, vel.z);
                     }
                  } else {
                     PotatoCannonProjectileType type = ammo.type();
                     Vec3 motion = this.getAimingVector().scale((double)type.velocityMultiplier() * 2.0);
                     Vec3 sprayBase = VecHelper.rotate(new Vec3(0.0, 0.1, 0.0), (double)(360.0F * this.level.random.nextFloat()), Axis.Z);
                     float sprayChange = 360.0F / (float)type.split();

                     for (int i = 0; i < type.split(); i++) {
                        PotatoProjectileEntity shootyBoomBoom = (PotatoProjectileEntity)AllEntityTypes.POTATO_PROJECTILE.create(this.getLevel());
                        if (shootyBoomBoom != null) {
                           shootyBoomBoom.setItem(ammo.stack());
                           ((PotatoProjectileEntityExtension)shootyBoomBoom).aeronautics$setDamageMultiplier(2.0F);
                           ((PotatoProjectileEntityExtension)shootyBoomBoom).aeronautics$setIsFromMountedPotatoCannon(true);
                           Vec3 splitMotion = motion;
                           if (type.split() > 1) {
                              float imperfection = 40.0F * (this.level.random.nextFloat() - 0.5F);
                              Vec3 sprayOffset = VecHelper.rotate(sprayBase, (double)((float)i * sprayChange + imperfection), Axis.Z);
                              splitMotion = motion.add(VecHelper.lookAt(sprayOffset, motion));
                           }

                           shootyBoomBoom.setPos(barrelPos.x, barrelPos.y, barrelPos.z);
                           shootyBoomBoom.setDeltaMovement(splitMotion);
                           this.level.addFreshEntity(shootyBoomBoom);
                        }
                     }

                     this.recoilMagnitude = (float)type.split() / 2.0F * AeroConfig.server().physics.mountedPotatoCannonMagnitude.getF();
                     AllSoundEvents.FWOOMP.playOnServer(this.level, this.worldPosition, 1.0F, ammo.type().soundPitch() + this.level.random.nextFloat() * 0.2F);
                     AeroAdvancements.HEAVIER_ARTILLERY.awardToNearby(this.getBlockPos(), this.level);
                  }
               }

               this.resetToCharging();
            }
            break;
         case CHARGING:
            if (this.initialAmmoReloadTicks != -1) {
               if (this.chargeTimer < 1.0F) {
                  this.chargeTimer = this.chargeTimer + this.getChargeUpSpeed();
               }

               if (this.chargeTimer >= 1.0F) {
                  this.currentState = MountedPotatoCannonBlockEntity.State.CHARGED;
               }
            }
      }
   }

   private void updateBlockedState() {
      Vec3 pos = new Vec3((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 0.5, (double)this.worldPosition.getZ() + 0.5);
      Vec3 beginning = pos.add(this.getAimingVector().scale(0.65F));
      Vec3 end = pos.add(this.getAimingVector().scale(1.15F));
      BlockHitResult ray = this.level.clip(new ClipContext(beginning, end, Block.COLLIDER, Fluid.NONE, CollisionContext.empty()));
      Vector3dc projected = Sable.HELPER.projectOutOfSubLevel(this.getLevel(), JOMLConversion.toJOML(ray.getLocation()));
      this.blocked = ray.getType() != Type.MISS;
      this.blockedLength = 1.0 - (projected.length() - beginning.length()) / (end.length() - beginning.length()) + 0.1875;
      if (this.blocked != (Boolean)this.getBlockState().getValue(MountedPotatoCannonBlock.BLOCKED)) {
         this.level.setBlockAndUpdate(this.getBlockPos(), (BlockState)this.getBlockState().setValue(MountedPotatoCannonBlock.BLOCKED, this.blocked));
      }
   }

   public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
      if (this.recoilMagnitude > 0.0F) {
         RECOIL_DIR.set(
            JOMLConversion.toJOML(Vec3.atLowerCornerOf(((Direction)this.getBlockState().getValue(DirectionalKineticBlock.FACING)).getOpposite().getNormal()))
         );
         RECOIL_CENTER.set(JOMLConversion.toJOML(Vec3.atCenterOf(this.getBlockPos())));
         RECOIL_DIR.mul((double)this.recoilMagnitude);
         handle.applyImpulseAtPoint(RECOIL_CENTER, RECOIL_DIR);
      }
   }

   public void resetAndUpdate() {
      this.currentState = MountedPotatoCannonBlockEntity.State.CHARGING;
      this.initialAmmoReloadTicks = -1;
      this.chargeTimer = 0.0F;
      this.itemTimer = 0;
      this.itemRotationId = -1;
      Ammo ammo = this.getInventory().getAmmo();
      if (ammo != null) {
         this.initialAmmoReloadTicks = ammo.type().reloadTicks();
         this.itemRotationId = this.level.getRandom().nextInt(10000);
      }

      if (!this.level.isClientSide()) {
         this.needsClientUpdate = true;
      }
   }

   private void resetToCharging() {
      this.currentState = MountedPotatoCannonBlockEntity.State.CHARGING;
      this.itemTimer = 0;
      this.chargeTimer = 0.0F;
   }

   protected void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.write(compound, registries, clientPacket);
      compound.put("inventory", this.inventory.write(registries));
      compound.putInt("ItemRotationID", this.itemRotationId);
      compound.putInt("ItemTimer", this.itemTimer);
      compound.putFloat("ChargeTimer", this.chargeTimer);
      NBTHelper.writeEnum(compound, "State", this.currentState);
      compound.putInt("BarrelTimer", this.barrelTimer);
      if (clientPacket) {
         compound.putBoolean("NeedsUpdate", this.needsClientUpdate);
         this.needsClientUpdate = false;
      }
   }

   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      this.inventory.read(registries, compound.getCompound("inventory"));
      this.inventory.updateCachedType(registries, this.inventory.slot.getStack());
      if (clientPacket && compound.getBoolean("NeedsUpdate")) {
         this.resetAndUpdate();
      }

      this.chargeTimer = compound.getFloat("ChargeTimer");
      this.barrelTimer = compound.getInt("BarrelTimer");
      this.itemRotationId = compound.getInt("ItemRotationID");
      this.itemTimer = compound.getInt("ItemTimer");
      this.currentState = (MountedPotatoCannonBlockEntity.State)NBTHelper.readEnum(compound, "State", MountedPotatoCannonBlockEntity.State.class);
   }

   private float getChargeUpSpeed() {
      return this.initialAmmoReloadTicks != -1 && this.getSpeed() != 0.0F ? Math.abs(this.getSpeed()) / (float)(64 * this.initialAmmoReloadTicks) : 0.0F;
   }

   public float getBarrelDistance(float partialTick) {
      float normalizedTimer = (float)(this.barrelTimer - 1) + partialTick;
      float x = Math.max(normalizedTimer - 0.5F, 0.0F);
      double recoilMultiplier = 0.75;
      float distance = (float)(Math.E * (double)x * 0.75 * Math.exp((double)(-x)));
      return -0.5F * distance;
   }

   public float getBellowDistance(float partialTick) {
      float normalizedTimer = (float)(this.barrelTimer - 1) + partialTick;
      float distance;
      if (this.currentState == MountedPotatoCannonBlockEntity.State.FIRING) {
         distance = 1.0F - (1.0F - normalizedTimer) * 0.75F;
      } else {
         distance = 1.0F - Math.min(this.chargeTimer + this.getChargeUpSpeed() * partialTick, 1.0F);
      }

      distance = Math.min(distance, 1.0F);
      distance = Math.max(distance, 0.0F);
      return distance * 0.15F;
   }

   public float getCogwheelAngle(float partialTicks) {
      return Mth.lerp(partialTicks, this.previousAngle, this.angle);
   }

   public float getCogwheelSpeed() {
      return -Mth.clamp(this.getSpeed(), -1.0F, 1.0F);
   }

   public float getItemTime(float partialTicks) {
      return (float)this.itemTimer + partialTicks;
   }

   public int getItemRotationId() {
      return this.itemRotationId;
   }

   public Vec3 getBarrelPos() {
      return new Vec3((double)this.getBlockPos().getX() + 0.5, (double)this.getBlockPos().getY() + 0.5, (double)this.getBlockPos().getZ() + 0.5)
         .add(this.getAimingVector().scale(1.2));
   }

   public Vec3 getAimingVector() {
      return Vec3.atLowerCornerOf(((Direction)this.getBlockState().getValue(DirectionalKineticBlock.FACING)).getNormal());
   }

   public MountedPotatoCannonInventory getInventory() {
      return this.inventory;
   }

   public boolean isBlocked() {
      return this.blocked;
   }

   public double getBlockedLength() {
      return this.blockedLength;
   }

   public AABB createRenderBoundingBox() {
      return super.createRenderBoundingBox().inflate(1.0);
   }

   public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      super.addToGoggleTooltip(tooltip, isPlayerSneaking);
      AeroLang.emptyLine(tooltip);
      AeroLang.blockName(this.getBlockState()).text(":").forGoggles(tooltip);
      Ammo ammo = this.inventory.getAmmo();
      if (ammo != null) {
         PotatoCannonProjectileType type = ammo.type();
         ItemStack currentStack = this.inventory.slot.getStack();
         AeroLang.translate("potato_cannon.ammo", currentStack.getDisplayName(), currentStack.getCount()).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
         float damage = (float)type.damage() * 2.0F;
         AeroLang.translate("potato_cannon.attack_damage", damage).style(ChatFormatting.DARK_GREEN).forGoggles(tooltip, 1);
         if (Math.abs(this.getSpeed()) > 0.0F) {
            AeroLang.translate("potato_cannon.reload_ticks", Math.round(1.0F / this.getChargeUpSpeed()))
               .style(ChatFormatting.DARK_GREEN)
               .forGoggles(tooltip, 1);
         }

         AeroLang.translate("potato_cannon.knockback", type.knockback()).style(ChatFormatting.DARK_GREEN).forGoggles(tooltip, 1);
         return true;
      } else {
         return false;
      }
   }

   public void clearContent() {
      this.inventory.clearContent();
   }

   public static enum State {
      CHARGED,
      FIRING,
      CHARGING;
   }
}

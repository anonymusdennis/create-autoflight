package com.simibubi.create.content.equipment.potatoCannon;

import com.simibubi.create.AllEnchantments;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.api.equipment.potatoCannon.PotatoCannonProjectileType;
import com.simibubi.create.api.equipment.potatoCannon.PotatoProjectileRenderMode;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.damageTypes.CreateDamageSources;
import com.simibubi.create.foundation.particle.AirParticleData;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PotatoProjectileEntity extends AbstractHurtingProjectile implements IEntityWithComplexSpawn {
   protected PotatoCannonProjectileType type;
   protected ItemStack stack = ItemStack.EMPTY;
   protected Entity stuckEntity;
   protected Vec3 stuckOffset;
   protected PotatoProjectileRenderMode stuckRenderer;
   protected double stuckFallSpeed;
   protected float additionalDamageMult = 1.0F;
   protected float additionalKnockback = 0.0F;
   protected float recoveryChance = 0.0F;

   public PotatoProjectileEntity(EntityType<? extends AbstractHurtingProjectile> type, Level level) {
      super(type, level);
   }

   public void setItem(ItemStack stack) {
      this.stack = stack;
      this.type = (PotatoCannonProjectileType)PotatoCannonProjectileType.getTypeForItem(this.level().registryAccess(), stack.getItem())
         .orElseGet(
            () -> this.level().registryAccess().registryOrThrow(CreateRegistries.POTATO_PROJECTILE_TYPE).getHolderOrThrow(AllPotatoProjectileTypes.FALLBACK)
         )
         .value();
   }

   public void setEnchantmentEffectsFromCannon(ItemStack cannon) {
      Registry<Enchantment> enchantmentRegistry = this.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
      int recovery = cannon.getEnchantmentLevel(enchantmentRegistry.getHolderOrThrow(AllEnchantments.POTATO_RECOVERY));
      if (recovery > 0) {
         this.recoveryChance = 0.125F + (float)recovery * 0.125F;
      }
   }

   public ItemStack getItem() {
      return this.stack;
   }

   @Nullable
   public PotatoCannonProjectileType getProjectileType() {
      return this.type;
   }

   public void readAdditionalSaveData(CompoundTag nbt) {
      this.setItem(ItemStack.parseOptional(this.registryAccess(), nbt.getCompound("Item")));
      this.additionalDamageMult = nbt.getFloat("AdditionalDamage");
      this.additionalKnockback = nbt.getFloat("AdditionalKnockback");
      this.recoveryChance = nbt.getFloat("Recovery");
      super.readAdditionalSaveData(nbt);
   }

   public void addAdditionalSaveData(CompoundTag nbt) {
      nbt.put("Item", this.stack.saveOptional(this.registryAccess()));
      nbt.putFloat("AdditionalDamage", this.additionalDamageMult);
      nbt.putFloat("AdditionalKnockback", this.additionalKnockback);
      nbt.putFloat("Recovery", this.recoveryChance);
      super.addAdditionalSaveData(nbt);
   }

   @Nullable
   public Entity getStuckEntity() {
      if (this.stuckEntity == null) {
         return null;
      } else {
         return !this.stuckEntity.isAlive() ? null : this.stuckEntity;
      }
   }

   public void setStuckEntity(Entity stuckEntity) {
      this.stuckEntity = stuckEntity;
      this.stuckOffset = this.position().subtract(stuckEntity.position());
      this.stuckRenderer = new AllPotatoProjectileRenderModes.StuckToEntity(this.stuckOffset);
      this.stuckFallSpeed = 0.0;
      this.setDeltaMovement(Vec3.ZERO);
   }

   public PotatoProjectileRenderMode getRenderMode() {
      return this.getStuckEntity() != null ? this.stuckRenderer : this.type.renderMode();
   }

   public void tick() {
      Entity stuckEntity = this.getStuckEntity();
      if (stuckEntity != null) {
         if (this.getY() < stuckEntity.getY() - 0.1) {
            this.pop(this.position());
            this.kill();
         } else {
            this.stuckFallSpeed = this.stuckFallSpeed + 0.007 * (double)this.type.gravityMultiplier();
            this.stuckOffset = this.stuckOffset.add(0.0, -this.stuckFallSpeed, 0.0);
            Vec3 pos = stuckEntity.position().add(this.stuckOffset);
            this.setPos(pos.x, pos.y, pos.z);
         }
      } else {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.05 * (double)this.type.gravityMultiplier(), 0.0).scale((double)this.type.drag()));
      }

      super.tick();
   }

   protected float getInertia() {
      return 1.0F;
   }

   protected ParticleOptions getTrailParticle() {
      return new AirParticleData(1.0F, 10.0F);
   }

   protected boolean shouldBurn() {
      return false;
   }

   protected void onHitEntity(EntityHitResult ray) {
      super.onHitEntity(ray);
      if (this.getStuckEntity() == null) {
         Vec3 hit = ray.getLocation();
         Entity target = ray.getEntity();
         float damage = (float)this.type.damage() * this.additionalDamageMult;
         float knockback = this.type.knockback() + this.additionalKnockback;
         Entity owner = this.getOwner();
         if (target.isAlive()) {
            if (owner instanceof LivingEntity livingEntity) {
               livingEntity.setLastHurtMob(target);
            }

            if (target instanceof PotatoProjectileEntity ppe) {
               if (this.tickCount < 10 && target.tickCount < 10) {
                  return;
               }

               if (ppe.getProjectileType() != this.getProjectileType()) {
                  if (owner instanceof Player p) {
                     AllAdvancements.POTATO_CANNON_COLLIDE.awardTo(p);
                  }

                  if (ppe.getOwner() instanceof Player p) {
                     AllAdvancements.POTATO_CANNON_COLLIDE.awardTo(p);
                  }
               }
            }

            this.pop(hit);
            if (!(target instanceof WitherBoss) || !((WitherBoss)target).isPowered()) {
               if (!this.type.preEntityHit(this.stack, ray)) {
                  boolean targetIsEnderman = target.getType() == EntityType.ENDERMAN;
                  int k = target.getRemainingFireTicks();
                  if (this.isOnFire() && !targetIsEnderman) {
                     target.igniteForSeconds(5.0F);
                  }

                  boolean onServer = !this.level().isClientSide;
                  DamageSource damageSource = this.causePotatoDamage();
                  if (onServer && !target.hurt(damageSource, damage)) {
                     target.setRemainingFireTicks(k);
                     this.kill();
                  } else if (!targetIsEnderman) {
                     if (!this.type.onEntityHit(this.stack, ray) && onServer) {
                        if (this.random.nextDouble() <= (double)this.recoveryChance) {
                           this.recoverItem();
                        } else {
                           this.spawnAtLocation(this.type.dropStack());
                        }
                     }

                     if (!(target instanceof LivingEntity livingentity)) {
                        playHitSound(this.level(), this.position());
                        this.kill();
                     } else {
                        if (this.type.reloadTicks() < 10) {
                           livingentity.invulnerableTime = this.type.reloadTicks() + 10;
                        }

                        if (onServer && knockback > 0.0F) {
                           Vec3 appliedMotion = this.getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize();
                           if (appliedMotion.lengthSqr() > 0.0) {
                              livingentity.knockback((double)knockback * 0.6, -appliedMotion.x, -appliedMotion.z);
                           }
                        }

                        if (onServer && owner instanceof LivingEntity) {
                           EnchantmentHelper.doPostAttackEffects((ServerLevel)this.level(), livingentity, damageSource);
                        }

                        if (livingentity != owner && livingentity instanceof Player && owner instanceof ServerPlayer && !this.isSilent()) {
                           ((ServerPlayer)owner).connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.ARROW_HIT_PLAYER, 0.0F));
                        }

                        if (onServer
                           && owner instanceof ServerPlayer serverplayerentity
                           && (!target.isAlive() && target.getType().getCategory() == MobCategory.MONSTER || target instanceof Player && target != owner)) {
                           AllAdvancements.POTATO_CANNON.awardTo(serverplayerentity);
                        }

                        if (this.type.sticky() && target.isAlive()) {
                           this.setStuckEntity(target);
                        } else {
                           this.kill();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void recoverItem() {
      if (!this.stack.isEmpty()) {
         this.spawnAtLocation(this.stack.copyWithCount(1));
      }
   }

   public static void playHitSound(Level world, Vec3 location) {
      AllSoundEvents.POTATO_HIT.playOnServer(world, BlockPos.containing(location));
   }

   public static void playLaunchSound(Level world, Vec3 location, float pitch) {
      AllSoundEvents.FWOOMP.playAt(world, location, 1.0F, pitch, true);
   }

   protected void onHitBlock(BlockHitResult ray) {
      Vec3 hit = ray.getLocation();
      this.pop(hit);
      if (!this.type.onBlockHit(this.level(), this.stack, ray) && !this.level().isClientSide) {
         if (this.random.nextDouble() <= (double)this.recoveryChance) {
            this.recoverItem();
         } else {
            this.spawnAtLocation(this.getProjectileType().dropStack());
         }
      }

      super.onHitBlock(ray);
      this.kill();
   }

   public boolean hurt(@NotNull DamageSource source, float amt) {
      if (source.is(DamageTypeTags.IS_FIRE)) {
         return false;
      } else if (this.isInvulnerableTo(source)) {
         return false;
      } else {
         this.pop(this.position());
         this.kill();
         return true;
      }
   }

   private void pop(Vec3 hit) {
      if (!this.stack.isEmpty()) {
         for (int i = 0; i < 7; i++) {
            Vec3 m = VecHelper.offsetRandomly(Vec3.ZERO, this.random, 0.25F);
            this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, this.stack), hit.x, hit.y, hit.z, m.x, m.y, m.z);
         }
      }

      if (!this.level().isClientSide) {
         playHitSound(this.level(), this.position());
      }
   }

   private DamageSource causePotatoDamage() {
      return CreateDamageSources.potatoCannon(this.level(), this, this.getOwner());
   }

   public static Builder<?> build(Builder<?> builder) {
      return builder.sized(0.25F, 0.25F);
   }

   public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
      CompoundTag compound = new CompoundTag();
      this.addAdditionalSaveData(compound);
      buffer.writeNbt(compound);
   }

   public void readSpawnData(RegistryFriendlyByteBuf additionalData) {
      this.readAdditionalSaveData(additionalData.readNbt());
   }
}

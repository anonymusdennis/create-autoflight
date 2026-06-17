package com.simibubi.create.content.logistics.box;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.logistics.chute.ChuteBlock;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.LivingEntity.Fallsounds;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class PackageEntity extends LivingEntity implements IEntityWithComplexSpawn {
   private Entity originalEntity;
   public ItemStack box;
   public int insertionDelay;
   public Vec3 clientPosition;
   public Vec3 vec2 = Vec3.ZERO;
   public Vec3 vec3 = Vec3.ZERO;
   public WeakReference<Player> tossedBy = new WeakReference<>(null);

   public PackageEntity(EntityType<?> entityTypeIn, Level worldIn) {
      super(entityTypeIn, worldIn);
      this.box = ItemStack.EMPTY;
      this.setYRot(this.random.nextFloat() * 360.0F);
      this.setYHeadRot(this.getYRot());
      this.yRotO = this.getYRot();
      this.insertionDelay = 30;
   }

   public PackageEntity(Level worldIn, double x, double y, double z) {
      this((EntityType<?>)AllEntityTypes.PACKAGE.get(), worldIn);
      this.setPos(x, y, z);
      this.refreshDimensions();
   }

   public static PackageEntity fromDroppedItem(Level world, Entity originalEntity, ItemStack itemstack) {
      PackageEntity packageEntity = (PackageEntity)((EntityType)AllEntityTypes.PACKAGE.get()).create(world);
      Vec3 position = originalEntity.position();
      packageEntity.setPos(position);
      packageEntity.setBox(itemstack);
      packageEntity.setDeltaMovement(originalEntity.getDeltaMovement().scale(1.5));
      packageEntity.originalEntity = originalEntity;
      if (world != null && !world.isClientSide && ChuteBlock.isChute(world.getBlockState(BlockPos.containing(position.x, position.y + 0.5, position.z)))) {
         packageEntity.setYRot((float)((int)packageEntity.getYRot() / 90 * 90));
      }

      return packageEntity;
   }

   public static PackageEntity fromItemStack(Level world, Vec3 position, ItemStack itemstack) {
      PackageEntity packageEntity = (PackageEntity)((EntityType)AllEntityTypes.PACKAGE.get()).create(world);
      packageEntity.setPos(position);
      packageEntity.setBox(itemstack);
      return packageEntity;
   }

   public ItemStack getPickedResult(HitResult target) {
      return this.box.copy();
   }

   public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createPackageAttributes() {
      return LivingEntity.createLivingAttributes().add(Attributes.MAX_HEALTH, 5.0).add(Attributes.MOVEMENT_SPEED, 1.0);
   }

   public static Builder<?> build(Builder<?> builder) {
      return builder.sized(1.0F, 1.0F);
   }

   public void travel(Vec3 p_213352_1_) {
      super.travel(p_213352_1_);
      if (this.level().isClientSide) {
         if (!(this.getDeltaMovement().length() < 0.0078125)) {
            if (this.tickCount < 20) {
               Vec3 motion = this.getDeltaMovement().scale(0.75);
               AABB bb = this.getBoundingBox();
               List<VoxelShape> entityStream = this.level().getEntityCollisions(this, bb.expandTowards(motion));
               motion = collideBoundingBox(this, motion, bb, this.level(), entityStream);
               Vec3 clientPos = this.position().add(motion);
               if (this.lerpSteps != 0) {
                  clientPos = VecHelper.lerp(Math.min(1.0F, (float)this.tickCount / 20.0F), clientPos, new Vec3(this.lerpX, this.lerpY, this.lerpZ));
               }

               if (this.tickCount < 5) {
                  this.setPos(clientPos.x, clientPos.y, clientPos.z);
               }

               if (this.tickCount < 20) {
                  this.lerpTo(clientPos.x, clientPos.y, clientPos.z, this.getYRot(), this.getXRot(), this.lerpSteps == 0 ? 3 : this.lerpSteps);
               }
            }
         }
      }
   }

   public void lerpMotion(double x, double y, double z) {
      this.setDeltaMovement(this.getDeltaMovement().add(x, y, z).scale(0.5));
   }

   public String getAddress() {
      return (String)this.box.get(AllDataComponents.PACKAGE_ADDRESS);
   }

   public void tick() {
      if (this.firstTick) {
         this.verifyInitialEntity();
         this.originalEntity = null;
      }

      if (this.level() instanceof PonderLevel) {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.06, 0.0));
         if (this.position().y < 0.125) {
            this.discard();
         }
      }

      this.insertionDelay = Math.min(this.insertionDelay + 1, 30);
      super.tick();
      if (!PackageItem.isPackage(this.box)) {
         this.discard();
      }
   }

   protected void verifyInitialEntity() {
      if (this.originalEntity instanceof ItemEntity itemEntity) {
         CompoundTag var3 = new CompoundTag();
         itemEntity.addAdditionalSaveData(var3);
         if (var3.getInt("PickupDelay") == 32767) {
            this.discard();
         }
      }
   }

   protected EntityDimensions getDefaultDimensions(Pose pose) {
      return this.box == null ? super.getDefaultDimensions(pose) : EntityDimensions.fixed(PackageItem.getWidth(this.box), PackageItem.getHeight(this.box));
   }

   public ItemStack getBox() {
      return this.box;
   }

   public static boolean centerPackage(Entity entity, Vec3 target) {
      return entity instanceof PackageEntity packageEntity ? packageEntity.decreaseInsertionTimer(target) : true;
   }

   public boolean decreaseInsertionTimer(@Nullable Vec3 targetSpot) {
      if (targetSpot != null) {
         this.setDeltaMovement(this.getDeltaMovement().scale(0.75).multiply(1.0, 0.25, 1.0));
         Vec3 pos = this.position().add(targetSpot.subtract(this.position()).scale(0.2F));
         this.setPos(pos.x, pos.y, pos.z);
         float yawTarget = (float)((int)this.getYRot() / 90 * 90);
         this.setYRot(AngleHelper.angleLerp(0.5, (double)this.getYRot(), (double)yawTarget));
      }

      this.insertionDelay = Math.max(this.insertionDelay - 3, 0);
      return this.insertionDelay == 0;
   }

   public void setBox(ItemStack box) {
      this.box = box.copy();
      this.refreshDimensions();
   }

   public boolean isPushable() {
      return true;
   }

   public boolean canCollideWith(Entity pEntity) {
      return pEntity instanceof PackageEntity && pEntity.getBoundingBox().maxY < this.getBoundingBox().minY + 0.125;
   }

   public boolean canBeCollidedWith() {
      return false;
   }

   public InteractionResult interact(Player pPlayer, InteractionHand pHand) {
      if (!pPlayer.getItemInHand(pHand).isEmpty()) {
         return super.interact(pPlayer, pHand);
      } else if (pPlayer.level().isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         pPlayer.setItemInHand(pHand, this.box);
         this.level().playSound(null, this.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 0.75F + this.level().random.nextFloat());
         this.remove(RemovalReason.DISCARDED);
         return InteractionResult.SUCCESS;
      }
   }

   public void push(Entity entityIn) {
      boolean isOtherPackage = entityIn instanceof PackageEntity;
      if (!isOtherPackage && this.tossedBy.get() != null) {
         this.tossedBy = new WeakReference<>(null);
      }

      if (isOtherPackage) {
         if (entityIn.getBoundingBox().minY < this.getBoundingBox().maxY) {
            super.push(entityIn);
         }
      } else if (entityIn.getBoundingBox().minY <= this.getBoundingBox().minY) {
         super.push(entityIn);
      }
   }

   public Vec3 getPassengerRidingPosition(Entity entity) {
      return this.position().add(0.0, (double)entity.getDimensions(this.getPose()).height(), 0.0);
   }

   protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float partialTick) {
      return super.getPassengerAttachmentPoint(entity, dimensions, partialTick).add(0.0, 0.125, 0.0);
   }

   protected void onInsideBlock(BlockState state) {
      super.onInsideBlock(state);
      if (this.isAlive()) {
         if (state.getBlock() == Blocks.WATER
            || state.hasProperty(BlockStateProperties.WATERLOGGED) && (Boolean)state.getValue(BlockStateProperties.WATERLOGGED)) {
            this.destroy(this.damageSources().drown());
            this.remove(RemovalReason.KILLED);
         }
      }
   }

   public boolean hurt(DamageSource source, float amount) {
      if (source.getEntity() instanceof Player player && !CommonHooks.onPlayerAttackTarget(player, this)) {
         return false;
      }

      if (this.level().isClientSide || !this.isAlive()) {
         return false;
      } else if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         this.remove(RemovalReason.KILLED);
         return false;
      } else if (!this.box.getItem().canBeHurtBy(this.box, source)) {
         return false;
      } else if (!source.equals(this.damageSources().inWall()) || !this.isPassenger() && this.insertionDelay >= 20) {
         if (source.is(DamageTypeTags.IS_FALL)) {
            return false;
         } else if (this.isInvulnerableTo(source)) {
            return false;
         } else if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            this.destroy(source);
            this.remove(RemovalReason.KILLED);
            return false;
         } else if (source.is(DamageTypeTags.IS_FIRE)) {
            if (this.isOnFire()) {
               this.takeDamage(source, 0.15F);
            } else {
               this.setRemainingFireTicks(100);
            }

            return false;
         } else {
            boolean wasShot = source.getDirectEntity() instanceof AbstractArrow;
            boolean shotCanPierce = wasShot && ((AbstractArrow)source.getDirectEntity()).getPierceLevel() > 0;
            if (source.getEntity() instanceof Player && !((Player)source.getEntity()).getAbilities().mayBuild) {
               return false;
            } else {
               this.destroy(source);
               this.remove(RemovalReason.KILLED);
               return shotCanPierce;
            }
         }
      } else {
         return false;
      }
   }

   private void takeDamage(DamageSource source, float amount) {
      float hp = this.getHealth();
      hp -= amount;
      if (hp <= 0.5F) {
         this.destroy(source);
         this.remove(RemovalReason.KILLED);
      } else {
         this.setHealth(hp);
      }
   }

   private void destroy(DamageSource source) {
      CatnipServices.NETWORK.sendToClientsTrackingEntity(this, new PackageDestroyPacket(this.getBoundingBox().getCenter(), this.box));
      AllSoundEvents.PACKAGE_POP.playOnServer(this.level(), this.blockPosition());
      if (this.level() instanceof ServerLevel serverLevel) {
         this.dropAllDeathLoot(serverLevel, source);
      }
   }

   protected void dropAllDeathLoot(ServerLevel level, DamageSource pDamageSource) {
      super.dropAllDeathLoot(level, pDamageSource);
      ItemStackHandler contents = PackageItem.getContents(this.box);

      for (int i = 0; i < contents.getSlots(); i++) {
         ItemStack itemstack = contents.getStackInSlot(i);
         if (itemstack.getItem() instanceof SpawnEggItem sei) {
            EntityType<?> entitytype = sei.getType(itemstack);
            Entity entity = entitytype.spawn(level, itemstack, null, this.blockPosition(), MobSpawnType.SPAWN_EGG, false, false);
            if (entity != null) {
               itemstack.shrink(1);
            }
         }

         if (!itemstack.isEmpty()) {
            ItemEntity entityIn = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), itemstack);
            level.addFreshEntity(entityIn);
         }
      }
   }

   public void readAdditionalSaveData(CompoundTag compound) {
      super.readAdditionalSaveData(compound);
      this.box = ItemStack.parseOptional(this.level().registryAccess(), compound.getCompound("Box"));
      this.refreshDimensions();
   }

   public void addAdditionalSaveData(CompoundTag compound) {
      super.addAdditionalSaveData(compound);
      compound.put("Box", this.box.saveOptional(this.level().registryAccess()));
   }

   public Iterable<ItemStack> getArmorSlots() {
      return Collections.emptyList();
   }

   public ItemStack getItemBySlot(EquipmentSlot pSlot) {
      return pSlot == EquipmentSlot.MAINHAND ? this.getBox() : ItemStack.EMPTY;
   }

   public void setItemSlot(EquipmentSlot pSlot, ItemStack pStack) {
      if (pSlot == EquipmentSlot.MAINHAND) {
         this.setBox(pStack);
      }
   }

   public HumanoidArm getMainArm() {
      return HumanoidArm.RIGHT;
   }

   public InteractionHand getUsedItemHand() {
      return InteractionHand.MAIN_HAND;
   }

   public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
      ItemStack.STREAM_CODEC.encode(buffer, this.getBox());
      Vec3 motion = this.getDeltaMovement();
      buffer.writeFloat((float)motion.x);
      buffer.writeFloat((float)motion.y);
      buffer.writeFloat((float)motion.z);
   }

   public void readSpawnData(RegistryFriendlyByteBuf additionalData) {
      this.setBox((ItemStack)ItemStack.STREAM_CODEC.decode(additionalData));
      this.setDeltaMovement((double)additionalData.readFloat(), (double)additionalData.readFloat(), (double)additionalData.readFloat());
   }

   public float getVoicePitch() {
      return 1.5F;
   }

   public Fallsounds getFallSounds() {
      return new Fallsounds(SoundEvents.CHISELED_BOOKSHELF_FALL, SoundEvents.CHISELED_BOOKSHELF_FALL);
   }

   @Nullable
   protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
      return null;
   }

   @Nullable
   protected SoundEvent getDeathSound() {
      return null;
   }

   public boolean isAffectedByPotions() {
      return false;
   }

   public boolean fireImmune() {
      return this.box.has(DataComponents.FIRE_RESISTANT) || super.fireImmune();
   }
}

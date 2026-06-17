package com.simibubi.create.content.kinetics.crusher;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.damageTypes.CreateDamageSources;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.sound.SoundScapes;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

public class CrushingWheelControllerBlockEntity extends SmartBlockEntity implements Clearable {
   public Entity processingEntity;
   private UUID entityUUID;
   protected boolean searchForEntity;
   public ProcessingInventory inventory = new ProcessingInventory(this::itemInserted) {
      @Override
      public boolean isItemValid(int slot, ItemStack stack) {
         return super.isItemValid(slot, stack) && CrushingWheelControllerBlockEntity.this.processingEntity == null;
      }
   };
   private RecipeWrapper wrapper = new RecipeWrapper(this.inventory);
   public float crushingspeed;

   public CrushingWheelControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      event.registerBlockEntity(ItemHandler.BLOCK, (BlockEntityType)AllBlockEntityTypes.CRUSHING_WHEEL_CONTROLLER.get(), (be, context) -> be.inventory);
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(new DirectBeltInputBehaviour(this).onlyInsertWhen(this::supportsDirectBeltInput));
   }

   private boolean supportsDirectBeltInput(Direction side) {
      BlockState blockState = this.getBlockState();
      if (blockState == null) {
         return false;
      } else {
         Direction direction = (Direction)blockState.getValue(CrushingWheelControllerBlock.FACING);
         return direction == Direction.DOWN || direction == side;
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (this.searchForEntity) {
         this.searchForEntity = false;
         List<Entity> search = this.level.getEntities((Entity)null, new AABB(this.getBlockPos()), e -> this.entityUUID.equals(e.getUUID()));
         if (search.isEmpty()) {
            this.clear();
         } else {
            this.processingEntity = search.get(0);
         }
      }

      if (this.isOccupied()) {
         if (this.crushingspeed != 0.0F) {
            if (this.level.isClientSide) {
               CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> this.tickAudio());
            }

            float speed = this.crushingspeed * 4.0F;
            Vec3 centerPos = VecHelper.getCenterOf(this.worldPosition);
            Direction facing = (Direction)this.getBlockState().getValue(CrushingWheelControllerBlock.FACING);
            int offset = facing.getAxisDirection().getStep();
            Vec3 outSpeed = new Vec3(
               (facing.getAxis() == Axis.X ? 0.25 : 0.0) * (double)offset,
               offset == 1 ? (facing.getAxis() == Axis.Y ? 0.5 : 0.0) : 0.0,
               (facing.getAxis() == Axis.Z ? 0.25 : 0.0) * (double)offset
            );
            Vec3 outPos = centerPos.add(
               (double)(facing.getAxis() == Axis.X ? 0.55F * (float)offset : 0.0F),
               (double)(facing.getAxis() == Axis.Y ? 0.55F * (float)offset : 0.0F),
               (double)(facing.getAxis() == Axis.Z ? 0.55F * (float)offset : 0.0F)
            );
            if (!this.hasEntity()) {
               float processingSpeed = Mth.clamp(
                  speed / (!this.inventory.appliedRecipe ? (float)Math.log((double)this.inventory.getStackInSlot(0).getCount()) / (float)Math.log(2.0) : 1.0F),
                  0.25F,
                  20.0F
               );
               this.inventory.remainingTime -= processingSpeed;
               this.spawnParticles(this.inventory.getStackInSlot(0));
               if (!this.level.isClientSide) {
                  if (this.inventory.remainingTime < 20.0F && !this.inventory.appliedRecipe) {
                     this.applyRecipe();
                     this.inventory.appliedRecipe = true;
                     this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 18);
                  } else if (!(this.inventory.remainingTime > 0.0F)) {
                     this.inventory.remainingTime = 0.0F;
                     if (facing != Direction.UP) {
                        BlockPos nextPos = this.worldPosition.below().relative(facing, facing.getAxis() == Axis.Y ? 0 : 1);
                        DirectBeltInputBehaviour behaviour = BlockEntityBehaviour.get(this.level, nextPos, DirectBeltInputBehaviour.TYPE);
                        if (behaviour != null) {
                           boolean changed = false;
                           if (!behaviour.canInsertFromSide(facing)) {
                              return;
                           }

                           for (int slot = 0; slot < this.inventory.getSlots(); slot++) {
                              ItemStack stack = this.inventory.getStackInSlot(slot);
                              if (!stack.isEmpty()) {
                                 ItemStack remainder = behaviour.handleInsertion(stack, facing, false);
                                 if (!ItemStack.matches(remainder, stack)) {
                                    this.inventory.setStackInSlot(slot, remainder);
                                    changed = true;
                                 }
                              }
                           }

                           if (changed) {
                              this.setChanged();
                              this.sendData();
                           }

                           return;
                        }
                     }

                     for (int slotx = 0; slotx < this.inventory.getSlots(); slotx++) {
                        ItemStack stack = this.inventory.getStackInSlot(slotx);
                        if (!stack.isEmpty()) {
                           ItemEntity entityIn = new ItemEntity(this.level, outPos.x, outPos.y, outPos.z, stack);
                           entityIn.setDeltaMovement(outSpeed);
                           entityIn.getPersistentData().put("BypassCrushingWheel", NbtUtils.writeBlockPos(this.worldPosition));
                           this.level.addFreshEntity(entityIn);
                        }
                     }

                     this.inventory.clear();
                     this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 18);
                  }
               }
            } else if (this.processingEntity.isAlive() && this.processingEntity.getBoundingBox().intersects(new AABB(this.worldPosition).inflate(0.5))) {
               double xMotion = ((double)((float)this.worldPosition.getX() + 0.5F) - this.processingEntity.getX()) / 2.0;
               double zMotion = ((double)((float)this.worldPosition.getZ() + 0.5F) - this.processingEntity.getZ()) / 2.0;
               if (this.processingEntity.isShiftKeyDown()) {
                  zMotion = 0.0;
                  xMotion = 0.0;
               }

               double movement = (double)(Math.max(-speed / 4.0F, -0.5F) * (float)(-offset));
               this.processingEntity
                  .setDeltaMovement(
                     new Vec3(
                        facing.getAxis() == Axis.X ? movement : xMotion,
                        facing.getAxis() == Axis.Y ? movement : 0.0,
                        facing.getAxis() == Axis.Z ? movement : zMotion
                     )
                  );
               if (!this.level.isClientSide) {
                  if (this.processingEntity instanceof ItemEntity itemEntity) {
                     itemEntity.setPickUpDelay(20);
                     if (facing.getAxis() == Axis.Y) {
                        if (this.processingEntity.getY() * (double)(-offset) < (centerPos.y - 0.25) * (double)(-offset)) {
                           this.intakeItem(itemEntity);
                        }
                     } else if (facing.getAxis() == Axis.Z) {
                        if (this.processingEntity.getZ() * (double)(-offset) < (centerPos.z - 0.25) * (double)(-offset)) {
                           this.intakeItem(itemEntity);
                        }
                     } else if (this.processingEntity.getX() * (double)(-offset) < (centerPos.x - 0.25) * (double)(-offset)) {
                        this.intakeItem(itemEntity);
                     }
                  } else {
                     Vec3 entityOutPos = outPos.add(
                        facing.getAxis() == Axis.X ? (double)(0.5F * (float)offset) : 0.0,
                        facing.getAxis() == Axis.Y ? (double)(0.5F * (float)offset) : 0.0,
                        facing.getAxis() == Axis.Z ? (double)(0.5F * (float)offset) : 0.0
                     );
                     int crusherDamage = (Integer)AllConfigs.server().kinetics.crushingDamage.get();
                     if (this.processingEntity instanceof LivingEntity livingEntity
                        && livingEntity.getHealth() - (float)crusherDamage <= 0.0F
                        && livingEntity.hurtTime <= 0) {
                        this.processingEntity.setPos(entityOutPos.x, entityOutPos.y, entityOutPos.z);
                     }

                     this.processingEntity.hurt(CreateDamageSources.crush(this.level), (float)crusherDamage);
                     if (!this.processingEntity.isAlive()) {
                        this.processingEntity.setPos(entityOutPos.x, entityOutPos.y, entityOutPos.z);
                     }
                  }
               }
            } else {
               this.clear();
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public void tickAudio() {
      float pitch = Mth.clamp(this.crushingspeed / 256.0F + 0.45F, 0.85F, 1.0F);
      if (this.entityUUID != null || !this.inventory.getStackInSlot(0).isEmpty()) {
         SoundScapes.play(SoundScapes.AmbienceGroup.CRUSHING, this.worldPosition, pitch);
      }
   }

   private void intakeItem(ItemEntity itemEntity) {
      this.inventory.clear();
      this.inventory.setStackInSlot(0, itemEntity.getItem().copy());
      this.itemInserted(this.inventory.getStackInSlot(0));
      itemEntity.discard();
      this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 18);
   }

   protected void spawnParticles(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         ParticleOptions particleData = null;
         if (stack.getItem() instanceof BlockItem) {
            particleData = new BlockParticleOption(ParticleTypes.BLOCK, ((BlockItem)stack.getItem()).getBlock().defaultBlockState());
         } else {
            particleData = new ItemParticleOption(ParticleTypes.ITEM, stack);
         }

         RandomSource r = this.level.random;

         for (int i = 0; i < 4; i++) {
            this.level
               .addParticle(
                  particleData,
                  (double)((float)this.worldPosition.getX() + r.nextFloat()),
                  (double)((float)this.worldPosition.getY() + r.nextFloat()),
                  (double)((float)this.worldPosition.getZ() + r.nextFloat()),
                  0.0,
                  0.0,
                  0.0
               );
         }
      }
   }

   private void applyRecipe() {
      Optional<RecipeHolder<StandardProcessingRecipe<RecipeWrapper>>> recipe = this.findRecipe();
      List<ItemStack> list = new ArrayList<>();
      if (recipe.isPresent()) {
         ItemStack input = this.inventory.getStackInSlot(0);
         int rolls = input.getCount();
         this.inventory.clear();

         for (int roll = 0; roll < rolls; roll++) {
            for (ItemStack stack : ((StandardProcessingRecipe)recipe.get().value()).rollResults(this.level.random)) {
               ItemHelper.addToList(stack, list);
            }
         }

         if (input.hasCraftingRemainingItem()) {
            ItemHelper.addToList(input.getCraftingRemainingItem(), list);
         }

         for (int slot = 0; slot < list.size() && slot + 1 < this.inventory.getSlots(); slot++) {
            this.inventory.setStackInSlot(slot + 1, list.get(slot));
         }
      } else {
         this.inventory.clear();
      }
   }

   public Optional<RecipeHolder<StandardProcessingRecipe<RecipeWrapper>>> findRecipe() {
      Optional<RecipeHolder<StandardProcessingRecipe<RecipeWrapper>>> crushingRecipe = AllRecipeTypes.CRUSHING.find(this.wrapper, this.level);
      if (!crushingRecipe.isPresent()) {
         crushingRecipe = AllRecipeTypes.MILLING.find(this.wrapper, this.level);
      }

      return crushingRecipe;
   }

   @Override
   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      if (this.hasEntity()) {
         compound.put("Entity", NbtUtils.createUUID(this.entityUUID));
      }

      compound.put("Inventory", this.inventory.serializeNBT(registries));
      compound.putFloat("Speed", this.crushingspeed);
      super.write(compound, registries, clientPacket);
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      if (compound.contains("Entity") && !this.isOccupied()) {
         this.entityUUID = NbtUtils.loadUUID(NBTHelper.getINBT(compound, "Entity"));
         this.searchForEntity = true;
      }

      this.crushingspeed = compound.getFloat("Speed");
      this.inventory.deserializeNBT(registries, compound.getCompound("Inventory"));
   }

   public void clearContent() {
      this.inventory.clear();
   }

   public void startCrushing(Entity entity) {
      this.processingEntity = entity;
      this.entityUUID = entity.getUUID();
   }

   private void itemInserted(ItemStack stack) {
      Optional<RecipeHolder<StandardProcessingRecipe<RecipeWrapper>>> recipe = this.findRecipe();
      this.inventory.remainingTime = recipe.isPresent() ? (float)((StandardProcessingRecipe)recipe.get().value()).getProcessingDuration() : 100.0F;
      this.inventory.appliedRecipe = false;
   }

   public void clear() {
      this.processingEntity = null;
      this.entityUUID = null;
   }

   public boolean isOccupied() {
      return this.hasEntity() || !this.inventory.isEmpty();
   }

   public boolean hasEntity() {
      return this.processingEntity != null;
   }
}

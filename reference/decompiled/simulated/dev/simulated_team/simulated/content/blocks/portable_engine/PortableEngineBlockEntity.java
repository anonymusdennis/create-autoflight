package dev.simulated_team.simulated.content.blocks.portable_engine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.IControlContraption.MovementMode;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity.RotationDirection;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimSoundEvents;
import dev.simulated_team.simulated.index.SimStats;
import dev.simulated_team.simulated.multiloader.inventory.ContainerSlot;
import dev.simulated_team.simulated.multiloader.inventory.ItemInfoWrapper;
import dev.simulated_team.simulated.service.SimItemService;
import java.util.List;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class PortableEngineBlockEntity extends GeneratingKineticBlockEntity implements Clearable {
   public static int INFINITE_THRESHOLD = 51840000;
   public PortableEngineInventory inventory;
   private int burnTime = 0;
   private boolean superHeated = false;
   protected float generatedSpeed;
   protected ScrollOptionBehaviour<MovementMode> movementDirection;
   protected float clientAngle;
   public float lastHatchOpenTime = 0.0F;
   public float hatchOpenTime = 0.0F;
   protected boolean eatingCake = false;
   protected LerpedFloat visualSpeed = LerpedFloat.linear();
   protected LerpedFloat visualStrength = LerpedFloat.linear();
   public boolean openHatchOverride;

   public PortableEngineBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
      super(typeIn, pos, state);
      this.inventory = new PortableEngineInventory(this);
   }

   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      this.movementDirection = new ScrollOptionBehaviour(
         RotationDirection.class,
         Component.translatable("create.contraptions.windmill.rotation_direction"),
         this,
         new PortableEngineBlockEntity.PortableEngineValueBoxTransform()
      );
      this.movementDirection.withCallback(t -> this.onDirectionChanged());
      behaviours.add(this.movementDirection);
      super.addBehaviours(behaviours);
   }

   public void clearContent() {
      this.inventory.clearContent();
   }

   private void onDirectionChanged() {
      if (!this.level.isClientSide) {
         this.updateGeneratedRotation();
      }
   }

   protected static BlockPos getCameraPos() {
      Entity renderViewEntity = Minecraft.getInstance().cameraEntity;
      return renderViewEntity == null ? BlockPos.ZERO : renderViewEntity.blockPosition();
   }

   public float getGeneratedSpeed() {
      return convertToDirection(
            this.generatedSpeed * (float)(this.movementDirection.getValue() > 0 ? -1 : 1),
            (Direction)this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)
         )
         * (float)(this.superHeated ? 2 : 1);
   }

   public void tick() {
      super.tick();
      boolean isLit = this.burnTime > 0;
      if (this.level.isClientSide) {
         float targetSpeed = this.isVirtual() ? this.speed : this.getGeneratedSpeed();
         this.visualSpeed.updateChaseTarget(targetSpeed);
         this.visualSpeed.tickChaser();
         float heatTarget = isLit ? 1.0F : 0.0F;
         float heatSpeed = 0.02F;
         if (this.visualStrength.getValue() > heatTarget) {
            heatSpeed = 0.1F;
         }

         this.visualStrength.chase((double)heatTarget, (double)heatSpeed, Chaser.EXP);
         this.visualStrength.tickChaser();
         float s = this.visualSpeed.getValue() * 3.0F / 10.0F;
         float soundAngle = Math.abs(this.clientAngle % 90.0F) - 45.0F;
         if (soundAngle > 0.0F && soundAngle < Math.abs(s)) {
            double pRand = this.level.getRandom().nextDouble();
            double distSq = Sable.HELPER
               .distanceSquaredWithSubLevels(this.level, JOMLConversion.atCenterOf(getCameraPos()), JOMLConversion.atCenterOf(this.worldPosition));
            double dist = Math.sqrt(distSq);
            double ratio = 1.0 - dist / 8.0;
            if (ratio > 0.0) {
               SimSoundEvents.PORTABLE_ENGINE_PUFF.playAt(this.level, this.worldPosition, (float)ratio, 1.0F, false);
            }

            if (pRand < 0.05) {
               SimSoundEvents.PORTABLE_ENGINE_AMBIENT.playAt(this.level, this.worldPosition, 0.8F, 1.0F, false);
            }
         }

         this.clientAngle += s;
         this.clientAngle %= 360.0F;
         if (isLit && !this.isVirtual()) {
            this.spawnParticles();
         }

         this.updateHatchTime();
      }

      if (!this.isVirtual()) {
         if (this.getGeneratedSpeed() != 0.0F && this.getSpeed() == 0.0F) {
            this.updateGeneratedRotation();
         }

         ContainerSlot slot = this.inventory.slot;
         ItemStack stack = slot.getStack();
         boolean previousSuperHeated = false;
         if (this.burnTime > 0 && !this.isCurrentFuelInfinite()) {
            this.burnTime--;
            if (PortableEngineBlock.analogPower(this.burnTime) != PortableEngineBlock.analogPower(this.burnTime + 1)) {
               this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
            }
         }

         if (this.burnTime <= 0 && !this.inventory.isEmpty()) {
            this.burnTime = SimItemService.INSTANCE.getBurnTime(stack);
            this.superHeated = this.getNextSuperHeated();
            if (this.burnTime > 0) {
               if (stack.getCount() == 1 && stack.getItem().hasCraftingRemainingItem()) {
                  slot.setStack(slot.getType().getCraftingRemainingItem().getDefaultInstance());
               } else {
                  slot.shrink(1L);
               }
            }
         }

         if (this.burnTime <= 0) {
            this.superHeated = false;
         }

         boolean isLitState = PortableEngineBlock.isLitState(this.getBlockState());
         int generatedSpeed = 32;
         if (this.generatedSpeed == 0.0F && isLit && (double)this.getSpeed() != 0.0) {
            float newSpeed = convertToDirection(
               (float)(this.movementDirection.getValue() > 0 ? -1 : 1), (Direction)this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)
            );
            if (Mth.sign((double)newSpeed) != Mth.sign((double)this.getSpeed())) {
               this.generatedSpeed = isLit ? 32.0F : 0.0F;
               MovementMode[] directions = MovementMode.values();
               MovementMode existingValue = directions[this.movementDirection.getValue()];
               this.movementDirection.setValue((existingValue.ordinal() + 1) % directions.length);
               this.updateGeneratedRotation();
            }
         }

         this.generatedSpeed = isLit ? 32.0F : 0.0F;
         if (isLitState && !isLit) {
            this.level.setBlock(this.getBlockPos(), (BlockState)this.getBlockState().setValue(BlockStateProperties.LIT, false), 2);
            this.updateGeneratedRotation();
         }

         if (!isLitState && isLit) {
            this.level.setBlock(this.getBlockPos(), (BlockState)this.getBlockState().setValue(BlockStateProperties.LIT, true), 2);
            this.level
               .playSound(
                  null,
                  this.worldPosition,
                  SimSoundEvents.PORTABLE_ENGINE_ROARS.event(),
                  SoundSource.BLOCKS,
                  0.125F + this.level.random.nextFloat() * 0.125F,
                  0.75F - this.level.random.nextFloat() * 0.25F
               );
            Vec3 pos = VecHelper.getCenterOf(this.worldPosition);
            Direction direction = (Direction)this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
            Vec3i N = direction.getNormal();
            Vec3 N2 = new Vec3((double)N.getX(), (double)N.getY(), (double)N.getZ());
            pos = pos.add((double)(-N.getX()) * 0.53, -0.1, (double)(-N.getZ()) * 0.53);
            Vec3 speed = VecHelper.offsetRandomly(Vec3.ZERO, RandomSource.create(), 0.01F).add(N2.scale(-0.03));

            for (int i = 0; i < 2; i++) {
               Vec3 random = VecHelper.offsetRandomly(Vec3.ZERO, RandomSource.create(), 0.1F);
               random = random.subtract(N2.scale(random.dot(N2)));
               pos = pos.add(random);
               this.level.addParticle(ParticleTypes.FLAME, pos.x, pos.y, pos.z, speed.x, speed.y, speed.z);
            }

            this.updateGeneratedRotation();
         }

         if (this.superHeated && isLitState && isLit) {
            this.updateGeneratedRotation();
         }

         if (!this.level.isClientSide()) {
            if (this.eatingCake) {
               this.eatingCake = false;
               this.sendData();
            }

            Direction direction = ((Direction)this.getBlockState().getValue(PortableEngineBlock.HORIZONTAL_FACING)).getOpposite();
            BlockPos front = this.getBlockPos().relative(direction);
            long time = this.level.getGameTime() % 60L;
            if (time == 0L && this.level.getBlockState(front).is(Blocks.CAKE)) {
               this.eatingCake = true;
               this.sendData();
               BlockState state = this.level.getBlockState(front);
               if ((Integer)state.getValue(CakeBlock.BITES) < 6) {
                  this.level.setBlock(front, (BlockState)state.cycle(CakeBlock.BITES), 2);
               } else {
                  this.level.removeBlock(front, false);
                  AABB aabb = new AABB(this.getBlockPos()).inflate(8.0);

                  for (Player player : this.level.getEntitiesOfClass(Player.class, aabb)) {
                     SimStats.PORTABLE_ENGINES_FED.awardTo(player);
                  }
               }

               this.burnTime += 100;
               this.level.playSound(null, this.getBlockPos(), SoundEvents.GENERIC_EAT, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
         }
      }
   }

   public float getHatchOpenTime(float partialTicks) {
      return Mth.lerp(partialTicks, this.lastHatchOpenTime, this.hatchOpenTime);
   }

   private void updateHatchTime() {
      boolean openHatch = false;
      BlockPos pos = this.getBlockPos();
      Vec3 center = pos.getCenter();

      for (Player player : this.level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(7.0))) {
         if (Sable.HELPER.distanceSquaredWithSubLevels(this.level, player.getEyePosition(), center)
            < Mth.square(player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 0.7)) {
            openHatch = this.canOpenHatch(player);
            if (openHatch) {
               break;
            }
         }
      }

      openHatch |= this.openHatchOverride;
      float speed = 1.35F;
      int dir = openHatch ? 1 : -1;
      if (this.eatingCake) {
         dir = -dir * 5;
      }

      this.lastHatchOpenTime = this.hatchOpenTime;
      this.hatchOpenTime = Math.clamp(this.hatchOpenTime + (float)dir * 1.35F, 0.0F, 10.0F);
   }

   private boolean canOpenHatch(Player player) {
      ItemStack heldItem = player.getMainHandItem();
      return this.inventory.insertGeneral(ItemInfoWrapper.generateFromStack(heldItem), heldItem.getCount(), true) > 0;
   }

   public void spawnParticles() {
      Vec3 hatchPos = VecHelper.getCenterOf(this.worldPosition);
      Direction direction = (Direction)this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
      Vec3i facingDirI = direction.getNormal();
      Vec3 facingDir = new Vec3((double)facingDirI.getX(), (double)facingDirI.getY(), (double)facingDirI.getZ());
      Vec3 rightDir = facingDir.yRot((float) (Math.PI / 2));
      hatchPos = hatchPos.add((double)(-facingDirI.getX()) * 0.53, -0.1, (double)(-facingDirI.getZ()) * 0.53);
      if ((double)Create.RANDOM.nextFloat() < 0.12) {
         Vec3 random = VecHelper.offsetRandomly(Vec3.ZERO, RandomSource.create(), 0.15F);
         random = random.subtract(facingDir.scale(random.dot(facingDir)));
         hatchPos = hatchPos.add(random);
         if (this.isSuperHeated() && (double)Create.RANDOM.nextFloat() < 0.3) {
            ParticleOptions particle = ParticleTypes.FLAME;
         } else {
            ParticleOptions particle = ParticleTypes.SMOKE;
         }

         Vec3 pos = VecHelper.offsetRandomly(Vec3.ZERO, RandomSource.create(), 0.01F);
      }

      for (int i = -1; i < 2; i += 2) {
         if ((double)Create.RANDOM.nextFloat() < 0.25) {
            Vec3 random = VecHelper.offsetRandomly(Vec3.ZERO, RandomSource.create(), 0.0625F);
            Vec3 pos = Vec3.upFromBottomCenterOf(this.worldPosition, 0.6875).add(facingDir.scale(0.5)).add(rightDir.scale(0.5 * (double)i)).add(random);
            Vec3 speed = VecHelper.offsetRandomly(Vec3.ZERO, RandomSource.create(), 0.01F);
            this.level.addParticle(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, speed.x, speed.y, speed.z);
         }
      }

      if (this.hatchOpenTime > 0.0F && (double)Create.RANDOM.nextFloat() < 0.08) {
         Vec3 random = VecHelper.offsetRandomly(Vec3.ZERO, RandomSource.create(), 0.1F);
         random = random.subtract(facingDir.scale(random.dot(facingDir)));
         hatchPos = hatchPos.add(random);
         this.level.addParticle(this.isSuperHeated() ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, hatchPos.x, hatchPos.y, hatchPos.z, 0.0, 0.0, 0.0);
      }
   }

   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.write(compound, registries, clientPacket);
      compound.putBoolean("SuperHeated", this.superHeated);
      compound.putFloat("GeneratedSpeed", this.generatedSpeed);
      compound.putBoolean("EatingCake", this.eatingCake);
      compound.put("Inventory", this.inventory.write(registries));
      compound.putInt("BurnTime", this.burnTime);
   }

   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      this.superHeated = compound.getBoolean("SuperHeated");
      this.inventory.read(registries, compound.getCompound("Inventory"));
      this.burnTime = compound.getInt("BurnTime");
      this.generatedSpeed = compound.getFloat("GeneratedSpeed");
      this.eatingCake = compound.getBoolean("EatingCake");
      if (clientPacket || this.isVirtual()) {
         this.visualSpeed.chase((double)this.getGeneratedSpeed(), 0.125, Chaser.EXP);
      }
   }

   public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      super.addToGoggleTooltip(tooltip, isPlayerSneaking);
      SimLang.translate("portable_engine.tooltip_name").text(":").forGoggles(tooltip);
      ItemStack currentStack = this.inventory.slot.getStack();
      boolean hasByProduct = !currentStack.isEmpty() && SimItemService.INSTANCE.getBurnTime(currentStack) == 0;
      LangBuilder noFuel = SimLang.translate("portable_engine.none").style(ChatFormatting.RED);
      LangBuilder stackName = SimLang.builder().add(currentStack.getHoverName()).text(" x" + currentStack.getCount()).style(ChatFormatting.GREEN);
      if (!this.isCurrentFuelInfinite()) {
         String langKey = hasByProduct ? "byproduct" : "fuel";
         SimLang.translate("portable_engine." + langKey, currentStack.isEmpty() ? noFuel : stackName).style(ChatFormatting.GRAY).forGoggles(tooltip);
      }

      if (this.burnTime > 0) {
         int seconds = this.getCurrentBurnTime() / 20;
         int secondsTotal = this.getTotalBurnTime() / 20;
         LangBuilder infiniteLang = SimLang.translate("portable_engine.infinite").style(ChatFormatting.LIGHT_PURPLE);
         LangBuilder timeLang = SimLang.text(this.getTime(secondsTotal)).style(this.isSuperHeated() ? ChatFormatting.GOLD : ChatFormatting.AQUA);
         SimLang.translate("portable_engine.time", this.isTotalFuelInfinite() ? infiniteLang : timeLang).style(ChatFormatting.GRAY).forGoggles(tooltip);
         if (this.superHeated) {
            if (this.isCurrentFuelInfinite()) {
               SimLang.translate("portable_engine.superheated").style(ChatFormatting.GOLD).forGoggles(tooltip);
            } else {
               SimLang.translate("portable_engine.superheated_time", this.getTime(this.getNextSuperHeated() ? secondsTotal : seconds))
                  .style(ChatFormatting.GOLD)
                  .forGoggles(tooltip);
            }
         }
      }

      return true;
   }

   private String getTime(int sec) {
      String s = "";
      int min = sec / 60;
      int hour = min / 60;
      sec = Math.floorMod(sec, 60);
      min = Math.floorMod(min, 60);
      if (hour > 0) {
         s = s + hour + "h ";
      }

      if (min < 10 && hour > 0) {
         s = s + "0";
      }

      if (min > 0 || hour > 0) {
         s = s + min + "m ";
      }

      if (sec < 10 && min > 0) {
         s = s + "0";
      }

      return s + sec + "s";
   }

   public boolean isCurrentFuelInfinite() {
      return this.burnTime >= INFINITE_THRESHOLD;
   }

   public boolean isTotalFuelInfinite() {
      return this.getNextBurnTime() >= INFINITE_THRESHOLD || this.isCurrentFuelInfinite();
   }

   public int getCurrentBurnTime() {
      return this.burnTime;
   }

   public void setCurrentBurnTime(int value) {
      this.burnTime = value;
   }

   public int getTotalBurnTime() {
      return this.getCurrentBurnTime() + this.inventory.slot.getStack().getCount() * this.getNextBurnTime();
   }

   private int getNextBurnTime() {
      return SimItemService.INSTANCE.getBurnTime(this.inventory.slot.getStack());
   }

   public boolean isSuperHeated() {
      return this.superHeated;
   }

   public void setSuperHeated(boolean value) {
      this.superHeated = value;
   }

   private boolean getNextSuperHeated() {
      return SimItemService.INSTANCE.getSuperheatedBurnTime(this.inventory.slot.getStack()) > 0;
   }

   private static class PortableEngineValueBoxTransform extends ValueBoxTransform {
      public Vec3 getLocalOffset(LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState) {
         Direction facing = (Direction)blockState.getValue(PortableEngineBlock.HORIZONTAL_FACING);
         float yRot = AngleHelper.horizontalAngle(facing);
         return VecHelper.rotateCentered(VecHelper.voxelSpace(8.0, 13.5, 7.4F), (double)yRot, Axis.Y);
      }

      public void rotate(LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState, PoseStack poseStack) {
         float yRot = AngleHelper.horizontalAngle((Direction)blockState.getValue(BlockStateProperties.HORIZONTAL_FACING));
         ((PoseTransformStack)((PoseTransformStack)TransformStack.of(poseStack).rotateYDegrees(yRot)).rotateXDegrees(90.0F)).translate(0.0, 0.1, 0.0);
      }
   }
}

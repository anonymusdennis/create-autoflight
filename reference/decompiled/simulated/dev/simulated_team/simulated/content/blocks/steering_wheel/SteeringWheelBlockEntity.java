package dev.simulated_team.simulated.content.blocks.steering_wheel;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencerInstructions;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlockEntity.SequenceContext;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.simulated_team.simulated.content.blocks.behaviour.HoldTipBehaviour;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimClickInteractions;
import java.util.List;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class SteeringWheelBlockEntity extends GeneratingKineticBlockEntity {
   public static final int RPM = 16;
   public boolean held = false;
   private int inUse = 0;
   public ScrollValueBehaviour angleInput;
   public float targetAngle = 0.0F;
   public float targetAngleToUpdate = 0.0F;
   private float angle = 0.0F;
   private float clientAngle = 0.0F;
   private float oldClientAngle = 0.0F;
   private double sequencedAngleLimit = 0.0;
   float generatedSpeed = 0.0F;
   float logicalSpeed = 0.0F;
   public BlockState material = Blocks.SPRUCE_PLANKS.defaultBlockState();
   private static final MutableComponent ROTATE_TIP = SimLang.translate("gui.hold_tip.hold_to_rotate").component();
   private int lastPlayedIncrement = 0;

   public SteeringWheelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.effects = new NoParticleKineticEffectHandler(this);
   }

   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(new HoldTipBehaviour(this, SteeringWheelBlockEntity::holdTipGetter));
      behaviours.add(this.angleInput = new SteeringWheelBlockEntity.SteeringWheelScrollValueBehaviour(this).between(1, 360));
      this.angleInput.value = 180;
   }

   public static MutableComponent holdTipGetter(Player player, BlockPos pos, BlockState state) {
      return SteeringWheelBlock.lookingAtWheel(player, pos, 1.0F, state) ? ROTATE_TIP : null;
   }

   public void startHolding() {
      this.held = true;
      this.notifyUpdate();
   }

   public void stopHolding() {
      this.held = false;
      this.notifyUpdate();
   }

   public float directionConvert(float val) {
      return -KineticBlockEntity.convertToDirection(val, (Direction)this.getBlockState().getValue(SteeringWheelBlock.FACING));
   }

   public void updateTargetAngle(float absoluteTarget) {
      absoluteTarget = Mth.clamp(absoluteTarget, (float)(-this.angleInput.getValue()), (float)this.angleInput.getValue());
      if (this.targetAngle != absoluteTarget) {
         this.targetAngle = absoluteTarget;
         float relativeAngle = absoluteTarget - this.angle;
         if ((double)Math.abs(relativeAngle) < 0.001 && this.inUse <= 0) {
            this.generatedSpeed = 0.0F;
            this.updateGeneratedRotation();
         } else {
            float rotationSpeed = 16.0F * Math.signum(relativeAngle);
            if (rotationSpeed != 0.0F) {
               float relativeValue = relativeAngle / rotationSpeed;
               if (relativeValue <= 0.0F && this.inUse <= 0) {
                  this.generatedSpeed = 0.0F;
                  this.updateGeneratedRotation();
               } else {
                  double degreesPerTick = (double)KineticBlockEntity.convertToAngular(rotationSpeed);
                  this.inUse = (int)Math.ceil((double)relativeAngle / degreesPerTick) + 2;
                  this.sequenceContext = new SequenceContext(SequencerInstructions.TURN_ANGLE, (double)relativeValue);
                  this.sequencedAngleLimit = (double)Math.abs(relativeAngle);
                  this.logicalSpeed = rotationSpeed;
                  Direction facing = (Direction)this.getBlockState().getValue(SteeringWheelBlock.FACING);
                  boolean floor = (Boolean)this.getBlockState().getValue(SteeringWheelBlock.ON_FLOOR);
                  if ((facing == Direction.NORTH || facing == Direction.WEST) == floor) {
                     this.generatedSpeed = -this.logicalSpeed;
                  } else {
                     this.generatedSpeed = this.logicalSpeed;
                  }

                  this.updateGeneratedRotation();
               }
            }
         }
      }
   }

   protected void copySequenceContextFrom(KineticBlockEntity sourceBE) {
   }

   public void tick() {
      super.tick();
      if (this.level.isClientSide) {
         this.oldClientAngle = this.clientAngle;
         if (SimClickInteractions.STEERING_WHEEL_MANAGER.isBlockActive(this.getBlockPos())) {
            this.clientAngle = this.targetAngleToUpdate;
         } else {
            this.clientAngle = this.clientAngle + (this.targetAngleToUpdate - this.clientAngle) * 0.25F;
         }
      }

      if (this.getGeneratedSpeed() != 0.0F) {
         this.integrateAngle();
      }

      if (this.inUse > 0) {
         this.inUse--;
         if (this.inUse == 0 && !this.level.isClientSide) {
            this.sequenceContext = null;
            this.generatedSpeed = 0.0F;
            this.updateGeneratedRotation();
         }
      } else if (!this.level.isClientSide) {
         this.updateTargetAngle(this.targetAngleToUpdate);
      }
   }

   private void integrateAngle() {
      float angularSpeed = this.getAngularSpeed();
      if (this.sequencedAngleLimit >= 0.0) {
         angularSpeed = (float)Mth.clamp((double)angularSpeed, -this.sequencedAngleLimit, this.sequencedAngleLimit);
         this.sequencedAngleLimit = Math.max(0.0, this.sequencedAngleLimit - (double)Math.abs(angularSpeed));
      }

      this.angle += angularSpeed;
   }

   public float getAngle() {
      return this.angle;
   }

   public float getAngularSpeed() {
      float speed = convertToAngular(this.getLogicalSpeed());
      if (this.getSpeed() == 0.0F || this.getLogicalSpeed() == 0.0F) {
         speed = 0.0F;
      }

      return speed;
   }

   public float getLogicalSpeed() {
      return this.inUse == 0 ? 0.0F : this.logicalSpeed;
   }

   public float getGeneratedSpeed() {
      return this.inUse == 0 ? 0.0F : this.generatedSpeed;
   }

   protected void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.write(compound, registries, clientPacket);
      compound.putFloat("Angle", this.angle);
      compound.putFloat("TargetAngle", this.targetAngle);
      if (this.targetAngleToUpdate != this.targetAngle) {
         compound.putFloat("TargetAngleToUpdate", this.targetAngleToUpdate);
      }

      if (clientPacket) {
         compound.putBoolean("Held", this.held);
      }

      compound.putInt("InUse", this.inUse);
      compound.putDouble("SequencedAngleLimit", this.sequencedAngleLimit);
      compound.putFloat("GeneratedSpeed", this.generatedSpeed);
      compound.put("Material", NbtUtils.writeBlockState(this.material));
   }

   public void writeSafe(CompoundTag compound, Provider registries) {
      super.writeSafe(compound, registries);
      compound.put("Material", NbtUtils.writeBlockState(this.material));
   }

   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      this.angle = compound.getFloat("Angle");
      if (clientPacket) {
         this.held = compound.getBoolean("Held");
      }

      if (!clientPacket || !SimClickInteractions.STEERING_WHEEL_MANAGER.isBlockActive(this.getBlockPos())) {
         this.targetAngle = compound.getFloat("TargetAngle");
         if (compound.contains("TargetAngleToUpdate")) {
            this.targetAngleToUpdate = compound.getFloat("TargetAngleToUpdate");
         } else {
            this.targetAngleToUpdate = this.targetAngle;
         }
      }

      this.inUse = compound.getInt("InUse");
      this.sequencedAngleLimit = compound.getDouble("SequencedAngleLimit");
      this.generatedSpeed = compound.getFloat("GeneratedSpeed");
      BlockState prevMaterial = this.material;
      if (compound.contains("Material")) {
         this.material = NbtUtils.readBlockState(this.blockHolderGetter(), compound.getCompound("Material"));
         if (this.material.isAir()) {
            this.material = Blocks.SPRUCE_PLANKS.defaultBlockState();
         }

         if (clientPacket && prevMaterial != this.material) {
            this.redraw();
         }
      }
   }

   public boolean shouldRenderShaft() {
      return true;
   }

   protected Block getStressConfigKey() {
      return (Block)SimBlocks.STEERING_WHEEL.get();
   }

   public float getRenderAngle(float partialTicks) {
      float renderAngle = Mth.lerp(partialTicks, this.oldClientAngle, this.clientAngle);
      Direction facing = (Direction)this.getBlockState().getValue(SteeringWheelBlock.FACING);
      return facing != Direction.NORTH && facing != Direction.WEST ? (float)Math.toRadians((double)renderAngle) : (float)Math.toRadians((double)(-renderAngle));
   }

   public float getInteractionAngle(float partialTicks) {
      return Mth.lerp(partialTicks, this.oldClientAngle, this.clientAngle);
   }

   public AABB createRenderBoundingBox() {
      return super.createRenderBoundingBox().inflate(0.4);
   }

   public void tickAudio() {
      super.tickAudio();
      float renderAngle = this.getRenderAngle(0.0F);
      if ((double)Math.abs(Math.abs(this.angle) - (float)this.angleInput.getValue()) < 0.01) {
         renderAngle += (float)((double)Math.signum(this.angle) * 0.01);
      }

      int playingIncrement = (int)Math.floor(Math.toDegrees((double)renderAngle) / 45.0);
      if (this.lastPlayedIncrement != playingIncrement) {
         int spokeCrossed = playingIncrement;
         if (this.lastPlayedIncrement - playingIncrement > 0) {
            spokeCrossed = playingIncrement + 1;
         }

         if ((float)spokeCrossed != Math.signum((float)(this.lastPlayedIncrement - playingIncrement)) * 4.0F) {
            switch (spokeCrossed) {
               case -4:
               case 4:
                  AllSoundEvents.CRANKING.playAt(this.level, this.worldPosition, 1.25F, 0.85F, true);
                  break;
               case 0:
                  AllSoundEvents.CRANKING.playAt(this.level, this.worldPosition, 1.25F, 0.5F, true);
                  break;
               default:
                  AllSoundEvents.CRANKING.playAt(this.level, this.worldPosition, 0.5F, 1.25F, true);
            }
         }

         this.lastPlayedIncrement = playingIncrement;
      }
   }

   private void redraw() {
      if (!this.isVirtual()) {
         this.requestModelDataUpdate();
      }

      if (this.hasLevel()) {
         this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 16);
         this.level.getChunkSource().getLightEngine().checkBlock(this.worldPosition);
      }
   }

   public boolean isMaterialValid(ItemStack stack) {
      if (stack.getItem() instanceof BlockItem blockItem) {
         BlockState material = blockItem.getBlock().defaultBlockState();
         return material == this.material ? false : material.is(BlockTags.PLANKS);
      } else {
         return false;
      }
   }

   public ItemInteractionResult applyMaterialIfValid(ItemStack stack) {
      if (!this.isMaterialValid(stack) || !(stack.getItem() instanceof BlockItem blockItem)) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else if (this.level.isClientSide() && !this.isVirtual()) {
         return ItemInteractionResult.SUCCESS;
      } else {
         this.material = blockItem.getBlock().defaultBlockState();
         this.notifyUpdate();
         this.level.levelEvent(2001, this.worldPosition, Block.getId(this.material));
         return ItemInteractionResult.SUCCESS;
      }
   }

   private static class SteeringWheelScrollValueBehaviour extends ScrollValueBehaviour {
      public SteeringWheelScrollValueBehaviour(SmartBlockEntity be) {
         super(SimLang.translate("torsion_spring.angle_limit").component(), be, new SteeringWheelBlockEntity.SteeringWheelValueBoxTransform());
         this.withFormatter(v -> Math.abs(v) + CreateLang.translateDirect("generic.unit.degrees", new Object[0]).getString());
      }

      public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
         return new ValueSettingsBoard(
            this.label, 360, 45, ImmutableList.of(Component.literal("⟳").withStyle(ChatFormatting.BOLD)), new ValueSettingsFormatter(this::formatValue)
         );
      }

      public MutableComponent formatValue(ValueSettings settings) {
         return SimLang.number((double)Math.max(1, Math.abs(settings.value())))
            .add(CreateLang.translateDirect("generic.unit.degrees", new Object[0]))
            .component();
      }
   }

   private static class SteeringWheelValueBoxTransform extends Sided {
      protected boolean isSideActive(BlockState state, Direction direction) {
         return direction == (state.getValue(SteeringWheelBlock.ON_FLOOR) ? Direction.UP : Direction.DOWN);
      }

      protected Vec3 getSouthLocation() {
         return VecHelper.voxelSpace(8.0, 8.0, 15.5);
      }

      public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
         super.rotate(level, pos, state, ms);
         Direction facing = (Direction)state.getValue(HorizontalDirectionalBlock.FACING);
         TransformStack.of(ms).rotateZDegrees(-AngleHelper.horizontalAngle(facing) + 180.0F);
      }
   }
}

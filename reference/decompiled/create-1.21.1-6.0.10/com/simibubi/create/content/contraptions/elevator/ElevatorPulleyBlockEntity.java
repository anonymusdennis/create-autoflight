package com.simibubi.create.content.contraptions.elevator;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.IControlContraption;
import com.simibubi.create.content.contraptions.pulley.PulleyBlockEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.util.List;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ElevatorPulleyBlockEntity extends PulleyBlockEntity {
   private float prevSpeed = 0.0F;
   private boolean arrived = true;
   private int clientOffsetTarget;
   private boolean initialOffsetReceived = false;

   public ElevatorPulleyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   private int getTargetOffset() {
      if (this.level.isClientSide) {
         return this.clientOffsetTarget;
      } else if (this.movedContraption != null && this.movedContraption.getContraption() instanceof ElevatorContraption ec) {
         Integer target = ec.getCurrentTargetY(this.level);
         return target == null ? (int)this.offset : this.worldPosition.getY() - target + ec.contactYOffset - 1;
      } else {
         return (int)this.offset;
      }
   }

   @Override
   public void attach(ControlledContraptionEntity contraption) {
      super.attach(contraption);
      if (this.offset >= 0.0F) {
         this.resetContraptionToOffset();
      }

      if (this.level.isClientSide) {
         CatnipServices.NETWORK.sendToServer(new ElevatorFloorListPacket.RequestFloorList(contraption));
      } else {
         if (contraption.getContraption() instanceof ElevatorContraption ec) {
            ElevatorColumn.getOrCreate(this.level, ec.getGlobalColumn()).setActive(true);
         }
      }
   }

   @Override
   public void tick() {
      boolean wasArrived = this.arrived;
      super.tick();
      if (this.movedContraption != null) {
         if (this.movedContraption.getContraption() instanceof ElevatorContraption ec) {
            if (this.level.isClientSide()) {
               ec.setClientYTarget(this.worldPosition.getY() - this.clientOffsetTarget + ec.contactYOffset - 1);
            }

            this.waitingForSpeedChange = false;
            ec.arrived = wasArrived;
            if (this.arrived) {
               double y = this.movedContraption.getY();
               int targetLevel = Mth.floor(0.5 + y) + ec.contactYOffset;
               Integer ecCurrentTargetY = ec.getCurrentTargetY(this.level);
               if (ecCurrentTargetY != null) {
                  targetLevel = ecCurrentTargetY;
               }

               if (this.level.isClientSide()) {
                  targetLevel = ec.clientYTarget;
               }

               if (!wasArrived && !this.level.isClientSide()) {
                  this.triggerContact(ec, targetLevel - ec.contactYOffset);
                  AllSoundEvents.CONTRAPTION_DISASSEMBLE.play(this.level, null, this.worldPosition.below((int)this.offset), 0.75F, 0.8F);
               }

               double diff = (double)targetLevel - y - (double)ec.contactYOffset;
               if (Math.abs(diff) > 0.0078125) {
                  diff *= 0.25;
               }

               this.movedContraption.setPos(this.movedContraption.position().add(0.0, diff, 0.0));
            }
         }
      }
   }

   @Override
   public void lazyTick() {
      super.lazyTick();
      if (!this.level.isClientSide() && this.arrived) {
         if (this.movedContraption != null && this.movedContraption.isAlive()) {
            if (this.movedContraption.getContraption() instanceof ElevatorContraption ec) {
               if (this.getTargetOffset() == (int)this.offset) {
                  double y = this.movedContraption.getY();
                  int targetLevel = Mth.floor(0.5 + y);
                  this.triggerContact(ec, targetLevel);
               }
            }
         }
      }
   }

   private void triggerContact(ElevatorContraption ec, int targetLevel) {
      ElevatorColumn.ColumnCoords coords = ec.getGlobalColumn();
      ElevatorColumn column = ElevatorColumn.get(this.level, coords);
      if (column != null) {
         BlockPos contactPos = column.contactAt(targetLevel + ec.contactYOffset);
         if (this.level.isLoaded(contactPos)) {
            BlockState contactState = this.level.getBlockState(contactPos);
            if (AllBlocks.ELEVATOR_CONTACT.has(contactState)) {
               if (!(Boolean)contactState.getValue(ElevatorContactBlock.POWERING)) {
                  ElevatorContactBlock ecb = (ElevatorContactBlock)AllBlocks.ELEVATOR_CONTACT.get();
                  ecb.withBlockEntityDo(this.level, contactPos, be -> be.activateBlock = true);
                  ecb.scheduleActivation(this.level, contactPos);
               }
            }
         }
      }
   }

   @Override
   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.write(compound, registries, clientPacket);
      if (clientPacket) {
         compound.putInt("ClientTarget", this.clientOffsetTarget);
      }
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      if (clientPacket) {
         this.clientOffsetTarget = compound.getInt("ClientTarget");
         if (!this.initialOffsetReceived) {
            this.offset = compound.getFloat("Offset");
            this.initialOffsetReceived = true;
            this.resetContraptionToOffset();
         }
      }
   }

   @Override
   public float getMovementSpeed() {
      int currentTarget = this.getTargetOffset();
      if (!this.level.isClientSide() && currentTarget != this.clientOffsetTarget) {
         this.clientOffsetTarget = currentTarget;
         this.sendData();
      }

      float diff = (float)currentTarget - this.offset;
      float movementSpeed = Mth.clamp(convertToLinear(this.getSpeed() * 2.0F), -1.99F, 1.99F);
      float rpmLimit = Math.abs(movementSpeed);
      float configacc = Mth.lerp(Math.abs(movementSpeed), 0.0075F, 0.0175F);
      float decelleration = (float)Math.sqrt((double)(2.0F * Math.abs(diff) * configacc));
      float speed = Mth.clamp(diff, -rpmLimit, rpmLimit);
      speed = Mth.clamp(speed, this.prevSpeed - configacc, this.prevSpeed + configacc);
      speed = Mth.clamp(speed, -decelleration, decelleration);
      this.arrived = Math.abs(diff) < 0.5F;
      if (speed > 9.765625E-4F && !this.level.isClientSide()) {
         this.setChanged();
      }

      return this.prevSpeed = speed;
   }

   @Override
   protected boolean shouldCreateRopes() {
      return false;
   }

   @Override
   public void disassemble() {
      if (this.movedContraption != null && this.movedContraption.getContraption() instanceof ElevatorContraption ec) {
         ElevatorColumn column = ElevatorColumn.get(this.level, ec.getGlobalColumn());
         if (column != null) {
            column.setActive(false);
         }
      }

      super.disassemble();
      this.offset = -1.0F;
      this.sendData();
   }

   public void clicked() {
      if (this.isPassive() && this.level.getBlockEntity(this.mirrorParent) instanceof ElevatorPulleyBlockEntity parent) {
         parent.clicked();
      } else {
         if (this.running) {
            this.disassemble();
         } else {
            this.assembleNextTick = true;
         }
      }
   }

   @Override
   protected boolean moveAndCollideContraption() {
      if (this.arrived) {
         return false;
      } else {
         super.moveAndCollideContraption();
         return false;
      }
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      this.registerAwardables(behaviours, new CreateAdvancement[]{AllAdvancements.CONTRAPTION_ACTORS});
   }

   @Override
   protected void assemble() throws AssemblyException {
      if (this.level.getBlockState(this.worldPosition).getBlock() instanceof ElevatorPulleyBlock) {
         if (this.getSpeed() != 0.0F) {
            int maxLength = (Integer)AllConfigs.server().kinetics.maxRopeLength.get();

            int i;
            for (i = 1; i <= maxLength; i++) {
               BlockPos ropePos = this.worldPosition.below(i);
               BlockState ropeState = this.level.getBlockState(ropePos);
               if (!ropeState.getCollisionShape(this.level, ropePos).isEmpty() && !ropeState.canBeReplaced()) {
                  break;
               }
            }

            this.offset = (float)(i - 1);
            this.forceMove = true;
            if (!this.level.isClientSide && this.mirrorParent == null) {
               this.needsContraption = false;
               BlockPos anchor = this.worldPosition.below(Mth.floor(this.offset + 1.0F));
               this.offset = (float)Mth.floor(this.offset);
               ElevatorContraption contraption = new ElevatorContraption((int)this.offset);
               float offsetOnSucess = this.offset;
               this.offset = 0.0F;
               boolean canAssembleStructure = contraption.assemble(this.level, anchor);
               if (!canAssembleStructure && this.getSpeed() > 0.0F) {
                  return;
               }

               if (!contraption.getBlocks().isEmpty()) {
                  this.offset = offsetOnSucess;
                  contraption.removeBlocksFromWorld(this.level, BlockPos.ZERO);
                  this.movedContraption = ControlledContraptionEntity.create(this.level, this, contraption);
                  this.movedContraption.setPos((double)anchor.getX(), (double)anchor.getY(), (double)anchor.getZ());
                  contraption.maxContactY = this.worldPosition.getY() + contraption.contactYOffset - 1;
                  contraption.minContactY = contraption.maxContactY - maxLength;
                  this.level.addFreshEntity(this.movedContraption);
                  this.forceMove = true;
                  this.needsContraption = true;
                  if (contraption.containsBlockBreakers()) {
                     this.award(AllAdvancements.CONTRAPTION_ACTORS);
                  }

                  for (BlockPos pos : contraption.createColliders(this.level, Direction.UP)) {
                     if (pos.getY() == 0) {
                        pos = pos.offset(anchor);
                        if (this.level.getBlockEntity(new BlockPos(pos.getX(), this.worldPosition.getY(), pos.getZ())) instanceof ElevatorPulleyBlockEntity pbe
                           )
                         {
                           pbe.startMirroringOther(this.worldPosition);
                        }
                     }
                  }

                  ElevatorColumn column = ElevatorColumn.getOrCreate(this.level, contraption.getGlobalColumn());
                  int target = (int)((float)(this.worldPosition.getY() + contraption.contactYOffset - 1) - this.offset);
                  column.target(target);
                  column.gatherAll();
                  column.setActive(true);
                  column.markDirty();
                  contraption.broadcastFloorData(this.level, column.contactAt(target));
                  this.clientOffsetTarget = column.getTargetedYLevel();
                  this.arrived = true;
               }
            }

            this.clientOffsetDiff = 0.0F;
            this.running = true;
            this.sendData();
         }
      }
   }

   @Override
   public void onSpeedChanged(float previousSpeed) {
      this.setChanged();
   }

   @Override
   protected IControlContraption.MovementMode getMovementMode() {
      return IControlContraption.MovementMode.MOVE_NEVER_PLACE;
   }
}

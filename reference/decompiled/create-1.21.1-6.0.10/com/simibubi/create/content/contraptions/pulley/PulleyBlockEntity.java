package com.simibubi.create.content.contraptions.pulley;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.piston.LinearActuatorBlockEntity;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class PulleyBlockEntity extends LinearActuatorBlockEntity implements ThresholdSwitchObservable {
   protected int initialOffset;
   private float prevAnimatedOffset;
   protected BlockPos mirrorParent;
   protected List<BlockPos> mirrorChildren;
   public WeakReference<AbstractContraptionEntity> sharedMirrorContraption;

   public PulleyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   protected AABB createRenderBoundingBox() {
      double expandY = (double)(-this.offset);
      if (this.sharedMirrorContraption != null) {
         AbstractContraptionEntity ace = this.sharedMirrorContraption.get();
         if (ace != null) {
            expandY = ace.getY() - (double)this.worldPosition.getY();
         }
      }

      return super.createRenderBoundingBox().expandTowards(0.0, expandY, 0.0);
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      super.addBehaviours(behaviours);
      this.registerAwardables(behaviours, new CreateAdvancement[]{AllAdvancements.PULLEY_MAXED});
   }

   @Override
   public void tick() {
      float prevOffset = this.offset;
      super.tick();
      if (this.level.isClientSide()
         && this.mirrorParent != null
         && (this.sharedMirrorContraption == null || this.sharedMirrorContraption.get() == null || !this.sharedMirrorContraption.get().isAlive())) {
         this.sharedMirrorContraption = null;
         if (this.level.getBlockEntity(this.mirrorParent) instanceof PulleyBlockEntity pte && pte.movedContraption != null) {
            this.sharedMirrorContraption = new WeakReference<>(pte.movedContraption);
         }
      }

      if (this.isVirtual()) {
         this.prevAnimatedOffset = this.offset;
      }

      this.invalidateRenderBoundingBox();
      if (prevOffset < 200.0F && this.offset >= 200.0F) {
         this.award(AllAdvancements.PULLEY_MAXED);
      }
   }

   @Override
   protected boolean isPassive() {
      return this.mirrorParent != null;
   }

   @Nullable
   public AbstractContraptionEntity getAttachedContraption() {
      return this.mirrorParent != null && this.sharedMirrorContraption != null ? this.sharedMirrorContraption.get() : this.movedContraption;
   }

   @Override
   protected void assemble() throws AssemblyException {
      if (this.level.getBlockState(this.worldPosition).getBlock() instanceof PulleyBlock) {
         if (this.speed != 0.0F || this.mirrorParent != null) {
            int maxLength = (Integer)AllConfigs.server().kinetics.maxRopeLength.get();

            int i;
            for (i = 1; i <= maxLength; i++) {
               BlockPos ropePos = this.worldPosition.below(i);
               BlockState ropeState = this.level.getBlockState(ropePos);
               if (!AllBlocks.ROPE.has(ropeState) && !AllBlocks.PULLEY_MAGNET.has(ropeState)) {
                  break;
               }
            }

            this.offset = (float)(i - 1);
            if (!(this.offset >= (float)this.getExtensionRange()) || !(this.getSpeed() > 0.0F)) {
               if (!(this.offset <= 0.0F) || !(this.getSpeed() < 0.0F)) {
                  if (!this.level.isClientSide && this.mirrorParent == null) {
                     this.needsContraption = false;
                     BlockPos anchor = this.worldPosition.below(Mth.floor(this.offset + 1.0F));
                     this.initialOffset = Mth.floor(this.offset);
                     PulleyContraption contraption = new PulleyContraption(this.initialOffset);
                     boolean canAssembleStructure = contraption.assemble(this.level, anchor);
                     if (canAssembleStructure) {
                        Direction movementDirection = this.getSpeed() > 0.0F ? Direction.DOWN : Direction.UP;
                        if (ContraptionCollider.isCollidingWithWorld(this.level, contraption, anchor.relative(movementDirection), movementDirection)) {
                           canAssembleStructure = false;
                        }
                     }

                     if (!canAssembleStructure && this.getSpeed() > 0.0F) {
                        return;
                     }

                     this.removeRopes();
                     if (!contraption.getBlocks().isEmpty()) {
                        contraption.removeBlocksFromWorld(this.level, BlockPos.ZERO);
                        this.movedContraption = ControlledContraptionEntity.create(this.level, this, contraption);
                        this.movedContraption.setPos((double)anchor.getX(), (double)anchor.getY(), (double)anchor.getZ());
                        this.level.addFreshEntity(this.movedContraption);
                        this.forceMove = true;
                        this.needsContraption = true;
                        if (contraption.containsBlockBreakers()) {
                           this.award(AllAdvancements.CONTRAPTION_ACTORS);
                        }

                        for (BlockPos pos : contraption.createColliders(this.level, Direction.UP)) {
                           if (pos.getY() == 0) {
                              pos = pos.offset(anchor);
                              if (this.level.getBlockEntity(new BlockPos(pos.getX(), this.worldPosition.getY(), pos.getZ())) instanceof PulleyBlockEntity pbe) {
                                 pbe.startMirroringOther(this.worldPosition);
                              }
                           }
                        }
                     }
                  }

                  if (this.mirrorParent != null) {
                     this.removeRopes();
                  }

                  this.clientOffsetDiff = 0.0F;
                  this.running = true;
                  this.sendData();
               }
            }
         }
      }
   }

   private void removeRopes() {
      for (int i = (int)this.offset; i > 0; i--) {
         BlockPos offset = this.worldPosition.below(i);
         BlockState oldState = this.level.getBlockState(offset);
         this.level.setBlock(offset, oldState.getFluidState().createLegacyBlock(), 66);
      }
   }

   @Override
   public void disassemble() {
      if (this.running || this.movedContraption != null || this.mirrorParent != null) {
         this.offset = (float)this.getGridOffset(this.offset);
         if (this.movedContraption != null) {
            this.resetContraptionToOffset();
         }

         if (!this.level.isClientSide) {
            if (this.shouldCreateRopes()) {
               if (this.offset > 0.0F) {
                  BlockPos magnetPos = this.worldPosition.below((int)this.offset);
                  FluidState ifluidstate = this.level.getFluidState(magnetPos);
                  if (this.level.getBlockState(magnetPos).getDestroySpeed(this.level, magnetPos) != -1.0F) {
                     this.level.destroyBlock(magnetPos, this.level.getBlockState(magnetPos).getCollisionShape(this.level, magnetPos).isEmpty());
                     this.level
                        .setBlock(
                           magnetPos,
                           (BlockState)AllBlocks.PULLEY_MAGNET
                              .getDefaultState()
                              .setValue(BlockStateProperties.WATERLOGGED, ifluidstate.getType() == Fluids.WATER),
                           66
                        );
                  }
               }

               boolean[] waterlog = new boolean[(int)this.offset];

               for (boolean destroyPass : Iterate.trueAndFalse) {
                  for (int i = 1; i <= (int)this.offset - 1; i++) {
                     BlockPos ropePos = this.worldPosition.below(i);
                     if (this.level.getBlockState(ropePos).getDestroySpeed(this.level, ropePos) != -1.0F) {
                        if (destroyPass) {
                           FluidState ifluidstate = this.level.getFluidState(ropePos);
                           waterlog[i] = ifluidstate.getType() == Fluids.WATER;
                           this.level.destroyBlock(ropePos, this.level.getBlockState(ropePos).getCollisionShape(this.level, ropePos).isEmpty());
                        } else {
                           this.level
                              .setBlock(
                                 this.worldPosition.below(i),
                                 (BlockState)AllBlocks.ROPE.getDefaultState().setValue(BlockStateProperties.WATERLOGGED, waterlog[i]),
                                 66
                              );
                        }
                     }
                  }
               }
            }

            if (this.movedContraption != null && this.mirrorParent == null) {
               this.movedContraption.disassemble();
            }

            this.notifyMirrorsOfDisassembly();
         }

         if (this.movedContraption != null) {
            this.movedContraption.discard();
         }

         this.movedContraption = null;
         this.initialOffset = 0;
         this.running = false;
         this.sendData();
      }
   }

   protected boolean shouldCreateRopes() {
      return !this.remove;
   }

   @Override
   protected Vec3 toPosition(float offset) {
      return this.movedContraption.getContraption() instanceof PulleyContraption contraption
         ? Vec3.atLowerCornerOf(contraption.anchor).add(0.0, (double)((float)contraption.getInitialOffset() - offset), 0.0)
         : Vec3.ZERO;
   }

   @Override
   protected void visitNewPosition() {
      super.visitNewPosition();
      if (!this.level.isClientSide) {
         if (this.movedContraption == null) {
            if (!(this.getSpeed() <= 0.0F)) {
               BlockPos posBelow = this.worldPosition.below((int)(this.offset + this.getMovementSpeed()) + 1);
               BlockState state = this.level.getBlockState(posBelow);
               if (BlockMovementChecks.isMovementNecessary(state, this.level, posBelow)) {
                  if (!BlockMovementChecks.isBrittle(state)) {
                     this.disassemble();
                     this.assembleNextTick = true;
                  }
               }
            }
         }
      }
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      this.initialOffset = compound.getInt("InitialOffset");
      this.needsContraption = compound.getBoolean("NeedsContraption");
      super.read(compound, registries, clientPacket);
      BlockPos prevMirrorParent = this.mirrorParent;
      this.mirrorParent = null;
      if (compound.contains("MirrorParent")) {
         this.mirrorParent = NBTHelper.readBlockPos(compound, "MirrorParent");
      }

      this.mirrorChildren = null;
      if (compound.contains("MirrorChildren")) {
         this.mirrorChildren = NBTHelper.readCompoundList(compound.getList("MirrorChildren", 10), t -> NBTHelper.readBlockPos(t, "Pos"));
      }

      if (this.mirrorParent != null) {
         this.offset = 0.0F;
         if (prevMirrorParent == null || !prevMirrorParent.equals(this.mirrorParent)) {
            this.sharedMirrorContraption = null;
         }
      }

      if (this.mirrorParent == null) {
         this.sharedMirrorContraption = null;
      }
   }

   @Override
   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      compound.putInt("InitialOffset", this.initialOffset);
      super.write(compound, registries, clientPacket);
      if (this.mirrorParent != null) {
         compound.put("MirrorParent", NbtUtils.writeBlockPos(this.mirrorParent));
      }

      if (this.mirrorChildren != null) {
         compound.put("MirrorChildren", NBTHelper.writeCompoundList(this.mirrorChildren, p -> {
            CompoundTag tag = new CompoundTag();
            tag.put("Pos", NbtUtils.writeBlockPos(p));
            return tag;
         }));
      }
   }

   public void startMirroringOther(BlockPos parent) {
      if (!parent.equals(this.worldPosition)) {
         if (this.level.getBlockEntity(parent) instanceof PulleyBlockEntity pbe) {
            if (pbe.getType() == this.getType()) {
               if (pbe.mirrorChildren == null) {
                  pbe.mirrorChildren = new ArrayList<>();
               }

               pbe.mirrorChildren.add(this.worldPosition);
               pbe.notifyUpdate();
               this.mirrorParent = parent;

               try {
                  this.assemble();
               } catch (AssemblyException var4) {
               }

               this.notifyUpdate();
            }
         }
      }
   }

   public void notifyMirrorsOfDisassembly() {
      if (this.mirrorChildren != null) {
         for (BlockPos blockPos : this.mirrorChildren) {
            if (this.level.getBlockEntity(blockPos) instanceof PulleyBlockEntity pbe) {
               pbe.offset = this.offset;
               pbe.disassemble();
               pbe.mirrorParent = null;
               pbe.notifyUpdate();
            }
         }

         this.mirrorChildren.clear();
         this.notifyUpdate();
      }
   }

   @Override
   protected int getExtensionRange() {
      return Math.max(0, Math.min((Integer)AllConfigs.server().kinetics.maxRopeLength.get(), this.worldPosition.getY() - 1 - this.level.getMinBuildHeight()));
   }

   @Override
   protected int getInitialOffset() {
      return this.initialOffset;
   }

   @Override
   protected Vec3 toMotionVector(float speed) {
      return new Vec3(0.0, (double)(-speed), 0.0);
   }

   @Override
   protected ValueBoxTransform getMovementModeSlot() {
      return new CenteredSideValueBoxTransform((state, d) -> d == Direction.UP);
   }

   @Override
   public float getInterpolatedOffset(float partialTicks) {
      if (this.isVirtual()) {
         return Mth.lerp(partialTicks, this.prevAnimatedOffset, this.offset);
      } else {
         boolean moving = this.running && (this.movedContraption == null || !this.movedContraption.isStalled());
         return super.getInterpolatedOffset(moving ? partialTicks : 0.5F);
      }
   }

   public void animateOffset(float forcedOffset) {
      this.offset = forcedOffset;
   }

   public BlockPos getMirrorParent() {
      return this.mirrorParent;
   }

   @Override
   public int getCurrentValue() {
      return this.worldPosition.getY() - (int)this.getInterpolatedOffset(0.5F);
   }

   @Override
   public int getMinValue() {
      return this.level.getMinBuildHeight();
   }

   @Override
   public int getMaxValue() {
      return this.worldPosition.getY();
   }

   @Override
   public MutableComponent format(int value) {
      return CreateLang.translateDirect("gui.threshold_switch.pulley_y_level", value);
   }
}

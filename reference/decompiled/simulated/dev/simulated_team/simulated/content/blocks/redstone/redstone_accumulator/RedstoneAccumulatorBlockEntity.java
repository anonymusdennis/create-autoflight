package dev.simulated_team.simulated.content.blocks.redstone.redstone_accumulator;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.content.redstone.diodes.BrassDiodeScrollValueBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour.StepContext;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.List;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class RedstoneAccumulatorBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, ClipboardCloneable {
   protected ScrollValueBehaviour inputDelay;
   protected int delayTicks;
   protected int outputSignal;
   protected LerpedFloat lerpedState = LerpedFloat.linear();

   public RedstoneAccumulatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.delayTicks = 0;
      this.outputSignal = 0;
   }

   public void initialize() {
      super.initialize();
      this.updateSignal();
   }

   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      this.inputDelay = new BrassDiodeScrollValueBehaviour(
         Component.translatable("block.simulated.redstone_accumulator.input_delay"),
         this,
         new RedstoneAccumulatorBlockEntity.RedstoneAccumulatorValueBoxTransform()
      );
      this.inputDelay.between(2, 72000);
      this.inputDelay.value = 10;
      this.inputDelay.withFormatter(this::format);
      this.inputDelay.withCallback(this::inputDelayChanged);
      behaviours.add(this.inputDelay);
   }

   public void tick() {
      super.tick();
      if (this.level != null) {
         Direction facing = (Direction)this.getBlockState().getValue(RedstoneAccumulatorBlock.FACING);
         boolean backSignal = (Boolean)this.getBlockState().getValue(RedstoneAccumulatorBlock.POWERED);
         boolean sideSignal = (Boolean)this.getBlockState().getValue(RedstoneAccumulatorBlock.SIDE_POWERED);
         if (!backSignal || !sideSignal) {
            if (!backSignal && !sideSignal) {
               this.delayTicks = 0;
            }

            int tempSignal = this.outputSignal;
            if (this.delayTicks == this.inputDelay.value) {
               if (backSignal) {
                  tempSignal++;
                  this.delayTicks = 0;
               } else if (sideSignal) {
                  tempSignal--;
                  this.delayTicks = 0;
               }

               if (tempSignal != this.outputSignal) {
                  this.setOutputSignal(tempSignal);
               }
            } else {
               this.delayTicks = Math.min(this.delayTicks + 1, this.inputDelay.value);
               this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
               this.level.updateNeighborsAt(this.worldPosition.relative(facing), this.getBlockState().getBlock());
            }

            if (this.level.isClientSide) {
               this.lerpedState.tickChaser();
            }

            this.lerpedState.chase((double)this.outputSignal, 0.4, Chaser.EXP);
         }
      }
   }

   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      this.outputSignal = tag.getInt("OutputSignal");
      this.delayTicks = tag.getInt("DelayTicks");
      this.lerpedState.chase((double)this.outputSignal, 0.4, Chaser.EXP);
      super.read(tag, registries, clientPacket);
   }

   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      tag.putInt("OutputSignal", this.outputSignal);
      tag.putInt("DelayTicks", this.delayTicks);
      super.write(tag, registries, clientPacket);
   }

   private void inputDelayChanged(Integer integer) {
      this.sendData();
   }

   public void updateSignal() {
      this.sendData();
   }

   public void setOutputSignal(int output) {
      boolean update = output != this.outputSignal;
      this.outputSignal = Mth.clamp(output, 0, 15);
      if (update) {
         this.updateFacingBlock(this.getBlockState().getBlock(), this.level);
      }
   }

   private void updateFacingBlock(Block block, Level levelIn) {
      levelIn.updateNeighborsAt(this.worldPosition, block);
      levelIn.updateNeighborsAt(this.worldPosition.relative(((Direction)this.getBlockState().getValue(RedstoneAccumulatorBlock.FACING)).getOpposite()), block);
   }

   private int step(StepContext context) {
      int value = context.currentValue;
      if (!context.forward) {
         value--;
      }

      if (value < 20) {
         return 1;
      } else {
         return value < 1200 ? 20 : 1200;
      }
   }

   private String format(int value) {
      if (value < 20) {
         return value + "t";
      } else {
         return value < 1200 ? value / 20 + "s" : value / 20 / 60 + "m";
      }
   }

   public String getClipboardKey() {
      return "Block";
   }

   public boolean readFromClipboard(@NotNull Provider provider, CompoundTag tag, Player player, Direction direction, boolean simulate) {
      if (!tag.contains("Inverted")) {
         return false;
      } else if (simulate) {
         return true;
      } else {
         BlockState blockState = this.getBlockState();
         if ((Boolean)blockState.getValue(RedstoneAccumulatorBlock.INVERTED) != tag.getBoolean("Inverted")) {
            this.level.setBlockAndUpdate(this.worldPosition, (BlockState)blockState.cycle(RedstoneAccumulatorBlock.INVERTED));
         }

         return true;
      }
   }

   public boolean writeToClipboard(@NotNull Provider provider, CompoundTag tag, Direction direction) {
      tag.putBoolean("Inverted", this.getBlockState().getOptionalValue(RedstoneAccumulatorBlock.INVERTED).orElse(false));
      return true;
   }

   private static class RedstoneAccumulatorValueBoxTransform extends ValueBoxTransform {
      public Vec3 getLocalOffset(LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState) {
         return new Vec3(0.5, 0.4125F, 0.5);
      }

      public void rotate(LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState, PoseStack poseStack) {
         float yRot = AngleHelper.horizontalAngle((Direction)blockState.getValue(RedstoneAccumulatorBlock.FACING)) + 180.0F;
         ((PoseTransformStack)TransformStack.of(poseStack).rotateYDegrees(yRot)).rotateXDegrees(90.0F);
      }
   }
}

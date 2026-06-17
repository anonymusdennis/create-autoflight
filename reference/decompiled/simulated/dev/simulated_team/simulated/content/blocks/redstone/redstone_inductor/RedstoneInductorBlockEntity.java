package dev.simulated_team.simulated.content.blocks.redstone.redstone_inductor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class RedstoneInductorBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, ClipboardCloneable {
   protected ScrollValueBehaviour inputDelay;
   int delayTicks;
   int outputSignal;
   LerpedFloat lerpedState = LerpedFloat.linear();

   public RedstoneInductorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.delayTicks = 0;
      this.outputSignal = 0;
   }

   public void initialize() {
      super.initialize();
      this.updateSignal();
   }

   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      this.inputDelay = new RedstoneInductorValueBehaviour(
         CreateLang.translateDirect("logistics.redstone_interval", new Object[0]), this, new RedstoneInductorBlockEntity.RedstoneInductorValueBoxTransform()
      );
      this.inputDelay.between(0, 72000);
      this.inputDelay.value = 10;
      this.inputDelay.withFormatter(this::format);
      this.inputDelay.withCallback(this::inputDelayChanged);
      behaviours.add(this.inputDelay);
   }

   private void inputDelayChanged(Integer integer) {
      this.updateSignal();
   }

   public void updateSignal() {
      this.updateFacingBlock((RedstoneInductorBlock)this.getBlockState().getBlock(), this.getLevel());
      this.notifyUpdate();
   }

   public void tick() {
      super.tick();
      if (this.level != null) {
         RedstoneInductorBlock block = (RedstoneInductorBlock)this.getBlockState().getBlock();
         int backSignal = block.getBackSignal(this.level, this.worldPosition, this.getBlockState());
         int tempPower = this.outputSignal;
         boolean powered = (Boolean)this.getBlockState().getValue(RedstoneInductorBlock.POWERED);
         if (!this.level.isClientSide) {
            if (tempPower == 0 && !powered) {
               this.delayTicks = 0;
            }

            if (this.inputDelay.getValue() != 0 && this.delayTicks >= this.inputDelay.getValue()) {
               this.delayTicks = 0;
               if (tempPower > backSignal) {
                  tempPower--;
               }

               if (tempPower < backSignal) {
                  tempPower++;
               }
            } else if (this.inputDelay.getValue() == 0 && this.delayTicks > 2) {
               this.delayTicks = 0;
               tempPower = backSignal;
            }

            this.delayTicks++;
            if (this.outputSignal != tempPower) {
               this.outputSignal = tempPower;
               this.updateFacingBlock(block, this.level);
               this.sendData();
            }
         }

         if (this.level.isClientSide) {
            this.lerpedState.tickChaser();
         }
      }
   }

   private void updateFacingBlock(RedstoneInductorBlock block, Level levelIn) {
      levelIn.updateNeighborsAt(this.worldPosition, block);
      levelIn.updateNeighborsAt(this.worldPosition.relative(((Direction)this.getBlockState().getValue(RedstoneInductorBlock.FACING)).getOpposite()), block);
   }

   private String format(int value) {
      if (value == 0) {
         return Component.translatable(
               "block.simulated.redstone_inductor." + (this.getBlockState().getValue(RedstoneInductorBlock.INVERTED) ? "invert" : "copy")
            )
            .getString();
      } else if (value <= 60) {
         return value + "t";
      } else {
         return value < 1200 ? value / 20 + "s" : value / 20 / 60 + "m";
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
         if ((Boolean)blockState.getValue(RedstoneInductorBlock.INVERTED) != tag.getBoolean("Inverted")) {
            this.level.setBlockAndUpdate(this.worldPosition, (BlockState)blockState.cycle(RedstoneInductorBlock.INVERTED));
         }

         return true;
      }
   }

   public boolean writeToClipboard(@NotNull Provider provider, CompoundTag tag, Direction direction) {
      tag.putBoolean("Inverted", this.getBlockState().getOptionalValue(RedstoneInductorBlock.INVERTED).orElse(false));
      return true;
   }

   private static class RedstoneInductorValueBoxTransform extends ValueBoxTransform {
      public Vec3 getLocalOffset(LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState) {
         return new Vec3(0.5, 0.34375, 0.5);
      }

      public void rotate(LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState, PoseStack poseStack) {
         float yRot = AngleHelper.horizontalAngle((Direction)blockState.getValue(BlockStateProperties.HORIZONTAL_FACING)) + 180.0F;
         ((PoseTransformStack)TransformStack.of(poseStack).rotateYDegrees(yRot)).rotateXDegrees(90.0F);
      }

      public float getScale() {
         return 0.5F;
      }
   }
}

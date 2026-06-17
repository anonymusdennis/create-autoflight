package com.simibubi.create.content.contraptions.actors.contraptionControls;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.utility.DyeHelper;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.List;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ContraptionControlsBlockEntity extends SmartBlockEntity implements Clearable {
   public FilteringBehaviour filtering;
   public boolean disabled;
   public boolean powered;
   public LerpedFloat indicator = LerpedFloat.angular().startWithValue(0.0);
   public LerpedFloat button = LerpedFloat.linear().startWithValue(0.0).chase(0.0, 0.125, Chaser.EXP);

   public ContraptionControlsBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(this.filtering = new FilteringBehaviour(this, new ContraptionControlsBlockEntity.ControlsSlot()));
      this.filtering.setLabel(CreateLang.translateDirect("contraptions.contoller.target"));
      this.filtering.withPredicate(AllTags.AllItemTags.CONTRAPTION_CONTROLLED::matches);
   }

   public void pressButton() {
      this.button.setValue(1.0);
   }

   public void updatePoweredState() {
      if (!this.level.isClientSide()) {
         boolean powered = this.level.hasNeighborSignal(this.worldPosition);
         if (this.powered != powered) {
            this.powered = powered;
            this.disabled = powered;
            this.notifyUpdate();
         }
      }
   }

   @Override
   public void initialize() {
      super.initialize();
      this.updatePoweredState();
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level.isClientSide()) {
         this.tickAnimations();
         int value = this.disabled ? 180 : 0;
         this.indicator.setValue((double)value);
         this.indicator.updateChaseTarget((float)value);
      }
   }

   public void clearContent() {
      this.filtering.setFilter(ItemStack.EMPTY);
   }

   public void tickAnimations() {
      this.button.tickChaser();
      this.indicator.tickChaser();
   }

   @Override
   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.read(tag, registries, clientPacket);
      this.disabled = tag.getBoolean("Disabled");
      this.powered = tag.getBoolean("Powered");
   }

   @Override
   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.write(tag, registries, clientPacket);
      tag.putBoolean("Disabled", this.disabled);
      tag.putBoolean("Powered", this.powered);
   }

   public static void sendStatus(Player player, ItemStack filter, boolean enabled) {
      MutableComponent state = CreateLang.translate("contraption.controls.actor_toggle." + (enabled ? "on" : "off"))
         .color((Integer)DyeHelper.getDyeColors(enabled ? DyeColor.LIME : DyeColor.ORANGE).getFirst())
         .component();
      if (filter.isEmpty()) {
         CreateLang.translate("contraption.controls.all_actor_toggle", state).sendStatus(player);
      } else {
         CreateLang.translate("contraption.controls.specific_actor_toggle", filter.getHoverName().getString(), state).sendStatus(player);
      }
   }

   public static class ControlsSlot extends ValueBoxTransform.Sided {
      @Override
      public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
         Direction facing = (Direction)state.getValue(ControlsBlock.FACING);
         float yRot = AngleHelper.horizontalAngle(facing);
         return VecHelper.rotateCentered(VecHelper.voxelSpace(8.0, 14.0, 5.5), (double)yRot, Axis.Y);
      }

      @Override
      public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
         Direction facing = (Direction)state.getValue(ControlsBlock.FACING);
         float yRot = AngleHelper.horizontalAngle(facing);
         ((PoseTransformStack)TransformStack.of(ms).rotateYDegrees(yRot + 180.0F)).rotateXDegrees(67.5F);
      }

      @Override
      public float getScale() {
         return 0.508F;
      }

      @Override
      protected Vec3 getSouthLocation() {
         return Vec3.ZERO;
      }
   }
}

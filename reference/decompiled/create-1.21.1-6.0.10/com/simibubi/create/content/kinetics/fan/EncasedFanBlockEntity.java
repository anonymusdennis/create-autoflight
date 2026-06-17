package com.simibubi.create.content.kinetics.fan;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.logistics.chute.ChuteBlockEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.util.List;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

@MethodsReturnNonnullByDefault
public class EncasedFanBlockEntity extends KineticBlockEntity implements IAirCurrentSource {
   public AirCurrent airCurrent = new AirCurrent(this);
   protected int airCurrentUpdateCooldown;
   protected int entitySearchCooldown;
   protected boolean updateAirFlow = true;

   public EncasedFanBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      super.addBehaviours(behaviours);
      this.registerAwardables(behaviours, new CreateAdvancement[]{AllAdvancements.ENCASED_FAN, AllAdvancements.FAN_PROCESSING});
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      if (clientPacket) {
         this.airCurrent.rebuild();
      }
   }

   @Override
   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.write(compound, registries, clientPacket);
   }

   @Override
   public AirCurrent getAirCurrent() {
      return this.airCurrent;
   }

   @Nullable
   @Override
   public Level getAirCurrentWorld() {
      return this.level;
   }

   @Override
   public BlockPos getAirCurrentPos() {
      return this.worldPosition;
   }

   @Override
   public Direction getAirflowOriginSide() {
      return (Direction)this.getBlockState().getValue(EncasedFanBlock.FACING);
   }

   @Override
   public Direction getAirFlowDirection() {
      float speed = this.getSpeed();
      if (speed == 0.0F) {
         return null;
      } else {
         Direction facing = (Direction)this.getBlockState().getValue(BlockStateProperties.FACING);
         speed = convertToDirection(speed, facing);
         return speed > 0.0F ? facing : facing.getOpposite();
      }
   }

   @Override
   public void remove() {
      super.remove();
      this.updateChute();
   }

   @Override
   public boolean isSourceRemoved() {
      return this.remove;
   }

   @Override
   public void onSpeedChanged(float prevSpeed) {
      super.onSpeedChanged(prevSpeed);
      this.updateAirFlow = true;
      this.updateChute();
   }

   public void updateChute() {
      Direction direction = (Direction)this.getBlockState().getValue(EncasedFanBlock.FACING);
      if (direction.getAxis().isVertical()) {
         if (this.level.getBlockEntity(this.worldPosition.relative(direction)) instanceof ChuteBlockEntity chuteBE) {
            if (direction == Direction.DOWN) {
               chuteBE.updatePull();
            } else {
               chuteBE.updatePush(1);
            }
         }
      }
   }

   public void blockInFrontChanged() {
      this.updateAirFlow = true;
   }

   @Override
   public void tick() {
      super.tick();
      boolean server = !this.level.isClientSide || this.isVirtual();
      if (server && this.airCurrentUpdateCooldown-- <= 0) {
         this.airCurrentUpdateCooldown = (Integer)AllConfigs.server().kinetics.fanBlockCheckRate.get();
         this.updateAirFlow = true;
      }

      if (this.updateAirFlow) {
         this.updateAirFlow = false;
         this.airCurrent.rebuild();
         if (this.airCurrent.maxDistance > 0.0F) {
            this.award(AllAdvancements.ENCASED_FAN);
         }

         this.sendData();
      }

      if (this.getSpeed() != 0.0F) {
         if (this.entitySearchCooldown-- <= 0) {
            this.entitySearchCooldown = 5;
            this.airCurrent.findEntities();
         }

         this.airCurrent.tick();
      }
   }
}

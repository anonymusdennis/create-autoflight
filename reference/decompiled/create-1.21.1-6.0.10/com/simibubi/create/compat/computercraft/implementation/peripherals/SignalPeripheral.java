package com.simibubi.create.compat.computercraft.implementation.peripherals;

import com.simibubi.create.Create;
import com.simibubi.create.compat.computercraft.events.ComputerEvent;
import com.simibubi.create.compat.computercraft.events.SignalStateChangeEvent;
import com.simibubi.create.compat.computercraft.implementation.CreateLuaTable;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.signal.SignalBlock;
import com.simibubi.create.content.trains.signal.SignalBlockEntity;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.content.trains.signal.SignalEdgeGroup;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.Map;
import java.util.UUID;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class SignalPeripheral extends SyncedPeripheral<SignalBlockEntity> {
   public SignalPeripheral(SignalBlockEntity blockEntity) {
      super(blockEntity);
   }

   @LuaFunction
   public final String getState() {
      return this.blockEntity.getState().toString();
   }

   @LuaFunction
   public final boolean isForcedRed() {
      return (Boolean)this.blockEntity.getBlockState().getValue(SignalBlock.POWERED);
   }

   @LuaFunction(
      mainThread = true
   )
   public final void setForcedRed(boolean powered) {
      Level level = this.blockEntity.getLevel();
      if (level != null) {
         level.setBlock(this.blockEntity.getBlockPos(), (BlockState)this.blockEntity.getBlockState().setValue(SignalBlock.POWERED, powered), 2);
      }
   }

   @LuaFunction
   public final CreateLuaTable listBlockingTrainNames() throws LuaException {
      SignalBoundary signal = this.blockEntity.getSignal();
      if (signal == null) {
         throw new LuaException("no signal");
      } else {
         CreateLuaTable trainList = new CreateLuaTable();
         int trainCounter = 1;

         for (boolean current : Iterate.trueAndFalse) {
            Map<BlockPos, Boolean> set = (Map<BlockPos, Boolean>)signal.blockEntities.get(current);
            if (set.containsKey(this.blockEntity.getBlockPos())) {
               UUID group = (UUID)signal.groups.get(current);
               Map<UUID, SignalEdgeGroup> signalEdgeGroups = Create.RAILWAYS.signalEdgeGroups;
               SignalEdgeGroup signalEdgeGroup = signalEdgeGroups.get(group);

               for (Train train : signalEdgeGroup.trains) {
                  trainList.put(trainCounter, train.name.getString());
                  trainCounter++;
               }
            }
         }

         return trainList;
      }
   }

   @LuaFunction
   public final String getSignalType() throws LuaException {
      SignalBoundary signal = this.blockEntity.getSignal();
      if (signal != null) {
         return signal.getTypeFor(this.blockEntity.getBlockPos()).toString();
      } else {
         throw new LuaException("no signal");
      }
   }

   @LuaFunction(
      mainThread = true
   )
   public final void cycleSignalType() throws LuaException {
      SignalBoundary signal = this.blockEntity.getSignal();
      if (signal != null) {
         signal.cycleSignalType(this.blockEntity.getBlockPos());
      } else {
         throw new LuaException("no signal");
      }
   }

   @Override
   public void prepareComputerEvent(@NotNull ComputerEvent event) {
      if (event instanceof SignalStateChangeEvent ssce) {
         this.queueEvent("train_signal_state_change", new Object[]{ssce.state.toString()});
      }
   }

   @NotNull
   public String getType() {
      return "Create_Signal";
   }
}

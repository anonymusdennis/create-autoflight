package com.simibubi.create.compat.computercraft.implementation.peripherals;

import com.simibubi.create.compat.computercraft.AttachedComputerPacket;
import com.simibubi.create.compat.computercraft.events.ComputerEvent;
import com.simibubi.create.compat.computercraft.implementation.ComputerBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.platform.CatnipServices;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class SyncedPeripheral<T extends SmartBlockEntity> implements IPeripheral {
   protected final T blockEntity;
   private final List<IComputerAccess> computers = new ArrayList<>();

   public SyncedPeripheral(T blockEntity) {
      this.blockEntity = blockEntity;
   }

   public void attach(@NotNull IComputerAccess computer) {
      synchronized (this.computers) {
         this.computers.add(computer);
         if (this.computers.size() == 1) {
            this.onFirstAttach();
         }

         this.updateBlockEntity();
      }
   }

   protected void onFirstAttach() {
   }

   public void detach(@NotNull IComputerAccess computer) {
      synchronized (this.computers) {
         this.computers.remove(computer);
         this.updateBlockEntity();
         if (this.computers.isEmpty()) {
            this.onLastDetach();
         }
      }
   }

   protected void onLastDetach() {
   }

   private void updateBlockEntity() {
      boolean hasAttachedComputer = !this.computers.isEmpty();
      this.blockEntity.getBehaviour(ComputerBehaviour.TYPE).setHasAttachedComputer(hasAttachedComputer);
      CatnipServices.NETWORK.sendToAllClients(new AttachedComputerPacket(this.blockEntity.getBlockPos(), hasAttachedComputer));
   }

   public boolean equals(@Nullable IPeripheral other) {
      return this == other;
   }

   public void prepareComputerEvent(@NotNull ComputerEvent event) {
   }

   protected void queueEvent(@NotNull String event, @Nullable Object... arguments) {
      Object[] sourceAndArgs = new Object[arguments.length + 1];
      System.arraycopy(arguments, 0, sourceAndArgs, 1, arguments.length);
      synchronized (this.computers) {
         for (IComputerAccess computer : this.computers) {
            sourceAndArgs[0] = computer.getAttachmentName();
            computer.queueEvent(event, sourceAndArgs);
         }
      }
   }
}

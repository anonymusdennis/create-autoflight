package com.simibubi.create.compat.computercraft.implementation;

import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.events.ComputerEvent;
import com.simibubi.create.compat.computercraft.implementation.luaObjects.PackageLuaObject;
import com.simibubi.create.compat.computercraft.implementation.peripherals.CreativeMotorPeripheral;
import com.simibubi.create.compat.computercraft.implementation.peripherals.DisplayLinkPeripheral;
import com.simibubi.create.compat.computercraft.implementation.peripherals.FrogportPeripheral;
import com.simibubi.create.compat.computercraft.implementation.peripherals.NixieTubePeripheral;
import com.simibubi.create.compat.computercraft.implementation.peripherals.PackagerPeripheral;
import com.simibubi.create.compat.computercraft.implementation.peripherals.PostboxPeripheral;
import com.simibubi.create.compat.computercraft.implementation.peripherals.RedstoneRequesterPeripheral;
import com.simibubi.create.compat.computercraft.implementation.peripherals.RepackagerPeripheral;
import com.simibubi.create.compat.computercraft.implementation.peripherals.SequencedGearshiftPeripheral;
import com.simibubi.create.compat.computercraft.implementation.peripherals.SignalPeripheral;
import com.simibubi.create.compat.computercraft.implementation.peripherals.SpeedControllerPeripheral;
import com.simibubi.create.compat.computercraft.implementation.peripherals.SpeedGaugePeripheral;
import com.simibubi.create.compat.computercraft.implementation.peripherals.StationPeripheral;
import com.simibubi.create.compat.computercraft.implementation.peripherals.StickerPeripheral;
import com.simibubi.create.compat.computercraft.implementation.peripherals.StockTickerPeripheral;
import com.simibubi.create.compat.computercraft.implementation.peripherals.StressGaugePeripheral;
import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;
import com.simibubi.create.compat.computercraft.implementation.peripherals.TableClothShopPeripheral;
import com.simibubi.create.compat.computercraft.implementation.peripherals.TrackObserverPeripheral;
import com.simibubi.create.content.contraptions.chassis.StickerBlockEntity;
import com.simibubi.create.content.kinetics.gauge.SpeedGaugeBlockEntity;
import com.simibubi.create.content.kinetics.gauge.StressGaugeBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlockEntity;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import com.simibubi.create.content.logistics.packagePort.postbox.PostboxBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.repackager.RepackagerBlockEntity;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.nixieTube.NixieTubeBlockEntity;
import com.simibubi.create.content.trains.observer.TrackObserverBlockEntity;
import com.simibubi.create.content.trains.signal.SignalBlockEntity;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.peripheral.IPeripheral;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.NotNull;

public class ComputerBehaviour extends AbstractComputerBehaviour {
   SyncedPeripheral<?> peripheral;
   Supplier<SyncedPeripheral<?>> peripheralSupplier;
   SmartBlockEntity be;

   public ComputerBehaviour(SmartBlockEntity be) {
      super(be);
      this.peripheralSupplier = getPeripheralFor(be);
      this.be = be;
   }

   public static Supplier<SyncedPeripheral<?>> getPeripheralFor(SmartBlockEntity be) {
      if (be instanceof SpeedControllerBlockEntity scbe) {
         return () -> new SpeedControllerPeripheral(scbe, scbe.targetSpeed);
      } else if (be instanceof CreativeMotorBlockEntity cmbe) {
         return () -> new CreativeMotorPeripheral(cmbe, cmbe.generatedSpeed);
      } else if (be instanceof DisplayLinkBlockEntity dlbe) {
         return () -> new DisplayLinkPeripheral(dlbe);
      } else if (be instanceof FrogportBlockEntity fpbe) {
         return () -> new FrogportPeripheral(fpbe);
      } else if (be instanceof PostboxBlockEntity pbbe) {
         return () -> new PostboxPeripheral(pbbe);
      } else if (be instanceof NixieTubeBlockEntity ntbe) {
         return () -> new NixieTubePeripheral(ntbe);
      } else if (be instanceof SequencedGearshiftBlockEntity sgbe) {
         return () -> new SequencedGearshiftPeripheral(sgbe);
      } else if (be instanceof SignalBlockEntity sbe) {
         return () -> new SignalPeripheral(sbe);
      } else if (be instanceof SpeedGaugeBlockEntity sgbe) {
         return () -> new SpeedGaugePeripheral(sgbe);
      } else if (be instanceof StressGaugeBlockEntity sgbe) {
         return () -> new StressGaugePeripheral(sgbe);
      } else if (be instanceof StockTickerBlockEntity sgbe) {
         return () -> new StockTickerPeripheral(sgbe);
      } else if (be instanceof RepackagerBlockEntity rpbe) {
         return () -> new RepackagerPeripheral(rpbe);
      } else if (be instanceof PackagerBlockEntity pgbe) {
         return () -> new PackagerPeripheral(pgbe);
      } else if (be instanceof RedstoneRequesterBlockEntity rrbe) {
         return () -> new RedstoneRequesterPeripheral(rrbe);
      } else if (be instanceof StationBlockEntity sbe) {
         return () -> new StationPeripheral(sbe);
      } else if (be instanceof TableClothBlockEntity tcbe) {
         return () -> new TableClothShopPeripheral(tcbe);
      } else if (be instanceof StickerBlockEntity sbe) {
         return () -> new StickerPeripheral(sbe);
      } else if (be instanceof StationBlockEntity sbe) {
         return () -> new StationPeripheral(sbe);
      } else if (be instanceof TrackObserverBlockEntity tobe) {
         return () -> new TrackObserverPeripheral(tobe);
      } else {
         throw new IllegalArgumentException("No peripheral available for " + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()));
      }
   }

   public static void registerItemDetailProviders() {
      VanillaDetailRegistries.ITEM_STACK.addProvider((out, stack) -> {
         if (PackageItem.isPackage(stack)) {
            PackageLuaObject packageLuaObject = new PackageLuaObject(null, stack);
            out.put("package", packageLuaObject);
         }
      });
   }

   @Override
   public IPeripheral getPeripheralCapability() {
      if (this.peripheral == null) {
         this.peripheral = this.peripheralSupplier.get();
      }

      return this.peripheral;
   }

   @Override
   public void removePeripheral() {
      if (this.peripheral != null) {
         this.getWorld().invalidateCapabilities(this.be.getBlockPos());
      }
   }

   @Override
   public void prepareComputerEvent(@NotNull ComputerEvent event) {
      if (this.peripheral != null) {
         this.peripheral.prepareComputerEvent(event);
      }
   }
}

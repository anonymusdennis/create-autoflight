package com.simibubi.create.content.fluids;

import com.simibubi.create.content.contraptions.actors.psi.PortableFluidInterfaceBlockEntity;
import com.simibubi.create.foundation.ICapabilityProvider;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.fluid.FluidHelper;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.Nullable;

public class FluidNetwork {
   private static final int CYCLES_PER_TICK = 16;
   Level world;
   BlockFace start;
   Supplier<ICapabilityProvider<IFluidHandler>> sourceSupplier;
   @Nullable
   ICapabilityProvider<IFluidHandler> source = null;
   int transferSpeed;
   int pauseBeforePropagation;
   List<BlockFace> queued;
   Set<Pair<BlockFace, PipeConnection>> frontier;
   Set<BlockPos> visited;
   FluidStack fluid;
   List<Pair<BlockFace, FlowSource>> targets;
   Map<BlockPos, WeakReference<FluidTransportBehaviour>> cache;

   public FluidNetwork(Level world, BlockFace location, Supplier<ICapabilityProvider<IFluidHandler>> sourceSupplier) {
      this.world = world;
      this.start = location;
      this.sourceSupplier = sourceSupplier;
      this.fluid = FluidStack.EMPTY;
      this.frontier = new HashSet<>();
      this.visited = new HashSet<>();
      this.targets = new ArrayList<>();
      this.cache = new HashMap<>();
      this.queued = new ArrayList<>();
      this.reset();
   }

   public void tick() {
      if (this.pauseBeforePropagation > 0) {
         this.pauseBeforePropagation--;
      } else {
         for (int cycle = 0; cycle < 16; cycle++) {
            boolean shouldContinue = false;
            Iterator<BlockFace> iterator = this.queued.iterator();

            while (iterator.hasNext()) {
               BlockFace blockFace = iterator.next();
               if (this.isPresent(blockFace)) {
                  PipeConnection pipeConnection = this.get(blockFace);
                  if (pipeConnection != null) {
                     if (blockFace.equals(this.start)) {
                        this.transferSpeed = (int)Math.max(1.0F, (Float)pipeConnection.pressure.get(true) / 2.0F);
                     }

                     this.frontier.add(Pair.of(blockFace, pipeConnection));
                  }

                  iterator.remove();
               }
            }

            iterator = this.frontier.iterator();

            while (iterator.hasNext()) {
               Pair<BlockFace, PipeConnection> pair = (Pair<BlockFace, PipeConnection>)iterator.next();
               BlockFace blockFace = (BlockFace)pair.getFirst();
               PipeConnection pipeConnection = (PipeConnection)pair.getSecond();
               if (pipeConnection.hasFlow()) {
                  PipeConnection.Flow flow = pipeConnection.flow.get();
                  if (!this.fluid.isEmpty() && !FluidStack.isSameFluidSameComponents(flow.fluid, this.fluid)) {
                     iterator.remove();
                  } else if (!flow.inbound) {
                     if (pipeConnection.comparePressure() >= 0.0F) {
                        iterator.remove();
                     }
                  } else if (flow.complete) {
                     if (this.fluid.isEmpty()) {
                        this.fluid = flow.fluid;
                     }

                     boolean canRemove = true;

                     for (Direction side : Iterate.directions) {
                        if (side != blockFace.getFace()) {
                           BlockFace adjacentLocation = new BlockFace(blockFace.getPos(), side);
                           PipeConnection adjacent = this.get(adjacentLocation);
                           if (adjacent != null) {
                              if (!adjacent.hasFlow()) {
                                 if (adjacent.hasPressure() && (Float)adjacent.pressure.getSecond() > 0.0F) {
                                    canRemove = false;
                                 }
                              } else {
                                 PipeConnection.Flow outFlow = adjacent.flow.get();
                                 if (outFlow.inbound) {
                                    if (adjacent.comparePressure() > 0.0F) {
                                       canRemove = false;
                                    }
                                 } else if (!outFlow.complete) {
                                    canRemove = false;
                                 } else if (!adjacent.source.isPresent() && !adjacent.determineSource(this.world, blockFace.getPos())) {
                                    canRemove = false;
                                 } else if (adjacent.source.isPresent() && adjacent.source.get().isEndpoint()) {
                                    this.targets.add(Pair.of(adjacentLocation, adjacent.source.get()));
                                 } else if (this.visited.add(adjacentLocation.getConnectedPos())) {
                                    this.queued.add(adjacentLocation.getOpposite());
                                    shouldContinue = true;
                                 }
                              }
                           }
                        }
                     }

                     if (canRemove) {
                        iterator.remove();
                     }
                  }
               }
            }

            if (!shouldContinue) {
               break;
            }
         }

         if (this.source == null) {
            this.source = this.sourceSupplier.get();
         }

         if (this.source != null) {
            this.keepPortableFluidInterfaceEngaged();
            if (!this.targets.isEmpty()) {
               for (Pair<BlockFace, FlowSource> pair : this.targets) {
                  if (pair.getSecond() == null || this.world.getGameTime() % 40L == 0L) {
                     PipeConnection pipeConnection = this.get((BlockFace)pair.getFirst());
                     if (pipeConnection != null) {
                        pipeConnection.source.ifPresent(fs -> {
                           if (fs.isEndpoint()) {
                              pair.setSecond(fs);
                           }
                        });
                     }
                  }
               }

               int flowSpeed = this.transferSpeed;
               Map<IFluidHandler, Integer> accumulatedFill = new IdentityHashMap<>();

               for (boolean simulate : Iterate.trueAndFalse) {
                  FluidAction action = simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE;
                  if (this.source == null) {
                     return;
                  }

                  IFluidHandler sourceCap = this.source.getCapability();
                  if (sourceCap == null) {
                     return;
                  }

                  FluidStack transfer = FluidStack.EMPTY;

                  for (int i = 0; i < sourceCap.getTanks(); i++) {
                     FluidStack contained = sourceCap.getFluidInTank(i);
                     if (!contained.isEmpty() && FluidStack.isSameFluidSameComponents(contained, this.fluid)) {
                        FluidStack toExtract = FluidHelper.copyStackWithAmount(contained, flowSpeed);
                        transfer = sourceCap.drain(toExtract, action);
                        break;
                     }
                  }

                  if (transfer.isEmpty()) {
                     FluidStack genericExtract = sourceCap.drain(flowSpeed, action);
                     if (!genericExtract.isEmpty() && FluidStack.isSameFluidSameComponents(genericExtract, this.fluid)) {
                        transfer = genericExtract;
                     }
                  }

                  if (transfer.isEmpty()) {
                     return;
                  }

                  if (simulate) {
                     flowSpeed = transfer.getAmount();
                  }

                  List<Pair<BlockFace, FlowSource>> availableOutputs = new ArrayList<>(this.targets);

                  while (!availableOutputs.isEmpty() && transfer.getAmount() > 0) {
                     int dividedTransfer = transfer.getAmount() / availableOutputs.size();
                     int remainder = transfer.getAmount() % availableOutputs.size();
                     Iterator<Pair<BlockFace, FlowSource>> iterator = availableOutputs.iterator();

                     while (iterator.hasNext()) {
                        Pair<BlockFace, FlowSource> pairx = iterator.next();
                        int toTransfer = dividedTransfer;
                        if (remainder > 0) {
                           toTransfer = dividedTransfer + 1;
                           remainder--;
                        }

                        if (transfer.isEmpty()) {
                           break;
                        }

                        ICapabilityProvider<IFluidHandler> targetHandlerProvider = ((FlowSource)pairx.getSecond()).provideHandler();
                        if (targetHandlerProvider == null) {
                           iterator.remove();
                        } else {
                           IFluidHandler targetHandler = targetHandlerProvider.getCapability();
                           if (targetHandler == null) {
                              iterator.remove();
                           } else {
                              int simulatedTransfer = toTransfer;
                              if (simulate) {
                                 simulatedTransfer = toTransfer + accumulatedFill.getOrDefault(targetHandler, 0);
                              }

                              FluidStack divided = transfer.copy();
                              divided.setAmount(simulatedTransfer);
                              int fill = targetHandler.fill(divided, action);
                              if (simulate) {
                                 accumulatedFill.put(targetHandler, fill);
                                 fill -= simulatedTransfer - toTransfer;
                              }

                              transfer.setAmount(transfer.getAmount() - fill);
                              if (fill < simulatedTransfer) {
                                 iterator.remove();
                              }
                           }
                        }
                     }
                  }

                  flowSpeed -= transfer.getAmount();
                  transfer = FluidStack.EMPTY;
               }
            }
         }
      }
   }

   private void keepPortableFluidInterfaceEngaged() {
      if (this.source instanceof PortableFluidInterfaceBlockEntity.InterfaceFluidHandler) {
         if (!this.frontier.isEmpty()) {
            ((PortableFluidInterfaceBlockEntity.InterfaceFluidHandler)this.source).keepAlive();
         }
      }
   }

   public void reset() {
      this.frontier.clear();
      this.visited.clear();
      this.targets.clear();
      this.queued.clear();
      this.fluid = FluidStack.EMPTY;
      this.queued.add(this.start);
      this.pauseBeforePropagation = 2;
   }

   @Nullable
   private PipeConnection get(BlockFace location) {
      BlockPos pos = location.getPos();
      FluidTransportBehaviour fluidTransfer = this.getFluidTransfer(pos);
      return fluidTransfer == null ? null : fluidTransfer.getConnection(location.getFace());
   }

   private boolean isPresent(BlockFace location) {
      return this.world.isLoaded(location.getPos());
   }

   @Nullable
   private FluidTransportBehaviour getFluidTransfer(BlockPos pos) {
      WeakReference<FluidTransportBehaviour> weakReference = this.cache.get(pos);
      FluidTransportBehaviour behaviour = weakReference != null ? weakReference.get() : null;
      if (behaviour != null && behaviour.blockEntity.isRemoved()) {
         behaviour = null;
      }

      if (behaviour == null) {
         behaviour = BlockEntityBehaviour.get(this.world, pos, FluidTransportBehaviour.TYPE);
         if (behaviour != null) {
            this.cache.put(pos, new WeakReference<>(behaviour));
         }
      }

      return behaviour;
   }
}

package com.simibubi.create.content.trains.station;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.compat.computercraft.events.StationTrainPresenceEvent;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.decoration.slidingDoor.DoorControlBehaviour;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.logistics.depot.DepotBehaviour;
import com.simibubi.create.content.logistics.packagePort.PackagePortBlockEntity;
import com.simibubi.create.content.logistics.packagePort.postbox.PostboxBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlockEntity;
import com.simibubi.create.content.trains.entity.AddTrainPacket;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.DiscoveredPath;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackGraphLocation;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleItem;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.WorldAttached;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import org.jetbrains.annotations.Nullable;

public class StationBlockEntity extends SmartBlockEntity implements TransformableBlockEntity {
   public TrackTargetingBehaviour<GlobalStation> edgePoint;
   public DoorControlBehaviour doorControls;
   public LerpedFloat flag;
   protected int failedCarriageIndex;
   protected AssemblyException lastException;
   protected DepotBehaviour depotBehaviour;
   public AbstractComputerBehaviour computerBehaviour;
   UUID imminentTrain;
   boolean trainPresent;
   boolean trainBackwards;
   boolean trainCanDisassemble;
   boolean trainHasSchedule;
   boolean trainHasAutoSchedule;
   int flagYRot = -1;
   boolean flagFlipped;
   public Component lastDisassembledTrainName;
   public int lastDisassembledMapColorIndex;
   public static WorldAttached<Map<BlockPos, BoundingBox>> assemblyAreas = new WorldAttached(w -> new HashMap());
   Direction assemblyDirection;
   int assemblyLength;
   int[] bogeyLocations;
   AbstractBogeyBlock<?>[] bogeyTypes;
   boolean[] upsideDownBogeys;
   int bogeyCount;

   public StationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.setLazyTickRate(20);
      this.lastException = null;
      this.failedCarriageIndex = -1;
      this.flag = LerpedFloat.linear().startWithValue(0.0);
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      event.registerBlockEntity(ItemHandler.BLOCK, (BlockEntityType)AllBlockEntityTypes.TRACK_STATION.get(), (be, context) -> be.depotBehaviour.itemHandler);
      if (Mods.COMPUTERCRAFT.isLoaded()) {
         event.registerBlockEntity(
            PeripheralCapability.get(),
            (BlockEntityType)AllBlockEntityTypes.TRACK_STATION.get(),
            (be, context) -> be.computerBehaviour.getPeripheralCapability()
         );
      }
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(this.edgePoint = new TrackTargetingBehaviour<>(this, EdgePointType.STATION));
      behaviours.add(this.doorControls = new DoorControlBehaviour(this));
      behaviours.add(this.depotBehaviour = new DepotBehaviour(this).onlyAccepts(AllItems.SCHEDULE::isIn).withCallback(s -> this.applyAutoSchedule()));
      this.depotBehaviour.addSubBehaviours(behaviours);
      this.registerAwardables(
         behaviours, new CreateAdvancement[]{AllAdvancements.CONTRAPTION_ACTORS, AllAdvancements.TRAIN, AllAdvancements.LONG_TRAIN, AllAdvancements.CONDUCTOR}
      );
      behaviours.add(this.computerBehaviour = ComputerCraftProxy.behaviour(this));
   }

   @Override
   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      this.lastException = AssemblyException.read(tag, registries);
      this.failedCarriageIndex = tag.getInt("FailedCarriageIndex");
      super.read(tag, registries, clientPacket);
      this.invalidateRenderBoundingBox();
      if (tag.contains("ForceFlag")) {
         this.trainPresent = tag.getBoolean("ForceFlag");
      }

      if (tag.contains("PrevTrainName")) {
         this.lastDisassembledTrainName = Serializer.fromJson(tag.getString("PrevTrainName"), registries);
      }

      this.lastDisassembledMapColorIndex = tag.getInt("PrevTrainColor");
      if (clientPacket) {
         if (!tag.contains("ImminentTrain")) {
            this.imminentTrain = null;
            this.trainPresent = false;
            this.trainCanDisassemble = false;
            this.trainBackwards = false;
         } else {
            this.imminentTrain = tag.getUUID("ImminentTrain");
            this.trainPresent = tag.contains("TrainPresent");
            this.trainCanDisassemble = tag.contains("TrainCanDisassemble");
            this.trainBackwards = tag.contains("TrainBackwards");
            this.trainHasSchedule = tag.contains("TrainHasSchedule");
            this.trainHasAutoSchedule = tag.contains("TrainHasAutoSchedule");
         }
      }
   }

   @Override
   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      AssemblyException.write(tag, registries, this.lastException);
      tag.putInt("FailedCarriageIndex", this.failedCarriageIndex);
      if (this.lastDisassembledTrainName != null) {
         tag.putString("PrevTrainName", Serializer.toJson(this.lastDisassembledTrainName, registries));
      }

      tag.putInt("PrevTrainColor", this.lastDisassembledMapColorIndex);
      super.write(tag, registries, clientPacket);
      if (clientPacket) {
         if (this.imminentTrain != null) {
            tag.putUUID("ImminentTrain", this.imminentTrain);
            if (this.trainPresent) {
               NBTHelper.putMarker(tag, "TrainPresent");
            }

            if (this.trainCanDisassemble) {
               NBTHelper.putMarker(tag, "TrainCanDisassemble");
            }

            if (this.trainBackwards) {
               NBTHelper.putMarker(tag, "TrainBackwards");
            }

            if (this.trainHasSchedule) {
               NBTHelper.putMarker(tag, "TrainHasSchedule");
            }

            if (this.trainHasAutoSchedule) {
               NBTHelper.putMarker(tag, "TrainHasAutoSchedule");
            }
         }
      }
   }

   @Nullable
   public GlobalStation getStation() {
      return this.edgePoint.getEdgePoint();
   }

   @Override
   public void lazyTick() {
      if (this.isAssembling() && !this.level.isClientSide) {
         this.refreshAssemblyInfo();
      }

      super.lazyTick();
   }

   @Override
   public void tick() {
      if (this.isAssembling() && this.level.isClientSide) {
         this.refreshAssemblyInfo();
      }

      super.tick();
      if (this.level.isClientSide) {
         float currentTarget = this.flag.getChaseTarget();
         if (currentTarget == 0.0F || this.flag.settled()) {
            int target = !this.trainPresent && !this.isAssembling() ? 0 : 1;
            if ((float)target != currentTarget) {
               this.flag.chase((double)target, 0.1F, Chaser.LINEAR);
               if (target == 1) {
                  AllSoundEvents.CONTRAPTION_DISASSEMBLE.playAt(this.level, this.worldPosition, 1.0F, 2.0F, true);
               }
            }
         }

         boolean settled = this.flag.getValue() > 0.15F;
         this.flag.tickChaser();
         if (currentTarget == 0.0F && settled != this.flag.getValue() > 0.15F) {
            AllSoundEvents.CONTRAPTION_ASSEMBLE.playAt(this.level, this.worldPosition, 0.75F, 1.5F, true);
         }
      } else {
         GlobalStation station = this.getStation();
         if (station != null) {
            Train imminentTrain = station.getImminentTrain();
            boolean trainPresent = imminentTrain != null && imminentTrain.getCurrentStation() == station;
            boolean canDisassemble = trainPresent && imminentTrain.canDisassemble();
            UUID imminentID = imminentTrain != null ? imminentTrain.id : null;
            boolean trainHasSchedule = trainPresent && imminentTrain.runtime.getSchedule() != null;
            boolean trainHasAutoSchedule = trainHasSchedule && imminentTrain.runtime.isAutoSchedule;
            boolean newlyArrived = this.trainPresent != trainPresent;
            if (trainPresent && imminentTrain.runtime.displayLinkUpdateRequested) {
               DisplayLinkBlock.notifyGatherers(this.level, this.worldPosition);
               imminentTrain.runtime.displayLinkUpdateRequested = false;
            }

            if (!this.level.isClientSide && this.computerBehaviour.hasAttachedComputer()) {
               if (this.imminentTrain == null && imminentTrain != null) {
                  this.computerBehaviour.prepareComputerEvent(new StationTrainPresenceEvent(StationTrainPresenceEvent.Type.IMMINENT, imminentTrain));
               }

               if (newlyArrived) {
                  if (trainPresent) {
                     this.computerBehaviour.prepareComputerEvent(new StationTrainPresenceEvent(StationTrainPresenceEvent.Type.ARRIVAL, imminentTrain));
                  } else {
                     Train train = Create.RAILWAYS.trains.get(this.imminentTrain);
                     if (train != null) {
                        this.computerBehaviour.prepareComputerEvent(new StationTrainPresenceEvent(StationTrainPresenceEvent.Type.DEPARTURE, train));
                     }
                  }
               }
            }

            if (newlyArrived) {
               this.applyAutoSchedule();
            }

            if (newlyArrived
               || this.trainCanDisassemble != canDisassemble
               || !Objects.equals(imminentID, this.imminentTrain)
               || this.trainHasSchedule != trainHasSchedule
               || this.trainHasAutoSchedule != trainHasAutoSchedule) {
               this.imminentTrain = imminentID;
               this.trainPresent = trainPresent;
               this.trainCanDisassemble = canDisassemble;
               this.trainBackwards = imminentTrain != null && imminentTrain.currentlyBackwards;
               this.trainHasSchedule = trainHasSchedule;
               this.trainHasAutoSchedule = trainHasAutoSchedule;
               this.notifyUpdate();
            }
         }
      }
   }

   public boolean trackClicked(Player player, InteractionHand hand, ITrackBlock track, BlockState state, BlockPos pos) {
      this.refreshAssemblyInfo();
      BoundingBox bb = (BoundingBox)((Map)assemblyAreas.get(this.level)).get(this.worldPosition);
      if (bb != null && bb.isInside(pos)) {
         BlockPos up = BlockPos.containing(track.getUpNormal(this.level, pos, state));
         BlockPos down = BlockPos.containing(track.getUpNormal(this.level, pos, state).scale(-1.0));
         int bogeyOffset = pos.distManhattan(this.edgePoint.getGlobalPosition()) - 1;
         if (!this.isValidBogeyOffset(bogeyOffset)) {
            for (boolean upsideDown : Iterate.falseAndTrue) {
               for (int i = -1; i <= 1; i++) {
                  BlockPos bogeyPos = pos.relative(this.assemblyDirection, i).offset(upsideDown ? down : up);
                  BlockState blockState = this.level.getBlockState(bogeyPos);
                  Block be = blockState.getBlock();
                  if (be instanceof AbstractBogeyBlock) {
                     AbstractBogeyBlock<?> bogey = (AbstractBogeyBlock<?>)be;
                     BlockEntity bex = this.level.getBlockEntity(bogeyPos);
                     if (bex instanceof AbstractBogeyBlockEntity) {
                        AbstractBogeyBlockEntity oldBE = (AbstractBogeyBlockEntity)bex;
                        CompoundTag oldData = oldBE.getBogeyData();
                        BlockState newBlock = bogey.getNextSize(oldBE);
                        if (newBlock.getBlock() == bogey) {
                           player.displayClientMessage(CreateLang.translateDirect("bogey.style.no_other_sizes").withStyle(ChatFormatting.RED), true);
                        }

                        this.level.setBlock(bogeyPos, newBlock, 3);
                        if (this.level.getBlockEntity(bogeyPos) instanceof AbstractBogeyBlockEntity newBE) {
                           newBE.setBogeyData(oldData);
                           IWrenchable.playRotateSound(this.level, bogeyPos);
                           return true;
                        }
                     }
                  }
               }
            }

            return false;
         } else {
            ItemStack handItem = player.getItemInHand(hand);
            if (!player.isCreative() && !AllBlocks.RAILWAY_CASING.isIn(handItem)) {
               player.displayClientMessage(CreateLang.translateDirect("train_assembly.requires_casing"), true);
               return false;
            } else {
               boolean upsideDown = player.getViewXRot(1.0F) < 0.0F
                  && track.getBogeyAnchor(this.level, pos, state).getBlock() instanceof AbstractBogeyBlock<?> bogey
                  && bogey.canBeUpsideDown();
               BlockPos targetPos = upsideDown ? pos.offset(down) : pos.offset(up);
               if (this.level.getBlockState(targetPos).getDestroySpeed(this.level, targetPos) == -1.0F) {
                  return false;
               } else {
                  this.level.destroyBlock(targetPos, true);
                  BlockState bogeyAnchor = track.getBogeyAnchor(this.level, pos, state);
                  if (bogeyAnchor.getBlock() instanceof AbstractBogeyBlock<?> bogeyx) {
                     bogeyAnchor = bogeyx.getVersion(bogeyAnchor, upsideDown);
                  }

                  bogeyAnchor = ProperWaterloggedBlock.withWater(this.level, bogeyAnchor, pos);
                  this.level.setBlock(targetPos, bogeyAnchor, 3);
                  player.displayClientMessage(CreateLang.translateDirect("train_assembly.bogey_created"), true);
                  SoundType soundtype = bogeyAnchor.getBlock().getSoundType(state, this.level, pos, player);
                  this.level
                     .playSound(null, pos, soundtype.getPlaceSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
                  if (!player.isCreative()) {
                     ItemStack itemInHand = player.getItemInHand(hand);
                     itemInHand.shrink(1);
                     if (itemInHand.isEmpty()) {
                        player.setItemInHand(hand, ItemStack.EMPTY);
                     }
                  }

                  return true;
               }
            }
         }
      } else {
         return false;
      }
   }

   public boolean enterAssemblyMode(@Nullable ServerPlayer sender) {
      if (this.isAssembling()) {
         return false;
      } else {
         this.tryDisassembleTrain(sender);
         if (!this.tryEnterAssemblyMode()) {
            return false;
         } else if (!(this.level.getBlockState(this.worldPosition).getBlock() instanceof StationBlock)) {
            return true;
         } else {
            BlockState newState = (BlockState)this.getBlockState().setValue(StationBlock.ASSEMBLING, true);
            this.level.setBlock(this.getBlockPos(), newState, 3);
            this.refreshBlockState();
            this.refreshAssemblyInfo();
            this.updateStationState(stationx -> stationx.assembling = true);
            GlobalStation station = this.getStation();
            if (station != null) {
               for (Train train : Create.RAILWAYS.sided(this.level).trains.values()) {
                  if (train.navigation.destination == station) {
                     DiscoveredPath preferredPath = train.runtime.startCurrentInstruction(this.level);
                     train.navigation.startNavigation(preferredPath != null ? preferredPath : train.navigation.findPathTo(station, Double.MAX_VALUE));
                  }
               }
            }

            return true;
         }
      }
   }

   public boolean exitAssemblyMode() {
      if (!this.isAssembling()) {
         return false;
      } else {
         this.cancelAssembly();
         BlockState newState = (BlockState)this.getBlockState().setValue(StationBlock.ASSEMBLING, false);
         this.level.setBlock(this.getBlockPos(), newState, 3);
         this.refreshBlockState();
         return this.updateStationState(station -> station.assembling = false);
      }
   }

   public boolean tryDisassembleTrain(@Nullable ServerPlayer sender) {
      GlobalStation station = this.getStation();
      if (station == null) {
         return false;
      } else {
         Train train = station.getPresentTrain();
         if (train == null) {
            return false;
         } else {
            BlockPos trackPosition = this.edgePoint.getGlobalPosition();
            if (!train.disassemble(this.getAssemblyDirection(), trackPosition.above())) {
               return false;
            } else {
               this.dropSchedule(sender, train);
               return true;
            }
         }
      }
   }

   public boolean isAssembling() {
      BlockState state = this.getBlockState();
      return state.hasProperty(StationBlock.ASSEMBLING) && (Boolean)state.getValue(StationBlock.ASSEMBLING);
   }

   public boolean tryEnterAssemblyMode() {
      if (!this.edgePoint.hasValidTrack()) {
         return false;
      } else {
         BlockPos targetPosition = this.edgePoint.getGlobalPosition();
         BlockState trackState = this.edgePoint.getTrackBlockState();
         ITrackBlock track = this.edgePoint.getTrack();
         Vec3 trackAxis = track.getTrackAxes(this.level, targetPosition, trackState).get(0);
         boolean axisFound = false;

         for (Axis axis : Iterate.axes) {
            if (trackAxis.get(axis) != 0.0) {
               if (axisFound) {
                  return false;
               }

               axisFound = true;
            }
         }

         return true;
      }
   }

   public void dropSchedule(@Nullable ServerPlayer sender, @Nullable Train train) {
      GlobalStation station = this.getStation();
      if (station != null) {
         if (train != null) {
            ItemStack schedule = train.runtime.returnSchedule(this.level.registryAccess());
            if (!schedule.isEmpty()) {
               if (sender != null && sender.getMainHandItem().isEmpty()) {
                  sender.getInventory().placeItemBackInInventory(schedule);
               } else {
                  Vec3 v = VecHelper.getCenterOf(this.getBlockPos());
                  ItemEntity itemEntity = new ItemEntity(this.getLevel(), v.x, v.y, v.z, schedule);
                  itemEntity.setDeltaMovement(Vec3.ZERO);
                  this.getLevel().addFreshEntity(itemEntity);
               }
            }
         }
      }
   }

   public void updateMapColor(int color) {
      GlobalStation station = this.getStation();
      if (station != null) {
         Train train = station.getPresentTrain();
         if (train != null) {
            train.mapColorIndex = color;
         }
      }
   }

   private boolean updateStationState(Consumer<GlobalStation> updateState) {
      GlobalStation station = this.getStation();
      TrackGraphLocation graphLocation = this.edgePoint.determineGraphLocation();
      if (station != null && graphLocation != null) {
         updateState.accept(station);
         Create.RAILWAYS.sync.pointAdded(graphLocation.graph, station);
         Create.RAILWAYS.markTracksDirty();
         return true;
      } else {
         return false;
      }
   }

   public void refreshAssemblyInfo() {
      if (this.edgePoint.hasValidTrack()) {
         if (!this.isVirtual()) {
            GlobalStation station = this.getStation();
            if (station == null || station.getPresentTrain() != null) {
               return;
            }
         }

         int prevLength = this.assemblyLength;
         BlockPos targetPosition = this.edgePoint.getGlobalPosition();
         BlockState trackState = this.edgePoint.getTrackBlockState();
         ITrackBlock track = this.edgePoint.getTrack();
         this.getAssemblyDirection();
         MutableBlockPos currentPos = targetPosition.mutable();
         currentPos.move(this.assemblyDirection);
         BlockPos bogeyOffset = BlockPos.containing(track.getUpNormal(this.level, targetPosition, trackState));
         int maxLength = (Integer)AllConfigs.server().trains.maxAssemblyLength.get();
         int maxBogeyCount = (Integer)AllConfigs.server().trains.maxBogeyCount.get();
         int bogeyIndex = 0;
         if (this.bogeyLocations == null) {
            this.bogeyLocations = new int[maxBogeyCount];
         }

         if (this.bogeyTypes == null) {
            this.bogeyTypes = new AbstractBogeyBlock[maxBogeyCount];
         }

         if (this.upsideDownBogeys == null) {
            this.upsideDownBogeys = new boolean[maxBogeyCount];
         }

         Arrays.fill(this.bogeyLocations, -1);
         Arrays.fill(this.bogeyTypes, null);
         Arrays.fill(this.upsideDownBogeys, false);

         for (int i = 0; i < maxLength; i++) {
            if (i == maxLength - 1) {
               this.assemblyLength = i;
               break;
            }

            if (!track.trackEquals(trackState, this.level.getBlockState(currentPos))) {
               this.assemblyLength = Math.max(0, i - 1);
               break;
            }

            BlockState potentialBogeyState = this.level.getBlockState(bogeyOffset.offset(currentPos));
            BlockPos upsideDownBogeyOffset = new BlockPos(bogeyOffset.getX(), bogeyOffset.getY() * -1, bogeyOffset.getZ());
            label59:
            if (bogeyIndex < this.bogeyLocations.length) {
               if (potentialBogeyState.getBlock() instanceof AbstractBogeyBlock<?> bogey && !bogey.isUpsideDown(potentialBogeyState)) {
                  this.bogeyTypes[bogeyIndex] = bogey;
                  this.bogeyLocations[bogeyIndex] = i;
                  this.upsideDownBogeys[bogeyIndex] = false;
                  bogeyIndex++;
                  break label59;
               }

               if ((potentialBogeyState = this.level.getBlockState(upsideDownBogeyOffset.offset(currentPos))).getBlock() instanceof AbstractBogeyBlock<?> bogey
                  && bogey.isUpsideDown(potentialBogeyState)) {
                  this.bogeyTypes[bogeyIndex] = bogey;
                  this.bogeyLocations[bogeyIndex] = i;
                  this.upsideDownBogeys[bogeyIndex] = true;
                  bogeyIndex++;
               }
            }

            currentPos.move(this.assemblyDirection);
         }

         this.bogeyCount = bogeyIndex;
         if (!this.level.isClientSide) {
            if (prevLength != this.assemblyLength) {
               if (!this.isVirtual()) {
                  Map<BlockPos, BoundingBox> map = (Map<BlockPos, BoundingBox>)assemblyAreas.get(this.level);
                  BlockPos startPosition = targetPosition.relative(this.assemblyDirection);
                  BlockPos trackEnd = startPosition.relative(this.assemblyDirection, this.assemblyLength - 1);
                  map.put(this.worldPosition, BoundingBox.fromCorners(startPosition, trackEnd));
               }
            }
         }
      }
   }

   public boolean updateName(String name) {
      if (!this.updateStationState(station -> station.name = name)) {
         return false;
      } else {
         this.notifyUpdate();
         return true;
      }
   }

   public boolean isValidBogeyOffset(int i) {
      if ((i < 3 || this.bogeyCount == 0) && i != 0) {
         return false;
      } else {
         for (int j : this.bogeyLocations) {
            if (j == -1) {
               break;
            }

            if (i >= j - 2 && i <= j + 2) {
               return false;
            }
         }

         return true;
      }
   }

   public Direction getAssemblyDirection() {
      if (this.assemblyDirection != null) {
         return this.assemblyDirection;
      } else if (!this.edgePoint.hasValidTrack()) {
         return null;
      } else {
         BlockPos targetPosition = this.edgePoint.getGlobalPosition();
         BlockState trackState = this.edgePoint.getTrackBlockState();
         ITrackBlock track = this.edgePoint.getTrack();
         AxisDirection axisDirection = this.edgePoint.getTargetDirection();
         Vec3 axis = track.getTrackAxes(this.level, targetPosition, trackState).get(0).normalize().scale((double)axisDirection.getStep());
         return this.assemblyDirection = Direction.getNearest(axis.x, axis.y, axis.z);
      }
   }

   @Override
   public void remove() {
      ((Map)assemblyAreas.get(this.level)).remove(this.worldPosition);
      super.remove();
   }

   public void assemble(UUID playerUUID) {
      this.refreshAssemblyInfo();
      if (this.bogeyLocations != null) {
         if (this.bogeyLocations[0] != 0) {
            this.exception(new AssemblyException(CreateLang.translateDirect("train_assembly.frontmost_bogey_at_station")), -1);
         } else if (this.edgePoint.hasValidTrack()) {
            BlockPos trackPosition = this.edgePoint.getGlobalPosition();
            BlockState trackState = this.edgePoint.getTrackBlockState();
            ITrackBlock track = this.edgePoint.getTrack();
            BlockPos bogeyOffset = BlockPos.containing(track.getUpNormal(this.level, trackPosition, trackState));
            TrackNodeLocation location = null;
            Vec3 center = Vec3.atBottomCenterOf(trackPosition).add(0.0, track.getElevationAtCenter(this.level, trackPosition, trackState), 0.0);
            Collection<TrackNodeLocation.DiscoveredLocation> ends = track.getConnected(this.level, trackPosition, trackState, true, null);
            Vec3 targetOffset = Vec3.atLowerCornerOf(this.assemblyDirection.getNormal());

            for (TrackNodeLocation.DiscoveredLocation end : ends) {
               if (Mth.equal(0.0, targetOffset.distanceToSqr(end.getLocation().subtract(center).normalize()))) {
                  location = end;
               }
            }

            if (location != null) {
               List<Double> pointOffsets = new ArrayList<>();
               int iPrevious = -100;

               for (int i = 0; i < this.bogeyLocations.length; i++) {
                  int loc = this.bogeyLocations[i];
                  if (loc == -1) {
                     break;
                  }

                  if (loc - iPrevious < 3) {
                     this.exception(new AssemblyException(CreateLang.translateDirect("train_assembly.bogeys_too_close", i, i + 1)), -1);
                     return;
                  }

                  double bogeySize = this.bogeyTypes[i].getWheelPointSpacing();
                  pointOffsets.add((double)loc + 0.5 - bogeySize / 2.0);
                  pointOffsets.add((double)loc + 0.5 + bogeySize / 2.0);
                  iPrevious = loc;
               }

               List<TravellingPoint> points = new ArrayList<>();
               Vec3 directionVec = Vec3.atLowerCornerOf(this.assemblyDirection.getNormal());
               TrackGraph graph = null;
               TrackNode secondNode = null;

               for (int j = 0; j < this.assemblyLength * 2 + 40; j++) {
                  double i = (double)j / 2.0;
                  if (points.size() == pointOffsets.size()) {
                     break;
                  }

                  TrackNodeLocation currentLocation = location;
                  location = new TrackNodeLocation(location.getLocation().add(directionVec.scale(0.5))).in(location.dimension);
                  if (graph == null) {
                     graph = Create.RAILWAYS.getGraph(this.level, currentLocation);
                  }

                  if (graph != null) {
                     TrackNode node = graph.locateNode(currentLocation);
                     if (node != null) {
                        for (int pointIndex = points.size(); pointIndex < pointOffsets.size(); pointIndex++) {
                           double offset = pointOffsets.get(pointIndex);
                           if (offset > i) {
                              break;
                           }

                           double positionOnEdge = i - offset;
                           Map<TrackNode, TrackEdge> connectionsFromNode = graph.getConnectionsFrom(node);
                           if (secondNode == null) {
                              for (Entry<TrackNode, TrackEdge> entry : connectionsFromNode.entrySet()) {
                                 TrackEdge edge = entry.getValue();
                                 TrackNode otherNode = entry.getKey();
                                 if (!edge.isTurn()) {
                                    Vec3 edgeDirection = edge.getDirection(true);
                                    if (Mth.equal(edgeDirection.normalize().dot(directionVec), -1.0)) {
                                       secondNode = otherNode;
                                    }
                                 }
                              }
                           }

                           if (secondNode == null) {
                              Create.LOGGER.warn("Cannot assemble: No valid starting node found");
                              return;
                           }

                           TrackEdge edge = connectionsFromNode.get(secondNode);
                           if (edge == null) {
                              Create.LOGGER.warn("Cannot assemble: Missing graph edge");
                              return;
                           }

                           points.add(new TravellingPoint(node, secondNode, edge, positionOnEdge, false));
                        }

                        secondNode = node;
                     }
                  }
               }

               if (points.size() != pointOffsets.size()) {
                  Create.LOGGER.warn("Cannot assemble: Not all Points created");
               } else if (points.size() == 0) {
                  this.exception(new AssemblyException(CreateLang.translateDirect("train_assembly.no_bogeys")), -1);
               } else {
                  List<CarriageContraption> contraptions = new ArrayList<>();
                  List<Carriage> carriages = new ArrayList<>();
                  List<Integer> spacing = new ArrayList<>();
                  boolean atLeastOneForwardControls = false;

                  for (int bogeyIndex = 0; bogeyIndex < this.bogeyCount; bogeyIndex++) {
                     int pointIndex = bogeyIndex * 2;
                     if (bogeyIndex > 0) {
                        spacing.add(this.bogeyLocations[bogeyIndex] - this.bogeyLocations[bogeyIndex - 1]);
                     }

                     CarriageContraption contraption = new CarriageContraption(this.assemblyDirection);
                     BlockPos bogeyPosOffset = trackPosition.offset(bogeyOffset);
                     BlockPos upsideDownBogeyPosOffset = trackPosition.offset(new BlockPos(bogeyOffset.getX(), bogeyOffset.getY() * -1, bogeyOffset.getZ()));

                     try {
                        int offsetx = this.bogeyLocations[bogeyIndex] + 1;
                        boolean success = contraption.assemble(
                           this.level,
                           this.upsideDownBogeys[bogeyIndex]
                              ? upsideDownBogeyPosOffset.relative(this.assemblyDirection, offsetx)
                              : bogeyPosOffset.relative(this.assemblyDirection, offsetx)
                        );
                        atLeastOneForwardControls |= contraption.hasForwardControls();
                        contraption.setSoundQueueOffset(offsetx);
                        if (!success) {
                           this.exception(new AssemblyException(CreateLang.translateDirect("train_assembly.nothing_attached", bogeyIndex + 1)), -1);
                           return;
                        }
                     } catch (AssemblyException var34) {
                        this.exception(var34, contraptions.size() + 1);
                        return;
                     }

                     AbstractBogeyBlock<?> typeOfFirstBogey = this.bogeyTypes[bogeyIndex];
                     boolean firstBogeyIsUpsideDown = this.upsideDownBogeys[bogeyIndex];
                     BlockPos firstBogeyPos = contraption.anchor;
                     AbstractBogeyBlockEntity firstBogeyBlockEntity = (AbstractBogeyBlockEntity)this.level.getBlockEntity(firstBogeyPos);
                     CarriageBogey firstBogey = new CarriageBogey(
                        typeOfFirstBogey, firstBogeyIsUpsideDown, firstBogeyBlockEntity.getBogeyData(), points.get(pointIndex), points.get(pointIndex + 1)
                     );
                     CarriageBogey secondBogey = null;
                     BlockPos secondBogeyPos = contraption.getSecondBogeyPos();
                     int bogeySpacing = 0;
                     if (secondBogeyPos != null) {
                        if (bogeyIndex == this.bogeyCount - 1
                           || !secondBogeyPos.equals(
                              (this.upsideDownBogeys[bogeyIndex + 1] ? upsideDownBogeyPosOffset : bogeyPosOffset)
                                 .relative(this.assemblyDirection, this.bogeyLocations[bogeyIndex + 1] + 1)
                           )) {
                           this.exception(new AssemblyException(CreateLang.translateDirect("train_assembly.not_connected_in_order")), contraptions.size() + 1);
                           return;
                        }

                        AbstractBogeyBlockEntity secondBogeyBlockEntity = (AbstractBogeyBlockEntity)this.level.getBlockEntity(secondBogeyPos);
                        bogeySpacing = this.bogeyLocations[bogeyIndex + 1] - this.bogeyLocations[bogeyIndex];
                        secondBogey = new CarriageBogey(
                           this.bogeyTypes[bogeyIndex + 1],
                           this.upsideDownBogeys[bogeyIndex + 1],
                           secondBogeyBlockEntity.getBogeyData(),
                           points.get(pointIndex + 2),
                           points.get(pointIndex + 3)
                        );
                        bogeyIndex++;
                     } else if (!typeOfFirstBogey.allowsSingleBogeyCarriage()) {
                        this.exception(new AssemblyException(CreateLang.translateDirect("train_assembly.single_bogey_carriage")), contraptions.size() + 1);
                        return;
                     }

                     contraptions.add(contraption);
                     carriages.add(new Carriage(firstBogey, secondBogey, bogeySpacing));
                  }

                  if (!atLeastOneForwardControls) {
                     this.exception(new AssemblyException(CreateLang.translateDirect("train_assembly.no_controls")), -1);
                  } else {
                     for (CarriageContraption contraption : contraptions) {
                        contraption.removeBlocksFromWorld(this.level, BlockPos.ZERO);
                        contraption.expandBoundsAroundAxis(Axis.Y);
                     }

                     Train train = new Train(
                        UUID.randomUUID(), playerUUID, graph, carriages, spacing, contraptions.stream().anyMatch(CarriageContraption::hasBackwardControls), 0
                     );
                     if (this.lastDisassembledTrainName != null) {
                        train.name = this.lastDisassembledTrainName;
                        train.mapColorIndex = this.lastDisassembledMapColorIndex;
                        this.lastDisassembledTrainName = null;
                        this.lastDisassembledMapColorIndex = 0;
                     }

                     for (int ix = 0; ix < contraptions.size(); ix++) {
                        CarriageContraption contraption = contraptions.get(ix);
                        Carriage carriage = carriages.get(ix);
                        carriage.setContraption(this.level, contraption);
                        if (contraption.containsBlockBreakers()) {
                           this.award(AllAdvancements.CONTRAPTION_ACTORS);
                        }
                     }

                     GlobalStation station = this.getStation();
                     if (station != null) {
                        train.setCurrentStation(station);
                        station.reserveFor(train);
                     }

                     train.collectInitiallyOccupiedSignalBlocks();
                     Create.RAILWAYS.addTrain(train);
                     CatnipServices.NETWORK.sendToAllClients(new AddTrainPacket(train));
                     this.clearException();
                     this.award(AllAdvancements.TRAIN);
                     if (contraptions.size() >= 6) {
                        this.award(AllAdvancements.LONG_TRAIN);
                     }
                  }
               }
            }
         }
      }
   }

   public void cancelAssembly() {
      this.assemblyLength = 0;
      ((Map)assemblyAreas.get(this.level)).remove(this.worldPosition);
      this.clearException();
   }

   private void clearException() {
      this.exception(null, -1);
   }

   private void exception(AssemblyException exception, int carriage) {
      this.failedCarriageIndex = carriage;
      this.lastException = exception;
      this.sendData();
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public AABB getRenderBoundingBox() {
      return this.isAssembling() ? AABB.INFINITE : super.getRenderBoundingBox();
   }

   @Override
   protected AABB createRenderBoundingBox() {
      return new AABB(Vec3.atLowerCornerOf(this.worldPosition), Vec3.atLowerCornerOf(this.edgePoint.getGlobalPosition())).inflate(2.0);
   }

   public ItemStack getAutoSchedule() {
      return this.depotBehaviour.getHeldItemStack();
   }

   @Override
   public void invalidate() {
      super.invalidate();
      this.computerBehaviour.removePeripheral();
   }

   private void applyAutoSchedule() {
      ItemStack stack = this.getAutoSchedule();
      if (AllItems.SCHEDULE.isIn(stack)) {
         Schedule schedule = ScheduleItem.getSchedule(this.level.registryAccess(), stack);
         if (schedule != null && !schedule.entries.isEmpty()) {
            GlobalStation station = this.getStation();
            if (station != null) {
               Train imminentTrain = station.getImminentTrain();
               if (imminentTrain != null && imminentTrain.getCurrentStation() == station) {
                  this.award(AllAdvancements.CONDUCTOR);
                  imminentTrain.runtime.setSchedule(schedule, true);
                  AllSoundEvents.CONFIRM.playOnServer(this.level, this.worldPosition, 1.0F, 1.0F);
                  if (this.level instanceof ServerLevel server) {
                     Vec3 var7 = Vec3.atBottomCenterOf(this.worldPosition.above());
                     server.sendParticles(ParticleTypes.HAPPY_VILLAGER, var7.x, var7.y, var7.z, 8, 0.35, 0.05, 0.35, 1.0);
                     server.sendParticles(ParticleTypes.END_ROD, var7.x, var7.y + 0.25, var7.z, 10, 0.05, 1.0, 0.05, 0.005F);
                  }
               }
            }
         }
      }
   }

   public boolean resolveFlagAngle() {
      if (this.flagYRot != -1) {
         return true;
      } else {
         BlockState target = this.edgePoint.getTrackBlockState();
         if (!(target.getBlock() instanceof ITrackBlock def)) {
            return false;
         } else {
            Vec3 var7 = null;
            BlockPos trackPos = this.edgePoint.getGlobalPosition();

            for (Vec3 vec3 : def.getTrackAxes(this.level, trackPos, target)) {
               var7 = vec3.scale((double)this.edgePoint.getTargetDirection().getStep());
            }

            if (var7 == null) {
               return false;
            } else {
               Direction nearest = Direction.getNearest(var7.x, 0.0, var7.z);
               this.flagYRot = (int)(-nearest.toYRot() - 90.0F);
               Vec3 diff = Vec3.atLowerCornerOf(trackPos.subtract(this.worldPosition)).multiply(1.0, 0.0, 1.0);
               if (diff.lengthSqr() == 0.0) {
                  return true;
               } else {
                  this.flagFlipped = diff.dot(Vec3.atLowerCornerOf(nearest.getClockWise().getNormal())) > 0.0;
                  return true;
               }
            }
         }
      }
   }

   @Override
   public void transform(BlockEntity be, StructureTransform transform) {
      this.edgePoint.transform(be, transform);
   }

   public void attachPackagePort(PackagePortBlockEntity ppbe) {
      GlobalStation station = this.getStation();
      if (station != null && !this.level.isClientSide) {
         if (ppbe instanceof PostboxBlockEntity pbe) {
            pbe.trackedGlobalStation = new WeakReference<>(station);
         }

         GlobalPackagePort globalPackagePort = station.connectedPorts.get(ppbe.getBlockPos());
         if (globalPackagePort == null) {
            globalPackagePort = new GlobalPackagePort();
            globalPackagePort.address = ppbe.addressFilter;
            station.connectedPorts.put(ppbe.getBlockPos(), globalPackagePort);
         } else {
            globalPackagePort.restoreOfflineBuffer(ppbe.inventory);
         }
      }
   }

   public void removePackagePort(PackagePortBlockEntity ppbe) {
      GlobalStation station = this.getStation();
      if (station != null) {
         station.connectedPorts.remove(ppbe.getBlockPos());
      }
   }
}

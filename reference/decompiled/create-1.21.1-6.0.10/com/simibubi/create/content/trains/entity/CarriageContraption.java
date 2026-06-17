package com.simibubi.create.content.trains.entity;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllContraptionTypes;
import com.simibubi.create.api.behaviour.interaction.ConductorBlockInteractionBehavior;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;
import com.simibubi.create.content.contraptions.minecart.TrainCargoManager;
import com.simibubi.create.content.contraptions.render.ClientContraption;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.foundation.collision.CollisionList;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

public class CarriageContraption extends Contraption {
   private Direction assemblyDirection;
   private boolean forwardControls;
   private boolean backwardControls;
   public Couple<Boolean> blockConductors;
   public Map<BlockPos, Couple<Boolean>> conductorSeats = new HashMap<>();
   public ArrivalSoundQueue soundQueue;
   protected MountedStorageManager storageProxy;
   private int bogeys;
   private boolean sidewaysControls;
   private BlockPos secondBogeyPos;
   private List<BlockPos> assembledBlockConductors = new ArrayList<>();
   public int portalCutoffMin;
   public int portalCutoffMax;
   static final MountedStorageManager fallbackStorage = new MountedStorageManager();

   public CarriageContraption() {
      this.blockConductors = Couple.create(false, false);
      this.soundQueue = new ArrivalSoundQueue();
      this.portalCutoffMin = Integer.MIN_VALUE;
      this.portalCutoffMax = Integer.MAX_VALUE;
      this.storage = new TrainCargoManager();
   }

   public void setSoundQueueOffset(int offset) {
      this.soundQueue.offset = offset;
   }

   public CarriageContraption(Direction assemblyDirection) {
      this();
      this.assemblyDirection = assemblyDirection;
      this.bogeys = 0;
   }

   @Override
   public boolean assemble(Level world, BlockPos pos) throws AssemblyException {
      if (!this.searchMovedStructure(world, pos, null)) {
         return false;
      } else if (this.blocks.size() <= 1) {
         return false;
      } else if (this.bogeys == 0) {
         return false;
      } else if (this.bogeys > 2) {
         throw new AssemblyException(CreateLang.translateDirect("train_assembly.too_many_bogeys", this.bogeys));
      } else if (this.sidewaysControls) {
         throw new AssemblyException(CreateLang.translateDirect("train_assembly.sideways_controls"));
      } else {
         for (BlockPos blazePos : this.assembledBlockConductors) {
            for (Direction direction : Iterate.directionsInAxis(this.assemblyDirection.getAxis())) {
               if (this.inControl(blazePos, direction)) {
                  this.blockConductors.set(direction != this.assemblyDirection, true);
               }
            }
         }

         for (BlockPos seatPos : this.getSeats()) {
            for (Direction directionx : Iterate.directionsInAxis(this.assemblyDirection.getAxis())) {
               if (this.inControl(seatPos, directionx)) {
                  this.conductorSeats.computeIfAbsent(seatPos, p -> Couple.create(false, false)).set(directionx != this.assemblyDirection, true);
               }
            }
         }

         return true;
      }
   }

   public boolean inControl(BlockPos pos, Direction direction) {
      BlockPos controlsPos = pos.relative(direction);
      if (!this.blocks.containsKey(controlsPos)) {
         return false;
      } else {
         StructureBlockInfo info = this.blocks.get(controlsPos);
         return !AllBlocks.TRAIN_CONTROLS.has(info.state()) ? false : info.state().getValue(ControlsBlock.FACING) == direction.getOpposite();
      }
   }

   public void swapStorageAfterAssembly(CarriageContraptionEntity cce) {
      Carriage carriage = cce.getCarriage();
      if (carriage.storage == null) {
         carriage.storage = (TrainCargoManager)this.storage;
         this.storage = new MountedStorageManager();
      }

      this.storageProxy = carriage.storage;
   }

   public void returnStorageForDisassembly(MountedStorageManager storage) {
      this.storage = storage;
   }

   @Override
   protected boolean isAnchoringBlockAt(BlockPos pos) {
      return false;
   }

   @Override
   protected Pair<StructureBlockInfo, BlockEntity> capture(Level world, BlockPos pos) {
      BlockState blockState = world.getBlockState(pos);
      if (ArrivalSoundQueue.isPlayable(blockState)) {
         int anchorCoord = VecHelper.getCoordinate(this.anchor, this.assemblyDirection.getAxis());
         int posCoord = VecHelper.getCoordinate(pos, this.assemblyDirection.getAxis());
         this.soundQueue.add((posCoord - anchorCoord) * this.assemblyDirection.getAxisDirection().getStep(), this.toLocalPos(pos));
      }

      if (blockState.getBlock() instanceof AbstractBogeyBlock) {
         this.bogeys++;
         if (this.bogeys == 2) {
            this.secondBogeyPos = pos;
         }
      }

      MovingInteractionBehaviour behaviour = MovingInteractionBehaviour.REGISTRY.get(blockState);
      if (behaviour instanceof ConductorBlockInteractionBehavior conductor && conductor.isValidConductor(blockState)) {
         this.assembledBlockConductors.add(this.toLocalPos(pos));
      }

      if (AllBlocks.TRAIN_CONTROLS.has(blockState)) {
         Direction facing = (Direction)blockState.getValue(ControlsBlock.FACING);
         if (facing.getAxis() != this.assemblyDirection.getAxis()) {
            this.sidewaysControls = true;
         } else {
            boolean forwards = facing == this.assemblyDirection;
            if (forwards) {
               this.forwardControls = true;
            } else {
               this.backwardControls = true;
            }
         }
      }

      return super.capture(world, pos);
   }

   @Override
   public CompoundTag writeNBT(Provider registries, boolean spawnPacket) {
      CompoundTag tag = super.writeNBT(registries, spawnPacket);
      NBTHelper.writeEnum(tag, "AssemblyDirection", this.getAssemblyDirection());
      tag.putBoolean("FrontControls", this.forwardControls);
      tag.putBoolean("BackControls", this.backwardControls);
      tag.putBoolean("FrontBlazeConductor", (Boolean)this.blockConductors.getFirst());
      tag.putBoolean("BackBlazeConductor", (Boolean)this.blockConductors.getSecond());
      ListTag list = NBTHelper.writeCompoundList(this.conductorSeats.entrySet(), e -> {
         CompoundTag compoundTag = new CompoundTag();
         compoundTag.put("Pos", NbtUtils.writeBlockPos((BlockPos)e.getKey()));
         compoundTag.putBoolean("Forward", (Boolean)((Couple)e.getValue()).getFirst());
         compoundTag.putBoolean("Backward", (Boolean)((Couple)e.getValue()).getSecond());
         return compoundTag;
      });
      tag.put("ConductorSeats", list);
      this.soundQueue.serialize(tag);
      return tag;
   }

   @Override
   public void readNBT(Level world, CompoundTag nbt, boolean spawnData) {
      this.assemblyDirection = (Direction)NBTHelper.readEnum(nbt, "AssemblyDirection", Direction.class);
      this.forwardControls = nbt.getBoolean("FrontControls");
      this.backwardControls = nbt.getBoolean("BackControls");
      this.blockConductors = Couple.create(nbt.getBoolean("FrontBlazeConductor"), nbt.getBoolean("BackBlazeConductor"));
      this.conductorSeats.clear();
      NBTHelper.iterateCompoundList(
         nbt.getList("ConductorSeats", 10),
         c -> this.conductorSeats.put(NBTHelper.readBlockPos(c, "Pos"), Couple.create(c.getBoolean("Forward"), c.getBoolean("Backward")))
      );
      this.soundQueue.deserialize(nbt);
      super.readNBT(world, nbt, spawnData);
   }

   @Override
   public boolean canBeStabilized(Direction facing, BlockPos localPos) {
      return false;
   }

   @Override
   public ContraptionType getType() {
      return (ContraptionType)AllContraptionTypes.CARRIAGE.value();
   }

   public Direction getAssemblyDirection() {
      return this.assemblyDirection;
   }

   public boolean hasForwardControls() {
      return this.forwardControls;
   }

   public boolean hasBackwardControls() {
      return this.backwardControls;
   }

   public BlockPos getSecondBogeyPos() {
      return this.secondBogeyPos;
   }

   @Nullable
   @Override
   public CollisionList getSimplifiedEntityColliders() {
      return this.notInPortal() ? super.getSimplifiedEntityColliders() : null;
   }

   @Override
   public boolean isHiddenInPortal(BlockPos localPos) {
      if (this.notInPortal()) {
         return super.isHiddenInPortal(localPos);
      } else {
         Direction facing = this.assemblyDirection;
         Axis axis = facing.getClockWise().getAxis();
         int coord = axis.choose(localPos.getZ(), localPos.getY(), localPos.getX()) * -facing.getAxisDirection().getStep();
         return !this.withinVisible(coord) || this.atSeam(coord);
      }
   }

   public boolean isHiddenInPortal(int posAlongMovementAxis) {
      return this.notInPortal() ? false : !this.withinVisible(posAlongMovementAxis) || this.atSeam(posAlongMovementAxis);
   }

   public boolean notInPortal() {
      return this.portalCutoffMin == Integer.MIN_VALUE && this.portalCutoffMax == Integer.MAX_VALUE;
   }

   public boolean atSeam(BlockPos localPos) {
      Direction facing = this.assemblyDirection;
      Axis axis = facing.getClockWise().getAxis();
      int coord = axis.choose(localPos.getZ(), localPos.getY(), localPos.getX()) * -facing.getAxisDirection().getStep();
      return this.atSeam(coord);
   }

   public boolean withinVisible(BlockPos localPos) {
      Direction facing = this.assemblyDirection;
      Axis axis = facing.getClockWise().getAxis();
      int coord = axis.choose(localPos.getZ(), localPos.getY(), localPos.getX()) * -facing.getAxisDirection().getStep();
      return this.withinVisible(coord);
   }

   public boolean atSeam(int posAlongMovementAxis) {
      return posAlongMovementAxis == this.portalCutoffMin || posAlongMovementAxis == this.portalCutoffMax;
   }

   public boolean withinVisible(int posAlongMovementAxis) {
      return posAlongMovementAxis > this.portalCutoffMin && posAlongMovementAxis < this.portalCutoffMax;
   }

   @Override
   public MountedStorageManager getStorage() {
      return this.storageProxy == null ? fallbackStorage : this.storageProxy;
   }

   @Override
   public void writeStorage(CompoundTag nbt, Provider registries, boolean spawnPacket) {
      if (spawnPacket) {
         if (this.storageProxy != null) {
            this.storageProxy.write(nbt, registries, spawnPacket);
         }
      }
   }

   @Override
   protected ClientContraption createClientContraption() {
      return new CarriageContraption.CarriageClientContraption(this);
   }

   static {
      fallbackStorage.initialize();
   }

   public class CarriageClientContraption extends ClientContraption {
      public final BitSet scratchBlockEntitiesOutsidePortal = new BitSet();

      public CarriageClientContraption(CarriageContraption contraption) {
         super(contraption);
      }

      @Override
      public ClientContraption.RenderedBlocks getRenderedBlocks() {
         if (CarriageContraption.this.notInPortal()) {
            return super.getRenderedBlocks();
         } else {
            Map<BlockPos, BlockState> values = new HashMap<>();
            CarriageContraption.this.blocks.forEach((pos, info) -> {
               if (CarriageContraption.this.withinVisible(pos)) {
                  values.put(pos, info.state());
               } else if (CarriageContraption.this.atSeam(pos)) {
                  values.put(pos, Blocks.PURPLE_STAINED_GLASS.defaultBlockState());
               }
            });
            return new ClientContraption.RenderedBlocks(pos -> values.getOrDefault(pos, Blocks.AIR.defaultBlockState()), values.keySet());
         }
      }

      @Override
      public BlockEntity readBlockEntity(Level level, StructureBlockInfo info, boolean legacy) {
         if (info.state().getBlock() instanceof AbstractBogeyBlock<?> bogey && !bogey.captureBlockEntityForTrain()) {
            return null;
         }

         return super.readBlockEntity(level, info, legacy);
      }

      @Override
      public BitSet getAndAdjustShouldRenderBlockEntities() {
         if (CarriageContraption.this.notInPortal()) {
            return super.getAndAdjustShouldRenderBlockEntities();
         } else {
            this.scratchBlockEntitiesOutsidePortal.clear();
            this.scratchBlockEntitiesOutsidePortal.or(this.shouldRenderBlockEntities);

            for (int i = 0; i < this.renderedBlockEntityView.size(); i++) {
               BlockEntity be = this.renderedBlockEntityView.get(i);
               if (CarriageContraption.this.isHiddenInPortal(be.getBlockPos())) {
                  this.scratchBlockEntitiesOutsidePortal.clear(i);
               }
            }

            return this.scratchBlockEntitiesOutsidePortal;
         }
      }
   }
}

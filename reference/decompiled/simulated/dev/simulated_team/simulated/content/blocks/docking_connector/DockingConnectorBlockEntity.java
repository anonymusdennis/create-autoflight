package dev.simulated_team.simulated.content.blocks.docking_connector;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintHandle;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.simulated_team.simulated.compat.computercraft.wired.DockingConnectorWiredElement;
import dev.simulated_team.simulated.content.blocks.redstone_magnet.MagnetBehaviour;
import dev.simulated_team.simulated.content.blocks.redstone_magnet.MagnetMap;
import dev.simulated_team.simulated.content.blocks.redstone_magnet.MagnetPair;
import dev.simulated_team.simulated.content.blocks.redstone_magnet.MagnetPairIdentifier;
import dev.simulated_team.simulated.content.blocks.redstone_magnet.RedstoneMagnetBlock;
import dev.simulated_team.simulated.content.blocks.redstone_magnet.SimMagnet;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimSoundEvents;
import dev.simulated_team.simulated.multiloader.inventory.AbstractContainer;
import dev.simulated_team.simulated.service.SimConfigService;
import dev.simulated_team.simulated.util.SimMathUtils;
import dev.simulated_team.simulated.util.SimMovementContext;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class DockingConnectorBlockEntity extends SmartBlockEntity implements SimMagnet, BlockEntitySubLevelActor, Clearable {
   public static MagnetMap<DockingConnectorBlockEntity> MAGNET_CONTROLLER = new MagnetMap();
   public boolean powered;
   public LerpedFloat extension = LerpedFloat.linear().chase(0.0, 0.1, Chaser.LINEAR);
   public LerpedFloat feet = LerpedFloat.linear().chase(0.0, 0.15, Chaser.LINEAR);
   public DockingConnectorSoloInventory inventory;
   public DockingConnectorTank tank;
   public DockingConnectorBattery battery;
   public BlockPos otherConnectorPosition = null;
   public UUID otherConnectorSubLevelId = null;
   protected DockingConnectorBlockEntity.DockingConnectorState state = DockingConnectorBlockEntity.DockingConnectorState.UNPOWERED;
   protected boolean virtualLock = false;
   protected double closestPairDistance = 0.0;
   private MagnetBehaviour magnetBehaviour;
   private FixedConstraintHandle constraintHandle;
   public final DockingConnectorWiredElement ccWiredElement;
   private DockingConnectorBlockEntity.ConstraintSmoother constraintSmoother = null;

   public DockingConnectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.inventory = new DockingConnectorSoloInventory();
      this.tank = new DockingConnectorTank(this);
      this.battery = new DockingConnectorBattery(
         (Integer)SimConfigService.INSTANCE.server().blocks.dockingConnectorFECapacity.get(),
         (Integer)SimConfigService.INSTANCE.server().blocks.dockingConnectorFEThroughput.get()
      );
      this.ccWiredElement = DockingConnectorWiredElement.create(this);
   }

   @Nullable
   public DockingConnectorBlockEntity getOtherConnector() {
      if (this.otherConnectorPosition == null) {
         return null;
      } else {
         return this.level.getBlockEntity(this.otherConnectorPosition) instanceof DockingConnectorBlockEntity be ? be : null;
      }
   }

   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      this.magnetBehaviour = new MagnetBehaviour(this, MAGNET_CONTROLLER);
      behaviours.add(this.magnetBehaviour);
   }

   public void initialize() {
      super.initialize();
      DockingConnectorBlockEntity otherConnector = this.getOtherConnector();
      if (otherConnector != null && this.constraintHandle == null && otherConnector.constraintHandle == null) {
         MagnetMap<DockingConnectorBlockEntity> controller = MAGNET_CONTROLLER;
         if (controller.getPair(this.level, this.getBlockPos(), this.otherConnectorPosition) == null) {
            controller.tryAddPair(this.level, this.getBlockPos(), this.otherConnectorPosition, DockingConnectorPair::new);
            DockingConnectorPair pair = (DockingConnectorPair)controller.getPair(this.level, this.getBlockPos(), this.otherConnectorPosition);
            if (pair != null) {
               pair.dock(true);
               this.notifyUpdate();
            }
         }
      }
   }

   public void tick() {
      super.tick();
      BlockState state = this.getBlockState();
      Direction direction = (Direction)state.getValue(BlockStateProperties.FACING);
      BlockPos pos = this.getBlockPos();
      BlockPos frontPos = pos.relative(direction);
      BlockState frontState = this.level.getBlockState(frontPos);
      this.powered = (Boolean)state.getValue(BlockStateProperties.POWERED);
      if (!frontState.isAir()
         && (
            !frontState.is((Block)SimBlocks.PAIRED_DOCKING_CONNECTOR.get())
               || ((Direction)frontState.getValue(BlockStateProperties.FACING)).getOpposite() != direction
         )) {
         this.powered = false;
      }

      if (!this.level.isClientSide() && this.isExtended()) {
         this.searchForPairs();
         if (!frontState.is((Block)SimBlocks.PAIRED_DOCKING_CONNECTOR.get())) {
            this.level
               .setBlock(
                  frontPos,
                  (BlockState)((PairedDockingConnectorBlock)SimBlocks.PAIRED_DOCKING_CONNECTOR.get())
                     .defaultBlockState()
                     .setValue(BlockStateProperties.FACING, direction.getOpposite()),
                  3
               );
         }
      }

      if (this.otherConnectorPosition != null
         && !this.level.isClientSide
         && (
            !(this.level.getBlockEntity(this.otherConnectorPosition) instanceof DockingConnectorBlockEntity be)
               || !Objects.equals(be.otherConnectorPosition, this.getBlockPos())
         )) {
         this.unDock();
         this.state = DockingConnectorBlockEntity.DockingConnectorState.EXTENDED;
         this.sendData();
      }

      float previousExtensionTarget = this.extension.getChaseTarget();
      this.extension.updateChaseTarget(this.powered ? 1.0F : 0.0F);
      if (this.extension.getChaseTarget() != previousExtensionTarget) {
         if (this.level.isClientSide()) {
            this.level
               .playLocalSound(
                  (double)pos.getX() + 0.5,
                  (double)pos.getY() + 0.5,
                  (double)pos.getZ() + 0.5,
                  this.powered ? SimSoundEvents.DOCKING_CONNECTOR_EXTENDS.event() : SimSoundEvents.DOCKING_CONNECTOR_RETRACTS.event(),
                  SoundSource.BLOCKS,
                  0.75F,
                  1.0F,
                  false
               );
         } else if (!this.powered && frontState.is((Block)SimBlocks.PAIRED_DOCKING_CONNECTOR.get())) {
            this.level.setBlock(frontPos, Blocks.AIR.defaultBlockState(), 3);
         }
      }

      boolean previousExtended = this.isExtended();
      this.extension.tickChaser();
      if (previousExtended != this.isExtended()) {
         this.level.setBlock(this.getBlockPos(), (BlockState)this.getBlockState().setValue(DockingConnectorBlock.EXTENDED, this.isExtended()), 6);
      }

      float previousFeetValue = this.feet.getValue();
      this.feet.updateChaseTarget(!this.hasOtherConnector() && !this.virtualLock ? 0.0F : 1.0F);
      this.feet.tickChaser();
      this.updateState();
   }

   public void lazyTick() {
      super.lazyTick();
      if (this.state == DockingConnectorBlockEntity.DockingConnectorState.EXTENDED || this.state == DockingConnectorBlockEntity.DockingConnectorState.LOCKING) {
         this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
      }
   }

   public void setVirtualLock(boolean lock) {
      this.virtualLock = lock;
   }

   private void removeConstraint() {
      if (this.constraintHandle != null) {
         this.constraintHandle.remove();
         this.constraintHandle = null;
      }

      this.constraintSmoother = null;
   }

   private void attachConstraints(
      DockingConnectorBlockEntity other, Quaterniondc targetOrientation, Vector3dc relativePos, Quaterniondc relativeOrientation, boolean isLocked
   ) {
      BlockPos pos = this.getBlockPos();
      BlockPos otherPos = other.getBlockPos();
      ServerSubLevel thisSubLevel = (ServerSubLevel)Sable.HELPER.getContaining(this.level, pos);
      ServerSubLevel otherSubLevel = (ServerSubLevel)Sable.HELPER.getContaining(this.level, otherPos);

      assert thisSubLevel != null;

      Vector3d anchorPos = JOMLConversion.toJOML(this.getTipPosition());
      Vector3d otherAnchorPos = JOMLConversion.toJOML(other.getTipPosition());
      ServerSubLevelContainer container = SubLevelContainer.getContainer((ServerLevel)this.level);
      SubLevelPhysicsSystem physicsSystem = container.physicsSystem();
      double partialPhysicsTick = physicsSystem.getPartialPhysicsTick();
      double physicsTime = (double)this.feet.getValue((float)partialPhysicsTick);
      double lerpFactor = Mth.clamp(physicsTime * physicsTime, 0.0, 1.0);
      if (isLocked) {
         lerpFactor = 1.0;
      }

      double rotationLerpFactor = Mth.clamp(lerpFactor * 2.0, 0.0, 1.0);
      if (this.constraintHandle != null) {
         this.constraintHandle.remove();
      }

      otherAnchorPos.fma(1.0 - lerpFactor, relativePos);
      FixedConstraintConfiguration constraint = new FixedConstraintConfiguration(
         anchorPos, otherAnchorPos, relativeOrientation.slerp(targetOrientation, rotationLerpFactor, new Quaterniond())
      );
      this.constraintHandle = (FixedConstraintHandle)container.physicsSystem().getPipeline().addConstraint(thisSubLevel, otherSubLevel, constraint);
   }

   private void searchForPairs() {
      Direction direction = (Direction)this.getBlockState().getValue(BlockStateProperties.FACING);
      MagnetMap<DockingConnectorBlockEntity> controller = MAGNET_CONTROLLER;
      if (this.hasOtherConnector()) {
         controller.tryAddPair(this.level, this.getBlockPos(), this.otherConnectorPosition, DockingConnectorPair::new);
      } else {
         Vector3d tempRelativePos = new Vector3d();
         this.closestPairDistance = Double.MAX_VALUE;
         SubLevel subLevel = Sable.HELPER.getContaining(this);
         SimMovementContext context = SimMovementContext.getMovementContext(this.level, this.getBlockPos().getCenter());

         for (SimMovementContext movementContext : controller.findNearby(context)) {
            if (movementContext.subLevel() != subLevel) {
               BlockEntity var10 = this.level.getBlockEntity(movementContext.localBlockPos());
               if (var10 instanceof DockingConnectorBlockEntity) {
                  DockingConnectorBlockEntity other = (DockingConnectorBlockEntity)var10;
                  if (!other.hasOtherConnector() && other.magnetActive()) {
                     controller.tryAddPair(this.level, this.getBlockPos(), movementContext.localBlockPos(), DockingConnectorPair::new);
                     DockingConnectorPair.getRelativePosition(this, other, tempRelativePos);
                     this.closestPairDistance = Math.min(tempRelativePos.length(), this.closestPairDistance);
                  }
               }
            }
         }

         BlockPos sameGridConnection = this.getBlockPos().offset(direction.getNormal().multiply(3));
         if (this.isExtended()
            && this.level.getBlockEntity(sameGridConnection) instanceof DockingConnectorBlockEntity other
            && ((Direction)other.getBlockState().getValue(BlockStateProperties.FACING)).getOpposite() == direction
            && other.isExtended()) {
            controller.tryAddPair(this.level, this.getBlockPos(), other.getBlockPos(), DockingConnectorPair::new);
         }
      }
   }

   private void updateState() {
      if (this.powered) {
         if (this.state != DockingConnectorBlockEntity.DockingConnectorState.LOCKED && this.isExtended()) {
            this.state = this.hasOtherConnector()
               ? DockingConnectorBlockEntity.DockingConnectorState.LOCKING
               : DockingConnectorBlockEntity.DockingConnectorState.EXTENDED;
         }
      } else {
         if (this.state != DockingConnectorBlockEntity.DockingConnectorState.UNPOWERED && this.hasOtherConnector()) {
            MagnetMap<DockingConnectorBlockEntity> controller = MAGNET_CONTROLLER;
            Map<MagnetPairIdentifier, MagnetPair<DockingConnectorBlockEntity>> map = controller.pairMap.get(this.level);
            if (map != null && map.get(new MagnetPairIdentifier(this.otherConnectorPosition, this.getBlockPos())) instanceof DockingConnectorPair pair) {
               pair.unDock();
            }
         }

         this.state = DockingConnectorBlockEntity.DockingConnectorState.UNPOWERED;
      }
   }

   public void pairTo(DockingConnectorBlockEntity other) {
      if (!other.getBlockPos().equals(this.otherConnectorPosition)) {
         DockingConnectorBlockEntity otherConnector = this.getOtherConnector();
         if (otherConnector != null && this.getBlockPos().equals(otherConnector.otherConnectorPosition)) {
            otherConnector.unDock();
         }

         this.unDock();
         MagnetMap<DockingConnectorBlockEntity> controller = MAGNET_CONTROLLER;
         controller.tryAddPair(this.level, this.getBlockPos(), other.getBlockPos(), DockingConnectorPair::new);
         DockingConnectorPair pair = (DockingConnectorPair)controller.getPair(this.level, this.getBlockPos(), other.getBlockPos());
         if (pair != null) {
            pair.dock(true);
            this.notifyUpdate();
         }
      }
   }

   public boolean isLocked() {
      return this.state == DockingConnectorBlockEntity.DockingConnectorState.LOCKED;
   }

   public float getPlateOffset() {
      return 0.5F + this.getExtensionDistance(0.0F);
   }

   public boolean isExtended() {
      return this.extension.getValue() == 1.0F && this.powered;
   }

   public boolean isRetracted() {
      return this.extension.getValue() == 0.0F;
   }

   public boolean isFeetExtended() {
      return this.otherConnectorPosition != null && this.feet.getValue() == 1.0F;
   }

   public boolean hasOtherConnector() {
      return this.otherConnectorPosition != null;
   }

   public float getExtensionDistance(float partialTick) {
      return SimMathUtils.smoothStep(this.extension.getValue(partialTick));
   }

   public float getFeetRotation(float partialTick) {
      float rotation = this.feet.getValue(partialTick);
      float rotationTarget = this.feet.getChaseTarget();
      if (rotationTarget == 1.0F) {
         rotation *= rotation;
      }

      return rotation;
   }

   public void setDock(
      DockingConnectorBlockEntity otherConnector,
      boolean isLocked,
      @Nullable Quaterniondc targetOrientation,
      Vector3dc relativePos,
      Quaterniondc relativeOrientation
   ) {
      BlockPos previous = this.otherConnectorPosition;
      SubLevel otherSubLevel = Sable.HELPER.getContaining(otherConnector);
      this.otherConnectorPosition = otherConnector.getBlockPos();
      this.otherConnectorSubLevelId = otherSubLevel != null ? otherSubLevel.getUniqueId() : null;
      this.updateState();
      if (this.state == DockingConnectorBlockEntity.DockingConnectorState.LOCKING) {
         if (targetOrientation != null && this.constraintSmoother == null) {
            this.constraintSmoother = new DockingConnectorBlockEntity.ConstraintSmoother(otherConnector, targetOrientation, relativePos, relativeOrientation);
         }

         if (isLocked) {
            this.state = DockingConnectorBlockEntity.DockingConnectorState.LOCKED;
            this.tank.connect(this.otherConnectorPosition, otherConnector.tank);
            this.battery.connect(otherConnector.battery);
            this.ccWiredElement.connect(otherConnector.ccWiredElement);
            this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
            if (this.constraintSmoother != null) {
               ServerSubLevelContainer container = SubLevelContainer.getContainer((ServerLevel)this.level);
               this.constraintSmoother.step(container, this, 1.0);
            }

            this.constraintSmoother = null;
         }
      }

      if (previous != this.otherConnectorPosition) {
         if (targetOrientation == null) {
            this.removeConstraint();
         }

         this.sendData();
      }
   }

   public void unDock() {
      DockingConnectorBlockEntity otherConnector = this.getOtherConnector();
      if (otherConnector != null) {
         this.ccWiredElement.disconnect(otherConnector.ccWiredElement);
      }

      this.closestPairDistance = Double.MAX_VALUE;
      this.otherConnectorSubLevelId = null;
      this.otherConnectorPosition = null;
      this.state = this.isExtended() ? DockingConnectorBlockEntity.DockingConnectorState.EXTENDED : DockingConnectorBlockEntity.DockingConnectorState.UNPOWERED;
      this.tank.disconnect();
      this.battery.disconnect();
      this.removeConstraint();
      this.sendData();
      this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
   }

   public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
      if (this.constraintSmoother != null) {
         this.constraintSmoother.partialStep(this);
      }
   }

   public void updateSignal() {
      boolean shouldPower = this.level.hasNeighborSignal(this.worldPosition);
      if (this.powered != shouldPower) {
         this.powered = shouldPower;
         this.sendData();
      }
   }

   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      tag.putBoolean("IsPowered", this.powered);
      tag.putFloat("Extension", this.extension.getValue());
      tag.putFloat("Target", this.extension.getChaseTarget());
      tag.putFloat("Feet", this.feet.getValue());
      if (this.otherConnectorPosition != null) {
         tag.put("OtherConnector", NbtUtils.writeBlockPos(this.otherConnectorPosition));
      }

      if (this.otherConnectorSubLevelId != null) {
         tag.putUUID("OtherConnectorSubLevelId", this.otherConnectorSubLevelId);
      }

      tag.put("Inventory", this.inventory.write(registries));
      tag.put("Tank", this.tank.write());
      tag.put("Battery", this.battery.write());
      super.write(tag, registries, clientPacket);
   }

   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      this.powered = tag.getBoolean("IsPowered");
      this.extension.setValue((double)tag.getFloat("Extension"));
      this.extension.updateChaseTarget(tag.getFloat("Target"));
      this.feet.setValue((double)tag.getFloat("Feet"));
      this.extension.setValue((double)this.extension.getValue());
      this.feet.setValue((double)this.feet.getValue());
      if (tag.contains("OtherConnector")) {
         this.otherConnectorPosition = (BlockPos)NbtUtils.readBlockPos(tag, "OtherConnector").orElse(null);
      } else {
         this.otherConnectorPosition = null;
      }

      if (tag.contains("OtherConnectorSubLevelId")) {
         this.otherConnectorSubLevelId = tag.getUUID("OtherConnectorSubLevelId");
      }

      this.inventory.read(registries, tag.getCompound("Inventory"));
      this.tank.read(tag.getCompound("Tank"));
      this.battery.read(tag.getCompound("Battery"));
      super.read(tag, registries, clientPacket);
   }

   public boolean triggerEvent(int id, int type) {
      if (id == 1) {
         this.extension.updateChaseTarget(this.powered ? 1.0F : 0.0F);
         this.feet.updateChaseTarget(this.hasOtherConnector() ? 1.0F : 0.0F);
         return true;
      } else {
         return super.triggerEvent(id, type);
      }
   }

   public void remove() {
      super.remove();
      this.removeConstraint();
      if (this.level == null || !this.level.isClientSide) {
         this.ccWiredElement.remove();
      }
   }

   @Override
   public Quaternionfc getOrientation() {
      return ((Direction)this.getBlockState().getValue(BlockStateProperties.FACING)).getRotation();
   }

   @Override
   public SubLevel getLatestSubLevel() {
      return Sable.HELPER.getContaining(this);
   }

   @Override
   public Vector3d setMagneticMoment(Vector3d v) {
      v.set(JOMLConversion.toJOML(Vec3.atLowerCornerOf(((Direction)this.getBlockState().getValue(DockingConnectorBlock.FACING)).getNormal())));
      v.mul(Math.sqrt((Double)SimConfigService.INSTANCE.server().physics.dockingConnectorStrength.get()));
      return v;
   }

   @Override
   public Vec3 getMagnetPosition() {
      return Vec3.atCenterOf(this.getBlockPos())
         .add(Vec3.atLowerCornerOf(((Direction)this.getBlockState().getValue(RedstoneMagnetBlock.FACING)).getNormal()).scale(1.4));
   }

   public Vec3 getTipPosition() {
      return Vec3.atCenterOf(this.getBlockPos())
         .add(Vec3.atLowerCornerOf(((Direction)this.getBlockState().getValue(RedstoneMagnetBlock.FACING)).getNormal()).scale(1.5));
   }

   @Override
   public boolean magnetActive() {
      return this.isExtended() && this.constraintHandle == null;
   }

   public AABB getBoundingBox(BlockState state) {
      return Shulker.getProgressAabb(1.0F, (Direction)state.getValue(ShulkerBoxBlock.FACING), this.getExtensionDistance(1.0F));
   }

   public AABB createRenderBoundingBox() {
      return super.createRenderBoundingBox().inflate(1.0);
   }

   @Nullable
   public Iterable<SubLevel> sable$getConnectionDependencies() {
      if (this.otherConnectorSubLevelId == null) {
         return null;
      } else {
         SubLevelContainer container = SubLevelContainer.getContainer(this.level);
         SubLevel otherSubLevel = container.getSubLevel(this.otherConnectorSubLevelId);
         return otherSubLevel == null ? null : List.of(otherSubLevel);
      }
   }

   public void clearContent() {
      this.inventory.clearContent();
   }

   public AbstractContainer getInventory() {
      DockingConnectorBlockEntity other = this.getOtherConnector();
      if (other != null) {
         this.inventory.dock();
         return new DockingConnectorDuoInventory(this, other);
      } else {
         this.inventory.unDock();
         return this.inventory;
      }
   }

   private static record ConstraintSmoother(
      BlockPos otherConnectorPos, Quaterniond targetRelativeOrientation, Vector3d initialRelativePosition, Quaterniond initialRelativeOrientation
   ) {
      private ConstraintSmoother(
         DockingConnectorBlockEntity otherConnectorPos,
         Quaterniondc targetRelativeOrientation,
         Vector3dc initialRelativePosition,
         Quaterniondc initialRelativeOrientation
      ) {
         this(
            otherConnectorPos.getBlockPos(),
            new Quaterniond(targetRelativeOrientation),
            new Vector3d(initialRelativePosition),
            new Quaterniond(initialRelativeOrientation)
         );
      }

      public void partialStep(DockingConnectorBlockEntity connector) {
         ServerSubLevelContainer container = SubLevelContainer.getContainer((ServerLevel)connector.level);
         SubLevelPhysicsSystem physicsSystem = container.physicsSystem();
         double partialPhysicsTick = physicsSystem.getPartialPhysicsTick();
         double physicsTime = (double)connector.feet.getValue((float)partialPhysicsTick);
         double lerpFactor = Mth.clamp(physicsTime * physicsTime, 0.0, 1.0);
         this.step(container, connector, lerpFactor);
      }

      public void step(ServerSubLevelContainer container, DockingConnectorBlockEntity connector, double lerpFactor) {
         BlockPos pos = connector.getBlockPos();
         if (connector.level.getBlockEntity(this.otherConnectorPos) instanceof DockingConnectorBlockEntity other) {
            ServerSubLevel thisSubLevel = (ServerSubLevel)Sable.HELPER.getContaining(connector.level, pos);
            ServerSubLevel otherSubLevel = (ServerSubLevel)Sable.HELPER.getContaining(connector.level, this.otherConnectorPos);

            assert thisSubLevel != null;

            Vector3d anchorPos = JOMLConversion.toJOML(connector.getTipPosition());
            Vector3d otherAnchorPos = JOMLConversion.toJOML(other.getTipPosition());
            double rotationLerpFactor = Mth.clamp(lerpFactor * 2.0, 0.0, 1.0);
            if (connector.constraintHandle != null) {
               connector.constraintHandle.remove();
            }

            otherAnchorPos.fma(1.0 - lerpFactor, this.initialRelativePosition);
            FixedConstraintConfiguration constraint = new FixedConstraintConfiguration(
               anchorPos, otherAnchorPos, this.initialRelativeOrientation.slerp(this.targetRelativeOrientation, rotationLerpFactor, new Quaterniond())
            );
            connector.constraintHandle = (FixedConstraintHandle)container.physicsSystem().getPipeline().addConstraint(thisSubLevel, otherSubLevel, constraint);
         }
      }
   }

   public static enum DockingConnectorState {
      UNPOWERED,
      EXTENDED,
      LOCKING,
      LOCKED;
   }
}

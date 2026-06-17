package com.simibubi.create.content.decoration.slidingDoor;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.elevator.ElevatorColumn;
import com.simibubi.create.content.contraptions.elevator.ElevatorContraption;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.lang.ref.WeakReference;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;

public class SlidingDoorMovementBehaviour implements MovementBehaviour {
   @Override
   public boolean mustTickWhileDisabled() {
      return true;
   }

   @Override
   public void tick(MovementContext context) {
      StructureBlockInfo structureBlockInfo = context.contraption.getBlocks().get(context.localPos);
      if (structureBlockInfo != null) {
         boolean open = SlidingDoorBlockEntity.isOpen(structureBlockInfo.state());
         if (!context.world.isClientSide()) {
            this.tickOpen(context, open);
         }

         if (context.contraption.getBlockEntityClientSide(context.localPos) instanceof SlidingDoorBlockEntity sdbe) {
            boolean var6 = sdbe.animation.settled();
            sdbe.animation.chase(open ? 1.0 : 0.0, 0.15F, Chaser.LINEAR);
            sdbe.animation.tickChaser();
            if (!var6 && sdbe.animation.settled() && !open) {
               context.world
                  .playLocalSound(
                     context.position.x, context.position.y, context.position.z, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.125F, 1.0F, false
                  );
            }
         }
      }
   }

   protected void tickOpen(MovementContext context, boolean currentlyOpen) {
      boolean shouldOpen = this.shouldOpen(context);
      if (this.shouldUpdate(context, shouldOpen)) {
         if (currentlyOpen != shouldOpen) {
            BlockPos pos = context.localPos;
            Contraption contraption = context.contraption;
            StructureBlockInfo info = contraption.getBlocks().get(pos);
            if (info != null && info.state().hasProperty(DoorBlock.OPEN)) {
               this.toggleDoor(pos, contraption, info);
               Direction facing = this.getDoorFacing(context);
               BlockPos inWorldDoor = BlockPos.containing(context.position).relative(facing);
               BlockState inWorldDoorState = context.world.getBlockState(inWorldDoor);
               if (inWorldDoorState.getBlock() instanceof DoorBlock db
                  && inWorldDoorState.hasProperty(DoorBlock.OPEN)
                  && inWorldDoorState.hasProperty(DoorBlock.FACING)
                  && inWorldDoorState.getOptionalValue(DoorBlock.FACING).orElse(Direction.UP).getAxis() == facing.getAxis()) {
                  db.setOpen(null, context.world, inWorldDoorState, inWorldDoor, shouldOpen);
               }

               if (shouldOpen) {
                  context.world.playSound(null, BlockPos.containing(context.position), SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 0.125F, 1.0F);
               }
            }
         }
      }
   }

   private void toggleDoor(BlockPos pos, Contraption contraption, StructureBlockInfo info) {
      BlockState newState = (BlockState)info.state().cycle(DoorBlock.OPEN);
      contraption.entity.setBlock(pos, new StructureBlockInfo(info.pos(), newState, info.nbt()));
      BlockPos otherPos = newState.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
      info = contraption.getBlocks().get(otherPos);
      if (info != null && info.state().hasProperty(DoorBlock.OPEN)) {
         newState = (BlockState)info.state().cycle(DoorBlock.OPEN);
         contraption.entity.setBlock(otherPos, new StructureBlockInfo(info.pos(), newState, info.nbt()));
         contraption.invalidateColliders();
      }
   }

   protected boolean shouldUpdate(MovementContext context, boolean shouldOpen) {
      if (context.firstMovement && shouldOpen) {
         return false;
      } else if (!context.data.contains("Open")) {
         context.data.putBoolean("Open", shouldOpen);
         return true;
      } else {
         boolean wasOpen = context.data.getBoolean("Open");
         context.data.putBoolean("Open", shouldOpen);
         return wasOpen != shouldOpen;
      }
   }

   protected boolean shouldOpen(MovementContext context) {
      if (context.disabled) {
         return false;
      } else {
         Contraption contraption;
         boolean var10000;
         label56: {
            contraption = context.contraption;
            label46:
            if (!(context.motion.length() < 0.0078125) || contraption.entity.isStalled()) {
               if (contraption instanceof ElevatorContraption ec && ec.arrived) {
                  break label46;
               }

               var10000 = false;
               break label56;
            }

            var10000 = true;
         }

         boolean canOpen = var10000;
         if (!canOpen) {
            context.temporaryData = null;
            return false;
         } else {
            if (context.temporaryData instanceof WeakReference<?> wr
               && wr.get() instanceof DoorControlBehaviour dcb
               && dcb.blockEntity != null
               && !dcb.blockEntity.isRemoved()) {
               return this.shouldOpenAt(dcb, context);
            }

            context.temporaryData = null;
            DoorControlBehaviour doorControls = null;
            if (contraption instanceof ElevatorContraption ec) {
               doorControls = this.getElevatorDoorControl(ec, context);
            }

            if (context.contraption.entity instanceof CarriageContraptionEntity cce) {
               doorControls = this.getTrainStationDoorControl(cce, context);
            }

            if (doorControls == null) {
               return false;
            } else {
               context.temporaryData = new WeakReference<>(doorControls);
               return this.shouldOpenAt(doorControls, context);
            }
         }
      }
   }

   protected boolean shouldOpenAt(DoorControlBehaviour controller, MovementContext context) {
      if (controller.mode == DoorControl.ALL) {
         return true;
      } else {
         return controller.mode == DoorControl.NONE ? false : controller.mode.matches(this.getDoorFacing(context));
      }
   }

   protected DoorControlBehaviour getElevatorDoorControl(ElevatorContraption ec, MovementContext context) {
      Integer currentTargetY = ec.getCurrentTargetY(context.world);
      if (currentTargetY == null) {
         return null;
      } else {
         ElevatorColumn.ColumnCoords columnCoords = ec.getGlobalColumn();
         if (columnCoords == null) {
            return null;
         } else {
            ElevatorColumn elevatorColumn = ElevatorColumn.get(context.world, columnCoords);
            return elevatorColumn == null ? null : BlockEntityBehaviour.get(context.world, elevatorColumn.contactAt(currentTargetY), DoorControlBehaviour.TYPE);
         }
      }
   }

   protected DoorControlBehaviour getTrainStationDoorControl(CarriageContraptionEntity cce, MovementContext context) {
      Carriage carriage = cce.getCarriage();
      if (carriage != null && carriage.train != null) {
         GlobalStation currentStation = carriage.train.getCurrentStation();
         if (currentStation == null) {
            return null;
         } else {
            BlockPos stationPos = currentStation.getBlockEntityPos();
            ResourceKey<Level> stationDim = currentStation.getBlockEntityDimension();
            MinecraftServer server = context.world.getServer();
            if (server == null) {
               return null;
            } else {
               ServerLevel stationLevel = server.getLevel(stationDim);
               return stationLevel != null && stationLevel.isLoaded(stationPos)
                  ? BlockEntityBehaviour.get(stationLevel, stationPos, DoorControlBehaviour.TYPE)
                  : null;
            }
         }
      } else {
         return null;
      }
   }

   protected Direction getDoorFacing(MovementContext context) {
      Direction stateFacing = (Direction)context.state.getValue(DoorBlock.FACING);
      Direction originalFacing = Direction.get(AxisDirection.POSITIVE, stateFacing.getAxis());
      Vec3 centerOfContraption = context.contraption.bounds.getCenter();
      Vec3 diff = Vec3.atCenterOf(context.localPos).add(Vec3.atLowerCornerOf(stateFacing.getNormal()).scale(-0.45F)).subtract(centerOfContraption);
      if (originalFacing.getAxis().choose(diff.x, diff.y, diff.z) < 0.0) {
         originalFacing = originalFacing.getOpposite();
      }

      Vec3 directionVec = Vec3.atLowerCornerOf(originalFacing.getNormal());
      directionVec = context.rotation.apply(directionVec);
      return Direction.getNearest(directionVec.x, directionVec.y, directionVec.z);
   }
}

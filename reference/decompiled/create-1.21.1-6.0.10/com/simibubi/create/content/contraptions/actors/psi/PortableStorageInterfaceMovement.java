package com.simibubi.create.content.contraptions.actors.psi;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import java.util.Optional;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public class PortableStorageInterfaceMovement implements MovementBehaviour {
   static final String _workingPos_ = "WorkingPos";
   static final String _clientPrevPos_ = "ClientPrevPos";

   @Override
   public Vec3 getActiveAreaOffset(MovementContext context) {
      return Vec3.atLowerCornerOf(((Direction)context.state.getValue(PortableStorageInterfaceBlock.FACING)).getNormal()).scale(1.85F);
   }

   @Override
   public boolean disableBlockEntityRendering() {
      return true;
   }

   @Nullable
   @Override
   public ActorVisual createVisual(VisualizationContext visualizationContext, VirtualRenderWorld simulationWorld, MovementContext movementContext) {
      return new PSIActorVisual(visualizationContext, simulationWorld, movementContext);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffer) {
      if (!VisualizationManager.supportsVisualization(context.world)) {
         PortableStorageInterfaceRenderer.renderInContraption(context, renderWorld, matrices, buffer);
      }
   }

   @Override
   public void visitNewPosition(MovementContext context, BlockPos pos) {
      boolean onCarriage = context.contraption instanceof CarriageContraption;
      if (!onCarriage || !(context.motion.length() > 0.25)) {
         if (!this.findInterface(context, pos)) {
            context.data.remove("WorkingPos");
         }
      }
   }

   @Override
   public void tick(MovementContext context) {
      if (context.world.isClientSide) {
         getAnimation(context).tickChaser();
      }

      boolean onCarriage = context.contraption instanceof CarriageContraption;
      if (!onCarriage || !(context.motion.length() > 0.25)) {
         if (context.world.isClientSide) {
            BlockPos pos = BlockPos.containing(context.position);
            if (!this.findInterface(context, pos)) {
               this.reset(context);
            }
         } else if (!context.data.contains("WorkingPos")) {
            if (context.stall) {
               this.cancelStall(context);
            }
         } else {
            BlockPos pos = NBTHelper.readBlockPos(context.data, "WorkingPos");
            Vec3 target = VecHelper.getCenterOf(pos);
            if (!context.stall && !onCarriage && context.position.closerThan(target, target.distanceTo(context.position.add(context.motion)))) {
               context.stall = true;
            }

            Optional<Direction> currentFacingIfValid = this.getCurrentFacingIfValid(context);
            if (!currentFacingIfValid.isPresent()) {
               this.reset(context);
            } else {
               PortableStorageInterfaceBlockEntity stationaryInterface = this.getStationaryInterfaceAt(
                  context.world, pos, context.state, currentFacingIfValid.get()
               );
               if (stationaryInterface == null) {
                  this.reset(context);
               } else {
                  if (stationaryInterface.connectedEntity == null) {
                     stationaryInterface.startTransferringTo(context.contraption, stationaryInterface.distance);
                  }

                  boolean timerBelow = stationaryInterface.transferTimer <= 4;
                  stationaryInterface.keepAlive = 2;
                  if (context.stall && timerBelow) {
                     context.stall = false;
                  }
               }
            }
         }
      }
   }

   protected boolean findInterface(MovementContext context, BlockPos pos) {
      if (context.contraption instanceof CarriageContraption cc && !cc.notInPortal()) {
         return false;
      }

      Optional<Direction> currentFacingIfValid = this.getCurrentFacingIfValid(context);
      if (!currentFacingIfValid.isPresent()) {
         return false;
      } else {
         Direction currentFacing = currentFacingIfValid.get();
         PortableStorageInterfaceBlockEntity psi = this.findStationaryInterface(context.world, pos, context.state, currentFacing);
         if (psi == null) {
            return false;
         } else if (psi.isPowered()) {
            return false;
         } else {
            context.data.put("WorkingPos", NbtUtils.writeBlockPos(psi.getBlockPos()));
            if (!context.world.isClientSide) {
               Vec3 diff = VecHelper.getCenterOf(psi.getBlockPos()).subtract(context.position);
               diff = VecHelper.project(diff, Vec3.atLowerCornerOf(currentFacing.getNormal()));
               float distance = (float)(diff.length() + 1.85F - 1.0);
               psi.startTransferringTo(context.contraption, distance);
            } else {
               context.data.put("ClientPrevPos", NbtUtils.writeBlockPos(pos));
               if (context.contraption instanceof CarriageContraption || context.contraption.entity.isStalled() || context.motion.lengthSqr() == 0.0) {
                  getAnimation(context).chase((double)(psi.getConnectionDistance() / 2.0F), 0.25, Chaser.LINEAR);
               }
            }

            return true;
         }
      }
   }

   @Override
   public void stopMoving(MovementContext context) {
   }

   @Override
   public void cancelStall(MovementContext context) {
      this.reset(context);
   }

   public void reset(MovementContext context) {
      context.data.remove("ClientPrevPos");
      context.data.remove("WorkingPos");
      context.stall = false;
      getAnimation(context).chase(0.0, 0.25, Chaser.LINEAR);
   }

   private PortableStorageInterfaceBlockEntity findStationaryInterface(Level world, BlockPos pos, BlockState state, Direction facing) {
      for (int i = 0; i < 2; i++) {
         PortableStorageInterfaceBlockEntity interfaceAt = this.getStationaryInterfaceAt(world, pos.relative(facing, i), state, facing);
         if (interfaceAt != null) {
            return interfaceAt;
         }
      }

      return null;
   }

   private PortableStorageInterfaceBlockEntity getStationaryInterfaceAt(Level world, BlockPos pos, BlockState state, Direction facing) {
      if (world.getBlockEntity(pos) instanceof PortableStorageInterfaceBlockEntity psi) {
         BlockState blockState = world.getBlockState(pos);
         if (blockState.getBlock() != state.getBlock()) {
            return null;
         } else if (blockState.getValue(PortableStorageInterfaceBlock.FACING) != facing.getOpposite()) {
            return null;
         } else {
            return psi.isPowered() ? null : psi;
         }
      } else {
         return null;
      }
   }

   private Optional<Direction> getCurrentFacingIfValid(MovementContext context) {
      Vec3 directionVec = Vec3.atLowerCornerOf(((Direction)context.state.getValue(PortableStorageInterfaceBlock.FACING)).getNormal());
      directionVec = context.rotation.apply(directionVec);
      Direction facingFromVector = Direction.getNearest(directionVec.x, directionVec.y, directionVec.z);
      return directionVec.distanceTo(Vec3.atLowerCornerOf(facingFromVector.getNormal())) > 0.5 ? Optional.empty() : Optional.of(facingFromVector);
   }

   public static LerpedFloat getAnimation(MovementContext context) {
      LerpedFloat nlf = (LerpedFloat)context.temporaryData;
      if (nlf instanceof LerpedFloat) {
         return nlf;
      } else {
         nlf = LerpedFloat.linear();
         context.temporaryData = nlf;
         return nlf;
      }
   }
}

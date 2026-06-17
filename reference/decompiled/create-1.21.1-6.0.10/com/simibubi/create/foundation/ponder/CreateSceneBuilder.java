package com.simibubi.create.foundation.ponder;

import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;
import com.simibubi.create.content.contraptions.glue.SuperGlueItem;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotationIndicatorParticleData;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.crafter.ConnectedInputHandler;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.kinetics.gauge.SpeedGaugeBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.logistics.funnel.FunnelBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.content.redstone.displayLink.LinkWithBulbBlockEntity;
import com.simibubi.create.content.trains.display.FlapDisplayBlockEntity;
import com.simibubi.create.content.trains.signal.SignalBlockEntity;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.ponder.element.BeltItemElement;
import com.simibubi.create.foundation.ponder.element.ExpandedParrotElement;
import com.simibubi.create.foundation.ponder.instruction.AnimateBlockEntityInstruction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.createmod.catnip.data.FunctionalHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.ParrotElement;
import net.createmod.ponder.api.element.ParrotPose;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.PonderSceneBuilder;
import net.createmod.ponder.foundation.PonderSceneBuilder.PonderEffectInstructions;
import net.createmod.ponder.foundation.PonderSceneBuilder.PonderSpecialInstructions;
import net.createmod.ponder.foundation.PonderSceneBuilder.PonderWorldInstructions;
import net.createmod.ponder.foundation.element.ElementLinkImpl;
import net.createmod.ponder.foundation.instruction.CreateParrotInstruction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class CreateSceneBuilder extends PonderSceneBuilder {
   private final CreateSceneBuilder.EffectInstructions effects = new CreateSceneBuilder.EffectInstructions();
   private final CreateSceneBuilder.WorldInstructions world = new CreateSceneBuilder.WorldInstructions();
   private final CreateSceneBuilder.SpecialInstructions special = new CreateSceneBuilder.SpecialInstructions();

   public CreateSceneBuilder(SceneBuilder baseSceneBuilder) {
      this(baseSceneBuilder.getScene());
   }

   private CreateSceneBuilder(PonderScene ponderScene) {
      super(ponderScene);
   }

   public CreateSceneBuilder.EffectInstructions effects() {
      return this.effects;
   }

   public CreateSceneBuilder.WorldInstructions world() {
      return this.world;
   }

   public CreateSceneBuilder.SpecialInstructions special() {
      return this.special;
   }

   public class EffectInstructions extends PonderEffectInstructions {
      public EffectInstructions() {
         super(CreateSceneBuilder.this);
      }

      public void superGlue(BlockPos pos, Direction side, boolean fullBlock) {
         CreateSceneBuilder.this.addInstruction(scene -> SuperGlueItem.spawnParticles(scene.getWorld(), pos, side, fullBlock));
      }

      private void rotationIndicator(BlockPos pos, boolean direction, BlockPos displayPos) {
         CreateSceneBuilder.this.addInstruction(
            scene -> {
               BlockState blockState = scene.getWorld().getBlockState(pos);
               BlockEntity blockEntity = scene.getWorld().getBlockEntity(pos);
               if (blockState.getBlock() instanceof KineticBlock kb) {
                  if (blockEntity instanceof KineticBlockEntity kbe) {
                     Axis rotationAxis = kb.getRotationAxis(blockState);
                     float speed = kbe.getTheoreticalSpeed();
                     IRotate.SpeedLevel speedLevel = IRotate.SpeedLevel.of(speed);
                     int color = direction ? (speed > 0.0F ? 15425035 : 1476519) : speedLevel.getColor();
                     int particleSpeed = speedLevel.getParticleSpeed();
                     particleSpeed = (int)((float)particleSpeed * Math.signum(speed));
                     Vec3 location = VecHelper.getCenterOf(displayPos);
                     RotationIndicatorParticleData particleData = new RotationIndicatorParticleData(
                        color, (float)particleSpeed, kb.getParticleInitialRadius(), kb.getParticleTargetRadius(), 20, rotationAxis
                     );

                     for (int i = 0; i < 20; i++) {
                        scene.getWorld().addParticle(particleData, location.x, location.y, location.z, 0.0, 0.0, 0.0);
                     }
                  }
               }
            }
         );
      }

      public void rotationSpeedIndicator(BlockPos pos) {
         this.rotationIndicator(pos, false, pos);
      }

      public void rotationDirectionIndicator(BlockPos pos) {
         this.rotationIndicator(pos, true, pos);
      }
   }

   public class SpecialInstructions extends PonderSpecialInstructions {
      public SpecialInstructions() {
         super(CreateSceneBuilder.this);
      }

      public ElementLink<ParrotElement> createBirb(Vec3 location, Supplier<? extends ParrotPose> pose) {
         ElementLink<ParrotElement> link = new ElementLinkImpl(ParrotElement.class);
         ParrotElement parrot = ExpandedParrotElement.create(location, pose);
         CreateSceneBuilder.this.addInstruction(new CreateParrotInstruction(10, Direction.DOWN, parrot));
         CreateSceneBuilder.this.addInstruction(scene -> scene.linkElement(parrot, link));
         return link;
      }

      public ElementLink<ParrotElement> birbOnTurntable(BlockPos pos) {
         return this.createBirb(VecHelper.getCenterOf(pos), () -> new CreateSceneBuilder.SpecialInstructions.ParrotSpinOnComponentPose(pos));
      }

      public ElementLink<ParrotElement> birbOnSpinnyShaft(BlockPos pos) {
         return this.createBirb(VecHelper.getCenterOf(pos).add(0.0, 0.5, 0.0), () -> new CreateSceneBuilder.SpecialInstructions.ParrotSpinOnComponentPose(pos));
      }

      public void conductorBirb(ElementLink<ParrotElement> birb, boolean conductor) {
         CreateSceneBuilder.this.addInstruction(
            scene -> scene.resolveOptional(birb)
                  .map(FunctionalHelper.filterAndCast(ExpandedParrotElement.class))
                  .ifPresent(expandedBirb -> expandedBirb.setConductor(conductor))
         );
      }

      public static class ParrotSpinOnComponentPose extends ParrotPose {
         private final BlockPos componentPos;

         public ParrotSpinOnComponentPose(BlockPos componentPos) {
            this.componentPos = componentPos;
         }

         public void tick(PonderScene scene, Parrot entity, Vec3 location) {
            BlockEntity blockEntity = scene.getWorld().getBlockEntity(this.componentPos);
            if (blockEntity instanceof KineticBlockEntity) {
               float rpm = ((KineticBlockEntity)blockEntity).getSpeed();
               entity.yRotO = entity.getYRot();
               entity.setYRot(entity.getYRot() + rpm * 0.3F);
            }
         }
      }
   }

   public class WorldInstructions extends PonderWorldInstructions {
      public WorldInstructions() {
         super(CreateSceneBuilder.this);
      }

      public void rotateBearing(BlockPos pos, float angle, int duration) {
         CreateSceneBuilder.this.addInstruction(AnimateBlockEntityInstruction.bearing(pos, angle, duration));
      }

      public void movePulley(BlockPos pos, float distance, int duration) {
         CreateSceneBuilder.this.addInstruction(AnimateBlockEntityInstruction.pulley(pos, distance, duration));
      }

      public void animateBogey(BlockPos pos, float distance, int duration) {
         CreateSceneBuilder.this.addInstruction(AnimateBlockEntityInstruction.bogey(pos, distance, duration + 1));
      }

      public void moveDeployer(BlockPos pos, float distance, int duration) {
         CreateSceneBuilder.this.addInstruction(AnimateBlockEntityInstruction.deployer(pos, distance, duration));
      }

      public void createItemOnBeltLike(BlockPos location, Direction insertionSide, ItemStack stack) {
         CreateSceneBuilder.this.addInstruction(scene -> {
            PonderLevel world = scene.getWorld();
            if (world.getBlockEntity(location) instanceof SmartBlockEntity beltBlockEntity) {
               DirectBeltInputBehaviour behaviour = beltBlockEntity.getBehaviour(DirectBeltInputBehaviour.TYPE);
               if (behaviour != null) {
                  behaviour.handleInsertion(stack, insertionSide.getOpposite(), false);
               }
            }
         });
         this.flapFunnel(location.above(), true);
      }

      public ElementLink<BeltItemElement> createItemOnBelt(BlockPos beltLocation, Direction insertionSide, ItemStack stack) {
         ElementLink<BeltItemElement> link = new ElementLinkImpl(BeltItemElement.class);
         CreateSceneBuilder.this.addInstruction(scene -> {
            PonderLevel world = scene.getWorld();
            if (world.getBlockEntity(beltLocation) instanceof BeltBlockEntity beltBlockEntity) {
               DirectBeltInputBehaviour behaviour = beltBlockEntity.getBehaviour(DirectBeltInputBehaviour.TYPE);
               behaviour.handleInsertion(stack, insertionSide.getOpposite(), false);
               BeltBlockEntity controllerBE = beltBlockEntity.getControllerBE();
               if (controllerBE != null) {
                  controllerBE.tick();
               }

               TransportedItemStackHandlerBehaviour transporter = beltBlockEntity.getBehaviour(TransportedItemStackHandlerBehaviour.TYPE);
               transporter.handleProcessingOnAllItems(tis -> {
                  BeltItemElement tracker = new BeltItemElement(tis);
                  scene.addElement(tracker);
                  scene.linkElement(tracker, link);
                  return TransportedItemStackHandlerBehaviour.TransportedResult.doNothing();
               });
            }
         });
         this.flapFunnel(beltLocation.above(), true);
         return link;
      }

      public void removeItemsFromBelt(BlockPos beltLocation) {
         CreateSceneBuilder.this.addInstruction(scene -> {
            PonderLevel world = scene.getWorld();
            if (world.getBlockEntity(beltLocation) instanceof SmartBlockEntity beltBlockEntity) {
               TransportedItemStackHandlerBehaviour transporter = beltBlockEntity.getBehaviour(TransportedItemStackHandlerBehaviour.TYPE);
               if (transporter != null) {
                  transporter.handleCenteredProcessingOnAllItems(0.52F, tis -> TransportedItemStackHandlerBehaviour.TransportedResult.removeItem());
               }
            }
         });
      }

      public void stallBeltItem(ElementLink<BeltItemElement> link, boolean stalled) {
         CreateSceneBuilder.this.addInstruction(scene -> {
            BeltItemElement resolve = (BeltItemElement)scene.resolve(link);
            if (resolve != null) {
               resolve.ifPresent(tis -> tis.locked = stalled);
            }
         });
      }

      public void changeBeltItemTo(ElementLink<BeltItemElement> link, ItemStack newStack) {
         CreateSceneBuilder.this.addInstruction(scene -> {
            BeltItemElement resolve = (BeltItemElement)scene.resolve(link);
            if (resolve != null) {
               resolve.ifPresent(tis -> tis.stack = newStack);
            }
         });
      }

      public void setKineticSpeed(Selection selection, float speed) {
         this.modifyKineticSpeed(selection, f -> speed);
      }

      public void multiplyKineticSpeed(Selection selection, float modifier) {
         this.modifyKineticSpeed(selection, f -> f * modifier);
      }

      public void modifyKineticSpeed(Selection selection, UnaryOperator<Float> speedFunc) {
         this.modifyBlockEntityNBT(selection, SpeedGaugeBlockEntity.class, nbt -> {
            float newSpeed = speedFunc.apply(Float.valueOf(nbt.getFloat("Speed")));
            nbt.putFloat("Value", SpeedGaugeBlockEntity.getDialTarget(newSpeed));
         });
         this.modifyBlockEntityNBT(selection, KineticBlockEntity.class, nbt -> nbt.putFloat("Speed", speedFunc.apply(Float.valueOf(nbt.getFloat("Speed")))));
      }

      public void propagatePipeChange(BlockPos pos) {
         this.modifyBlockEntity(pos, PumpBlockEntity.class, be -> be.onSpeedChanged(0.0F));
      }

      public void setFilterData(Selection selection, Class<? extends BlockEntity> teType, ItemStack filter) {
         this.modifyBlockEntityNBT(selection, teType, nbt -> nbt.put("Filter", filter.saveOptional(CreateSceneBuilder.this.world().getHolderLookupProvider())));
      }

      public void instructArm(BlockPos armLocation, ArmBlockEntity.Phase phase, ItemStack heldItem, int targetedPoint) {
         this.modifyBlockEntityNBT(CreateSceneBuilder.this.scene.getSceneBuildingUtil().select().position(armLocation), ArmBlockEntity.class, compound -> {
            NBTHelper.writeEnum(compound, "Phase", phase);
            compound.put("HeldItem", heldItem.saveOptional(CreateSceneBuilder.this.world().getHolderLookupProvider()));
            compound.putInt("TargetPointIndex", targetedPoint);
            compound.putFloat("MovementProgress", 0.0F);
         });
      }

      public void flapFunnel(BlockPos position, boolean outward) {
         this.modifyBlockEntity(position, FunnelBlockEntity.class, funnel -> funnel.flap(!outward));
      }

      public void setCraftingResult(BlockPos crafter, ItemStack output) {
         this.modifyBlockEntity(crafter, MechanicalCrafterBlockEntity.class, mct -> mct.setScriptedResult(output));
      }

      public void connectCrafterInvs(BlockPos position1, BlockPos position2) {
         CreateSceneBuilder.this.addInstruction(s -> {
            ConnectedInputHandler.toggleConnection(s.getWorld(), position1, position2);
            s.forEach(WorldSectionElement.class, WorldSectionElement::queueRedraw);
         });
      }

      public void toggleControls(BlockPos position) {
         this.cycleBlockProperty(position, ControlsBlock.VIRTUAL);
      }

      public void animateTrainStation(BlockPos position, boolean trainPresent) {
         this.modifyBlockEntityNBT(
            CreateSceneBuilder.this.getScene().getSceneBuildingUtil().select().position(position),
            StationBlockEntity.class,
            c -> c.putBoolean("ForceFlag", trainPresent)
         );
      }

      public void conductorBlaze(BlockPos position, boolean conductor) {
         this.modifyBlockEntityNBT(
            CreateSceneBuilder.this.getScene().getSceneBuildingUtil().select().position(position),
            BlazeBurnerBlockEntity.class,
            c -> c.putBoolean("TrainHat", conductor)
         );
      }

      public void changeSignalState(BlockPos position, SignalBlockEntity.SignalState state) {
         this.modifyBlockEntityNBT(
            CreateSceneBuilder.this.getScene().getSceneBuildingUtil().select().position(position),
            SignalBlockEntity.class,
            c -> NBTHelper.writeEnum(c, "State", state)
         );
      }

      public void setDisplayBoardText(BlockPos position, int line, Component text) {
         this.modifyBlockEntity(position, FlapDisplayBlockEntity.class, t -> t.applyTextManually(line, text));
      }

      public void dyeDisplayBoard(BlockPos position, int line, DyeColor color) {
         this.modifyBlockEntity(position, FlapDisplayBlockEntity.class, t -> t.setColour(line, color));
      }

      public void flashDisplayLink(BlockPos position) {
         this.modifyBlockEntity(position, LinkWithBulbBlockEntity.class, LinkWithBulbBlockEntity::pulse);
      }

      public void restoreBlocks(Selection selection) {
         super.restoreBlocks(selection);
         this.markSmartBlockEntityVirtual(selection);
      }

      public void setBlocks(Selection selection, BlockState state, boolean spawnParticles) {
         super.setBlocks(selection, state, spawnParticles);
         this.markSmartBlockEntityVirtual(selection);
      }

      public void modifyBlocks(Selection selection, UnaryOperator<BlockState> stateFunc, boolean spawnParticles) {
         super.modifyBlocks(selection, stateFunc, spawnParticles);
         this.markSmartBlockEntityVirtual(selection);
      }

      private void markSmartBlockEntityVirtual(Selection selection) {
         CreateSceneBuilder.this.addInstruction(scene -> selection.forEach(pos -> {
               if (scene.getWorld().getBlockEntity(pos) instanceof SmartBlockEntity smartBlockEntity) {
                  smartBlockEntity.markVirtual();
               }
            }));
      }
   }
}

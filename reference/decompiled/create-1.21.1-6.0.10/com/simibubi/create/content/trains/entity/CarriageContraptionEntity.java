package com.simibubi.create.content.trains.entity;

import com.google.common.base.Strings;
import com.simibubi.create.AllEntityDataSerializers;
import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.Create;
import com.simibubi.create.CreateClient;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.ContraptionBlockChangedPacket;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.trains.CubeParticleData;
import com.simibubi.create.content.trains.TrainHUDUpdatePacket;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.catnip.theme.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class CarriageContraptionEntity extends OrientedContraptionEntity {
   private static final EntityDataAccessor<CarriageSyncData> CARRIAGE_DATA = SynchedEntityData.defineId(
      CarriageContraptionEntity.class, AllEntityDataSerializers.CARRIAGE_DATA
   );
   private static final EntityDataAccessor<Optional<UUID>> TRACK_GRAPH = SynchedEntityData.defineId(
      CarriageContraptionEntity.class, EntityDataSerializers.OPTIONAL_UUID
   );
   private static final EntityDataAccessor<Boolean> SCHEDULED = SynchedEntityData.defineId(CarriageContraptionEntity.class, EntityDataSerializers.BOOLEAN);
   public UUID trainId;
   public int carriageIndex;
   private Carriage carriage;
   public boolean validForRender;
   public boolean movingBackwards;
   public boolean leftTickingChunks;
   public boolean firstPositionUpdate;
   private boolean arrivalSoundPlaying;
   private boolean arrivalSoundReversed;
   private int arrivalSoundTicks;
   private Vec3 serverPrevPos;
   @OnlyIn(Dist.CLIENT)
   public CarriageSounds sounds;
   @OnlyIn(Dist.CLIENT)
   public CarriageParticles particles;
   Vec3 derailParticleOffset;
   private Set<BlockPos> particleSlice = new HashSet<>();
   private float particleAvgY = 0.0F;
   double navDistanceTotal = 0.0;
   int hudPacketCooldown = 0;
   boolean stationMessage = false;

   public CarriageContraptionEntity(EntityType<?> type, Level world) {
      super(type, world);
      this.validForRender = false;
      this.firstPositionUpdate = true;
      this.arrivalSoundTicks = Integer.MIN_VALUE;
      this.derailParticleOffset = VecHelper.offsetRandomly(Vec3.ZERO, world.random, 1.5F).multiply(1.0, 0.25, 1.0);
   }

   public boolean isControlledByLocalInstance() {
      return true;
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(CARRIAGE_DATA, new CarriageSyncData());
      builder.define(TRACK_GRAPH, Optional.empty());
      builder.define(SCHEDULED, false);
   }

   public void syncCarriage() {
      CarriageSyncData carriageData = this.getCarriageData();
      if (carriageData != null) {
         if (this.carriage != null) {
            carriageData.update(this, this.carriage);
         }
      }
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
      super.onSyncedDataUpdated(key);
      if (this.level().isClientSide) {
         this.bindCarriage();
         if (TRACK_GRAPH.equals(key)) {
            this.updateTrackGraph();
         }

         if (CARRIAGE_DATA.equals(key)) {
            CarriageSyncData carriageData = this.getCarriageData();
            if (carriageData == null) {
               return;
            }

            if (this.carriage == null) {
               return;
            }

            carriageData.apply(this, this.carriage);
         }
      }
   }

   public CarriageSyncData getCarriageData() {
      return (CarriageSyncData)this.entityData.get(CARRIAGE_DATA);
   }

   public boolean hasSchedule() {
      return (Boolean)this.entityData.get(SCHEDULED);
   }

   public void setServerSidePrevPosition() {
      this.serverPrevPos = this.position();
   }

   @Override
   public Vec3 getPrevPositionVec() {
      return !this.level().isClientSide() && this.serverPrevPos != null ? this.serverPrevPos : super.getPrevPositionVec();
   }

   public boolean isLocalCoordWithin(BlockPos localPos, int min, int max) {
      if (!(this.getContraption() instanceof CarriageContraption cc)) {
         return false;
      } else {
         Direction facing = cc.getAssemblyDirection();
         Axis axis = facing.getClockWise().getAxis();
         int coord = axis.choose(localPos.getZ(), localPos.getY(), localPos.getX()) * -facing.getAxisDirection().getStep();
         return coord >= min && coord <= max;
      }
   }

   public static CarriageContraptionEntity create(Level world, CarriageContraption contraption) {
      CarriageContraptionEntity entity = new CarriageContraptionEntity((EntityType<?>)AllEntityTypes.CARRIAGE_CONTRAPTION.get(), world);
      entity.setContraption(contraption);
      entity.setInitialOrientation(contraption.getAssemblyDirection().getClockWise());
      entity.startAtInitialYaw();
      return entity;
   }

   @Override
   public void tick() {
      super.tick();
      if (this.contraption instanceof CarriageContraption cc) {
         for (Entity entity : this.getPassengers()) {
            if (!(entity instanceof Player)) {
               BlockPos seatOf = cc.getSeatOf(entity.getUUID());
               if (seatOf != null && cc.conductorSeats.get(seatOf) != null) {
                  this.alignPassenger(entity);
               }
            }
         }
      }
   }

   @Override
   public void setBlock(BlockPos localPos, StructureBlockInfo newInfo) {
      if (this.carriage != null) {
         this.carriage.forEachPresentEntity(cce -> {
            cce.contraption.getBlocks().put(localPos, newInfo);
            CatnipServices.NETWORK.sendToClientsTrackingEntity(cce, new ContraptionBlockChangedPacket(cce.getId(), localPos, newInfo.state()));
         });
      }
   }

   @Override
   protected void tickContraption() {
      if (this.nonDamageTicks > 0) {
         this.nonDamageTicks--;
      }

      if (this.contraption instanceof CarriageContraption cc) {
         if (this.carriage == null) {
            if (this.level().isClientSide) {
               this.bindCarriage();
            } else {
               this.discard();
            }
         } else if (!Create.RAILWAYS.sided(this.level()).trains.containsKey(this.carriage.train.id)) {
            this.discard();
         } else {
            this.tickActors();
            boolean isStalled = this.isStalled();
            this.carriage.stalled = isStalled;
            CarriageSyncData carriageData = this.getCarriageData();
            if (!this.level().isClientSide) {
               this.entityData.set(SCHEDULED, this.carriage.train.runtime.getSchedule() != null);
               boolean shouldCarriageSyncThisTick = this.carriage.train.shouldCarriageSyncThisTick(this.level().getGameTime(), this.getType().updateInterval());
               if (shouldCarriageSyncThisTick && carriageData.isDirty()) {
                  this.entityData.set(CARRIAGE_DATA, null);
                  this.entityData.set(CARRIAGE_DATA, carriageData);
                  carriageData.setDirty(false);
               }

               Navigation navigation = this.carriage.train.navigation;
               if (navigation.announceArrival
                  && Math.abs(navigation.distanceToDestination) < 60.0
                  && this.carriageIndex == (this.carriage.train.speed < 0.0 ? this.carriage.train.carriages.size() - 1 : 0)) {
                  navigation.announceArrival = false;
                  this.arrivalSoundPlaying = true;
                  this.arrivalSoundReversed = this.carriage.train.speed < 0.0;
                  this.arrivalSoundTicks = Integer.MIN_VALUE;
               }

               if (this.arrivalSoundPlaying) {
                  this.tickArrivalSound(cc);
               }

               this.entityData.set(TRACK_GRAPH, Optional.ofNullable(this.carriage.train.graph).map(g -> g.id));
               this.level().gameEvent(this, GameEvent.RESONATE_8, this.position());
            } else {
               Carriage.DimensionalCarriageEntity dce = this.carriage.getDimensional(this.level());
               if (this.tickCount % 10 == 0) {
                  this.updateTrackGraph();
               }

               if (dce.pointsInitialised) {
                  carriageData.approach(this, this.carriage, 1.0F / (float)this.getType().updateInterval());
                  if (!this.carriage.train.derailed) {
                     this.carriage.updateContraptionAnchors();
                  }

                  this.xo = this.getX();
                  this.yo = this.getY();
                  this.zo = this.getZ();
                  dce.alignEntity(this);
                  if (this.sounds == null) {
                     this.sounds = new CarriageSounds(this);
                  }

                  this.sounds.tick(dce);
                  if (this.particles == null) {
                     this.particles = new CarriageParticles(this);
                  }

                  this.particles.tick(dce);
                  double distanceTo = 0.0;
                  if (!this.firstPositionUpdate) {
                     Vec3 diff = this.position().subtract(this.xo, this.yo, this.zo);
                     Vec3 relativeDiff = VecHelper.rotate(diff, (double)this.yaw, Axis.Y);
                     double signum = Math.signum(-relativeDiff.x);
                     distanceTo = diff.length() * signum;
                     this.movingBackwards = signum < 0.0;
                  }

                  ((CarriageBogey)this.carriage.bogeys.getFirst()).updateAngles(this, distanceTo);
                  if (this.carriage.isOnTwoBogeys()) {
                     ((CarriageBogey)this.carriage.bogeys.getSecond()).updateAngles(this, distanceTo);
                  }

                  if (this.carriage.train.derailed) {
                     this.spawnDerailParticles(this.carriage);
                  }

                  if (dce.pivot != null) {
                     this.spawnPortalParticles(dce);
                  }

                  this.firstPositionUpdate = false;
                  this.validForRender = true;
               }
            }
         }
      }
   }

   private void bindCarriage() {
      if (this.carriage == null) {
         Train train = Create.RAILWAYS.sided(this.level()).trains.get(this.trainId);
         if (train != null && train.carriages.size() > this.carriageIndex) {
            this.carriage = train.carriages.get(this.carriageIndex);
            if (this.carriage != null) {
               Carriage.DimensionalCarriageEntity dimensional = this.carriage.getDimensional(this.level());
               dimensional.entity = new WeakReference<>(this);
               dimensional.pivot = null;
               this.carriage.updateContraptionAnchors();
               dimensional.updateRenderedCutoff();
            }

            this.updateTrackGraph();
         }
      }
   }

   private void tickArrivalSound(CarriageContraption cc) {
      List<Carriage> carriages = this.carriage.train.carriages;
      if (this.arrivalSoundTicks == Integer.MIN_VALUE) {
         int carriageCount = carriages.size();
         Integer tick = null;

         for (int index = 0; index < carriageCount; index++) {
            int i = this.arrivalSoundReversed ? carriageCount - 1 - index : index;
            Carriage carriage = carriages.get(i);
            CarriageContraptionEntity entity = carriage.getDimensional(this.level()).entity.get();
            if (entity == null || !(entity.contraption instanceof CarriageContraption otherCC)) {
               break;
            }

            tick = this.arrivalSoundReversed ? otherCC.soundQueue.lastTick() : otherCC.soundQueue.firstTick();
            if (tick != null) {
               break;
            }
         }

         if (tick == null) {
            this.arrivalSoundPlaying = false;
            return;
         }

         this.arrivalSoundTicks = tick;
      }

      if (this.tickCount % 2 != 0) {
         boolean keepTicking = false;

         for (Carriage c : carriages) {
            CarriageContraptionEntity entityx = c.getDimensional(this.level()).entity.get();
            if (entityx != null && entityx.contraption instanceof CarriageContraption otherCC) {
               keepTicking |= otherCC.soundQueue.tick(entityx, this.arrivalSoundTicks, this.arrivalSoundReversed);
            }
         }

         if (!keepTicking) {
            this.arrivalSoundPlaying = false;
         } else {
            this.arrivalSoundTicks = this.arrivalSoundTicks + (this.arrivalSoundReversed ? -1 : 1);
         }
      }
   }

   @Override
   public void tickActors() {
      super.tickActors();
   }

   @Override
   protected boolean isActorActive(MovementContext context, MovementBehaviour actor) {
      if (this.contraption instanceof CarriageContraption cc) {
         return !super.isActorActive(context, actor) ? false : cc.notInPortal() || this.level().isClientSide();
      } else {
         return false;
      }
   }

   @Override
   protected void handleStallInformation(double x, double y, double z, float angle) {
   }

   private void spawnDerailParticles(Carriage carriage) {
      if (this.random.nextFloat() < 0.05F) {
         Vec3 v = this.position().add(this.derailParticleOffset);
         this.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, v.x, v.y, v.z, 0.0, 0.04, 0.0);
      }
   }

   protected void addPassenger(Entity pPassenger) {
      super.addPassenger(pPassenger);
      if (pPassenger instanceof Player player) {
         player.getPersistentData().put("ContraptionMountLocation", VecHelper.writeNBT(player.position()));
      }
   }

   private void spawnPortalParticles(Carriage.DimensionalCarriageEntity dce) {
      Vec3 pivot = dce.pivot.getLocation().add(0.0, 1.5, 0.0);
      if (!this.particleSlice.isEmpty()) {
         boolean alongX = Mth.equal(pivot.x, (double)Math.round(pivot.x));
         int extraFlip = Direction.fromYRot((double)this.yaw).getAxisDirection().getStep();
         Vec3 emitter = pivot.add(0.0, (double)this.particleAvgY, 0.0);
         double speed = this.position().distanceTo(this.getPrevPositionVec());
         int size = (int)((double)this.particleSlice.size() * Mth.clamp(4.0 - speed * 4.0, 0.0, 4.0));

         for (BlockPos pos : this.particleSlice) {
            if (size == 0 || this.random.nextInt(size) == 0) {
               if (alongX) {
                  pos = new BlockPos(0, pos.getY(), pos.getX());
               }

               Vec3 v = pivot.add((double)(pos.getX() * extraFlip), (double)pos.getY(), (double)(pos.getZ() * extraFlip));
               CubeParticleData data = new CubeParticleData(0.25F, 0.0F, 0.5F, 0.65F + (this.random.nextFloat() - 0.5F) * 0.25F, 4, false);
               Vec3 m = v.subtract(emitter).normalize().scale(0.325F);
               m = VecHelper.rotate(m, (double)(this.random.nextFloat() * 360.0F), alongX ? Axis.X : Axis.Z);
               m = m.add(VecHelper.offsetRandomly(Vec3.ZERO, this.random, 0.25F));
               this.level().addParticle(data, v.x, v.y, v.z, m.x, m.y, m.z);
            }
         }
      }
   }

   public void onClientRemoval() {
      super.onClientRemoval();
      this.entityData.set(CARRIAGE_DATA, new CarriageSyncData());
      if (this.carriage != null) {
         Carriage.DimensionalCarriageEntity dce = this.carriage.getDimensional(this.level());
         dce.pointsInitialised = false;
         this.carriage.leadingBogey().couplingAnchors = Couple.create(null, null);
         this.carriage.trailingBogey().couplingAnchors = Couple.create(null, null);
      }

      this.firstPositionUpdate = true;
      if (this.sounds != null) {
         this.sounds.stop();
      }
   }

   @Override
   protected void writeAdditional(CompoundTag compound, Provider registries, boolean spawnPacket) {
      super.writeAdditional(compound, registries, spawnPacket);
      compound.putUUID("TrainId", this.trainId);
      compound.putInt("CarriageIndex", this.carriageIndex);
   }

   @Override
   protected void readAdditional(CompoundTag compound, boolean spawnPacket) {
      super.readAdditional(compound, spawnPacket);
      this.trainId = compound.getUUID("TrainId");
      this.carriageIndex = compound.getInt("CarriageIndex");
      if (spawnPacket) {
         this.xOld = this.getX();
         this.yOld = this.getY();
         this.zOld = this.getZ();
      }
   }

   @Override
   public Component getContraptionName() {
      return this.carriage != null ? this.carriage.train.name : super.getContraptionName();
   }

   public Couple<Boolean> checkConductors() {
      Couple<Boolean> sides = Couple.create(false, false);
      if (!(this.contraption instanceof CarriageContraption cc)) {
         return sides;
      } else {
         sides.setFirst((Boolean)cc.blockConductors.getFirst());
         sides.setSecond((Boolean)cc.blockConductors.getSecond());

         for (Entity entity : this.getPassengers()) {
            if (!(entity instanceof Player)) {
               BlockPos seatOf = cc.getSeatOf(entity.getUUID());
               if (seatOf != null) {
                  Couple<Boolean> validSides = cc.conductorSeats.get(seatOf);
                  if (validSides != null) {
                     sides.setFirst((Boolean)sides.getFirst() || (Boolean)validSides.getFirst());
                     sides.setSecond((Boolean)sides.getSecond() || (Boolean)validSides.getSecond());
                  }
               }
            }
         }

         return sides;
      }
   }

   @Override
   public boolean startControlling(BlockPos controlsLocalPos, Player player) {
      if (player == null || player.isSpectator()) {
         return false;
      } else if (this.carriage == null) {
         return false;
      } else if (this.carriage.train.derailed) {
         return false;
      } else {
         Train train = this.carriage.train;
         if (train.runtime.getSchedule() != null && !train.runtime.paused) {
            train.status.manualControls();
         }

         train.navigation.cancelNavigation();
         train.runtime.paused = true;
         train.navigation.waitingForSignal = null;
         return true;
      }
   }

   public Component getDisplayName() {
      return (Component)(this.carriage == null ? CreateLang.translateDirect("train") : this.carriage.train.name);
   }

   @Override
   public boolean control(BlockPos controlsLocalPos, Collection<Integer> heldControls, Player player) {
      if (this.carriage == null) {
         return false;
      } else if (this.carriage.train.derailed) {
         return false;
      } else if (this.level().isClientSide) {
         return true;
      } else if (player.isSpectator()) {
         return false;
      } else if (!this.canInteractWithBlock(player, VecHelper.getCenterOf(controlsLocalPos), 8.0)) {
         return false;
      } else if (heldControls.contains(5)) {
         return false;
      } else {
         StructureBlockInfo info = this.contraption.getBlocks().get(controlsLocalPos);
         Direction initialOrientation = this.getInitialOrientation().getCounterClockWise();
         boolean inverted = false;
         if (info != null && info.state().hasProperty(ControlsBlock.FACING)) {
            inverted = !((Direction)info.state().getValue(ControlsBlock.FACING)).equals(initialOrientation);
         }

         if (this.hudPacketCooldown-- <= 0 && player instanceof ServerPlayer sp) {
            CatnipServices.NETWORK.sendToClient(sp, new TrainHUDUpdatePacket.Clientbound(this.carriage.train));
            this.hudPacketCooldown = 5;
         }

         int targetSpeed = 0;
         if (heldControls.contains(0)) {
            targetSpeed++;
         }

         if (heldControls.contains(1)) {
            targetSpeed--;
         }

         int targetSteer = 0;
         if (heldControls.contains(2)) {
            targetSteer++;
         }

         if (heldControls.contains(3)) {
            targetSteer--;
         }

         if (inverted) {
            targetSpeed *= -1;
            targetSteer *= -1;
         }

         if (targetSpeed != 0) {
            this.carriage.train.burnFuel();
         }

         boolean slow = inverted ^ targetSpeed < 0;
         boolean spaceDown = heldControls.contains(4);
         GlobalStation currentStation = this.carriage.train.getCurrentStation();
         if (currentStation != null && spaceDown) {
            this.sendPrompt(
               player, CreateLang.translateDirect("train.arrived_at", Component.literal(currentStation.name).withStyle(s -> s.withColor(7358000))), false
            );
            return true;
         } else {
            if (this.carriage.train.speedBeforeStall != null
               && targetSpeed != 0
               && Math.signum(this.carriage.train.speedBeforeStall) != (double)Math.signum((float)targetSpeed)) {
               this.carriage.train.cancelStall();
            }

            if (currentStation != null && targetSpeed != 0) {
               this.stationMessage = false;
               this.sendPrompt(
                  player,
                  CreateLang.translateDirect("train.departing_from", Component.literal(currentStation.name).withStyle(s -> s.withColor(7358000))),
                  false
               );
            }

            if (currentStation == null) {
               Navigation nav = this.carriage.train.navigation;
               if (nav.destination != null) {
                  if (!spaceDown) {
                     nav.cancelNavigation();
                  }

                  if (spaceDown) {
                     double f = nav.distanceToDestination / this.navDistanceTotal;
                     int progress = (int)(Mth.clamp(1.0 - (1.0 - f) * (1.0 - f), 0.0, 1.0) * 30.0);
                     boolean arrived = progress == 0;
                     MutableComponent whiteComponent = Component.literal(Strings.repeat("|", progress));
                     MutableComponent greenComponent = Component.literal(Strings.repeat("|", 30 - progress));
                     int fromColor = 16761412;
                     int toColor = 5413141;
                     int mixedColor = Color.mixColors(toColor, fromColor, (float)progress / 30.0F);
                     int targetColor = arrived ? toColor : 5524805;
                     MutableComponent component = greenComponent.withStyle(st -> st.withColor(mixedColor))
                        .append(whiteComponent.withStyle(st -> st.withColor(targetColor)));
                     this.sendPrompt(player, component, true);
                     this.carriage.train.manualTick = true;
                     return true;
                  }
               }

               double directedSpeed = targetSpeed != 0 ? (double)targetSpeed : this.carriage.train.speed;
               GlobalStation lookAhead = nav.findNearestApproachable(
                  !this.carriage.train.doubleEnded || (directedSpeed != 0.0 ? directedSpeed > 0.0 : !inverted)
               );
               if (lookAhead != null) {
                  if (spaceDown) {
                     this.carriage.train.manualTick = true;
                     nav.startNavigation(nav.findPathTo(lookAhead, -1.0));
                     this.carriage.train.manualTick = false;
                     this.navDistanceTotal = nav.distanceToDestination;
                     return true;
                  }

                  this.displayApproachStationMessage(player, lookAhead);
               } else {
                  this.cleanUpApproachStationMessage(player);
               }
            }

            this.carriage.train.manualSteer = targetSteer < 0
               ? TravellingPoint.SteerDirection.RIGHT
               : (targetSteer > 0 ? TravellingPoint.SteerDirection.LEFT : TravellingPoint.SteerDirection.NONE);
            double topSpeed = (double)(this.carriage.train.maxSpeed() * AllConfigs.server().trains.manualTrainSpeedModifier.getF());
            double cappedTopSpeed = topSpeed * this.carriage.train.throttle;
            if (this.carriage.getLeadingPoint().edge != null && this.carriage.getLeadingPoint().edge.isTurn()
               || this.carriage.getTrailingPoint().edge != null && this.carriage.getTrailingPoint().edge.isTurn()) {
               topSpeed = (double)this.carriage.train.maxTurnSpeed();
            }

            if (slow) {
               topSpeed /= 4.0;
            }

            this.carriage.train.targetSpeed = Math.min(topSpeed, cappedTopSpeed) * (double)targetSpeed;
            boolean counteringAcceleration = Math.abs((double)Math.signum((float)targetSpeed) - Math.signum(this.carriage.train.speed)) > 1.5;
            if (slow && !counteringAcceleration) {
               this.carriage.train.backwardsDriver = player;
            }

            this.carriage.train.manualTick = true;
            this.carriage.train.approachTargetSpeed(counteringAcceleration ? 2.0F : 1.0F);
            return true;
         }
      }
   }

   private void sendPrompt(Player player, MutableComponent component, boolean shadow) {
      if (player instanceof ServerPlayer sp) {
         CatnipServices.NETWORK.sendToClient(sp, new TrainPromptPacket(component, shadow));
      }
   }

   private void displayApproachStationMessage(Player player, GlobalStation station) {
      this.sendPrompt(player, CreateLang.translateDirect("contraption.controls.approach_station", Component.keybind("key.jump"), station.name), false);
      this.stationMessage = true;
   }

   private void cleanUpApproachStationMessage(Player player) {
      if (this.stationMessage) {
         player.displayClientMessage(CommonComponents.EMPTY, true);
         this.stationMessage = false;
      }
   }

   private void updateTrackGraph() {
      if (this.carriage != null) {
         Optional<UUID> optional = (Optional<UUID>)this.entityData.get(TRACK_GRAPH);
         if (optional.isEmpty()) {
            this.carriage.train.graph = null;
            this.carriage.train.derailed = true;
         } else {
            TrackGraph graph = CreateClient.RAILWAYS.sided(this.level()).trackNetworks.get(optional.get());
            if (graph != null) {
               this.carriage.train.graph = graph;
               this.carriage.train.derailed = false;
            }
         }
      }
   }

   public boolean shouldBeSaved() {
      return false;
   }

   public Carriage getCarriage() {
      return this.carriage;
   }

   public void setCarriage(Carriage carriage) {
      this.carriage = carriage;
      this.trainId = carriage.train.id;
      this.carriageIndex = carriage.train.carriages.indexOf(carriage);
      if (this.contraption instanceof CarriageContraption cc) {
         cc.swapStorageAfterAssembly(this);
      }

      if (carriage.train.graph != null) {
         this.entityData.set(TRACK_GRAPH, Optional.of(carriage.train.graph.id));
      }

      Carriage.DimensionalCarriageEntity dimensional = carriage.getDimensional(this.level());
      dimensional.pivot = null;
      carriage.updateContraptionAnchors();
      dimensional.updateRenderedCutoff();
   }

   @OnlyIn(Dist.CLIENT)
   public void updateRenderedPortalCutoff() {
      if (this.carriage != null) {
         this.particleSlice.clear();
         this.particleAvgY = 0.0F;
         if (this.contraption instanceof CarriageContraption cc) {
            Direction forward = cc.getAssemblyDirection().getClockWise();
            Axis axis = forward.getAxis();
            boolean x = axis == Axis.X;
            boolean flip = true;

            for (BlockPos pos : this.contraption.getBlocks().keySet()) {
               if (cc.atSeam(pos)) {
                  int pX = x ? pos.getX() : pos.getZ();
                  pX *= forward.getAxisDirection().getStep() * (flip ? 1 : -1);
                  pos = new BlockPos(pX, pos.getY(), 0);
                  this.particleSlice.add(pos);
                  this.particleAvgY = this.particleAvgY + (float)pos.getY();
               }
            }
         }

         if (this.particleSlice.size() > 0) {
            this.particleAvgY = this.particleAvgY / (float)this.particleSlice.size();
         }
      }
   }
}

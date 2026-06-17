package dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import dev.eriksonn.aeronautics.config.AeroConfig;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.contraption.PropellerBearingContraptionEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.behaviour.PropellerActorBehaviour;
import dev.eriksonn.aeronautics.data.AeroLang;
import dev.eriksonn.aeronautics.index.AeroAdvancements;
import dev.eriksonn.aeronautics.util.AeroSoundDistUtil;
import dev.ryanhcode.sable.api.block.propeller.BlockEntityPropeller;
import dev.ryanhcode.sable.api.block.propeller.BlockEntitySubLevelPropellerActor;
import dev.simulated_team.simulated.api.BearingSlowdownController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public class PropellerBearingBlockEntity
   extends MechanicalBearingBlockEntity
   implements MechanicalBearingTileEntityExtension,
   BlockEntitySubLevelPropellerActor,
   IHaveGoggleInformation,
   BlockEntityPropeller {
   private static final MutableComponent SCROLL_OPTION_TITLE = AeroLang.translate("scroll_option.thrust_direction").component();
   public final Vector3d thrustDirection;
   public final Vector3d facingDirection = new Vector3d();
   public float totalSailPower;
   public boolean disassemblySlowdown = false;
   public float prevAngle;
   public BearingSlowdownController slowdownController = new BearingSlowdownController();
   protected PropellerActorBehaviour behavior;
   protected List<BlockPos> sailPositions;
   protected float lastGeneratedSpeed;
   private ScrollOptionBehaviour<PropellerBearingBlockEntity.ThrustDirection> thrustDirectionOption;
   private float rotationSpeed = 0.0F;
   private boolean insideMainTick = false;
   @Nullable
   private Object currentSoundInstance;

   public PropellerBearingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.sailPositions = new ArrayList<>();
      this.thrustDirection = new Vector3d();
      this.behavior.setThrustDirection(this.thrustDirection);
   }

   private static double getConfigAirflowMult() {
      return (Double)AeroConfig.server().physics.propellerBearingAirflowMult.get();
   }

   private static double getConfigThrust() {
      return (Double)AeroConfig.server().physics.propellerBearingThrust.get();
   }

   public float calculateStressApplied() {
      if (this.running && !this.disassemblySlowdown) {
         int sails = 0;
         if (this.movedContraption != null) {
            sails = ((BearingContraption)this.movedContraption.getContraption()).getSailBlocks();
         }

         sails = Math.max(sails, 2);
         float stress = (float)sails * (float)BlockStressValues.getImpact(this.getStressConfigKey());
         this.lastStressApplied = stress;
         return stress;
      } else {
         this.lastStressApplied = 0.0F;
         return 0.0F;
      }
   }

   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      super.addBehaviours(behaviours);
      this.movementMode.setValue(2);
      behaviours.remove(this.movementMode);
      this.thrustDirectionOption = new ScrollOptionBehaviour(
         PropellerBearingBlockEntity.ThrustDirection.class, SCROLL_OPTION_TITLE, this, this.getMovementModeSlot()
      );
      this.getThrustDirectionOption().withCallback($ -> this.onDirectionChanged());
      behaviours.add(this.getThrustDirectionOption());
      behaviours.add(this.behavior = this.getAndPreparePropBehaviour());
   }

   public PropellerActorBehaviour createProp() {
      return new PropellerActorBehaviour(this, this);
   }

   public PropellerActorBehaviour getAndPreparePropBehaviour() {
      PropellerActorBehaviour prop = this.createProp();
      prop.setParticleAmountUpdater(() -> 0.02 * (double)Math.abs(this.getClampedRotationRate()) * (double)this.totalSailPower);
      prop.setParticleCountProperties(50, 10.0);
      prop.setParticlePositionUpdater((v, random) -> this.getRandomSailPosition(random, v).add(this.facingDirection));
      return prop;
   }

   public Direction getBlockDirection() {
      return (Direction)this.getBlockState().getValue(PropellerBearingBlock.FACING);
   }

   public double getThrust() {
      return Math.pow((double)this.totalSailPower, 1.5) * (double)this.getDirectionIndependentSpeed() * getConfigThrust();
   }

   public boolean isActive() {
      return Math.abs(this.rotationSpeed) > 0.01F && this.movedContraption != null;
   }

   public double getAirflow() {
      return Math.sqrt((double)this.totalSailPower) * (double)this.getDirectionIndependentSpeed() * getConfigAirflowMult();
   }

   public float getDirectionIndependentSpeed() {
      return (float)((Direction)this.getBlockState().getValue(BlockStateProperties.FACING)).getAxisDirection().getStep()
         * this.getClampedRotationRate()
         * 3.3333333F
         * (float)(this.getThrustDirectionOption().value == 1 ? -1 : 1);
   }

   public BlockEntityPropeller getPropeller() {
      return this;
   }

   public void tick() {
      this.prevAngle = this.angle;
      Vec3i normal = ((Direction)this.getBlockState().getValue(BlockStateProperties.FACING)).getNormal();
      this.facingDirection.set((double)normal.getX(), (double)normal.getY(), (double)normal.getZ());
      if (this.disassemblySlowdown) {
         this.updateSlowdownSpeed();
      } else {
         this.updateRotationSpeed();
      }

      this.insideMainTick = true;
      super.tick();
      this.insideMainTick = false;
      if (this.movedContraption != null && !this.movedContraption.isAlive()) {
         this.movedContraption = null;
      }

      if (this.movedContraption == null && !this.isVirtual()) {
         this.angle = 0.0F;
         this.setRotationSpeed(0.0F);
         this.disassemblySlowdown = false;
      }

      if (this.speed != 0.0F) {
         this.lastGeneratedSpeed = this.speed;
      }

      if (this.isActive()) {
         this.activeTick();
      }
   }

   public void activeTick() {
      this.behavior.pushEntities();
      if (this.level.isClientSide) {
         this.behavior.spawnParticles();
      }
   }

   public void onDirectionChanged() {
      if (!this.level.isClientSide && this.running) {
         this.updateGeneratedRotation();
      }
   }

   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      compound.putFloat("LastGenerated", this.lastGeneratedSpeed);
      compound.putFloat("RotationSpeed", this.getRotationSpeed());
      compound.putBoolean("DisassemblySlowdown", this.disassemblySlowdown);
      if (this.disassemblySlowdown) {
         this.slowdownController.serializeIntoNBT(compound);
      }

      super.write(compound, registries, clientPacket);
   }

   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      if (!this.wasMoved) {
         this.lastGeneratedSpeed = compound.getFloat("LastGenerated");
      }

      this.setRotationSpeed(compound.getFloat("RotationSpeed"));
      this.disassemblySlowdown = compound.getBoolean("DisassemblySlowdown");
      if (this.disassemblySlowdown) {
         this.slowdownController.deserializeFromNBT(compound);
      }

      super.read(compound, registries, clientPacket);
   }

   public float getInterpolatedAngle(float partialTicks) {
      if (this.isVirtual()) {
         return Mth.lerp(partialTicks + 0.5F, this.prevAngle, this.angle);
      } else {
         if (this.movedContraption == null || this.movedContraption.isStalled() || !this.running) {
            partialTicks = 0.0F;
         }

         return this.disassemblySlowdown
            ? this.slowdownController.getAngle(partialTicks)
            : Mth.lerp(partialTicks, this.angle, this.angle + this.getAngularSpeed());
      }
   }

   public float getAngularSpeed() {
      float speed = this.getRotationSpeed();
      if (this.insideMainTick && this.disassemblySlowdown) {
         speed = this.slowdownController.getSpeed(1.0F);
      }

      if (this.level.isClientSide) {
         speed *= ServerSpeedProvider.get();
         speed += this.clientAngleDiff / 3.0F;
      }

      return speed;
   }

   private void updateRotationSpeed() {
      float nextSpeed = convertToAngular(this.getSpeed());
      if (this.isVirtual()) {
         this.setRotationSpeed(nextSpeed);
      }

      if (this.getSpeed() == 0.0F) {
         nextSpeed = 0.0F;
      }

      if (this.totalSailPower > 0.0F) {
         this.setRotationSpeed(Mth.lerp(0.4F / (float)Math.sqrt((double)this.totalSailPower), this.getRotationSpeed(), nextSpeed));
      } else {
         this.setRotationSpeed(nextSpeed);
      }
   }

   private void updateSlowdownSpeed() {
      if (this.slowdownController.stepGoal() && !this.level.isClientSide) {
         this.disassemble();
      } else {
         this.setRotationSpeed(this.slowdownController.getSpeed(0.0F));
         this.angle = this.slowdownController.getAngle(0.0F);
      }
   }

   public void attach(ControlledContraptionEntity contraption) {
      super.attach(contraption);
      this.contraptionInitialize();
      if (this.level.isClientSide) {
         this.currentSoundInstance = AeroSoundDistUtil.tickPropellerSounds(this, this.currentSoundInstance);
      }
   }

   public void assemble() {
      if (this.level.getBlockState(this.worldPosition).getBlock() instanceof BearingBlock) {
         Direction direction = (Direction)this.getBlockState().getValue(PropellerBearingBlock.FACING);
         BearingContraption contraption = new BearingContraption(this.isWindmill(), direction);

         try {
            if (this.isPropeller()) {
               ((BearingContraptionExtension)contraption).aeronautics$setPropeller();
            }

            if (!contraption.assemble(this.level, this.worldPosition)) {
               return;
            }

            this.lastException = null;
         } catch (AssemblyException var4) {
            this.lastException = var4;
            this.sendData();
            return;
         }

         contraption.removeBlocksFromWorld(this.level, BlockPos.ZERO);
         this.movedContraption = PropellerBearingContraptionEntity.create(this.level, this, contraption);
         BlockPos anchor = this.worldPosition.relative(direction);
         this.movedContraption.setPos((double)anchor.getX(), (double)anchor.getY(), (double)anchor.getZ());
         this.movedContraption.setRotationAxis(direction.getAxis());
         this.level.addFreshEntity(this.movedContraption);
         AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(this.level, this.worldPosition);
         AeroAdvancements.IN_THRUST_WE_TRUST.awardToNearby(this.getBlockPos(), this.getLevel());
         this.running = true;
         this.angle = 0.0F;
         this.updateGeneratedRotation();
         this.setRotationSpeed(0.0F);
         this.contraptionInitialize();
         this.sendData();
      }
   }

   public void disassemble() {
      if (this.running && this.movedContraption != null) {
         this.angle = 0.0F;
         this.behavior.getLayers().clear();
         this.applyRotation();
         super.disassemble();
      }
   }

   public void setAssembleNextTick(boolean value) {
      this.assembleNextTick = value;
   }

   public void startDisassemblySlowdown() {
      if (!this.disassemblySlowdown && this.movedContraption != null) {
         this.slowdownController
            .generate(
               1.0F + 3.5F * (float)Math.sqrt((double)this.totalSailPower),
               this.getInterpolatedAngle(0.0F),
               this.getRotationSpeed(),
               (Direction)this.getBlockState().getValue(PropellerBearingBlock.FACING),
               this.getMovedContraption().getContraption()
            );
         this.disassemblySlowdown = true;
         this.updateGeneratedRotation();
         this.sendData();
      }
   }

   public void contraptionInitialize() {
      Direction direction = (Direction)this.getBlockState().getValue(PropellerBearingBlock.FACING);
      this.thrustDirection.set((double)direction.getStepX(), (double)direction.getStepY(), (double)direction.getStepZ());
      this.findSails();
   }

   public float getSailPower(StructureBlockInfo info) {
      BlockState state = info.state();
      if (AllBlocks.COPYCAT_PANEL.has(state)) {
         BlockState newState = NbtUtils.readBlockState(this.blockHolderGetter(), info.nbt().getCompound("Material"));
         if (!newState.isAir()) {
            state = newState;
         }
      }

      float power = 0.0F;
      if (state.is(AllBlockTags.WINDMILL_SAILS.tag)) {
         power++;
      }

      return power;
   }

   public void findSails() {
      this.sailPositions = new ArrayList<>();
      this.totalSailPower = 0.0F;
      this.behavior.getLayers().clear();
      if (this.movedContraption != null) {
         Map<BlockPos, StructureBlockInfo> Blocks = this.movedContraption.getContraption().getBlocks();
         Vec3i direction = ((Direction)this.getBlockState().getValue(PropellerBearingBlock.FACING)).getNormal();
         HashMap<Integer, Tuple<Integer, Integer>> layerHashMap = new HashMap<>();

         for (Entry<BlockPos, StructureBlockInfo> entry : Blocks.entrySet()) {
            float sailPower = this.getSailPower(entry.getValue());
            if (sailPower > 0.0F) {
               BlockPos currentPos = entry.getKey();
               this.sailPositions.add(currentPos);
               int offset = direction.getX() * currentPos.getX() + direction.getY() * currentPos.getY() + direction.getZ() * currentPos.getZ();
               this.totalSailPower += sailPower;
               currentPos = currentPos.offset(direction.multiply(-offset));
               int radius = currentPos.getX() * currentPos.getX() + currentPos.getY() * currentPos.getY() + currentPos.getZ() * currentPos.getZ();
               if (layerHashMap.containsKey(offset)) {
                  Tuple<Integer, Integer> tuple = layerHashMap.get(offset);
                  if (radius < (Integer)tuple.getA()) {
                     tuple.setA(radius);
                  }

                  if (radius > (Integer)tuple.getB()) {
                     tuple.setB(radius);
                  }
               } else {
                  layerHashMap.put(offset, new Tuple(radius, radius));
               }
            }
         }

         for (Entry<Integer, Tuple<Integer, Integer>> entryx : layerHashMap.entrySet()) {
            Tuple<Integer, Integer> tuplex = entryx.getValue();
            double inner = Math.max(Math.sqrt((double)((Integer)tuplex.getA()).intValue()) - 0.5, 0.0);
            double outer = Math.sqrt((double)((Integer)tuplex.getB()).intValue()) + 0.5;
            this.behavior.addPropellerLayer(new PropellerActorBehaviour.PropellerLayer((double)(entryx.getKey() + 1), inner, outer));
         }
      }
   }

   private Vector3d getRandomSailPosition(RandomSource random, Vector3d pos) {
      BlockPos sailPos = this.sailPositions.get(random.nextInt(this.sailPositions.size()));
      Vec3 floatPos = new Vec3((double)sailPos.getX(), (double)sailPos.getY(), (double)sailPos.getZ());
      floatPos = this.movedContraption.applyRotation(floatPos, 0.0F);
      pos.set(random.nextDouble() * 2.0 - 1.0, random.nextDouble() * 2.0 - 1.0, random.nextDouble() * 2.0 - 1.0).mul(0.5);
      pos.fma(-this.thrustDirection.dot(pos), this.thrustDirection);
      pos.add(floatPos.x, floatPos.y, floatPos.z);
      return pos;
   }

   public float getClampedRotationRate() {
      if (this.disassemblySlowdown) {
         float max = Math.max(this.slowdownController.getInitialVelocity(), 0.0F);
         float min = Math.min(this.slowdownController.getInitialVelocity(), 0.0F);
         return Math.min(Math.max(this.getRotationSpeed(), min), max);
      } else {
         return this.getRotationSpeed();
      }
   }

   public boolean isWoodenTop() {
      return false;
   }

   @Override
   public boolean isPropeller() {
      return true;
   }

   public float getRotationSpeed() {
      return this.rotationSpeed;
   }

   public void setRotationSpeed(float rotationSpeed) {
      this.rotationSpeed = rotationSpeed;
   }

   public ScrollOptionBehaviour<PropellerBearingBlockEntity.ThrustDirection> getThrustDirectionOption() {
      return this.thrustDirectionOption;
   }

   public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      return !super.addToGoggleTooltip(tooltip, isPlayerSneaking) ? false : this.behavior.addToGoggleTooltip(tooltip, isPlayerSneaking);
   }

   public PropellerBearingContraptionEntity getMovedContraption() {
      ControlledContraptionEntity var2 = this.movedContraption;
      return var2 instanceof PropellerBearingContraptionEntity ? (PropellerBearingContraptionEntity)var2 : null;
   }

   public static enum ThrustDirection implements INamedIconOptions {
      RIGHT_HANDED(AllIcons.I_REFRESH, "pull_when_clockwise"),
      LEFT_HANDED(AllIcons.I_ROTATE_CCW, "push_when_clockwise");

      private final String translationKey;
      private final AllIcons icon;

      private ThrustDirection(final AllIcons icon, final String name) {
         this.icon = icon;
         this.translationKey = "aeronautics.generic." + name;
      }

      public AllIcons getIcon() {
         return this.icon;
      }

      public String getTranslationKey() {
         return this.translationKey;
      }
   }
}

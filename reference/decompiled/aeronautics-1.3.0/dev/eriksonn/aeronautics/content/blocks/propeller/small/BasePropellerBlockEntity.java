package dev.eriksonn.aeronautics.content.blocks.propeller.small;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.eriksonn.aeronautics.content.blocks.propeller.behaviour.PropellerActorBehaviour;
import dev.eriksonn.aeronautics.index.AeroAdvancements;
import dev.ryanhcode.sable.api.block.propeller.BlockEntityPropeller;
import dev.ryanhcode.sable.api.block.propeller.BlockEntitySubLevelPropellerActor;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public abstract class BasePropellerBlockEntity extends KineticBlockEntity implements BlockEntitySubLevelPropellerActor, BlockEntityPropeller {
   private final Quaternionf rot = new Quaternionf();
   public float rotationSpeed = 0.0F;
   public PropellerActorBehaviour prop;
   private float previousAngle;
   private float angle;

   public BasePropellerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
      super(typeIn, pos, state);
   }

   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      super.addBehaviours(behaviours);
      this.prop = this.createBehavior();
      behaviours.add(this.prop);
   }

   public PropellerActorBehaviour createBehavior() {
      PropellerActorBehaviour prop = new PropellerActorBehaviour(this, this);
      prop.setThrustDirection(JOMLConversion.toJOML(Vec3.atLowerCornerOf(this.getBlockDirection().getNormal())));
      prop.setParticleAmountUpdater(() -> 0.12 * (double)Math.abs(this.rotationSpeed));
      prop.setParticleCountProperties(5, 2.0);
      prop.addSimpleLayer((double)this.getOffset(), (double)this.getRadius());
      prop.setParticlePositionUpdater((v, random) -> {
         PropellerActorBehaviour.PropellerLayer layer = prop.getLayers().get(random.nextInt(prop.getLayers().size()));
         double R = Math.sqrt(Mth.lerp((double)random.nextFloat(), layer.innerRadiusSquared(), layer.outerRadiusSquared()));
         double angle = (Math.PI * 2) * (double)random.nextFloat();
         v.set(Math.cos(angle) * R, layer.offset(), Math.sin(angle) * R);
         this.rot.transform(v);
      });
      return prop;
   }

   public BlockEntityPropeller getPropeller() {
      return this;
   }

   public abstract double getConfigThrust();

   public abstract double getConfigAirflow();

   public abstract float getRadius();

   public float getOffset() {
      return 0.0F;
   }

   public void tick() {
      this.updateRotationSpeed();
      this.setPreviousAngle(this.getAngle());
      this.setAngle(this.getAngle() + this.rotationSpeed);
      this.rot.set(this.getBlockDirection().getRotation());
      super.tick();
      if (this.isActive() && !this.isVirtual()) {
         this.onActiveTick();
      }
   }

   public void onActiveTick() {
      this.prop.pushEntities();
      this.prop.spawnParticles();
   }

   protected float getDirectionIndependentSpeed() {
      return (float)this.getBlockDirection().getAxisDirection().getStep()
         * this.rotationSpeed
         * 3.3333333F
         * (float)(this.getBlockState().getValue(BasePropellerBlock.REVERSED) ? -1 : 1);
   }

   private void updateRotationSpeed() {
      float nextSpeed = convertToAngular(this.getSpeed());
      if (this.getSpeed() == 0.0F) {
         nextSpeed = 0.0F;
      }

      float lerpAmount = 0.15F;
      this.rotationSpeed = Mth.lerp(0.15F, this.rotationSpeed, nextSpeed);
   }

   public void onSpeedChanged(float previousSpeed) {
      super.onSpeedChanged(previousSpeed);
      if (Math.abs(this.getSpeed()) > 0.0F) {
         AeroAdvancements.FOR_EVERY_ACTION.awardToNearby(this.getBlockPos(), this.getLevel());
      }
   }

   protected void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.write(compound, registries, clientPacket);
      compound.putFloat("RotationSpeed", this.rotationSpeed);
   }

   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      this.rotationSpeed = compound.getFloat("RotationSpeed");
   }

   public float getPreviousAngle() {
      return this.previousAngle;
   }

   public void setPreviousAngle(float previousAngle) {
      this.previousAngle = previousAngle;
   }

   public float getAngle() {
      return this.angle;
   }

   public void setAngle(float angle) {
      this.angle = angle;
   }

   public Direction getBlockDirection() {
      return (Direction)this.getBlockState().getValue(BlockStateProperties.FACING);
   }

   public double getAirflow() {
      return this.getConfigAirflow() * (double)this.getDirectionIndependentSpeed();
   }

   public double getThrust() {
      return this.getConfigThrust() * (double)this.getDirectionIndependentSpeed();
   }

   public boolean isActive() {
      return Math.abs(this.rotationSpeed) > 0.01F;
   }

   public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      return !super.addToGoggleTooltip(tooltip, isPlayerSneaking) ? false : this.prop.addToGoggleTooltip(tooltip, isPlayerSneaking);
   }
}

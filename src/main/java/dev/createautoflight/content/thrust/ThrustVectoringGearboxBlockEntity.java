package dev.createautoflight.content.thrust;

import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import dev.createautoflight.content.navigation.AssemblyResolver;
import dev.createautoflight.content.navigation.AutoflightAssemblyBlock;
import dev.createautoflight.registry.ModBlockEntities;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

/**
 * Kinetic pass-through split shaft (always 1:1 when sourced). Thrust is modulated via
 * {@link #thrustDemand} applied in {@link #sable$physicsTick} — varying shaft ratio on a
 * split shaft trips Create's same-network overspeed / flicker breaker.
 */
public class ThrustVectoringGearboxBlockEntity extends SplitShaftBlockEntity
        implements BlockEntitySubLevelActor, AutoflightAssemblyBlock {
    public static final float MAX_OUTPUT_RPM = 256f;
    public static final float RPM_PER_NEWTON = 0.15f;
    public static final float MAX_THRUST_NEWTONS = MAX_OUTPUT_RPM * RPM_PER_NEWTON;

    private Direction thrustAxis = Direction.UP;
    /** PID thrust correction along {@link #thrustAxis} (Newtons). */
    private float thrustDemand;

    public ThrustVectoringGearboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ThrustVectoringGearboxBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.THRUST_VECTORING_GEARBOX.get(), pos, state);
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (!hasSource()) {
            return 0f;
        }
        return 1f;
    }

    public void setThrustDemand(float newtons) {
        float clamped = Math.clamp(newtons, -MAX_THRUST_NEWTONS, MAX_THRUST_NEWTONS);
        if (Math.abs(clamped - this.thrustDemand) > 1e-4f) {
            this.thrustDemand = clamped;
            setChanged();
        }
    }

    public float getThrustDemand() {
        return thrustDemand;
    }

    /** Debug equivalent of thrust demand as signed RPM. */
    public float getTargetOutputRpm() {
        return Math.abs(RPM_PER_NEWTON) < 1e-6f ? 0f : -thrustDemand / RPM_PER_NEWTON;
    }

    public void setTargetOutputRpm(float rpm) {
        setThrustDemand(-rpm * RPM_PER_NEWTON);
    }

    public Direction getThrustAxis() {
        return thrustAxis;
    }

    public void setThrustAxis(Direction thrustAxis) {
        this.thrustAxis = thrustAxis;
        setChanged();
    }

    public float getThrustPerRpm() {
        return RPM_PER_NEWTON;
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle body, double dt) {
        if (level == null || level.isClientSide || Math.abs(thrustDemand) < 1e-6) {
            return;
        }
        ServerSubLevel root = AssemblyResolver.resolveRootAssembly(subLevel);
        Vector3d worldAxis = ThrustAssemblyHelper.worldThrustAxis(root, thrustAxis);
        Vector3d impulse = new Vector3d(worldAxis).mul(thrustDemand * dt);
        QueuedForceGroup propulsion = subLevel.getOrCreateQueuedForceGroup(
                ForceGroups.REGISTRY.get(ResourceLocation.fromNamespaceAndPath("sable", "propulsion"))
        );
        propulsion.applyAndRecordPointForce(JOMLConversion.atCenterOf(worldPosition), impulse);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("ThrustAxis", thrustAxis.get3DDataValue());
        tag.putFloat("ThrustDemand", thrustDemand);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("ThrustAxis")) {
            thrustAxis = Direction.from3DDataValue(tag.getInt("ThrustAxis"));
        }
        if (tag.contains("ThrustDemand")) {
            thrustDemand = Math.clamp(tag.getFloat("ThrustDemand"), -MAX_THRUST_NEWTONS, MAX_THRUST_NEWTONS);
        } else if (tag.contains("TargetRpm")) {
            setTargetOutputRpm(tag.getFloat("TargetRpm"));
        }
    }
}

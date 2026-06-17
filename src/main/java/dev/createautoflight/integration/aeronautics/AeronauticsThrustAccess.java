package dev.createautoflight.integration.aeronautics;

import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlockEntity;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.block.propeller.BlockEntityPropeller;
import dev.ryanhcode.sable.api.block.propeller.BlockEntitySubLevelPropellerActor;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/**
 * Reads propeller thrust on a Sable assembly and estimates hover thrust from assembly mass.
 */
public final class AeronauticsThrustAccess {
    /** Rough gravity scale matching Aeronautics propeller thrust units. */
    private static final double GRAVITY_THRUST = 9.81;

    private AeronauticsThrustAccess() {}

    public static boolean isAvailable() {
        return ModList.get().isLoaded("aeronautics");
    }

    public static AssemblyThrustSample measure(ServerSubLevel root) {
        if (!isAvailable()) {
            return AssemblyThrustSample.zero();
        }

        Vector3d measured = new Vector3d();
        Quaterniond orientation = root.logicalPose().orientation();

        for (BlockEntitySubLevelActor actor : root.getPlot().getBlockEntityActors()) {
            if (!(actor instanceof BlockEntitySubLevelPropellerActor propActor)) {
                continue;
            }
            BlockEntityPropeller propeller = propActor.getPropeller();
            if (propeller == null || !propeller.isActive()) {
                continue;
            }
            Vector3d thrustDir = resolveThrustDirection(propeller, orientation);
            double magnitude = propeller.getScaledThrust();
            measured.add(new Vector3d(thrustDir).mul(magnitude));
        }

        double mass = root.getMassTracker().isInvalid() ? 0.0 : root.getMassTracker().getMass();
        Vector3d hover = new Vector3d(0, mass * GRAVITY_THRUST, 0);
        return new AssemblyThrustSample(measured, hover);
    }

    private static Vector3d resolveThrustDirection(BlockEntityPropeller propeller, Quaterniond orientation) {
        if (propeller instanceof PropellerBearingBlockEntity bearing) {
            Vector3d local = new Vector3d(bearing.thrustDirection);
            if (local.lengthSquared() > 1e-12) {
                return orientation.transform(local, new Vector3d()).normalize();
            }
        }
        Direction blockDir = propeller.getBlockDirection();
        Vector3d local = new Vector3d(blockDir.getStepX(), blockDir.getStepY(), blockDir.getStepZ());
        return orientation.transform(local, new Vector3d()).normalize();
    }

    public static Vector3d localAxisToWorld(ServerSubLevel root, Direction localAxis) {
        Vector3d local = axisVector(localAxis);
        return root.logicalPose().orientation().transform(local, new Vector3d()).normalize();
    }

    public static Vector3d axisVector(Direction axis) {
        return new Vector3d(axis.getStepX(), axis.getStepY(), axis.getStepZ());
    }

    public static BlockEntity blockEntity(BlockEntitySubLevelActor actor) {
        return (BlockEntity) actor;
    }
}

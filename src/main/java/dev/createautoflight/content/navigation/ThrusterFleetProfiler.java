package dev.createautoflight.content.navigation;

import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.createautoflight.content.thruster.ThrusterBlockEntity;
import org.joml.Vector3d;

public final class ThrusterFleetProfiler {
    private static final double DEFAULT_ACCEL = 4.0;

    private ThrusterFleetProfiler() {}

    public record FleetProfile(double maxThrustPerTick, int activeThrusters, double maxAcceleration) {}

    public static FleetProfile navProfile(ServerSubLevel root) {
        return profile(root, ThrusterBlockEntity.ThrusterMode.NAVIGATION);
    }

    public static FleetProfile brakeProfile(ServerSubLevel root) {
        return profile(root, ThrusterBlockEntity.ThrusterMode.BRAKE);
    }

    public static FleetProfile profile(ServerSubLevel root) {
        return navProfile(root);
    }

    private static FleetProfile profile(ServerSubLevel root, ThrusterBlockEntity.ThrusterMode mode) {
        double totalThrust = 0;
        int count = 0;
        for (BlockEntitySubLevelActor actor : root.getPlot().getBlockEntityActors()) {
            if (actor instanceof ThrusterBlockEntity thruster && thruster.isFleetCandidate(mode)) {
                totalThrust += thruster.getMaxThrust() * (thruster.getStrengthPercent() / 100.0);
                count++;
            }
        }
        if (count == 0) {
            return new FleetProfile(0, 0, DEFAULT_ACCEL);
        }
        double mass = root.getMassTracker() != null && !root.getMassTracker().isInvalid()
                ? root.getMassTracker().getMass()
                : 100.0;
        double accel = totalThrust / Math.max(mass, 1.0);
        return new FleetProfile(totalThrust / count, count, Math.max(accel, 0.1));
    }

    public static double effectiveDeceleration(FleetProfile nav, FleetProfile brake) {
        return Math.max(nav.maxAcceleration(), brake.maxAcceleration());
    }

    public static double stoppingDistance(double speed, FleetProfile profile) {
        if (profile.maxAcceleration() < 1e-6) {
            return 0;
        }
        return (speed * speed) / (2 * profile.maxAcceleration());
    }

    public static double combinedStoppingDistance(double speed, FleetProfile nav, FleetProfile brake) {
        return stoppingDistance(speed, new FleetProfile(0, 0, effectiveDeceleration(nav, brake)));
    }

    public static Vector3d scaleByForce(Vector3d desiredImpulse, FleetProfile profile, double dt) {
        double cap = profile.maxThrustPerTick() * dt;
        double len = desiredImpulse.length();
        if (len > cap && len > 1e-8) {
            return new Vector3d(desiredImpulse).mul(cap / len);
        }
        return desiredImpulse;
    }
}

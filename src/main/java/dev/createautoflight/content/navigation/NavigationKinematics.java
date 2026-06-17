package dev.createautoflight.content.navigation;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public final class NavigationKinematics {
    private static final double STOP_ROTATE_ANGLE_RAD = Math.toRadians(15);

    private NavigationKinematics() {}

    public record KinematicsResult(
            double distanceToTarget,
            double pitchToTarget,
            double yawToTarget,
            double movementSpeed,
            boolean needsStopAndRotate
    ) {}

    public static KinematicsResult measure(
            ServerSubLevel root,
            Vector3d targetWorld,
            Vector3d currentVelocity
    ) {
        Vector3d center = AssemblyBoundsTracker.assemblyCenterWorld(root);
        Vector3d toTarget = new Vector3d(targetWorld).sub(center);
        double dist = toTarget.length();
        if (dist < 1e-6) {
            return new KinematicsResult(0, 0, 0, currentVelocity.length(), false);
        }

        Vector3d dir = new Vector3d(toTarget).div(dist);
        Quaterniond orientation = new Quaterniond(root.logicalPose().orientation());

        Vector3d forward = orientation.transform(new Vector3d(0, 0, 1), new Vector3d()).normalize();
        Vector3d up = orientation.transform(new Vector3d(0, 1, 0), new Vector3d()).normalize();

        double yaw = Math.atan2(
                forward.x * dir.z - forward.z * dir.x,
                forward.x * dir.x + forward.z * dir.z
        );
        Vector3d flatForward = new Vector3d(forward.x, 0, forward.z);
        Vector3d flatDir = new Vector3d(dir.x, 0, dir.z);
        if (flatForward.lengthSquared() > 1e-6 && flatDir.lengthSquared() > 1e-6) {
            flatForward.normalize();
            flatDir.normalize();
            yaw = Math.atan2(
                    flatForward.x * flatDir.z - flatForward.z * flatDir.x,
                    flatForward.x * flatDir.x + flatForward.z * flatDir.z
            );
        }

        double pitch = Math.asin(Math.clamp(-dir.dot(up), -1, 1));
        double headingError = alignmentAngle(forward, dir);

        return new KinematicsResult(
                dist,
                pitch,
                yaw,
                currentVelocity.length(),
                headingError > STOP_ROTATE_ANGLE_RAD
        );
    }

    public static Quaterniond desiredOrientationToward(ServerSubLevel root, Vector3d targetWorld) {
        return desiredOrientationToward(root, targetWorld, false);
    }

    public static Quaterniond desiredOrientationToward(ServerSubLevel root, Vector3d targetWorld, boolean invert) {
        Vector3d center = AssemblyBoundsTracker.assemblyCenterWorld(root);
        Vector3d toTarget = new Vector3d(targetWorld).sub(center);
        if (invert) {
            toTarget.y = -toTarget.y;
        }
        if (toTarget.lengthSquared() < 1e-6) {
            return new Quaterniond(root.logicalPose().orientation());
        }
        return orientationFromForward(toTarget);
    }

    public static Vector3d toTargetVector(Vector3d center, Vector3d targetWorld, boolean invert) {
        Vector3d toTarget = new Vector3d(targetWorld).sub(center);
        if (invert) {
            toTarget.negate();
        }
        return toTarget;
    }

    /**
     * Helicopter gyro targets: full {@code maxPitchDeg} toward the destination when far,
     * fading linearly to 0° within {@code arrivalRadius} blocks of the goal (horizontal).
     */
    public static GyroTargetAngles helicopterTargetAngles(
            Vector3d center,
            Vector3d targetWorld,
            int maxPitchDeg,
            boolean invertPitch,
            double distToDest,
            int arrivalRadius
    ) {
        return helicopterTargetAngles(center, targetWorld, maxPitchDeg, invertPitch, distToDest, arrivalRadius, 0.0, 0.0);
    }

    public static GyroTargetAngles helicopterTargetAngles(
            Vector3d center,
            Vector3d targetWorld,
            int maxPitchDeg,
            boolean invertPitch,
            double distToDest,
            int arrivalRadius,
            double preserveRollDeg,
            double preserveYawDeg
    ) {
        Vector3d toTarget = new Vector3d(targetWorld).sub(center);
        double horizontalDist = Math.hypot(toTarget.x, toTarget.z);
        double pitchScaleDist = horizontalDist > 1e-3 ? horizontalDist : Math.abs(toTarget.y);

        if (horizontalDist < 1e-3) {
            double pitchDeg = helicopterEffectivePitchDeg(
                    toTarget, maxPitchDeg, pitchScaleDist, arrivalRadius, invertPitch
            );
            return new GyroTargetAngles(pitchDeg, preserveYawDeg, preserveRollDeg);
        }

        Vector3d flatDir = new Vector3d(toTarget.x, 0, toTarget.z).normalize();
        double pitchDeg = helicopterEffectivePitchDeg(
                toTarget, maxPitchDeg, pitchScaleDist, arrivalRadius, invertPitch
        );
        double yawDeg = Math.toDegrees(Math.atan2(flatDir.x, flatDir.z));
        return new GyroTargetAngles(pitchDeg, yawDeg, preserveRollDeg);
    }

    /** Builds orientation from level-flight pitch/yaw (degrees) and optional bank roll. */
    public static Quaterniond orientationFromLevelAttitude(GyroTargetAngles angles) {
        double yawRad = Math.toRadians(angles.yawDeg());
        double pitchRad = Math.toRadians(angles.pitchDeg());
        double rollRad = Math.toRadians(angles.rollDeg());
        return new Quaterniond()
                .rotateY(yawRad)
                .rotateX(-pitchRad)
                .rotateZ(rollRad);
    }

    public static Vector3d helicopterMovementDirection(
            Vector3d center,
            Vector3d targetWorld,
            int maxPitchDeg,
            boolean invertThrust,
            double distToDest,
            int arrivalRadius
    ) {
        Vector3d toTarget = new Vector3d(targetWorld).sub(center);
        double horizontalDist = Math.hypot(toTarget.x, toTarget.z);
        double pitchScaleDist = horizontalDist > 1e-3 ? horizontalDist : Math.abs(toTarget.y);

        if (horizontalDist < 1e-3) {
            double len = toTarget.length();
            Vector3d dir = len > 1e-6 ? new Vector3d(toTarget).div(len) : new Vector3d(0, 1, 0);
            return invertThrust ? dir.negate() : dir;
        }

        Vector3d flatDir = new Vector3d(toTarget.x, 0, toTarget.z).normalize();
        double pitchRad = Math.toRadians(
                helicopterEffectivePitchDeg(toTarget, maxPitchDeg, pitchScaleDist, arrivalRadius, false)
        );
        Vector3d dir = pitchDownFromHorizontal(flatDir, pitchRad);
        if (invertThrust) {
            dir.negate();
        }
        return dir;
    }

    public static Quaterniond desiredHelicopterOrientation(
            ServerSubLevel root,
            Vector3d targetWorld,
            int maxPitchDeg,
            boolean invertPitch,
            double distToDest,
            int arrivalRadius
    ) {
        Vector3d center = AssemblyBoundsTracker.assemblyCenterWorld(root);
        GyroTargetAngles current = GyroTargetAngles.fromOrientation(root.logicalPose().orientation());
        GyroTargetAngles angles = helicopterTargetAngles(
                center, targetWorld, maxPitchDeg, invertPitch, distToDest, arrivalRadius,
                current.rollDeg(), current.yawDeg()
        );
        return orientationFromLevelAttitude(angles);
    }

    /**
     * Signed pitch in degrees (nose-down positive). Magnitude is {@code maxPitchDeg} scaled by horizontal
     * distance to goal, with sign from whether the destination is below, above, or level with the craft.
     */
    private static double helicopterEffectivePitchDeg(
            Vector3d toTarget,
            int maxPitchDeg,
            double pitchScaleDist,
            int arrivalRadius,
            boolean invertPitch
    ) {
        double scale = helicopterPitchScale(pitchScaleDist, arrivalRadius);
        if (scale <= 1e-6 || maxPitchDeg <= 0) {
            return 0.0;
        }

        double sign;
        if (Math.abs(toTarget.y) < 0.5) {
            sign = 1.0;
        } else {
            sign = toTarget.y < 0.0 ? 1.0 : -1.0;
        }
        double pitchDeg = sign * maxPitchDeg * scale;
        return invertPitch ? -pitchDeg : pitchDeg;
    }

    /** 1 at {@code arrivalRadius} or farther, 0 at the goal. */
    private static double helicopterPitchScale(double scaleDist, int arrivalRadius) {
        if (arrivalRadius <= 0) {
            return 0.0;
        }
        return Math.clamp(scaleDist / arrivalRadius, 0.0, 1.0);
    }

    /** Rotates a horizontal direction by signed pitch (positive = nose-down) around the lateral axis. */
    private static Vector3d pitchDownFromHorizontal(Vector3d flatDir, double pitchRad) {
        if (Math.abs(pitchRad) <= 1e-8) {
            return new Vector3d(flatDir);
        }
        Vector3d right = new Vector3d(0, 1, 0).cross(flatDir, new Vector3d());
        if (right.lengthSquared() < 1e-12) {
            return new Vector3d(flatDir);
        }
        right.normalize();
        return rotateAroundAxis(flatDir, right, pitchRad);
    }

    private static Vector3d rotateAroundAxis(Vector3d vector, Vector3d axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        Vector3d cross = axis.cross(vector, new Vector3d());
        double dot = axis.dot(vector);
        return new Vector3d(vector)
                .mul(cos)
                .add(cross.mul(sin))
                .add(new Vector3d(axis).mul(dot * (1.0 - cos)));
    }

    /**
     * Builds an orientation whose local +Z axis points along {@code forward} in world space.
     */
    private static Quaterniond orientationFromForward(Vector3d forward) {
        Vector3d fwd = new Vector3d(forward).normalize();
        Vector3d worldUp = new Vector3d(0, 1, 0);
        Vector3d right = worldUp.cross(fwd, new Vector3d());
        if (right.lengthSquared() < 1e-6) {
            right.set(1, 0, 0);
        } else {
            right.normalize();
        }
        Vector3d up = fwd.cross(right, new Vector3d()).normalize();

        var m = new org.joml.Matrix3d();
        m.setColumn(0, right);
        m.setColumn(1, up);
        m.setColumn(2, fwd);
        return new Quaterniond().setFromNormalized(m);
    }

    private static double alignmentAngle(Vector3d currentDir, Vector3d targetDir) {
        double cos = Math.clamp(currentDir.dot(targetDir), -1.0, 1.0);
        double sin = currentDir.cross(targetDir, new Vector3d()).length();
        return Math.atan2(sin, cos);
    }
}

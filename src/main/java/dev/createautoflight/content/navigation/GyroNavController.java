package dev.createautoflight.content.navigation;

import dev.createautoflight.content.gyroscope.GyroPrecisionController;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public final class GyroNavController {
    private static final double STOP_ROTATE_ANGLE_RAD = Math.toRadians(15);
    private static final double NAV_KP = 2.4;
    private static final double NAV_PRECISION_KP = 6.0;
    private static final double CORRECTION_RAMP = 1e-4;

    private GyroNavController() {}

    public record GyroNavTorque(Vector3d localTorque, boolean blockTranslation, double headingErrorRad) {}

    /**
     * Drives enabled local axes toward {@code qTarget} using a body-frame rotation-vector PD controller.
     */
    public static GyroNavTorque computeTowardOrientation(
            Quaterniond currentOrientation,
            Quaterniond qTarget,
            Vector3d localAngVel,
            boolean stabilizePitch,
            boolean stabilizeYaw,
            boolean stabilizeRoll,
            int acceptAngleDeg
    ) {
        Quaterniond qDelta = currentOrientation.conjugate(new Quaterniond()).mul(qTarget);
        qDelta.normalize();
        if (qDelta.w < 0.0) {
            qDelta.set(-qDelta.x, -qDelta.y, -qDelta.z, -qDelta.w);
        }

        Vector3d rotBody = rotationVectorBody(qDelta);
        double totalErr = rotBody.length();

        boolean blockTranslation = totalErr > STOP_ROTATE_ANGLE_RAD;
        if (totalErr < 1e-8) {
            return new GyroNavTorque(new Vector3d(), blockTranslation, totalErr);
        }

        double acceptRad = Math.toRadians(Math.max(0, acceptAngleDeg));
        double angleScale = totalErr <= acceptRad
                ? 0.0
                : Math.clamp((totalErr - acceptRad) / CORRECTION_RAMP, 0.0, 1.0);
        if (totalErr <= GyroPrecisionController.PRECISION_ZONE_RAD) {
            angleScale = Math.max(angleScale, Math.clamp(totalErr / GyroPrecisionController.PRECISION_ZONE_RAD, 0.05, 1.0));
        }

        double kp = totalErr <= GyroPrecisionController.PRECISION_ZONE_RAD ? NAV_PRECISION_KP : NAV_KP;
        if (totalErr <= GyroPrecisionController.PRECISION_HOLD_RAD) {
            kp = Math.max(kp, kp * (GyroPrecisionController.PRECISION_HOLD_RAD / Math.max(totalErr, 1e-6)));
        }

        Vector3d torque = new Vector3d();
        double damp = totalErr <= GyroPrecisionController.PRECISION_ZONE_RAD ? 1.2 : 0.5;
        if (stabilizePitch) {
            torque.x = Math.clamp(rotBody.x * kp * angleScale, -Math.PI, Math.PI);
            torque.x -= localAngVel.x * damp;
        }
        if (stabilizeYaw) {
            torque.y = Math.clamp(rotBody.y * kp * angleScale, -Math.PI, Math.PI);
            torque.y -= localAngVel.y * damp;
        }
        if (stabilizeRoll) {
            torque.z = Math.clamp(rotBody.z * kp * angleScale, -Math.PI, Math.PI);
            torque.z -= localAngVel.z * damp;
        }

        return new GyroNavTorque(torque, blockTranslation, totalErr);
    }

    /** Drives toward explicit level-flight euler targets. */
    public static GyroNavTorque compute(
            Quaterniond currentOrientation,
            GyroTargetAngles targetAngles,
            Vector3d localAngVel,
            boolean stabilizePitch,
            boolean stabilizeYaw,
            boolean stabilizeRoll,
            int acceptAngleDeg
    ) {
        Quaterniond qTarget = NavigationKinematics.orientationFromLevelAttitude(targetAngles);
        return computeTowardOrientation(
                currentOrientation,
                qTarget,
                localAngVel,
                stabilizePitch,
                stabilizeYaw,
                stabilizeRoll,
                acceptAngleDeg
        );
    }

    /** Body-frame rotation vector (axis * angle) from a delta quaternion. */
    private static Vector3d rotationVectorBody(Quaterniond qDelta) {
        double w = Math.clamp(qDelta.w, -1.0, 1.0);
        Vector3d axis = new Vector3d(qDelta.x, qDelta.y, qDelta.z);
        if (w < 0.0) {
            w = -w;
            axis.negate();
        }
        double sinHalf = axis.length();
        if (sinHalf < 1e-8) {
            return new Vector3d();
        }
        double angle = 2.0 * Math.atan2(sinHalf, w);
        return axis.div(sinHalf).mul(angle);
    }

    /** Shortest rotation taking {@code from} to {@code to} (both unit vectors). */
    public static Quaterniond rotationBetween(Vector3d from, Vector3d to) {
        Vector3d cross = from.cross(to, new Vector3d());
        double dot = Math.clamp(from.dot(to), -1.0, 1.0);
        Quaterniond q = new Quaterniond(cross.x, cross.y, cross.z, 1.0 + dot);
        if (q.lengthSquared() < 1e-12) {
            if (dot < 0.0) {
                Vector3d ortho = Math.abs(from.x) < 0.9
                        ? new Vector3d(1, 0, 0).cross(from, new Vector3d())
                        : new Vector3d(0, 1, 0).cross(from, new Vector3d());
                ortho.normalize();
                return new Quaterniond().rotateAxis(Math.PI, ortho);
            }
            return new Quaterniond();
        }
        q.normalize();
        return q;
    }
}

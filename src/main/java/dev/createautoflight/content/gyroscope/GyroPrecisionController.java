package dev.createautoflight.content.gyroscope;

import org.joml.Vector3d;

/**
 * Adaptive torque ramp for the final degree of alignment when accept tolerance is 0°.
 * Within {@link #PRECISION_ZONE_RAD} of the goal: force 90% damping, keep correcting toward
 * exact zero, and boost torque when angle stops decreasing.
 */
public final class GyroPrecisionController {
    public static final double PRECISION_ZONE_RAD = Math.toRadians(1.0);
    public static final double PRECISION_HOLD_RAD = Math.toRadians(0.05);
    private static final int STUCK_TICKS = 5;
    private static final double FORCE_BOOST = 1.18;
    private static final double MAX_FORCE_MULT = 10.0;
    private static final double MIN_PROGRESS_RAD = Math.toRadians(0.002);

    private double forceMultiplier = 1.0;
    private int stuckTicks;
    private double lastAngle = Double.NaN;

    public record PrecisionState(
            boolean active,
            int dampingPercent,
            double forceMultiplier
    ) {
        public static PrecisionState inactive() {
            return new PrecisionState(false, -1, 1.0);
        }
    }

    public PrecisionState update(double angleRad) {
        if (angleRad > PRECISION_ZONE_RAD) {
            reset();
            return new PrecisionState(false, -1, 1.0);
        }

        if (!Double.isNaN(lastAngle)) {
            double progress = lastAngle - angleRad;
            if (progress < MIN_PROGRESS_RAD) {
                stuckTicks++;
            } else {
                stuckTicks = 0;
            }
            if (stuckTicks >= STUCK_TICKS) {
                forceMultiplier = Math.min(MAX_FORCE_MULT, forceMultiplier * FORCE_BOOST);
                stuckTicks = 0;
            }
        }
        lastAngle = angleRad;

        return new PrecisionState(true, GyroscopeBlockEntity.MAX_DAMPING_PERCENT, forceMultiplier);
    }

    public void reset() {
        forceMultiplier = 1.0;
        stuckTicks = 0;
        lastAngle = Double.NaN;
    }

    public static Vector3d scaleCorrectionTorque(
            Vector3d localCorrection,
            double angleRad,
            double angleScale,
            double maxTorque,
            boolean stabilizePitch,
            boolean stabilizeYaw,
            boolean stabilizeRoll,
            double kp,
            double forceMultiplier
    ) {
        Vector3d torque = new Vector3d();
        boolean holdBand = angleRad <= PRECISION_HOLD_RAD;
        double holdScale = holdBand ? Math.max(angleScale, angleRad / PRECISION_HOLD_RAD) : angleScale;

        if (stabilizePitch) {
            torque.x = correctionComponent(localCorrection.x, holdScale, kp) * maxTorque * forceMultiplier;
        }
        if (stabilizeYaw) {
            torque.y = correctionComponent(localCorrection.y, holdScale, kp) * maxTorque * forceMultiplier;
        }
        if (stabilizeRoll) {
            torque.z = correctionComponent(localCorrection.z, holdScale, kp) * maxTorque * forceMultiplier;
        }
        return torque;
    }

    private static double correctionComponent(double correction, double angleScale, double kp) {
        return Math.clamp(correction * kp * angleScale, -1.0, 1.0);
    }
}

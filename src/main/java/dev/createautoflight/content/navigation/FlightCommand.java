package dev.createautoflight.content.navigation;

import org.joml.Quaterniond;
import org.joml.Vector3d;

public record FlightCommand(
        Vector3d desiredWorldVelocity,
        Quaterniond desiredOrientation,
        boolean navActive,
        boolean collisionShrinkActive,
        float capsuleRadiusOverride,
        Vector3d activeWaypoint,
        boolean blockTranslation,
        ApproachPhase approachPhase,
        boolean requestBrakeAssist,
        boolean helicopterAssist,
        GyroTargetAngles gyroTargetAngles,
        int navMaxThrust
) {
    public static final float CAPSULE_RADIUS_FULL = -1f;
    public static final float CAPSULE_RADIUS_APPROACH = 0.5f;

    public static FlightCommand idle() {
        return new FlightCommand(
                new Vector3d(),
                new Quaterniond(),
                false,
                false,
                CAPSULE_RADIUS_FULL,
                null,
                false,
                ApproachPhase.NONE,
                false,
                false,
                GyroTargetAngles.ZERO,
                NavigationSettings.DEFAULT_NAV_MAX_THRUST
        );
    }

    /** Published briefly after nav stops when idle braking is enabled. */
    public static FlightCommand idleBraking() {
        return new FlightCommand(
                new Vector3d(),
                new Quaterniond(),
                false,
                false,
                CAPSULE_RADIUS_FULL,
                null,
                false,
                ApproachPhase.DOCKED,
                true,
                false,
                GyroTargetAngles.ZERO,
                NavigationSettings.DEFAULT_NAV_MAX_THRUST
        );
    }

    public boolean inApproach() {
        return approachPhase != ApproachPhase.NONE && approachPhase != ApproachPhase.DOCKED;
    }
}

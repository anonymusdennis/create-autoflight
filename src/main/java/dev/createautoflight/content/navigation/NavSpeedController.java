package dev.createautoflight.content.navigation;

public final class NavSpeedController {
    public static final double BASE_CRUISE_BLOCKS_PER_SEC = 8.0;
    /** Max approach speed as fraction of slow speed cap. */
    private static final double APPROACH_SPEED_FACTOR = 0.65;
    /** Final dock uses sqrt profile: v = sqrt(2 * a * dist * margin). */
    private static final double FINAL_DOCK_DECEL_MARGIN = 0.45;

    private NavSpeedController() {}

    public record SpeedPlan(
            double cruiseSpeed,
            double slowSpeed,
            double approachSpeedCap,
            double finalDockSpeedCap,
            boolean coasting
    ) {}

    public static SpeedPlan compute(
            NavigationSettings settings,
            ApproachPhase phase,
            double distToDest,
            double stoppingDistance,
            double maxDeceleration,
            double closestObstacle
    ) {
        double cruise = BASE_CRUISE_BLOCKS_PER_SEC * (settings.getCruiseSpeedPercent() / 100.0);
        double slow = BASE_CRUISE_BLOCKS_PER_SEC * (settings.getSlowSpeedPercent() / 100.0);

        if (phase != ApproachPhase.NONE && phase != ApproachPhase.DOCKED) {
            double approachScale = Math.clamp(
                    distToDest / Math.max(settings.getAvoidanceOffDistance(), 1),
                    0.05,
                    1.0
            );
            cruise *= approachScale * APPROACH_SPEED_FACTOR;
            slow *= approachScale * APPROACH_SPEED_FACTOR;
        } else if (distToDest < 2 * stoppingDistance) {
            double scale = Math.clamp(distToDest / (2 * stoppingDistance), 0.1, 1.0);
            cruise *= scale;
            slow *= scale;
        }

        if (closestObstacle < slow && phase == ApproachPhase.NONE) {
            double cap = closestObstacle + 10;
            cruise = Math.min(cruise, cap);
            slow = Math.min(slow, cap);
        }

        double approachCap = slow * APPROACH_SPEED_FACTOR;
        double finalDockCap = Math.sqrt(Math.max(0, 2 * maxDeceleration * distToDest * FINAL_DOCK_DECEL_MARGIN));
        finalDockCap = Math.min(finalDockCap, slow);

        boolean coasting = false;

        return new SpeedPlan(cruise, slow, approachCap, finalDockCap, coasting);
    }

    public static double selectSpeed(
            SpeedPlan plan,
            ApproachPhase phase,
            double distToDest,
            int arrivalRadius,
            double currentSpeed
    ) {
        if (phase == ApproachPhase.DOCKED) {
            return 0;
        }
        if (plan.coasting() && phase == ApproachPhase.NONE) {
            return 0;
        }

        return switch (phase) {
            case FINAL_DOCK -> Math.min(plan.finalDockSpeedCap(), plan.slowSpeed());
            case APPROACH -> {
                double blend = Math.clamp(
                        (distToDest - arrivalRadius) / Math.max(settingsBlendRange(arrivalRadius), 1),
                        0,
                        1
                );
                yield plan.approachSpeedCap() * blend;
            }
            case NONE -> {
                double blend = Math.clamp((distToDest - arrivalRadius) / Math.max(arrivalRadius, 1), 0, 1);
                yield plan.slowSpeed() + (plan.cruiseSpeed() - plan.slowSpeed()) * blend;
            }
            default -> 0;
        };
    }

    /** Never command faster than we can bleed off with current speed (docking safety). */
    public static double clampToStoppingProfile(double targetSpeed, double distToDest, double maxDeceleration) {
        if (maxDeceleration < 1e-6 || distToDest <= 0) {
            return targetSpeed;
        }
        double safeMax = Math.sqrt(2 * maxDeceleration * distToDest * FINAL_DOCK_DECEL_MARGIN);
        return Math.min(targetSpeed, safeMax);
    }

    private static double settingsBlendRange(int arrivalRadius) {
        return Math.max(arrivalRadius * 2.0, 8.0);
    }
}

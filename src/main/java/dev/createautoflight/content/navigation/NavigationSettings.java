package dev.createautoflight.content.navigation;

public final class NavigationSettings {
    public static final int DEFAULT_ARRIVAL_RADIUS = 100;
    public static final int DEFAULT_AVOIDANCE_OFF_DISTANCE = 32;
    public static final int DEFAULT_CRUISE_SPEED_PERCENT = 100;
    public static final int DEFAULT_SLOW_SPEED_PERCENT = 25;
    public static final int MAX_AVOIDANCE_OFF_DISTANCE = 64;
    /** Distance (blocks) within which station-keep / docked hold applies zero cruise thrust. */
    public static final double HOLD_DOCK_TOLERANCE = 2.0;
    public static final int DEFAULT_HELICOPTER_MAX_PITCH_DEG = 20;
    public static final int MAX_HELICOPTER_PITCH_DEG = 45;
    public static final int DEFAULT_NAV_MAX_THRUST = 128;
    public static final int MAX_NAV_MAX_THRUST = 2048;

    private boolean activated;
    private boolean debugOverlayEnabled;
    private boolean idleBraking = true;
    private boolean helicopterMode;
    /** When true, helicopter pitch tilts nose-up instead of nose-down (seesaw); heading stays toward target. */
    private boolean invertAngle;
    /** When true, cruise thrust moves away from the destination instead of toward it. */
    private boolean invertThrust;
    private int helicopterMaxPitchDeg = DEFAULT_HELICOPTER_MAX_PITCH_DEG;
    private int navMaxThrust = DEFAULT_NAV_MAX_THRUST;
    private int avoidanceOffDistance = DEFAULT_AVOIDANCE_OFF_DISTANCE;
    private int arrivalRadius = DEFAULT_ARRIVAL_RADIUS;
    private int cruiseSpeedPercent = DEFAULT_CRUISE_SPEED_PERCENT;
    private int slowSpeedPercent = DEFAULT_SLOW_SPEED_PERCENT;
    private boolean ignoreTerrain;

    public boolean isActivated() { return activated; }
    public boolean isDebugOverlayEnabled() { return debugOverlayEnabled; }
    public int getAvoidanceOffDistance() { return avoidanceOffDistance; }
    public int getArrivalRadius() { return arrivalRadius; }
    public int getCruiseSpeedPercent() { return cruiseSpeedPercent; }
    public int getSlowSpeedPercent() { return slowSpeedPercent; }
    public boolean isIgnoreTerrain() { return ignoreTerrain; }
    public boolean isIdleBraking() { return idleBraking; }
    public boolean isHelicopterMode() { return helicopterMode; }
    public boolean isInvertAngle() { return invertAngle; }
    public boolean isInvertThrust() { return invertThrust; }
    public int getHelicopterMaxPitchDeg() { return helicopterMaxPitchDeg; }
    public int getNavMaxThrust() { return navMaxThrust; }

    public void setActivated(boolean activated) { this.activated = activated; }
    public void setDebugOverlayEnabled(boolean debugOverlayEnabled) { this.debugOverlayEnabled = debugOverlayEnabled; }
    public void setAvoidanceOffDistance(int avoidanceOffDistance) {
        this.avoidanceOffDistance = Math.clamp(avoidanceOffDistance, 0, MAX_AVOIDANCE_OFF_DISTANCE);
    }
    public void setArrivalRadius(int arrivalRadius) { this.arrivalRadius = Math.max(1, arrivalRadius); }
    public void setCruiseSpeedPercent(int cruiseSpeedPercent) {
        this.cruiseSpeedPercent = Math.clamp(cruiseSpeedPercent, 0, 100);
    }
    public void setSlowSpeedPercent(int slowSpeedPercent) {
        this.slowSpeedPercent = Math.clamp(slowSpeedPercent, 0, 100);
    }
    public void setIgnoreTerrain(boolean ignoreTerrain) { this.ignoreTerrain = ignoreTerrain; }
    public void setIdleBraking(boolean idleBraking) { this.idleBraking = idleBraking; }
    public void setHelicopterMode(boolean helicopterMode) { this.helicopterMode = helicopterMode; }
    public void setInvertAngle(boolean invertAngle) { this.invertAngle = invertAngle; }
    public void setInvertThrust(boolean invertThrust) { this.invertThrust = invertThrust; }
    public void setHelicopterMaxPitchDeg(int helicopterMaxPitchDeg) {
        this.helicopterMaxPitchDeg = Math.clamp(helicopterMaxPitchDeg, 0, MAX_HELICOPTER_PITCH_DEG);
    }
    public void setNavMaxThrust(int navMaxThrust) {
        this.navMaxThrust = Math.clamp(navMaxThrust, 1, MAX_NAV_MAX_THRUST);
    }

    public void apply(
            boolean activated,
            boolean debugOverlayEnabled,
            int avoidanceOffDistance,
            int arrivalRadius,
            int cruiseSpeedPercent,
            int slowSpeedPercent,
            boolean ignoreTerrain,
            boolean idleBraking,
            boolean helicopterMode,
            boolean invertAngle,
            boolean invertThrust,
            int helicopterMaxPitchDeg,
            int navMaxThrust
    ) {
        setActivated(activated);
        setDebugOverlayEnabled(debugOverlayEnabled);
        setAvoidanceOffDistance(avoidanceOffDistance);
        setArrivalRadius(arrivalRadius);
        setCruiseSpeedPercent(cruiseSpeedPercent);
        setSlowSpeedPercent(slowSpeedPercent);
        setIgnoreTerrain(ignoreTerrain);
        setIdleBraking(idleBraking);
        setHelicopterMode(helicopterMode);
        setInvertAngle(invertAngle);
        setInvertThrust(invertThrust);
        setHelicopterMaxPitchDeg(helicopterMaxPitchDeg);
        setNavMaxThrust(navMaxThrust);
    }
}

package dev.createautoflight.content.thrust;

public final class ThrustPidController {
    private double integral;
    private double lastError;

    public double update(double error, double dt, double kp, double ki, double kd) {
        integral += error * dt;
        integral = Math.clamp(integral, -500, 500);
        double derivative = dt > 1e-6 ? (error - lastError) / dt : 0;
        lastError = error;
        return kp * error + ki * integral + kd * derivative;
    }

    public void reset() {
        integral = 0;
        lastError = 0;
    }
}

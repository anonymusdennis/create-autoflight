package dev.createautoflight.integration.aeronautics;

import org.joml.Vector3d;

/** Measured assembly thrust and hover baseline in world space (Newtons-scale from Aeronautics). */
public record AssemblyThrustSample(Vector3d measuredWorld, Vector3d hoverBaselineWorld) {
    public static AssemblyThrustSample zero() {
        return new AssemblyThrustSample(new Vector3d(), new Vector3d());
    }

    public double thrustAlong(Vector3d worldAxis) {
        return measuredWorld.dot(worldAxis);
    }

    public double hoverAlong(Vector3d worldAxis) {
        return hoverBaselineWorld.dot(worldAxis);
    }
}

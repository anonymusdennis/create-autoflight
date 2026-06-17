package dev.createautoflight.content.navigation.path;

import org.joml.Vector3d;

public final class FlightPathCapsule {
    private final Vector3d p0;
    private final Vector3d p1;
    private final double radius;

    public FlightPathCapsule(Vector3d p0, Vector3d p1, double radius) {
        this.p0 = new Vector3d(p0);
        this.p1 = new Vector3d(p1);
        this.radius = Math.max(0.5, radius);
    }

    public Vector3d p0() { return p0; }
    public Vector3d p1() { return p1; }
    public double radius() { return radius; }

    public boolean intersectsAABB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        double cx = (minX + maxX) * 0.5;
        double cy = (minY + maxY) * 0.5;
        double cz = (minZ + maxZ) * 0.5;
        double half = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ)) * 0.5;
        return Math.sqrt(distanceSquaredToPoint(cx, cy, cz)) <= radius + half;
    }

    public boolean intersectsPoint(double x, double y, double z) {
        return distanceSquaredToPoint(x, y, z) <= radius * radius;
    }

    public double distanceSquaredToPoint(double x, double y, double z) {
        double t = closestT(x, y, z);
        double px = p0.x + (p1.x - p0.x) * t;
        double py = p0.y + (p1.y - p0.y) * t;
        double pz = p0.z + (p1.z - p0.z) * t;
        double dx = x - px;
        double dy = y - py;
        double dz = z - pz;
        return dx * dx + dy * dy + dz * dz;
    }

    private double closestT(double x, double y, double z) {
        double dx = p1.x - p0.x;
        double dy = p1.y - p0.y;
        double dz = p1.z - p0.z;
        double lenSq = dx * dx + dy * dy + dz * dz;
        if (lenSq < 1e-8) {
            return 0;
        }
        double t = ((x - p0.x) * dx + (y - p0.y) * dy + (z - p0.z) * dz) / lenSq;
        return Math.clamp(t, 0, 1);
    }
}

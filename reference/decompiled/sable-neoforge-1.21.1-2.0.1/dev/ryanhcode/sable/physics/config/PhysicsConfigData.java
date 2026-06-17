package dev.ryanhcode.sable.physics.config;

public class PhysicsConfigData {
   public int solverIterations = 18;
   public int pgsIterations = 2;
   public int stabilizationIterations = 2;
   public double contactSpringDampingRatio = 5.0;
   public double contactSpringFrequency = 40.0;
   public int minDynamicBodiesPerIsland = 128;
   public int substepsPerTick = 2;
}

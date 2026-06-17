package dev.simulated_team.simulated.config.server.physics;

import net.createmod.catnip.config.ConfigBase;
import net.createmod.catnip.config.ConfigBase.ConfigFloat;

public class SimPhysics extends ConfigBase {
   public final ConfigFloat redstoneMagnetStrength = this.f(
      1000.0F, 0.0F, Float.MAX_VALUE, "redstoneMagnetStrength", new String[]{"The maximum force two magnets will apply towards each other"}
   );
   public final ConfigFloat dockingConnectorStrength = this.f(
      1000.0F, 0.0F, Float.MAX_VALUE, "dockingConnectorStrength", new String[]{"The maximum force two docking connectors will apply towards each other"}
   );
   public final ConfigFloat redstoneMagnetLinearAccelerationClamping = this.f(
      500.0F, 0.0F, Float.MAX_VALUE, "redstoneMagnetLinearAccelerationClamping", new String[]{"Limit for linear acceleration for a magnet pair"}
   );
   public final ConfigFloat redstoneMagnetAngularAccelerationClamping = this.f(
      50.0F, 0.0F, Float.MAX_VALUE, "redstoneMagnetAngularAccelerationClamping", new String[]{"Limit for angular acceleration for a magnet pair"}
   );
   public final ConfigFloat dockingConnectorLinearAccelerationClamping = this.f(
      500.0F, 0.0F, Float.MAX_VALUE, "dockingConnectorLinearAccelerationClamping", new String[]{"Limit for linear acceleration for a docking connector pair"}
   );
   public final ConfigFloat dockingConnectorAngularAccelerationClamping = this.f(
      50.0F, 0.0F, Float.MAX_VALUE, "dockingConnectorAngularAccelerationClamping", new String[]{"Limit for angular acceleration for a docking connector pair"}
   );
   public final ConfigFloat swivelBearingStiffness = this.f(
      1600.0F, 0.0F, Float.MAX_VALUE, "swivel_stiffness", new String[]{"The stiffness of locked swivel bearing joints"}
   );
   public final ConfigFloat swivelBearingFriction = this.f(
      0.3F, 0.0F, Float.MAX_VALUE, "swivel_friction", new String[]{"The friction / damping of unlocked swivel bearing joints"}
   );
   public final ConfigFloat swivelBearingDamping = this.f(
      40.0F, 0.0F, Float.MAX_VALUE, "swivel_damping", new String[]{"The damping of locked swivel bearing joints"}
   );
   public final ConfigFloat dockingConnectorAngleTolerance = this.f(
      20.0F, 0.0F, 365.0F, "docking_connector_angle", new String[]{"The angle tolerance in degrees for docking connectors to link"}
   );
   public final ConfigFloat dockingConnectorDistanceTolerance = this.f(
      0.5F, 0.0F, 4.0F, "docking_connector_distance", new String[]{"The distance tolerance in blocks for docking connectors to link"}
   );
   public final ConfigFloat handleMaxForce = this.f(
      120.0F, 0.0F, Float.MAX_VALUE, "handleMaxForce", new String[]{"The maximum force handles are allowed to apply to the contraption they are attached to"}
   );
   public final ConfigFloat physicsStaffLinearStiffness = this.f(
      2650.0F, 0.0F, Float.MAX_VALUE, "physics_staff_linear_stiffness", new String[]{SimPhysics.Comments.physicsStaffLinearStiffness}
   );
   public final ConfigFloat physicsStaffLinearDamping = this.f(
      125.0F, 0.0F, Float.MAX_VALUE, "physics_staff_linear_damping", new String[]{SimPhysics.Comments.physicsStaffLinearDamping}
   );
   public final ConfigFloat physicsStaffAngularStiffness = this.f(
      10000.0F, 0.0F, Float.MAX_VALUE, "physics_staff_angular_stiffness", new String[]{SimPhysics.Comments.physicsStaffAngularStiffness}
   );
   public final ConfigFloat physicsStaffAngularDamping = this.f(
      850.0F, 0.0F, Float.MAX_VALUE, "physics_staff_angular_damping", new String[]{SimPhysics.Comments.physicsStaffAngularDamping}
   );

   public String getName() {
      return "physics";
   }

   private static class Comments {
      private static final String redstoneMagnetStrength = "The maximum force two magnets will apply towards each other";
      private static final String dockingConnectorStrength = "The maximum force two docking connectors will apply towards each other";
      private static final String redstoneMagnetLinearAccelerationClamping = "Limit for linear acceleration for a magnet pair";
      private static final String redstoneMagnetAngularAccelerationClamping = "Limit for angular acceleration for a magnet pair";
      private static final String dockingConnectorLinearAccelerationClamping = "Limit for linear acceleration for a docking connector pair";
      private static final String dockingConnectorAngularAccelerationClamping = "Limit for angular acceleration for a docking connector pair";
      private static final String swivelBearingStiffness = "The stiffness of locked swivel bearing joints";
      private static final String swivelBearingDamping = "The damping of locked swivel bearing joints";
      private static final String swivelBearingFriction = "The friction / damping of unlocked swivel bearing joints";
      private static final String dockingConnectorAngleTolerance = "The angle tolerance in degrees for docking connectors to link";
      private static final String dockingConnectorDistanceTolerance = "The distance tolerance in blocks for docking connectors to link";
      private static final String handleMaxForce = "The maximum force handles are allowed to apply to the contraption they are attached to";
      public static String physicsStaffLinearStiffness = "The linear stiffness of the joint motors used to hold sub-levels by the Creative Physics Staff";
      public static String physicsStaffLinearDamping = "The linear damping of the joint motors used to hold sub-levels by the Creative Physics Staff";
      public static String physicsStaffAngularStiffness = "The angular stiffness of the joint motors used to hold sub-levels by the Creative Physics Staff";
      public static String physicsStaffAngularDamping = "The angular damping of the joint motors used to hold sub-levels by the Creative Physics Staff";
   }
}

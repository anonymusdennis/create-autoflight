package dev.createautoflight.content.navigation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public record NavigationDebugSnapshot(
        Vec3 destinationWorld,
        Vec3 activeWaypointWorld,
        List<Vec3> pathWaypoints,
        Vec3 capsuleP0,
        Vec3 capsuleP1,
        float capsuleRadius,
        Vec3 assemblyCenterWorld,
        Vec3 desiredVelocityWorld,
        String mode,
        String pathfinderState,
        String approachPhase,
        double distToDest,
        double closestObstacleDist,
        boolean collisionShrinkActive,
        boolean brakeAssistActive,
        Vec3 boundsMinWorld,
        Vec3 boundsMaxWorld,
        double targetPitchDeg,
        double targetYawDeg,
        double targetRollDeg,
        double currentPitchDeg,
        double currentYawDeg,
        double currentRollDeg
) {
    public static NavigationDebugSnapshot empty() {
        return new NavigationDebugSnapshot(
                null, null, List.of(), null, null, 0,
                null, null, "Idle", "Path_Clear", "NONE",
                0, Double.MAX_VALUE, false, false, null, null,
                0, 0, 0, 0, 0, 0
        );
    }

    public void write(CompoundTag tag) {
        if (destinationWorld != null) {
            tag.putDouble("DestX", destinationWorld.x);
            tag.putDouble("DestY", destinationWorld.y);
            tag.putDouble("DestZ", destinationWorld.z);
        }
        if (activeWaypointWorld != null) {
            tag.putDouble("WpX", activeWaypointWorld.x);
            tag.putDouble("WpY", activeWaypointWorld.y);
            tag.putDouble("WpZ", activeWaypointWorld.z);
        }
        ListTag path = new ListTag();
        for (Vec3 p : pathWaypoints) {
            CompoundTag pt = new CompoundTag();
            pt.putDouble("X", p.x);
            pt.putDouble("Y", p.y);
            pt.putDouble("Z", p.z);
            path.add(pt);
        }
        tag.put("Path", path);
        if (capsuleP0 != null && capsuleP1 != null) {
            tag.putDouble("Cap0X", capsuleP0.x);
            tag.putDouble("Cap0Y", capsuleP0.y);
            tag.putDouble("Cap0Z", capsuleP0.z);
            tag.putDouble("Cap1X", capsuleP1.x);
            tag.putDouble("Cap1Y", capsuleP1.y);
            tag.putDouble("Cap1Z", capsuleP1.z);
            tag.putFloat("CapR", capsuleRadius);
        }
        if (assemblyCenterWorld != null) {
            tag.putDouble("CenterX", assemblyCenterWorld.x);
            tag.putDouble("CenterY", assemblyCenterWorld.y);
            tag.putDouble("CenterZ", assemblyCenterWorld.z);
        }
        if (desiredVelocityWorld != null) {
            tag.putDouble("VelX", desiredVelocityWorld.x);
            tag.putDouble("VelY", desiredVelocityWorld.y);
            tag.putDouble("VelZ", desiredVelocityWorld.z);
        }
        tag.putString("Mode", mode);
        tag.putString("PfState", pathfinderState);
        tag.putString("ApproachPhase", approachPhase);
        tag.putDouble("Dist", distToDest);
        tag.putDouble("ClosestObs", closestObstacleDist);
        tag.putBoolean("Shrink", collisionShrinkActive);
        tag.putBoolean("BrakeAssist", brakeAssistActive);
        if (boundsMinWorld != null && boundsMaxWorld != null) {
            tag.putDouble("BMinX", boundsMinWorld.x);
            tag.putDouble("BMinY", boundsMinWorld.y);
            tag.putDouble("BMinZ", boundsMinWorld.z);
            tag.putDouble("BMaxX", boundsMaxWorld.x);
            tag.putDouble("BMaxY", boundsMaxWorld.y);
            tag.putDouble("BMaxZ", boundsMaxWorld.z);
        }
        tag.putDouble("TgtPitch", targetPitchDeg);
        tag.putDouble("TgtYaw", targetYawDeg);
        tag.putDouble("TgtRoll", targetRollDeg);
        tag.putDouble("CurPitch", currentPitchDeg);
        tag.putDouble("CurYaw", currentYawDeg);
        tag.putDouble("CurRoll", currentRollDeg);
    }

    public static NavigationDebugSnapshot read(CompoundTag tag) {
        Vec3 dest = tag.contains("DestX")
                ? new Vec3(tag.getDouble("DestX"), tag.getDouble("DestY"), tag.getDouble("DestZ"))
                : null;
        Vec3 wp = tag.contains("WpX")
                ? new Vec3(tag.getDouble("WpX"), tag.getDouble("WpY"), tag.getDouble("WpZ"))
                : null;
        List<Vec3> path = new ArrayList<>();
        ListTag pathTag = tag.getList("Path", Tag.TAG_COMPOUND);
        for (Tag entry : pathTag) {
            CompoundTag pt = (CompoundTag) entry;
            path.add(new Vec3(pt.getDouble("X"), pt.getDouble("Y"), pt.getDouble("Z")));
        }
        Vec3 cap0 = tag.contains("Cap0X")
                ? new Vec3(tag.getDouble("Cap0X"), tag.getDouble("Cap0Y"), tag.getDouble("Cap0Z"))
                : null;
        Vec3 cap1 = tag.contains("Cap1X")
                ? new Vec3(tag.getDouble("Cap1X"), tag.getDouble("Cap1Y"), tag.getDouble("Cap1Z"))
                : null;
        Vec3 center = tag.contains("CenterX")
                ? new Vec3(tag.getDouble("CenterX"), tag.getDouble("CenterY"), tag.getDouble("CenterZ"))
                : null;
        Vec3 vel = tag.contains("VelX")
                ? new Vec3(tag.getDouble("VelX"), tag.getDouble("VelY"), tag.getDouble("VelZ"))
                : null;
        Vec3 bMin = tag.contains("BMinX")
                ? new Vec3(tag.getDouble("BMinX"), tag.getDouble("BMinY"), tag.getDouble("BMinZ"))
                : null;
        Vec3 bMax = tag.contains("BMaxX")
                ? new Vec3(tag.getDouble("BMaxX"), tag.getDouble("BMaxY"), tag.getDouble("BMaxZ"))
                : null;
        return new NavigationDebugSnapshot(
                dest, wp, path, cap0, cap1,
                tag.contains("CapR") ? tag.getFloat("CapR") : 0,
                center, vel,
                tag.getString("Mode"),
                tag.getString("PfState"),
                tag.contains("ApproachPhase") ? tag.getString("ApproachPhase") : "NONE",
                tag.getDouble("Dist"),
                tag.getDouble("ClosestObs"),
                tag.getBoolean("Shrink"),
                tag.getBoolean("BrakeAssist"),
                bMin, bMax,
                tag.contains("TgtPitch") ? tag.getDouble("TgtPitch") : 0,
                tag.contains("TgtYaw") ? tag.getDouble("TgtYaw") : 0,
                tag.contains("TgtRoll") ? tag.getDouble("TgtRoll") : 0,
                tag.contains("CurPitch") ? tag.getDouble("CurPitch") : 0,
                tag.contains("CurYaw") ? tag.getDouble("CurYaw") : 0,
                tag.contains("CurRoll") ? tag.getDouble("CurRoll") : 0
        );
    }
}

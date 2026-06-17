package dev.createautoflight.content.thrust;

import dev.createautoflight.content.navigation.AssemblyResolver;
import dev.createautoflight.integration.aeronautics.AeronauticsThrustAccess;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.Direction;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ThrustAssemblyHelper {
    private ThrustAssemblyHelper() {}

    public static List<ThrustVectoringGearboxBlockEntity> gearboxesOn(ServerSubLevel root) {
        List<ThrustVectoringGearboxBlockEntity> result = new ArrayList<>();
        for (BlockEntitySubLevelActor actor : root.getPlot().getBlockEntityActors()) {
            if (actor instanceof ThrustVectoringGearboxBlockEntity gearbox) {
                result.add(gearbox);
            }
        }
        return result;
    }

    public static List<DynamicThrustControllerBlockEntity> controllersOn(ServerSubLevel root) {
        List<DynamicThrustControllerBlockEntity> result = new ArrayList<>();
        for (BlockEntitySubLevelActor actor : root.getPlot().getBlockEntityActors()) {
            if (actor instanceof DynamicThrustControllerBlockEntity controller) {
                result.add(controller);
            }
        }
        return result;
    }

    public static DynamicThrustControllerBlockEntity primaryController(ServerSubLevel root) {
        return controllersOn(root).stream()
                .min(Comparator.comparingLong(c -> c.getBlockPos().asLong()))
                .orElse(null);
    }

    public static ServerSubLevel rootOf(BlockEntitySubLevelActor actor) {
        if (AeronauticsThrustAccess.blockEntity(actor).getLevel() == null) {
            return null;
        }
        var containing = dev.ryanhcode.sable.Sable.HELPER.getContaining(
                AeronauticsThrustAccess.blockEntity(actor).getLevel(),
                AeronauticsThrustAccess.blockEntity(actor).getBlockPos()
        );
        if (containing instanceof ServerSubLevel subLevel) {
            return AssemblyResolver.resolveRootAssembly(subLevel);
        }
        return null;
    }

    public static Vector3d worldThrustAxis(ServerSubLevel root, Direction localAxis) {
        return AeronauticsThrustAccess.localAxisToWorld(root, localAxis);
    }

    /** Negative RPM produces thrust opposite to the configured local axis. */
    public static float signedRpmForDemand(double thrustAlongAxis, float thrustPerRpm) {
        if (Math.abs(thrustPerRpm) < 1e-6f) {
            return 0f;
        }
        return (float) (-thrustAlongAxis / thrustPerRpm);
    }
}

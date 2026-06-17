package dev.createautoflight.content.navigation;

import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FlightCommandBus {
    private static final Map<UUID, FlightCommand> COMMANDS = new ConcurrentHashMap<>();
    private static final Map<UUID, NavigationDebugSnapshot> DEBUG = new ConcurrentHashMap<>();
    private static final Map<UUID, BlockPos> PRIMARY_NAV = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> PASSIVE_BRAKING = new ConcurrentHashMap<>();

    private FlightCommandBus() {}

    public static void publish(UUID assemblyId, BlockPos navPos, FlightCommand command, NavigationDebugSnapshot debug) {
        COMMANDS.put(assemblyId, command);
        if (command.navActive()) {
            PASSIVE_BRAKING.remove(assemblyId);
        }
        if (debug != null && !"Idle".equals(debug.mode())) {
            DEBUG.put(assemblyId, debug);
        } else {
            DEBUG.remove(assemblyId);
        }
        PRIMARY_NAV.put(assemblyId, navPos);
    }

    public static void clear(UUID assemblyId) {
        COMMANDS.remove(assemblyId);
        DEBUG.remove(assemblyId);
        PRIMARY_NAV.remove(assemblyId);
        PASSIVE_BRAKING.remove(assemblyId);
    }

    public static void clearAll() {
        COMMANDS.clear();
        DEBUG.clear();
        PRIMARY_NAV.clear();
        PASSIVE_BRAKING.clear();
    }

    public static void setPassiveBrakingAllowed(UUID assemblyId, boolean allowed) {
        if (allowed) {
            PASSIVE_BRAKING.remove(assemblyId);
        } else {
            PASSIVE_BRAKING.put(assemblyId, false);
        }
    }

    public static boolean isPassiveBrakingAllowed(UUID assemblyId) {
        return PASSIVE_BRAKING.getOrDefault(assemblyId, true);
    }

    public static FlightCommand get(UUID assemblyId) {
        return COMMANDS.getOrDefault(assemblyId, FlightCommand.idle());
    }

    public static NavigationDebugSnapshot getDebug(UUID assemblyId) {
        return DEBUG.get(assemblyId);
    }

    public static BlockPos getPrimaryNavPos(UUID assemblyId) {
        return PRIMARY_NAV.get(assemblyId);
    }

    public static boolean isShrinkActive(UUID assemblyId) {
        FlightCommand cmd = COMMANDS.get(assemblyId);
        return cmd != null && cmd.collisionShrinkActive();
    }
}

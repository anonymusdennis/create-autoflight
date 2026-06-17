package dev.createautoflight.client;

import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientThrustDebugCache {
    private static final Set<BlockPos> ENABLED = ConcurrentHashMap.newKeySet();

    private ClientThrustDebugCache() {}

    public static void setEnabled(BlockPos pos, boolean enabled) {
        if (enabled) {
            ENABLED.add(pos.immutable());
        } else {
            ENABLED.remove(pos);
        }
    }

    public static Set<BlockPos> positions() {
        return Collections.unmodifiableSet(ENABLED);
    }

    public static void clearAll() {
        ENABLED.clear();
    }
}

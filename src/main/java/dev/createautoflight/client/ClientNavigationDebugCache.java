package dev.createautoflight.client;

import dev.createautoflight.content.navigation.NavigationBlockEntity;
import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientNavigationDebugCache {
    private static final Set<BlockPos> POSITIONS = ConcurrentHashMap.newKeySet();

    private ClientNavigationDebugCache() {}

    public static void setEnabled(BlockPos pos, boolean enabled) {
        if (enabled) {
            POSITIONS.add(pos.immutable());
        } else {
            POSITIONS.remove(pos);
        }
    }

    public static void pruneMissing(net.minecraft.world.level.Level level) {
        POSITIONS.removeIf(pos -> {
            return !(level.getBlockEntity(pos) instanceof NavigationBlockEntity);
        });
    }

    public static Set<BlockPos> positions() {
        return Collections.unmodifiableSet(POSITIONS);
    }

    public static void clearAll() {
        POSITIONS.clear();
    }
}

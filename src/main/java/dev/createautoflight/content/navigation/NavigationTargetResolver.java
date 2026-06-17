package dev.createautoflight.content.navigation;

import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class NavigationTargetResolver {
    private static final ResourceLocation NAV_TABLE_ID =
            ResourceLocation.fromNamespaceAndPath("simulated", "navigation_table");

    private NavigationTargetResolver() {}

    public record ResolvedTarget(Vec3 worldPosition, NavigationMode mode) {}

    public static boolean hasNavTable(Level level, BlockPos navPos) {
        return findNavTable(level, navPos).isPresent();
    }

    public static Optional<NavTableBlockEntity> findNavTable(Level level, BlockPos navPos) {
        BlockEntity be = level.getBlockEntity(navPos.above());
        if (!(be instanceof NavTableBlockEntity navTable)) {
            return Optional.empty();
        }
        if (!BuiltInRegistries.BLOCK.getKey(navTable.getBlockState().getBlock()).equals(NAV_TABLE_ID)) {
            return Optional.empty();
        }
        return Optional.of(navTable);
    }

    /**
     * Snapshots the nav table's current world target once (compass / coordinate target at engage time).
     */
    public static Optional<ResolvedTarget> snapshotNavGoal(Level level, BlockPos navPos) {
        return resolveNavTable(level, navPos);
    }

    /**
     * Live nav table target — only use when no lever latch is active.
     */
    public static Optional<ResolvedTarget> resolveNavTable(Level level, BlockPos navPos) {
        Optional<NavTableBlockEntity> navTable = findNavTable(level, navPos);
        if (navTable.isEmpty()) {
            return Optional.empty();
        }
        NavTableBlockEntity table = navTable.get();
        if (table.getHeldItem().isEmpty()) {
            return Optional.empty();
        }
        NavigationTarget target = NavigationTarget.ofStack(table.getHeldItem());
        if (target == null) {
            return Optional.empty();
        }
        Vec3 pos = table.getTargetPosition(true);
        if (pos == null) {
            return Optional.empty();
        }
        return Optional.of(new ResolvedTarget(pos, NavigationMode.NAV_TABLE));
    }
}

package dev.createautoflight.content.navigation;

import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Reads external lever / button power into the nav stack without treating the nav table's own
 * navigation output as a lever-on signal.
 */
public final class NavigationLeverInput {
    private static final ResourceLocation NAV_TABLE_ID =
            ResourceLocation.fromNamespaceAndPath("simulated", "navigation_table");

    private NavigationLeverInput() {}

    public static boolean isNavigationEnabled(Level level, BlockPos navPos) {
        if (level == null) {
            return false;
        }
        if (NavigationTargetResolver.hasNavTable(level, navPos)) {
            return isTableInputPowered(level, navPos.above()) || isNavBlockInputPowered(level, navPos);
        }
        return level.hasNeighborSignal(navPos);
    }

    private static boolean isTableInputPowered(Level level, BlockPos tablePos) {
        BlockState state = level.getBlockState(tablePos);
        if (!BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(NAV_TABLE_ID)) {
            return false;
        }
        Direction facing = state.getValue(NavTableBlock.FACING);
        Direction.Axis outputAxis = facing.getAxis();
        for (Direction dir : Direction.values()) {
            if (dir.getAxis() == outputAxis) {
                continue;
            }
            if (level.getSignal(tablePos.relative(dir), dir.getOpposite()) > 0) {
                return true;
            }
        }
        return false;
    }

    /** Side inputs on the nav block; ignores the table output coming from above. */
    private static boolean isNavBlockInputPowered(Level level, BlockPos navPos) {
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP) {
                continue;
            }
            if (level.getSignal(navPos.relative(dir), dir.getOpposite()) > 0) {
                return true;
            }
        }
        return false;
    }
}

package dev.createautoflight.content.navigation;

/**
 * Approach / docking phase — foundation for automated landing and docking.
 */
public enum ApproachPhase {
    /** Normal cruise with full avoidance. */
    NONE,
    /** Within avoidance-off distance: shrunk hitbox, no dodge search, reduced speed. */
    APPROACH,
    /** Final deceleration segment before arrival radius. */
    FINAL_DOCK,
    /** Within arrival radius — zero commanded velocity, station-keep. */
    DOCKED
}

package dev.ryanhcode.sable.debug;

import java.util.UUID;
import net.minecraft.core.Direction.Axis;

public record GizmoSelection(UUID subLevel, Axis axis) {
}

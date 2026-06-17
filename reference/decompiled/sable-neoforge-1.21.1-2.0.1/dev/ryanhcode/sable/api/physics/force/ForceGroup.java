package dev.ryanhcode.sable.api.physics.force;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record ForceGroup(@NotNull Component name, @Nullable Component description, int color, boolean defaultDisplayed) {
}

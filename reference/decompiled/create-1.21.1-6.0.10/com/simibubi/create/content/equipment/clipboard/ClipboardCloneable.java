package com.simibubi.create.content.equipment.clipboard;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public interface ClipboardCloneable {
   String getClipboardKey();

   boolean writeToClipboard(@NotNull Provider var1, CompoundTag var2, Direction var3);

   boolean readFromClipboard(@NotNull Provider var1, CompoundTag var2, Player var3, Direction var4, boolean var5);
}

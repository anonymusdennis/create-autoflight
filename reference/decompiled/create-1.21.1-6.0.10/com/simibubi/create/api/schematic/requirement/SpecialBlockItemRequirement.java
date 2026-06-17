package com.simibubi.create.api.schematic.requirement;

import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface SpecialBlockItemRequirement {
   ItemRequirement getRequiredItems(BlockState var1, @Nullable BlockEntity var2);
}

package com.simibubi.create.content.logistics.packager;

import com.simibubi.create.api.packager.InventoryIdentifier;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public record IdentifiedInventory(@Nullable InventoryIdentifier identifier, IItemHandler handler) {
}

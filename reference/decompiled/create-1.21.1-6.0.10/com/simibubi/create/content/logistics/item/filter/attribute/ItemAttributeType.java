package com.simibubi.create.content.logistics.item.filter.attribute;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public interface ItemAttributeType {
   @NotNull
   ItemAttribute createAttribute();

   List<ItemAttribute> getAllAttributes(ItemStack var1, Level var2);

   MapCodec<? extends ItemAttribute> codec();

   StreamCodec<? super RegistryFriendlyByteBuf, ? extends ItemAttribute> streamCodec();
}

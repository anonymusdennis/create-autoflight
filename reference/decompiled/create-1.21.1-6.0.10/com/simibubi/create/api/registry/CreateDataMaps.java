package com.simibubi.create.api.registry;

import com.simibubi.create.Create;
import com.simibubi.create.api.data.datamaps.BlazeBurnerFuel;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

public class CreateDataMaps {
   public static final DataMapType<Item, BlazeBurnerFuel> REGULAR_BLAZE_BURNER_FUELS = DataMapType.builder(
         Create.asResource("regular_blaze_burner_fuels"), Registries.ITEM, BlazeBurnerFuel.CODEC
      )
      .build();
   public static final DataMapType<Item, BlazeBurnerFuel> SUPERHEATED_BLAZE_BURNER_FUELS = DataMapType.builder(
         Create.asResource("superheated_blaze_burner_fuels"), Registries.ITEM, BlazeBurnerFuel.CODEC
      )
      .build();

   private CreateDataMaps() {
      throw new AssertionError("This class should not be instantiated");
   }
}

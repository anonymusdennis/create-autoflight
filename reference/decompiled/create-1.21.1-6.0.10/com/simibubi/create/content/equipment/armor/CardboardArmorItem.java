package com.simibubi.create.content.equipment.armor;

import com.simibubi.create.Create;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.Item.Properties;

public class CardboardArmorItem extends BaseArmorItem {
   public CardboardArmorItem(Type type, Properties properties) {
      super(AllArmorMaterials.CARDBOARD, type, properties, Create.asResource("cardboard"));
   }
}

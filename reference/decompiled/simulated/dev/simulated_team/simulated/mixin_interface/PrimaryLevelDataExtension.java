package dev.simulated_team.simulated.mixin_interface;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.end.EndDragonFight.Data;

public interface PrimaryLevelDataExtension {
   ResourceLocation getPreset();

   void setPreset(ResourceLocation var1);

   void setEndDragonFight(Data var1);
}

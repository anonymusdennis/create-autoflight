package dev.simulated_team.simulated.service;

import com.tterrag.registrate.builders.EntityBuilder;
import dev.simulated_team.simulated.index.SimEntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public interface SimEntityService {
   SimEntityService INSTANCE = ServiceUtil.load(SimEntityService.class);

   CompoundTag getCustomData(Entity var1);

   double getPlayerReach(Player var1);

   boolean isFake(Player var1);

   <T extends Entity, P> EntityBuilder<T, P> loaderEntityTransform(EntityBuilder<T, P> var1, SimEntityTypes.EntityLoaderData var2);
}

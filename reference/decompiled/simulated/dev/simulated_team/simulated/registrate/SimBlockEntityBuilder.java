package dev.simulated_team.simulated.registrate;

import com.simibubi.create.foundation.data.CreateBlockEntityBuilder;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.builders.BlockEntityBuilder.BlockEntityFactory;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class SimBlockEntityBuilder<T extends BlockEntity, P> extends CreateBlockEntityBuilder<T, P> {
   public SimBlockEntityBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, BlockEntityFactory<T> factory) {
      super(owner, parent, name, callback, factory);
   }
}
